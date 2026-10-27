package com.cordobot.criptopulse.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Up = Color(0xFF16C784)
val Down = Color(0xFFEA3943)

private val colors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Up,
    background = Color(0xFF0B0F19),
    onBackground = Color.White,
    surface = Color(0xFF131A2A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C2436),
    onSurfaceVariant = Color(0xFF9AA4B8)
)

private val typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
)

@Composable
fun CriptoPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
