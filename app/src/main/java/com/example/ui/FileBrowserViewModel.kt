package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StorageRepository
import com.example.model.BreadcrumbSegment
import com.example.model.ClipboardAction
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOption
import com.example.model.StagedClipboard
import com.example.model.StorageVolumeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface ActiveDialog {
  data object NewFolder : ActiveDialog
  data object NewFile : ActiveDialog
  data class Rename(val item: FileItem) : ActiveDialog
  data class DeleteConfirm(val items: List<FileItem>) : ActiveDialog
}

class FileBrowserViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = StorageRepository(application.applicationContext)

  private val _currentPath = MutableStateFlow(repository.defaultWorkspacePath)
  val currentPath: StateFlow<String> = _currentPath.asStateFlow()

  private val _breadcrumbs = MutableStateFlow<List<BreadcrumbSegment>>(emptyList())
  val breadcrumbs: StateFlow<List<BreadcrumbSegment>> = _breadcrumbs.asStateFlow()

  private val _items = MutableStateFlow<List<FileItem>>(emptyList())
  val items: StateFlow<List<FileItem>> = _items.asStateFlow()

  private val _storageVolumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
  val storageVolumes: StateFlow<List<StorageVolumeInfo>> = _storageVolumes.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _userNotice = MutableStateFlow<String?>(null)
  val userNotice: StateFlow<String?> = _userNotice.asStateFlow()

  private val _showHiddenFolders = MutableStateFlow(repository.getShowHiddenFolders())
  val showHiddenFolders: StateFlow<Boolean> = _showHiddenFolders.asStateFlow()

  private val _selectedItemPaths = MutableStateFlow<Set<String>>(emptySet())
  val selectedItemPaths: StateFlow<Set<String>> = _selectedItemPaths.asStateFlow()

  private val _clipboard = MutableStateFlow<StagedClipboard?>(null)
  val clipboard: StateFlow<StagedClipboard?> = _clipboard.asStateFlow()

  private val _activePreviewFile = MutableStateFlow<FileItem?>(null)
  val activePreviewFile: StateFlow<FileItem?> = _activePreviewFile.asStateFlow()

  private val _activePreviewText = MutableStateFlow<String?>(null)
  val activePreviewText: StateFlow<String?> = _activePreviewText.asStateFlow()

  private val _isEditingText = MutableStateFlow(false)
  val isEditingText: StateFlow<Boolean> = _isEditingText.asStateFlow()

  private val _activeDialog = MutableStateFlow<ActiveDialog?>(null)
  val activeDialog: StateFlow<ActiveDialog?> = _activeDialog.asStateFlow()

  private val backHistory = mutableListOf<String>()

  init {
    loadVolumes()
    navigateTo(_currentPath.value, recordHistory = false)
  }

  fun loadVolumes() {
    viewModelScope.launch {
      _storageVolumes.value = repository.getStorageVolumes()
    }
  }

  fun onStoragePermissionGranted() {
    loadVolumes()
    val extPath = repository.externalStoragePath
    val appFilesPrefix = getApplication<Application>().filesDir.absolutePath
    if (extPath != null && repository.isExternalStorageReadable && _currentPath.value.startsWith(appFilesPrefix)) {
      backHistory.clear()
      navigateTo(extPath, recordHistory = false)
    } else {
      refreshDirectory()
    }
  }

  fun navigateTo(path: String, recordHistory: Boolean = true) {
    if (recordHistory && _currentPath.value != path) {
      backHistory.add(_currentPath.value)
    }
    _currentPath.value = path
    _selectedItemPaths.value = emptySet()
    updateBreadcrumbs(path)
    refreshDirectory()
  }

  fun navigateUp(): Boolean {
    if (_activePreviewFile.value != null) {
      closePreview()
      return true
    }
    if (_activeDialog.value != null) {
      closeDialog()
      return true
    }
    if (_selectedItemPaths.value.isNotEmpty()) {
      clearSelection()
      return true
    }
    if (backHistory.isNotEmpty()) {
      val prev = backHistory.removeAt(backHistory.lastIndex)
      _currentPath.value = prev
      _selectedItemPaths.value = emptySet()
      updateBreadcrumbs(prev)
      refreshDirectory()
      return true
    }
    val curFile = File(_currentPath.value)
    val parent = curFile.parentFile
    if (parent != null && parent.exists() && parent.canRead()) {
      navigateTo(parent.absolutePath, recordHistory = true)
      return true
    }
    return false
  }

  fun refreshDirectory() {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      val result = repository.listDirectory(
        dirPath = _currentPath.value,
        showHidden = _showHiddenFolders.value,
        sortOption = SortOption.TYPE_ASC
      )
      result.fold(
        onSuccess = { itemsList ->
          _items.value = itemsList
          _isLoading.value = false
        },
        onFailure = { error ->
          _isLoading.value = false
          _errorMessage.value = error.localizedMessage ?: "Failed to read directory"
        }
      )
    }
  }

  private fun updateBreadcrumbs(fullPath: String) {
    val segments = mutableListOf<BreadcrumbSegment>()

    if (fullPath.startsWith("/storage/emulated/0")) {
      val base = "/storage/emulated/0"
      segments.add(BreadcrumbSegment(name = "Storage", fullPath = base, isRoot = true))
      val rel = fullPath.removePrefix(base).trimStart('/')
      if (rel.isNotEmpty()) {
        val parts = rel.split('/').filter { it.isNotEmpty() }
        var accum = base
        for (part in parts) {
          accum += "/$part"
          segments.add(BreadcrumbSegment(name = part, fullPath = accum))
        }
      }
    } else {
      val parts = fullPath.split('/').filter { it.isNotEmpty() }
      var accum = ""
      segments.add(BreadcrumbSegment(name = "Root", fullPath = "/", isRoot = true))
      for (part in parts) {
        accum += "/$part"
        segments.add(BreadcrumbSegment(name = part, fullPath = accum))
      }
    }

    _breadcrumbs.value = segments
  }

  fun toggleShowHiddenFolders() {
    val newVal = !_showHiddenFolders.value
    _showHiddenFolders.value = newVal
    repository.setShowHiddenFolders(newVal)
    _userNotice.value = if (newVal) "Showing hidden folders" else "Hidden folders concealed"
    refreshDirectory()
  }

  fun toggleHideSelectedItems() {
    val selected = _items.value.filter { _selectedItemPaths.value.contains(it.path) }
    if (selected.isEmpty()) return

    val allUserHidden = selected.all { it.isUserHidden }
    val paths = selected.map { it.path }

    if (allUserHidden) {
      repository.unhidePaths(paths)
      _userNotice.value = "Unhid ${selected.size} item(s)"
    } else {
      repository.hidePaths(paths)
      _userNotice.value = if (_showHiddenFolders.value) {
        "Marked ${selected.size} item(s) as hidden"
      } else {
        "Hid ${selected.size} item(s)"
      }
    }
    clearSelection()
    refreshDirectory()
  }

  fun toggleSelectItem(item: FileItem) {
    _selectedItemPaths.update { current ->
      if (current.contains(item.path)) current - item.path else current + item.path
    }
  }

  fun selectAll() {
    _selectedItemPaths.value = _items.value.map { it.path }.toSet()
  }

  fun clearSelection() {
    _selectedItemPaths.value = emptySet()
  }

  fun stageClipboard(action: ClipboardAction) {
    val selectedPaths = _selectedItemPaths.value
    val selectedItems = _items.value.filter { selectedPaths.contains(it.path) }
    if (selectedItems.isNotEmpty()) {
      _clipboard.value = StagedClipboard(action, selectedItems)
      clearSelection()
      _userNotice.value = "${selectedItems.size} item(s) staged to ${action.name.lowercase()}"
    }
  }

  fun clearClipboard() {
    _clipboard.value = null
  }

  fun executePaste() {
    val clip = _clipboard.value ?: return
    viewModelScope.launch {
      _isLoading.value = true
      val paths = clip.items.map { it.path }
      val result = if (clip.action == ClipboardAction.COPY) {
        repository.copyItems(paths, _currentPath.value)
      } else {
        repository.moveItems(paths, _currentPath.value)
      }

      result.fold(
        onSuccess = { count ->
          _isLoading.value = false
          _clipboard.value = null
          _userNotice.value = "${if (clip.action == ClipboardAction.COPY) "Copied" else "Moved"} $count item(s)"
          refreshDirectory()
          loadVolumes()
        },
        onFailure = { error ->
          _isLoading.value = false
          _errorMessage.value = "Paste failed: ${error.localizedMessage}"
        }
      )
    }
  }

  fun showDialog(dialog: ActiveDialog) {
    _activeDialog.value = dialog
  }

  fun closeDialog() {
    _activeDialog.value = null
  }

  fun createFolder(name: String) {
    viewModelScope.launch {
      val res = repository.createFolder(_currentPath.value, name)
      res.fold(
        onSuccess = {
          closeDialog()
          _userNotice.value = "Folder created: $name"
          refreshDirectory()
        },
        onFailure = { err ->
          _errorMessage.value = err.localizedMessage
        }
      )
    }
  }

  fun createFile(name: String, content: String = "") {
    viewModelScope.launch {
      val res = repository.createFile(_currentPath.value, name, content)
      res.fold(
        onSuccess = {
          closeDialog()
          _userNotice.value = "File created: $name"
          refreshDirectory()
        },
        onFailure = { err ->
          _errorMessage.value = err.localizedMessage
        }
      )
    }
  }

  fun renameItem(item: FileItem, newName: String) {
    viewModelScope.launch {
      val res = repository.rename(item.path, newName)
      res.fold(
        onSuccess = {
          closeDialog()
          clearSelection()
          _userNotice.value = "Renamed to $newName"
          refreshDirectory()
        },
        onFailure = { err ->
          _errorMessage.value = err.localizedMessage
        }
      )
    }
  }

  fun deleteItems(items: List<FileItem>) {
    viewModelScope.launch {
      val res = repository.deleteItems(items.map { it.path })
      res.fold(
        onSuccess = { count ->
          closeDialog()
          clearSelection()
          _userNotice.value = "Deleted $count item(s)"
          refreshDirectory()
          loadVolumes()
        },
        onFailure = { err ->
          _errorMessage.value = err.localizedMessage
        }
      )
    }
  }

  fun openFile(item: FileItem, onOpenExternalDocument: (FileItem) -> Boolean = { false }) {
    if (item.isDirectory) {
      navigateTo(item.path)
      return
    }

    if (item.category == FileCategory.IMAGE) {
      _activePreviewFile.value = item
      _activePreviewText.value = null
      _isEditingText.value = false
      return
    }

    if (item.isTextDocument) {
      viewModelScope.launch {
        _isLoading.value = true
        val textRes = repository.readFileText(item.path)
        textRes.fold(
          onSuccess = { text ->
            _activePreviewFile.value = item
            _activePreviewText.value = text
            _isEditingText.value = false
            _isLoading.value = false
          },
          onFailure = {
            _isLoading.value = false
            if (!onOpenExternalDocument(item)) {
              _errorMessage.value = "Cannot open ${item.name}"
            }
          }
        )
      }
      return
    }

    // For PDFs, Office documents, Audio, Video, etc., try external viewer first, else fallback modal
    if (!onOpenExternalDocument(item)) {
      _activePreviewFile.value = item
      _activePreviewText.value = null
      _isEditingText.value = false
    }
  }

  fun toggleEditMode() {
    _isEditingText.update { !it }
  }

  fun saveFileContent(newContent: String) {
    val file = _activePreviewFile.value ?: return
    viewModelScope.launch {
      _isLoading.value = true
      val res = repository.writeFileText(file.path, newContent)
      res.fold(
        onSuccess = {
          _activePreviewText.value = newContent
          _isEditingText.value = false
          _isLoading.value = false
          _userNotice.value = "Saved changes to ${file.name}"
          refreshDirectory()
        },
        onFailure = { err ->
          _isLoading.value = false
          _errorMessage.value = "Failed to save: ${err.localizedMessage}"
        }
      )
    }
  }

  fun closePreview() {
    _activePreviewFile.value = null
    _activePreviewText.value = null
    _isEditingText.value = false
  }

  fun dismissNotice() {
    _userNotice.value = null
  }

  fun clearError() {
    _errorMessage.value = null
  }
}
