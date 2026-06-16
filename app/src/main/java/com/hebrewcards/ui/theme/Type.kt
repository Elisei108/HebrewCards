package com.hebrewcards.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Шрифт Heebo для иврита (файлы в res/font/)
// Скачать: https://fonts.google.com/specimen/Heebo
// Нужны: heebo_regular.ttf и heebo_medium.ttf
val HeeboFontFamily = FontFamily(
    Font(com.hebrewcards.R.font.heebo_regular, FontWeight.Normal),
    Font(com.hebrewcards.R.font.heebo_medium, FontWeight.Medium)
)

// Размеры ивритского текста — переключаются в настройках
object HebrewTextSize {
    val Small  = 28.sp
    val Medium = 36.sp
    val Large  = 46.sp
}

// Стили для ивритского текста
object HebrewTextStyle {
    fun cardWord(sizeSp: Float) = TextStyle(
        fontFamily = HeeboFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = sizeSp.sp
    )

    val hint = TextStyle(
        fontFamily = HeeboFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    )

    val letterCheck = TextStyle(
        fontFamily = HeeboFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp
    )

    val input = TextStyle(
        fontFamily = HeeboFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    )

    val badge = TextStyle(
        fontFamily = HeeboFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    )
}
