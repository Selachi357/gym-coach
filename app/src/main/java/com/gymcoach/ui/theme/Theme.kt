package com.gymcoach.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.gymcoach.ui.hero.PixelFont
import com.gymcoach.ui.hero.Retro

/** One retro palette for the whole app, like the menus of an old 16-bit RPG. */
private val RetroColors = darkColorScheme(
    primary = Retro.gold,
    onPrimary = Retro.night,
    primaryContainer = Color(0xFF3A3360),
    onPrimaryContainer = Retro.parchment,
    secondary = Retro.xp,
    onSecondary = Retro.night,
    secondaryContainer = Color(0xFF243D33),
    onSecondaryContainer = Retro.parchment,
    tertiary = Retro.ember,
    onTertiary = Retro.night,
    tertiaryContainer = Color(0xFF4A2A1E),
    onTertiaryContainer = Retro.parchment,
    background = Color(0xFF12121F),
    onBackground = Retro.parchment,
    surface = Color(0xFF12121F),
    onSurface = Retro.parchment,
    surfaceVariant = Retro.panel,
    onSurfaceVariant = Color(0xFFC9BC9F),
    surfaceContainerLowest = Color(0xFF0D0D18),
    surfaceContainerLow = Retro.night,
    surfaceContainer = Retro.night,
    surfaceContainerHigh = Retro.panel,
    surfaceContainerHighest = Color(0xFF34345A),
    inverseSurface = Retro.parchment,
    inverseOnSurface = Retro.night,
    outline = Color(0xFF9C8A55),
    outlineVariant = Color(0xFF4A4A70),
    error = Color(0xFFFF5A5A),
    onError = Retro.night,
    errorContainer = Color(0xFF5A1E1E),
    onErrorContainer = Retro.parchment,
)

private fun pixel(size: Int) = TextStyle(fontFamily = PixelFont, fontSize = size.sp, lineHeight = (size * 1.7).sp)

/** Press Start 2P is wide, so every size is a few steps smaller than Material's defaults. */
private val RetroType = Typography(
    displayLarge = pixel(28),
    displayMedium = pixel(24),
    displaySmall = pixel(20),
    headlineLarge = pixel(18),
    headlineMedium = pixel(16),
    headlineSmall = pixel(14),
    titleLarge = pixel(13),
    titleMedium = pixel(11),
    titleSmall = pixel(10),
    bodyLarge = pixel(11),
    bodyMedium = pixel(9),
    bodySmall = pixel(8),
    labelLarge = pixel(10),
    labelMedium = pixel(8),
    labelSmall = pixel(7),
)

/** Square corners everywhere: no rounded shapes in a pixel world. */
private val RetroShapes = Shapes(
    extraSmall = RoundedCornerShape(0),
    small = RoundedCornerShape(0),
    medium = RoundedCornerShape(0),
    large = RoundedCornerShape(0),
    extraLarge = RoundedCornerShape(0),
)

@Composable
fun GymCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RetroColors, typography = RetroType, shapes = RetroShapes, content = content)
}
