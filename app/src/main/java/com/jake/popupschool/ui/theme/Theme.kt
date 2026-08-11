package com.jake.popupschool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Deep indigo + warm gold accent, meant to read as a calm, premium palette. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A3F8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF1E1650),
    secondary = Color(0xFFB4883A),
    onSecondary = Color(0xFF3A2C00),
    secondaryContainer = Color(0xFFFBE6BB),
    onSecondaryContainer = Color(0xFF3A2C00),
    tertiary = Color(0xFF7A5265),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEE9F9),
    onSurfaceVariant = Color(0xFF48435A),
    outline = Color(0xFFC7C0DA),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BEFF),
    onPrimary = Color(0xFF2C2260),
    primaryContainer = Color(0xFF3E3478),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFFE7C685),
    onSecondary = Color(0xFF3A2C00),
    secondaryContainer = Color(0xFF564200),
    onSecondaryContainer = Color(0xFFFBE6BB),
    tertiary = Color(0xFFE8B6CE),
    background = Color(0xFF15121F),
    surface = Color(0xFF1D1A29),
    surfaceVariant = Color(0xFF2C2740),
    onSurfaceVariant = Color(0xFFCBC3DD),
    outline = Color(0xFF564F6E),
    error = Color(0xFFFFB4AB)
)

private val PremiumShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun PopupSchoolTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, shapes = PremiumShapes, content = content)
}
