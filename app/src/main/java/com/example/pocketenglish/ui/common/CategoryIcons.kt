package com.example.pocketenglish.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pocketenglish.model.PhraseCategory
import com.example.pocketenglish.ui.theme.CategoryAirport
import com.example.pocketenglish.ui.theme.CategoryEmergency
import com.example.pocketenglish.ui.theme.CategoryGreeting
import com.example.pocketenglish.ui.theme.CategoryHotel
import com.example.pocketenglish.ui.theme.CategoryRestaurant
import com.example.pocketenglish.ui.theme.CategoryShopping

val PhraseCategory.icon: ImageVector
    get() = when (this) {
        PhraseCategory.AIRPORT -> Icons.Filled.Flight
        PhraseCategory.HOTEL -> Icons.Filled.Hotel
        PhraseCategory.RESTAURANT -> Icons.Filled.Restaurant
        PhraseCategory.SHOPPING -> Icons.Filled.ShoppingBag
        PhraseCategory.EMERGENCY -> Icons.Filled.Sos
        PhraseCategory.GREETING -> Icons.Filled.WavingHand
    }

/** カテゴリごとの色。選択中のチップの背景やアイコンの色に使う（白い文字・アイコンを載せられる濃さ）。 */
val PhraseCategory.color: Color
    get() = when (this) {
        PhraseCategory.AIRPORT -> CategoryAirport
        PhraseCategory.HOTEL -> CategoryHotel
        PhraseCategory.RESTAURANT -> CategoryRestaurant
        PhraseCategory.SHOPPING -> CategoryShopping
        PhraseCategory.EMERGENCY -> CategoryEmergency
        PhraseCategory.GREETING -> CategoryGreeting
    }
