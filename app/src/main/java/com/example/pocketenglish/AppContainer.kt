package com.example.pocketenglish

import android.content.Context
import com.example.pocketenglish.data.UserPreferencesRepository
import com.example.pocketenglish.tts.TtsManager

/** 依存オブジェクトの置き場（DIライブラリを使わない軽量な手動DI）。 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val ttsManager: TtsManager by lazy { TtsManager(appContext) }
}
