package com.hebrewcards.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hebrewcards.BuildConfig
import com.hebrewcards.ui.theme.*
import com.hebrewcards.util.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onThemeChange:  (Boolean) -> Unit = {},
    onEngineChange: (String)  -> Unit = {}
) {
    val context = LocalContext.current
    val colors  = LocalAppColors.current

    val prefs = remember { context.getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE) }

    // Тема
    var isDarkTheme    by remember { mutableStateOf(prefs.getBoolean("is_dark_theme", true)) }
    // Выбранный TTS движок (пустая строка = системный по умолчанию)
    var selectedEngine by remember { mutableStateOf(prefs.getString("tts_engine", "") ?: "") }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Настройки", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Карточка 1 — Отображение
            Card(
                shape  = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text     = "Отображение",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color    = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = "Тёмная тема",
                            fontSize = 16.sp,
                            color    = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked         = isDarkTheme,
                            onCheckedChange = { isDark ->
                                isDarkTheme = isDark
                                onThemeChange(isDark)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = ColorFlashcard)
                        )
                    }
                }
            }

            // Карточка 2 — Звук (TTS)
            TtsCard(
                colors         = colors,
                selectedEngine = selectedEngine,
                onEngineChange = { pkg ->
                    selectedEngine = pkg
                    prefs.edit().putString("tts_engine", pkg).apply()
                    onEngineChange(pkg)
                }
            )

            // Карточка 3 — О приложении
            Card(
                shape  = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = "О приложении",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textSecondary,
                        modifier   = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Версия", fontSize = 16.sp, color = colors.textPrimary)
                        Text(BuildConfig.VERSION_NAME, fontSize = 16.sp, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

// Вынесен отдельно, чтобы TtsManager создавался единожды внутри remember
@Composable
private fun TtsCard(
    colors: AppColors,
    selectedEngine: String,
    onEngineChange: (String) -> Unit
) {
    val context = LocalContext.current
    val engines = remember {
        TtsManager(context.applicationContext).getAvailableEngines()
    }

    Card(
        shape  = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "Звук",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textSecondary,
                modifier   = Modifier.padding(bottom = 12.dp)
            )
            if (engines.isEmpty()) {
                Text(
                    text     = "Нет доступных TTS движков",
                    fontSize = 14.sp,
                    color    = colors.textSecondary
                )
            } else {
                engines.forEach { engine ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEngine == engine.name,
                            onClick  = { onEngineChange(engine.name) },
                            colors   = RadioButtonDefaults.colors(selectedColor = ColorFlashcard)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(engine.label, fontSize = 15.sp, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}
