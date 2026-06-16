package com.hebrewcards.data.db.dao

import androidx.room.*
import com.hebrewcards.data.db.entity.Deck
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Long): Deck?

    @Insert
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    // Количество колод — для определения первого запуска
    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int
}
