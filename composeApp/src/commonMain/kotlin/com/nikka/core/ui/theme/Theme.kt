package com.nikka.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NikkaColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = LavenderPrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = MauveSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = MauveSecondaryDark,
    onSecondaryContainer = TextPrimary,
    tertiary = PeriwinkleAccent,
    onTertiary = TextPrimary,
    tertiaryContainer = PeriwinkleAccentDark,
    onTertiaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LavenderPrimaryDark,
    outlineVariant = DarkSurfaceVariant,
)

@Composable
fun NikkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NikkaColorScheme,
        typography = NikkaTypography(),
        content = content,
    )
}
