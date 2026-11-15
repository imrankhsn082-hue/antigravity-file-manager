package com.antigravity.filemanager.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun BreadcrumbsBar(
    rootDirectory: File,
    currentDirectory: File,
    onNavigateTo: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculate breadcrumb segments relative to rootDirectory
    val segments = buildList {
        var current: File? = currentDirectory
        while (current != null) {
            add(current)
            if (current == rootDirectory || current.parentFile == null) break
            current = current.parentFile
        }
    }.reversed()

    // Auto-scroll to end when folder changes
    LaunchedEffect(currentDirectory) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, file ->
                val isLast = index == segments.size - 1
                val isRoot = file == rootDirectory

                TextButton(
                    onClick = { if (!isLast) onNavigateTo(file) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (isRoot) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Internal Storage",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Storage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Separator",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
