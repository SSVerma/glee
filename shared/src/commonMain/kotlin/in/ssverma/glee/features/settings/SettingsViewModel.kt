package `in`.ssverma.glee.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import glee.shared.generated.resources.Res
import glee.shared.generated.resources.default_system_prompt
import `in`.ssverma.glee.core.preferences.GleeSettings
import `in`.ssverma.glee.features.chat.domain.usecase.AiChatManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class SettingsViewModel(
    private val settings: GleeSettings,
    private val chatManager: AiChatManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            settings.isAdaptiveColorsEnabled.collect { enabled ->
                _uiState.update { it.copy(isAdaptiveColorsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            settings.shouldShowIncognitoInfo.collect { shouldShow ->
                _uiState.update { it.copy(shouldShowIncognitoInfo = shouldShow) }
            }
        }

        viewModelScope.launch {
            combine(
                settings.systemPrompt,
                settings.temperature,
                settings.topK,
                settings.useGpu,
                settings.isAgentic
            ) { systemPrompt, temp, topK, useGpu, isAgentic ->
                val defaultPrompt = getString(Res.string.default_system_prompt)
                _uiState.update {
                    it.copy(
                        systemPrompt = systemPrompt ?: defaultPrompt,
                        modelConfig = it.modelConfig.copy(
                            temperature = temp,
                            topK = topK,
                            useGpu = useGpu,
                            isAgentic = isAgentic
                        )
                    )
                }
            }.collect {}
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetThemeMode -> settings.setThemeMode(intent.mode)
            is SettingsIntent.SetAdaptiveColors -> settings.setAdaptiveColorsEnabled(intent.enabled)
            is SettingsIntent.UpdateModelConfig -> _uiState.update { it.copy(modelConfig = intent.config) }
            SettingsIntent.SaveIntelligenceConfig -> {
                val state = _uiState.value
                settings.setSystemPrompt(state.systemPrompt)
                settings.setTemperature(state.modelConfig.temperature)
                settings.setTopK(state.modelConfig.topK)
                settings.setUseGpu(state.modelConfig.useGpu)
                settings.setIsAgentic(state.modelConfig.isAgentic)
                chatManager.updateSystemPrompt(state.systemPrompt)
            }

            is SettingsIntent.UpdateSystemPrompt -> _uiState.update { it.copy(systemPrompt = intent.prompt) }
            SettingsIntent.RestoreDefaultSystemPrompt -> {
                viewModelScope.launch {
                    val default = getString(Res.string.default_system_prompt)
                    _uiState.update { it.copy(systemPrompt = default) }
                }
            }

            SettingsIntent.ToggleSystemPromptEditor -> _uiState.update {
                it.copy(
                    showSystemPromptEditor = !it.showSystemPromptEditor
                )
            }

            is SettingsIntent.SetShowIncognitoInfo -> settings.setShouldShowIncognitoInfo(intent.show)
        }
    }
}
