package com.example.pocketenglish.ui.interpreter

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pocketenglish.PocketEnglishApp
import com.example.pocketenglish.data.UserPreferencesRepository
import com.example.pocketenglish.data.remote.ApiConfig
import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.tts.TtsManager
import com.example.pocketenglish.tts.TtsStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 3パターンの種類。表示順はこの定義順。 */
enum class EnglishStyle(val labelJa: String, val hint: String) {
    POLITE("丁寧", "店員さん・初対面の人に"),
    CASUAL("カジュアル", "友だち・気軽な場面で"),
    SHORT("超短縮", "とっさの一言で通じる"),
}

/** 結果エリアに何を出すか。 */
sealed interface ResultState {
    data object Empty : ResultState
    data class Loading(val japanese: String) : ResultState
    data class Success(val translations: Translations) : ResultState

    /** AIで変換できなかった。内蔵フレーズから近いものを代わりに出す。 */
    data class Fallback(
        val japanese: String,
        val error: InterpretError,
        val similar: List<Phrase>,
    ) : ResultState
}

data class InterpreterUiState(
    val input: String = "",
    val result: ResultState = ResultState.Empty,
    val speech: SpeechState = SpeechState.Idle,
    val speakingId: String? = null,
    val slowSpeech: Boolean = false,
    val ttsStatus: TtsStatus = TtsStatus.Initializing,
    val hasApiKey: Boolean = ApiConfig.hasApiKey,
)

class InterpreterViewModel(
    context: Context,
    private val prefs: UserPreferencesRepository,
    private val tts: TtsManager,
    private val api: InterpreterApi = InterpreterApi(),
) : ViewModel() {

    private val input = MutableStateFlow("")
    private val result = MutableStateFlow<ResultState>(ResultState.Empty)
    private var interpretJob: Job? = null

    private val _speechErrors = Channel<SpeechError>(Channel.CONFLATED)

    /** 音声入力の失敗（スナックバーで1回だけ知らせる）。 */
    val speechErrors: Flow<SpeechError> = _speechErrors.receiveAsFlow()

    private val speechInput = JapaneseSpeechInput(
        context = context,
        deliverResult = { text ->
            input.value = text
            // 話し終えたらそのまま変換（「瞬間」通訳なのでボタンを押させない）
            interpret()
        },
        deliverError = { _speechErrors.trySend(it) },
    )

    val uiState: StateFlow<InterpreterUiState> = combine(
        combine(input, result, speechInput.state) { a, b, c -> Triple(a, b, c) },
        tts.speakingId,
        prefs.slowSpeech,
        tts.status,
    ) { (text, res, speech), speakingId, slow, ttsStatus ->
        InterpreterUiState(
            input = text,
            result = res,
            speech = speech,
            speakingId = speakingId,
            slowSpeech = slow,
            ttsStatus = ttsStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InterpreterUiState(),
    )

    fun onInputChange(text: String) {
        input.value = text
    }

    fun interpret() {
        val japanese = input.value.trim()
        if (japanese.isEmpty()) return
        interpretJob?.cancel()
        tts.stop()
        result.value = ResultState.Loading(japanese)
        interpretJob = viewModelScope.launch {
            result.value = when (val r = api.interpret(japanese)) {
                is InterpretResult.Success -> ResultState.Success(r.translations)
                is InterpretResult.Failure -> ResultState.Fallback(
                    japanese = japanese,
                    error = r.error,
                    similar = LocalPhraseMatcher.findSimilar(japanese),
                )
            }
        }
    }

    fun clear() {
        interpretJob?.cancel()
        tts.stop()
        input.value = ""
        result.value = ResultState.Empty
    }

    fun startListening() {
        tts.stop()
        speechInput.start()
    }

    fun stopListening() = speechInput.stopListening()

    fun cancelListening() = speechInput.cancel()

    val isSpeechAvailable: Boolean get() = speechInput.isAvailable

    /** @return 再生（または停止）を受け付けたら true。 */
    fun speak(english: String, utteranceId: String): Boolean =
        tts.toggle(english, utteranceId, slow = uiState.value.slowSpeech)

    fun setSlowSpeech(enabled: Boolean) {
        viewModelScope.launch { prefs.setSlowSpeech(enabled) }
    }

    fun onScreenHidden() {
        speechInput.cancel()
    }

    override fun onCleared() {
        speechInput.destroy()
        tts.stop()
    }

    companion object {
        fun utteranceId(style: EnglishStyle) = "interpreter_${style.name}"
        fun utteranceId(phrase: Phrase) = "interpreter_phrase_${phrase.id}"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PocketEnglishApp
                InterpreterViewModel(
                    context = app,
                    prefs = app.container.userPreferencesRepository,
                    tts = app.container.ttsManager,
                )
            }
        }
    }
}
