package com.hebrewcards.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hebrewcards.data.db.dao.CardDao
import com.hebrewcards.data.db.dao.DeckDao
import com.hebrewcards.data.db.dao.ProgressDao
import com.hebrewcards.data.db.entity.*

@Database(
    entities = [
        Deck::class,
        Card::class,
        VerbCard::class,
        CardProgress::class,
        SessionResult::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun progressDao(): ProgressDao

    companion object {
        private const val DATABASE_NAME = "hebrewcards.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
