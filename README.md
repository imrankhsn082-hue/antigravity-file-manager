# 🚀 Antigravity Files (Android File Manager)

A modern, fast, and elegant **Native Android File Manager** built with **Kotlin** and **Jetpack Compose (Material 3)**.

---

## ✨ Features

- **🎨 Modern Material 3 UI:** Clean interface with dynamic theming and full Dark/Light mode support.
- **📊 Storage Overview:** Real-time visual storage meter showing used and free disk space.
- **🏷️ Category Filtering:** Instant 1-tap filtering for:
  - 📷 Images
  - 🎬 Videos
  - 🎵 Audio
  - 📄 Documents
  - 📦 APKs
  - 🗜️ Archives
- **🧭 Breadcrumbs Navigation:** Interactive clickable path bar for effortless directory traversal.
- **📁 Full File Operations:**
  - Create new folders
  - Rename files & folders
  - Delete files/folders (recursive directory deletion)
  - Inspect file metadata (Path, Size, Type, Last Modified)
- **🔍 Instant Search:** Fast search for files and folders.
- **🔀 Flexible Sorting:** Sort by Name (A-Z, Z-A), Date Modified, or File Size.
- **📱 Grid & List Layouts:** Switch seamlessly between compact list view and card grid view.
- **🔒 Modern Permissions:** Handles Android 11+ (API 30+) `MANAGE_EXTERNAL_STORAGE` seamlessly with an intuitive permission onboarding screen.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin (1.9.24)
- **UI Framework:** Jetpack Compose (Material 3, BOM 2024.06.00)
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow
- **Minimum SDK:** Android 8.0 (API 26)
- **Target / Compile SDK:** Android 14 (API 34)
- **CI/CD:** Automated APK compilation & GitHub Release via **GitHub Actions**

---

## 🤖 Automated Cloud Build (GitHub Actions)

This project is configured with GitHub Actions (`.github/workflows/build-apk.yml`). Every time you push to `main`:
1. GitHub spins up a 16GB Linux VM with JDK 17 & Gradle 8.7.
2. Compiles the native Android application.
3. Packages the APK (`Antigravity-Files-v1.0.apk`).
4. Generates a downloadable **GitHub Artifact** and **GitHub Release**!
