package com.example.pocketenglish.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Translate
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
import com.example.pocketenglish.ui.chat.ChatScreen
import com.example.pocketenglish.ui.interpreter.InterpreterScreen
import com.example.pocketenglish.ui.listening.ListeningScreen
import com.example.pocketenglish.ui.phrase.PhraseScreen
import kotlinx.coroutines.launch

/** 選択中のタブは塗りつぶし、それ以外は線だけのアイコンにする（Android・iPhone共通の見せ方）。 */
enum class AppTab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    PHRASE("フレーズ", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    AI_CHAT("AI会話", Icons.Filled.Forum, Icons.Outlined.Forum),
    INTERPRETER("瞬間通訳", Icons.Filled.Translate, Icons.Outlined.Translate),
    LISTENING("聞き取り", Icons.Filled.Hearing, Icons.Outlined.Hearing),
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
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
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
            AppTab.AI_CHAT -> ChatScreen(showMessage = showMessage, modifier = contentModifier)
            AppTab.INTERPRETER -> InterpreterScreen(showMessage = showMessage, modifier = contentModifier)
            AppTab.LISTENING -> ListeningScreen(showMessage = showMessage, modifier = contentModifier)
        }
    }
}
