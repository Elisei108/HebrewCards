package com.hebrewcards.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Менеджер TTS. Язык всегда he-IL.
 * Поддерживает сторонние движки (HebrewTTS и др.).
 * Произносит только по нажатию кнопки — не автоматически.
 */
class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speechRate = 1.0f

    companion object {
        private const val TAG = "TtsManager"
        val LOCALE_HEBREW = Locale("he", "IL")
    }

    fun getAvailableEngines(): List<TextToSpeech.EngineInfo> {
        val temp = TextToSpeech(context, null)
        val engines = temp.engines
        temp.shutdown()
        return engines
    }

    fun init(enginePackage: String? = null, onReady: () -> Unit = {}) {
        tts?.shutdown()
        isReady = false

        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(LOCALE_HEBREW)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Иврит не поддерживается — fallback на системный движок")
                    initFallback(onReady)
                } else {
                    isReady = true
                    tts?.setSpeechRate(speechRate)
                    onReady()
                }
            } else {
                initFallback(onReady)
            }
        }

        tts = if (enginePackage != null) TextToSpeech(context, listener, enginePackage)
               else TextToSpeech(context, listener)
    }

    private fun initFallback(onReady: () -> Unit) {
        tts?.shutdown()
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(LOCALE_HEBREW)
                isReady = true
                tts?.setSpeechRate(speechRate)
            }
            onReady()
        }
    }

    fun speak(text: String) {
        if (!isReady) { Log.w(TAG, "TTS не готов"); return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hc_word")
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    val ready: Boolean get() = isReady
}
