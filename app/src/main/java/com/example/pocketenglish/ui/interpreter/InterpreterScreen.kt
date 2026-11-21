package com.example.pocketenglish.ui.interpreter

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.tts.TtsStatus

private const val TTS_ERROR_MESSAGE = "音声を再生できません。英語の音声データを確認してください"

/** 入力欄が空のときに出す例文（タップで入力欄に入る）。 */
private val EXAMPLES = listOf(
    "トイレはどこですか？",
    "これを2つください",
    "写真を撮ってもらえますか？",
    "駅までどのくらいかかりますか？",
    "カードで払えますか？",
)

@Composable
fun InterpreterScreen(
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InterpreterViewModel = viewModel(factory = InterpreterViewModel.Factory),
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
            showMessage("マイクが許可されていないため音声入力は使えません。文字で入力してください")
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.speechErrors.collect { error ->
            showMessage(
                when (error) {
                    SpeechError.UNAVAILABLE -> "この端末では音声入力が使えません。文字で入力してください"
                    SpeechError.NO_MATCH -> "聞き取れませんでした。もう一度話してください"
                    SpeechError.NETWORK -> "音声認識に接続できません。電波を確認するか文字で入力してください"
                    SpeechError.PERMISSION -> "マイクの許可が必要です"
                    SpeechError.OTHER -> "音声入力に失敗しました。もう一度お試しください"
                },
            )
        }
    }

    // 別タブに移ったり、アプリを閉じたりしたらマイクを止める
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onScreenHidden() }

    InterpreterContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSubmit = {
            keyboard?.hide()
            viewModel.interpret()
        },
        onClear = viewModel::clear,
        onExample = { example ->
            keyboard?.hide()
            viewModel.onInputChange(example)
            viewModel.interpret()
        },
        onMicTap = {
            when {
                state.speech is SpeechState.Listening -> viewModel.stopListening()
                !viewModel.isSpeechAvailable ->
                    showMessage("この端末では音声入力が使えません。文字で入力してください")
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
        onSlowSpeechChange = viewModel::setSlowSpeech,
        modifier = modifier,
    )
}

