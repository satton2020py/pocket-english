package com.example.pocketenglish.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pocketenglish.ui.common.ComingSoonScreen
import com.example.pocketenglish.ui.interpreter.InterpreterScreen
import com.example.pocketenglish.ui.phrase.PhraseScreen
import kotlinx.coroutines.launch

enum class AppTab(val label: String, val icon: ImageVector) {
    PHRASE("フレーズ", Icons.Filled.Bookmarks),
    AI_CHAT("AI会話", Icons.Filled.SmartToy),
    INTERPRETER("瞬間通訳", Icons.Filled.Translate),
}

@Composable
fun MainScreen() {
    var currentTab by rememberSaveable { mutableStateOf(AppTab.PHRASE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            // 連続操作でメッセージが溜まらないよう、表示中のものは即座に差し替える
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (currentTab) {
            AppTab.PHRASE -> PhraseScreen(showMessage = showMessage, modifier = contentModifier)
            AppTab.AI_CHAT -> ComingSoonScreen(
                title = "AI会話",
                description = "カフェ注文・入国審査・道案内・フリートークの\n音声ロールプレイは次のステップで追加します",
                icon = AppTab.AI_CHAT.icon,
                modifier = contentModifier,
            )
            AppTab.INTERPRETER -> InterpreterScreen(showMessage = showMessage, modifier = contentModifier)
        }
    }
}
