package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PandaDarkColorScheme = darkColorScheme(
    primary = PandaPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PandaPurpleDark,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = PandaCyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = PandaGreenAccent,
    onTertiary = Color.Black,
    background = PandaBgDeep,
    onBackground = TextPrimary,
    surface = PandaBgCard,
    onSurface = TextPrimary,
    surfaceVariant = PandaBgCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBorder,
    error = PandaRedError,
    onError = Color.White
)

@Composable
fun PandaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PandaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatible name for existing previews or templates
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PandaTheme(content = content)
}
