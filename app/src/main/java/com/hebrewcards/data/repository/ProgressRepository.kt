package com.hebrewcards.data.repository

import com.hebrewcards.data.db.dao.ProgressDao
import com.hebrewcards.data.db.entity.CardProgress
import com.hebrewcards.data.db.entity.CardStatus
import com.hebrewcards.data.db.entity.SessionResult
import kotlinx.coroutines.flow.Flow

class ProgressRepository(private val progressDao: ProgressDao) {

    suspend fun getProgress(cardId: Long): CardProgress? =
        progressDao.getProgress(cardId)

    fun getProgressByDeck(deckId: Long): Flow<List<CardProgress>> =
        progressDao.getProgressByDeck(deckId)

    suspend fun upsertProgress(progress: CardProgress) =
        progressDao.upsertProgress(progress)

    suspend fun insertSessionResult(result: SessionResult): Long =
        progressDao.insertSessionResult(result)

    fun getSessionsByDeck(deckId: Long): Flow<List<SessionResult>> =
        progressDao.getSessionsByDeck(deckId)

    // Количество сессий начиная с полуночи — для стрика
    suspend fun getSessionCountToday(): Int {
        val startOfDay = getStartOfDayMillis()
        return progressDao.getSessionCountSince(startOfDay)
    }

    // Всего правильных ответов за всё время
    suspend fun getTotalCorrectCount(): Int =
        progressDao.getTotalCorrectCount() ?: 0

    private fun getStartOfDayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
