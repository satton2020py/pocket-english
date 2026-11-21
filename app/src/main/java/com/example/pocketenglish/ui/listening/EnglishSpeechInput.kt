package com.example.pocketenglish.ui.listening

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 英語の聞き取り状態。 */
sealed interface HearingState {
    data object Idle : HearingState

    /** 聞き取り中。partial は途中経過の英語（まだ確定していない）。 */
    data class Listening(val partial: String = "") : HearingState
}

/** 聞き取りが失敗した理由。 */
enum class HearingError { UNAVAILABLE, NO_MATCH, NETWORK, PERMISSION, OTHER }

/**
 * Android 標準の SpeechRecognizer で相手が話す英語（en-US）を聞き取る。
 * SpeechRecognizer はメインスレッドからのみ操作すること（ViewModel から呼ぶ前提）。
 */
class EnglishSpeechInput(
    context: Context,
    private val deliverResult: (String) -> Unit,
    private val deliverError: (HearingError) -> Unit,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<HearingState>(HearingState.Idle)
    val state: StateFlow<HearingState> = _state.asStateFlow()

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start() {
        if (!isAvailable) {
            deliverError(HearingError.UNAVAILABLE)
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        r.cancel()
        _state.value = HearingState.Listening()
        r.startListening(intent)
    }

    /** 相手が話し終わるのを待たずに確定させる（結果は onResults で届く）。 */
    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        _state.value = HearingState.Idle
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = HearingState.Idle
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
            if (_state.value is HearingState.Listening) _state.value = HearingState.Listening(text)
        }

        override fun onResults(results: Bundle?) {
            _state.value = HearingState.Idle
            val text = results.firstResult()
            if (text.isNullOrBlank()) deliverError(HearingError.NO_MATCH) else deliverResult(text)
        }

        override fun onError(error: Int) {
            // 途中まで聞き取れていたら、エラーでもその英語を使う（雑音で打ち切られた場合など）
            val partial = (_state.value as? HearingState.Listening)?.partial.orEmpty()
            _state.value = HearingState.Idle
            if (partial.isNotBlank() && error != SpeechRecognizer.ERROR_CLIENT) {
                deliverResult(partial)
                return
            }
            deliverError(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> HearingError.NO_MATCH
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER -> HearingError.NETWORK
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> HearingError.PERMISSION
                    // 連打でエンジンが混み合っただけなら何も言わない
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> return
                    else -> HearingError.OTHER
                },
            )
        }
    }

    private fun Bundle?.firstResult(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
}
