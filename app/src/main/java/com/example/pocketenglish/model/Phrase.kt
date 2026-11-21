package com.example.pocketenglish.model

/** フレーズのカテゴリ。表示順はこの定義順。 */
enum class PhraseCategory(val labelJa: String) {
    AIRPORT("空港・移動"),
    HOTEL("ホテル"),
    RESTAURANT("レストラン"),
    SHOPPING("買い物"),
    EMERGENCY("緊急・トラブル"),
    GREETING("日常の挨拶"),
}

/**
 * 内蔵フレーズ1件。
 * @property id お気に入り保存のキー。データ更新時も変えないこと。
 * @property note 使う場面の一言メモ（任意）。
 */
data class Phrase(
    val id: String,
    val category: PhraseCategory,
    val english: String,
    val japanese: String,
    val note: String? = null,
)
