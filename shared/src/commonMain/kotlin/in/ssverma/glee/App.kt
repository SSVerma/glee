package `in`.ssverma.glee

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import `in`.ssverma.glee.core.ui.components.WebPerformanceDialog
import `in`.ssverma.glee.core.ui.theme.GleeTheme
import `in`.ssverma.glee.features.chat.domain.model.ThemeMode
import `in`.ssverma.glee.features.chat.ui.ChatIntent
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import `in`.ssverma.glee.features.settings.SettingsViewModel
import `in`.ssverma.glee.features.settings.SettingsIntent
import `in`.ssverma.glee.navigation.RootNavHost
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val chatViewModel: ChatViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    
    val chatState by chatViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    val isDark = when (settingsState.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    WebPerformanceDialog(
        showDialog = chatState.showDownloadDialog,
        onDismissRequest = { chatViewModel.onIntent(ChatIntent.SetShowDownloadDialog(false)) }
    )

    GleeTheme(
        darkTheme = isDark,
        dynamicColor = settingsState.isAdaptiveColorsEnabled
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RootNavHost(viewModel = chatViewModel)
        }
    }
}
