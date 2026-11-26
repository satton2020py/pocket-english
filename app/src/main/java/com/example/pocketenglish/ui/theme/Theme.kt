package com.example.pocketenglish.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// primary = 押せるもの（オレンジ）、secondary = 自分の発言など（ネイビー）、tertiary = ヒント（黄色）。
// カードは surfaceContainer（ライトでは白）で、少しグレーの背景から浮かせる。
private val LightColors = lightColorScheme(
    primary = OrangeStrong,
    onPrimary = Color.White,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OnOrangeContainerLight,
    secondary = MidnightNavy,
    onSecondary = Color.White,
    secondaryContainer = NavyContainerLight,
    onSecondaryContainer = OnNavyContainerLight,
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    tertiaryContainer = TipContainerLight,
    onTertiaryContainer = OnTipContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = CardLight,
    surfaceContainerLow = CardLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = OrangeBright,
    onPrimary = OnOrangeBright,
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = OnOrangeContainerDark,
    secondary = NavyBright,
    onSecondary = OnNavyBright,
    secondaryContainer = NavyContainerDark,
    onSecondaryContainer = OnNavyContainerDark,
    tertiary = Color(0xFFFFD36B),
    onTertiary = Color(0xFF3D2B00),
    tertiaryContainer = TipContainerDark,
    onTertiaryContainer = OnTipContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/**
 * 視認性を優先し、Dynamic Color（壁紙由来の色）はあえて使わず固定の高コントラスト配色にする。
 */
@Composable
fun PocketEnglishTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

/** お気に入り星の色（テーマ連動）。 */
val favoriteStarColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) FavoriteStarDark else FavoriteStarLight
