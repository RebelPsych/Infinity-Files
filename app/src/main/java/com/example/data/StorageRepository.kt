package com.example.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.model.FileCategory
import com.example.model.FileInspectionInfo
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.StorageVolumeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class StorageRepository(private val context: Context) {

  private val prefs = context.getSharedPreferences("infinity_files_prefs", Context.MODE_PRIVATE)
  private val bookmarksKey = "bookmarked_paths"
  private val hiddenPathsKey = "hidden_folder_paths"
  private val showHiddenKey = "show_hidden_folders"

  val isExternalStorageReadable: Boolean
    get() {
      val extRoot = Environment.getExternalStorageDirectory()
      return extRoot != null && extRoot.exists() && extRoot.canRead() && extRoot.listFiles() != null
    }

  val externalStoragePath: String?
    get() = Environment.getExternalStorageDirectory()?.absolutePath

  val defaultWorkspacePath: String by lazy {
    val extRoot = Environment.getExternalStorageDirectory()
    if (extRoot != null && extRoot.exists() && extRoot.canRead() && (extRoot.listFiles() != null)) {
      extRoot.absolutePath
    } else {
      val workspace = File(context.filesDir, "InfinityWorkspace")
      if (!workspace.exists()) {
        workspace.mkdirs()
        seedInitialWorkspace(workspace)
      }
      workspace.absolutePath
    }
  }

  init {
    val wsFile = File(context.filesDir, "InfinityWorkspace")
    if (!wsFile.exists() || wsFile.list()?.isEmpty() == true) {
      wsFile.mkdirs()
      seedInitialWorkspace(wsFile)
    }

    // Also populate common folders on external storage if writable
    val ext = Environment.getExternalStorageDirectory()
    if (ext != null && ext.exists() && ext.canWrite()) {
      runCatching {
        File(ext, "Download").mkdirs()
        File(ext, "Documents").mkdirs()
        File(ext, "Pictures").mkdirs()
        File(ext, "DCIM").mkdirs()
        File(ext, "Music").mkdirs()
      }
    }
  }

  suspend fun getStorageVolumes(): List<StorageVolumeInfo> = withContext(Dispatchers.IO) {
    val volumes = mutableListOf<StorageVolumeInfo>()

    // 1. Device Shared Internal Storage (/storage/emulated/0)
    val extRoot = Environment.getExternalStorageDirectory()
    if (extRoot != null && extRoot.exists()) {
      val stats = getPathStats(extRoot)
      volumes.add(
        StorageVolumeInfo(
          id = "device_root",
          title = "Internal Storage",
          subtitle = extRoot.absolutePath,
          path = extRoot.absolutePath,
          totalBytes = stats.first,
          freeBytes = stats.second,
          isPrimary = true
        )
      )
    }

    // 2. Discover external SD cards or mounted storage in /storage and externalFilesDirs
    val discoveredPaths = volumes.map { it.path }.toMutableSet()
    val storageDir = File("/storage")
    if (storageDir.exists() && storageDir.canRead()) {
      storageDir.listFiles()?.forEach { file ->
        if (file.isDirectory && file.name != "emulated" && file.name != "self" && file.canRead() && !discoveredPaths.contains(file.absolutePath)) {
          discoveredPaths.add(file.absolutePath)
          val stats = getPathStats(file)
          volumes.add(
            StorageVolumeInfo(
              id = "sd_${file.name}",
              title = "SD Card (${file.name})",
              subtitle = file.absolutePath,
              path = file.absolutePath,
              totalBytes = stats.first,
              freeBytes = stats.second
            )
          )
        }
      }
    }
    context.getExternalFilesDirs(null)?.forEach { appDir ->
      if (appDir != null) {
        val marker = "/Android/data/"
        val idx = appDir.absolutePath.indexOf(marker)
        if (idx > 0) {
          val volRoot = File(appDir.absolutePath.substring(0, idx))
          if (volRoot.exists() && volRoot.canRead() && !discoveredPaths.contains(volRoot.absolutePath)) {
            discoveredPaths.add(volRoot.absolutePath)
            val stats = getPathStats(volRoot)
            volumes.add(
              StorageVolumeInfo(
                id = "vol_${volRoot.name}",
                title = "External (${volRoot.name})",
                subtitle = volRoot.absolutePath,
                path = volRoot.absolutePath,
                totalBytes = stats.first,
                freeBytes = stats.second
              )
            )
          }
        }
      }
    }

    // 3. System Root Partition (/)
    val rootDir = File("/")
    if (rootDir.exists() && rootDir.canRead()) {
      val stats = getPathStats(rootDir)
      volumes.add(
        StorageVolumeInfo(
          id = "sys_root",
          title = "System Root (/)",
          subtitle = "/",
          path = "/",
          totalBytes = stats.first,
          freeBytes = stats.second
        )
      )
    }

    // 4. App Storage / Sandbox
    val workspaceFile = File(context.filesDir, "InfinityWorkspace")
    if (workspaceFile.exists()) {
      val wsStats = getPathStats(workspaceFile)
      volumes.add(
        StorageVolumeInfo(
          id = "workspace",
          title = "App Files Workspace",
          subtitle = workspaceFile.absolutePath,
          path = workspaceFile.absolutePath,
          totalBytes = wsStats.first,
          freeBytes = wsStats.second
        )
      )
    }

    volumes
  }

  private fun getPathStats(file: File): Pair<Long, Long> {
    return runCatching {
      val stat = StatFs(file.path)
      val total = stat.blockCountLong * stat.blockSizeLong
      val free = stat.availableBlocksLong * stat.blockSizeLong
      Pair(total, free)
    }.getOrDefault(Pair(100L * 1024 * 1024 * 1024, 45L * 1024 * 1024 * 1024))
  }

  suspend fun listDirectory(
    dirPath: String,
    showHidden: Boolean,
    sortOption: SortOption,
    searchQuery: String = "",
    categoryFilter: FileCategory? = null
  ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
    runCatching {
      val dir = File(dirPath)
      if (!dir.exists()) {
        throw IllegalArgumentException("Directory does not exist: $dirPath")
      }
      if (!dir.isDirectory) {
        throw IllegalArgumentException("Path is not a directory: $dirPath")
      }

      val files = dir.listFiles() ?: emptyArray()
      val bookmarkedSet = getBookmarkedPaths()
      val hiddenSet = getHiddenPaths()

      var items = files.asSequence()
        .filter { file ->
          val isDotOrHidden = file.name.startsWith(".") || hiddenSet.contains(file.absolutePath)
          showHidden || !isDotOrHidden
        }
        .map { file ->
          FileItem.fromFile(
            file = file,
            isBookmarked = bookmarkedSet.contains(file.absolutePath),
            isUserHidden = hiddenSet.contains(file.absolutePath)
          )
        }

      // Filter by search query if present
      if (searchQuery.isNotBlank()) {
        val query = searchQuery.trim().lowercase()
        items = items.filter { it.name.lowercase().contains(query) }
      }

      // Filter by category if chosen (except folders are usually kept or filtered)
      if (categoryFilter != null) {
        items = items.filter { it.category == categoryFilter || (it.isDirectory && categoryFilter == FileCategory.FOLDER) }
      }

      val list = items.toList()

      // Sort
      sortItems(list, sortOption)
    }
  }

  private fun sortItems(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
    val folders = items.filter { it.isDirectory }
    val files = items.filter { !it.isDirectory }

    val folderComparator: Comparator<FileItem> = when (sortOption) {
      SortOption.NAME_ASC -> compareBy { it.name.lowercase() }
      SortOption.NAME_DESC -> compareByDescending { it.name.lowercase() }
      SortOption.DATE_DESC -> compareByDescending { it.lastModified }
      SortOption.DATE_ASC -> compareBy { it.lastModified }
      SortOption.SIZE_DESC -> compareByDescending { it.childCount ?: 0 }
      SortOption.SIZE_ASC -> compareBy { it.childCount ?: 0 }
      SortOption.TYPE_ASC -> compareBy { it.name.lowercase() }
    }

    val fileComparator: Comparator<FileItem> = when (sortOption) {
      SortOption.NAME_ASC -> compareBy { it.name.lowercase() }
      SortOption.NAME_DESC -> compareByDescending { it.name.lowercase() }
      SortOption.DATE_DESC -> compareByDescending { it.lastModified }
      SortOption.DATE_ASC -> compareBy { it.lastModified }
      SortOption.SIZE_DESC -> compareByDescending { it.size }
      SortOption.SIZE_ASC -> compareBy { it.size }
      SortOption.TYPE_ASC -> compareBy({ it.extension }, { it.name.lowercase() })
    }

    return folders.sortedWith(folderComparator) + files.sortedWith(fileComparator)
  }

  suspend fun createFolder(parentPath: String, folderName: String): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
      val safeName = folderName.trim().replace("/", "").replace("\\", "")
      if (safeName.isEmpty()) throw IllegalArgumentException("Folder name cannot be empty")
      val newFolder = File(parentPath, safeName)
      if (newFolder.exists()) throw IllegalStateException("A file or folder with this name already exists")
      if (!newFolder.mkdirs()) throw IllegalStateException("Could not create folder")
      newFolder
    }
  }

  suspend fun createFile(parentPath: String, fileName: String, content: String = ""): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
      val safeName = fileName.trim().replace("/", "").replace("\\", "")
      if (safeName.isEmpty()) throw IllegalArgumentException("File name cannot be empty")
      val newFile = File(parentPath, safeName)
      if (newFile.exists()) throw IllegalStateException("A file with this name already exists")
      newFile.writeText(content)
      newFile
    }
  }

  suspend fun rename(path: String, newName: String): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
      val target = File(path)
      if (!target.exists()) throw IllegalArgumentException("Target not found")
      val safeName = newName.trim().replace("/", "").replace("\\", "")
      if (safeName.isEmpty()) throw IllegalArgumentException("New name cannot be empty")
      val destination = File(target.parentFile, safeName)
      if (destination.exists()) throw IllegalStateException("An item with name '$safeName' already exists")
      val success = target.renameTo(destination)
      if (!success) throw IllegalStateException("Failed to rename item")
      updateHiddenPathOnMoveOrRename(target.absolutePath, destination.absolutePath)
      destination
    }
  }

  suspend fun deleteItems(paths: List<String>): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
      var count = 0
      for (p in paths) {
        val f = File(p)
        if (f.exists()) {
          val deleted = if (f.isDirectory) f.deleteRecursively() else f.delete()
          if (deleted) {
            removeHiddenPath(p)
            count++
          }
        }
      }
      count
    }
  }

  suspend fun copyItems(sourcePaths: List<String>, destDirPath: String): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
      val destDir = File(destDirPath)
      if (!destDir.exists() || !destDir.isDirectory) {
        throw IllegalArgumentException("Target is not a valid directory")
      }
      var count = 0
      for (srcPath in sourcePaths) {
        val src = File(srcPath)
        if (src.exists()) {
          var target = File(destDir, src.name)
          if (target.exists()) {
            val baseName = src.nameWithoutExtension
            val ext = if (src.extension.isNotEmpty()) ".${src.extension}" else ""
            target = File(destDir, "${baseName}_copy$ext")
          }
          if (src.isDirectory) {
            src.copyRecursively(target, overwrite = true)
          } else {
            src.copyTo(target, overwrite = true)
          }
          count++
        }
      }
      count
    }
  }

  suspend fun moveItems(sourcePaths: List<String>, destDirPath: String): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
      val destDir = File(destDirPath)
      if (!destDir.exists() || !destDir.isDirectory) {
        throw IllegalArgumentException("Target is not a valid directory")
      }
      var count = 0
      for (srcPath in sourcePaths) {
        val src = File(srcPath)
        if (src.exists()) {
          val target = File(destDir, src.name)
          if (target.absolutePath == src.absolutePath) continue
          if (src.renameTo(target)) {
            count++
          } else {
            // Fallback copy + delete
            if (src.isDirectory) {
              src.copyRecursively(target, overwrite = true)
              src.deleteRecursively()
            } else {
              src.copyTo(target, overwrite = true)
              src.delete()
            }
            count++
          }
        }
      }
      count
    }
  }

  suspend fun readFileText(path: String, maxChars: Int = 100_000): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      val file = File(path)
      if (!file.exists()) throw IllegalArgumentException("File does not exist")
      if (file.length() > maxChars * 4) {
        // Read prefix
        val bytes = ByteArray(maxChars)
        val read = FileInputStream(file).use { it.read(bytes) }
        String(bytes, 0, read) + "\n\n... [File truncated for preview; size is ${FileItem.formatByteSize(file.length())}]"
      } else {
        file.readText()
      }
    }
  }

  suspend fun writeFileText(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val file = File(path)
      file.writeText(content)
      Unit
    }
  }

  suspend fun inspectFile(fileItem: FileItem): FileInspectionInfo = withContext(Dispatchers.IO) {
    val file = File(fileItem.path)
    if (!file.exists()) return@withContext FileInspectionInfo(fileItem)

    var lineCount: Int? = null
    var md5Str: String? = null
    var sha256Str: String? = null

    if (!file.isDirectory && file.length() < 25 * 1024 * 1024) { // Only calculate hash for files < 25MB
      runCatching {
        val md5 = MessageDigest.getInstance("MD5")
        val sha256 = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)

        FileInputStream(file).use { stream ->
          var bytesRead: Int
          while (stream.read(buffer).also { bytesRead = it } != -1) {
            md5.update(buffer, 0, bytesRead)
            sha256.update(buffer, 0, bytesRead)
          }
        }

        md5Str = md5.digest().joinToString("") { "%02x".format(it) }
        sha256Str = sha256.digest().joinToString("") { "%02x".format(it) }
      }

      // Check text line count if text/code/doc
      if (fileItem.category == FileCategory.DOCUMENT || fileItem.category == FileCategory.CODE) {
        runCatching {
          lineCount = file.useLines { it.count() }
        }
      }
    }

    FileInspectionInfo(
      fileItem = fileItem,
      md5Hash = md5Str,
      sha256Hash = sha256Str,
      lineCount = lineCount
    )
  }

  fun getShowHiddenFolders(): Boolean {
    return prefs.getBoolean(showHiddenKey, false)
  }

  fun setShowHiddenFolders(show: Boolean) {
    prefs.edit().putBoolean(showHiddenKey, show).apply()
  }

  fun getHiddenPaths(): Set<String> {
    return prefs.getStringSet(hiddenPathsKey, emptySet())?.toSet() ?: emptySet()
  }

  fun hidePaths(paths: List<String>) {
    val current = getHiddenPaths().toMutableSet()
    current.addAll(paths)
    prefs.edit().putStringSet(hiddenPathsKey, current).apply()
  }

  fun unhidePaths(paths: List<String>) {
    val current = getHiddenPaths().toMutableSet()
    current.removeAll(paths.toSet())
    prefs.edit().putStringSet(hiddenPathsKey, current).apply()
  }

  private fun removeHiddenPath(path: String) {
    val current = getHiddenPaths().toMutableSet()
    if (current.remove(path)) {
      prefs.edit().putStringSet(hiddenPathsKey, current).apply()
    }
  }

  private fun updateHiddenPathOnMoveOrRename(oldPath: String, newPath: String) {
    val current = getHiddenPaths().toMutableSet()
    if (current.remove(oldPath)) {
      current.add(newPath)
      prefs.edit().putStringSet(hiddenPathsKey, current).apply()
    }
  }

  fun getBookmarkedPaths(): Set<String> {
    return prefs.getStringSet(bookmarksKey, emptySet()) ?: emptySet()
  }

  fun toggleBookmark(path: String): Boolean {
    val current = getBookmarkedPaths().toMutableSet()
    val isNowBookmarked = if (current.contains(path)) {
      current.remove(path)
      false
    } else {
      current.add(path)
      true
    }
    prefs.edit().putStringSet(bookmarksKey, current).apply()
    return isNowBookmarked
  }

  private fun seedInitialWorkspace(workspace: File) {
    runCatching {
      // 1. Documents Folder
      val docs = File(workspace, "Documents").apply { mkdirs() }
      File(docs, "Infinity_Roadmap.md").writeText(
        """
        # 🚀 Infinity Files - Clutter-Free Directory Browser
        
        Welcome to **Infinity Files**! Designed from the ground up to eliminate the bloated junk, advertising popups, and nested mazes of stock file managers.
        
        ### Core Principles:
        - **Speed First:** Instant directory listing with zero latency.
        - **Uncluttered:** Breadcrumbs with quick jump, clear visual category tags.
        - **Privacy Focused:** Completely device-local, zero tracking.
        - **Built-in Power Tools:** In-app syntax viewer/editor, cryptographic hash calculator (MD5, SHA-256), batch operations, and storage breakdown.
        
        ### Quick Gestures:
        - Tap any breadcrumb chip at the top to navigate upwards instantly.
        - Tap any text, markdown, code, or json file to read or edit on the fly.
        - Long-press any file or folder to select multiple items for batch copy, move, or deletion.
        """.trimIndent()
      )

      File(docs, "Device_Audit_Checklist.txt").writeText(
        """
        [x] Verify storage partition mounts
        [x] Scan local document tree
        [ ] Archive temporary logs
        [ ] Check SHA-256 integrity on downloaded packages
        """.trimIndent()
      )

      File(docs, "Expenses_Summary.csv").writeText(
        """
        Date,Category,Description,Amount,Status
        2026-11-01,Hardware,NVMe SSD Backup,129.99,Completed
        2026-11-03,Software,Cloud Certificate,45.00,Completed
        2026-11-06,Office,Ergonomic Peripherals,89.50,Pending
        """.trimIndent()
      )

      // 2. Development / Code
      val code = File(workspace, "Development").apply { mkdirs() }
      File(code, "AppConfig.json").writeText(
        """
        {
          "application": "Infinity Files",
          "version": "1.0.0",
          "engine": "Jetpack Compose",
          "cleanMode": true,
          "enableQuickThumbnails": true,
          "features": {
            "instantSearch": true,
            "hashInspection": true,
            "batchFileOperations": true
          }
        }
        """.trimIndent()
      )

      File(code, "FileProcessor.kt").writeText(
        """
        package com.infinity.tools

        class FileProcessor {
            fun inspect(fileName: String): String {
                return "Processed file: ${'$'}fileName at ${'$'}{System.currentTimeMillis()}"
            }
        }
        """.trimIndent()
      )

      File(code, "theme_styles.css").writeText(
        """
        :root {
          --infinity-cyan: #00e5ff;
          --infinity-slate: #0f172a;
          --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .container {
          display: flex;
          background: var(--infinity-slate);
        }
        """.trimIndent()
      )

      // 3. Media & Pictures
      val media = File(workspace, "Media").apply { mkdirs() }
      File(media, "README_Wallpapers.txt").writeText(
        "Store wallpapers, camera snapshots, and graphics here for instant preview."
      )

      // 4. Downloads
      val downloads = File(workspace, "Downloads").apply { mkdirs() }
      File(downloads, "release_notes_v1.0.txt").writeText(
        "Infinity Files build deployed. All systems operational."
      )

      // 5. System Logs
      val logs = File(workspace, "Logs").apply { mkdirs() }
      File(logs, "storage_diagnostic.log").writeText(
        """
        [INFO] Infinity Files directory index initialized.
        [INFO] VFS mapped cleanly.
        [INFO] Zero tracking daemon disabled.
        [READY] User workspace mounted.
        """.trimIndent()
      )
    }
  }
}
