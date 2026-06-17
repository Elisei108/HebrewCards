package com.hebrewcards.ui.screen.study

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hebrewcards.data.db.entity.Card
import com.hebrewcards.domain.model.StudyMode
import com.hebrewcards.ui.theme.*
import com.hebrewcards.util.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Вспомогательные типы для проверки написания/диктанта ────────────────────

private sealed class CheckResult {
    object Correct : CheckResult()
    data class Wrong(val diff: List<DiffChar>) : CheckResult()
}

private data class DiffChar(val char: Char, val kind: DiffKind)

private enum class DiffKind { CORRECT, WRONG, MISSING }

// Убираем всё кроме ивритских букв (U+05D0..U+05EA) и одиночных пробелов
private fun stripNikud(text: String): String =
    text.filter { it.code in 0x05D0..0x05EA || it == ' ' }
        .trim()
        .replace(Regex(" {2,}"), " ")

// Побуквенное сравнение: выравниваем ввод пользователя с правильным ответом
private fun buildDiff(user: String, correct: String): List<DiffChar> {
    val maxLen = maxOf(user.length, correct.length)
    return (0 until maxLen).map { i ->
        val u = user.getOrNull(i)
        val c = correct.getOrNull(i)
        when {
            u != null && c != null -> DiffChar(u, if (u == c) DiffKind.CORRECT else DiffKind.WRONG)
            u != null              -> DiffChar(u, DiffKind.WRONG)     // лишняя буква пользователя
            else                   -> DiffChar(c!!, DiffKind.MISSING)  // недостающая буква
        }
    }
}

// ─── Главный экран ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    deckId: Long,
    mode: StudyMode,
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val vm: StudyViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StudyViewModel(application, deckId, mode) as T
        }
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    // ttsReady: нужен для автовоспроизведения в режиме диктанта
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember { TtsManager(application) }
    DisposableEffect(Unit) {
        // Читаем настройки движка и скорости из SharedPreferences
        val prefs = application.getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE)
        val enginePackage = prefs.getString("tts_engine", "").orEmpty().takeIf { it.isNotBlank() }
        val speed = prefs.getFloat("tts_speed", 1.0f)
        tts.init(enginePackage = enginePackage, onReady = { tts.setSpeed(speed); ttsReady = true })
        onDispose { tts.shutdown() }
    }

    val colors    = LocalAppColors.current
    val modeColor = modeAccentColor(mode)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text       = modeName(mode),
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
                LinearProgressIndicator(
                    progress   = { state.progressFraction },
                    modifier   = Modifier.fillMaxWidth(),
                    color      = modeColor,
                    trackColor = colors.border,
                    strokeCap  = StrokeCap.Round
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator(color = modeColor)
                }
            }
            state.isComplete -> {
                SessionCompleteContent(state, navController, padding)
            }
            state.current != null -> {
                // key сбрасывает remember-состояние при смене карточки
                when (mode) {
                    StudyMode.WRITING -> key(state.current!!.card.id) {
                        WritingContent(
                            studyCard     = state.current!!,
                            answered      = state.answered,
                            initialTotal  = state.initialTotal,
                            padding       = padding,
                            onCorrect     = vm::swipeRight,
                            onRecordError = vm::recordError,
                            onSpeak       = { tts.speak(state.current!!.card.hebrew) }
                        )
                    }
                    StudyMode.DICTATION -> key(state.current!!.card.id) {
                        DictationContent(
                            studyCard     = state.current!!,
                            answered      = state.answered,
                            initialTotal  = state.initialTotal,
                            padding       = padding,
                            ttsReady      = ttsReady,
                            onCorrect     = vm::swipeRight,
                            onRecordError = vm::recordError,
                            onSpeak       = { tts.speak(state.current!!.card.hebrew) }
                        )
                    }
                    StudyMode.CHOICE -> key(state.current!!.card.id) {
                        ChoiceContent(
                            studyCard    = state.current!!,
                            answered     = state.answered,
                            initialTotal = state.initialTotal,
                            allCards     = state.allCards,
                            padding      = padding,
                            onCorrect    = vm::swipeRight,
                            onWrong      = { vm.swipeLeft() },
                            onSpeak      = { tts.speak(state.current!!.card.hebrew) },
                            onBack       = { navController.popBackStack() }
                        )
                    }
                    StudyMode.FLASHCARD -> FlashcardContent(
                        state        = state,
                        padding      = padding,
                        onFlip       = vm::flip,
                        onSwipeRight = vm::swipeRight,
                        onSwipeLeft  = vm::swipeLeft,
                        onSpeak      = { tts.speak(state.current!!.card.hebrew) }
                    )
                }
            }
        }
    }
}

