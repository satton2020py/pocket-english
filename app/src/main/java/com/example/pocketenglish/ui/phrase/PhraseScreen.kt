package com.example.pocketenglish.ui.phrase

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketenglish.data.PhraseData
import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.model.PhraseCategory
import com.example.pocketenglish.tts.TtsStatus
import com.example.pocketenglish.ui.common.SlowSpeechChip
import com.example.pocketenglish.ui.common.color
import com.example.pocketenglish.ui.common.icon
import com.example.pocketenglish.ui.theme.english
import com.example.pocketenglish.ui.theme.favoriteStarColor

private const val TTS_ERROR_MESSAGE = "音声を再生できません。英語の音声データを確認してください"

@Composable
fun PhraseScreen(
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhraseViewModel = viewModel(factory = PhraseViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 「見せる」全画面表示中のフレーズ（画面回転でも閉じないよう ID で保持）
    var showingId by rememberSaveable { mutableStateOf<String?>(null) }

    PhraseContent(
        state = state,
        onSelectFilter = viewModel::selectFilter,
        onQueryChange = viewModel::setQuery,
        onPhraseTap = { phrase ->
            val accepted = viewModel.onPhraseTapped(phrase)
            if (!accepted) showMessage(TTS_ERROR_MESSAGE)
        },
        onShow = { phrase -> showingId = phrase.id },
        onToggleFavorite = { item ->
            viewModel.toggleFavorite(item.phrase)
            showMessage(if (item.isFavorite) "お気に入りから外しました" else "★ お気に入りに追加しました")
        },
        onSlowSpeechChange = viewModel::setSlowSpeech,
        onOpenTtsSettings = {
            if (!openTtsSettings(context)) showMessage("音声設定を開けませんでした")
        },
        modifier = modifier,
    )

    val showing = showingId?.let { id -> PhraseData.all.firstOrNull { it.id == id } }
    if (showing != null) {
        ShowToStaffDialog(
            phrase = showing,
            isSpeaking = state.items.any { it.phrase.id == showing.id && it.isSpeaking },
            onSpeak = { slow ->
                if (!viewModel.speak(showing, slow)) showMessage(TTS_ERROR_MESSAGE)
            },
            onDismiss = { showingId = null },
        )
    }
}

@Composable
private fun PhraseContent(
    state: PhraseUiState,
    onSelectFilter: (PhraseFilter) -> Unit,
    onQueryChange: (String) -> Unit,
    onPhraseTap: (Phrase) -> Unit,
    onShow: (Phrase) -> Unit,
    onToggleFavorite: (PhraseItem) -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
    onOpenTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searching = state.query.isNotBlank()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Text(text = "フレーズ", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            SlowSpeechChip(selected = state.slowSpeech, onToggle = { onSlowSpeechChange(!state.slowSpeech) })
        }

        SearchField(query = state.query, onQueryChange = onQueryChange)

        if (!searching) {
            CategoryTiles(state = state, onSelectFilter = onSelectFilter)
        }

        val status = state.ttsStatus
        if (status is TtsStatus.Unavailable) {
            TtsWarning(status = status, onOpenSettings = onOpenTtsSettings)
        }

        Text(
            text = when {
                searching -> "「${state.query.trim()}」の検索結果 ${state.items.size}件"
                else -> "タップで再生 ・ 長押しで相手に見せる"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )

        // カテゴリ切替・検索でリスト先頭に戻す
        val listState = rememberLazyListState()
        LaunchedEffect(state.filter, state.query) { listState.scrollToItem(0) }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.items.isEmpty() && searching -> EmptyMessage(
                    icon = Icons.Filled.Search,
                    title = "見つかりませんでした",
                    body = "別のことばで探すか、瞬間通訳タブで英語にしてみてください",
                )
                state.items.isEmpty() && state.filter == PhraseFilter.Favorites -> EmptyMessage(
                    icon = Icons.Outlined.StarBorder,
                    title = "お気に入りはまだありません",
                    body = "よく使うフレーズの ★ をタップすると\nここにまとまります",
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items = state.items, key = { it.phrase.id }) { item ->
                        PhraseCard(
                            item = item,
                            onTap = { onPhraseTap(item.phrase) },
                            onShow = { onShow(item.phrase) },
                            onToggleFavorite = { onToggleFavorite(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("日本語か英語で探す") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "検索をやめる")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

/** Googleマップのカテゴリのように、アイコン付きの色タイルを横に並べる。 */
@Composable
private fun CategoryTiles(state: PhraseUiState, onSelectFilter: (PhraseFilter) -> Unit) {
    val filters: List<PhraseFilter> = remember {
        PhraseCategory.entries.map { PhraseFilter.Category(it) } + PhraseFilter.Favorites
    }
    val rowState = rememberLazyListState()
    LaunchedEffect(state.filter) {
        val index = filters.indexOf(state.filter)
        if (index >= 0) rowState.animateScrollToItem(index)
    }
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = filters, key = { it.toString() }) { filter ->
            when (filter) {
                PhraseFilter.Favorites -> CategoryTile(
                    label = "お気に入り ${state.favoriteCount}",
                    icon = Icons.Filled.Star,
                    // 白い文字が読める濃さの琥珀色
                    color = Color(0xFFA86200),
                    selected = filter == state.filter,
                    onClick = { onSelectFilter(filter) },
                )
                is PhraseFilter.Category -> CategoryTile(
                    label = filter.category.shortLabel,
                    icon = filter.category.icon,
                    color = filter.category.color,
                    selected = filter == state.filter,
                    onClick = { onSelectFilter(filter) },
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (selected) color else color.copy(alpha = 0.14f),
        label = "tileColor",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .width(84.dp)
            .semantics { this.selected = selected },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else color,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** タイル用の短いカテゴリ名。 */
private val PhraseCategory.shortLabel: String
    get() = when (this) {
        PhraseCategory.AIRPORT -> "空港・移動"
        PhraseCategory.HOTEL -> "ホテル"
        PhraseCategory.RESTAURANT -> "食事"
        PhraseCategory.SHOPPING -> "買い物"
        PhraseCategory.EMERGENCY -> "緊急"
        PhraseCategory.GREETING -> "挨拶"
    }

@Composable
private fun TtsWarning(status: TtsStatus.Unavailable, onOpenSettings: () -> Unit) {
    val message = when (status.reason) {
        TtsStatus.Reason.ENGINE_ERROR -> "音声読み上げエンジンが使えません。"
        TtsStatus.Reason.LANGUAGE_MISSING_DATA -> "英語の音声データが未ダウンロードです。"
        TtsStatus.Reason.LANGUAGE_NOT_SUPPORTED -> "この端末の音声エンジンは英語に未対応です。"
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) { Text("設定") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseCard(
    item: PhraseItem,
    onTap: () -> Unit,
    onShow: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (item.isSpeaking) colors.primaryContainer else colors.surfaceContainer,
        label = "cardColor",
    )
    val shape = RoundedCornerShape(18.dp)

    Surface(
        color = container,
        contentColor = if (item.isSpeaking) colors.onPrimaryContainer else colors.onSurface,
        shape = shape,
        border = BorderStroke(
            if (item.isSpeaking) 2.dp else 1.dp,
            if (item.isSpeaking) colors.primary else colors.outlineVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = if (item.isSpeaking) "再生を止める" else "英語を再生",
                onLongClickLabel = "全画面で相手に見せる",
                onClick = onTap,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShow()
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 話す英文を一番大きく、日本語は一段小さく灰色で添える
                Text(text = item.phrase.english, style = MaterialTheme.typography.titleLarge.english())
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.phrase.japanese,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isSpeaking) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
                item.phrase.note?.let { note ->
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = colors.tertiaryContainer,
                        contentColor = colors.onTertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledIconButton(
                    onClick = onTap,
                    colors = if (item.isSpeaking) {
                        IconButtonDefaults.filledIconButtonColors()
                    } else {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        )
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (item.isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (item.isSpeaking) "再生を止める" else "英語を再生",
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    },
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (item.isFavorite) "お気に入りから外す" else "お気に入りに追加",
                        tint = if (item.isFavorite) favoriteStarColor else colors.outline,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

/**
 * 店員さんなどに画面を向けて見せるための全画面表示。
 * 声に出しにくい場面でも、大きな英文を見せれば伝わる（Google翻訳の全画面表示と同じ考え方）。
 * 屋外でも読めるよう、テーマに関係なく白地に濃い文字で表示する。
 */
@Composable
private fun ShowToStaffDialog(
    phrase: Phrase,
    isSpeaking: Boolean,
    onSpeak: (slow: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        // 画面いっぱいに描き、ステータスバー・ナビゲーションバー（戻る/ホームのボタン）の分だけ内側に余白をとる。
        // decorFitsSystemWindows = true のままだと端末によってはボタンの下に潜り込んで押せなくなる
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "相手に見せる",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF4D5A72),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = Color(0xFF0F1A2C))
                }
            }
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text(
                    text = phrase.english,
                    style = MaterialTheme.typography.headlineSmall.english(),
                    fontSize = 44.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F1A2C),
                )
            }
            Text(
                text = phrase.japanese,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4D5A72),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { onSpeak(true) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("ゆっくり")
                }
                Button(
                    onClick = { onSpeak(false) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Icon(
                        if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSpeaking) "止める" else "再生")
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(icon: ImageVector, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 英語音声データのインストール画面を開く。開けなければ端末のTTS設定へ。 */
private fun openTtsSettings(context: Context): Boolean {
    val intents = listOf(
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
        Intent("com.android.settings.TTS_SETTINGS"),
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (e: ActivityNotFoundException) {
            // 次の候補を試す
        }
    }
    return false
}
