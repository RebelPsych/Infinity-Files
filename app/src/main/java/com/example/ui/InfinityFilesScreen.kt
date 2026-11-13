package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ClipboardAction
import com.example.model.FileItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileItemGridCard
import com.example.ui.components.FilePreviewModal
import com.example.ui.components.NewFileDialog
import com.example.ui.components.NewFolderDialog
import com.example.ui.components.RenameDialog
import com.example.ui.components.TopBreadcrumbBar
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfinityFilesScreen(
  viewModel: FileBrowserViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val snackbarHostState = remember { SnackbarHostState() }

  val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
  val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
  val items by viewModel.items.collectAsStateWithLifecycle()
  val storageVolumes by viewModel.storageVolumes.collectAsStateWithLifecycle()
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
  val userNotice by viewModel.userNotice.collectAsStateWithLifecycle()
  val showHiddenFolders by viewModel.showHiddenFolders.collectAsStateWithLifecycle()
  val selectedItemPaths by viewModel.selectedItemPaths.collectAsStateWithLifecycle()
  val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
  val activePreviewFile by viewModel.activePreviewFile.collectAsStateWithLifecycle()
  val activePreviewText by viewModel.activePreviewText.collectAsStateWithLifecycle()
  val isEditingText by viewModel.isEditingText.collectAsStateWithLifecycle()
  val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()

  // Storage permission check state
  var hasStoragePermission by remember {
    mutableStateOf(checkStoragePermission(context))
  }

  // Re-check storage permission when returning from system settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val grantedNow = checkStoragePermission(context)
        if (grantedNow && !hasStoragePermission) {
          hasStoragePermission = true
          viewModel.onStoragePermissionGranted()
        } else {
          hasStoragePermission = grantedNow
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Permission launcher for Android 10 and below
  val legacyPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val isGranted = permissions.values.all { it }
    hasStoragePermission = isGranted
    if (isGranted) {
      viewModel.onStoragePermissionGranted()
    }
  }

  // Handle native back button
  BackHandler {
    viewModel.navigateUp()
  }

  // Show user notice snackbar
  LaunchedEffect(userNotice) {
    userNotice?.let { notice ->
      snackbarHostState.showSnackbar(notice)
      viewModel.dismissNotice()
    }
  }

  // Show error snackbar
  LaunchedEffect(errorMessage) {
    errorMessage?.let { err ->
      snackbarHostState.showSnackbar(err)
      viewModel.clearError()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = PureBlack,
    topBar = {
      TopBreadcrumbBar(
        breadcrumbs = breadcrumbs,
        canGoUp = breadcrumbs.size > 1,
        onNavigateUp = { viewModel.navigateUp() },
        onBreadcrumbClick = { path -> viewModel.navigateTo(path) },
        storageVolumes = storageVolumes,
        currentPath = currentPath,
        onSelectVolume = { path -> viewModel.navigateTo(path) },
        showHiddenFolders = showHiddenFolders,
        onToggleShowHidden = { viewModel.toggleShowHiddenFolders() },
        onNewFolder = { viewModel.showDialog(ActiveDialog.NewFolder) },
        onNewFile = { viewModel.showDialog(ActiveDialog.NewFile) }
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(PureBlack)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {

        // Storage Permission Banner (if not granted on device)
        if (!hasStoragePermission) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Slate900,
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = PurpleAccent,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Storage Access Required",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite
                )
                Text(
                  text = "Allow access to browse all device folders and files",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching {
                      val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                      }
                      context.startActivity(intent)
                    }.onFailure {
                      val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                      context.startActivity(intent)
                    }
                  } else {
                    legacyPermissionLauncher.launch(
                      arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                      )
                    )
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("grant_storage_permission_button")
              ) {
                Text("Grant", fontSize = 12.sp)
              }
            }
          }
        }

        // Staged Clipboard Bar (Copy / Move)
        AnimatedVisibility(
          visible = clipboard != null,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          clipboard?.let { clip ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
              color = Slate900,
              shape = RoundedCornerShape(10.dp)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (clip.action == ClipboardAction.COPY) Icons.Default.ContentCopy else Icons.Default.DriveFileMove,
                  contentDescription = null,
                  tint = PurpleAccent,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${clip.items.size} item(s) staged to ${clip.action.name.lowercase()}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = PureWhite,
                  modifier = Modifier.weight(1f)
                )
                Button(
                  onClick = { viewModel.executePaste() },
                  colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("paste_button")
                ) {
                  Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Paste Here", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { viewModel.clearClipboard() }) {
                  Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = PureWhite, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }

        // Grid View of All Accessible Folders and Files
        if (isLoading && items.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = PurpleAccent)
          }
        } else if (items.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Slate800,
                modifier = Modifier.size(64.dp)
              )
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Empty Folder",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PureWhite
              )
              Spacer(modifier = Modifier.height(16.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                  onClick = { viewModel.showDialog(ActiveDialog.NewFolder) },
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("New Folder")
                }
                Button(
                  onClick = { viewModel.showDialog(ActiveDialog.NewFile) },
                  colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("New File")
                }
              }
            }
          }
        } else {
          val isSelectionMode = selectedItemPaths.isNotEmpty()

          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentPadding = PaddingValues(
              start = 8.dp,
              end = 8.dp,
              top = 8.dp,
              bottom = if (isSelectionMode) 92.dp else 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(items, key = { it.path }) { item ->
              val isSelected = selectedItemPaths.contains(item.path)
              FileItemGridCard(
                item = item,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onClick = {
                  viewModel.openFile(item) { docItem ->
                    openExternalDocument(context, docItem)
                  }
                },
                onLongClick = { viewModel.toggleSelectItem(item) }
              )
            }
          }
        }
      }

      // Selection Action Bar (when long-pressing to select items)
      AnimatedVisibility(
        visible = selectedItemPaths.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 12.dp, vertical = 12.dp)
      ) {
        val selectedItems = items.filter { selectedItemPaths.contains(it.path) }
        val allUserHidden = selectedItems.isNotEmpty() && selectedItems.all { it.isUserHidden }

        Surface(
          color = Slate900,
          shape = RoundedCornerShape(16.dp),
          tonalElevation = 6.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { viewModel.clearSelection() }) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = PureWhite)
              }
              Text(
                text = "${selectedItemPaths.size}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              IconButton(
                onClick = { viewModel.selectAll() },
                modifier = Modifier.testTag("select_all_button")
              ) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = PurpleAccentLight)
              }

              // Hide / Unhide selected folders or files
              IconButton(
                onClick = { viewModel.toggleHideSelectedItems() },
                modifier = Modifier.testTag("toggle_hide_selected_button")
              ) {
                Icon(
                  imageVector = if (allUserHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (allUserHidden) "Unhide" else "Hide",
                  tint = PureWhite
                )
              }

              if (selectedItemPaths.size == 1) {
                IconButton(
                  onClick = {
                    val singleItem = selectedItems.firstOrNull()
                    singleItem?.let { viewModel.showDialog(ActiveDialog.Rename(it)) }
                  },
                  modifier = Modifier.testTag("rename_button")
                ) {
                  Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", tint = PureWhite)
                }

                if (selectedItems.firstOrNull()?.isDirectory == false) {
                  IconButton(
                    onClick = {
                      selectedItems.firstOrNull()?.let { shareFile(context, it) }
                    },
                    modifier = Modifier.testTag("share_button")
                  ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = PureWhite)
                  }
                }
              }

              IconButton(
                onClick = { viewModel.stageClipboard(ClipboardAction.COPY) },
                modifier = Modifier.testTag("stage_copy_button")
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PureWhite)
              }

              IconButton(
                onClick = { viewModel.stageClipboard(ClipboardAction.MOVE) },
                modifier = Modifier.testTag("stage_move_button")
              ) {
                Icon(Icons.Default.DriveFileMove, contentDescription = "Move", tint = PurpleAccentLight)
              }

              IconButton(
                onClick = {
                  viewModel.showDialog(ActiveDialog.DeleteConfirm(selectedItems))
                },
                modifier = Modifier.testTag("batch_delete_button")
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }
  }

  // Active Dialogs Handling
  when (val dlg = activeDialog) {
    is ActiveDialog.NewFolder -> {
      NewFolderDialog(
        onDismiss = { viewModel.closeDialog() },
        onConfirm = { name -> viewModel.createFolder(name) }
      )
    }
    is ActiveDialog.NewFile -> {
      NewFileDialog(
        onDismiss = { viewModel.closeDialog() },
        onConfirm = { name, content -> viewModel.createFile(name, content) }
      )
    }
    is ActiveDialog.Rename -> {
      RenameDialog(
        item = dlg.item,
        onDismiss = { viewModel.closeDialog() },
        onConfirm = { newName -> viewModel.renameItem(dlg.item, newName) }
      )
    }
    is ActiveDialog.DeleteConfirm -> {
      DeleteConfirmDialog(
        items = dlg.items,
        onDismiss = { viewModel.closeDialog() },
        onConfirm = { viewModel.deleteItems(dlg.items) }
      )
    }
    null -> {}
  }

  // Image & Document Preview Modal
  activePreviewFile?.let { previewFile ->
    FilePreviewModal(
      file = previewFile,
      previewText = activePreviewText,
      isEditing = isEditingText,
      onToggleEdit = { viewModel.toggleEditMode() },
      onSaveContent = { newContent -> viewModel.saveFileContent(newContent) },
      onShare = { shareFile(context, previewFile) },
      onDismiss = { viewModel.closePreview() }
    )
  }
}

private fun checkStoragePermission(context: Context): Boolean {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    Environment.isExternalStorageManager()
  } else {
    val readGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    val writeGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    readGranted && writeGranted
  }
}

private fun openExternalDocument(context: Context, fileItem: FileItem): Boolean {
  return runCatching {
    val file = File(fileItem.path)
    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, fileItem.mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(viewIntent)
    true
  }.getOrDefault(false)
}

private fun shareFile(context: Context, fileItem: FileItem) {
  runCatching {
    val file = File(fileItem.path)
    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_STREAM, uri)
      type = fileItem.mimeType
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, "Share ${fileItem.name}")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }.onFailure {
    Toast.makeText(context, "Could not open share: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
  }
}
