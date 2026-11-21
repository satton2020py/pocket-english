package com.example.pocketenglish.ui.listening

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketenglish.tts.TtsStatus

private const val TTS_ERROR_MESSAGE = "音声を再生できません。英語の音声データを確認してください"

@Composable
fun ListeningScreen(
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListeningViewModel = viewModel(factory = ListeningViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            showMessage("マイクが許可されていないため聞き取りは使えません。英語を文字で入力してください")
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.hearingErrors.collect { error ->
            showMessage(
                when (error) {
                    HearingError.UNAVAILABLE -> "この端末では音声認識が使えません。英語を文字で入力してください"
                    HearingError.NO_MATCH -> "英語を聞き取れませんでした。相手の近くでもう一度どうぞ"
                    HearingError.NETWORK -> "音声認識に接続できません。電波を確認してください"
                    HearingError.PERMISSION -> "マイクの許可が必要です"
                    HearingError.OTHER -> "聞き取りに失敗しました。もう一度お試しください"
                },
            )
        }
    }

    // 別タブに移ったり、アプリを閉じたりしたらマイクを止める
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onScreenHidden() }

    ListeningContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSubmit = {
            keyboard?.hide()
            viewModel.submitInput()
        },
        onMicTap = {
            when {
                state.hearing is HearingState.Listening -> viewModel.stopListening()
                !viewModel.isSpeechAvailable ->
                    showMessage("この端末では音声認識が使えません。英語を文字で入力してください")
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> {
                    keyboard?.hide()
                    viewModel.startListening()
                }
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onCancelListening = viewModel::cancelListening,
        onSpeak = { english, id ->
            if (!viewModel.speak(english, id)) showMessage(TTS_ERROR_MESSAGE)
        },
        onRetry = viewModel::retry,
        onClearHistory = viewModel::clearHistory,
        onSlowSpeechChange = viewModel::setSlowSpeech,
        modifier = modifier,
    )
}

@Composable
private fun ListeningContent(
    state: ListeningUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMicTap: () -> Unit,
    onCancelListening: () -> Unit,
    onSpeak: (english: String, utteranceId: String) -> Unit,
    onRetry: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "英語の聞き取り", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "相手の英語を聞き取って日本語に。英文タップでもう一度再生",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.entries.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "履歴を消す")
                }
            }
        }

        if (!state.hasApiKey) {
            Banner(
                icon = Icons.Outlined.Info,
                message = "AIのAPIキーが未設定のため、聞き取った英語だけを表示します（内蔵フレーズに近いものがあれば訳を添えます）",
            )
        }
        if (state.ttsStatus is TtsStatus.Unavailable) {
            Banner(icon = Icons.Outlined.Info, message = TTS_ERROR_MESSAGE, isError = true)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.entries.isEmpty() && state.hearing !is HearingState.Listening) {
                EmptyGuide()
            } else {
                History(state = state, onSpeak = onSpeak, onRetry = onRetry)
            }
        }

        InputPanel(
            state = state,
            onInputChange = onInputChange,
            onSubmit = onSubmit,
            onMicTap = onMicTap,
            onCancelListening = onCancelListening,
            onSlowSpeechChange = onSlowSpeechChange,
        )
    }
}

@Composable
private fun Banner(icon: ImageVector, message: String, isError: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isError) colors.errorContainer else colors.secondaryContainer,
        contentColor = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyGuide() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Hearing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("下のボタンを押して、相手に話してもらいましょう", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "スマホを相手に向けると聞き取りやすくなります。\n看板やメニューの英語は文字で入力しても訳せます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun History(
    state: ListeningUiState,
    onSpeak: (english: String, utteranceId: String) -> Unit,
    onRetry: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val listening = state.hearing as? HearingState.Listening
    val newestId = state.entries.firstOrNull()?.id
    // 新しい発言が来たら先頭（最新）までスクロールする
    LaunchedEffect(newestId, listening != null) { listState.animateScrollToItem(0) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (listening != null) {
            item(key = "listening") { ListeningCard(partial = listening.partial) }
        }
        items(items = state.entries, key = { it.id }) { entry ->
            val id = ListeningViewModel.utteranceId(entry)
            EntryCard(
                entry = entry,
                isSpeaking = state.speakingId == id,
                speakingId = state.speakingId,
                onSpeak = onSpeak,
                onRetry = { onRetry(entry.id) },
            )
        }
    }
}

