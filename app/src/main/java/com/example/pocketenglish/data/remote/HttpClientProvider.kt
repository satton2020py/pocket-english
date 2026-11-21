package com.example.pocketenglish.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** アプリ全体で共有する OkHttpClient（コネクションプールを使い回すため単一インスタンス）。 */
object HttpClientProvider {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
