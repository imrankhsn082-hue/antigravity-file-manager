package com.antigravity.filemanager.data

import android.content.Context
import android.content.SharedPreferences
import com.antigravity.filemanager.model.FileItem
import org.json.JSONArray
import java.io.File

class RecentFilesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("antigravity_recent_files", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECENTS = "recent_file_paths"
        private const val MAX_RECENTS = 20
    }

    fun getRecentFiles(): List<FileItem> {
        val rawJson = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<FileItem>()
            val existingPaths = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val path = array.getString(i)
                val file = File(path)
                if (file.exists() && !file.isDirectory) {
                    list.add(FileItem(file))
                    existingPaths.add(path)
                }
            }
            if (existingPaths.size != array.length()) {
                savePaths(existingPaths)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecentFile(file: File) {
        if (!file.exists() || file.isDirectory) return
        val path = file.absolutePath
        val rawJson = prefs.getString(KEY_RECENTS, null)
        val list = mutableListOf<String>()
        if (rawJson != null) {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val p = array.getString(i)
                    if (p != path) list.add(p)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        list.add(0, path)
        val trimmed = if (list.size > MAX_RECENTS) list.subList(0, MAX_RECENTS) else list
        savePaths(trimmed)
    }

    fun clearRecents() {
        prefs.edit().remove(KEY_RECENTS).apply()
    }

    private fun savePaths(paths: List<String>) {
        val array = JSONArray()
        paths.forEach { array.put(it) }
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }
}
