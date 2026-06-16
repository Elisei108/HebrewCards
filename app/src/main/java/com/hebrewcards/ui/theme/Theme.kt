package com.hebrewcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Расширенные цвета приложения — доступны через LocalAppColors.current
data class AppColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isDark: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        background    = DarkBackground,
        surface       = DarkSurface,
        surface2      = DarkSurface2,
        border        = DarkBorder,
        textPrimary   = DarkTextPrimary,
        textSecondary = DarkTextSecondary,
        textTertiary  = DarkTextTertiary,
        isDark        = true
    )
}

private val darkColorScheme = darkColorScheme(
    primary      = ColorFlashcard,
    background   = DarkBackground,
    surface      = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface    = DarkTextPrimary,
    outline      = DarkBorder
)

private val lightColorScheme = lightColorScheme(
    primary      = ColorFlashcard,
    background   = LightBackground,
    surface      = LightSurface,
    onBackground = LightTextPrimary,
    onSurface    = LightTextPrimary,
    outline      = LightBorder
)

@Composable
fun HebrewCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

    val appColors = if (darkTheme) {
        AppColors(
            background    = DarkBackground,
            surface       = DarkSurface,
            surface2      = DarkSurface2,
            border        = DarkBorder,
            textPrimary   = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textTertiary  = DarkTextTertiary,
            isDark        = true
        )
    } else {
        AppColors(
            background    = LightBackground,
            surface       = LightSurface,
            surface2      = LightSurface2,
            border        = LightBorder,
            textPrimary   = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary  = LightTextTertiary,
            isDark        = false
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content
        )
    }
}
