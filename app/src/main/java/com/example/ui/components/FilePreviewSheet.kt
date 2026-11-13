package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleAccentLight
import com.example.ui.theme.Slate400
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
      // Minimal Header: just the name and actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = file.name,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = PureWhite,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        if (previewText != null) {
          if (isEditing) {
            Button(
              onClick = { onSaveContent(editedContent) },
              colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
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
              Icon(Icons.Default.Edit, contentDescription = "Edit File", tint = PurpleAccentLight)
            }
          }
        }

        IconButton(onClick = onShare) {
          Icon(Icons.Default.Share, contentDescription = "Share", tint = PureWhite)
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
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
                color = PureWhite
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
              val lines = previewText.lines()
              val lineNumbers = (1..lines.size).joinToString("\n")
              Text(
                text = lineNumbers,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Slate400,
                modifier = Modifier.padding(end = 12.dp)
              )

              Text(
                text = previewText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = PureWhite
              )
            }
          }
        }
      } else {
        // Non-text document fallback with direct Open/Share button
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = file.name,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onShare,
              colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
              Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open / Share")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
