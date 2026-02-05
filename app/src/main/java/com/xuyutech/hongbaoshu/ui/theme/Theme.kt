package com.xuyutech.hongbaoshu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFA58B6F),
    onPrimary = Color(0xFFFDFBF7),
    secondary = Color(0xFF7D6A54),
    onSecondary = Color(0xFFF6F1E7),
    background = Color(0xFFF6F1E7),
    onBackground = Color(0xFF2E2A24),
    surface = Color(0xFFF2ECE0),
    onSurface = Color(0xFF2E2A24),
    surfaceVariant = Color(0xFFEFE8DB),
    onSurfaceVariant = Color(0xFF5F584F),
    tertiary = Color(0xFF9A6D4B),
    onTertiary = Color(0xFFF7F2E8),
    outline = Color(0xFFE1D7C8),
    outlineVariant = Color(0xFFDDD2C2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB59A7A),
    onPrimary = Color(0xFF1C1A17),
    secondary = Color(0xFF8B755C),
    onSecondary = Color(0xFF1C1A17),
    background = Color(0xFF1C1A17),
    onBackground = Color(0xFFEDE7DC),
    surface = Color(0xFF2B2721),
    onSurface = Color(0xFFEDE7DC),
    surfaceVariant = Color(0xFF25221D),
    onSurfaceVariant = Color(0xFFC6BDAF),
    tertiary = Color(0xFF9A6D4B),
    onTertiary = Color(0xFF1C1A17),
    outline = Color(0xFF3A342D),
    outlineVariant = Color(0xFF433B33)
)

@Composable
fun HongbaoshuTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
