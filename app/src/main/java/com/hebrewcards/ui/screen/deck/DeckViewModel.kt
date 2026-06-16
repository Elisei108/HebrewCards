package com.hebrewcards.ui.screen.deck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.db.entity.CardStatus
import com.hebrewcards.data.db.entity.Deck
import com.hebrewcards.data.repository.DeckProgress
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeckUiState(
    val deck: Deck? = null,
    val progress: DeckProgress = DeckProgress(0, 0, 0, 0),
    val isLoading: Boolean = true
)

class DeckViewModel(application: Application, private val deckId: Long) : AndroidViewModel(application) {

    private val db           = AppDatabase.getInstance(application)
    private val deckRepo     = DeckRepository(db.deckDao(), db.cardDao(), db.progressDao())
    private val progressRepo = ProgressRepository(db.progressDao())

    private val _uiState = MutableStateFlow(DeckUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()

    init {
        loadDeck()
    }

    private fun loadDeck() {
        viewModelScope.launch {
            val deck = deckRepo.getDeckById(deckId)
            _uiState.update { it.copy(deck = deck) }

            // Наблюдаем за прогрессом — обновляется после каждой сессии
            progressRepo.getProgressByDeck(deckId).collect { list ->
                val total    = list.size
                val known    = list.count { it.status == CardStatus.KNOWN }
                val learning = list.count { it.status == CardStatus.LEARNING }
                val new      = list.count { it.status == CardStatus.NEW }
                _uiState.update {
                    it.copy(
                        progress  = DeckProgress(total, known, learning, new),
                        isLoading = false
                    )
                }
            }
        }
    }
}
