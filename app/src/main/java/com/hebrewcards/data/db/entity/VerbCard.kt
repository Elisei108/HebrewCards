package com.hebrewcards.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "verb_cards",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VerbCard(
    @PrimaryKey val cardId: Long,
    val binyan: String,
    val root: String,
    val presentSingular: String,       // с огласовками — для отображения
    val presentSingularPlain: String,  // без огласовок — для проверки
    val conjugationJson: String?
)
