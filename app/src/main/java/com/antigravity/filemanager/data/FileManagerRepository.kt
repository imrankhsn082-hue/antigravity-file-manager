package com.antigravity.filemanager.data

import android.os.Environment
import android.os.StatFs
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
}
