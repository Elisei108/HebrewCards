package com.hebrewcards.ui.screen.deck

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.domain.usecase.ImportDeckUseCase
import com.hebrewcards.domain.usecase.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Одна пара для ручного добавления
data class ManualCardPair(
    val id: Int,           // локальный id для LazyColumn key
    val hebrew: String  = "",
    val russian: String = ""
)

data class AddDeckUiState(
    val deckName: String         = "",
    val csvText: String          = "",
    val isLoading: Boolean       = false,
    val errorMessage: String?    = null,
    val importedDeckId: Long?    = null,  // не null = успех, переходим назад

    // Состояние ручного добавления
    val showManualSheet: Boolean         = false,
    val manualDeckName: String           = "",
    val manualCards: List<ManualCardPair> = listOf(ManualCardPair(id = 0)),
    val manualError: String?             = null
)

class AddDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val db      = AppDatabase.getInstance(application)
    private val repo    = DeckRepository(db.deckDao(), db.cardDao(), db.progressDao())
    private val useCase = ImportDeckUseCase(repo)

    private val _uiState = MutableStateFlow(AddDeckUiState())
    val uiState: StateFlow<AddDeckUiState> = _uiState.asStateFlow()

    // ── CSV-импорт ──────────────────────────────────────────────────────────

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
            when (val result = useCase.execute(state.deckName.trim(), state.csvText)) {
                is ImportResult.Success ->
                    _uiState.update { it.copy(isLoading = false, importedDeckId = result.deckId) }
                is ImportResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    // Импорт из файла — получаем текст файла снаружи, название из имени файла
    fun importFromFile(fileName: String, fileContent: String) {
        // Берём имя файла без расширения как название колоды (можно будет поменять)
        val deckName = fileName.removeSuffix(".csv").replace("_", " ").trim()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = useCase.execute(deckName, fileContent)) {
                is ImportResult.Success ->
                    _uiState.update { it.copy(isLoading = false, importedDeckId = result.deckId) }
                is ImportResult.Error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    // Импорт из Uri — читаем файл на Dispatchers.IO, затем вызываем importFromFile
    fun importFromUri(context: Context, uri: Uri) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val (fileName, fileContent) = withContext(Dispatchers.IO) {
                    val displayName = getDisplayName(context, uri)
                    val content = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText() ?: ""
                    displayName to content
                }
                if (fileContent.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Файл пустой или не читается") }
                    return@launch
                }
                if (!looksLikeCsv(fileContent)) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Файл не похож на CSV — выберите текстовый файл с данными") }
                    return@launch
                }
                val deckName = fileName.removeSuffix(".csv").replace("_", " ").trim()
                when (val result = useCase.execute(deckName, fileContent)) {
                    is ImportResult.Success ->
                        _uiState.update { it.copy(isLoading = false, importedDeckId = result.deckId) }
                    is ImportResult.Error ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Ошибка чтения файла: ${e.message}") }
            }
        }
    }

    // Проверяем что файл хотя бы отдалённо похож на текстовый CSV
    private fun looksLikeCsv(text: String): Boolean {
        val sample = text.take(1000)
        val badChars = sample.count { it.code < 0x20 && it != '\n' && it != '\r' && it != '\t' }
        return badChars < sample.length / 20
    }

    // Получаем отображаемое имя файла через ContentResolver (SAF)
    private fun getDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(
            uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx) ?: "колода"
            }
        }
        return uri.lastPathSegment?.substringAfterLast("/") ?: "колода"
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Ручное добавление ───────────────────────────────────────────────────

    fun openManualSheet() {
        _uiState.update {
            it.copy(
                showManualSheet = true,
                manualDeckName  = "",
                manualCards     = listOf(ManualCardPair(id = 0)),
                manualError     = null
            )
        }
    }

    fun closeManualSheet() {
        _uiState.update { it.copy(showManualSheet = false) }
    }

    fun onManualDeckNameChange(value: String) {
        _uiState.update { it.copy(manualDeckName = value, manualError = null) }
    }

    // Обновить иврит в паре по id
    fun onManualHebrewChange(id: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                manualCards = state.manualCards.map { if (it.id == id) it.copy(hebrew = value) else it },
                manualError = null
            )
        }
    }

    // Обновить перевод в паре по id
    fun onManualRussianChange(id: Int, value: String) {
        _uiState.update { state ->
            state.copy(
                manualCards = state.manualCards.map { if (it.id == id) it.copy(russian = value) else it },
                manualError = null
            )
        }
    }

    // Добавить новую пустую пару
    fun addManualCard() {
        _uiState.update { state ->
            val nextId = (state.manualCards.maxOfOrNull { it.id } ?: 0) + 1
            state.copy(manualCards = state.manualCards + ManualCardPair(id = nextId))
        }
    }

    // Удалить пару по id (минимум одна пара остаётся)
    fun removeManualCard(id: Int) {
        _uiState.update { state ->
            if (state.manualCards.size <= 1) return@update state
            state.copy(manualCards = state.manualCards.filter { it.id != id })
        }
    }

    // Сохранить ручную колоду — конвертируем пары в CSV и вызываем ImportDeckUseCase
    fun saveManualDeck() {
        val state = _uiState.value

        if (state.manualDeckName.isBlank()) {
            _uiState.update { it.copy(manualError = "Введите название колоды") }
            return
        }

        // Оставляем только заполненные пары
        val validPairs = state.manualCards.filter {
            it.hebrew.isNotBlank() && it.russian.isNotBlank()
        }

        if (validPairs.isEmpty()) {
            _uiState.update { it.copy(manualError = "Добавьте хотя бы одну карточку") }
            return
        }

        // Собираем CSV из пар
        val csv = validPairs.joinToString("\n") { "${it.hebrew};${it.russian}" }

        _uiState.update { it.copy(isLoading = true, manualError = null) }

        viewModelScope.launch {
            when (val result = useCase.execute(state.manualDeckName.trim(), csv)) {
                is ImportResult.Success ->
                    _uiState.update {
                        it.copy(isLoading = false, showManualSheet = false, importedDeckId = result.deckId)
                    }
                is ImportResult.Error ->
                    _uiState.update { it.copy(isLoading = false, manualError = result.message) }
            }
        }
    }
}
