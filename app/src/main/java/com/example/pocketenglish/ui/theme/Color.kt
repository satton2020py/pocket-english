package com.example.pocketenglish.ui.theme

import androidx.compose.ui.graphics.Color

// ネイビー（文字）×サンライズオレンジ（押せるもの）の配色。
// 屋外の直射日光下でも読めるよう、文字と背景のコントラスト比は高めに保つ。

/** ブランドのオレンジ。アプリアイコンやダークテーマのボタンに使う。 */
val SunriseOrange = Color(0xFFFF6A2B)

/** 白文字を載せても読める（4.5:1以上）よう、ライトテーマのボタンは少し濃いオレンジにする。 */
val OrangeStrong = Color(0xFFCC4510)
val OrangeContainerLight = Color(0xFFFFE3D5)
val OnOrangeContainerLight = Color(0xFF3A1300)

val MidnightNavy = Color(0xFF13294B)
val NavyContainerLight = Color(0xFFDCE6F7)
val OnNavyContainerLight = Color(0xFF0A1B36)

// ワンポイント（ヒント）用のあたたかい黄色
val TipContainerLight = Color(0xFFFFF0C7)
val OnTipContainerLight = Color(0xFF3D2B00)

// Light
val SurfaceLight = Color(0xFFF5F7FB)
val CardLight = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFE9EEF6)
val SurfaceContainerHighestLight = Color(0xFFE1E7F1)
val OnSurfaceLight = Color(0xFF0F1A2C)
val OnSurfaceVariantLight = Color(0xFF4D5A72)
val OutlineLight = Color(0xFF6B7690)
val OutlineVariantLight = Color(0xFFD9E0EB)
val ErrorLight = Color(0xFFC62828)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Dark
val OrangeBright = Color(0xFFFF8A50)
val OnOrangeBright = Color(0xFF3A1300)
val OrangeContainerDark = Color(0xFF7A2A06)
val OnOrangeContainerDark = Color(0xFFFFDCCB)
val NavyBright = Color(0xFF2F5597)
val OnNavyBright = Color(0xFFFFFFFF)
val NavyContainerDark = Color(0xFF1E3762)
val OnNavyContainerDark = Color(0xFFDCE6F7)
val TipContainerDark = Color(0xFF4A3800)
val OnTipContainerDark = Color(0xFFFFE8A8)
val SurfaceDark = Color(0xFF0B1322)
val SurfaceContainerLowDark = Color(0xFF121D31)
val CardDark = Color(0xFF16233A)
val SurfaceContainerHighDark = Color(0xFF1E2C46)
val SurfaceContainerHighestDark = Color(0xFF26354F)
val OnSurfaceDark = Color(0xFFEEF2F8)
val OnSurfaceVariantDark = Color(0xFFB4BFD2)
val OutlineDark = Color(0xFF8D99AF)
val OutlineVariantDark = Color(0xFF2A3853)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

/** お気に入りの星。ライト/ダーク両方で背景から浮く色。 */
val FavoriteStarLight = Color(0xFFC77700)
val FavoriteStarDark = Color(0xFFFFC107)

/** フレーズのカテゴリ色（白いアイコンを載せる前提で、どれも白とのコントラスト3:1以上）。 */
val CategoryAirport = Color(0xFF2F6FDB)
val CategoryHotel = Color(0xFF6A4FD8)
val CategoryRestaurant = Color(0xFFC86A00)
val CategoryShopping = Color(0xFFD0336F)
val CategoryEmergency = Color(0xFFD32F2F)
val CategoryGreeting = Color(0xFF0E8A6B)
