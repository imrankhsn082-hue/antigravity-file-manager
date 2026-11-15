package com.antigravity.filemanager.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antigravity.filemanager.ui.components.ViewerMenuAction
import com.antigravity.filemanager.ui.components.ViewerTopBar
import com.antigravity.filemanager.ui.components.openWithExternally
import com.antigravity.filemanager.ui.components.printBitmap
import com.antigravity.filemanager.ui.components.shareFileExternally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders PDF pages with the platform [PdfRenderer] into bitmaps.
 * A single instance owns the file descriptor and renderer, and pages are
 * rendered one at a time behind a mutex because PdfRenderer is not thread safe.
 */
private class PdfPageRenderer(file: File) {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val mutex = Mutex()

    val pageCount: Int = renderer.pageCount

    suspend fun renderPage(index: Int, targetWidth: Int): Bitmap? = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val page = renderer.openPage(index)
                try {
                    val width = targetWidth.coerceIn(320, 2400)
                    val height = (width.toFloat() * page.height.toFloat() / page.width.toFloat())
                        .toInt()
                        .coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } finally {
                    page.close()
                }
            }.getOrNull()
        }
    }

    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

/**
 * In-app PDF reader: every page is rendered to a bitmap and shown in a scrollable
 * list, with the standard viewer top bar and overflow menu.
 */
@Composable
fun PdfViewerScreen(
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
    val scope = rememberCoroutineScope()

    val renderer = remember(filePath) {
        runCatching { PdfPageRenderer(file) }.getOrNull()
    }

    DisposableEffect(renderer) {
        onDispose { renderer?.close() }
    }

    val menuActions = listOf(
        ViewerMenuAction("Open with") { openWithExternally(context, file) },
        ViewerMenuAction("Move to Trash", destructive = true) { onMoveToTrash() },
        ViewerMenuAction("Move to") { onMoveTo() },
        ViewerMenuAction("Copy to") { onCopyTo() },
        ViewerMenuAction("File info") { onShowInfo() },
        ViewerMenuAction("Print") {
            val target = renderer
            if (target == null) {
                Toast.makeText(context, "Unable to print this document", Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    val bitmap = target.renderPage(0, 1600)
                    if (bitmap != null) {
                        printBitmap(context, file.name, bitmap)
                    } else {
                        Toast.makeText(context, "Unable to print this document", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ViewerTopBar(
            title = file.name,
            subtitle = renderer?.let { "${it.pageCount} page${if (it.pageCount == 1) "" else "s"}" },
            isFavorite = isFavorite,
            onBack = onBack,
            onShare = { shareFileExternally(context, file) },
            onToggleFavorite = onToggleFavorite,
            menuActions = menuActions
        )

        if (renderer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "This PDF could not be opened",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "It may be password protected or corrupted. Try \"Open with\" to use another app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val pageWidthPx = constraints.maxWidth

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    items(renderer.pageCount) { index ->
                        PdfPageItem(
                            renderer = renderer,
                            index = index,
                            widthPx = pageWidthPx
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    renderer: PdfPageRenderer,
    index: Int,
    widthPx: Int
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, index, widthPx) {
        value = renderer.renderPage(index, widthPx)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val page = bitmap
        if (page != null) {
            Image(
                bitmap = page.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
    }
}
