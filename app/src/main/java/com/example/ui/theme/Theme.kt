package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighContrastSlateBlackColorScheme = darkColorScheme(
  primary = PureWhite,
  onPrimary = PureBlack,
  primaryContainer = Slate800,
  onPrimaryContainer = PureWhite,
  secondary = PurpleAccentLight,
  onSecondary = PureBlack,
  secondaryContainer = Slate850,
  onSecondaryContainer = PurpleAccentLight,
  tertiary = PurpleAccent,
  onTertiary = PureBlack,
  background = PureBlack,
  onBackground = PureWhite,
  surface = PureBlack,
  onSurface = PureWhite,
  surfaceVariant = Slate900,
  onSurfaceVariant = Slate300,
  outline = Slate700,
  outlineVariant = Slate800
)

@Composable
fun InfinityFilesTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = HighContrastSlateBlackColorScheme,
    typography = Typography,
    content = content
  )
}
