package com.hebrewcards.domain.usecase

import com.hebrewcards.data.db.entity.Card
import com.hebrewcards.data.db.entity.CardProgress
import com.hebrewcards.data.db.entity.CardStatus
import com.hebrewcards.data.db.entity.Deck
import com.hebrewcards.data.db.entity.VerbCard
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.domain.model.DeckType
import com.hebrewcards.util.Transliterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Результат импорта
sealed class ImportResult {
    data class Success(val deckId: Long, val cardCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class ImportDeckUseCase(private val repo: DeckRepository) {

    /**
     * Импортирует колоду из CSV текста.
     * Формат обычной карточки: иврит;перевод
     * Формат глагольной колоды: первая строка #VERB, потом инфинитив;перевод;биньян;корень;настоящее
     */
    suspend fun execute(deckName: String, csvText: String): ImportResult = withContext(Dispatchers.IO) {
        if (deckName.isBlank()) return@withContext ImportResult.Error("Введите название колоды")

        val lines = csvText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return@withContext ImportResult.Error("Колода пустая — добавьте карточки")

        // Определяем тип колоды по маркеру #VERB в первой строке
        val isVerb = lines.firstOrNull()?.uppercase() == "#VERB"
        val dataLines = if (isVerb) lines.drop(1) else lines

        // Фильтруем комментарии (строки начинающиеся с #)
        val cardLines = dataLines.filter { !it.startsWith("#") }

        if (cardLines.isEmpty()) return@withContext ImportResult.Error("Нет карточек для импорта")

        try {
            if (isVerb) importVerbDeck(deckName, cardLines)
            else importRegularDeck(deckName, cardLines)
        } catch (e: Exception) {
            ImportResult.Error("Ошибка импорта: ${e.message}")
        }
    }

    // Импорт обычной колоды
    private suspend fun importRegularDeck(name: String, lines: List<String>): ImportResult {
        val deck = Deck(
            name          = name,
            type          = DeckType.REGULAR,
            createdAt     = System.currentTimeMillis(),
            lastStudiedAt = null
        )
        val deckId = repo.insertDeck(deck)

        val cards = mutableListOf<Card>()
        val errors = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            val parts = line.split(";")
            if (parts.size < 2) {
                errors.add("Строка ${index + 1}: неверный формат «$line»")
                return@forEachIndexed
            }
            val hebrew  = parts[0].trim()
            val russian = parts[1].trim()

            if (hebrew.isBlank() || russian.isBlank()) {
                errors.add("Строка ${index + 1}: пустое поле")
                return@forEachIndexed
            }

            cards.add(
                Card(
                    deckId          = deckId,
                    hebrew          = hebrew,
                    hebrewPlain     = Transliterator.removeNikud(hebrew),
                    russian         = russian,
                    transliteration = Transliterator.transliterate(hebrew),
                    position        = index
                )
            )
        }

        if (cards.isEmpty()) {
            // Удаляем пустую колоду
            repo.deleteDeck(repo.getDeckById(deckId)!!)
            return ImportResult.Error("Ни одна карточка не импортирована.\n${errors.joinToString("\n")}")
        }

        // Вставляем карточки и создаём начальный прогресс (все NEW)
        repo.insertCardsWithProgress(deckId, cards)

        return ImportResult.Success(deckId, cards.size)
    }

    // Импорт глагольной колоды
    private suspend fun importVerbDeck(name: String, lines: List<String>): ImportResult {
        val deck = Deck(
            name          = name,
            type          = DeckType.VERB,
            createdAt     = System.currentTimeMillis(),
            lastStudiedAt = null
        )
        val deckId = repo.insertDeck(deck)

        val cards     = mutableListOf<Card>()
        val verbCards = mutableListOf<VerbCard>()
        val errors    = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            val parts = line.split(";")
            if (parts.size < 5) {
                errors.add("Строка ${index + 1}: нужно 5 полей (инфинитив;перевод;биньян;корень;настоящее)")
                return@forEachIndexed
            }

            val infinitive      = parts[0].trim()
            val russian         = parts[1].trim()
            val binyan          = parts[2].trim()
            val root            = parts[3].trim()
            val presentSingular = parts[4].trim()
            val conjugationJson = if (parts.size > 5) parts[5].trim() else null

            if (infinitive.isBlank() || russian.isBlank()) {
                errors.add("Строка ${index + 1}: пустое поле")
                return@forEachIndexed
            }

            // Временный id — заменится после вставки
            cards.add(
                Card(
                    deckId          = deckId,
                    hebrew          = infinitive,
                    hebrewPlain     = Transliterator.removeNikud(infinitive),
                    russian         = russian,
                    transliteration = Transliterator.transliterate(infinitive),
                    position        = index
                )
            )
            // VerbCard — cardId проставим после вставки карточек
            verbCards.add(
                VerbCard(
                    cardId               = 0L, // placeholder
                    binyan               = binyan,
                    root                 = root,
                    presentSingular      = presentSingular,
                    presentSingularPlain = Transliterator.removeNikud(presentSingular),
                    conjugationJson      = conjugationJson
                )
            )
        }

        if (cards.isEmpty()) {
            repo.deleteDeck(repo.getDeckById(deckId)!!)
            return ImportResult.Error("Ни одна карточка не импортирована.\n${errors.joinToString("\n")}")
        }

        // Вставляем и получаем реальные id карточек
        repo.insertVerbCardsWithProgress(deckId, cards, verbCards)

        return ImportResult.Success(deckId, cards.size)
    }
}
