package com.antigravity.filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.antigravity.filemanager.model.CategoryBreakdown
import com.antigravity.filemanager.ui.theme.*

private data class Segment(
    val name: String,
    val size: Long,
    val formattedSize: String,
    val color: Color
)

@Composable
fun StorageBreakdownCard(
    breakdown: CategoryBreakdown?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (breakdown == null && !isLoading) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Storage Breakdown",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage by Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (breakdown != null && breakdown.totalTrackedSize > 0) {
                val segments = listOf(
                    Segment("Images", breakdown.imagesSize, breakdown.formattedImages, ImagePink),
                    Segment("Videos", breakdown.videosSize, breakdown.formattedVideos, VideoPurple),
                    Segment("Audio", breakdown.audioSize, breakdown.formattedAudio, AudioGreen),
                    Segment("Docs", breakdown.docsSize, breakdown.formattedDocs, DocumentBlue),
                    Segment("APKs", breakdown.apksSize, breakdown.formattedApks, ApkGreen),
                    Segment("Archives", breakdown.archivesSize, breakdown.formattedArchives, ArchiveOrange),
                    Segment("Other", breakdown.othersSize, breakdown.formattedOthers, Color(0xFF78909C))
                )

                // Segmented Horizontal Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    segments.filter { it.size > 0 }.forEach { seg ->
                        val weight = (seg.size.toFloat() / breakdown.totalTrackedSize.toFloat()).coerceAtLeast(0.01f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(seg.color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val activeSegments = segments.filter { it.size > 0 }
                    val chunked = activeSegments.chunked(2)
                    chunked.forEach { rowSegments ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowSegments.forEach { seg ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(seg.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${seg.name}: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = seg.formattedSize,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (rowSegments.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else if (isLoading) {
                Text(
                    text = "Analyzing storage breakdown...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
