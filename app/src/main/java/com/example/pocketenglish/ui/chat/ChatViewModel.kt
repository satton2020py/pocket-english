package com.example.pocketenglish.ui.chat

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pocketenglish.PocketEnglishApp
import com.example.pocketenglish.data.remote.ApiConfig
import com.example.pocketenglish.tts.TtsManager
import com.example.pocketenglish.tts.TtsStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 画面に出す1発言。AIの発言には [tip]（直前のユーザー発話へのワンポイント）が付くことがある。 */
data class ChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val tip: String? = null,
)

data class ChatUiState(
    /** null のときはシチュエーション選択画面。 */
    val situation: ChatSituation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isWaitingReply: Boolean = false,
    val error: ChatResult.Failure? = null,
    val isListening: Boolean = false,
    val partialText: String = "",
    val speakingId: String? = null,
    val ttsStatus: TtsStatus = TtsStatus.Initializing,
    val hasApiKey: Boolean = ApiConfig.hasApiKey,
)

private data class Conversation(
    val situation: ChatSituation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isWaitingReply: Boolean = false,
    val error: ChatResult.Failure? = null,
)

class ChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val tts: TtsManager,
    private val speech: SpeechInput,
    private val client: ClaudeChatClient,
) : ViewModel() {

    private val conversation = MutableStateFlow(
        Conversation(situation = savedStateHandle.get<String>(KEY_SITUATION)?.let(::decodeSituation)),
    )
    private var requestJob: Job? = null
    private var nextId = 0L

    init {
        // プロセス再生成でシチュエーションだけ復元された場合は、最初のひと言から始め直す
        conversation.value.situation?.let { startConversation(it, speakOpening = false) }
    }

    val uiState: StateFlow<ChatUiState> = combine(
        conversation,
        speech.isListening,
        speech.partialText,
        tts.speakingId,
        tts.status,
    ) { conv, listening, partial, speakingId, ttsStatus ->
        ChatUiState(
            situation = conv.situation,
            messages = conv.messages,
            isWaitingReply = conv.isWaitingReply,
            error = conv.error,
            isListening = listening,
            partialText = partial,
            speakingId = speakingId,
            ttsStatus = ttsStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(situation = conversation.value.situation, messages = conversation.value.messages),
    )

    val isSpeechAvailable: Boolean get() = speech.isAvailable

    fun selectSituation(situation: ChatSituation) {
        savedStateHandle[KEY_SITUATION] = situation.name
        startConversation(situation, speakOpening = true)
    }

    /** 同じシチュエーションで最初からやり直す。 */
    fun restart() {
        conversation.value.situation?.let { startConversation(it, speakOpening = true) }
    }

    fun backToSituations() {
        cancelAll()
        savedStateHandle[KEY_SITUATION] = null
        conversation.value = Conversation()
    }

    /** 音声入力を開始する。認識結果はそのままAIに送る。 */
    fun startListening(onFailure: (String) -> Unit) {
        if (conversation.value.isWaitingReply) return
        // 読み上げ音声をマイクが拾わないよう先に止める
        tts.stop()
        speech.start(onText = ::send, onFailure = onFailure)
    }

    fun stopListening() = speech.stop()

    /** タブ切替などで画面を離れたときに、聞き取りと読み上げを止める。 */
    fun onLeaveScreen() {
        speech.cancel()
        tts.stop()
    }

    fun send(text: String) {
        val trimmed = text.trim()
        val conv = conversation.value
        if (trimmed.isEmpty() || conv.situation == null || conv.isWaitingReply) return
        val userMessage = ChatMessage(id = newId(), fromUser = true, text = trimmed)
        conversation.update { it.copy(messages = it.messages + userMessage, error = null) }
        requestReply()
    }

    /** 通信エラー後に、同じ履歴でもう一度返答をもらう。 */
    fun retry() {
        if (conversation.value.messages.lastOrNull()?.fromUser == true) requestReply()
    }

    fun dismissError() {
        conversation.update { it.copy(error = null) }
    }

    /** @return 再生を受け付けたら true。TTSが使えない場合は false。 */
    fun toggleSpeak(message: ChatMessage): Boolean = tts.toggle(message.text, message.id)

    private fun startConversation(situation: ChatSituation, speakOpening: Boolean) {
        cancelAll()
        val opening = ChatMessage(id = newId(), fromUser = false, text = situation.opening)
        conversation.value = Conversation(situation = situation, messages = listOf(opening))
        if (speakOpening) tts.speak(opening.text, opening.id)
    }

    private fun requestReply() {
        val conv = conversation.value
        val situation = conv.situation ?: return
        val history = conv.messages.map { ChatTurn(fromUser = it.fromUser, text = it.text) }
        conversation.update { it.copy(isWaitingReply = true, error = null) }
        requestJob = viewModelScope.launch {
            when (val result = client.reply(situation, history)) {
                is ChatResult.Success -> {
                    val aiMessage = ChatMessage(
                        id = newId(),
                        fromUser = false,
                        text = result.reply.reply,
                        tip = result.reply.tip.ifBlank { null },
                    )
                    conversation.update {
                        it.copy(messages = it.messages + aiMessage, isWaitingReply = false)
                    }
                    tts.speak(aiMessage.text, aiMessage.id)
                }
                is ChatResult.Failure -> conversation.update {
                    it.copy(isWaitingReply = false, error = result)
                }
            }
        }
    }

    private fun cancelAll() {
        requestJob?.cancel()
        requestJob = null
        speech.cancel()
        tts.stop()
    }

    private fun newId(): String = "chat-${nextId++}"

    override fun onCleared() {
        requestJob?.cancel()
        speech.destroy()
        tts.stop()
    }

    companion object {
        private const val KEY_SITUATION = "chat_situation"

        private fun decodeSituation(name: String): ChatSituation? =
            ChatSituation.entries.firstOrNull { it.name == name }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                ChatViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    tts = (app as PocketEnglishApp).container.ttsManager,
                    speech = SpeechInput(app),
                    client = ClaudeChatClient(),
                )
            }
        }
    }
}
