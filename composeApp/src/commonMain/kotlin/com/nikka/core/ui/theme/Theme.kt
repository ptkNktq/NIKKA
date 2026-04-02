package com.nikka.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NikkaColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = TextPrimary,
    primaryContainer = TealPrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = SageSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = SageSecondaryDark,
    onSecondaryContainer = TextPrimary,
    tertiary = LavenderAccent,
    onTertiary = TextPrimary,
    tertiaryContainer = LavenderAccentDark,
    onTertiaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TealPrimaryDark,
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
