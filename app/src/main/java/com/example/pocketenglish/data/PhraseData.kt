package com.example.pocketenglish.data

import com.example.pocketenglish.model.Phrase
import com.example.pocketenglish.model.PhraseCategory

/** オフラインで使える内蔵フレーズ（各カテゴリ15件）。 */
object PhraseData {

    val all: List<Phrase> = listOf(
        // 空港・移動
        Phrase("airport_01", PhraseCategory.AIRPORT, "I'm here for sightseeing.", "観光で来ました。", "入国審査で目的を聞かれたら"),
        Phrase("airport_02", PhraseCategory.AIRPORT, "I'll be staying for five days.", "5日間滞在します。", "滞在期間を聞かれたら（数字を入れ替え）"),
        Phrase("airport_03", PhraseCategory.AIRPORT, "Where is the baggage claim?", "手荷物受取所はどこですか？"),
        Phrase("airport_04", PhraseCategory.AIRPORT, "How do I get to downtown?", "市内へはどう行けばいいですか？"),
        Phrase("airport_05", PhraseCategory.AIRPORT, "Could you take me to this address, please?", "この住所までお願いします。", "タクシーで住所を見せながら"),
        Phrase("airport_06", PhraseCategory.AIRPORT, "I'd like a window seat, please.", "窓側の席をお願いします。", "チェックインカウンターで。通路側は aisle seat"),
        Phrase("airport_07", PhraseCategory.AIRPORT, "Where is gate 23?", "23番ゲートはどこですか？", "番号は搭乗券を見て入れ替え"),
        Phrase("airport_08", PhraseCategory.AIRPORT, "Is this flight on time?", "この便は定刻通りですか？", "遅延が気になるとき"),
        Phrase("airport_09", PhraseCategory.AIRPORT, "My suitcase didn't come out.", "スーツケースが出てきませんでした。", "手荷物受取所の係員に。荷物引換証を見せる"),
        Phrase("airport_10", PhraseCategory.AIRPORT, "I have nothing to declare.", "申告するものはありません。", "税関で聞かれたら"),
        Phrase("airport_11", PhraseCategory.AIRPORT, "Where can I buy a ticket?", "切符はどこで買えますか？", "駅やバス乗り場で"),
        Phrase("airport_12", PhraseCategory.AIRPORT, "Does this bus go to the city center?", "このバスは市の中心部に行きますか？", "乗る前に運転手に確認"),
        Phrase("airport_13", PhraseCategory.AIRPORT, "Which platform for the train to the airport?", "空港行きの電車は何番線ですか？", "行き先を入れ替えて使える"),
        Phrase("airport_14", PhraseCategory.AIRPORT, "Please let me know when we get there.", "着いたら教えてください。", "バスやタクシーで降りる場所が不安なとき"),
        Phrase("airport_15", PhraseCategory.AIRPORT, "Can I get a receipt, please?", "領収書をもらえますか？", "タクシーを降りるとき"),

        // ホテル
        Phrase("hotel_01", PhraseCategory.HOTEL, "I have a reservation under Tanaka.", "田中で予約しています。", "名前は自分の名字に"),
        Phrase("hotel_02", PhraseCategory.HOTEL, "Can I check in early?", "早めにチェックインできますか？"),
        Phrase("hotel_03", PhraseCategory.HOTEL, "What's the Wi-Fi password?", "Wi-Fiのパスワードは何ですか？"),
        Phrase("hotel_04", PhraseCategory.HOTEL, "Could you keep my luggage until three?", "3時まで荷物を預かってもらえますか？"),
        Phrase("hotel_05", PhraseCategory.HOTEL, "The air conditioner isn't working.", "エアコンが動きません。"),
        Phrase("hotel_06", PhraseCategory.HOTEL, "What time is breakfast?", "朝食は何時ですか？"),
        Phrase("hotel_07", PhraseCategory.HOTEL, "Could I have another towel, please?", "タオルをもう1枚もらえますか？"),
        Phrase("hotel_08", PhraseCategory.HOTEL, "I'd like to check out, please.", "チェックアウトをお願いします。"),
        Phrase("hotel_09", PhraseCategory.HOTEL, "Could I have a late checkout?", "チェックアウトを遅らせてもらえますか？", "有料のこともあるので料金も確認"),
        Phrase("hotel_10", PhraseCategory.HOTEL, "I locked myself out of my room.", "部屋に鍵を置いたまま閉め出されました。", "オートロックで締め出されたとき"),
        Phrase("hotel_11", PhraseCategory.HOTEL, "There's no hot water.", "お湯が出ません。"),
        Phrase("hotel_12", PhraseCategory.HOTEL, "Could you call a taxi for me?", "タクシーを呼んでもらえますか？", "フロントで"),
        Phrase("hotel_13", PhraseCategory.HOTEL, "Is there a convenience store nearby?", "近くにコンビニはありますか？"),
        Phrase("hotel_14", PhraseCategory.HOTEL, "Could I have a wake-up call at seven?", "7時にモーニングコールをお願いできますか？", "時間は入れ替え"),
        Phrase("hotel_15", PhraseCategory.HOTEL, "Is breakfast included?", "朝食は付いていますか？", "予約内容を確認するとき"),

        // レストラン
        Phrase("restaurant_01", PhraseCategory.RESTAURANT, "A table for two, please.", "2名でお願いします。"),
        Phrase("restaurant_02", PhraseCategory.RESTAURANT, "What do you recommend?", "おすすめは何ですか？"),
        Phrase("restaurant_03", PhraseCategory.RESTAURANT, "I'll have this one, please.", "これをお願いします。", "メニューを指さしながら"),
        Phrase("restaurant_04", PhraseCategory.RESTAURANT, "I'm allergic to nuts.", "ナッツアレルギーがあります。"),
        Phrase("restaurant_05", PhraseCategory.RESTAURANT, "Can I get the check, please?", "お会計をお願いします。"),
        Phrase("restaurant_06", PhraseCategory.RESTAURANT, "Do you have a menu in Japanese?", "日本語のメニューはありますか？"),
        Phrase("restaurant_07", PhraseCategory.RESTAURANT, "Could we sit by the window?", "窓際の席に座れますか？"),
        Phrase("restaurant_08", PhraseCategory.RESTAURANT, "Could I have some water, please?", "お水をもらえますか？", "有料の国もある。無料の水道水は tap water"),
        Phrase("restaurant_09", PhraseCategory.RESTAURANT, "Is this spicy?", "これは辛いですか？"),
        Phrase("restaurant_10", PhraseCategory.RESTAURANT, "Could I have it without onions?", "玉ねぎ抜きにできますか？", "抜きたい食材を入れ替え"),
        Phrase("restaurant_11", PhraseCategory.RESTAURANT, "To go, please.", "持ち帰りでお願いします。", "店内で食べるなら For here, please."),
        Phrase("restaurant_12", PhraseCategory.RESTAURANT, "This isn't what I ordered.", "これは注文したものと違います。"),
        Phrase("restaurant_13", PhraseCategory.RESTAURANT, "Could we have separate checks?", "別々に会計できますか？", "友人と割り勘したいとき"),
        Phrase("restaurant_14", PhraseCategory.RESTAURANT, "Is the tip included?", "チップは含まれていますか？", "レシートに service charge があれば不要なことが多い"),
        Phrase("restaurant_15", PhraseCategory.RESTAURANT, "Everything was delicious.", "とてもおいしかったです。", "帰り際にひとこと"),

        // 買い物
        Phrase("shopping_01", PhraseCategory.SHOPPING, "I'm just looking, thanks.", "見ているだけです、ありがとう。"),
        Phrase("shopping_02", PhraseCategory.SHOPPING, "How much is this?", "これはいくらですか？"),
        Phrase("shopping_03", PhraseCategory.SHOPPING, "Do you have this in a smaller size?", "これの小さいサイズはありますか？"),
        Phrase("shopping_04", PhraseCategory.SHOPPING, "Can I pay by card?", "カードで払えますか？"),
        Phrase("shopping_05", PhraseCategory.SHOPPING, "Could I get a bag, please?", "袋をもらえますか？"),
        Phrase("shopping_06", PhraseCategory.SHOPPING, "Can I try this on?", "試着してもいいですか？"),
        Phrase("shopping_07", PhraseCategory.SHOPPING, "Do you have this in another color?", "これの別の色はありますか？"),
        Phrase("shopping_08", PhraseCategory.SHOPPING, "Where is the fitting room?", "試着室はどこですか？"),
        Phrase("shopping_09", PhraseCategory.SHOPPING, "Is this on sale?", "これはセール品ですか？"),
        Phrase("shopping_10", PhraseCategory.SHOPPING, "Can I get a tax refund?", "免税手続きはできますか？", "パスポートが必要なことが多い"),
        Phrase("shopping_11", PhraseCategory.SHOPPING, "Could you gift-wrap it?", "プレゼント用に包んでもらえますか？"),
        Phrase("shopping_12", PhraseCategory.SHOPPING, "I'll take it.", "これにします。", "買うと決めたとき"),
        Phrase("shopping_13", PhraseCategory.SHOPPING, "Can I exchange this?", "これを交換できますか？", "レシートを見せながら。返品は return"),
        Phrase("shopping_14", PhraseCategory.SHOPPING, "Where can I find the restroom?", "お手洗いはどこですか？"),
        Phrase("shopping_15", PhraseCategory.SHOPPING, "Do you have a cheaper one?", "もっと安いものはありますか？"),

        // 緊急・トラブル
        Phrase("emergency_01", PhraseCategory.EMERGENCY, "Please help me!", "助けてください！"),
        Phrase("emergency_02", PhraseCategory.EMERGENCY, "I lost my passport.", "パスポートをなくしました。"),
        Phrase("emergency_03", PhraseCategory.EMERGENCY, "Please call an ambulance.", "救急車を呼んでください。"),
        Phrase("emergency_04", PhraseCategory.EMERGENCY, "Where is the nearest police station?", "一番近い警察署はどこですか？"),
        Phrase("emergency_05", PhraseCategory.EMERGENCY, "I don't feel well.", "気分が悪いです。"),
        Phrase("emergency_06", PhraseCategory.EMERGENCY, "My wallet was stolen.", "財布を盗まれました。"),
        Phrase("emergency_07", PhraseCategory.EMERGENCY, "I need a doctor.", "医者に診てもらいたいです。"),
        Phrase("emergency_08", PhraseCategory.EMERGENCY, "I have a pain here.", "ここが痛いです。", "痛む場所を指さしながら"),
        Phrase("emergency_09", PhraseCategory.EMERGENCY, "Is there a pharmacy nearby?", "近くに薬局はありますか？"),
        Phrase("emergency_10", PhraseCategory.EMERGENCY, "I'm lost.", "道に迷いました。"),
        Phrase("emergency_11", PhraseCategory.EMERGENCY, "Can I use your phone?", "電話を借りてもいいですか？"),
        Phrase("emergency_12", PhraseCategory.EMERGENCY, "I missed my flight.", "飛行機に乗り遅れました。", "航空会社のカウンターで"),
        Phrase("emergency_13", PhraseCategory.EMERGENCY, "Does anyone here speak Japanese?", "日本語を話せる人はいますか？"),
        Phrase("emergency_14", PhraseCategory.EMERGENCY, "Please contact the Japanese embassy.", "日本大使館に連絡してください。"),
        Phrase("emergency_15", PhraseCategory.EMERGENCY, "Leave me alone!", "放っておいて！", "しつこい勧誘や客引きをはっきり断るとき"),

        // 日常の挨拶
        Phrase("greeting_01", PhraseCategory.GREETING, "Hi, how are you?", "こんにちは、元気ですか？"),
        Phrase("greeting_02", PhraseCategory.GREETING, "Nice to meet you.", "はじめまして。"),
        Phrase("greeting_03", PhraseCategory.GREETING, "Thank you so much.", "本当にありがとうございます。"),
        Phrase("greeting_04", PhraseCategory.GREETING, "Excuse me, could you say that again?", "すみません、もう一度言ってもらえますか？"),
        Phrase("greeting_05", PhraseCategory.GREETING, "Could you speak more slowly, please?", "もう少しゆっくり話してもらえますか？"),
        Phrase("greeting_06", PhraseCategory.GREETING, "Good morning.", "おはようございます。"),
        Phrase("greeting_07", PhraseCategory.GREETING, "See you later.", "またね。", "軽い別れのあいさつ"),
        Phrase("greeting_08", PhraseCategory.GREETING, "Have a nice day.", "よい一日を。", "お店を出るときにも使える"),
        Phrase("greeting_09", PhraseCategory.GREETING, "I'm from Japan.", "日本から来ました。", "出身を聞かれたら"),
        Phrase("greeting_10", PhraseCategory.GREETING, "I'm sorry, I don't understand.", "すみません、わかりません。"),
        Phrase("greeting_11", PhraseCategory.GREETING, "What does this mean?", "これはどういう意味ですか？", "看板やメニューを指さしながら"),
        Phrase("greeting_12", PhraseCategory.GREETING, "Could you take a picture of us?", "写真を撮ってもらえますか？"),
        Phrase("greeting_13", PhraseCategory.GREETING, "Can I take a picture here?", "ここで写真を撮ってもいいですか？", "美術館や店内で"),
        Phrase("greeting_14", PhraseCategory.GREETING, "No problem.", "大丈夫ですよ。", "お礼や謝罪への返事として"),
        Phrase("greeting_15", PhraseCategory.GREETING, "It was nice talking to you.", "お話しできてよかったです。", "会話の締めに"),
    )

    fun byCategory(category: PhraseCategory): List<Phrase> = all.filter { it.category == category }
}
