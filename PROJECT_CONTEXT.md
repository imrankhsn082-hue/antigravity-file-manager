# 🤖 AI Assistant Context & Project Architecture Guide

> [!IMPORTANT]
> **MANDATORY INSTRUCTION FOR ALL FUTURE AI AGENTS & DEVELOPERS:**
> Every time you modify this codebase, add a new feature, refactor existing code, or update dependencies, you **MUST immediately update this `PROJECT_CONTEXT.md` file**.
> Ensure the **Feature Catalog**, **File Structure**, **Version Log**, and **Changelog** reflect all updates before finishing your task.

---

## 📌 Project Overview

- **App Name:** Antigravity Files (Android File Manager)
- **Package Name:** `com.antigravity.filemanager`
- **Current Version:** `2.1.0` (versionCode: `3`)
- **Primary Language:** Kotlin (1.9.24)
- **UI Toolkit:** Jetpack Compose (Material 3, BOM `2024.06.00`)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target / Compile SDK:** 34 (Android 14)
- **Architecture:** Single-Activity MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Repository:** [https://github.com/imrankhsn082-hue/antigravity-file-manager](https://github.com/imrankhsn082-hue/antigravity-file-manager)

---

## 🏗️ Environment & Build Strategy

- **Development Environment:** Android Termux (Lightweight editing, git operations, CLI).
- **CI/CD & Compilation Engine:** **GitHub Actions** (`.github/workflows/build-apk.yml`).
  - Because building heavy Android SDK & Gradle projects locally on mobile can be slow or memory-constrained, all compilation is automated via GitHub Actions on a 16GB RAM Ubuntu cloud runner.
  - On every `git push` to `main`, GitHub Actions automatically:
    1. Sets up JDK 17 & Gradle 8.7.
    2. Compiles `gradle assembleDebug --stacktrace`.
    3. Packages the debug APK (`Antigravity-Files-vX.Y.apk`).
    4. Uploads the build artifact.
    5. Publishes a GitHub Release with the APK attached.

---

## 📂 Complete File Structure & Architecture Map

```
antigravity-file-manager/
├── .github/
│   └── workflows/
│       └── build-apk.yml              # Automated cloud compilation & release workflow
├── app/
│   ├── build.gradle.kts               # App module build config (Compose, Coil, SDK versions)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # App permissions, activities, FileProvider
│           ├── java/com/antigravity/filemanager/
│           │   ├── MainActivity.kt    # Root activity, permission handling, theme wrapper
│           │   ├── model/
│           │   │   ├── FileItem.kt    # Core file model, file types, formatters
│           │   │   ├── StorageStats.kt# Disk space usage calculations (total/used/free)
│           │   │   ├── TrashItem.kt   # Recycle bin item with original path & timestamp
│           │   │   └── CategoryBreakdown.kt # Size distribution across media types
│           │   ├── data/
│           │   │   ├── FileManagerRepository.kt # File I/O, search, sorting, recursive ops
│           │   │   ├── FileManagerPreferences.kt # SharedPreferences for user settings
│           │   │   ├── BookmarkRepository.kt    # Favorites persistence
│           │   │   ├── RecentFilesRepository.kt # History of 20 most recent files
│           │   │   └── TrashRepository.kt       # Recycle bin management in /.Trash/
│           │   ├── ui/
│           │   │   ├── components/
│           │   │   │   ├── BreadcrumbsBar.kt      # Clickable path navigation bar
│           │   │   │   ├── CategoryChips.kt       # Quick media filter chips
│           │   │   │   ├── FileActionDialogs.kt   # Dialogs (New Folder, New File, Rename, Delete, APK Install)
│           │   │   │   ├── FileListItem.kt        # List & Grid item layouts with thumbnails & 3-dot menus
│           │   │   │   ├── PermissionScreen.kt    # Android 11+ storage permission onboarding
│           │   │   │   ├── StorageBreakdownCard.kt# Proportional visual category size distribution bar
│           │   │   │   └── StorageCard.kt         # Internal storage usage overview card
│           │   │   ├── screens/
│           │   │   │   ├── MainFileScreen.kt      # Primary dashboard, Drawer, Multi-select bar, FABs
│           │   │   │   ├── BookmarksScreen.kt     # Starred/Favorited folders screen
│           │   │   │   ├── RecentFilesScreen.kt   # Recently accessed files list
│           │   │   │   ├── SettingsScreen.kt      # Theme toggle, sort order, hidden files preferences
│           │   │   │   ├── TextViewerScreen.kt    # Built-in monospace text & code viewer
│           │   │   │   └── TrashScreen.kt         # Recycle bin with restore & permanent delete
│           │   │   ├── theme/
│           │   │   │   ├── Color.kt               # Material 3 colors & category tints
│           │   │   │   ├── Theme.kt               # Light/Dark/Dynamic Material You theming
│           │   │   │   └── Type.kt                # Typography definitions
│           │   │   └── viewmodel/
│           │   │       └── FileManagerViewModel.kt# Central state container, clipboard, multi-select
│           └── res/
│               ├── mipmap-*/                  # Custom high-res app launcher icons (mdpi to xxxhdpi)
│               ├── mipmap-anydpi-v26/         # Adaptive launcher icon XMLs for Android 8.0+
│               ├── values/                    # strings.xml, colors.xml, themes.xml, ic_launcher_background.xml
│               └── xml/
│                   └── file_provider_paths.xml# Storage paths for secure file sharing
├── build.gradle.kts                   # Root build script
├── settings.gradle.kts                # Plugin & repo resolution management
├── gradle.properties                  # JVM args & AndroidX flags
├── PROJECT_CONTEXT.md                 # THIS FILE (AI Assistant architecture guide)
└── README.md                          # User-facing project documentation
```

---

## 🌟 Comprehensive Feature Catalog

### 1. 🖼️ Async Image & Video Thumbnails (Coil 2.6.0)
- Uses `io.coil-kt:coil-compose` for memory-efficient async thumbnail caching.
- Shows real image previews for JPEG, PNG, WEBP, GIF, SVG.
- Video files display previews with an overlay play badge.
- Color-coded icons serve as fallback for documents, code, archives, and APKs.

### 2. ☑️ Multi-Select Batch Operations
- Long-press any file or folder to enter contextual selection mode.
- Bottom action bar provides:
  - Selected count indicator
  - Select All / Deselect All
  - Batch Delete (moves to Trash)
  - Batch Share via `ACTION_SEND_MULTIPLE`
  - Batch Copy & Batch Cut/Move

### 3. ✂️ Full Clipboard (Copy, Cut, Paste)
- State tracked via `ClipboardState(files, isCut)`.
- Seamless cross-folder paste using recursive Java NIO / File operations.
- Dedicated Paste Extended FAB and TopAppBar badge counter showing items in queue.

### 4. 📤 Secure File Sharing (FileProvider)
- Implements `androidx.core.content.FileProvider` mapped in `res/xml/file_provider_paths.xml`.
- Share single or multiple files with any external Android app safely.

### 5. ⭐ Favorites & Bookmarks
- Star favorite folders to jump directly to deep storage paths.
- Stored persistently in `BookmarkRepository` using JSON via `SharedPreferences`.
- Dedicated `BookmarksScreen` accessible from Navigation Drawer.

### 6. 🕐 Recent Files Tracker
- Automatically logs the last 20 opened files (via `RecentFilesRepository`).
- Tapping a recent file navigates directly to its directory and opens it.

### 7. 🗑️ Recycle Bin / Trash System
- Safe deletion moves files to `/.Trash/` instead of irreversible removal.
- Includes metadata preservation in `TrashItem` (original path, deletion timestamp).
- Dedicated `TrashScreen` with **Restore**, **Delete Permanently**, and **Empty Trash** actions.

### 8. 📊 Storage Breakdown by Category
- Calculates disk usage distribution across Media categories (Images, Videos, Audio, Docs, APKs, Archives, Others).
- Visualized in `StorageBreakdownCard` via a proportional colored horizontal bar.

### 9. 📄 Built-in Monospace Text & Code Viewer
- Opens `.txt`, `.md`, `.json`, `.xml`, `.kt`, `.java`, `.py`, `.sh`, `.html`, `.css`, etc., natively inside `TextViewerScreen`.
- Features monospace typography, vertical/horizontal scrolling, and a **Copy All** action.

### 10. ⚙️ Settings & Dynamic Theme Engine
- Supports **Light Mode**, **Dark Mode**, and **System Default**.
- Persistent toggles for **Show Hidden Files** (`.files`) and default sort preferences.

### 11. 🗂️ Modal Navigation Drawer
- Left sliding drawer linking to:
  - 📁 Internal Storage (Home)
  - ⭐ Favorites / Bookmarks
  - 🕐 Recent Files
  - 🗑️ Recycle Bin
  - ⚙️ Settings

### 12. 📦 APK Quick Installer
- Dedicated dialog displaying APK name and size with a one-tap **Install** action.
- Configured with `REQUEST_INSTALL_PACKAGES` permission in `AndroidManifest.xml`.

### 13. 📋 Copy Path to Clipboard
- One-tap action in the item dropdown to copy absolute storage paths to Android's `ClipboardManager`.

### 14. 💾 Persistent Preferences
- User's selected sort criteria (Name, Date, Size) and layout (Grid vs List) persist across app launches via `FileManagerPreferences`.

### 15. 📝 Quick File & Folder Creation
- Dual FABs on the main screen to create either a new subfolder or an empty text file immediately.

### 16. 🎨 Custom Brand App Icon
- High-resolution 3D pearlescent folder and upward arrow icon.
- Scaled for all densities: `mdpi` (48px), `hdpi` (72px), `xhdpi` (96px), `xxhdpi` (144px), `xxxhdpi` (192px).
- Adaptive launcher icons (`anydpi-v26`) with separate foreground and background layers.

---

## 🔒 Permissions Handled

| Permission | API Level | Purpose |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` | Android 11+ (API 30+) | Full filesystem access (All Files Access) |
| `READ_EXTERNAL_STORAGE` | API 26 - 32 | Read files on legacy Android versions |
| `WRITE_EXTERNAL_STORAGE` | API 26 - 29 | Write/Modify files on legacy Android versions |
| `REQUEST_INSTALL_PACKAGES`| API 26+ | Ability to trigger package installation for APK files |

---

## 🔄 Version Changelog

### v2.1.0 (Current)
- Added custom high-resolution Material 3 brand app icon with adaptive mipmap layers.
- Fixed launcher icon integration in `AndroidManifest.xml`.
- Updated release pipeline to bundle `Antigravity-Files-v2.1.apk`.

### v2.0.0
- Added 15 major productivity features: Multi-select, Copy/Cut/Paste, Bookmarks, Recent files, Recycle Bin, Storage breakdown, Built-in text viewer, Settings, Navigation Drawer, APK installer, Copy path, and Coil thumbnails.

### v1.0.0
- Initial release: Core file browsing, breadcrumbs, search, sort, grid/list view, storage card, and GitHub Actions CI/CD setup.

---

> [!TIP]
> **REMINDER TO FUTURE AI AGENTS:** Always update this file whenever you alter the project!
