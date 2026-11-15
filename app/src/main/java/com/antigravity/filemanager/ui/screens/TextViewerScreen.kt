package com.antigravity.filemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.filemanager.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewerScreen(
    filePath: String,
    onBack: () -> Unit,
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
                    // Limit reading to 5MB to avoid OutOfMemoryError
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = FileItem.formatFileSize(file.length()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            fileContent?.let { content ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(file.name, content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "All text copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !fileContent.isNullOrEmpty()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy all text")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
