package com.antigravity.filemanager.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.antigravity.filemanager.model.FileType

data class CategoryItem(
    val title: String,
    val type: FileType?,
    val icon: ImageVector
)

val Categories = listOf(
    CategoryItem("All", null, Icons.Default.Folder),
    CategoryItem("Images", FileType.IMAGE, Icons.Default.Image),
    CategoryItem("Videos", FileType.VIDEO, Icons.Default.Videocam),
    CategoryItem("Audio", FileType.AUDIO, Icons.Default.Audiotrack),
    CategoryItem("Docs", FileType.DOCUMENT, Icons.Default.Description),
    CategoryItem("APKs", FileType.APK, Icons.Default.Android),
    CategoryItem("Archives", FileType.ARCHIVE, Icons.Default.Archive)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChips(
    selectedCategory: FileType?,
    onCategorySelected: (FileType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Categories.forEach { category ->
            val isSelected = selectedCategory == category.type
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(if (isSelected) null else category.type) },
                label = { Text(category.title) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.title,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
