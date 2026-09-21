package com.rdbb.jiancircle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen80,
    onPrimary = Color(0xFF003828),
    primaryContainer = BrandGreenContainerDark,
    onPrimaryContainer = BrandGreenContainer,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenContainer,
    onPrimaryContainer = Color(0xFF003828),
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun JianCircleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 默认使用品牌绿，不跟随系统动态取色，保证视觉统一
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
