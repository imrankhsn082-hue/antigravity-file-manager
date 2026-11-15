package com.antigravity.filemanager.data

import android.os.Environment
import android.os.StatFs
import com.antigravity.filemanager.model.CategoryBreakdown
import com.antigravity.filemanager.model.FileItem
import com.antigravity.filemanager.model.FileType
import com.antigravity.filemanager.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    SIZE_LARGEST,
    SIZE_SMALLEST
}

class FileManagerRepository {

    val rootStorageDirectory: File
        get() = Environment.getExternalStorageDirectory()

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val path = Environment.getExternalStorageDirectory().path
        val stat = StatFs(path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
        val usedPercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f

        StorageStats(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            usedPercent = usedPercent
        )
    }

    suspend fun listFiles(
        directory: File,
        sortOption: SortOption = SortOption.NAME_ASC,
        showHidden: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.canRead()) {
            return@withContext emptyList()
        }

        val rawFiles = directory.listFiles() ?: return@withContext emptyList()

        val items = rawFiles
            .filter { showHidden || !it.name.startsWith(".") }
            .map { FileItem(it) }

        // Sort: folders always first, then apply sort option
        val (folders, files) = items.partition { it.isDirectory }

        val sortedFolders = sortItemList(folders, sortOption)
        val sortedFiles = sortItemList(files, sortOption)

        sortedFolders + sortedFiles
    }

    private fun sortItemList(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
        return when (sortOption) {
            SortOption.NAME_ASC -> items.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> items.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> items.sortedByDescending { it.lastModified }
            SortOption.DATE_OLDEST -> items.sortedBy { it.lastModified }
            SortOption.SIZE_LARGEST -> items.sortedByDescending { it.size }
            SortOption.SIZE_SMALLEST -> items.sortedBy { it.size }
        }
    }

    suspend fun searchFiles(
        directory: File,
        query: String,
        maxResults: Int = 100
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !directory.exists()) return@withContext emptyList()
        val results = mutableListOf<FileItem>()

        fun walk(dir: File) {
            if (results.size >= maxResults) return
            val children = dir.listFiles() ?: return
            for (file in children) {
                if (file.name.startsWith(".")) continue
                if (file.name.contains(query, ignoreCase = true)) {
                    results.add(FileItem(file))
                }
                if (file.isDirectory && results.size < maxResults) {
                    walk(file)
                }
            }
        }

        walk(directory)
        results
    }

    suspend fun createFolder(parent: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        val trimmed = folderName.trim()
        if (trimmed.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Folder name cannot be empty"))
        val target = File(parent, trimmed)
        if (target.exists()) {
            return@withContext Result.failure(IllegalStateException("Folder already exists"))
        }
        val success = target.mkdirs()
        if (success) Result.success(target) else Result.failure(Exception("Failed to create folder"))
    }

    suspend fun createFile(parent: File, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) return@withContext Result.failure(IllegalArgumentException("File name cannot be empty"))
        val target = File(parent, trimmed)
        if (target.exists()) {
            return@withContext Result.failure(IllegalStateException("File already exists"))
        }
        try {
            val created = target.createNewFile()
            if (created) Result.success(target) else Result.failure(Exception("Failed to create file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(target: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
        val destination = File(target.parentFile, trimmed)
        if (destination.exists()) {
            return@withContext Result.failure(IllegalStateException("A file with this name already exists"))
        }
        val success = target.renameTo(destination)
        if (success) Result.success(destination) else Result.failure(Exception("Failed to rename"))
    }

    suspend fun deleteFile(target: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
            if (success) Result.success(true) else Result.failure(Exception("Failed to delete file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFileOrDirectory(source: File, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Source does not exist"))
            }
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            var destFile = File(destinationDir, source.name)
            if (destFile.exists()) {
                val base = source.nameWithoutExtension
                val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
                var counter = 1
                while (destFile.exists()) {
                    destFile = File(destinationDir, "${base}_copy$counter$ext")
                    counter++
                }
            }

            if (source.isDirectory) {
                source.copyRecursively(destFile, overwrite = false)
            } else {
                source.copyTo(destFile, overwrite = false)
            }
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFileOrDirectory(source: File, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Source does not exist"))
            }
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            var destFile = File(destinationDir, source.name)
            if (destFile.exists() && destFile.absolutePath != source.absolutePath) {
                val base = source.nameWithoutExtension
                val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
                var counter = 1
                while (destFile.exists()) {
                    destFile = File(destinationDir, "${base}_copy$counter$ext")
                    counter++
                }
            }

            val moved = source.renameTo(destFile)
            if (moved) {
                Result.success(destFile)
            } else {
                // Fallback across filesystems
                if (source.isDirectory) {
                    source.copyRecursively(destFile, overwrite = true)
                    source.deleteRecursively()
                } else {
                    source.copyTo(destFile, overwrite = true)
                    source.delete()
                }
                Result.success(destFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategoryFiles(
        category: FileType,
        baseDir: File = rootStorageDirectory,
        maxResults: Int = 200
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()

        fun scan(dir: File) {
            if (list.size >= maxResults) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    // Skip Android/data or system folders for performance
                    if (child.name.equals("Android", ignoreCase = true)) continue
                    scan(child)
                } else {
                    val item = FileItem(child)
                    if (item.fileType == category) {
                        list.add(item)
                    }
                }
            }
        }

        scan(baseDir)
        list.sortedByDescending { it.lastModified }
    }

    suspend fun getCategoryBreakdown(baseDir: File = rootStorageDirectory): CategoryBreakdown = withContext(Dispatchers.IO) {
        var images = 0L
        var videos = 0L
        var audio = 0L
        var docs = 0L
        var apks = 0L
        var archives = 0L
        var others = 0L

        fun scan(dir: File, depth: Int = 0) {
            if (depth > 6) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name.startsWith(".")) continue
                if (child.isDirectory) {
                    if (child.name.equals("Android", ignoreCase = true)) continue
                    if (child.name.equals(".Trash", ignoreCase = true)) continue
                    scan(child, depth + 1)
                } else {
                    val len = child.length()
                    val item = FileItem(child)
                    when (item.fileType) {
                        FileType.IMAGE -> images += len
                        FileType.VIDEO -> videos += len
                        FileType.AUDIO -> audio += len
                        FileType.DOCUMENT -> docs += len
                        FileType.APK -> apks += len
                        FileType.ARCHIVE -> archives += len
                        else -> others += len
                    }
                }
            }
        }

        scan(baseDir)
        val total = images + videos + audio + docs + apks + archives + others
        CategoryBreakdown(
            imagesSize = images,
            videosSize = videos,
            audioSize = audio,
            docsSize = docs,
            apksSize = apks,
            archivesSize = archives,
            othersSize = others,
            totalTrackedSize = total
        )
    }
}
