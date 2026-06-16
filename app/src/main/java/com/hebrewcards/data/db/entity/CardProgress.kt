package com.hebrewcards.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class CardStatus {
    NEW,
    LEARNING,
    KNOWN
}

@Entity(
    tableName = "card_progress",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardProgress(
    @PrimaryKey val cardId: Long,
    val deckId: Long,
    val status: CardStatus,
    val errorCount: Int,
    val correctStreak: Int,       // правильных подряд — для перехода в KNOWN
    val lastStudiedAt: Long?
)
