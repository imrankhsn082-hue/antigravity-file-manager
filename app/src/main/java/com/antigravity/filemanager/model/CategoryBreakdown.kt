package com.antigravity.filemanager.model

data class CategoryBreakdown(
    val imagesSize: Long = 0L,
    val videosSize: Long = 0L,
    val audioSize: Long = 0L,
    val docsSize: Long = 0L,
    val apksSize: Long = 0L,
    val archivesSize: Long = 0L,
    val othersSize: Long = 0L,
    val totalTrackedSize: Long = 0L
) {
    val formattedImages: String get() = FileItem.formatFileSize(imagesSize)
    val formattedVideos: String get() = FileItem.formatFileSize(videosSize)
    val formattedAudio: String get() = FileItem.formatFileSize(audioSize)
    val formattedDocs: String get() = FileItem.formatFileSize(docsSize)
    val formattedApks: String get() = FileItem.formatFileSize(apksSize)
    val formattedArchives: String get() = FileItem.formatFileSize(archivesSize)
    val formattedOthers: String get() = FileItem.formatFileSize(othersSize)
    val formattedTotal: String get() = FileItem.formatFileSize(totalTrackedSize)
}
