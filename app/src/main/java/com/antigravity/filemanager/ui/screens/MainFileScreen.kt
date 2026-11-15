package com.antigravity.filemanager.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.antigravity.filemanager.ui.theme.FolderYellow
import com.antigravity.filemanager.ui.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayList

enum class AppScreen {
    FILES,
    BOOKMARKS,
    RECENT_FILES,
    TRASH,
    SETTINGS,
    TEXT_VIEWER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFileScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(AppScreen.FILES) }
    var textViewerPath by remember { mutableStateOf<String?>(null) }
    var apkToInstall by remember { mutableStateOf<FileItem?>(null) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Intercept back button
    BackHandler(enabled = true) {
        when {
            drawerState.isOpen -> {
                coroutineScope.launch { drawerState.close() }
            }
            uiState.isMultiSelectMode -> {
                viewModel.clearSelection()
            }
            currentScreen != AppScreen.FILES -> {
                currentScreen = AppScreen.FILES
            }
            else -> {
                val handled = viewModel.navigateUp()
                if (!handled) {
                    (context as? android.app.Activity)?.finish()
                }
            }
        }
    }

    // Collect snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.messageEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun handleItemClick(item: FileItem) {
        if (item.isDirectory) {
            viewModel.navigateTo(item.file)
        } else if (item.isTextFile) {
            viewModel.recordRecentFile(item.file)
            textViewerPath = item.path
            currentScreen = AppScreen.TEXT_VIEWER
        } else if (item.isApkFile) {
            viewModel.recordRecentFile(item.file)
            apkToInstall = item
        } else {
            viewModel.recordRecentFile(item.file)
            openFileWithIntent(context, item.file)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == AppScreen.FILES && !uiState.isMultiSelectMode,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Antigravity Files",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "v2.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Internal Storage") },
                    icon = { Icon(Icons.Default.SdStorage, contentDescription = null) },
                    selected = currentScreen == AppScreen.FILES && uiState.currentDirectory == viewModel.rootDirectory,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        currentScreen = AppScreen.FILES
                        viewModel.navigateTo(viewModel.rootDirectory)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Favorites") },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = FolderYellow) },
                    selected = currentScreen == AppScreen.BOOKMARKS,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.loadBookmarks()
                        currentScreen = AppScreen.BOOKMARKS
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Recent Files") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    selected = currentScreen == AppScreen.RECENT_FILES,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.loadRecentFiles()
                        currentScreen = AppScreen.RECENT_FILES
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Recycle Bin") },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    selected = currentScreen == AppScreen.TRASH,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.loadTrash()
                        currentScreen = AppScreen.TRASH
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        currentScreen = AppScreen.SETTINGS
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        when (currentScreen) {
            AppScreen.BOOKMARKS -> {
                BookmarksScreen(
                    bookmarks = uiState.bookmarks,
                    onNavigateTo = { dir ->
                        currentScreen = AppScreen.FILES
                        viewModel.navigateTo(dir)
                    },
                    onRemoveBookmark = viewModel::removeBookmark,
                    onBack = { currentScreen = AppScreen.FILES }
                )
            }
            AppScreen.RECENT_FILES -> {
                RecentFilesScreen(
                    recentFiles = uiState.recentFiles,
                    onFileClick = { item ->
                        item.file.parentFile?.let { parent ->
                            viewModel.navigateTo(parent)
                        }
                        currentScreen = AppScreen.FILES
                        handleItemClick(item)
                    },
                    onClearRecents = viewModel::clearRecentFiles,
                    onBack = { currentScreen = AppScreen.FILES }
                )
            }
            AppScreen.TRASH -> {
                TrashScreen(
                    trashItems = uiState.trashItems,
                    onRestore = viewModel::restoreTrashItem,
                    onDeletePermanently = viewModel::deletePermanently,
                    onEmptyTrash = viewModel::emptyTrash,
                    onBack = { currentScreen = AppScreen.FILES }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.FILES }
                )
            }
            AppScreen.TEXT_VIEWER -> {
                TextViewerScreen(
                    filePath = textViewerPath ?: "",
                    onBack = { currentScreen = AppScreen.FILES }
                )
            }
            AppScreen.FILES -> {
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                if (uiState.isMultiSelectMode) {
                                    Text("${uiState.selectedItems.size} selected")
                                } else if (uiState.isSearching) {
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
                                if (uiState.isMultiSelectMode) {
                                    IconButton(onClick = viewModel::clearSelection) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                                    }
                                } else if (uiState.isSearching) {
                                    IconButton(onClick = viewModel::closeSearch) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                                    }
                                } else if (uiState.currentDirectory != viewModel.rootDirectory || uiState.selectedCategory != null) {
                                    IconButton(onClick = { viewModel.navigateUp() }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                } else {
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                                    }
                                }
                            },
                            actions = {
                                if (uiState.isMultiSelectMode) {
                                    TextButton(onClick = viewModel::selectAll) {
                                        Text("Select All")
                                    }
                                } else {
                                    // Paste action when clipboard has items
                                    if (uiState.clipboard != null) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text("${uiState.clipboard?.files?.size ?: 0}")
                                                }
                                            }
                                        ) {
                                            IconButton(onClick = { viewModel.pasteFiles(uiState.currentDirectory) }) {
                                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                            }
                                        }
                                        IconButton(onClick = viewModel::clearClipboard) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Clipboard")
                                        }
                                    }

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

                                    IconButton(onClick = { currentScreen = AppScreen.SETTINGS }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (uiState.isMultiSelectMode) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = viewModel::clearSelection) {
                                        Icon(Icons.Default.Close, contentDescription = "Deselect All")
                                    }
                                    IconButton(onClick = viewModel::copySelected) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                    IconButton(onClick = viewModel::cutSelected) {
                                        Icon(Icons.Default.ContentCut, contentDescription = "Move")
                                    }
                                    IconButton(onClick = { shareFiles(viewModel.getSelectedFileItems(), context) }) {
                                        Icon(Icons.Default.Share, contentDescription = "Share")
                                    }
                                    IconButton(onClick = { showDeleteSelectedDialog = true }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Paste action FAB if clipboard not empty
                            if (uiState.clipboard != null && !uiState.isMultiSelectMode) {
                                ExtendedFloatingActionButton(
                                    onClick = { viewModel.pasteFiles(uiState.currentDirectory) },
                                    icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                                    text = {
                                        val count = uiState.clipboard?.files?.size ?: 0
                                        val action = if (uiState.clipboard?.isCut == true) "Move" else "Paste"
                                        Text("$action ($count)")
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            }

                            if (uiState.selectedCategory == null && !uiState.isSearching && !uiState.isMultiSelectMode) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SmallFloatingActionButton(
                                        onClick = { showCreateFileDialog = true },
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(Icons.Default.NoteAdd, contentDescription = "New File")
                                    }

                                    FloatingActionButton(
                                        onClick = { showCreateFolderDialog = true },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Storage card & Breakdown on Root folder
                        if (uiState.currentDirectory == viewModel.rootDirectory && uiState.selectedCategory == null && !uiState.isSearching) {
                            StorageCard(storageStats = uiState.storageStats)
                            StorageBreakdownCard(
                                breakdown = uiState.categoryBreakdown,
                                isLoading = uiState.isBreakdownLoading
                            )
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
                                        val isSelected = uiState.selectedItems.contains(item.path)
                                        FileGridItem(
                                            item = item,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isSelected = isSelected,
                                            onClick = { handleItemClick(item) },
                                            onLongClick = {
                                                if (!uiState.isMultiSelectMode) {
                                                    viewModel.enterMultiSelect(item.path)
                                                } else {
                                                    viewModel.toggleSelection(item.path)
                                                }
                                            },
                                            onToggleSelect = { viewModel.toggleSelection(item.path) }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(displayItems, key = { it.path }) { item ->
                                        val isSelected = uiState.selectedItems.contains(item.path)
                                        val isBookmarked = viewModel.isBookmarked(item.path)
                                        FileListItem(
                                            item = item,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isSelected = isSelected,
                                            isBookmarked = isBookmarked,
                                            onClick = { handleItemClick(item) },
                                            onLongClick = {
                                                if (!uiState.isMultiSelectMode) {
                                                    viewModel.enterMultiSelect(item.path)
                                                } else {
                                                    viewModel.toggleSelection(item.path)
                                                }
                                            },
                                            onToggleSelect = { viewModel.toggleSelection(item.path) },
                                            onRename = { itemToRename = item },
                                            onDelete = { itemToDelete = item },
                                            onDetails = { itemForDetails = item },
                                            onCopyPath = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Path", item.path))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Path copied to clipboard")
                                                }
                                            },
                                            onShare = { shareFiles(listOf(item), context) },
                                            onCopy = { viewModel.copyFiles(listOf(item)) },
                                            onCut = { viewModel.cutFiles(listOf(item)) },
                                            onToggleBookmark = { viewModel.toggleBookmark(item.path) }
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

    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { name ->
                showCreateFileDialog = false
                viewModel.createFile(name)
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

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Move to Recycle Bin") },
            text = { Text("Move ${uiState.selectedItems.size} selected item(s) to Recycle Bin?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectedDialog = false
                        viewModel.deleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    itemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { itemForDetails = null }
        )
    }

    apkToInstall?.let { item ->
        ApkInstallDialog(
            item = item,
            onDismiss = { apkToInstall = null },
            onInstall = {
                apkToInstall = null
                installApk(context, item.file)
            }
        )
    }
}

fun shareFiles(items: List<FileItem>, context: Context) {
    try {
        val nonDirFiles = items.filter { !it.isDirectory && it.file.exists() }
        if (nonDirFiles.isEmpty()) {
            Toast.makeText(context, "Cannot share folders directly", Toast.LENGTH_SHORT).show()
            return
        }

        val authority = "${context.packageName}.provider"
        if (nonDirFiles.size == 1) {
            val file = nonDirFiles.first().file
            val uri = FileProvider.getUriForFile(context, authority, file)
            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
        } else {
            val uris = ArrayList<Uri>()
            for (f in nonDirFiles) {
                uris.add(FileProvider.getUriForFile(context, authority, f.file))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${nonDirFiles.size} files"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share files: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun installApk(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to launch installer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun openFileWithIntent(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.provider"
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
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
