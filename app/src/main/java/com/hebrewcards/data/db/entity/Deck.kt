package com.hebrewcards.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hebrewcards.domain.model.DeckType

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DeckType,
    val createdAt: Long,
    val lastStudiedAt: Long?
)
