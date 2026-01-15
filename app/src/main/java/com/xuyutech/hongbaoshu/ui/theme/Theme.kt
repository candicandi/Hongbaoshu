package com.xuyutech.hongbaoshu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8B4513),      // 传统的褐色/书本色
    onPrimary = Color.White,
    secondary = Color(0xFFD2B48C),    // 浅褐色
    onSecondary = Color.Black,
    background = Color(0xFFFAF5EE),   // 羊皮纸/米色背景
    onBackground = Color(0xFF2D2D2D), // 深灰色文字
    surface = Color(0xFFFAF5EE),
    onSurface = Color(0xFF2D2D2D),
    surfaceVariant = Color(0xFFE0D5C1), // 稍深一点的米色，用于控件背景
    onSurfaceVariant = Color(0xFF4A4A4A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA0522D),
    onPrimary = Color.White,
    secondary = Color(0xFF8B4513),
    onSecondary = Color.White,
    background = Color(0xFF1E1E1E),   // 深色背景
    onBackground = Color(0xFFE0E0E0), // 浅灰色文字
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0)
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
