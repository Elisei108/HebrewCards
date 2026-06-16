package com.hebrewcards.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hebrewcards.data.db.AppDatabase
import com.hebrewcards.data.repository.DeckRepository
import com.hebrewcards.domain.model.DeckType
import java.io.File

sealed class ExportResult {
    data class Success(val fileCount: Int, val folderPath: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

class ExportDeckUseCase(
    private val repo: DeckRepository,
    private val db: AppDatabase
) {

    suspend fun exportAll(context: Context): ExportResult {
        return try {
            val decks = repo.getAllDecksOnce()
            if (decks.isEmpty()) return ExportResult.Error("Нет колод для экспорта")

            var exported = 0

            for (deck in decks) {
                val cards = db.cardDao().getCardsByDeckOnce(deck.id)
                if (cards.isEmpty()) continue

                // Собираем CSV текст в зависимости от типа колоды
                val csvText = buildString {
                    if (deck.type == DeckType.VERB) {
                        appendLine("#VERB")
                        for (card in cards) {
                            val verb = db.cardDao().getVerbCard(card.id) ?: continue
                            appendLine("${card.hebrew};${card.russian};${verb.binyan};${verb.root};${verb.presentSingular}")
                        }
                    } else {
                        for (card in cards) {
                            appendLine("${card.hebrew};${card.russian}")
                        }
                    }
                }.trimEnd()

                // Имя файла — только ASCII-символы
                val fileName = deck.name
                    .replace(" ", "_")
                    .replace("·", "")
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                    .lowercase()
                    .ifBlank { "deck_${deck.id}" } + ".csv"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29+: MediaStore.Downloads, разрешение не нужно
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                    ) ?: continue
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csvText.toByteArray(Charsets.UTF_8))
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                } else {
                    // API 26-28: записываем в приватную папку Downloads приложения
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    File(dir, fileName).writeText(csvText, Charsets.UTF_8)
                }

                exported++
            }

            if (exported == 0) ExportResult.Error("Нет карточек для экспорта")
            else ExportResult.Success(exported, "Загрузки")
        } catch (e: Exception) {
            ExportResult.Error("Ошибка экспорта: ${e.message}")
        }
    }
}
