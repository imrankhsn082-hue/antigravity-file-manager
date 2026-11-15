package com.antigravity.filemanager.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.antigravity.filemanager.model.TrashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class TrashRepository(
    context: Context,
    private val rootDir: File = Environment.getExternalStorageDirectory()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("antigravity_trash_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TRASH_ITEMS = "trash_items_json"
    }

    val trashDirectory: File
        get() = File(rootDir, ".Trash")

    suspend fun getTrashItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        val rawJson = prefs.getString(KEY_TRASH_ITEMS, null) ?: return@withContext emptyList()
        try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<TrashItem>()
            val validArray = JSONArray()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = TrashItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    originalPath = obj.getString("originalPath"),
                    trashPath = obj.getString("trashPath"),
                    deletedTime = obj.getLong("deletedTime"),
                    isDirectory = obj.getBoolean("isDirectory"),
                    size = obj.getLong("size")
                )
                if (File(item.trashPath).exists()) {
                    list.add(item)
                    validArray.put(obj)
                }
            }

            if (validArray.length() != array.length()) {
                prefs.edit().putString(KEY_TRASH_ITEMS, validArray.toString()).apply()
            }
            list.sortedByDescending { it.deletedTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun moveToTrash(target: File): Result<TrashItem> = withContext(Dispatchers.IO) {
        try {
            if (!target.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: ${target.absolutePath}"))
            }

            if (!trashDirectory.exists()) {
                trashDirectory.mkdirs()
            }

            val id = UUID.randomUUID().toString()
            val trashFileName = "${System.currentTimeMillis()}_${target.name}"
            val destination = File(trashDirectory, trashFileName)

            val isDirectory = target.isDirectory
            val size = if (isDirectory) calculateFolderSize(target) else target.length()
            val originalPath = target.absolutePath
            val originalName = target.name

            val moved = target.renameTo(destination) || copyAndDelete(target, destination)
            if (!moved) {
                return@withContext Result.failure(Exception("Failed to move file to trash"))
            }

            val trashItem = TrashItem(
                id = id,
                name = originalName,
                originalPath = originalPath,
                trashPath = destination.absolutePath,
                deletedTime = System.currentTimeMillis(),
                isDirectory = isDirectory,
                size = size
            )

            saveTrashItem(trashItem)
            Result.success(trashItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreItem(item: TrashItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (!trashFile.exists()) {
                removeTrashItem(item.id)
                return@withContext Result.failure(IllegalArgumentException("Trashed file no longer exists"))
            }

            val originalFile = File(item.originalPath)
            originalFile.parentFile?.mkdirs()

            // Check if a file with the original name already exists at destination
            val destination = if (originalFile.exists()) {
                val prefix = originalFile.nameWithoutExtension
                val ext = if (originalFile.extension.isNotEmpty()) ".${originalFile.extension}" else ""
                File(originalFile.parentFile, "${prefix}_restored_${System.currentTimeMillis()}$ext")
            } else {
                originalFile
            }

            val restored = trashFile.renameTo(destination) || copyAndDelete(trashFile, destination)
            if (restored) {
                removeTrashItem(item.id)
                Result.success(destination)
            } else {
                Result.failure(Exception("Failed to restore item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(item: TrashItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(item.trashPath)
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            removeTrashItem(item.id)
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val items = getTrashItems()
            items.forEach { item ->
                val file = File(item.trashPath)
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            // Clear any lingering files in trashDirectory
            trashDirectory.listFiles()?.forEach { f ->
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }
            prefs.edit().remove(KEY_TRASH_ITEMS).apply()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateFolderSize(folder: File): Long {
        var size = 0L
        folder.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }

    private fun copyAndDelete(source: File, dest: File): Boolean {
        return try {
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
                source.deleteRecursively()
            } else {
                source.copyTo(dest, overwrite = true)
                source.delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveTrashItem(item: TrashItem) {
        val rawJson = prefs.getString(KEY_TRASH_ITEMS, null)
        val array = if (rawJson != null) JSONArray(rawJson) else JSONArray()
        val obj = JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("originalPath", item.originalPath)
            put("trashPath", item.trashPath)
            put("deletedTime", item.deletedTime)
            put("isDirectory", item.isDirectory)
            put("size", item.size)
        }
        array.put(obj)
        prefs.edit().putString(KEY_TRASH_ITEMS, array.toString()).apply()
    }

    private fun removeTrashItem(id: String) {
        val rawJson = prefs.getString(KEY_TRASH_ITEMS, null) ?: return
        try {
            val array = JSONArray(rawJson)
            val updated = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("id") != id) {
                    updated.put(obj)
                }
            }
            prefs.edit().putString(KEY_TRASH_ITEMS, updated.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
