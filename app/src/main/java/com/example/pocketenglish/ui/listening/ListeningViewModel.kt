package com.example.pocketenglish.ui.listening

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
import com.example.pocketenglish.ui.common.PhotoEncoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** 聞き取った1発言の翻訳状態。 */
sealed interface TranslationState {
    data object Translating : TranslationState
    data class Done(val japanese: String) : TranslationState

    /** AIで訳せなかった。内蔵フレーズから近いものがあれば添える。 */
    data class Failed(val error: TranslateError, val similar: List<Phrase>) : TranslationState
}

/** 聞き取り履歴の1件。[english] は表示・再生に使う英文。 */
data class HeardEntry(
    val id: Long,
    val english: String,
    val translation: TranslationState,
    /** カメラ翻訳の結果。読み取り前・失敗時は [english] が空。 */
    val fromPhoto: Boolean = false,
)

data class ListeningUiState(
    val entries: List<HeardEntry> = emptyList(),
    val input: String = "",
    val hearing: HearingState = HearingState.Idle,
    val speakingId: String? = null,
    val slowSpeech: Boolean = false,
    val ttsStatus: TtsStatus = TtsStatus.Initializing,
    val hasApiKey: Boolean = ApiConfig.hasApiKey,
)

class ListeningViewModel(
    context: Context,
    private val prefs: UserPreferencesRepository,
    private val tts: TtsManager,
    private val api: ListeningApi = ListeningApi(),
) : ViewModel() {

    private val entries = MutableStateFlow<List<HeardEntry>>(emptyList())
    private val input = MutableStateFlow("")
    private val jobs = mutableMapOf<Long, Job>()

    /** 読み取りに失敗した写真（再翻訳用）。成功したら捨てる。 */
    private val photos = mutableMapOf<Long, ByteArray>()
    private var nextId = 0L

    private val _hearingErrors = Channel<HearingError>(Channel.CONFLATED)

    /** 聞き取りの失敗（スナックバーで1回だけ知らせる）。 */
    val hearingErrors: Flow<HearingError> = _hearingErrors.receiveAsFlow()

    private val speechInput = EnglishSpeechInput(
        context = context,
        deliverResult = { text -> addAndTranslate(text) },
        deliverError = { _hearingErrors.trySend(it) },
    )

    val uiState: StateFlow<ListeningUiState> = combine(
        combine(entries, input, speechInput.state) { a, b, c -> Triple(a, b, c) },
        tts.speakingId,
        prefs.slowSpeech,
        tts.status,
    ) { (list, text, hearing), speakingId, slow, ttsStatus ->
        ListeningUiState(
            entries = list,
            input = text,
            hearing = hearing,
            speakingId = speakingId,
            slowSpeech = slow,
            ttsStatus = ttsStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListeningUiState(),
    )

    fun onInputChange(text: String) {
        input.value = text
    }

    /** 文字で入力した英語（看板・メニューなど）を訳す。 */
    fun submitInput() {
        val text = input.value.trim()
        if (text.isEmpty()) return
        input.value = ""
        addAndTranslate(text)
    }

    private fun addAndTranslate(english: String) {
        val id = nextId++
        // 新しいものを一番上に
        entries.update { listOf(HeardEntry(id, english, TranslationState.Translating)) + it.take(MAX_ENTRIES - 1) }
        forgetDroppedPhotos()
        translate(id, english)
    }

    /** カメラ翻訳: 撮った写真の英語を読み取って訳す。 */
    fun addPhoto(photo: File) {
        val id = nextId++
        tts.stop()
        entries.update {
            listOf(HeardEntry(id, english = "", translation = TranslationState.Translating, fromPhoto = true)) +
                it.take(MAX_ENTRIES - 1)
        }
        forgetDroppedPhotos()
        jobs[id] = viewModelScope.launch {
            val jpeg = PhotoEncoder.encodeForUpload(photo)
            if (jpeg == null) {
                updateEntry(id) { it.copy(translation = failed(TranslateError.PHOTO_UNREADABLE)) }
            } else {
                photos[id] = jpeg
                translatePhoto(id, jpeg)
            }
            jobs.remove(id)
        }
    }

    private suspend fun translatePhoto(id: Long, jpeg: ByteArray) {
        when (val r = api.translatePhoto(jpeg)) {
            is TranslateResult.Success -> {
                photos.remove(id)
                updateEntry(id) {
                    it.copy(
                        english = r.translation.english,
                        translation = TranslationState.Done(r.translation.japanese),
                    )
                }
            }
            // 写真の場合は比べる英文がないので、内蔵フレーズの候補は出さない
            is TranslateResult.Failure -> updateEntry(id) { it.copy(translation = failed(r.error)) }
        }
    }

    private fun failed(error: TranslateError) = TranslationState.Failed(error = error, similar = emptyList())

    /** 失敗した翻訳をやり直す。 */
    fun retry(id: Long) {
        val entry = entries.value.firstOrNull { it.id == id } ?: return
        if (entry.fromPhoto && entry.english.isEmpty()) {
            val jpeg = photos[id] ?: return
            updateEntry(id) { it.copy(translation = TranslationState.Translating) }
            jobs[id]?.cancel()
            jobs[id] = viewModelScope.launch {
                translatePhoto(id, jpeg)
                jobs.remove(id)
            }
            return
        }
        updateEntry(id) { it.copy(translation = TranslationState.Translating) }
        translate(id, entry.english)
    }

    private fun translate(id: Long, english: String) {
        jobs[id]?.cancel()
        jobs[id] = viewModelScope.launch {
            when (val r = api.translate(english)) {
                is TranslateResult.Success -> updateEntry(id) {
                    it.copy(
                        english = r.translation.english,
                        translation = TranslationState.Done(r.translation.japanese),
                    )
                }
                is TranslateResult.Failure -> updateEntry(id) {
                    it.copy(
                        translation = TranslationState.Failed(
                            error = r.error,
                            similar = EnglishPhraseMatcher.findSimilar(english),
                        ),
                    )
                }
            }
            jobs.remove(id)
        }
    }

    /** 履歴からあふれた写真をメモリから消す。 */
    private fun forgetDroppedPhotos() {
        val kept = entries.value.mapTo(mutableSetOf()) { it.id }
        photos.keys.retainAll(kept)
    }

    private fun updateEntry(id: Long, transform: (HeardEntry) -> HeardEntry) {
        entries.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun clearHistory() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        tts.stop()
        entries.value = emptyList()
        photos.clear()
    }

    fun startListening() {
        // 自分の端末の読み上げ音を拾わないよう、再生は止めてから聞く
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
        private const val MAX_ENTRIES = 30

        fun utteranceId(entry: HeardEntry) = "listening_${entry.id}"
        fun utteranceId(phrase: Phrase) = "listening_phrase_${phrase.id}"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PocketEnglishApp
                ListeningViewModel(
                    context = app,
                    prefs = app.container.userPreferencesRepository,
                    tts = app.container.ttsManager,
                )
            }
        }
    }
}
