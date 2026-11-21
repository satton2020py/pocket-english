package com.example.pocketenglish.ui.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * ロールプレイのシチュエーション。
 * [opening] はAIの最初のひと言（API呼び出しなしで即表示・読み上げする）。
 * [role] はシステムプロンプトに入れるAIの役どころ。
 */
enum class ChatSituation(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val role: String,
    val opening: String,
) {
    CAFE(
        label = "カフェで注文",
        description = "店員さんとサイズ・持ち帰りなどのやり取り",
        icon = Icons.Filled.LocalCafe,
        role = "You are a friendly barista at a busy coffee shop in New York. " +
            "Take the customer's order step by step: drink, size, hot or iced, for here or to go, " +
            "name for the cup, and payment.",
        opening = "Hi there! What can I get for you today?",
    ),
    IMMIGRATION(
        label = "入国審査",
        description = "目的・滞在期間・滞在先を聞かれる練習",
        icon = Icons.Filled.Badge,
        role = "You are an immigration officer at a US airport. Be polite but brief and official. " +
            "Ask typical questions one at a time: purpose of visit, length of stay, " +
            "where they are staying, occupation, return ticket. Finish by welcoming them.",
        opening = "Good afternoon. May I see your passport, please? What's the purpose of your visit?",
    ),
    DIRECTIONS(
        label = "道案内",
        description = "道を尋ねる・教えてもらう練習",
        icon = Icons.Filled.Map,
        role = "You are a kind local person on a street in London. The traveler (the user) has just " +
            "stopped you. Help them find places, give simple directions with landmarks, " +
            "and suggest transport if it is far.",
        opening = "Oh, hi! You look a little lost. Can I help you find something?",
    ),
    FREE_TALK(
        label = "フリートーク",
        description = "旅行の話題で自由におしゃべり",
        icon = Icons.Filled.Forum,
        role = "You are a cheerful fellow traveler chatting at a hostel lounge. " +
            "Talk casually about travel, food, plans, and hobbies, and ask the user easy follow-up questions.",
        opening = "Hey! Where are you from? Is this your first time traveling here?",
    ),
}
