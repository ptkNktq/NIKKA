package com.nikka.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import nikka.composeapp.generated.resources.Res
import nikka.composeapp.generated.resources.noto_sans_jp_regular
import org.jetbrains.compose.resources.Font

@Composable
fun NotoSansJpFamily(): FontFamily = FontFamily(
    Font(Res.font.noto_sans_jp_regular),
)

@Composable
fun NikkaTypography(): Typography {
    val fontFamily = NotoSansJpFamily()
    val default = Typography()
    return Typography(
        displayLarge = default.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = default.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = default.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = default.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = default.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = default.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = default.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = default.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = default.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = default.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = default.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = default.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = default.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = default.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = default.labelSmall.copy(fontFamily = fontFamily),
    )
}
