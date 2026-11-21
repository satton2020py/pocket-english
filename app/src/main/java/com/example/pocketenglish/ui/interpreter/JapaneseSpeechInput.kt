package com.example.pocketenglish.ui.interpreter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 音声入力の状態。 */
sealed interface SpeechState {
    data object Idle : SpeechState

    /** 聞き取り中。partial は途中経過の文字列（まだ確定していない）。 */
    data class Listening(val partial: String = "") : SpeechState
}

/** 音声入力が失敗した理由。 */
enum class SpeechError { UNAVAILABLE, NO_MATCH, NETWORK, PERMISSION, OTHER }

/**
 * Android 標準の SpeechRecognizer で日本語を聞き取る。
 * SpeechRecognizer はメインスレッドからのみ操作すること（ViewModel から呼ぶ前提）。
 */
class JapaneseSpeechInput(
    context: Context,
    private val deliverResult: (String) -> Unit,
    private val deliverError: (SpeechError) -> Unit,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start() {
        if (!isAvailable) {
            deliverError(SpeechError.UNAVAILABLE)
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        r.cancel()
        _state.value = SpeechState.Listening()
        r.startListening(intent)
    }

    /** 話し終わりを待たずに確定させる（結果は onResults で届く）。 */
    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = SpeechState.Idle
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults.firstResult() ?: return
            if (_state.value is SpeechState.Listening) _state.value = SpeechState.Listening(text)
        }

        override fun onResults(results: Bundle?) {
            _state.value = SpeechState.Idle
            val text = results.firstResult()
            if (text.isNullOrBlank()) deliverError(SpeechError.NO_MATCH) else deliverResult(text)
        }

        override fun onError(error: Int) {
            _state.value = SpeechState.Idle
            deliverError(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_MATCH
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER -> SpeechError.NETWORK
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION
                    // 連打でエンジンが混み合っただけなら何も言わない
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> return
                    else -> SpeechError.OTHER
                },
            )
        }
    }

    private fun Bundle?.firstResult(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
}
