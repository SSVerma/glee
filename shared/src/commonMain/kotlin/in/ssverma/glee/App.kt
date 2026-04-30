package `in`.ssverma.glee

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import `in`.ssverma.glee.features.chat.domain.model.ThemeMode
import `in`.ssverma.glee.features.chat.ui.ChatIntent
import `in`.ssverma.glee.features.chat.ui.ChatViewModel
import `in`.ssverma.glee.navigation.RootNavHost
import `in`.ssverma.glee.core.ui.theme.GleeTheme
import `in`.ssverma.glee.core.ui.components.WebPerformanceDialog
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel: ChatViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    val isDark = when (uiState.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    WebPerformanceDialog(
        showDialog = uiState.showDownloadDialog,
        onDismissRequest = { viewModel.onIntent(ChatIntent.SetShowDownloadDialog(false)) }
    )

    GleeTheme(
        darkTheme = isDark,
        dynamicColor = uiState.isAdaptiveColorsEnabled
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RootNavHost(viewModel = viewModel)
        }
    }
}
