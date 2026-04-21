package `in`.ssverma.glee

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import `in`.ssverma.glee.domain.model.ThemeMode
import `in`.ssverma.glee.ui.chat.ChatViewModel
import `in`.ssverma.glee.ui.navigation.GleeNavHost
import `in`.ssverma.glee.ui.theme.GleeTheme
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

    GleeTheme(
        darkTheme = isDark,
        dynamicColor = uiState.isAdaptiveColorsEnabled
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            GleeNavHost(viewModel = viewModel)
        }
    }
}
