package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FileItem
import com.example.model.StorageVolumeInfo
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent

@Composable
fun NewFolderDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var folderName by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PureWhite)
    },
    title = { Text("New Directory") },
    text = {
      Column {
        Text(
          "Enter folder name:",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = folderName,
          onValueChange = {
            folderName = it
            isError = false
          },
          singleLine = true,
          isError = isError,
          placeholder = { Text("e.g. Projects, Wallpapers") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_folder_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (folderName.trim().isNotEmpty()) {
            onConfirm(folderName.trim())
          } else {
            isError = true
          }
        },
        modifier = Modifier.testTag("confirm_create_folder")
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun NewFileDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit
) {
  var fileName by remember { mutableStateOf("") }
  var initialContent by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.NoteAdd, contentDescription = null, tint = PurpleAccent)
    },
    title = { Text("New File") },
    text = {
      Column {
        Text(
          "Enter file name with extension:",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = fileName,
          onValueChange = {
            fileName = it
            isError = false
          },
          singleLine = true,
          isError = isError,
          placeholder = { Text("e.g. notes.md, config.json") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_file_name_input")
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          "Optional initial content:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = initialContent,
          onValueChange = { initialContent = it },
          maxLines = 4,
          placeholder = { Text("Write content here...") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fileName.trim().isNotEmpty()) {
            onConfirm(fileName.trim(), initialContent)
          } else {
            isError = true
          }
        },
        modifier = Modifier.testTag("confirm_create_file")
      ) {
        Text("Create File")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun RenameDialog(
  item: FileItem,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var newName by remember { mutableStateOf(item.name) }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    },
    title = { Text("Rename ${if (item.isDirectory) "Directory" else "File"}") },
    text = {
      Column {
        OutlinedTextField(
          value = newName,
          onValueChange = {
            newName = it
            isError = false
          },
          singleLine = true,
          isError = isError,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("rename_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (newName.trim().isNotEmpty()) {
            onConfirm(newName.trim())
          } else {
            isError = true
          }
        },
        modifier = Modifier.testTag("confirm_rename")
      ) {
        Text("Rename")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun DeleteConfirmDialog(
  items: List<FileItem>,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    },
    title = { Text("Delete ${if (items.size == 1) items.first().name else "${items.size} Items"}?") },
    text = {
      Text(
        "Are you sure you want to permanently delete ${if (items.size == 1) "'${items.first().name}'" else "these ${items.size} items"}? This action cannot be undone.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete")
      ) {
        Text("Delete")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBreakdownSheet(
  volumes: List<StorageVolumeInfo>,
  onSelectVolume: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("storage_breakdown_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .navigationBarsPadding()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.PieChart,
          contentDescription = null,
          tint = PurpleAccent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Storage Partitions & Space",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        volumes.forEach { vol ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                onSelectVolume(vol.path)
                onDismiss()
              },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (vol.isPrimary) Icons.Default.Folder else Icons.Default.Storage,
                  contentDescription = null,
                  tint = if (vol.isPrimary) PurpleAccent else PureWhite,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = vol.title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                  text = "${(vol.usedPercentage * 100).toInt()}% used",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = vol.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(8.dp))
              LinearProgressIndicator(
                progress = { vol.usedPercentage },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = if (vol.usedPercentage > 0.85f) MaterialTheme.colorScheme.error else PurpleAccent,
                trackColor = MaterialTheme.colorScheme.outlineVariant
              )

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Used: ${vol.formattedUsed}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Free: ${vol.formattedFree} of ${vol.formattedTotal}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLocationsSheet(
  volumes: List<StorageVolumeInfo>,
  onSelectPath: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .navigationBarsPadding()
    ) {
      Text(
        text = "Quick Storage Locations",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(14.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        volumes.forEach { vol ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .clickable {
                onSelectPath(vol.path)
                onDismiss()
              }
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (vol.isPrimary) Icons.Default.Folder else Icons.Default.SdCard,
              contentDescription = null,
              tint = PurpleAccent,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = vol.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = vol.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
