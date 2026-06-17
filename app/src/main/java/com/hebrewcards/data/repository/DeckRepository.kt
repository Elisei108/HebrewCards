package com.hebrewcards.data.repository

import androidx.room.withTransaction
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.db.dao.CardDao
import com.hebrewcards.data.db.dao.DeckDao
import com.hebrewcards.data.db.dao.ProgressDao
import com.hebrewcards.data.db.entity.Card
import com.hebrewcards.data.db.entity.CardProgress
import com.hebrewcards.data.db.entity.CardStatus
import com.hebrewcards.data.db.entity.Deck
import com.hebrewcards.data.db.entity.VerbCard
import kotlinx.coroutines.flow.Flow

class DeckRepository(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val progressDao: ProgressDao,
    private val db: AppDatabase? = null  // нужен для withTransaction
) {
    fun getAllDecks(): Flow<List<Deck>> = deckDao.getAllDecks()

    suspend fun getDeckById(id: Long): Deck? = deckDao.getDeckById(id)

    suspend fun insertDeck(deck: Deck): Long = deckDao.insertDeck(deck)

    suspend fun updateDeck(deck: Deck) = deckDao.updateDeck(deck)

    suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck)

    suspend fun getDeckCount(): Int = deckDao.getDeckCount()

    suspend fun getAllDecksOnce(): List<Deck> = deckDao.getAllDecksOnce()

    suspend fun getCountByStatus(deckId: Long, status: CardStatus): Int =
        progressDao.getCountByStatus(deckId, status)

    // Прогресс колоды для дашборда
    suspend fun getDeckProgress(deckId: Long): DeckProgress {
        val total    = cardDao.getCardCount(deckId)
        val known    = progressDao.getCountByStatus(deckId, CardStatus.KNOWN)
        val learning = progressDao.getCountByStatus(deckId, CardStatus.LEARNING)
        val new      = progressDao.getCountByStatus(deckId, CardStatus.NEW)
        return DeckProgress(total, known, learning, new)
    }

    // Вставить обычные карточки + создать начальный прогресс атомарно
    suspend fun insertCardsWithProgress(deckId: Long, cards: List<Card>) {
        val doInsert = suspend {
            val ids = cardDao.insertCards(cards)
            val progressList = ids.map { id ->
                CardProgress(
                    cardId        = id,
                    deckId        = deckId,
                    status        = CardStatus.NEW,
                    errorCount    = 0,
                    correctStreak = 0,
                    lastStudiedAt = null
                )
            }
            progressDao.insertProgressList(progressList)
        }
        if (db != null) db.withTransaction { doInsert() } else doInsert()
    }

    // Вставить глагольные карточки + VerbCard + прогресс атомарно
    suspend fun insertVerbCardsWithProgress(
        deckId: Long,
        cards: List<Card>,
        verbCards: List<VerbCard>
    ) {
        val doInsert = suspend {
            val ids = cardDao.insertCards(cards)
            // Привязываем VerbCard к реальным id по индексу вставки
            val verbCardsWithIds = ids.mapIndexed { index, id ->
                verbCards[index].copy(cardId = id)
            }
            cardDao.insertVerbCards(verbCardsWithIds)
            val progressList = ids.map { id ->
                CardProgress(
                    cardId        = id,
                    deckId        = deckId,
                    status        = CardStatus.NEW,
                    errorCount    = 0,
                    correctStreak = 0,
                    lastStudiedAt = null
                )
            }
            progressDao.insertProgressList(progressList)
        }
        if (db != null) db.withTransaction { doInsert() } else doInsert()
    }

    // Сбросить прогресс — все карточки снова NEW
    suspend fun resetDeckProgress(deckId: Long) {
        progressDao.resetDeckProgress(deckId)
        val cards = cardDao.getCardsByDeckOnce(deckId)
        val progressList = cards.map { card ->
            CardProgress(
                cardId        = card.id,
                deckId        = deckId,
                status        = CardStatus.NEW,
                errorCount    = 0,
                correctStreak = 0,
                lastStudiedAt = null
            )
        }
        progressDao.insertProgressList(progressList)
    }
}

// Прогресс колоды для UI
data class DeckProgress(
    val total: Int,
    val known: Int,
    val learning: Int,
    val new: Int
) {
    val knownFraction: Float
        get() = if (total == 0) 0f else known.toFloat() / total.toFloat()

    val remaining: Int get() = new + learning
}
