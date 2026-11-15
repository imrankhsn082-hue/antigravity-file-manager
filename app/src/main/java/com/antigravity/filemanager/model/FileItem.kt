package com.antigravity.filemanager.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileType {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    CODE,
    OTHER
}

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(Locale.getDefault()),
    val fileType: FileType = determineFileType(file)
) {
    /**
     * Number of direct children for a directory. Computed lazily because listing a
     * directory is an expensive disk operation that should not run for every item
     * that is constructed (e.g. during search or category scans).
     */
    val childCount: Int by lazy(LazyThreadSafetyMode.NONE) {
        if (isDirectory) file.listFiles()?.size ?: 0 else 0
    }

    val formattedSize: String
        get() = if (isDirectory) {
            "$childCount items"
        } else {
            formatFileSize(size)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val isTextFile: Boolean
        get() {
            if (isDirectory) return false
            val textExtensions = setOf(
                "txt", "md", "markdown", "json", "xml", "html", "htm", "css", "js", "ts",
                "kt", "kts", "java", "py", "sh", "bash", "c", "cpp", "h", "hpp", "cs",
                "rb", "go", "rs", "php", "sql", "yaml", "yml", "ini", "conf", "properties",
                "gradle", "log", "csv", "tsv"
            )
            return extension in textExtensions
        }

    val isApkFile: Boolean
        get() = !isDirectory && (extension == "apk" || fileType == FileType.APK)

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val boundedGroup = digitGroups.coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, boundedGroup.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[boundedGroup])
        }

        fun determineFileType(file: File): FileType {
            if (file.isDirectory) return FileType.FOLDER
            val ext = file.extension.lowercase(Locale.getDefault())
            return when (ext) {
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> FileType.IMAGE
                "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv" -> FileType.VIDEO
                "mp3", "wav", "m4a", "flac", "ogg", "aac", "opus" -> FileType.AUDIO
                "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "odt" -> FileType.DOCUMENT
                "zip", "rar", "7z", "tar", "gz", "bz2" -> FileType.ARCHIVE
                "apk", "apks", "xapk" -> FileType.APK
                "kt", "java", "json", "xml", "html", "css", "js", "ts", "py", "sh", "c", "cpp", "md" -> FileType.CODE
                else -> FileType.OTHER
            }
        }
    }
}
