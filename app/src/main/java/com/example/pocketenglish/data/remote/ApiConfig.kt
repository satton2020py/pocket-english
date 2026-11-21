package com.example.pocketenglish.data.remote

import com.example.pocketenglish.BuildConfig

/**
 * AI通信の設定。APIキーは local.properties → BuildConfig 経由でのみ取得し、
 * ソースコードやGitには一切書かない。
 */
object ApiConfig {
    const val BASE_URL = "https://api.anthropic.com/v1/messages"
    const val ANTHROPIC_VERSION = "2023-06-01"

    val apiKey: String get() = BuildConfig.ANTHROPIC_API_KEY
    val model: String get() = BuildConfig.ANTHROPIC_MODEL

    /** キー未設定のときはAI機能を無効化し、UIで案内を出すために使う。 */
    val hasApiKey: Boolean get() = apiKey.isNotBlank()
}
