package com.example.pocketenglish.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android 標準の SpeechRecognizer を英語（en-US）で使うラッパー（AI会話タブ専用）。
 * SpeechRecognizer はメインスレッドから操作する必要がある。
 */
class SpeechInput(context: Context) {

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    /** 認識途中の文字列（話している最中に画面に出す）。 */
    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    /**
     * 認識を開始する。確定した文字列は [onText]、失敗は日本語メッセージで [onFailure] に渡す。
     */
    fun start(onText: (String) -> Unit, onFailure: (String) -> Unit) {
        val sr = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also { recognizer = it }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.let { _partialText.value = it }
            }

            override fun onResults(results: Bundle?) {
                finish()
                val text = firstResult(results)
                if (text.isNullOrBlank()) onFailure(NO_MATCH_MESSAGE) else onText(text)
            }

            override fun onError(error: Int) {
                finish()
                onFailure(errorMessage(error))
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        _partialText.value = ""
        _isListening.value = true
        sr.startListening(intent)
    }

    /** 話し終わりを待たずに確定させる（結果は onResults に届く）。 */
    fun stop() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        finish()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        finish()
    }

    private fun finish() {
        _isListening.value = false
        _partialText.value = ""
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> NO_MATCH_MESSAGE
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "音声認識の通信に失敗しました。文字入力もお使いいただけます"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "マイクの権限がありません。設定から許可してください"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "音声認識が使用中です。少し待ってからもう一度お試しください"
        SpeechRecognizer.ERROR_AUDIO -> "マイクの音声を取得できませんでした"
        else -> "音声認識でエラーが起きました。文字入力もお使いいただけます"
    }

    private companion object {
        const val NO_MATCH_MESSAGE = "うまく聞き取れませんでした。もう一度ゆっくり話してみてください"
    }
}
