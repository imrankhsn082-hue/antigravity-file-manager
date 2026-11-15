package com.antigravity.filemanager.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.io.File

class BookmarkRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("antigravity_bookmarks", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BOOKMARKS = "saved_bookmarks"
    }

    fun getBookmarks(): List<String> {
        val rawJson = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val path = array.getString(i)
                if (File(path).exists()) {
                    list.add(path)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isBookmarked(path: String): Boolean {
        return getBookmarks().contains(path)
    }

    fun toggleBookmark(path: String): Boolean {
        val current = getBookmarks().toMutableList()
        val willBeBookmarked = !current.contains(path)
        if (willBeBookmarked) {
            current.add(path)
        } else {
            current.remove(path)
        }
        saveBookmarks(current)
        return willBeBookmarked
    }

    fun addBookmark(path: String) {
        val current = getBookmarks().toMutableList()
        if (!current.contains(path)) {
            current.add(path)
            saveBookmarks(current)
        }
    }

    fun removeBookmark(path: String) {
        val current = getBookmarks().toMutableList()
        if (current.remove(path)) {
            saveBookmarks(current)
        }
    }

    private fun saveBookmarks(bookmarks: List<String>) {
        val array = JSONArray()
        bookmarks.forEach { array.put(it) }
        prefs.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }
}
