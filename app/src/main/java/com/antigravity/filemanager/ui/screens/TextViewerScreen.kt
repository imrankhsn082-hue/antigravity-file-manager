package com.antigravity.filemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.filemanager.model.FileItem
import com.antigravity.filemanager.ui.components.ViewerMenuAction
import com.antigravity.filemanager.ui.components.ViewerTopBar
import com.antigravity.filemanager.ui.components.openWithExternally
import com.antigravity.filemanager.ui.components.shareFileExternally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Built-in monospace text / code viewer with the shared viewer chrome
 * (back, share, favorite, overflow menu).
 */
@Composable
fun TextViewerScreen(
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
    var fileContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    errorMessage = "File does not exist: ${file.name}"
                } else if (file.length() > 5 * 1024 * 1024) {
                    // Limit reading to 1MB to avoid OutOfMemoryError on huge logs.
                    val preview = file.bufferedReader().use { reader ->
                        val buffer = CharArray(1024 * 1024)
                        val read = reader.read(buffer)
                        if (read > 0) String(buffer, 0, read) + "\n\n... [File truncated at 1MB]" else ""
                    }
                    fileContent = preview
                } else {
                    fileContent = file.readText(Charsets.UTF_8)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to read file: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val menuActions = listOf(
        ViewerMenuAction("Copy all text") {
            val content = fileContent
            if (content.isNullOrEmpty()) {
                Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
            } else {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(file.name, content))
                Toast.makeText(context, "All text copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        },
        ViewerMenuAction("Open with") { openWithExternally(context, file) },
        ViewerMenuAction("Move to Trash", destructive = true) { onMoveToTrash() },
        ViewerMenuAction("Move to") { onMoveTo() },
        ViewerMenuAction("Copy to") { onCopyTo() },
        ViewerMenuAction("File info") { onShowInfo() }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ViewerTopBar(
            title = file.name,
            subtitle = FileItem.formatFileSize(file.length()),
            isFavorite = isFavorite,
            onBack = onBack,
            onShare = { shareFileExternally(context, file) },
            onToggleFavorite = onToggleFavorite,
            menuActions = menuActions
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                fileContent != null -> {
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = fileContent!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
