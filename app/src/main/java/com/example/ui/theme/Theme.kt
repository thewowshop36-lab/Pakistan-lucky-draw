package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PakistanLotteryColorScheme = darkColorScheme(
  primary = GoldPrimary,
  onPrimary = Color(0xFF1F1600),
  primaryContainer = GoldContainer,
  onPrimaryContainer = GoldBright,
  secondary = EmeraldVibrant,
  onSecondary = Color.Black,
  secondaryContainer = EmeraldSurfaceVariant,
  onSecondaryContainer = Color(0xFFA7F3D0),
  tertiary = GoldBright,
  onTertiary = Color.Black,
  background = EmeraldDark,
  onBackground = TextPrimary,
  surface = EmeraldSurface,
  onSurface = TextPrimary,
  surfaceVariant = EmeraldSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = EmeraldBorder,
  outlineVariant = Color(0xFF285E40),
  error = AccentRuby,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For Pakistan Lottery luxury design, we commit to the tailored brand color scheme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = PakistanLotteryColorScheme,
    typography = Typography,
    content = content
  )
}
