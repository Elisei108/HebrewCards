package com.hebrewcards.ui.screen.study

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.db.entity.*
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.data.repository.ProgressRepository
import com.hebrewcards.domain.model.StudyMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val KNOWN_THRESHOLD = 3   // правильных подряд → KNOWN
private const val KNOWN_INTERVAL  = 10  // каждые N карточек вставляем одну KNOWN

data class StudyCard(
    val card: Card,
    val progress: CardProgress
)

data class StudyUiState(
    val remaining: List<StudyCard> = emptyList(),
    val allCards: List<Card> = emptyList(),   // все карточки колоды — нужны ChoiceContent для вариантов
    val initialTotal: Int = 0,
    val answered: Int = 0,         // свайпов вправо (знаю)
    val repeatCount: Int = 0,      // свайпов влево (повторить)
    val isFlipped: Boolean = false,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val startTime: Long = 0L,
    val currentStreak: Int = 0,    // текущая серия без ошибок в этой сессии
    val maxStreak: Int = 0,        // рекорд серии в этой сессии
    val finalDurationSeconds: Int = 0  // зафиксированная длительность, не тикает после завершения
) {
    val current: StudyCard? get() = remaining.firstOrNull()
    val progressFraction: Float
        get() = if (initialTotal == 0) 0f else answered.toFloat() / initialTotal.toFloat()
}

