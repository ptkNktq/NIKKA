package com.nikka.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NikkaColorScheme = lightColorScheme(
    primary = PastelPink,
    onPrimary = TextOnLight,
    primaryContainer = PastelPinkLight,
    onPrimaryContainer = TextOnLight,
    secondary = PastelLavender,
    onSecondary = TextOnLight,
    secondaryContainer = PastelLavenderLight,
    onSecondaryContainer = TextOnLight,
    tertiary = PastelMint,
    onTertiary = TextOnLight,
    tertiaryContainer = PastelMintLight,
    onTertiaryContainer = TextOnLight,
    background = PastelBackground,
    onBackground = TextOnLight,
    surface = PastelSurface,
    onSurface = TextOnLight,
    surfaceVariant = PastelSurfaceVariant,
    onSurfaceVariant = TextOnLightVariant,
    outline = PastelPinkDark,
    outlineVariant = PastelPinkLight,
)

@Composable
fun NikkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NikkaColorScheme,
        typography = NikkaTypography(),
        content = content,
    )
}
