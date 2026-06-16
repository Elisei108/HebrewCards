package com.hebrewcards

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hebrewcards.ui.navigation.HebrewCardsNavGraph
import com.hebrewcards.ui.theme.HebrewCardsTheme
import com.hebrewcards.util.TtsManager

private const val PREFS_NAME    = "hebrewcards_prefs"
private const val KEY_DARK_THEME = "is_dark_theme"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            // Читаем сохранённое значение темы; по умолчанию — тёмная
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean(KEY_DARK_THEME, true)) }

            // TTS-менеджер уровня Activity для смены движка без перезапуска
            val ttsManager = remember { TtsManager(applicationContext) }

            HebrewCardsTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HebrewCardsNavGraph(
                        onThemeChange  = { isDark ->
                            isDarkTheme = isDark
                            prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
                        },
                        onEngineChange = { pkg -> ttsManager.reinit(pkg) }
                    )
                }
            }
        }
    }
}
