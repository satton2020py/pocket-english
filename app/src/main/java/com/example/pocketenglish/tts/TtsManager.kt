package com.example.pocketenglish.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** TTSエンジンの状態。 */
sealed interface TtsStatus {
    data object Initializing : TtsStatus
    data object Ready : TtsStatus

    /** 英語音声データが端末にない、またはエンジン自体が使えない。 */
    data class Unavailable(val reason: Reason) : TtsStatus

    enum class Reason { ENGINE_ERROR, LANGUAGE_MISSING_DATA, LANGUAGE_NOT_SUPPORTED }
}

/**
 * Android標準の TextToSpeech をラップし、Compose から扱いやすい StateFlow を公開する。
 * アプリ全体で1インスタンスを共有する（全タブで同じエンジンを使い回す）。
 */
class TtsManager(context: Context) {

    private val _status = MutableStateFlow<TtsStatus>(TtsStatus.Initializing)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    /** 現在読み上げ中の utteranceId（フレーズID）。読み上げていなければ null。 */
    private val _speakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    /** 初期化完了前にタップされた発話。準備でき次第1回だけ再生する。 */
    @Volatile
    private var pending: PendingUtterance? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { initStatus ->
        onInit(initStatus)
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speakingId.value = utteranceId
            }

            override fun onDone(utteranceId: String?) {
                clearIfCurrent(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                clearIfCurrent(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                clearIfCurrent(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                clearIfCurrent(utteranceId)
            }
        })
    }

    private fun onInit(initStatus: Int) {
        if (initStatus != TextToSpeech.SUCCESS) {
            _status.value = TtsStatus.Unavailable(TtsStatus.Reason.ENGINE_ERROR)
            return
        }
        val result = tts.setLanguage(Locale.US)
        _status.value = when (result) {
            TextToSpeech.LANG_MISSING_DATA ->
                TtsStatus.Unavailable(TtsStatus.Reason.LANGUAGE_MISSING_DATA)
            TextToSpeech.LANG_NOT_SUPPORTED ->
                TtsStatus.Unavailable(TtsStatus.Reason.LANGUAGE_NOT_SUPPORTED)
            else -> TtsStatus.Ready
        }
        if (_status.value == TtsStatus.Ready) {
            pending?.let { speakNow(it) }
        }
        pending = null
    }

    /**
     * 英文を読み上げる。再生中の音声は中断して新しい音声を優先する（連打しても詰まらない）。
     * @return 再生を開始（または初期化待ちで予約）できたら true
     */
    fun speak(text: String, utteranceId: String, slow: Boolean = false): Boolean {
        val utterance = PendingUtterance(text, utteranceId, if (slow) SLOW_RATE else NORMAL_RATE)
        return when (_status.value) {
            TtsStatus.Ready -> speakNow(utterance)
            TtsStatus.Initializing -> {
                pending = utterance
                true
            }
            is TtsStatus.Unavailable -> false
        }
    }

    /** 同じフレーズを再生中ならストップ、そうでなければ再生する。 */
    fun toggle(text: String, utteranceId: String, slow: Boolean = false): Boolean {
        if (_speakingId.value == utteranceId) {
            stop()
            return true
        }
        return speak(text, utteranceId, slow)
    }

    fun stop() {
        pending = null
        if (_status.value == TtsStatus.Ready) {
            tts.stop()
        }
        _speakingId.value = null
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    private fun speakNow(utterance: PendingUtterance): Boolean {
        tts.setSpeechRate(utterance.rate)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utterance.id)
        }
        val result = tts.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, params, utterance.id)
        return result == TextToSpeech.SUCCESS
    }

    private fun clearIfCurrent(utteranceId: String?) {
        _speakingId.compareAndSet(utteranceId, null)
    }

    private data class PendingUtterance(val text: String, val id: String, val rate: Float)

    private companion object {
        const val NORMAL_RATE = 0.95f
        const val SLOW_RATE = 0.65f
    }
}
