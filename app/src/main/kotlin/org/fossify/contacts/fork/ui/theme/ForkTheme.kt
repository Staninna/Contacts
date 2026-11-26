package org.fossify.contacts.fork.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive theme for the fork's screens.
 *
 * Colors come from the wallpaper (Material You) on Android 12+, with a hand-tuned
 * violet fallback below that. Motion uses the expressive spring scheme, shapes are a
 * notch rounder than the baseline, and [ForkTokens] holds the few extra values the
 * screens share (segmented list corners, avatar palettes).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalForkTokens provides ForkTokens(avatarPalettes = avatarPalettes(scheme)),
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = ForkShapes,
            content = content,
        )
    }
}

private val ForkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** A container color and the content color that reads on it. */
@Immutable
data class AvatarPalette(val container: Color, val content: Color)

@Immutable
data class ForkTokens(val avatarPalettes: List<AvatarPalette>)

val LocalForkTokens = staticCompositionLocalOf { ForkTokens(emptyList()) }

/** Avatar colors drawn from the theme, so letter avatars always harmonize with the wallpaper. */
private fun avatarPalettes(s: ColorScheme) = listOf(
    AvatarPalette(s.primaryContainer, s.onPrimaryContainer),
    AvatarPalette(s.secondaryContainer, s.onSecondaryContainer),
    AvatarPalette(s.tertiaryContainer, s.onTertiaryContainer),
    AvatarPalette(s.primaryFixedDim, s.onPrimaryFixed),
    AvatarPalette(s.tertiaryFixed, s.onTertiaryFixed),
    AvatarPalette(s.secondaryFixedDim, s.onSecondaryFixed),
)

@Composable
fun avatarPaletteFor(key: String): AvatarPalette {
    val palettes = LocalForkTokens.current.avatarPalettes
    if (palettes.isEmpty()) {
        return AvatarPalette(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
    return palettes[Math.floorMod(key.hashCode(), palettes.size)]
}

private val FallbackLight = lightColorScheme(
    primary = Color(0xFF65558F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF4D3D75),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF4A4458),
    tertiary = Color(0xFF7E5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF633B48),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F1FA),
    surfaceContainer = Color(0xFFF2ECF4),
    surfaceContainerHigh = Color(0xFFECE6EE),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCAC4CF),
)

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFCFBDFE),
    onPrimary = Color(0xFF36275D),
    primaryContainer = Color(0xFF4D3D75),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFCBC2DB),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF4A2532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F24),
    surfaceContainerHigh = Color(0xFF2B292F),
    surfaceContainerHighest = Color(0xFF36343A),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454E),
)
