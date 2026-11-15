package com.antigravity.filemanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.filemanager.data.BookmarkRepository
import com.antigravity.filemanager.data.FileManagerPreferences
import com.antigravity.filemanager.data.FileManagerRepository
import com.antigravity.filemanager.data.RecentFilesRepository
import com.antigravity.filemanager.data.SortOption
import com.antigravity.filemanager.data.ThemeMode
import com.antigravity.filemanager.data.TrashRepository
import com.antigravity.filemanager.model.CategoryBreakdown
import com.antigravity.filemanager.model.FileItem
import com.antigravity.filemanager.model.FileType
import com.antigravity.filemanager.model.StorageStats
import com.antigravity.filemanager.model.TrashItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ClipboardState(
    val files: List<FileItem>,
    val isCut: Boolean
)

data class FileManagerUiState(
    val currentDirectory: File,
    val items: List<FileItem> = emptyList(),
    val storageStats: StorageStats? = null,
    val categoryBreakdown: CategoryBreakdown? = null,
    val isLoading: Boolean = false,
    val isBreakdownLoading: Boolean = false,
    val selectedCategory: FileType? = null,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val showHiddenFiles: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Multi-select state
    val selectedItems: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    // Clipboard state
    val clipboard: ClipboardState? = null,
    // Bookmarks
    val bookmarks: List<String> = emptyList(),
    // Recent files
    val recentFiles: List<FileItem> = emptyList(),
    // Recent media shown in the home "stories" carousel
    val recentMedia: List<FileItem> = emptyList(),
    val isRecentMediaLoading: Boolean = false,
    val hasLoadedRecentMedia: Boolean = false,
    // Trash items
    val trashItems: List<TrashItem> = emptyList()
)

class FileManagerViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FileManagerRepository = FileManagerRepository(),
    private val preferences: FileManagerPreferences = FileManagerPreferences(application),
    private val bookmarkRepository: BookmarkRepository = BookmarkRepository(application),
    private val recentFilesRepository: RecentFilesRepository = RecentFilesRepository(application),
    private val trashRepository: TrashRepository = TrashRepository(application)
) : AndroidViewModel(application) {

    val rootDirectory: File = repository.rootStorageDirectory

    private val _uiState = MutableStateFlow(
        FileManagerUiState(
            currentDirectory = rootDirectory,
            sortOption = preferences.sortOption,
            isGridView = preferences.isGridView,
            showHiddenFiles = preferences.showHiddenFiles,
            themeMode = preferences.themeMode
        )
    )
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadCurrentDirectory()
        refreshStorageStats()
        loadBookmarks()
        loadRecentFiles()
        loadTrash()
        // The category breakdown scans the whole storage, so it is loaded lazily
        // the first time the storage overview is shown instead of on app start.
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getStorageStats()
                _uiState.update { it.copy(storageStats = stats) }
            } catch (e: Exception) {
                // Ignore stat errors
            }
        }
    }

    /** Loads the breakdown once; repeated calls while cached are effectively free. */
    fun ensureCategoryBreakdownLoaded() {
        val state = _uiState.value
        if (state.categoryBreakdown == null && !state.isBreakdownLoading) {
            loadCategoryBreakdown()
        }
    }

    fun loadCategoryBreakdown(forceRefresh: Boolean = false) {
        if (_uiState.value.isBreakdownLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBreakdownLoading = true) }
            try {
                val breakdown = repository.getCategoryBreakdown(forceRefresh = forceRefresh)
                _uiState.update { it.copy(categoryBreakdown = breakdown, isBreakdownLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBreakdownLoading = false) }
            }
        }
    }

    /** Refreshes the breakdown only when it is already on screen. */
    private fun refreshBreakdownIfLoaded() {
        if (_uiState.value.categoryBreakdown != null) {
            loadCategoryBreakdown(forceRefresh = true)
        }
    }

    /** Loads the home "stories" carousel media once per session (or on demand). */
    fun ensureRecentMediaLoaded(forceRefresh: Boolean = false) {
        val state = _uiState.value
        if (state.isRecentMediaLoading) return
        if (!forceRefresh && state.hasLoadedRecentMedia) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRecentMediaLoading = true) }
            val media = try {
                repository.getRecentMedia()
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.update {
                it.copy(
                    recentMedia = media,
                    isRecentMediaLoading = false,
                    hasLoadedRecentMedia = true
                )
            }
        }
    }

    private fun refreshMediaIfLoaded() {
        if (_uiState.value.hasLoadedRecentMedia) {
            ensureRecentMediaLoaded(forceRefresh = true)
        }
    }

    fun loadCurrentDirectory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val items = if (state.selectedCategory != null) {
                    repository.getCategoryFiles(state.selectedCategory)
                } else {
                    repository.listFiles(
                        directory = state.currentDirectory,
                        sortOption = state.sortOption,
                        showHidden = state.showHiddenFiles
                    )
                }
                _uiState.update {
                    it.copy(
                        items = items,
                        isLoading = false,
                        // Clean up selected items that no longer exist
                        selectedItems = it.selectedItems.filter { path -> File(path).exists() }.toSet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _messageEvents.emit("Error loading files: ${e.localizedMessage}")
            }
        }
    }

    fun navigateTo(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                currentDirectory = directory,
                selectedCategory = null,
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList(),
                isMultiSelectMode = false,
                selectedItems = emptySet()
            )
        }
        loadCurrentDirectory()
    }

    fun navigateUp(): Boolean {
        val state = _uiState.value

        if (state.isMultiSelectMode) {
            clearSelection()
            return true
        }

        if (state.selectedCategory != null) {
            _uiState.update { it.copy(selectedCategory = null) }
            loadCurrentDirectory()
            return true
        }

        if (state.isSearching) {
            closeSearch()
            return true
        }

        val parent = state.currentDirectory.parentFile
        if (parent != null && state.currentDirectory != rootDirectory && parent.canRead()) {
            navigateTo(parent)
            return true
        }
        return false
    }

    fun selectCategory(category: FileType?) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCategory = category,
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList(),
                isMultiSelectMode = false,
                selectedItems = emptySet()
            )
        }
        loadCurrentDirectory()
    }

    fun setSortOption(option: SortOption) {
        preferences.sortOption = option
        _uiState.update { it.copy(sortOption = option) }
        loadCurrentDirectory()
    }

    fun toggleGridView() {
        val newView = !_uiState.value.isGridView
        preferences.isGridView = newView
        _uiState.update { it.copy(isGridView = newView) }
    }

    fun toggleShowHiddenFiles() {
        val newShow = !_uiState.value.showHiddenFiles
        preferences.showHiddenFiles = newShow
        _uiState.update { it.copy(showHiddenFiles = newShow) }
        loadCurrentDirectory()
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun openSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(isSearching = true, searchQuery = "", searchResults = emptyList())
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce so that fast typing does not trigger a disk walk per keystroke.
            delay(250)
            val state = _uiState.value
            val results = repository.searchFiles(
                directory = state.currentDirectory,
                query = trimmed,
                showHidden = state.showHiddenFiles
            )
            // Discard stale results if the query changed while scanning.
            if (_uiState.value.searchQuery.trim() == trimmed) {
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun closeSearch() {
        searchJob?.cancel()
        searchJob = null
        _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
    }

    /** True while search mode is open with an actual query typed in. */
    private fun isShowingSearchResults(state: FileManagerUiState): Boolean {
        return state.isSearching && state.searchQuery.isNotBlank()
    }

    // --- Creation Operations ---

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = repository.createFolder(_uiState.value.currentDirectory, name)
            result.onSuccess {
                repository.invalidateCaches()
                _messageEvents.emit("Folder \"$name\" created")
                loadCurrentDirectory()
            }.onFailure {
                _messageEvents.emit("Failed to create folder: ${it.localizedMessage}")
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val result = repository.createFile(_uiState.value.currentDirectory, name)
            result.onSuccess {
                repository.invalidateCaches()
                _messageEvents.emit("File \"$name\" created")
                loadCurrentDirectory()
            }.onFailure {
                _messageEvents.emit("Failed to create file: ${it.localizedMessage}")
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = repository.renameFile(item.file, newName)
            result.onSuccess {
                repository.invalidateCaches()
                _messageEvents.emit("Renamed to \"$newName\"")
                loadCurrentDirectory()
            }.onFailure {
                _messageEvents.emit("Failed to rename: ${it.localizedMessage}")
            }
        }
    }

    // --- Multi-Select Mode Operations ---

    fun enterMultiSelect(path: String) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedItems = setOf(path)
            )
        }
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val updated = state.selectedItems.toMutableSet()
            if (updated.contains(path)) {
                updated.remove(path)
            } else {
                updated.add(path)
            }
            state.copy(
                selectedItems = updated,
                isMultiSelectMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        val state = _uiState.value
        val currentItems = if (isShowingSearchResults(state)) state.searchResults else state.items
        val allPaths = currentItems.map { it.path }.toSet()
        _uiState.update {
            it.copy(
                selectedItems = allPaths,
                isMultiSelectMode = allPaths.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedItems = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun getSelectedFileItems(): List<FileItem> {
        val state = _uiState.value
        val selectedPaths = state.selectedItems
        val currentItems = if (isShowingSearchResults(state)) state.searchResults else state.items
        return currentItems.filter { selectedPaths.contains(it.path) }
    }

    // --- Copy / Cut / Paste Operations ---

    fun copyFiles(items: List<FileItem>) {
        if (items.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = items, isCut = false),
                isMultiSelectMode = false,
                selectedItems = emptySet()
            )
        }
        viewModelScope.launch {
            _messageEvents.emit("Copied ${items.size} item(s) to clipboard")
        }
    }

    fun cutFiles(items: List<FileItem>) {
        if (items.isEmpty()) return
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(files = items, isCut = true),
                isMultiSelectMode = false,
                selectedItems = emptySet()
            )
        }
        viewModelScope.launch {
            _messageEvents.emit("Cut ${items.size} item(s)")
        }
    }

    fun copySelected() {
        val items = getSelectedFileItems()
        copyFiles(items)
    }

    fun cutSelected() {
        val items = getSelectedFileItems()
        cutFiles(items)
    }

    fun pasteFiles(destination: File) {
        val clipboard = _uiState.value.clipboard ?: return
        if (clipboard.files.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            val failedItems = mutableListOf<FileItem>()

            for (item in clipboard.files) {
                val result = if (clipboard.isCut) {
                    repository.moveFileOrDirectory(item.file, destination)
                } else {
                    repository.copyFileOrDirectory(item.file, destination)
                }
                if (result.isSuccess) successCount++ else failedItems.add(item)
            }

            repository.invalidateCaches()

            if (clipboard.isCut) {
                // Keep the clipboard populated with only the items that failed, so the
                // user can retry instead of losing track of them.
                _uiState.update {
                    it.copy(
                        clipboard = if (failedItems.isEmpty()) {
                            null
                        } else {
                            ClipboardState(files = failedItems, isCut = true)
                        }
                    )
                }
            }

            val errorCount = failedItems.size
            val msg = if (errorCount == 0) {
                "Successfully ${if (clipboard.isCut) "moved" else "copied"} $successCount item(s)"
            } else {
                "${if (clipboard.isCut) "Moved" else "Copied"} $successCount item(s), $errorCount failed"
            }
            _messageEvents.emit(msg)
            loadCurrentDirectory()
            refreshStorageStats()
            refreshBreakdownIfLoaded()
            refreshMediaIfLoaded()
        }
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null) }
    }

    // --- Trash / Recycle Bin Operations ---

    fun loadTrash() {
        viewModelScope.launch {
            val items = trashRepository.getTrashItems()
            _uiState.update { it.copy(trashItems = items) }
        }
    }

    fun deleteFile(item: FileItem) {
        viewModelScope.launch {
            val result = trashRepository.moveToTrash(item.file)
            result.onSuccess {
                repository.invalidateCaches()
                _messageEvents.emit("Moved \"${item.name}\" to Recycle Bin")
                loadCurrentDirectory()
                refreshStorageStats()
                loadTrash()
                refreshBreakdownIfLoaded()
                refreshMediaIfLoaded()
            }.onFailure {
                _messageEvents.emit("Failed to move to Recycle Bin: ${it.localizedMessage}")
            }
        }
    }

    fun deleteSelected() {
        val items = getSelectedFileItems()
        if (items.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            for (item in items) {
                val res = trashRepository.moveToTrash(item.file)
                if (res.isSuccess) count++
            }
            repository.invalidateCaches()
            clearSelection()
            _messageEvents.emit("Moved $count item(s) to Recycle Bin")
            loadCurrentDirectory()
            refreshStorageStats()
            loadTrash()
            refreshBreakdownIfLoaded()
            refreshMediaIfLoaded()
        }
    }

    fun restoreTrashItem(item: TrashItem) {
        viewModelScope.launch {
            val result = trashRepository.restoreItem(item)
            result.onSuccess { restoredFile ->
                repository.invalidateCaches()
                _messageEvents.emit("Restored \"${item.name}\"")
                loadTrash()
                loadCurrentDirectory()
                refreshStorageStats()
                refreshBreakdownIfLoaded()
                refreshMediaIfLoaded()
            }.onFailure {
                _messageEvents.emit("Failed to restore: ${it.localizedMessage}")
            }
        }
    }

    fun deletePermanently(item: TrashItem) {
        viewModelScope.launch {
            val result = trashRepository.deletePermanently(item)
            result.onSuccess {
                repository.invalidateCaches()
                _messageEvents.emit("Permanently deleted \"${item.name}\"")
                loadTrash()
                refreshStorageStats()
                refreshBreakdownIfLoaded()
                refreshMediaIfLoaded()
            }.onFailure {
                _messageEvents.emit("Failed to delete permanently: ${it.localizedMessage}")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val result = trashRepository.emptyTrash()
            result.onSuccess {
                repository.invalidateCaches()
                _messageEvents.emit("Recycle Bin emptied")
                loadTrash()
                refreshStorageStats()
                refreshBreakdownIfLoaded()
                refreshMediaIfLoaded()
            }.onFailure {
                _messageEvents.emit("Failed to empty Recycle Bin: ${it.localizedMessage}")
            }
        }
    }

    // --- Bookmarks / Favorites Operations ---

    fun loadBookmarks() {
        val bookmarks = bookmarkRepository.getBookmarks()
        _uiState.update { it.copy(bookmarks = bookmarks) }
    }

    fun toggleBookmark(path: String) {
        val isBookmarked = bookmarkRepository.toggleBookmark(path)
        loadBookmarks()
        viewModelScope.launch {
            val name = File(path).name
            val msg = if (isBookmarked) "Added \"$name\" to Favorites" else "Removed \"$name\" from Favorites"
            _messageEvents.emit(msg)
        }
    }

    fun isBookmarked(path: String): Boolean {
        return bookmarkRepository.isBookmarked(path)
    }

    fun getBookmarks(): List<String> {
        return bookmarkRepository.getBookmarks()
    }

    fun removeBookmark(path: String) {
        bookmarkRepository.removeBookmark(path)
        loadBookmarks()
        viewModelScope.launch {
            _messageEvents.emit("Removed from Favorites")
        }
    }

    // --- Recent Files Operations ---

    fun recordRecentFile(file: File) {
        if (!file.exists() || file.isDirectory) return
        recentFilesRepository.addRecentFile(file)
        loadRecentFiles()
    }

    fun loadRecentFiles() {
        val recents = recentFilesRepository.getRecentFiles()
        _uiState.update { it.copy(recentFiles = recents) }
    }

    fun getRecentFiles(): List<FileItem> {
        return recentFilesRepository.getRecentFiles()
    }

    fun clearRecentFiles() {
        recentFilesRepository.clearRecents()
        _uiState.update { it.copy(recentFiles = emptyList()) }
        viewModelScope.launch {
            _messageEvents.emit("Recent files cleared")
        }
    }
}
