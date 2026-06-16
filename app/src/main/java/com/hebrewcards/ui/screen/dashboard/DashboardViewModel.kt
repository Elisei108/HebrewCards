package com.hebrewcards.ui.screen.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.db.entity.Deck
import com.hebrewcards.data.repository.DeckProgress
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.data.repository.ProgressRepository
import com.hebrewcards.domain.model.DeckType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Состояние одной колоды на дашборде
data class DeckUiItem(
    val deck: Deck,
    val progress: DeckProgress
)

// Полное состояние экрана дашборда
data class DashboardUiState(
    val decks: List<DeckUiItem> = emptyList(),
    val totalWordsLearned: Int = 0,
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val deckRepo = DeckRepository(db.deckDao(), db.cardDao(), db.progressDao())
    private val progressRepo = ProgressRepository(db.progressDao())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
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
