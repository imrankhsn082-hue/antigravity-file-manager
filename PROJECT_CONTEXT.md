# 🤖 AI Assistant Context & Project Architecture Guide

> [!IMPORTANT]
> **MANDATORY INSTRUCTION FOR ALL FUTURE AI AGENTS & DEVELOPERS:**
> Every time you modify this codebase, add a new feature, refactor existing code, or update dependencies, you **MUST immediately update this `PROJECT_CONTEXT.md` file**.
> Ensure the **Feature Catalog**, **File Structure**, **Version Log**, and **Changelog** reflect all updates before finishing your task.

---

## 📌 Project Overview

- **App Name:** Antigravity Files (Android File Manager)
- **Package Name:** `com.antigravity.filemanager`
- **Current Version:** `2.5.0` (versionCode: `7`)
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
│   ├── proguard-rules.pro             # R8/ProGuard keep rules for the release build
│   └── src/
│       ├── test/java/...              # JVM unit tests (FileItem formatting & type detection)
│       └── main/
│           ├── AndroidManifest.xml    # App permissions, activities, FileProvider
│           ├── java/com/antigravity/filemanager/
│           │   ├── MainActivity.kt    # Root activity, permission handling, theme wrapper
│           │   ├── AntigravityApp.kt  # Application + global Coil image loader (video frame decoder)
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
│           │   │   │   ├── HomeDashboard.kt       # Home: media stories row, category grid, collections, storage tiles
│           │   │   │   ├── PermissionScreen.kt    # Android 11+ storage permission onboarding
│           │   │   │   ├── StorageBreakdownCard.kt# Proportional visual category size distribution bar
│           │   │   │   ├── StorageCard.kt         # Internal storage usage overview card
│           │   │   │   └── ViewerChrome.kt        # Shared viewer top bar, overflow menu, share/print helpers
│           │   │   ├── screens/
│           │   │   │   ├── MainFileScreen.kt      # Primary dashboard, Drawer, Multi-select bar, FABs, file routing
│           │   │   │   ├── ImageViewerScreen.kt   # Full screen image viewer (pinch zoom, pan, rotate, edit, print)
│           │   │   │   ├── AudioPlayerScreen.kt   # In-app audio player (artwork, playlist, shuffle, repeat, speed)
│           │   │   │   ├── VideoPlayerScreen.kt   # In-app video player (custom controls, ±10s, rotate, speed, volume boost, audio & subtitle tracks)
│           │   │   │   ├── PdfViewerScreen.kt     # In-app PDF reader (PdfRenderer page bitmaps)
│           │   │   │   ├── BookmarksScreen.kt     # Starred folders & files screen
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
│               ├── values-night/              # Dark launch theme (prevents white flash before Compose starts)
│               └── xml/
│                   └── file_provider_paths.xml# Storage paths for secure file sharing
├── build.gradle.kts                   # Root build script
├── settings.gradle.kts                # Plugin & repo resolution management
├── gradle.properties                  # JVM args & AndroidX flags
├── gradlew / gradlew.bat              # Gradle wrapper scripts (local builds)
├── gradle/wrapper/                    # Wrapper JAR + distribution properties (Gradle 8.7)
├── PROJECT_CONTEXT.md                 # THIS FILE (AI Assistant architecture guide)
└── README.md                          # User-facing project documentation
```

---

## 🌟 Comprehensive Feature Catalog

### 1. 🖼️ Async Image & Video Thumbnails (Coil 2.6.0)
- Uses `io.coil-kt:coil-compose` for memory-efficient async thumbnail caching.
- Shows real image previews for JPEG, PNG, WEBP, GIF, SVG.
- Video files display real frame previews via `io.coil-kt:coil-video` (`VideoFrameDecoder`), registered globally in `AntigravityApp` (`ImageLoaderFactory`), with an overlay play badge.
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
- The About section reads the version, package and Android platform from `BuildConfig`/`Build.VERSION` at runtime.

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

### 17. 🏠 Stories-Style Home Dashboard (v2.3.0)
- The app opens on a dedicated dashboard (`showDashboard` state in `MainFileScreen`) instead of the raw folder list.
- **Recent media stories row**: horizontally scrolling 166×218dp rounded cards built from `repository.getRecentMedia()` (scans `DCIM`, `Pictures`, `Screenshots`, `Download`, `Movies`; max 3 levels / 4000 entries, sorted newest first). Cards show a cover thumbnail, filename, parent folder or a **Downloaded** badge, a per-card play badge for videos and a 3-dot menu.
- **Categories grid**: two-column tiles for Downloads, Images, Videos, Audio, Documents and Apps with live sizes from `CategoryBreakdown` (now includes `downloadsSize`). Media/Apps tiles open the category filter, Downloads opens the `Download` folder.
- **Collections**: Starred (Favorites screen) and Recycle Bin.
- **All storage**: Internal storage (free space, opens the folder list) and Other storage (system + other apps).
- Centered blue pill FAB with **New folder** and **New file** actions; `HomeHeader` offers menu, search and settings.
- The folder list itself keeps a top bar with back-to-home, grid/list toggle, sort menu and overflow menu (search, paste, settings).

### 18. 🎬 Built-in Viewers: Image, Audio, Video, PDF & Text (v2.4.0)
- **Image viewer** (`ImageViewerScreen`): full-screen, pinch-to-zoom (1x–6x), pan, double-tap zoom, 90° rotation, blue action pill (Edit via system editor, Rotate), and the viewer top bar with share, favorite and the overflow menu (Use as, Open with, Move to Trash, Move to, Copy to, File info, Print).
- **Audio player** (`AudioPlayerScreen`, Media3 ExoPlayer): embedded album art (with music-note placeholder), file name, seek bar with elapsed/total time, shuffle, previous, play/pause, next, repeat (off/all/one) and playback speed. All audio files of the same folder become the playlist.
- **Video player** (`VideoPlayerScreen`, Media3 ExoPlayer + `PlayerView`): custom control bar with seek, elapsed/total time, -10s / +10s, play/pause, screen rotate toggle, buffering spinner, tap-to-toggle chrome, keep-screen-on, and playback speed in the overflow menu.
- **Sound boost** (v2.5.0): a separate volume slider (0–200%) under the speaker button. 0–100% drives `ExoPlayer.volume`; above 100% a `LoudnessEnhancer` (up to +1200 mB) lifts quiet movies/tracks without touching the system volume. The enhanced session is tracked through `player.audioSessionId` and released on dispose.
- **Dual audio switching** (v2.5.0): every supported audio track of the current video is listed (label → language → track number) behind the headphones button and in the overflow menu; selecting one applies a `TrackSelectionOverride`, "Automatic" clears the override. Videos with a single track show a toast instead of an empty menu.
- **Subtitles** (v2.5.0): embedded subtitle tracks are listed behind the CC button with an "Off" entry that disables `C.TRACK_TYPE_TEXT`; the overflow menu cycles Off → track 1 → track 2 → Off. If the file has no embedded subtitle, the menu offers **"Add subtitle file…"**, which opens the system document picker and attaches an external `.srt` / `.vtt` / `.ass` / `.ssa` / `.ttml` file as a `SubtitleConfiguration` (MIME type detected from the file extension), reloading the media item at the current position. A **"Remove added subtitle"** action detaches it again.
- **PDF viewer** (`PdfViewerScreen`): renders every page with the platform `PdfRenderer` behind a mutex into white-bitmap images listed in a `LazyColumn`; shows the page count, handles password-protected/corrupt files gracefully, and supports print.
- **Text viewer** upgraded to the shared chrome: copy-all, share, favorite, Open with, Move/Copy to, File info.
- **Routing**: tapping a file in the list, recents, favorites or the home carousel opens the right viewer (`IMAGE`, `VIDEO`, `AUDIO`, `.pdf`, text) automatically; anything else falls back to the system "Open with" chooser.
- **Shared helpers** (`ViewerChrome.kt`): `ViewerTopBar`, `ViewerMenuAction`, `formatPlaybackTime`, `shareFileExternally`, `openWithExternally`, `useImageAs`, `loadDownsampledBitmap`, `printBitmap` / `printFile` (androidx.print).
- **File favorites**: files can now be starred from the list overflow or from any viewer, and `BookmarksScreen` opens file entries in their viewer instead of trying to navigate into them.

---

## 🔒 Permissions Handled

| Permission | API Level | Purpose |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` | Android 11+ (API 30+) | Full filesystem access (All Files Access) |
| `READ_EXTERNAL_STORAGE` | API 26 - 32 | Read files on legacy Android versions |
| `WRITE_EXTERNAL_STORAGE` | API 26 - 29 | Write/Modify files on legacy Android versions |
| `REQUEST_INSTALL_PACKAGES`| API 26+ | Ability to trigger package installation for APK files |
| `MODIFY_AUDIO_SETTINGS` | API 26+ | Attach the `LoudnessEnhancer` audio effect used by the video player sound boost |

