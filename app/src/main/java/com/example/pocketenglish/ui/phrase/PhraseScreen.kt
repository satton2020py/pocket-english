package com.example.pocketenglish.ui.phrase

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.model.PhraseCategory
import com.example.pocketenglish.tts.TtsStatus
import com.example.pocketenglish.ui.common.icon
import com.example.pocketenglish.ui.theme.favoriteStarColor

@Composable
fun PhraseScreen(
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhraseViewModel = viewModel(factory = PhraseViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    PhraseContent(
        state = state,
        onSelectFilter = viewModel::selectFilter,
        onPhraseTap = { phrase ->
            val accepted = viewModel.onPhraseTapped(phrase)
            if (!accepted) showMessage("音声を再生できません。英語の音声データを確認してください")
        },
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
}

@Composable
private fun PhraseContent(
    state: PhraseUiState,
    onSelectFilter: (PhraseFilter) -> Unit,
    onPhraseTap: (Phrase) -> Unit,
    onToggleFavorite: (PhraseItem) -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
    onOpenTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Header(state = state)

        val status = state.ttsStatus
        if (status is TtsStatus.Unavailable) {
            TtsWarning(status = status, onOpenSettings = onOpenTtsSettings)
        }

        // カテゴリ切替でリスト先頭に戻す
        val listState = rememberLazyListState()
        LaunchedEffect(state.filter) { listState.scrollToItem(0) }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.items.isEmpty() && state.filter == PhraseFilter.Favorites) {
                EmptyFavorites()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items = state.items, key = { it.phrase.id }) { item ->
                        PhraseCard(
                            item = item,
                            onTap = { onPhraseTap(item.phrase) },
                            onToggleFavorite = { onToggleFavorite(item) },
                        )
                    }
                }
            }
        }

        // 親指が届く画面下部に操作系をまとめる
        BottomControls(
            state = state,
            onSelectFilter = onSelectFilter,
            onSlowSpeechChange = onSlowSpeechChange,
        )
    }
}

@Composable
private fun Header(state: PhraseUiState) {
    val title = when (val f = state.filter) {
        PhraseFilter.Favorites -> "お気に入り"
        is PhraseFilter.Category -> f.category.labelJa
    }
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "タップで再生 ・ 長押し / ★ でお気に入り",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    onToggleFavorite: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (item.isSpeaking) colors.primaryContainer else colors.surfaceContainer,
        label = "cardColor",
    )
    val shape = RoundedCornerShape(16.dp)

    Surface(
        color = container,
        contentColor = if (item.isSpeaking) colors.onPrimaryContainer else colors.onSurface,
        shape = shape,
        border = if (item.isSpeaking) BorderStroke(2.dp, colors.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = if (item.isSpeaking) "再生を止める" else "英語を再生",
                onLongClickLabel = if (item.isFavorite) "お気に入りから外す" else "お気に入りに追加",
                onClick = onTap,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleFavorite()
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (item.isSpeaking) colors.primary else colors.outline,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.phrase.english, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.phrase.japanese,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isSpeaking) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
                item.phrase.note?.let { note ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "💡 $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isSpeaking) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    )
                }
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
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.StarBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("お気に入りはまだありません", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "よく使うフレーズを長押し、または★をタップすると\nここにまとまります",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BottomControls(
    state: PhraseUiState,
    onSelectFilter: (PhraseFilter) -> Unit,
    onSlowSpeechChange: (Boolean) -> Unit,
) {
    val filters: List<PhraseFilter> = remember {
        listOf<PhraseFilter>(PhraseFilter.Favorites) +
            PhraseCategory.entries.map { PhraseFilter.Category(it) }
    }
    val rowState = rememberLazyListState()
    LaunchedEffect(state.filter) {
        val index = filters.indexOf(state.filter)
        if (index >= 0) rowState.animateScrollToItem(index)
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                FilterChip(
                    selected = state.slowSpeech,
                    onClick = { onSlowSpeechChange(!state.slowSpeech) },
                    label = { Text("ゆっくり再生") },
                    leadingIcon = {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = filters, key = { it.toString() }) { filter ->
                    val selected = filter == state.filter
                    when (filter) {
                        PhraseFilter.Favorites -> FilterChip(
                            selected = selected,
                            onClick = { onSelectFilter(filter) },
                            label = { Text("お気に入り ${state.favoriteCount}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = favoriteStarColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = categoryChipColors(),
                            modifier = Modifier.height(44.dp),
                        )
                        is PhraseFilter.Category -> FilterChip(
                            selected = selected,
                            onClick = { onSelectFilter(filter) },
                            label = { Text(filter.category.labelJa) },
                            leadingIcon = {
                                Icon(filter.category.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = categoryChipColors(),
                            modifier = Modifier.height(44.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
)

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
