package com.example.pocketenglish.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** マイクの左右に置く小さな丸ボタン（カメラ・キーボードなど）。 */
data class DockAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val selected: Boolean = false,
)

private val MicSize = 76.dp
private val SideSize = 52.dp

/**
 * 音声で使う3タブ（AI会話・瞬間通訳・聞き取り）共通の操作部。
 * 「左の小ボタン / 中央の大きなマイク / 右の小ボタン」をどのタブでも同じ位置に置き、迷わず押せるようにする。
 * マイクは下に影がある立体的なボタンで、押すと沈む。聞き取り中は赤くなり、周りが脈打つ。
 */
@Composable
fun VoiceDock(
    isListening: Boolean,
    micIcon: ImageVector,
    micContentDescription: String,
    onMicClick: () -> Unit,
    caption: String,
    modifier: Modifier = Modifier,
    micEnabled: Boolean = true,
    leading: DockAction? = null,
    trailing: DockAction? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            SideButton(leading)
            MicButton(
                isListening = isListening,
                icon = micIcon,
                contentDescription = micContentDescription,
                enabled = micEnabled,
                onClick = onMicClick,
            )
            SideButton(trailing)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun SideButton(action: DockAction?) {
    if (action == null) {
        // マイクを常に中央に保つため、ボタンがなくても同じ幅を空けておく
        Spacer(Modifier.size(SideSize))
        return
    }
    val colors = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick = action.onClick,
        enabled = action.enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (action.selected) colors.secondary else colors.surfaceContainerHigh,
            contentColor = if (action.selected) colors.onSecondary else colors.onSurface,
        ),
        modifier = Modifier.size(SideSize),
    ) {
        Icon(action.icon, contentDescription = action.contentDescription, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val face = when {
        !enabled -> colors.onSurface.copy(alpha = 0.12f).compositeOver(colors.surface)
        isListening -> colors.error
        else -> colors.primary
    }
    val contentColor = when {
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        isListening -> colors.onError
        else -> colors.onPrimary
    }
    // 下側の影（立体感）。押している間は面が沈んで影が隠れる
    val edge = Color.Black.copy(alpha = 0.28f).compositeOver(face)
    val depth by animateDpAsState(if (pressed || !enabled) 0.dp else 5.dp, label = "micDepth")

    val pulse = rememberInfiniteTransition(label = "micPulse")
    val ring by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ring",
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(MicSize + 24.dp)) {
        if (isListening) {
            Box(
                Modifier
                    .size(MicSize)
                    .scale(ring)
                    .clip(CircleShape)
                    .background(colors.error.copy(alpha = 0.22f)),
            )
        }
        Box(
            Modifier
                .size(MicSize)
                .offset(y = 5.dp)
                .clip(CircleShape)
                .background(edge),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(MicSize)
                .offset(y = 5.dp - depth)
                .clip(CircleShape)
                .background(face)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                )
                .semantics { this.contentDescription = contentDescription },
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(38.dp))
        }
    }
}

/** 「ゆっくり再生」の切り替え。各画面のタイトル行の右端に置く。 */
@Composable
fun SlowSpeechChip(selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text("ゆっくり") },
        leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier,
    )
}
