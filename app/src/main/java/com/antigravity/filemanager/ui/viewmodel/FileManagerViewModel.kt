package com.antigravity.filemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.filemanager.data.FileManagerRepository
import com.antigravity.filemanager.data.SortOption
import com.antigravity.filemanager.model.FileItem
import com.antigravity.filemanager.model.FileType
import com.antigravity.filemanager.model.StorageStats
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class FileManagerUiState(
    val currentDirectory: File,
    val items: List<FileItem> = emptyList(),
    val storageStats: StorageStats? = null,
    val isLoading: Boolean = false,
    val selectedCategory: FileType? = null,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val showHiddenFiles: Boolean = false
)

class FileManagerViewModel(
    private val repository: FileManagerRepository = FileManagerRepository()
) : ViewModel() {

    val rootDirectory: File = repository.rootStorageDirectory

    private val _uiState = MutableStateFlow(
        FileManagerUiState(currentDirectory = rootDirectory)
    )
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    init {
        loadCurrentDirectory()
        refreshStorageStats()
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
                _uiState.update { it.copy(items = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _messageEvents.emit("Error loading files: ${e.localizedMessage}")
            }
        }
    }

    fun navigateTo(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        _uiState.update {
            it.copy(
                currentDirectory = directory,
                selectedCategory = null,
                searchQuery = "",
                isSearching = false
            )
        }
        loadCurrentDirectory()
    }

    fun navigateUp(): Boolean {
        val state = _uiState.value
        if (state.selectedCategory != null) {
            _uiState.update { it.copy(selectedCategory = null) }
            loadCurrentDirectory()
            return true
        }

        if (state.isSearching) {
            _uiState.update { it.copy(isSearching = false, searchQuery = "") }
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
        _uiState.update { it.copy(selectedCategory = category, searchQuery = "", isSearching = false) }
        loadCurrentDirectory()
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        loadCurrentDirectory()
    }

    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleShowHiddenFiles() {
        _uiState.update { it.copy(showHiddenFiles = !it.showHiddenFiles) }
        loadCurrentDirectory()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
        if (query.isNotBlank()) {
            viewModelScope.launch {
                val results = repository.searchFiles(_uiState.value.currentDirectory, query)
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun closeSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = repository.createFolder(_uiState.value.currentDirectory, name)
            result.onSuccess {
                _messageEvents.emit("Folder \"$name\" created")
                loadCurrentDirectory()
            }.onFailure {
                _messageEvents.emit("Failed to create folder: ${it.localizedMessage}")
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = repository.renameFile(item.file, newName)
            result.onSuccess {
                _messageEvents.emit("Renamed to \"$newName\"")
                loadCurrentDirectory()
            }.onFailure {
                _messageEvents.emit("Failed to rename: ${it.localizedMessage}")
            }
        }
    }

    fun deleteFile(item: FileItem) {
        viewModelScope.launch {
            val result = repository.deleteFile(item.file)
            result.onSuccess {
                _messageEvents.emit("Deleted \"${item.name}\"")
                loadCurrentDirectory()
                refreshStorageStats()
            }.onFailure {
                _messageEvents.emit("Failed to delete: ${it.localizedMessage}")
            }
        }
    }
}
