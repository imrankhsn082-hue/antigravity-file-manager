package com.antigravity.filemanager.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.antigravity.filemanager.ui.components.ViewerMenuAction
import com.antigravity.filemanager.ui.components.ViewerTopBar
import com.antigravity.filemanager.ui.components.formatPlaybackTime
import com.antigravity.filemanager.ui.components.openWithExternally
import com.antigravity.filemanager.ui.components.shareFileExternally
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private val VIDEO_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Maximum boost the volume slider can add on top of 100% system volume. */
private const val MAX_BOOST_MILLIBELS = 1200

private fun videoSpeedLabel(speed: Float): String = when (speed) {
    0.5f -> "0.5x"
    0.75f -> "0.75x"
    1f -> "1.0x"
    1.25f -> "1.25x"
    1.5f -> "1.5x"
    2f -> "2.0x"
    else -> String.format(Locale.US, "%.2fx", speed)
}

/** One selectable audio or subtitle track of the current video. */
private data class TrackChoice(
    val label: String,
    val group: Tracks.Group,
    val trackIndex: Int
)

private fun supportedTrackChoices(tracks: Tracks?, type: Int): List<TrackChoice> {
    if (tracks == null) return emptyList()
    val choices = mutableListOf<TrackChoice>()
    tracks.groups
        .filter { it.type == type }
        .forEach { group ->
            group.formats.indices.forEach { index ->
                if (group.isTrackSupported(index)) {
                    val format = group.getTrackFormat(index)
                    val label = format.label?.takeIf { it.isNotBlank() }
                        ?: format.language?.takeIf { it.isNotBlank() }?.let { language ->
                            "Language: ${language.uppercase(Locale.getDefault())}"
                        }
                        ?: "Track ${choices.size + 1}"
                    choices.add(TrackChoice(label, group, index))
                }
            }
        }
    return choices
}

/** Reads the display name of a content:// URI so we can pick the subtitle MIME type. */
private fun contentDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}

/** Maps a subtitle file extension to the MIME type ExoPlayer expects. */
private fun subtitleMimeType(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase(Locale.US)) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        "ttml", "xml", "dfxp" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

