package com.hebrewcards.data.db.dao

import androidx.room.*
import com.hebrewcards.data.db.entity.CardProgress
import com.hebrewcards.data.db.entity.CardStatus
import com.hebrewcards.data.db.entity.SessionResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM card_progress WHERE cardId = :cardId")
    suspend fun getProgress(cardId: Long): CardProgress?

    @Query("SELECT * FROM card_progress WHERE deckId = :deckId")
    fun getProgressByDeck(deckId: Long): Flow<List<CardProgress>>

    @Query("SELECT * FROM card_progress WHERE deckId = :deckId")
    suspend fun getProgressByDeckOnce(deckId: Long): List<CardProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: CardProgress)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressList(progressList: List<CardProgress>)

    @Query("SELECT COUNT(*) FROM card_progress WHERE deckId = :deckId AND status = :status")
    suspend fun getCountByStatus(deckId: Long, status: CardStatus): Int

    @Query("DELETE FROM card_progress WHERE deckId = :deckId")
    suspend fun resetDeckProgress(deckId: Long)

    // SessionResult
    @Insert
    suspend fun insertSessionResult(result: SessionResult): Long

    @Query("SELECT * FROM session_results WHERE deckId = :deckId ORDER BY completedAt DESC")
    fun getSessionsByDeck(deckId: Long): Flow<List<SessionResult>>

    @Query("SELECT * FROM session_results ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLastSession(): SessionResult?

    @Query("SELECT COUNT(*) FROM session_results WHERE completedAt >= :startOfDay")
    suspend fun getSessionCountSince(startOfDay: Long): Int

    @Query("SELECT SUM(correctCount) FROM session_results")
    suspend fun getTotalCorrectCount(): Int?
}
