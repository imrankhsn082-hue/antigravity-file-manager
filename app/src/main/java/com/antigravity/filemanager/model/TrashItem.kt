package com.antigravity.filemanager.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrashItem(
    val id: String,
    val name: String,
    val originalPath: String,
    val trashPath: String,
    val deletedTime: Long,
    val isDirectory: Boolean,
    val size: Long
) {
    val file: File get() = File(trashPath)

    val formattedSize: String
        get() = FileItem.formatFileSize(size)

    val formattedDeletedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(deletedTime))
        }

    val fileType: FileType
        get() = if (isDirectory) FileType.FOLDER else FileItem.determineFileType(File(originalPath))
}
