package com.antigravity.filemanager.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.antigravity.filemanager.data.SortOption
import com.antigravity.filemanager.model.FileItem
import com.antigravity.filemanager.ui.components.*
import com.antigravity.filemanager.ui.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFileScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Intercept back button to navigate up folders
    BackHandler(enabled = true) {
        val handled = viewModel.navigateUp()
        if (!handled) {
            // Exit activity or default back behavior
            (context as? android.app.Activity)?.finish()
        }
    }

    // Collect snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.messageEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSearching) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Search files...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = if (uiState.selectedCategory != null) {
                                uiState.selectedCategory!!.name.lowercase().replaceFirstChar { it.uppercase() }
                            } else {
                                "Antigravity Files"
                            }
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.isSearching) {
                        IconButton(onClick = viewModel::closeSearch) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                        }
                    } else if (uiState.currentDirectory != viewModel.rootDirectory || uiState.selectedCategory != null) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!uiState.isSearching) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged(" ") }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }

                    IconButton(onClick = viewModel::toggleGridView) {
                        Icon(
                            imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View"
                        )
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(SortOption.NAME_ASC)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z to A)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(SortOption.NAME_DESC)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Newest first)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(SortOption.DATE_NEWEST)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest first)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(SortOption.DATE_OLDEST)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Largest first)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(SortOption.SIZE_LARGEST)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Size (Smallest first)") },
                                onClick = {
                                    showSortMenu = false
                                    viewModel.setSortOption(SortOption.SIZE_SMALLEST)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedCategory == null && !uiState.isSearching) {
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Show Storage card only on Root folder without category
            if (uiState.currentDirectory == viewModel.rootDirectory && uiState.selectedCategory == null && !uiState.isSearching) {
                StorageCard(storageStats = uiState.storageStats)
            }

            // Category filter chips
            if (!uiState.isSearching) {
                CategoryChips(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = viewModel::selectCategory
                )
            }

            // Breadcrumbs navigation
            if (uiState.selectedCategory == null && !uiState.isSearching) {
                BreadcrumbsBar(
                    rootDirectory = viewModel.rootDirectory,
                    currentDirectory = uiState.currentDirectory,
                    onNavigateTo = viewModel::navigateTo
                )
            }

            val displayItems = if (uiState.isSearching) uiState.searchResults else uiState.items

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.isSearching) "No files match your search" else "This folder is empty",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (uiState.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayItems, key = { it.path }) { item ->
                            FileGridItem(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        viewModel.navigateTo(item.file)
                                    } else {
                                        openFile(context, item.file)
                                    }
                                },
                                onLongClick = { itemForDetails = item }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(displayItems, key = { it.path }) { item ->
                            FileListItem(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        viewModel.navigateTo(item.file)
                                    } else {
                                        openFile(context, item.file)
                                    }
                                },
                                onRename = { itemToRename = item },
                                onDelete = { itemToDelete = item },
                                onDetails = { itemForDetails = item }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                showCreateFolderDialog = false
                viewModel.createFolder(name)
            }
        )
    }

    itemToRename?.let { item ->
        RenameDialog(
            item = item,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                itemToRename = null
                viewModel.renameFile(item, newName)
            }
        )
    }

    itemToDelete?.let { item ->
        DeleteConfirmDialog(
            item = item,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                itemToDelete = null
                viewModel.deleteFile(item)
            }
        )
    }

    itemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { itemForDetails = null }
        )
    }
}

private fun openFile(context: Context, file: File) {
    try {
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        val uri: Uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}
