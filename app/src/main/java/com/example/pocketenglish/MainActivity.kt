package com.example.pocketenglish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pocketenglish.ui.MainScreen
import com.example.pocketenglish.ui.theme.PocketEnglishTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as PocketEnglishApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 起動直後からTTSの初期化を始めておき、最初のタップで待たせない
        container.ttsManager
        setContent {
            PocketEnglishTheme {
                MainScreen()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // バックグラウンドに回ったら読み上げを止める（電車内などで鳴り続けないように）
        container.ttsManager.stop()
    }
}
