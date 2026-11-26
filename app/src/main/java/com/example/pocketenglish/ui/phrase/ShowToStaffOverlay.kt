package com.example.pocketenglish.ui.phrase

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketenglish.data.PhraseData
import com.example.pocketenglish.ui.theme.english

private val Ink = Color(0xFF0F1A2C)
private val InkMuted = Color(0xFF4D5A72)

/**
 * 店員さんなどに画面を向けて見せるための全画面表示。
 * 声に出しにくい場面でも、大きな英文を見せれば伝わる（Google翻訳の全画面表示と同じ考え方）。
 *
 * Dialog だと端末によってナビゲーションバーの余白がとれず下が切れるため、
 * アプリ本体（edge-to-edge）の一番上に重ねて表示し、システムバーの分だけ内側に余白をとる。
 * 屋外でも読めるよう、テーマに関係なく白地に濃い文字。「逆さ」で文字だけ180°回して、向かいの相手に向けられる。
 */
@Composable
fun ShowToStaffOverlay(
    phraseId: String,
    showMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PhraseViewModel = viewModel(factory = PhraseViewModel.Factory),
) {
    val phrase = remember(phraseId) { PhraseData.all.firstOrNull { it.id == phraseId } } ?: return
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSpeaking = state.items.any { it.phrase.id == phraseId && it.isSpeaking }
    var flipped by rememberSaveable(phraseId) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")

    BackHandler(onBack = onDismiss)

    // 見せている間は画面を消さない
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val speak = { slow: Boolean ->
        if (!viewModel.speak(phrase, slow)) showMessage(TTS_ERROR_MESSAGE)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            // 下のタブなどに触れても反応しないよう、タップをここで受け止める
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "相手に見せる",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = Ink)
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { rotationZ = rotation },
            ) {
                Text(
                    text = phrase.english,
                    style = MaterialTheme.typography.headlineSmall.english(),
                    fontSize = 44.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = phrase.japanese,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkMuted,
                )
            }

            OutlinedButton(
                onClick = { flipped = !flipped },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.ScreenRotation, contentDescription = null, tint = Ink)
                Spacer(Modifier.width(8.dp))
                Text(if (flipped) "向きを戻す" else "逆さにして向かいの相手に見せる", color = Ink)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { speak(true) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("ゆっくり")
                }
                Button(
                    onClick = { speak(false) },
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
