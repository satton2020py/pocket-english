package com.example.pocketenglish.data.remote

import com.example.pocketenglish.BuildConfig

/**
 * AI通信の設定（Gemini API）。APIキーは local.properties → BuildConfig 経由でのみ取得し、
 * ソースコードやGitには一切書かない。
 */
object ApiConfig {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    val apiKey: String get() = BuildConfig.GEMINI_API_KEY
    val model: String get() = BuildConfig.GEMINI_MODEL

    /** generateContent のエンドポイント。キーはURLではなくヘッダーで送る。 */
    val generateContentUrl: String get() = "$BASE_URL/$model:generateContent"

    /** キー未設定のときはAI機能を無効化し、UIで案内を出すために使う。 */
    val hasApiKey: Boolean get() = apiKey.isNotBlank()
}
