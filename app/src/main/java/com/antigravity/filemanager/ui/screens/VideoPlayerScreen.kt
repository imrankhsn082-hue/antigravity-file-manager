package com.antigravity.filemanager.ui.screens

import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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

private fun videoSpeedLabel(speed: Float): String = when (speed) {
    0.5f -> "0.5x"
    0.75f -> "0.75x"
    1f -> "1.0x"
    1.25f -> "1.25x"
    1.5f -> "1.5x"
    2f -> "2.0x"
    else -> String.format(Locale.US, "%.2fx", speed)
}

/**
 * In-app video player with a custom control bar: seek, elapsed/total time,
 * -10s / +10s, play/pause, rotate and playback speed in the overflow menu.
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

    LaunchedEffect(filePath) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.playWhenReady = true
    }

    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            isBuffering = player.playbackState == Player.STATE_BUFFERING
            delay(250)
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        val max = if (player.duration > 0) player.duration else target
        player.seekTo(target.coerceAtMost(max))
    }

    val menuActions = listOf(
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
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
