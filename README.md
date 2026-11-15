# 🚀 Antigravity Files (Android File Manager)

A modern, fast, and feature-rich **Native Android File Manager** built with **Kotlin** and **Jetpack Compose (Material 3)**.

---

## ✨ Features (v2.0.0)

- **🎨 Modern Material 3 UI:** Clean interface with dynamic theming and full Dark/Light/System mode support.
- **🖼️ Coil Async Thumbnails:** Smooth asynchronous thumbnail loading for images and video previews with play overlay.
- **☑️ Multi-Select Batch Operations:** Long-press to enter multi-select mode. Batch copy, cut/move, share, and delete.
- **📋 Copy / Cut / Paste:** Global clipboard state with paste button, count badge, and folder traversal.
- **📤 Native File Sharing:** Share single or multiple files seamlessly via Android `FileProvider`.
- **⭐ Favorites & Bookmarks:** Bookmark frequently used folders for instant access via dedicated Favorites screen.
- **🕐 Recent Files:** Automatic tracking of recently opened files with thumbnail previews and direct navigation.
- **🗑️ Safe Recycle Bin (Trash):** Two-step safe deletion into `.Trash` with single-click restore and permanent purge.
- **📊 Storage Meter & Category Breakdown:** Storage overview plus visual category distribution bar (Images, Videos, Audio, Docs, APKs, etc.).
- **📝 Built-in Text File Viewer:** Monospace scrollable viewer for text, code, markdown, logs, and config files with quick copy.
- **⚙️ Preferences & Customization:** Dark/Light/System theme switch, hidden files toggle, and default sort configuration.
- **📂 Modal Navigation Drawer:** Clean navigation between Storage, Favorites, Recents, Recycle Bin, and Settings.
- **📦 APK Quick Installer:** Direct APK inspection dialog with package details and 1-tap installation intent.
- **🔗 Copy File Path:** Quick copy absolute path to Android system clipboard.
- **💾 Preference Persistence:** Auto-saved view modes (grid/list), sorting criteria, and theme settings via SharedPreferences.
- **📄 Quick File Creation:** Instant empty text file and folder creation shortcut via FAB.
- **🧭 Breadcrumbs Navigation:** Interactive clickable path bar for effortless directory traversal.
- **🔍 Instant Search:** Fast search across folder hierarchies.
- **🔒 Modern Permissions:** Handles Android 11+ (API 30+) `MANAGE_EXTERNAL_STORAGE` seamlessly with an intuitive permission screen.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin (1.9.24)
- **UI Framework:** Jetpack Compose (Material 3, BOM 2024.06.00)
- **Image Loading:** Coil Compose (2.6.0)
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Minimum SDK:** Android 8.0 (API 26)
- **Target / Compile SDK:** Android 14 (API 34)
- **CI/CD:** Automated APK compilation & GitHub Release via **GitHub Actions**

---

## 🤖 Automated Cloud Build (GitHub Actions)

This project is configured with GitHub Actions (`.github/workflows/build-apk.yml`). Every time you push to `main`:
1. GitHub spins up a Linux VM with JDK 17 & Gradle 8.7.
2. Compiles the native Android application.
3. Packages the APK (`Antigravity-Files-v2.0.apk`).
4. Generates a downloadable **GitHub Artifact** and **GitHub Release (v2.0.0)**!
