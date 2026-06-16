package com.hebrewcards.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.hebrewcards.domain.model.StudyMode

@Entity(
    tableName = "session_results",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val mode: StudyMode,
    val totalCards: Int,
    val correctCount: Int,
    val errorCount: Int,
    val durationSeconds: Int,
    val completedAt: Long
)