// ─── Режим диктанта ───────────────────────────────────────────────────────────

@Composable
private fun DictationContent(
    studyCard: StudyCard,
    answered: Int,
    initialTotal: Int,
    padding: PaddingValues,
    ttsReady: Boolean,
    onCorrect: () -> Unit,
    onRecordError: () -> Unit,
    onSpeak: () -> Unit
) {
    val card     = studyCard.card
    val colors   = LocalAppColors.current
    val keyboard = LocalSoftwareKeyboardController.current

    var inputText   by rememberSaveable { mutableStateOf("") }
    var checkResult by remember { mutableStateOf<CheckResult?>(null) }

    val focusRequester = remember { FocusRequester() }

    // Исключение из правила "TTS только по кнопке": диктант воспроизводит слово автоматически.
    // LaunchedEffect(ttsReady) перезапускается при смене карточки (key сбросил composable),
    // поэтому каждая новая карточка будет воспроизведена, как только TTS готов.
    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            delay(150)
            onSpeak()
        }
    }

    // Клавиатура появляется после старта воспроизведения
    LaunchedEffect(Unit) {
        delay(400)
        focusRequester.requestFocus()
    }

    fun doCheck() {
        val normalized = stripNikud(inputText)
        if (normalized == card.hebrewPlain) {
            keyboard?.hide()
            checkResult = CheckResult.Correct
        } else {
            checkResult = CheckResult.Wrong(buildDiff(normalized, card.hebrewPlain))
            onRecordError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Счётчик
        Text(
            text     = "${answered + 1} / $initialTotal",
            fontSize = 13.sp,
            color    = colors.textSecondary
        )

        Spacer(Modifier.height(32.dp))

        // Большая кнопка воспроизведения (80dp, фиолетовый акцент)
        Box(
            modifier         = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ColorDictation)
                .clickable(onClick = onSpeak),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.PlayArrow,
                contentDescription = "Воспроизвести",
                tint               = Color.White,
                modifier           = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text     = "Нажми для повтора",
            fontSize = 12.sp,
            color    = colors.textTertiary
        )

        Spacer(Modifier.height(32.dp))

        // Поле ввода иврита — RTL, шрифт Heebo, фокус акцент фиолетовый
        OutlinedTextField(
            value         = inputText,
            onValueChange = { inputText = it },
            placeholder   = {
                Text(
                    text      = "כתוב כאן...",
                    style     = HebrewTextStyle.input,
                    color     = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            },
            textStyle       = HebrewTextStyle.input.copy(textAlign = TextAlign.Center),
            modifier        = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled         = checkResult == null,
            singleLine      = true,
            shape           = RoundedCornerShape(14.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = ColorDictation,
                unfocusedBorderColor = colors.border,
                disabledBorderColor  = colors.border,
                focusedTextColor     = colors.textPrimary,
                unfocusedTextColor   = colors.textPrimary,
                disabledTextColor    = colors.textPrimary,
                cursorColor          = ColorDictation
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { doCheck() })
        )

        Spacer(Modifier.height(16.dp))

        // Кнопка проверки или результат (через общий компонент)
        if (checkResult == null) {
            Button(
                onClick  = ::doCheck,
                enabled  = inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ColorDictation)
            ) {
                Text("Проверить", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        } else {
            LetterByLetterFeedback(
                result      = checkResult!!,
                hebrewPlain = card.hebrewPlain,
                accentColor = ColorDictation,
                onCorrect   = onCorrect,
                onRetry     = {
                    inputText   = ""
                    checkResult = null
                    focusRequester.requestFocus()
                },
                // В диктанте русский перевод раскрывается только после правильного ответа
                revealContent = {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = card.russian,
                        fontSize  = 20.sp,
                        color     = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Режим написания ──────────────────────────────────────────────────────────

@Composable
private fun WritingContent(
    studyCard: StudyCard,
    answered: Int,
    initialTotal: Int,
    padding: PaddingValues,
    onCorrect: () -> Unit,
    onRecordError: () -> Unit,
    onSpeak: () -> Unit
) {
    val card    = studyCard.card
    val colors  = LocalAppColors.current
    val keyboard = LocalSoftwareKeyboardController.current

    var inputText    by rememberSaveable { mutableStateOf("") }
    var checkResult  by remember { mutableStateOf<CheckResult?>(null) }
    var showTranslit by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        focusRequester.requestFocus()
    }

    fun doCheck() {
        val normalized = stripNikud(inputText)
        if (normalized == card.hebrewPlain) {
            keyboard?.hide()
            checkResult = CheckResult.Correct
        } else {
            checkResult = CheckResult.Wrong(buildDiff(normalized, card.hebrewPlain))
            onRecordError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text     = "${answered + 1} / $initialTotal",
            fontSize = 13.sp,
            color    = colors.textSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Русский перевод (виден сразу) + TTS по кнопке
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text       = card.russian,
                fontSize   = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp)
            )
            IconButton(
                onClick  = onSpeak,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.VolumeUp, "Произнести", tint = colors.textSecondary)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Транслитерация — скрыта по умолчанию
        if (showTranslit) {
            Text(
                text      = card.transliteration,
                style     = HebrewTextStyle.hint,
                color     = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
        }
        TextButton(onClick = { showTranslit = !showTranslit }) {
            Text(
                text     = if (showTranslit) "Скрыть подсказку" else "Показать транслитерацию",
                color    = ColorWriting,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = inputText,
            onValueChange = { inputText = it },
            placeholder   = {
                Text(
                    text      = "כתוב כאן...",
                    style     = HebrewTextStyle.input,
                    color     = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            },
            textStyle       = HebrewTextStyle.input.copy(textAlign = TextAlign.Center),
            modifier        = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled         = checkResult == null,
            singleLine      = true,
            shape           = RoundedCornerShape(14.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = ColorWriting,
                unfocusedBorderColor = colors.border,
                disabledBorderColor  = colors.border,
                focusedTextColor     = colors.textPrimary,
                unfocusedTextColor   = colors.textPrimary,
                disabledTextColor    = colors.textPrimary,
                cursorColor          = ColorWriting
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { doCheck() })
        )

        Spacer(Modifier.height(16.dp))

        if (checkResult == null) {
            Button(
                onClick  = ::doCheck,
                enabled  = inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ColorWriting)
            ) {
                Text("Проверить", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        } else {
            LetterByLetterFeedback(
                result      = checkResult!!,
                hebrewPlain = card.hebrewPlain,
                accentColor = ColorWriting,
                onCorrect   = onCorrect,
                onRetry     = {
                    inputText   = ""
                    checkResult = null
                    focusRequester.requestFocus()
                }
                // revealContent не передаём: в написании русский виден заранее
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Общий компонент разбора ответа (написание + диктант) ────────────────────

@Composable
private fun LetterByLetterFeedback(
    result: CheckResult,
    hebrewPlain: String,
    accentColor: Color,                               // цвет кнопки "Ещё раз" = цвет режима
    onCorrect: () -> Unit,
    onRetry: () -> Unit,
    revealContent: (@Composable () -> Unit)? = null   // контент после "Правильно!" (для диктанта)
) {
    val colors = LocalAppColors.current
    when (result) {
        is CheckResult.Correct -> {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint               = ColorFlashcard,
                    modifier           = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Правильно!",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = ColorFlashcard
                )
            }
            revealContent?.invoke()
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = onCorrect,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ColorFlashcard)
            ) {
                Text("Следующее слово →", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        is CheckResult.Wrong -> {
            DiffDisplay(result.diff)
            Spacer(Modifier.height(10.dp))
            Text(
                text      = "Правильно: $hebrewPlain",
                style     = HebrewTextStyle.hint,
                color     = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Ещё раз", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Побуквенный разбор: RTL Row, цвета из дизайн-системы
@Composable
private fun DiffDisplay(diff: List<DiffChar>) {
    val colors = LocalAppColors.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            diff.forEach { dc ->
                val (color, decoration) = when (dc.kind) {
                    DiffKind.CORRECT -> Pair(ColorCorrectLetter, TextDecoration.None)
                    DiffKind.WRONG   -> Pair(ColorWrongLetter,   TextDecoration.None)
                    DiffKind.MISSING -> Pair(colors.textTertiary, TextDecoration.Underline)
                }
                Text(
                    text     = dc.char.toString(),
                    style    = HebrewTextStyle.letterCheck.copy(
                        color          = color,
                        textDecoration = decoration
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

// ─── Режим выбора из 4 ───────────────────────────────────────────────────────

// Генерирует 4 варианта: 1 правильный + 3 случайных из колоды; если не хватает — дублируем
private fun buildOptions(card: Card, all: List<Card>): List<String> {
    val correct     = card.russian
    val distractors = all
        .filter { it.id != card.id }
        .map { it.russian }
        .distinct()
        .filter { it != correct }
        .shuffled()

    val result = mutableListOf(correct)
    var suffix = 2
    var idx    = 0
    while (result.size < 4) {
        val next = distractors.getOrNull(idx++)
        if (next != null) result.add(next)
        else { result.add("$correct ($suffix)"); suffix++ }
    }
    return result.shuffled()
}

@Composable
private fun ChoiceContent(
    studyCard: StudyCard,
    answered: Int,
    initialTotal: Int,
    allCards: List<Card>,
    padding: PaddingValues,
    onCorrect: () -> Unit,
    onWrong: () -> Unit,
    onSpeak: () -> Unit,
    onBack: () -> Unit
) {
    val card   = studyCard.card
    val colors = LocalAppColors.current
    val scope  = rememberCoroutineScope()

    // Пустая колода — защита от крайнего случая
    if (allCards.isEmpty()) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Нет новых слов для изучения", fontSize = 18.sp, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onBack,
                colors  = ButtonDefaults.buttonColors(containerColor = ColorChoice)
            ) {
                Text("← Назад", color = Color.White)
            }
        }
        return
    }

    var options  by remember { mutableStateOf(listOf<String>()) }
    var selected by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(card.id) { options = buildOptions(card, allCards) }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${answered + 1} / $initialTotal", fontSize = 13.sp, color = colors.textSecondary)

        Spacer(Modifier.height(32.dp))

        // Ивритское слово крупным шрифтом
        Text(
            text      = card.hebrew,
            style     = HebrewTextStyle.cardWord(46f),
            color     = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Кнопка TTS
        IconButton(onClick = onSpeak) {
            @Suppress("DEPRECATION")
            Icon(Icons.Default.VolumeUp, "Произнести", tint = colors.textSecondary)
        }

        Spacer(Modifier.height(32.dp))

        // Сетка 2×2
        if (options.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { option ->
                            val isCorrect  = option == card.russian
                            val isSelected = option == selected

                            val bgColor     = when {
                                selected == null -> colors.surface
                                isCorrect        -> ColorFlashcard.copy(alpha = 0.15f)
                                isSelected       -> ColorWrongLetter.copy(alpha = 0.15f)
                                else             -> colors.surface
                            }
                            val borderColor = when {
                                selected == null -> colors.border
                                isCorrect        -> ColorFlashcard
                                isSelected       -> ColorWrongLetter
                                else             -> colors.border
                            }
                            val textColor   = when {
                                selected == null -> colors.textPrimary
                                isCorrect        -> ColorFlashcard
                                isSelected       -> ColorWrongLetter
                                else             -> colors.textSecondary
                            }

                            OutlinedButton(
                                onClick = {
                                    if (selected != null) return@OutlinedButton
                                    selected = option
                                    scope.launch {
                                        if (isCorrect) { delay(600); onCorrect() }
                                        else           { delay(1200); onWrong() }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(72.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    containerColor         = bgColor,
                                    disabledContainerColor = bgColor
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Text(
                                    text      = option,
                                    color     = textColor,
                                    textAlign = TextAlign.Center,
                                    fontSize  = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Режим флипкарт ───────────────────────────────────────────────────────────

@Composable
private fun FlashcardContent(
    state: StudyUiState,
    padding: PaddingValues,
    onFlip: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSpeak: () -> Unit
) {
    val colors = LocalAppColors.current
    val card   = state.current!!

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text     = "${state.answered + 1} / ${state.initialTotal}",
            fontSize = 13.sp,
            color    = colors.textSecondary
        )

        Spacer(Modifier.weight(1f))

        SwipeableFlipCard(
            studyCard    = card,
            isFlipped    = state.isFlipped,
            onFlip       = onFlip,
            onSwipeRight = onSwipeRight,
            onSwipeLeft  = onSwipeLeft,
            onSpeak      = onSpeak
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text     = if (state.isFlipped) "Свайп или кнопки ниже" else "Нажми, чтобы перевернуть",
            fontSize = 13.sp,
            color    = colors.textTertiary
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = onSwipeLeft,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, ColorChoice)
            ) {
                Text("← Повторить", color = ColorChoice, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick  = onSwipeRight,
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ColorFlashcard)
            ) {
                Text("Знаю →", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SwipeableFlipCard(
    studyCard: StudyCard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSpeak: () -> Unit
) {
    val density        = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }
    val cardOffset     = remember { Animatable(0f) }
    val scope          = rememberCoroutineScope()

    LaunchedEffect(studyCard.card.id) { cardOffset.snapTo(0f) }

    val offsetVal      = cardOffset.value
    val overlayAlpha   = (abs(offsetVal) / 300f).coerceIn(0f, 0.45f)
    val isSwipingRight = offsetVal > 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .offset { IntOffset(offsetVal.roundToInt(), 0) }
            .graphicsLayer { rotationZ = (offsetVal / 25f).coerceIn(-12f, 12f) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                cardOffset.value > swipeThreshold -> {
                                    cardOffset.animateTo(1200f, tween(220))
                                    onSwipeRight()
                                }
                                cardOffset.value < -swipeThreshold -> {
                                    cardOffset.animateTo(-1200f, tween(220))
                                    onSwipeLeft()
                                }
                                else -> cardOffset.animateTo(
                                    0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    },
                    onDragCancel     = { scope.launch { cardOffset.animateTo(0f) } },
                    onHorizontalDrag = { _, delta ->
                        scope.launch { cardOffset.snapTo(cardOffset.value + delta) }
                    }
                )
            }
            .pointerInput(Unit) { detectTapGestures { onFlip() } }
    ) {
        FlipCardSurface(card = studyCard.card, isFlipped = isFlipped, onSpeak = onSpeak)

        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        (if (isSwipingRight) ColorFlashcard else ColorChoice).copy(alpha = overlayAlpha)
                    )
            )
        }
        if (overlayAlpha > 0.12f) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Text(
                    text       = if (isSwipingRight) "Знаю ✓" else "Повторить ↺",
                    color      = Color.White,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FlipCardSurface(card: Card, isFlipped: Boolean, onSpeak: () -> Unit) {
    val colors  = LocalAppColors.current
    val density = LocalDensity.current

    val flipAngle by animateFloatAsState(
        targetValue   = if (isFlipped) 180f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label         = "flip"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .graphicsLayer {
                rotationY      = flipAngle
                cameraDistance = 12f * density.density
            }
            .background(colors.surface)
    ) {
        if (flipAngle <= 90f) {
            FrontContent(card = card, onSpeak = onSpeak)
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                BackContent(card = card)
            }
        }
    }
}

@Composable
private fun FrontContent(card: Card, onSpeak: () -> Unit) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text      = card.hebrew,
                style     = HebrewTextStyle.cardWord(HebrewTextSize.Large.value),
                color     = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
        IconButton(
            onClick  = onSpeak,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
        ) {
            Icon(Icons.Default.VolumeUp, "Произнести", tint = colors.textSecondary,
                modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun BackContent(card: Card) {
    val colors = LocalAppColors.current
    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text       = card.russian,
            fontSize   = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.textPrimary,
            textAlign  = TextAlign.Center
        )
        if (card.transliteration.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text      = card.transliteration,
                fontSize  = 16.sp,
                color     = colors.textSecondary,
                textAlign = TextAlign.Center,
                style     = HebrewTextStyle.hint
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text      = card.hebrewPlain,
            style     = HebrewTextStyle.cardWord(20f),
            color     = colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Экран завершения сессии ─────────────────────────────────────────────────

@Composable
private fun SessionCompleteContent(
    state: StudyUiState,
    navController: NavController,
    padding: PaddingValues
) {
    val colors   = LocalAppColors.current
    val duration = ((System.currentTimeMillis() - state.startTime) / 1000).toInt()
    val minutes  = duration / 60
    val seconds  = duration % 60

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "Сессия завершена!",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = colors.textPrimary
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResultStat("✓", "${state.answered}", "правильно", ColorFlashcard)
            ResultStat("✗", "${state.repeatCount}", "ошибок",   ColorWrongLetter)
            ResultStat(
                icon  = "⏱",
                value = if (minutes > 0) "${minutes}м ${seconds}с" else "${seconds}с",
                label = "время",
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick  = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = ColorFlashcard)
        ) {
            Text("Вернуться к колоде", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ResultStat(icon: String, value: String, label: String, color: Color) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = colors.textSecondary)
    }
}

// ─── Утилиты ─────────────────────────────────────────────────────────────────

private fun modeName(mode: StudyMode) = when (mode) {
    StudyMode.FLASHCARD -> "Флипкарты"
    StudyMode.WRITING   -> "Написание"
    StudyMode.DICTATION -> "Диктант"
    StudyMode.CHOICE    -> "Выбор из 4"
}

private fun modeAccentColor(mode: StudyMode) = when (mode) {
    StudyMode.FLASHCARD -> ColorFlashcard
    StudyMode.WRITING   -> ColorWriting
    StudyMode.DICTATION -> ColorDictation
    StudyMode.CHOICE    -> ColorChoice
}