class StudyViewModel(
    application: Application,
    private val deckId: Long,
    private val mode: StudyMode
) : AndroidViewModel(application) {

    private val db           = AppDatabase.getInstance(application)
    private val deckRepo     = DeckRepository(db.deckDao(), db.cardDao(), db.progressDao(), db)
    private val progressRepo = ProgressRepository(db.progressDao())

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        loadQueue()
    }

    private fun loadQueue() {
        viewModelScope.launch {
            // Читаем ограничение сессии из настроек (0 = без ограничений)
            val prefs       = getApplication<Application>().getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE)
            val sessionSize = prefs.getInt("session_size", 0)

            val cards    = db.cardDao().getCardsByDeckOnce(deckId)
            val progList = db.progressDao().getProgressByDeckOnce(deckId)
            val progMap  = progList.associateBy { it.cardId }

            val studyCards = cards.map { card ->
                val prog = progMap[card.id] ?: CardProgress(
                    cardId        = card.id,
                    deckId        = deckId,
                    status        = CardStatus.NEW,
                    errorCount    = 0,
                    correctStreak = 0,
                    lastStudiedAt = null
                )
                StudyCard(card, prog)
            }

            var queue = buildQueue(studyCards)
            // Обрезаем очередь до заданного размера сессии
            if (sessionSize > 0) queue = queue.take(sessionSize)

            _uiState.update {
                it.copy(
                    remaining    = queue,
                    allCards     = cards,
                    initialTotal = queue.size,
                    isLoading    = false,
                    startTime    = System.currentTimeMillis()
                )
            }
        }
    }

    // LEARNING первыми, затем NEW; каждые KNOWN_INTERVAL — вставляем одну KNOWN
    private fun buildQueue(cards: List<StudyCard>): List<StudyCard> {
        val learning = cards.filter { it.progress.status == CardStatus.LEARNING }.shuffled()
        val new      = cards.filter { it.progress.status == CardStatus.NEW }.shuffled()
        val known    = cards.filter { it.progress.status == CardStatus.KNOWN }.shuffled()

        val primary = learning + new
        if (primary.isEmpty()) return known.take(5)

        val result   = mutableListOf<StudyCard>()
        var knownIdx = 0
        primary.forEachIndexed { i, card ->
            result.add(card)
            if ((i + 1) % KNOWN_INTERVAL == 0 && knownIdx < known.size) {
                result.add(known[knownIdx++])
            }
        }
        return result
    }

    fun flip() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    // Свайп вправо — знаю: обновляем прогресс, серию, переходим к следующей
    fun swipeRight() {
        val state   = _uiState.value
        val current = state.current ?: return

        viewModelScope.launch {
            val updated = advanceProgress(current.progress)
            progressRepo.upsertProgress(updated)

            val newRemaining  = state.remaining.drop(1)
            val isComplete    = newRemaining.isEmpty()
            val newStreak     = state.currentStreak + 1
            val newMaxStreak  = maxOf(state.maxStreak, newStreak)

            _uiState.update {
                it.copy(
                    remaining     = newRemaining,
                    answered      = it.answered + 1,
                    isFlipped     = false,
                    isComplete    = isComplete,
                    currentStreak = newStreak,
                    maxStreak     = newMaxStreak
                )
            }
            if (isComplete) saveSession()
        }
    }

    // Режим написания — фиксируем ошибку, серия сбрасывается
    fun recordError() {
        val state   = _uiState.value
        val current = state.current ?: return

        viewModelScope.launch {
            // При ошибке сбрасываем correctStreak и возвращаем KNOWN → LEARNING
            val updated = current.progress.copy(
                errorCount    = current.progress.errorCount + 1,
                correctStreak = 0,
                status        = if (current.progress.status == CardStatus.KNOWN) CardStatus.LEARNING else current.progress.status,
                lastStudiedAt = System.currentTimeMillis()
            )
            progressRepo.upsertProgress(updated)

            // Обновляем in-memory прогресс первой карточки очереди
            val newRemaining = listOf(current.copy(progress = updated)) + state.remaining.drop(1)
            _uiState.update {
                it.copy(
                    remaining     = newRemaining,
                    repeatCount   = it.repeatCount + 1,
                    currentStreak = 0
                )
            }
        }
    }

    // Свайп влево — повторить: карточка в конец очереди, серия сбрасывается
    fun swipeLeft() {
        val state   = _uiState.value
        val current = state.current ?: return

        val newRemaining = state.remaining.drop(1) + current

        _uiState.update {
            it.copy(
                remaining     = newRemaining,
                repeatCount   = it.repeatCount + 1,
                isFlipped     = false,
                currentStreak = 0
            )
        }
    }

    private fun advanceProgress(p: CardProgress): CardProgress {
        val now = System.currentTimeMillis()
        return when (p.status) {
            CardStatus.NEW -> p.copy(
                status        = CardStatus.LEARNING,
                correctStreak = 1,
                lastStudiedAt = now
            )
            CardStatus.LEARNING -> {
                val streak = p.correctStreak + 1
                if (streak >= KNOWN_THRESHOLD) {
                    p.copy(status = CardStatus.KNOWN, correctStreak = streak, lastStudiedAt = now)
                } else {
                    p.copy(correctStreak = streak, lastStudiedAt = now)
                }
            }
            CardStatus.KNOWN -> p.copy(lastStudiedAt = now)
        }
    }

    private fun saveSession() {
        viewModelScope.launch {
            val state    = _uiState.value
            val duration = ((System.currentTimeMillis() - state.startTime) / 1000).toInt()
            // Фиксируем длительность в state — SessionCompleteContent читает её, не пересчитывает
            _uiState.update { it.copy(finalDurationSeconds = duration) }
            progressRepo.insertSessionResult(
                SessionResult(
                    deckId          = deckId,
                    mode            = mode,
                    totalCards      = state.initialTotal,
                    correctCount    = state.answered,
                    errorCount      = state.repeatCount,
                    durationSeconds = duration,
                    completedAt     = System.currentTimeMillis()
                )
            )
            deckRepo.getDeckById(deckId)?.let { deck ->
                deckRepo.updateDeck(deck.copy(lastStudiedAt = System.currentTimeMillis()))
            }
            // Обновляем дневной стрик после завершения сессии
            val prefs = getApplication<Application>()
                .getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE)
            updateStreak(prefs)
        }
    }

    // Пересчитывает стрик дней подряд на основе даты последней сессии
    private fun updateStreak(prefs: SharedPreferences) {
        val today   = LocalDate.now().toString()
        val last    = prefs.getString("streak_last_date", "") ?: ""
        val current = prefs.getInt("streak_current", 0)

        val newStreak = when {
            last == today                                       -> current  // уже занимался сегодня
            last == LocalDate.now().minusDays(1).toString()    -> current + 1  // занимался вчера
            else                                               -> 1         // пропустил день(и)
        }
        val newMax = maxOf(prefs.getInt("streak_max", 0), newStreak)
        prefs.edit()
            .putString("streak_last_date", today)
            .putInt("streak_current", newStreak)
            .putInt("streak_max", newMax)
            .apply()
    }
}