@Composable
private fun ListeningCard(partial: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, colors.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Hearing, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("聞き取り中…", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = partial.ifEmpty { "（英語を待っています）" },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: HeardEntry,
    isSpeaking: Boolean,
    speakingId: String?,
    onSpeak: (english: String, utteranceId: String) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isSpeaking) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (isSpeaking) colors.onPrimaryContainer else colors.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = if (isSpeaking) BorderStroke(2.dp, colors.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.english,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onSpeak(entry.english, ListeningViewModel.utteranceId(entry)) }) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSpeaking) "再生を止める" else "英語をもう一度再生",
                        tint = if (isSpeaking) colors.primary else colors.outline,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp))
            when (val t = entry.translation) {
                TranslationState.Translating -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("日本語に翻訳中…", style = MaterialTheme.typography.bodyMedium)
                }

                is TranslationState.Done -> Text(
                    text = t.japanese,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp),
                )

                is TranslationState.Failed -> FailedTranslation(
                    state = t,
                    speakingId = speakingId,
                    onSpeak = onSpeak,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun FailedTranslation(
    state: TranslationState.Failed,
    speakingId: String?,
    onSpeak: (english: String, utteranceId: String) -> Unit,
    onRetry: () -> Unit,
) {
    val reason = when (state.error) {
        TranslateError.NO_API_KEY -> "APIキー未設定のため翻訳できません。"
        TranslateError.OFFLINE -> "オフラインのため翻訳できませんでした。"
        TranslateError.TIMEOUT -> "通信がタイムアウトしました。"
        TranslateError.INVALID_KEY -> "APIキーが無効です。local.properties を確認してください。"
        TranslateError.BUSY -> "無料枠の上限に達したか、混み合っています。少し待ってお試しください。"
        TranslateError.SERVER -> "AIサーバーでエラーが発生しました。"
        TranslateError.BAD_RESPONSE -> "AIの返答を読み取れませんでした。"
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (state.error != TranslateError.NO_API_KEY && state.error != TranslateError.INVALID_KEY) {
                TextButton(onClick = onRetry) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("再翻訳")
                }
            }
        }
        state.similar.forEach { phrase ->
            val id = ListeningViewModel.utteranceId(phrase)
            Spacer(Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(
                            "近い内蔵フレーズ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(phrase.english, style = MaterialTheme.typography.bodyLarge)
                        Text(phrase.japanese, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onSpeak(phrase.english, id) }) {
                        Icon(
                            imageVector = if (speakingId == id) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "フレーズを再生",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputPanel(
    state: ListeningUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMicTap: () -> Unit,
    onCancelListening: () -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
) {
    val listening = state.hearing is HearingState.Listening

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                enabled = !listening,
                placeholder = { Text("英語を文字で入力（看板・メニューなど）") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                trailingIcon = {
                    IconButton(onClick = onSubmit, enabled = state.input.isNotBlank() && !listening) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "日本語に翻訳")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = state.slowSpeech,
                    onClick = { onSlowSpeechChange(!state.slowSpeech) },
                    label = { Text("ゆっくり再生") },
                    leadingIcon = {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onMicTap,
                    colors = if (listening) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Icon(if (listening) Icons.Filled.Stop else Icons.Filled.Hearing, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (listening) "聞き終えたらタップ" else "英語を聞き取る",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (listening) {
                    IconButton(onClick = onCancelListening) {
                        Icon(Icons.Filled.Close, contentDescription = "聞き取りをやめる")
                    }
                }
            }
        }
    }
}