---

## 🔄 Version Changelog

### v2.5.0 (Current)
- **Video player audio & subtitle upgrade**:
  - **Separate sound boost bar**: speaker button toggles a 0–200% volume slider. Up to 100% it drives the player volume; above 100% an `LoudnessEnhancer` adds up to +1200 mB gain for quiet videos, then the effect is released on dispose. Requires `MODIFY_AUDIO_SETTINGS`.
  - **Dual / multi audio track switching**: a headphones button (and the overflow menu) lists every supported audio track with an "Automatic" reset option, applied through `TrackSelectionOverride`.
  - **Subtitle select / change / off**: a CC button lists the embedded subtitle tracks plus "Off", and the overflow menu cycles through them.
  - **External subtitle files**: "Add subtitle file…" picks an `.srt` / `.vtt` / `.ass` / `.ssa` / `.ttml` document, detects its MIME type and attaches it as a `MediaItem.SubtitleConfiguration` while keeping the current playback position; "Remove added subtitle" detaches it.

### v2.4.0
- **Built-in viewers** for every common file type, all sharing the reference top bar (back, share, favorite, 3-dot menu):
  - **Images** (`ImageViewerScreen`): pinch zoom, pan, double-tap zoom, rotate, Edit, Print, Use as.
  - **Audio** (`AudioPlayerScreen`): ExoPlayer playlist of the folder, embedded album art, seek bar with times, shuffle/prev/play/next/repeat, playback speed.
  - **Video** (`VideoPlayerScreen`): custom controls with seek timeline, ±10 seconds, play/pause, rotate, buffering indicator, playback speed.
  - **PDF** (`PdfViewerScreen`): platform `PdfRenderer` pages in a scrollable list, page count, print, graceful error for locked/corrupt files.
  - **Text/code** (`TextViewerScreen`): restyled with the shared chrome and copy-all.
