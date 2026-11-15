package com.antigravity.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.antigravity.filemanager.model.CategoryBreakdown
import com.antigravity.filemanager.model.FileItem
import com.antigravity.filemanager.model.FileType
import com.antigravity.filemanager.model.StorageStats

/**
 * Home dashboard inspired by the reference design: a stories-style recent media
 * carousel, a two column category grid, collections and a storage overview.
 */
@Composable
fun HomeDashboard(
    recentMedia: List<FileItem>,
    isMediaLoading: Boolean,
    breakdown: CategoryBreakdown?,
    storageStats: StorageStats?,
    onOpenMedia: (FileItem) -> Unit,
    onMediaMenu: (FileItem) -> Unit,
    onCategorySelected: (FileType?) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenStarred: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenInternalStorage: () -> Unit,
    onOpenOtherStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 132.dp)
    ) {
        if (isMediaLoading || recentMedia.isNotEmpty()) {
            item {
                RecentStoriesRow(
                    mediaItems = recentMedia,
                    isLoading = isMediaLoading,
                    onOpen = onOpenMedia,
                    onMenu = onMediaMenu
                )
            }
        }

        item { DashboardSectionHeader("Categories") }
        item {
            CategoryGrid(
                breakdown = breakdown,
                onCategorySelected = onCategorySelected,
                onOpenFolder = onOpenFolder
            )
        }

        item { DashboardSectionHeader("Collections") }
        item {
            CollectionsRow(
                onOpenStarred = onOpenStarred,
                onOpenTrash = onOpenTrash
            )
        }

        item { DashboardSectionHeader("All storage") }
        item {
            AllStorageRow(
                storageStats = storageStats,
                breakdown = breakdown,
                onOpenInternalStorage = onOpenInternalStorage,
                onOpenOtherStorage = onOpenOtherStorage
            )
        }
    }
}

@Composable
private fun DashboardSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
    )
}

/* ------------------------------------------------------------------ */
/* Stories                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun RecentStoriesRow(
    mediaItems: List<FileItem>,
    isLoading: Boolean,
    onOpen: (FileItem) -> Unit,
    onMenu: (FileItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mediaItems.isEmpty() && isLoading) {
            items(3) { StorySkeleton() }
        } else {
            items(mediaItems, key = { it.path }) { item ->
                StoryCard(item = item, onOpen = { onOpen(item) }, onMenu = { onMenu(item) })
            }
        }
    }
}

@Composable
private fun StorySkeleton() {
    Box(
        modifier = Modifier
            .width(166.dp)
            .height(218.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    )
}

@Composable
private fun StoryCard(
    item: FileItem,
    onOpen: () -> Unit,
    onMenu: () -> Unit
) {
    val isDownloaded = item.file.parentFile?.name?.equals("Download", ignoreCase = true) == true
    val parentName = item.file.parentFile?.name ?: "File"

    Box(
        modifier = Modifier
            .width(166.dp)
            .height(218.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
    ) {
        when (item.fileType) {
            FileType.IMAGE, FileType.VIDEO -> {
                AsyncImage(
                    model = item.file,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = getFileIconAndColor(item.fileType).first,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                )
            }
        }

        // Bottom scrim so the labels stay readable over any thumbnail.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )

        if (item.fileType == FileType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            )
        }

        if (isDownloaded) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Downloaded",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (isDownloaded) "Downloaded" else parentName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Categories                                                         */
/* ------------------------------------------------------------------ */

private data class DashboardCategory(
    val title: String,
    val size: Long,
    val icon: ImageVector,
    val type: FileType?,
    val folder: String? = null
)

@Composable
private fun CategoryGrid(
    breakdown: CategoryBreakdown?,
    onCategorySelected: (FileType?) -> Unit,
    onOpenFolder: (String) -> Unit
) {
    val categories = listOf(
        DashboardCategory("Downloads", breakdown?.downloadsSize ?: 0L, Icons.Default.Download, null, "Download"),
        DashboardCategory("Images", breakdown?.imagesSize ?: 0L, Icons.Default.Image, FileType.IMAGE),
        DashboardCategory("Videos", breakdown?.videosSize ?: 0L, Icons.Default.Movie, FileType.VIDEO),
        DashboardCategory("Audio", breakdown?.audioSize ?: 0L, Icons.Default.MusicNote, FileType.AUDIO),
        DashboardCategory("Documents", breakdown?.docsSize ?: 0L, Icons.Default.Description, FileType.DOCUMENT),
        DashboardCategory("Apps", breakdown?.apksSize ?: 0L, Icons.Default.Apps, FileType.APK)
    )

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { category ->
                    CategoryCard(
                        category = category,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (category.type != null) {
                                onCategorySelected(category.type)
                            } else {
                                category.folder?.let(onOpenFolder)
                            }
                        }
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    category: DashboardCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FileItem.formatFileSize(category.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Collections                                                        */
/* ------------------------------------------------------------------ */

@Composable
private fun CollectionsRow(
    onOpenStarred: () -> Unit,
    onOpenTrash: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CollectionCard(
            title = "Starred",
            icon = Icons.Outlined.StarBorder,
            onClick = onOpenStarred,
            modifier = Modifier.weight(1f)
        )
        CollectionCard(
            title = "Recycle Bin",
            icon = Icons.Default.Delete,
            onClick = onOpenTrash,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Storage                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun AllStorageRow(
    storageStats: StorageStats?,
    breakdown: CategoryBreakdown?,
    onOpenInternalStorage: () -> Unit,
    onOpenOtherStorage: () -> Unit
) {
    val otherBytes = ((storageStats?.usedBytes ?: 0L) - (breakdown?.totalTrackedSize ?: 0L))
        .coerceAtLeast(0L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StorageTile(
            title = "Internal storage",
            subtitle = storageStats?.let { "${it.formattedAvailable} free" } ?: "Calculating...",
            icon = Icons.Default.Smartphone,
            onClick = onOpenInternalStorage,
            modifier = Modifier.weight(1f)
        )
        StorageTile(
            title = "Other storage",
            subtitle = if (otherBytes > 0) "${FileItem.formatFileSize(otherBytes)} system & apps" else "System and other apps",
            icon = Icons.Default.Storage,
            onClick = onOpenOtherStorage,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
