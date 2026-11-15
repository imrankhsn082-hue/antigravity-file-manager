package com.antigravity.filemanager.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class FileManagerPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("antigravity_file_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHOW_HIDDEN = "show_hidden_files"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_IS_GRID_VIEW = "is_grid_view"
    }

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try {
                ThemeMode.valueOf(name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var showHiddenFiles: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HIDDEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_HIDDEN, value).apply()
        }

    var sortOption: SortOption
        get() {
            val name = prefs.getString(KEY_SORT_OPTION, SortOption.NAME_ASC.name) ?: SortOption.NAME_ASC.name
            return try {
                SortOption.valueOf(name)
            } catch (e: Exception) {
                SortOption.NAME_ASC
            }
        }
        set(value) {
            prefs.edit().putString(KEY_SORT_OPTION, value.name).apply()
        }

    var isGridView: Boolean
        get() = prefs.getBoolean(KEY_IS_GRID_VIEW, false)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_GRID_VIEW, value).apply()
        }
}
