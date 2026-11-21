package com.example.pocketenglish.data

import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.model.PhraseCategory

/** オフラインで使える内蔵フレーズ（各カテゴリ5件）。 */
object PhraseData {

    val all: List<Phrase> = listOf(
        // 空港・移動
        Phrase("airport_01", PhraseCategory.AIRPORT, "I'm here for sightseeing.", "観光で来ました。", "入国審査で目的を聞かれたら"),
        Phrase("airport_02", PhraseCategory.AIRPORT, "I'll be staying for five days.", "5日間滞在します。", "滞在期間を聞かれたら（数字を入れ替え）"),
        Phrase("airport_03", PhraseCategory.AIRPORT, "Where is the baggage claim?", "手荷物受取所はどこですか？"),
        Phrase("airport_04", PhraseCategory.AIRPORT, "How do I get to downtown?", "市内へはどう行けばいいですか？"),
        Phrase("airport_05", PhraseCategory.AIRPORT, "Could you take me to this address, please?", "この住所までお願いします。", "タクシーで住所を見せながら"),

        // ホテル
        Phrase("hotel_01", PhraseCategory.HOTEL, "I have a reservation under Tanaka.", "田中で予約しています。", "名前は自分の名字に"),
        Phrase("hotel_02", PhraseCategory.HOTEL, "Can I check in early?", "早めにチェックインできますか？"),
        Phrase("hotel_03", PhraseCategory.HOTEL, "What's the Wi-Fi password?", "Wi-Fiのパスワードは何ですか？"),
        Phrase("hotel_04", PhraseCategory.HOTEL, "Could you keep my luggage until three?", "3時まで荷物を預かってもらえますか？"),
        Phrase("hotel_05", PhraseCategory.HOTEL, "The air conditioner isn't working.", "エアコンが動きません。"),

        // レストラン
        Phrase("restaurant_01", PhraseCategory.RESTAURANT, "A table for two, please.", "2名でお願いします。"),
        Phrase("restaurant_02", PhraseCategory.RESTAURANT, "What do you recommend?", "おすすめは何ですか？"),
        Phrase("restaurant_03", PhraseCategory.RESTAURANT, "I'll have this one, please.", "これをお願いします。", "メニューを指さしながら"),
        Phrase("restaurant_04", PhraseCategory.RESTAURANT, "I'm allergic to nuts.", "ナッツアレルギーがあります。"),
        Phrase("restaurant_05", PhraseCategory.RESTAURANT, "Can I get the check, please?", "お会計をお願いします。"),

        // 買い物
        Phrase("shopping_01", PhraseCategory.SHOPPING, "I'm just looking, thanks.", "見ているだけです、ありがとう。"),
        Phrase("shopping_02", PhraseCategory.SHOPPING, "How much is this?", "これはいくらですか？"),
        Phrase("shopping_03", PhraseCategory.SHOPPING, "Do you have this in a smaller size?", "これの小さいサイズはありますか？"),
        Phrase("shopping_04", PhraseCategory.SHOPPING, "Can I pay by card?", "カードで払えますか？"),
        Phrase("shopping_05", PhraseCategory.SHOPPING, "Could I get a bag, please?", "袋をもらえますか？"),

        // 緊急・トラブル
        Phrase("emergency_01", PhraseCategory.EMERGENCY, "Please help me!", "助けてください！"),
        Phrase("emergency_02", PhraseCategory.EMERGENCY, "I lost my passport.", "パスポートをなくしました。"),
        Phrase("emergency_03", PhraseCategory.EMERGENCY, "Please call an ambulance.", "救急車を呼んでください。"),
        Phrase("emergency_04", PhraseCategory.EMERGENCY, "Where is the nearest police station?", "一番近い警察署はどこですか？"),
        Phrase("emergency_05", PhraseCategory.EMERGENCY, "I don't feel well.", "気分が悪いです。"),

        // 日常の挨拶
        Phrase("greeting_01", PhraseCategory.GREETING, "Hi, how are you?", "こんにちは、元気ですか？"),
        Phrase("greeting_02", PhraseCategory.GREETING, "Nice to meet you.", "はじめまして。"),
        Phrase("greeting_03", PhraseCategory.GREETING, "Thank you so much.", "本当にありがとうございます。"),
        Phrase("greeting_04", PhraseCategory.GREETING, "Excuse me, could you say that again?", "すみません、もう一度言ってもらえますか？"),
        Phrase("greeting_05", PhraseCategory.GREETING, "Could you speak more slowly, please?", "もう少しゆっくり話してもらえますか？"),
    )

    fun byCategory(category: PhraseCategory): List<Phrase> = all.filter { it.category == category }
}
