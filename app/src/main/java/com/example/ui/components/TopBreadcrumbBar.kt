package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.example.model.BreadcrumbSegment
import com.example.model.StorageVolumeInfo
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun TopBreadcrumbBar(
  breadcrumbs: List<BreadcrumbSegment>,
  canGoUp: Boolean,
  onNavigateUp: () -> Unit,
  onBreadcrumbClick: (String) -> Unit,
  storageVolumes: List<StorageVolumeInfo>,
  currentPath: String,
  onSelectVolume: (String) -> Unit,
  onNewFolder: () -> Unit,
  onNewFile: () -> Unit,
  modifier: Modifier = Modifier
) {
  val breadcrumbScrollState = rememberScrollState()
  var partitionMenuExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(breadcrumbs) {
    if (breadcrumbs.isNotEmpty()) {
      breadcrumbScrollState.animateScrollTo(breadcrumbScrollState.maxValue)
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = PureBlack,
    tonalElevation = 0.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(top = 4.dp, bottom = 4.dp)
    ) {
      // Top Row: Navigation controls & Partition Switcher
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (canGoUp) {
          IconButton(
            onClick = onNavigateUp,
            modifier = Modifier
              .size(38.dp)
              .testTag("nav_up_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Up",
              tint = PureWhite
            )
          }
        }

        // Partition Switcher Button
        Box {
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { partitionMenuExpanded = true }
              .testTag("partition_switcher"),
            color = Slate900,
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Partitions",
                tint = PurpleAccent,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              val activeVolume = storageVolumes.find { currentPath.startsWith(it.path) }
              Text(
                text = activeVolume?.title ?: "Storage",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
              )
            }
          }

          DropdownMenu(
            expanded = partitionMenuExpanded,
            onDismissRequest = { partitionMenuExpanded = false },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
          ) {
            storageVolumes.forEach { volume ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text(
                      text = volume.title,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = PureWhite
                    )
                    Text(
                      text = volume.path,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                },
                onClick = {
                  partitionMenuExpanded = false
                  onSelectVolume(volume.path)
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Create Folder & Create File Quick Actions
        IconButton(
          onClick = onNewFolder,
          modifier = Modifier
            .size(38.dp)
            .testTag("action_new_folder")
        ) {
          Icon(
            imageVector = Icons.Default.CreateNewFolder,
            contentDescription = "New Folder",
            tint = PureWhite
          )
        }

        IconButton(
          onClick = onNewFile,
          modifier = Modifier
            .size(38.dp)
            .testTag("action_new_file")
        ) {
          Icon(
            imageVector = Icons.Default.NoteAdd,
            contentDescription = "New File",
            tint = PureWhite
          )
        }
      }

      // Breadcrumb Horizontal Path
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, bottom = 4.dp)
          .horizontalScroll(breadcrumbScrollState)
          .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        breadcrumbs.forEachIndexed { index, segment ->
          val isLast = index == breadcrumbs.lastIndex

          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { onBreadcrumbClick(segment.fullPath) }
              .testTag("breadcrumb_${segment.name}"),
            color = if (isLast) Slate900 else Color.Transparent,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = segment.name,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
              ),
              color = if (isLast) PureWhite else Slate700,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
          }

          if (!isLast) {
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = Slate700,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }
  }
}
