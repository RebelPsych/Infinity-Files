package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FileCategory
import com.example.model.FileInspectionInfo
import com.example.model.FileItem
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewModal(
  file: FileItem,
  previewText: String?,
  isEditing: Boolean,
  onToggleEdit: () -> Unit,
  onSaveContent: (String) -> Unit,
  onShare: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var editedContent by remember(previewText) { mutableStateOf(previewText ?: "") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = PureBlack,
    modifier = Modifier.testTag("file_preview_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(horizontal = 16.dp)
        .navigationBarsPadding()
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = file.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
          )
          Text(
            text = "${file.formattedSize} • ${file.mimeType}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (previewText != null) {
          if (isEditing) {
            Button(
              onClick = { onSaveContent(editedContent) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .padding(end = 4.dp)
                .testTag("save_file_button")
            ) {
              Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save")
            }
          } else {
            IconButton(
              onClick = onToggleEdit,
              modifier = Modifier.testTag("edit_file_button")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit File", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }

        IconButton(onClick = onShare) {
          Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // Content Body
      if (file.category == FileCategory.IMAGE) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Slate900),
          contentAlignment = Alignment.Center
        ) {
          AsyncImage(
            model = File(file.path),
            contentDescription = file.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
          )
        }
      } else if (previewText != null) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          colors = CardDefaults.cardColors(containerColor = Slate900),
          shape = RoundedCornerShape(12.dp)
        ) {
          val verticalScroll = rememberScrollState()
          val horizontalScroll = rememberScrollState()

          if (isEditing) {
            BasicTextField(
              value = editedContent,
              onValueChange = { editedContent = it },
              textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              ),
              cursorBrush = SolidColor(PurpleAccentLight),
              modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(verticalScroll)
                .testTag("code_editor_field")
            )
          } else {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(12.dp)
            ) {
              // Line numbers
              val lines = previewText.lines()
              val lineNumbers = (1..lines.size).joinToString("\n")
              Text(
                text = lineNumbers,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 12.dp)
              )

              // Code/Text content
              Text(
                text = previewText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      } else {
        // Fallback info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Binary / Media File",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Use external apps or Share to open this file.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInspectionSheet(
  info: FileInspectionInfo,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()
  val context = LocalContext.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("file_inspection_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          tint = PurpleAccent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "File Properties & Hashes",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PropertyRow("Name", info.fileItem.name)
        PropertyRow("Path", info.absolutePath)
        PropertyRow("Size", "${info.fileItem.formattedSize} (${info.fileItem.size} bytes)")
        PropertyRow("Type", info.fileItem.mimeType)
        PropertyRow("Modified", info.fileItem.formattedDate)
        PropertyRow("Permissions", info.permissions)
        info.lineCount?.let {
          PropertyRow("Lines", "$it lines")
        }

        if (info.md5Hash != null) {
          HashRow("MD5", info.md5Hash, context)
        }
        if (info.sha256Hash != null) {
          HashRow("SHA-256", info.sha256Hash, context)
        }
      }
    }
  }
}

@Composable
private fun PropertyRow(label: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun HashRow(algorithm: String, hash: String, context: Context) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(10.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = algorithm,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = PurpleAccent
        )
        Text(
          text = hash,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2
        )
      }
      IconButton(
        onClick = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText(algorithm, hash)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(context, "$algorithm copied", Toast.LENGTH_SHORT).show()
        }
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = "Copy Hash",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
