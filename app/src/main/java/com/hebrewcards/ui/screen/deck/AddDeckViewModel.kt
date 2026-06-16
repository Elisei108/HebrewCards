package com.hebrewcards.ui.screen.deck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.domain.usecase.ImportDeckUseCase
import com.hebrewcards.domain.usecase.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddDeckUiState(
    val deckName: String         = "",
    val csvText: String          = "",
    val isLoading: Boolean       = false,
    val errorMessage: String?    = null,
    val importedDeckId: Long?    = null  // не null = успех, переходим на экран колоды
)

class AddDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val db       = AppDatabase.getInstance(application)
    private val repo     = DeckRepository(db.deckDao(), db.cardDao(), db.progressDao())
    private val useCase  = ImportDeckUseCase(repo)

    private val _uiState = MutableStateFlow(AddDeckUiState())
    val uiState: StateFlow<AddDeckUiState> = _uiState.asStateFlow()

    fun onDeckNameChange(value: String) {
        _uiState.update { it.copy(deckName = value, errorMessage = null) }
    }

    fun onCsvTextChange(value: String) {
        _uiState.update { it.copy(csvText = value, errorMessage = null) }
    }

    fun importDeck() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = useCase.execute(state.deckName.trim(), state.csvText)
            when (result) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, importedDeckId = result.deckId)
                    }
                }
                is ImportResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