/**
 * In-app video player with:
 * - custom transport controls (seek, ±10s, play/pause, rotate, speed)
 * - a separate volume slider that can boost the sound above 100% (LoudnessEnhancer)
 * - audio track switching for videos with dual/multiple audio tracks
 * - subtitle track selection (or turning subtitles off)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    filePath: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToTrash: () -> Unit,
    onMoveTo: () -> Unit,
    onCopyTo: () -> Unit,
    onShowInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }
    val activity = context as? android.app.Activity

    val player = remember {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var externalSubtitle by remember { mutableStateOf<MediaItem.SubtitleConfiguration?>(null) }
    var subtitlesOff by remember { mutableStateOf(false) }
    var hasStartedPlayback by remember { mutableStateOf(false) }

    LaunchedEffect(filePath, externalSubtitle) {
        val resumePosition = if (hasStartedPlayback) player.currentPosition else 0L
        val itemBuilder = MediaItem.Builder().setUri(Uri.fromFile(file))
        externalSubtitle?.let { subtitle ->
            itemBuilder.setSubtitleConfigurations(listOf(subtitle))
        }
        player.setMediaItem(itemBuilder.build())
        player.prepare()
        if (resumePosition > 0L) {
            player.seekTo(resumePosition)
        }
        player.playWhenReady = true
        hasStartedPlayback = true
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = contentDisplayName(context, uri) ?: "subtitle"
            // Adding a new subtitle should always switch them back on.
            subtitlesOff = false
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()
            externalSubtitle = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(subtitleMimeType(name))
                .setLanguage("und")
                .setLabel(name)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            Toast.makeText(context, "Subtitle added: $name", Toast.LENGTH_SHORT).show()
        }
    }

    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }
    var controlsVisible by remember { mutableStateOf(true) }

    // --- Volume / sound boost -------------------------------------------------
    var volumePercent by remember { mutableStateOf(100f) }
    var showVolumePanel by remember { mutableStateOf(false) }
    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }
    var enhancerSessionId by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { loudnessEnhancer?.release() }
        }
    }

    LaunchedEffect(volumePercent, loudnessEnhancer) {
        player.volume = (volumePercent / 100f).coerceIn(0f, 1f)
        val gain = ((volumePercent - 100f).coerceAtLeast(0f) / 100f * MAX_BOOST_MILLIBELS).toInt()
        val enhancer = loudnessEnhancer
        if (enhancer != null) {
            runCatching {
                enhancer.setTargetGain(gain)
                enhancer.setEnabled(gain > 0)
            }
        }
    }

    // --- Audio / subtitle tracks ---------------------------------------------
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val audioChoices = supportedTrackChoices(currentTracks, C.TRACK_TYPE_AUDIO)
    val subtitleChoices = supportedTrackChoices(currentTracks, C.TRACK_TYPE_TEXT)
    val selectedAudio = audioChoices.firstOrNull { it.group.isTrackSelected(it.trackIndex) }
    val selectedSubtitle = subtitleChoices.firstOrNull { it.group.isTrackSelected(it.trackIndex) }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            isBuffering = player.playbackState == Player.STATE_BUFFERING

            val sessionId = player.audioSessionId
            if (sessionId != 0 && sessionId != enhancerSessionId) {
                runCatching { loudnessEnhancer?.release() }
                loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
                enhancerSessionId = sessionId
            }

            delay(250)
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        val max = if (player.duration > 0) player.duration else target
        player.seekTo(target.coerceAtMost(max))
    }

    fun applyTrackOverride(choice: TrackChoice, type: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(
                TrackSelectionOverride(choice.group.mediaTrackGroup, choice.trackIndex)
            )
            .build()
    }

    fun useAutomaticTracks(type: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(type)
            .setTrackTypeDisabled(type, false)
            .build()
    }

    fun turnSubtitlesOff() {
        subtitlesOff = true
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    val menuActions = listOf(
        ViewerMenuAction("Audio track", trailingText = selectedAudio?.label) {
            if (audioChoices.size < 2) {
                Toast.makeText(context, "This video has a single audio track", Toast.LENGTH_SHORT).show()
            } else {
                val currentIndex = selectedAudio?.let { audioChoices.indexOf(it) } ?: -1
                val next = audioChoices[(currentIndex + 1).mod(audioChoices.size)]
                applyTrackOverride(next, C.TRACK_TYPE_AUDIO)
                Toast.makeText(context, "Audio: ${next.label}", Toast.LENGTH_SHORT).show()
            }
        },
        ViewerMenuAction(
            label = "Subtitles",
            trailingText = if (subtitlesOff) "Off" else selectedSubtitle?.label
        ) {
            if (subtitleChoices.isEmpty()) {
                Toast.makeText(
                    context,
                    "No embedded subtitles — use \"Add subtitle file…\"",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val currentPosition = if (subtitlesOff || selectedSubtitle == null) {
                    0
                } else {
                    subtitleChoices.indexOf(selectedSubtitle) + 1
                }
                val nextPosition = (currentPosition + 1).mod(subtitleChoices.size + 1)
                if (nextPosition == 0) {
                    turnSubtitlesOff()
                    Toast.makeText(context, "Subtitles off", Toast.LENGTH_SHORT).show()
                } else {
                    val choice = subtitleChoices[nextPosition - 1]
                    subtitlesOff = false
                    applyTrackOverride(choice, C.TRACK_TYPE_TEXT)
                    Toast.makeText(context, "Subtitles: ${choice.label}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        ViewerMenuAction(
            label = if (externalSubtitle != null) "Remove added subtitle" else "Add subtitle file…"
        ) {
            if (externalSubtitle != null) {
                externalSubtitle = null
                Toast.makeText(context, "Added subtitle removed", Toast.LENGTH_SHORT).show()
            } else {
                subtitlePicker.launch(arrayOf("*/*"))
            }
        },
        ViewerMenuAction("Open with") { openWithExternally(context, file) },
        ViewerMenuAction("Move to Trash", destructive = true) { onMoveToTrash() },
        ViewerMenuAction("Move to") { onMoveTo() },
        ViewerMenuAction("Copy to") { onCopyTo() },
        ViewerMenuAction("File info") { onShowInfo() },
        ViewerMenuAction("Playback speed", trailingText = videoSpeedLabel(speed)) {
            val next = VIDEO_SPEEDS.indexOfFirst { it == speed }
            speed = VIDEO_SPEEDS[(next + 1).mod(VIDEO_SPEEDS.size)]
            player.setPlaybackSpeed(speed)
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (controlsVisible) {
            ViewerTopBar(
                title = file.name,
                isFavorite = isFavorite,
                onBack = onBack,
                onShare = { shareFileExternally(context, file) },
                onToggleFavorite = onToggleFavorite,
                menuActions = menuActions,
                contentColor = Color.White
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { controlsVisible = !controlsVisible },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val boundPlayer = player
                    PlayerView(ctx).apply {
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                        this.player = boundPlayer
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Quick actions: volume boost, audio track, subtitles, speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showVolumePanel = !showVolumePanel }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume and sound boost",
                            tint = if (volumePercent > 100f) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White
                            }
                        )
                    }

                    if (audioChoices.size > 1) {
                        OptionsMenuButton(
                            icon = Icons.Default.Audiotrack,
                            contentDescription = "Audio track",
                            description = selectedAudio?.label,
                            tint = Color.White,
                            options = audioChoices.map { choice ->
                                choice.label to {
                                    applyTrackOverride(choice, C.TRACK_TYPE_AUDIO)
                                    Toast.makeText(context, "Audio: ${choice.label}", Toast.LENGTH_SHORT).show()
                                }
                            } + listOf(
                                "Automatic" to {
                                    useAutomaticTracks(C.TRACK_TYPE_AUDIO)
                                    Toast.makeText(context, "Audio: automatic", Toast.LENGTH_SHORT).show()
                                }
                            ),
                            selectedLabel = selectedAudio?.label
                        )
                    }
                    if (subtitleChoices.isNotEmpty()) {
                        val options = mutableListOf<Pair<String, () -> Unit>>()
                        options.add(
                            "Off" to {
                                turnSubtitlesOff()
                                Toast.makeText(context, "Subtitles off", Toast.LENGTH_SHORT).show()
                            }
                        )
                        subtitleChoices.forEach { choice ->
                            options.add(
                                choice.label to {
                                    subtitlesOff = false
                                    applyTrackOverride(choice, C.TRACK_TYPE_TEXT)
                                    Toast.makeText(context, "Subtitles: ${choice.label}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        OptionsMenuButton(
                            icon = Icons.Default.Subtitles,
                            contentDescription = "Subtitles",
                            description = selectedSubtitle?.label,
                            tint = if (subtitlesOff || selectedSubtitle == null) Color.White else MaterialTheme.colorScheme.primary,
                            options = options,
                            selectedLabel = if (subtitlesOff) "Off" else selectedSubtitle?.label
                        )
                    }

                    IconButton(onClick = {
                        val next = VIDEO_SPEEDS.indexOfFirst { it == speed }
                        speed = VIDEO_SPEEDS[(next + 1).mod(VIDEO_SPEEDS.size)]
                        player.setPlaybackSpeed(speed)
                        Toast.makeText(context, "Speed: ${videoSpeedLabel(speed)}", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback speed ${videoSpeedLabel(speed)}",
                            tint = Color.White
                        )
                    }
                }

                if (showVolumePanel) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volume",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Slider(
                            value = volumePercent,
                            onValueChange = { volumePercent = it },
                            valueRange = 0f..200f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        Text(
                            text = "${volumePercent.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (volumePercent > 100f) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatPlaybackTime(position),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    val safeDuration = duration.coerceAtLeast(0L)
                    Slider(
                        value = if (safeDuration > 0) {
                            position.coerceIn(0L, safeDuration).toFloat()
                        } else {
                            0f
                        },
                        onValueChange = { value ->
                            position = value.toLong()
                            player.seekTo(position)
                        },
                        valueRange = 0f..(if (safeDuration > 0) safeDuration.toFloat() else 1f),
                        enabled = safeDuration > 0,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Text(
                        text = formatPlaybackTime(safeDuration),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    IconButton(onClick = {
                        activity?.let { act ->
                            act.requestedOrientation =
                                if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate video",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { seekBy(-10_000L) }) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Back 10 seconds",
                            tint = Color.White
                        )
                    }

                    Surface(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        shape = CircleShape,
                        color = Color.White,
                        contentColor = Color.Black
                    ) {
                        Box(
                            modifier = Modifier.size(68.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    IconButton(onClick = { seekBy(10_000L) }) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Tap to show controls",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(12.dp)
            )
        }
    }
}

/** Small icon button that opens a dropdown list of selectable tracks. */
@Composable
private fun OptionsMenuButton(
    icon: ImageVector,
    contentDescription: String,
    description: String?,
    tint: Color,
    options: List<Pair<String, () -> Unit>>,
    selectedLabel: String?
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (description != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    enabled = false,
                    onClick = {}
                )
            }
            options.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (label == selectedLabel) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    },
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
        }
    }
}
