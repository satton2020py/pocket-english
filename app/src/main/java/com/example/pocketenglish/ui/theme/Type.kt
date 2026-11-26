package com.example.pocketenglish.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pocketenglish.R

/**
 * 英文用の丸ゴシック系フォント Nunito（SIL Open Font License 1.1、res/font に同梱）。
 * 日本語の字形は持たないので、日本語の文字は端末標準のフォントで表示される。
 */
val EnglishFontFamily = FontFamily(
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

/** 話す英文に使う見た目（Nunito ExtraBold）。日本語より一段目立たせる。 */
fun TextStyle.english(): TextStyle = copy(fontFamily = EnglishFontFamily, fontWeight = FontWeight.ExtraBold)

// 移動中でも一瞬で読めるよう、英文は大きく太めに。日本語は端末標準（Noto Sans CJK）を使う
val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
)
