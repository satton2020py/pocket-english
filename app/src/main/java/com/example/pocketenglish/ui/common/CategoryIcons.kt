package com.example.pocketenglish.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pocketenglish.model.PhraseCategory

val PhraseCategory.icon: ImageVector
    get() = when (this) {
        PhraseCategory.AIRPORT -> Icons.Filled.Flight
        PhraseCategory.HOTEL -> Icons.Filled.Hotel
        PhraseCategory.RESTAURANT -> Icons.Filled.Restaurant
        PhraseCategory.SHOPPING -> Icons.Filled.ShoppingBag
        PhraseCategory.EMERGENCY -> Icons.Filled.Warning
        PhraseCategory.GREETING -> Icons.Filled.EmojiPeople
    }