@Composable
private fun InterpreterContent(
    state: InterpreterUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onExample: (String) -> Unit,
    onMicTap: () -> Unit,
    onCancelListening: () -> Unit,
    onSpeak: (english: String, utteranceId: String) -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
            Text(text = "瞬間通訳", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "日本語を話すか入力 → 3パターンの英語に。タップで読み上げ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!state.hasApiKey) {
            Banner(
                icon = Icons.Outlined.Info,
                message = "AIのAPIキーが未設定です。内蔵フレーズから近い表現を探して表示します",
            )
        }
        if (state.ttsStatus is TtsStatus.Unavailable) {
            Banner(icon = Icons.Outlined.Info, message = TTS_ERROR_MESSAGE, isError = true)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ResultArea(state = state, onExample = onExample, onSpeak = onSpeak)
        }

        InputPanel(
            state = state,
            onInputChange = onInputChange,
            onSubmit = onSubmit,
            onClear = onClear,
            onMicTap = onMicTap,
            onCancelListening = onCancelListening,
            onSlowSpeechChange = onSlowSpeechChange,
        )
    }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, isError: Boolean = false) {
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
private fun ResultArea(
    state: InterpreterUiState,
    onExample: (String) -> Unit,
    onSpeak: (english: String, utteranceId: String) -> Unit,
) {
    when (val result = state.result) {
        ResultState.Empty -> Examples(onExample = onExample)

        is ResultState.Loading -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("「${result.japanese}」を英語に変換中…", style = MaterialTheme.typography.bodyLarge)
        }

        is ResultState.Success -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { SourceLabel(result.translations.japanese) }
            items(items = EnglishStyle.entries, key = { it.name }) { style ->
                val id = InterpreterViewModel.utteranceId(style)
                val english = result.translations.text(style)
                SpeakableCard(
                    label = style.labelJa,
                    caption = style.hint,
                    english = english,
                    isSpeaking = state.speakingId == id,
                    emphasized = style == EnglishStyle.SHORT,
                    onTap = { onSpeak(english, id) },
                )
            }
        }

        is ResultState.Fallback -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { SourceLabel(result.japanese) }
            item { FallbackNotice(result.error, hasSimilar = result.similar.isNotEmpty()) }
            items(items = result.similar, key = { it.id }) { phrase: Phrase ->
                val id = InterpreterViewModel.utteranceId(phrase)
                SpeakableCard(
                    label = "内蔵フレーズ",
                    caption = phrase.japanese,
                    english = phrase.english,
                    isSpeaking = state.speakingId == id,
                    emphasized = false,
                    onTap = { onSpeak(phrase.english, id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Examples(onExample: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("下のマイクを押して日本語で話してください", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "例文をタップして試す",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            EXAMPLES.forEach { example ->
                AssistChip(onClick = { onExample(example) }, label = { Text(example) })
            }
        }
    }
}

@Composable
private fun SourceLabel(japanese: String) {
    Text(
        text = "「$japanese」",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun FallbackNotice(error: InterpretError, hasSimilar: Boolean) {
    val reason = when (error) {
        InterpretError.NO_API_KEY -> "APIキー未設定のためAI変換は使えません。"
        InterpretError.OFFLINE -> "オフラインのためAI変換できませんでした。"
        InterpretError.TIMEOUT -> "通信がタイムアウトしました。"
        InterpretError.INVALID_KEY -> "APIキーが無効です。local.properties を確認してください。"
        InterpretError.BUSY -> "AIが混み合っています。少し待ってから再度お試しください。"
        InterpretError.SERVER -> "AIサーバーでエラーが発生しました。"
        InterpretError.BAD_RESPONSE -> "AIの返答を読み取れませんでした。もう一度お試しください。"
    }
    val tail = if (hasSimilar) "内蔵フレーズから近いものを表示します" else "内蔵フレーズにも近いものが見つかりませんでした"
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("$reason\n$tail", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SpeakableCard(
    label: String,
    caption: String,
    english: String,
    isSpeaking: Boolean,
    emphasized: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (isSpeaking) colors.primaryContainer else colors.surfaceContainer,
        label = "cardColor",
    )
    val shape = RoundedCornerShape(16.dp)
    Surface(
        color = container,
        contentColor = if (isSpeaking) colors.onPrimaryContainer else colors.onSurface,
        shape = shape,
        border = if (isSpeaking) BorderStroke(2.dp, colors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                role = Role.Button,
                onClickLabel = if (isSpeaking) "再生を止める" else "英語を再生",
                onClick = onTap,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isSpeaking) colors.primary else colors.secondaryContainer,
                        contentColor = if (isSpeaking) colors.onPrimary else colors.onSecondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSpeaking) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = english,
                    style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = if (emphasized) FontWeight.Bold else null,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (isSpeaking) colors.primary else colors.outline,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun InputPanel(
    state: InterpreterUiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onMicTap: () -> Unit,
    onCancelListening: () -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
) {
    val listening = state.speech as? SpeechState.Listening
    val isLoading = state.result is ResultState.Loading

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            OutlinedTextField(
                value = if (listening != null) listening.partial else state.input,
                onValueChange = onInputChange,
                enabled = listening == null,
                placeholder = { Text(if (listening != null) "聞き取り中…" else "日本語で入力") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                trailingIcon = {
                    Row {
                        if (state.input.isNotEmpty() && listening == null) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Filled.Close, contentDescription = "入力を消す")
                            }
                        }
                        IconButton(
                            onClick = onSubmit,
                            enabled = state.input.isNotBlank() && listening == null && !isLoading,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "英語に変換")
                        }
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
                    colors = if (listening != null) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Icon(
                        if (listening != null) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (listening != null) "話し終えたらタップ" else "日本語で話す",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (listening != null) {
                    IconButton(onClick = onCancelListening) {
                        Icon(Icons.Filled.Close, contentDescription = "音声入力をやめる")
                    }
                }
            }
        }
    }
}
