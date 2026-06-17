package com.hebrewcards.ui.screen.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.db.entity.Deck
import com.hebrewcards.data.repository.DeckProgress
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.data.repository.ProgressRepository
import com.hebrewcards.domain.model.DeckType
import com.hebrewcards.domain.usecase.ImportDeckUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Состояние одной колоды на дашборде
data class DeckUiItem(
    val deck: Deck,
    val progress: DeckProgress
)

// Полное состояние экрана дашборда
data class DashboardUiState(
    val decks: List<DeckUiItem> = emptyList(),
    val totalWordsLearned: Int = 0,
    val currentStreak: Int = 0,  // дней занятий подряд
    val maxStreak: Int = 0,      // рекорд серии без ошибок за всё время
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val deckRepo = DeckRepository(db.deckDao(), db.cardDao(), db.progressDao(), db)
    private val progressRepo = ProgressRepository(db.progressDao())
    private val importUseCase = ImportDeckUseCase(deckRepo)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        checkFirstRun()
        loadDashboard()
    }

    // Проверяем флаг первого запуска; если не установлен — загружаем демо-колоду
    private fun checkFirstRun() {
        viewModelScope.launch {
            val prefs = getApplication<Application>()
                .getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("first_run_done", false)) {
                loadDemoDeck()
                prefs.edit().putBoolean("first_run_done", true).apply()
            }
        }
    }

    // Читаем CSV из assets и импортируем как демо-колоду
    private suspend fun loadDemoDeck() {
        try {
            val csvText = withContext(Dispatchers.IO) {
                getApplication<Application>().assets
                    .open("decks/ulpan_alef_01.csv")
                    .bufferedReader()
                    .readText()
            }
            importUseCase.execute("Ульпан алеф · урок 1", csvText)
        } catch (_: Exception) {
            // Не критично — запуск продолжится без демо-колоды
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val prefs = getApplication<Application>()
                .getSharedPreferences("hebrewcards_prefs", Context.MODE_PRIVATE)

            deckRepo.getAllDecks().collect { decks ->
                // Для каждой колоды загружаем прогресс
                val deckItems = decks.map { deck ->
                    DeckUiItem(
                        deck     = deck,
                        progress = deckRepo.getDeckProgress(deck.id)
                    )
                }
                val totalLearned = progressRepo.getTotalCorrectCount()

                _uiState.update {
                    it.copy(
                        decks             = deckItems,
                        totalWordsLearned = totalLearned,
                        // Читаем стрик свежим при каждом обновлении (после сессии прилетит новое значение)
                        currentStreak     = prefs.getInt("streak_current", 0),
                        maxStreak         = prefs.getInt("streak_max", 0),
                        isLoading         = false
                    )
                }
            }
        }
    }

    // Удалить колоду
    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            deckRepo.deleteDeck(deck)
        }
    }

    // Сбросить прогресс колоды
    fun resetProgress(deckId: Long) {
        viewModelScope.launch {
            deckRepo.resetDeckProgress(deckId)
        }
    }
}
