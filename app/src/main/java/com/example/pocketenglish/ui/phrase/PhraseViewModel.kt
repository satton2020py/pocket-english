package com.example.pocketenglish.ui.phrase

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pocketenglish.PocketEnglishApp
import com.example.pocketenglish.data.PhraseData
import com.example.pocketenglish.data.UserPreferencesRepository
import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.model.PhraseCategory
import com.example.pocketenglish.tts.TtsManager
import com.example.pocketenglish.tts.TtsStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 画面上部のリストに何を出すか。 */
sealed interface PhraseFilter {
    data object Favorites : PhraseFilter
    data class Category(val category: PhraseCategory) : PhraseFilter
}

data class PhraseItem(
    val phrase: Phrase,
    val isFavorite: Boolean,
    val isSpeaking: Boolean,
)

data class PhraseUiState(
    val filter: PhraseFilter = PhraseFilter.Category(PhraseCategory.AIRPORT),
    val items: List<PhraseItem> = emptyList(),
    val favoriteCount: Int = 0,
    val slowSpeech: Boolean = false,
    val ttsStatus: TtsStatus = TtsStatus.Initializing,
)

class PhraseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val prefs: UserPreferencesRepository,
    private val tts: TtsManager,
) : ViewModel() {

    // 選択中フィルタは画面回転・プロセス再生成でも保持する
    private val filterKey = savedStateHandle.getStateFlow(KEY_FILTER, PhraseCategory.AIRPORT.name)

    val uiState: StateFlow<PhraseUiState> = combine(
        filterKey,
        prefs.favoriteIds,
        prefs.slowSpeech,
        tts.speakingId,
        tts.status,
    ) { key, favorites, slow, speakingId, ttsStatus ->
        val filter = decodeFilter(key)
        val source = when (filter) {
            PhraseFilter.Favorites -> PhraseData.all.filter { it.id in favorites }
            is PhraseFilter.Category -> PhraseData.byCategory(filter.category)
        }
        PhraseUiState(
            filter = filter,
            items = source.map { phrase ->
                PhraseItem(
                    phrase = phrase,
                    isFavorite = phrase.id in favorites,
                    isSpeaking = phrase.id == speakingId,
                )
            },
            // データ更新で消えたIDを数えないよう、内蔵データに存在するものだけ数える
            favoriteCount = PhraseData.all.count { it.id in favorites },
            slowSpeech = slow,
            ttsStatus = ttsStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PhraseUiState(),
    )

    fun selectFilter(filter: PhraseFilter) {
        savedStateHandle[KEY_FILTER] = encodeFilter(filter)
    }

    /** @return 再生（または停止）を受け付けたら true。TTSが使えない場合は false。 */
    fun onPhraseTapped(phrase: Phrase): Boolean =
        tts.toggle(phrase.english, phrase.id, slow = uiState.value.slowSpeech)

    fun toggleFavorite(phrase: Phrase) {
        viewModelScope.launch { prefs.toggleFavorite(phrase.id) }
    }

    fun setSlowSpeech(enabled: Boolean) {
        viewModelScope.launch { prefs.setSlowSpeech(enabled) }
    }

    override fun onCleared() {
        tts.stop()
    }

    companion object {
        private const val KEY_FILTER = "phrase_filter"
        private const val FAVORITES = "FAVORITES"

        private fun encodeFilter(filter: PhraseFilter): String = when (filter) {
            PhraseFilter.Favorites -> FAVORITES
            is PhraseFilter.Category -> filter.category.name
        }

        private fun decodeFilter(key: String): PhraseFilter =
            if (key == FAVORITES) {
                PhraseFilter.Favorites
            } else {
                val category = PhraseCategory.entries.firstOrNull { it.name == key }
                    ?: PhraseCategory.AIRPORT
                PhraseFilter.Category(category)
            }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PocketEnglishApp
                PhraseViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    prefs = app.container.userPreferencesRepository,
                    tts = app.container.ttsManager,
                )
            }
        }
    }
}
