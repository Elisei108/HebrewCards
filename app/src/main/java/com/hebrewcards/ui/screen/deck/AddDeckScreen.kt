package com.hebrewcards.ui.screen.deck

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hebrewcards.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeckScreen(
    navController: NavController,
    vm: AddDeckViewModel = viewModel()
) {
    val uiState   by vm.uiState.collectAsStateWithLifecycle()
    val colors    = LocalAppColors.current
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current

    // Переходим назад когда импорт успешен
    LaunchedEffect(uiState.importedDeckId) {
        uiState.importedDeckId?.let { navController.popBackStack() }
    }

    // Лаунчер системного файлового пикера — чтение файла делегируется в ViewModel на IO-потоке
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> vm.importFromUri(context, uri) }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Новая колода", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Три способа добавить колоду ──────────────────────────────
            Text(
                "Выберите способ",
                fontSize   = 13.sp,
                color      = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Кнопка: вручную
                MethodButton(
                    emoji    = "✍️",
                    label    = "Вручную",
                    color    = ColorFlashcard,
                    modifier = Modifier.weight(1f),
                    onClick  = { vm.openManualSheet() }
                )
                // Кнопка: из файла
                MethodButton(
                    emoji    = "📂",
                    label    = "Из файла",
                    color    = ColorWriting,
                    modifier = Modifier.weight(1f),
                    onClick  = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"   // показываем все файлы, фильтр по .csv не все пикеры поддерживают
                        }
                        filePicker.launch(intent)
                    }
                )
            }

            // ── Разделитель ──────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
                Text("или вставьте CSV", fontSize = 12.sp, color = colors.textTertiary)
                HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
            }

            // ── CSV-вставка ──────────────────────────────────────────────

            // Название колоды
            OutlinedTextField(
                value         = uiState.deckName,
                onValueChange = vm::onDeckNameChange,
                label         = { Text("Название колоды") },
                placeholder   = { Text("Например: Ульпан алеф · урок 1") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = outlinedTextFieldColors(colors),
                shape  = RoundedCornerShape(14.dp)
            )

            // Поле CSV
            Column {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Карточки (иврит;перевод)",
                        fontSize   = 13.sp,
                        color      = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = {
                        clipboard.getText()?.text?.takeIf { it.isNotBlank() }
                            ?.let { vm.onCsvTextChange(it) }
                    }) {
                        Icon(
                            Icons.Default.ContentPaste,
                            null,
                            modifier = Modifier.size(15.dp),
                            tint     = ColorWriting
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Вставить", color = ColorWriting, fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value         = uiState.csvText,
                    onValueChange = vm::onCsvTextChange,
                    placeholder   = {
                        Text(
                            "שָׁלוֹם;мир\nבַּיִת;дом\nמַיִם;вода",
                            color    = colors.textTertiary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 360.dp),
                    colors = outlinedTextFieldColors(colors),
                    shape  = RoundedCornerShape(14.dp)
                )

                val cardCount = uiState.csvText.lines()
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

            // Ошибка CSV-импорта
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

            // Кнопка импорта CSV
            Button(
                onClick  = vm::importDeck,
                enabled  = !uiState.isLoading &&
                           uiState.deckName.isNotBlank() &&
                           uiState.csvText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = ColorFlashcard,
                    disabledContainerColor = colors.border
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
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

    // ── Нижний лист ручного добавления ───────────────────────────────────
    if (uiState.showManualSheet) {
        ManualAddSheet(
            uiState  = uiState,
            vm       = vm,
            onDismiss = { vm.closeManualSheet() }
        )
    }
}

// ── Кнопка способа добавления ────────────────────────────────────────────────

@Composable
private fun MethodButton(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(64.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Нижний лист ручного добавления ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAddSheet(
    uiState: AddDeckUiState,
    vm: AddDeckViewModel,
    onDismiss: () -> Unit
) {
    val colors      = LocalAppColors.current
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = colors.surface,
        dragHandle       = {
            // Небольшой хэндл сверху
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Text(
                "✍️  Добавить карточки вручную",
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary,
                modifier   = Modifier.padding(top = 8.dp)
            )

            // Название колоды
            OutlinedTextField(
                value         = uiState.manualDeckName,
                onValueChange = vm::onManualDeckNameChange,
                label         = { Text("Название колоды") },
                placeholder   = { Text("Например: Мои слова") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = outlinedTextFieldColors(colors),
                shape  = RoundedCornerShape(14.dp)
            )

            // Список пар
            uiState.manualCards.forEach { pair ->
                ManualCardRow(
                    pair       = pair,
                    canDelete  = uiState.manualCards.size > 1,
                    onHebrewChange  = { vm.onManualHebrewChange(pair.id, it) },
                    onRussianChange = { vm.onManualRussianChange(pair.id, it) },
                    onDelete        = { vm.removeManualCard(pair.id) }
                )
            }

            // Кнопка "+ Ещё карточку"
            OutlinedButton(
                onClick  = { vm.addManualCard() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = ColorFlashcard.copy(alpha = 0.06f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorFlashcard.copy(alpha = 0.35f))
            ) {
                Icon(Icons.Default.Add, null, tint = ColorFlashcard, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ещё карточку", color = ColorFlashcard, fontSize = 14.sp)
            }

            // Счётчик
            val filledCount = uiState.manualCards.count {
                it.hebrew.isNotBlank() && it.russian.isNotBlank()
            }
            if (filledCount > 0) {
                Text(
                    "$filledCount ${cardWord(filledCount)} готово",
                    fontSize  = 12.sp,
                    color     = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }

            // Ошибка ручного добавления
            uiState.manualError?.let { error ->
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

            // Кнопка сохранить
            Button(
                onClick  = { vm.saveManualDeck() },
                enabled  = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = ColorFlashcard,
                    disabledContainerColor = colors.border
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Сохранить колоду",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

// ── Одна строка: иврит + перевод ─────────────────────────────────────────────

@Composable
private fun ManualCardRow(
    pair: ManualCardPair,
    canDelete: Boolean,
    onHebrewChange: (String) -> Unit,
    onRussianChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface2)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Верхняя строка: номер пары + кнопка удалить
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "🇮🇱  Слово на иврите",
                fontSize = 12.sp,
                color    = colors.textSecondary
            )
            if (canDelete) {
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Удалить",
                        tint     = colors.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Поле иврита — RTL, шрифт Heebo
        OutlinedTextField(
            value         = pair.hebrew,
            onValueChange = onHebrewChange,
            placeholder   = { Text("שָׁלוֹם", color = colors.textTertiary, fontFamily = HeeboFontFamily) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(
                fontFamily  = HeeboFontFamily,
                fontSize    = 20.sp,
                textAlign   = TextAlign.Right
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = outlinedTextFieldColors(colors),
            shape  = RoundedCornerShape(10.dp)
        )

        Text(
            "🇷🇺  Перевод",
            fontSize = 12.sp,
            color    = colors.textSecondary
        )

        // Поле перевода
        OutlinedTextField(
            value         = pair.russian,
            onValueChange = onRussianChange,
            placeholder   = { Text("мир, покой", color = colors.textTertiary) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            colors = outlinedTextFieldColors(colors),
            shape  = RoundedCornerShape(10.dp)
        )
    }
}

// ── Вспомогательные ──────────────────────────────────────────────────────────

// Склонение слова "карточка"
private fun cardWord(n: Int): String = when {
    n % 100 in 11..19          -> "карточек"
    n % 10 == 1                -> "карточка"
    n % 10 in 2..4             -> "карточки"
    else                       -> "карточек"
}

// Цвета для OutlinedTextField в нашей теме
@Composable
private fun outlinedTextFieldColors(colors: AppColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = ColorFlashcard,
        unfocusedBorderColor    = colors.border,
        focusedLabelColor       = ColorFlashcard,
        unfocusedLabelColor     = colors.textSecondary,
        cursorColor             = ColorFlashcard,
        focusedTextColor        = colors.textPrimary,
        unfocusedTextColor      = colors.textPrimary,
        unfocusedContainerColor = colors.surface,
        focusedContainerColor   = colors.surface
    )
