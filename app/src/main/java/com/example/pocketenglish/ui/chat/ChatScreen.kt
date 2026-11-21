package com.example.pocketenglish.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketenglish.tts.TtsStatus

@Composable
fun ChatScreen(
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // タブを離れたら聞き取り・読み上げを止める
    DisposableEffect(Unit) {
        onDispose { viewModel.onLeaveScreen() }
    }

    val startListening = { viewModel.startListening(onFailure = showMessage) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            showMessage("マイクが許可されていないため音声入力は使えません。下の文字入力で会話できます")
        }
    }

    val situation = state.situation
    if (situation == null) {
        SituationPicker(
            hasApiKey = state.hasApiKey,
            onSelect = viewModel::selectSituation,
            modifier = modifier,
        )
    } else {
        Conversation(
            situation = situation,
            state = state,
            onBack = viewModel::backToSituations,
            onRestart = viewModel::restart,
            onMicClick = {
                when {
                    state.isListening -> viewModel.stopListening()
                    !viewModel.isSpeechAvailable ->
                        showMessage("この端末では音声認識が使えません。文字入力で会話できます")
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> startListening()
                    else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onSendText = viewModel::send,
            onSpeak = { message ->
                if (!viewModel.toggleSpeak(message)) {
                    showMessage("音声を再生できません。英語の音声データを確認してください")
                }
            },
            onRetry = viewModel::retry,
            onDismissError = viewModel::dismissError,
            modifier = modifier,
        )
    }
}

// ---------------------------------------------------------------------------
// シチュエーション選択
// ---------------------------------------------------------------------------

@Composable
private fun SituationPicker(
    hasApiKey: Boolean,
    onSelect: (ChatSituation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("AI会話", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "場面を選ぶと、AIが相手役になって英語で話しかけてきます。マイクで答えてみましょう",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasApiKey) {
            NoticeCard(
                text = "APIキーが未設定のため、AIの返答は届きません。local.properties に ANTHROPIC_API_KEY を設定してビルドしてください",
            )
        }
        ChatSituation.entries.forEach { situation ->
            Surface(
                onClick = { onSelect(situation) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                situation.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(situation.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            situation.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 会話画面
// ---------------------------------------------------------------------------

@Composable
private fun Conversation(
    situation: ChatSituation,
    state: ChatUiState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onMicClick: () -> Unit,
    onSendText: (String) -> Unit,
    onSpeak: (ChatMessage) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 上部バー
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "場面選択に戻る")
            }
            Icon(situation.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                situation.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRestart) {
                Icon(Icons.Filled.Refresh, contentDescription = "最初からやり直す")
            }
        }

        if (!state.hasApiKey) {
            NoticeCard(
                text = "APIキーが未設定のため、AIの返答は届きません",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.ttsStatus is TtsStatus.Unavailable) {
            NoticeCard(
                text = "英語の音声を再生できません。端末の音声設定で英語の音声データを確認してください",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        val listState = rememberLazyListState()
        val itemCount = state.messages.size + (if (state.isWaitingReply || state.error != null) 1 else 0)
        LaunchedEffect(itemCount) {
            if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(items = state.messages, key = { it.id }) { message ->
                if (message.fromUser) {
                    UserBubble(message)
                } else {
                    AiBubble(
                        message = message,
                        isSpeaking = state.speakingId == message.id,
                        onSpeak = { onSpeak(message) },
                    )
                }
            }
            val error = state.error
            if (state.isWaitingReply) {
                item(key = "waiting") { WaitingBubble() }
            } else if (error != null) {
                item(key = "error") {
                    ErrorCard(error = error, onRetry = onRetry, onDismiss = onDismissError)
                }
            }
        }

        InputArea(
            state = state,
            onMicClick = onMicClick,
            onSendText = onSendText,
        )
    }
}

@Composable
private fun AiBubble(message: ChatMessage, isSpeaking: Boolean, onSpeak: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            onClick = onSpeak,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .semantics { contentDescription = "${message.text}（タップで読み上げ）" },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        message.tip?.let { tip ->
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Filled.Lightbulb,
                        contentDescription = "ワンポイント",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun WaitingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            "相手が考えています…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorCard(error: ChatResult.Failure, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("閉じる") }
                if (error.retryable) {
                    TextButton(onClick = onRetry) { Text("もう一度送る") }
                }
            }
        }
    }
}

@Composable
private fun InputArea(
    state: ChatUiState,
    onMicClick: () -> Unit,
    onSendText: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val canSend = !state.isWaitingReply && !state.isListening
    val submit = {
        if (draft.isNotBlank() && canSend) {
            onSendText(draft)
            draft = ""
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = when {
                    state.isListening && state.partialText.isNotBlank() -> state.partialText
                    state.isListening -> "聞いています… 英語で話してください（もう一度押すと送信）"
                    state.isWaitingReply -> "返答を待っています"
                    else -> "マイクを押して英語で話しかけてみましょう"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            LargeFloatingActionButton(
                onClick = { if (!state.isWaitingReply) onMicClick() },
                containerColor = if (state.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (state.isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.semantics {
                    contentDescription = if (state.isListening) "聞き取りを終えて送信" else "音声で話す"
                },
            ) {
                Icon(
                    if (state.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            // 声を出せない場所（電車内など）向けの文字入力
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("文字で入力することもできます") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { submit() },
                    enabled = draft.isNotBlank() && canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "送信")
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
