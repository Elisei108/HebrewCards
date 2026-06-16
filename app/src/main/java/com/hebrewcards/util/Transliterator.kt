package com.hebrewcards.util

/**
 * Транслитерация иврита → латиница. Офлайн, без API.
 *
 * שָׁלוֹם → shalom
 * תַּפּוּחַ → tapuakh
 * בַּיִת → bayit
 */
object Transliterator {

    private val NIKUD_REGEX = Regex("[\u05B0-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]")

    private val LETTER_MAP = mapOf(
        'א' to "", 'ב' to "v", 'ג' to "g", 'ד' to "d", 'ה' to "h",
        'ו' to "v", 'ז' to "z", 'ח' to "kh", 'ט' to "t", 'י' to "y",
        'כ' to "kh", 'ך' to "kh", 'ל' to "l", 'מ' to "m", 'ם' to "m",
        'נ' to "n", 'ן' to "n", 'ס' to "s", 'ע' to "", 'פ' to "f",
        'ף' to "f", 'צ' to "ts", 'ץ' to "ts", 'ק' to "k", 'ר' to "r",
        'ש' to "sh", 'ת' to "t"
    )

    // Убирает огласовки — используется для hebrewPlain
    fun removeNikud(hebrew: String): String =
        hebrew.replace(NIKUD_REGEX, "").replace('\u05BC', ' ')
            .trim().replace("  ", " ")

    // Транслитерирует иврит в латиницу
    fun transliterate(hebrew: String): String {
        val result = StringBuilder()
        val chars = hebrew.toList()
        var i = 0
        while (i < chars.size) {
            val ch = chars[i]
            when {
                ch.code in 0x05B0..0x05C7 -> { /* огласовки — пропуск */ }

                // Дагеш меняет б→b, п→p, к→k
                ch == '\u05BC' -> {
                    val prev = chars.take(i).lastOrNull { it.code in 0x05D0..0x05EA }
                    when (prev) {
                        'ב' -> if (result.endsWith("v")) { result.deleteCharAt(result.length - 1); result.append("b") }
                        'פ' -> if (result.endsWith("f")) { result.deleteCharAt(result.length - 1); result.append("p") }
                        'כ' -> if (result.endsWith("kh")) { result.delete(result.length - 2, result.length); result.append("k") }
                    }
                }

                // שׂ (шин с точкой слева) → s
                ch == 'ש' && i + 1 < chars.size && chars[i + 1] == '\u05C2' -> result.append("s")

                ch.code in 0x05D0..0x05EA -> result.append(LETTER_MAP[ch] ?: "")

                else -> result.append(ch)
            }
            i++
        }
        return result.toString().replace("  ", " ").trim().lowercase()
    }
}
