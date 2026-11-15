package com.antigravity.filemanager.model

data class StorageStats(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercent: Float
) {
    val formattedTotal: String
        get() = FileItem.formatFileSize(totalBytes)

    val formattedAvailable: String
        get() = FileItem.formatFileSize(availableBytes)

    val formattedUsed: String
        get() = FileItem.formatFileSize(usedBytes)
}
