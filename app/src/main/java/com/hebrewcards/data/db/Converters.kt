package com.hebrewcards.data.db

import androidx.room.TypeConverter
import com.hebrewcards.data.db.entity.CardStatus
import com.hebrewcards.domain.model.DeckType
import com.hebrewcards.domain.model.StudyMode

// Конвертеры enum → строка для Room
class Converters {

    @TypeConverter fun fromDeckType(v: DeckType): String = v.name
    @TypeConverter fun toDeckType(v: String): DeckType = DeckType.valueOf(v)

    @TypeConverter fun fromStudyMode(v: StudyMode): String = v.name
    @TypeConverter fun toStudyMode(v: String): StudyMode = StudyMode.valueOf(v)

    @TypeConverter fun fromCardStatus(v: CardStatus): String = v.name
    @TypeConverter fun toCardStatus(v: String): CardStatus = CardStatus.valueOf(v)
}
