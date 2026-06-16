package com.hebrewcards.ui.screen.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hebrewcards.ui.navigation.Routes
import com.hebrewcards.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeckScreen(
    navController: NavController,
    vm: AddDeckViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors  = LocalAppColors.current
    val clipboard = LocalClipboardManager.current

    // Когда импорт успешен — переходим на дашборд
    LaunchedEffect(uiState.importedDeckId) {
        uiState.importedDeckId?.let {
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Новая колода",
                        color      = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint               = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Поле названия колоды
            OutlinedTextField(
                value         = uiState.deckName,
                onValueChange = vm::onDeckNameChange,
                label         = { Text("Название колоды") },
                placeholder   = { Text("Например: Ульпан алеф · урок 1") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                colors = outlinedTextFieldColors(colors),
                shape  = RoundedCornerShape(14.dp)
            )

            // Подсказка по формату
            FormatHintCard(colors)

            // Поле для вставки CSV
            Column {
                Row(
                    modifier       = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Карточки (CSV)",
                        fontSize   = 14.sp,
                        color      = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    // Кнопка вставить из буфера обмена
                    TextButton(
                        onClick = {
                            val text = clipboard.getText()?.text
                            if (!text.isNullOrBlank()) {
                                vm.onCsvTextChange(text)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint     = ColorWriting
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Вставить", color = ColorWriting, fontSize = 14.sp)
                    }
                }

                OutlinedTextField(
                    value         = uiState.csvText,
                    onValueChange = vm::onCsvTextChange,
                    placeholder   = {
                        Text(
                            "שָׁלוֹם;мир, покой\nבַּיִת;дом\nמַיִם;вода",
                            color    = colors.textTertiary,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    colors = outlinedTextFieldColors(colors),
                    shape  = RoundedCornerShape(14.dp)
                )

                // Счётчик карточек
                val cardCount = uiState.csvText
                    .lines()
                    .count { it.trim().isNotBlank() && !it.trim().startsWith("#") }
                if (cardCount > 0) {
                    Text(
                        "$cardCount карточек",
                        fontSize = 12.sp,
                        color    = colors.textTertiary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            // Ошибка
            uiState.errorMessage?.let { error ->
                Text(
                    text     = error,
                    color    = ColorDestructive,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorDestructive.copy(alpha = 0.1f))
                        .padding(12.dp)
                )
            }

            // Кнопка импорта
            Button(
                onClick  = vm::importDeck,
                enabled  = !uiState.isLoading &&
                           uiState.deckName.isNotBlank() &&
                           uiState.csvText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = ColorFlashcard,
                    disabledContainerColor = colors.border
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Импортировать колоду",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Подсказка по формату CSV
@Composable
private fun FormatHintCard(colors: AppColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Формат карточек",
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.textSecondary
        )
        Text(
            "Одна карточка — одна строка:",
            fontSize = 12.sp,
            color    = colors.textTertiary
        )
        Text(
            "иврит;перевод",
            fontSize = 12.sp,
            color    = ColorWriting,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text(
            "Строки начинающиеся с # — комментарии.\nДля глагольной колоды первая строка: #VERB",
            fontSize = 12.sp,
            color    = colors.textTertiary
        )
    }
}

// Цвета для OutlinedTextField в нашей теме
@Composable
private fun outlinedTextFieldColors(colors: AppColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = ColorFlashcard,
        unfocusedBorderColor = colors.border,
        focusedLabelColor    = ColorFlashcard,
        unfocusedLabelColor  = colors.textSecondary,
        cursorColor          = ColorFlashcard,
        focusedTextColor     = colors.textPrimary,
        unfocusedTextColor   = colors.textPrimary,
        unfocusedContainerColor = colors.surface,
        focusedContainerColor   = colors.surface
    )
