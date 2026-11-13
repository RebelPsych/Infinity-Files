package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGridCard(
  item: FileItem,
  isSelected: Boolean,
  isSelectionMode: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isImage = item.category == FileCategory.IMAGE && item.isReadable
  val contentAlpha = if (item.isHidden) 0.55f else 1f

  Surface(
    modifier = modifier
      .aspectRatio(0.85f)
      .clip(RoundedCornerShape(12.dp))
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) PurpleAccent else Slate800,
        shape = RoundedCornerShape(12.dp)
      )
      .combinedClickable(
        onClick = { if (isSelectionMode) onLongClick() else onClick() },
        onLongClick = onLongClick
      )
      .testTag("grid_card_${item.name}"),
    color = if (isSelected) Slate800 else Slate900,
    shape = RoundedCornerShape(12.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Thumbnail or Black-filled White-outline Icon
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (item.isDirectory) {
            Image(
              painter = painterResource(id = R.drawable.ic_folder_white_outline),
              contentDescription = "Folder",
              modifier = Modifier.size(48.dp)
            )
          } else if (isImage) {
            AsyncImage(
              model = File(item.path),
              contentDescription = item.name,
              modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp)),
              contentScale = ContentScale.Crop
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.ic_file_white_outline),
              contentDescription = "File",
              modifier = Modifier.size(44.dp)
            )
          }
        }

        // Only the name, clean and clutter-free
        Text(
          text = item.name,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 15.sp
          ),
          color = PureWhite,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
        )
      }

      if (item.isHidden) {
        Icon(
          imageVector = Icons.Default.VisibilityOff,
          contentDescription = "Hidden",
          tint = Slate400,
          modifier = Modifier
            .size(14.dp)
            .align(Alignment.TopStart)
        )
      }

      if (isSelectionMode) {
        Icon(
          imageVector = Icons.Outlined.CheckCircle,
          contentDescription = if (isSelected) "Selected" else "Unselected",
          tint = if (isSelected) PurpleAccent else Slate800,
          modifier = Modifier
            .size(20.dp)
            .align(Alignment.TopEnd)
        )
      }
    }
  }
}
