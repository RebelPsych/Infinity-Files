package com.example.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory(val label: String) {
  FOLDER("Folder"),
  DOCUMENT("Document"),
  CODE("Code"),
  IMAGE("Image"),
  AUDIO("Audio"),
  VIDEO("Video"),
  ARCHIVE("Archive"),
  OTHER("File")
}

enum class SortOption(val displayName: String) {
  NAME_ASC("Name (A to Z)"),
  NAME_DESC("Name (Z to A)"),
  DATE_DESC("Date (Newest first)"),
  DATE_ASC("Date (Oldest first)"),
  SIZE_DESC("Size (Largest first)"),
  SIZE_ASC("Size (Smallest first)"),
  TYPE_ASC("Type (Folders first)")
}

enum class ViewMode {
  LIST,
  GRID,
  COMPACT
}

data class FileItem(
  val name: String,
  val path: String,
  val parentPath: String,
  val isDirectory: Boolean,
  val size: Long,
  val lastModified: Long,
  val extension: String,
  val mimeType: String,
  val isReadable: Boolean,
  val isWritable: Boolean,
  val isHidden: Boolean,
  val childCount: Int? = null,
  val category: FileCategory = FileCategory.OTHER,
  val isBookmarked: Boolean = false
) {
  val formattedSize: String
    get() {
      if (isDirectory) {
        return childCount?.let { "$it items" } ?: "Folder"
      }
      return formatByteSize(size)
    }

  val formattedDate: String
    get() {
      if (lastModified <= 0L) return "Unknown date"
      val sdf = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
      return sdf.format(Date(lastModified))
    }

  val relativeDate: String
    get() {
      if (lastModified <= 0L) return ""
      val now = System.currentTimeMillis()
      val diff = now - lastModified
      val seconds = diff / 1000
      val minutes = seconds / 60
      val hours = minutes / 60
      val days = hours / 24

      return when {
        diff < 0 -> "Just now"
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
          val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
          sdf.format(Date(lastModified))
        }
      }
    }

  companion object {
    fun fromFile(file: File, isBookmarked: Boolean = false): FileItem {
      val isDir = file.isDirectory
      val ext = if (isDir) "" else file.extension.lowercase()
      val category = if (isDir) FileCategory.FOLDER else determineCategory(ext)
      val mime = determineMimeType(ext)
      val children = if (isDir) {
        runCatching { file.listFiles()?.size }.getOrNull()
      } else null

      return FileItem(
        name = file.name.ifEmpty { file.path },
        path = file.absolutePath,
        parentPath = file.parent ?: "",
        isDirectory = isDir,
        size = if (isDir) 0L else file.length(),
        lastModified = file.lastModified(),
        extension = ext,
        mimeType = mime,
        isReadable = file.canRead(),
        isWritable = file.canWrite(),
        isHidden = file.name.startsWith("."),
        childCount = children,
        category = category,
        isBookmarked = isBookmarked
      )
    }

    private fun determineCategory(extension: String): FileCategory {
      return when (extension) {
        "txt", "md", "markdown", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "rtf", "csv" -> FileCategory.DOCUMENT
        "kt", "kts", "java", "py", "js", "ts", "json", "xml", "html", "css", "yaml", "yml", "sh", "c", "cpp", "h", "rs", "go", "sql" -> FileCategory.CODE
        "png", "jpg", "jpeg", "webp", "gif", "bmp", "svg", "ico" -> FileCategory.IMAGE
        "mp3", "wav", "ogg", "m4a", "flac", "aac", "mid" -> FileCategory.AUDIO
        "mp4", "mkv", "avi", "mov", "webm", "flv", "wmv" -> FileCategory.VIDEO
        "zip", "tar", "gz", "7z", "rar", "bz2", "xz" -> FileCategory.ARCHIVE
        else -> FileCategory.OTHER
      }
    }

    private fun determineMimeType(extension: String): String {
      return when (extension) {
        "txt" -> "text/plain"
        "md", "markdown" -> "text/markdown"
        "pdf" -> "application/pdf"
        "csv" -> "text/csv"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "html" -> "text/html"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "mp4" -> "video/mp4"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
      }
    }

    fun formatByteSize(bytes: Long): String {
      if (bytes <= 0) return "0 B"
      val units = arrayOf("B", "KB", "MB", "GB", "TB")
      val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
      val formatted = String.format(Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
      val cleanFormatted = if (formatted.endsWith(".0")) formatted.substring(0, formatted.length - 2) else formatted
      return "$cleanFormatted ${units.getOrElse(digitGroups) { "B" }}"
    }
  }
}

data class StorageVolumeInfo(
  val id: String,
  val title: String,
  val subtitle: String,
  val path: String,
  val totalBytes: Long,
  val freeBytes: Long,
  val isPrimary: Boolean = false,
  val isCustomTree: Boolean = false
) {
  val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
  val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
  val formattedUsed: String get() = FileItem.formatByteSize(usedBytes)
  val formattedTotal: String get() = FileItem.formatByteSize(totalBytes)
  val formattedFree: String get() = FileItem.formatByteSize(freeBytes)
}

data class BreadcrumbSegment(
  val name: String,
  val fullPath: String,
  val isRoot: Boolean = false
)

enum class ClipboardAction {
  COPY,
  MOVE
}

data class StagedClipboard(
  val action: ClipboardAction,
  val items: List<FileItem>
)

data class FileInspectionInfo(
  val fileItem: FileItem,
  val md5Hash: String? = null,
  val sha256Hash: String? = null,
  val lineCount: Int? = null,
  val absolutePath: String = fileItem.path,
  val permissions: String = (if (fileItem.isReadable) "r" else "-") + (if (fileItem.isWritable) "w" else "-")
)
