package com.hebrewcards.data.db.dao

import androidx.room.*
import com.hebrewcards.data.db.entity.Card
import com.hebrewcards.data.db.entity.VerbCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY position ASC")
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY position ASC")
    suspend fun getCardsByDeckOnce(deckId: Long): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): Card?

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    suspend fun getCardCount(deckId: Long): Int

    @Insert
    suspend fun insertCard(card: Card): Long

    @Insert
    suspend fun insertCards(cards: List<Card>)

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeck(deckId: Long)

    @Query("SELECT MAX(position) FROM cards WHERE deckId = :deckId")
    suspend fun getMaxPosition(deckId: Long): Int?

    // VerbCard
    @Query("SELECT * FROM verb_cards WHERE cardId = :cardId")
    suspend fun getVerbCard(cardId: Long): VerbCard?

    @Insert
    suspend fun insertVerbCard(verbCard: VerbCard)

    @Insert
    suspend fun insertVerbCards(verbCards: List<VerbCard>)
}
