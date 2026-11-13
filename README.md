# Infinity Files

A lightweight, clutter-free Android directory browser built with Kotlin and Jetpack Compose.

## Features

- **Direct Storage Browsing:** Opens straight into accessible local storage partitions (Internal Storage, SD/External Volumes, System Root, App Workspace) without ads, search bars, or category clutter.
- **High-Contrast Minimalist Grid:** Slate and pure black Material 3 interface with custom black-filled, white-outlined folder and file icons.
- **Show / Hide Folders:** Toggle hidden folders (`dot` folders and user-hidden folders) from the top bar, or long-press any folder to hide/unhide it.
- **Image & Document Previews:** Instant image thumbnails, full-screen image viewer, in-app text/code viewer & editor, and external app opening via `FileProvider` for PDFs, Office documents, and media.
- **File Operations:** Create folders and files, rename, safe deletion, and a staged Copy/Move clipboard across directories.

## APK Download

A pre-built debug APK is included in the [`test/`](test/) directory:

- `test/InfinityFiles-debug.apk`

## Building from Source

1. Open the project in Android Studio or build via Gradle:
   ```bash
   gradle assembleDebug
   ```
2. Run local JVM & Robolectric unit tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
