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
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.domain.usecase.ExportDeckUseCase
import com.hebrewcards.domain.usecase.ExportResult
import com.hebrewcards.ui.theme.*
import com.hebrewcards.util.TtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onThemeChange:       (Boolean) -> Unit = {},
    onEngineChange:      (String)  -> Unit = {},
    onSpeedChange:       (Float)   -> Unit = {},
    onSessionSizeChange: (Int)     -> Unit = {}
) {
    val context = LocalContext.current
    val colors  = LocalAppColors.current

    val prefs = remember { context.getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE) }

    // Для экспорта колод
    val scope           = rememberCoroutineScope()
    val snackbarState   = remember { SnackbarHostState() }
    var isExporting     by remember { mutableStateOf(false) }
    val db              = remember { AppDatabase.getInstance(context) }
    val exportUseCase   = remember {
        ExportDeckUseCase(
            DeckRepository(db.deckDao(), db.cardDao(), db.progressDao()),
            db
        )
    }

    // Тема
    var isDarkTheme    by remember { mutableStateOf(prefs.getBoolean("is_dark_theme", true)) }
    // Выбранный TTS движок (пустая строка = системный по умолчанию)
    var selectedEngine by remember { mutableStateOf(prefs.getString("tts_engine", "") ?: "") }
    // Скорость речи (1.0f = нормально, 0.7f = медленно)
    var selectedSpeed       by remember { mutableStateOf(prefs.getFloat("tts_speed", 1.0f)) }
    // Количество слов за сессию (0 = все)
    var selectedSessionSize by remember { mutableStateOf(prefs.getInt("session_size", 0)) }

    Scaffold(
        containerColor = colors.background,
        snackbarHost   = { SnackbarHost(snackbarState) },
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
                    onEngineChange(pkg)
                },
                selectedSpeed  = selectedSpeed,
                onSpeedChange  = { rate ->
                    selectedSpeed = rate
                    onSpeedChange(rate)
                }
            )

            // Карточка 3 — Обучение
            Card(
                shape  = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = "Обучение",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textSecondary,
                        modifier   = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text     = "Количество слов за сессию",
                        fontSize = 13.sp,
                        color    = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    listOf(10 to "10 слов", 20 to "20 слов", 0 to "Все").forEach { (size, label) ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSessionSize == size,
                                onClick  = {
                                    selectedSessionSize = size
                                    onSessionSizeChange(size)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ColorFlashcard)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, color = colors.textPrimary)
                        }
                    }
                }
            }

            // Карточка 4 — Данные (экспорт)
            Card(
                shape  = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = "Данные",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = colors.textSecondary,
                        modifier   = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                val result = exportUseCase.exportAll(context)
                                isExporting = false
                                val message = when (result) {
                                    is ExportResult.Success ->
                                        "Экспортировано ${result.fileCount} колод → папка ${result.folderPath}"
                                    is ExportResult.Error   -> result.message
                                }
                                snackbarState.showSnackbar(message)
                            }
                        },
                        enabled  = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = ColorWriting)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color    = colors.background,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Экспорт...", color = colors.background)
                        } else {
                            Text("📤 Экспортировать колоды", color = colors.background)
                        }
                    }
                }
            }

            // Карточка 5 — О приложении
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
    onEngineChange: (String) -> Unit,
    selectedSpeed: Float,
    onSpeedChange: (Float) -> Unit
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

            // Список TTS движков
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

            // Секция скорости речи
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 8.dp),
                color     = colors.border
            )
            Text(
                text     = "Скорость речи",
                fontSize = 13.sp,
                color    = colors.textSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier          = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedSpeed == 1.0f,
                        onClick  = { onSpeedChange(1.0f) },
                        colors   = RadioButtonDefaults.colors(selectedColor = ColorFlashcard)
                    )
                    Text("Нормально", fontSize = 15.sp, color = colors.textPrimary)
                }
                Row(
                    modifier          = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedSpeed == 0.7f,
                        onClick  = { onSpeedChange(0.7f) },
                        colors   = RadioButtonDefaults.colors(selectedColor = ColorFlashcard)
                    )
                    Text("Медленно", fontSize = 15.sp, color = colors.textPrimary)
                }
            }
        }
    }
}