- **Overflow menu everywhere**: Use as, Open with, Move to Trash, Move to, Copy to, File info, Print, Playback speed (where relevant).
- **Automatic routing** from the file list, recents, favorites and the home media carousel; unknown types still fall back to the system "Open with" chooser.
- **File-level favorites**: files can be starred from the list or any viewer and are opened from the Favorites screen.
- New dependencies: `androidx.media3:media3-exoplayer` + `media3-ui` (1.3.1) and `androidx.print:print` (1.0.0).

### v2.3.0
- **New home dashboard** modelled on the reference design: stories-style recent media carousel (`HomeDashboard.kt`), two-column Categories grid (Downloads / Images / Videos / Audio / Documents / Apps with live sizes), Collections (Starred, Recycle Bin) and an All storage section (Internal storage, Other storage).
- **Centered action pill** on the home screen with New folder / New file, plus a minimal home header (menu, search, settings).
- **Redesigned folder list** (`FileListItem`): 56dp rounded-square icon/thumbnail tiles, bold filenames and relative timestamps ("34 minutes ago", "Aug 5") via `FileItem.relativeDate`; top bar with back-to-home, grid/list, sort and overflow menus.
- **Real video thumbnails** everywhere via `coil-video` (`VideoFrameDecoder`) registered in the new `AntigravityApp` application class.
- **New data**: `FileManagerRepository.getRecentMedia()`, downloads-size tracking in `CategoryBreakdown.downloadsSize`, `FolderLavender` accent, and `recentMedia` state cached in `FileManagerViewModel` (`ensureRecentMediaLoaded` / `refreshMediaIfLoaded`).
- Navigation: Home ⇄ folder list is a `showDashboard` flag; system back returns home before exiting.

### v2.2.0
- **Fixed search**: the toolbar search button now opens the search field (`openSearch`/`closeSearch`), queries are debounced (250 ms) and stale results are discarded, and hidden-file settings are respected.
- **Version consistency**: version/package/platform are read from `BuildConfig` and `Build.VERSION` instead of hardcoded UI strings.
- **CI hardening**: `build-apk.yml` derives the version from `versionName`, names/attaches the APK dynamically, and fails when the release asset is missing.
- **Performance**: the category breakdown is loaded lazily (no full-storage scan at startup), repository scans are cached for 2 minutes and invalidated on every mutation, folder child counts are lazy, and bookmark lookups no longer touch disk per row.
- **File operation safety**: copy/move rejects a folder targeting itself or its own subtree; failed cut items stay on the clipboard for retry; the breakdown refreshes after delete/restore/empty-trash.
- **Hardening**: FileProvider no longer exposes `root-path`; added `app/proguard-rules.pro`; added Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`); added a dark launch theme (`values-night`) and removed unused colors.
- Process-death resilience: the active screen and text-viewer path use `rememberSaveable`.
- Added JVM unit tests (`app/src/test`) for size formatting and file type detection, run by CI before packaging.

### v2.1.0
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
