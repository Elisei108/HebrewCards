package com.hebrewcards.ui.screen.deck

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hebrewcards.data.repository.DeckProgress
import com.hebrewcards.domain.model.DeckType
import com.hebrewcards.domain.model.StudyMode
import com.hebrewcards.ui.navigation.Routes
import com.hebrewcards.ui.theme.*

private data class ModeInfo(
    val mode: StudyMode,
    val title: String,
    val description: String,
    val emoji: String,
    val color: Color
)

private val studyModes = listOf(
    ModeInfo(StudyMode.FLASHCARD, "Флипкарты",  "Переверни и вспомни",     "🃏", ColorFlashcard),
    ModeInfo(StudyMode.WRITING,   "Написание",  "Напиши слово на иврите",  "✏️", ColorWriting),
    ModeInfo(StudyMode.DICTATION, "Диктант",    "Услышь и напиши",         "🎧", ColorDictation),
    ModeInfo(StudyMode.CHOICE,    "Выбор из 4", "Выбери правильный ответ", "🎯", ColorChoice),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    deckId: Long,
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val vm: DeckViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DeckViewModel(application, deckId) as T
        }
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text     = if (uiState.deck?.type == DeckType.VERB) "🔤" else "📖",
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = uiState.deck?.name ?: "",
                            color      = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint               = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorFlashcard)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProgressCard(uiState.progress, colors)

            if (uiState.progress.total == 0) {
                EmptyDeckHint(colors)
            } else {
                StudyModesSection(deckId, navController, colors)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProgressCard(progress: DeckProgress, colors: AppColors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text       = "Прогресс",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.textPrimary
            )
            Text(
                text     = "${progress.total} карточек",
                fontSize = 13.sp,
                color    = colors.textSecondary
            )
        }

        // Прогресс-бар: known / total
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.knownFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ColorFlashcard)
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            StatusChip("✓", "${progress.known}",   "известных", ColorFlashcard, Modifier.weight(1f))
            StatusChip("↺", "${progress.learning}", "учим",      ColorWriting,   Modifier.weight(1f))
            StatusChip("★", "${progress.new}",      "новых",     ColorChoice,    Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusChip(
    icon: String,
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 13.sp, color = color)
            Text(count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Text(label, fontSize = 11.sp, color = LocalAppColors.current.textSecondary)
    }
}

@Composable
private fun StudyModesSection(deckId: Long, navController: NavController, colors: AppColors) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text       = "Режим обучения",
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color      = colors.textPrimary
        )

        studyModes.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { info ->
                    StudyModeCard(
                        info     = info,
                        onClick  = { navController.navigate(Routes.study(deckId, info.mode.name)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Если в строке один элемент (нечётное число) — добавляем заполнитель
                if (row.size < 2) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StudyModeCard(
    info: ModeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Card(
        modifier  = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Иконка режима в цветном бейдже
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(info.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(info.emoji, fontSize = 24.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = info.title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.textPrimary,
                    maxLines   = 1
                )
                Text(
                    text       = info.description,
                    fontSize   = 12.sp,
                    color      = colors.textSecondary,
                    lineHeight = 16.sp,
                    maxLines   = 2
                )
            }
        }
    }
}

@Composable
private fun EmptyDeckHint(colors: AppColors) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📭", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text       = "Колода пуста",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Medium,
            color      = colors.textPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text     = "Добавьте карточки через CSV-импорт",
            fontSize = 13.sp,
            color    = colors.textSecondary
        )
    }
}
