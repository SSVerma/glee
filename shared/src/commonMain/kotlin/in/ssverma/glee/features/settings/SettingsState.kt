package `in`.ssverma.glee.features.settings

import androidx.compose.runtime.Immutable
import `in`.ssverma.glee.features.chat.domain.model.GleeModelConfig
import `in`.ssverma.glee.features.chat.domain.model.ThemeMode

@Immutable
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,
    val isAdaptiveColorsEnabled: Boolean = true,
    val modelConfig: GleeModelConfig = GleeModelConfig(),
    val systemPrompt: String = "",
    val showSystemPromptEditor: Boolean = false,
    val shouldShowIncognitoInfo: Boolean = true
)

sealed interface SettingsIntent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent
    data class SetAdaptiveColors(val enabled: Boolean) : SettingsIntent
    data class UpdateModelConfig(val config: GleeModelConfig) : SettingsIntent
    data object SaveIntelligenceConfig : SettingsIntent
    data class UpdateSystemPrompt(val prompt: String) : SettingsIntent
    data object RestoreDefaultSystemPrompt : SettingsIntent
    data object ToggleSystemPromptEditor : SettingsIntent
    data class SetShowIncognitoInfo(val show: Boolean) : SettingsIntent
}
