package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332D00),
    onPrimaryContainer = OceanPrimaryDark,
    secondary = OceanSecondaryDark,
    onSecondary = Color.Black,
    background = Color(0xFF121212), // Black
    surface = Color(0xFF1E1E1E), // Dark Grey
    surfaceVariant = Color(0xFF2C2C2E), // Grey Variant
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFAAAAB4)
)

private val LightColorScheme = lightColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF9C4),
    onPrimaryContainer = Color(0xFF332D00),
    secondary = OceanSecondaryLight,
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF), // White
    surface = Color(0xFFF9F9F9), // Light Grey
    surfaceVariant = Color(0xFFEEEEEE), // Light Grey Variant
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF555555)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamicColor to ensure our bespoke Shades of Blue branding loads beautifully
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
