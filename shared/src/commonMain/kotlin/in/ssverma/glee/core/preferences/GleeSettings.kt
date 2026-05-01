package `in`.ssverma.glee.core.preferences

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.russhwolf.settings.set
import `in`.ssverma.glee.features.chat.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GleeSettings(private val settings: ObservableSettings) {

    @OptIn(ExperimentalSettingsApi::class)
    private val flowSettings = settings.toFlowSettings()

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ADAPTIVE_COLORS = "adaptive_colors"
        private const val KEY_SHOW_INCOGNITO_INFO = "show_incognito_info"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_MODEL_TEMP = "model_temp"
        private const val KEY_MODEL_TOP_K = "model_top_k"
        private const val KEY_MODEL_USE_GPU = "model_use_gpu"
        private const val KEY_MODEL_IS_AGENTIC = "model_is_agentic"
        private const val KEY_SELECTED_MODEL_ID = "selected_model_id"
    }

    @OptIn(ExperimentalSettingsApi::class)
    val selectedModelId: Flow<String?> = flowSettings.getStringOrNullFlow(KEY_SELECTED_MODEL_ID)

    fun setSelectedModelId(id: String?) {
        settings[KEY_SELECTED_MODEL_ID] = id
    }

    @OptIn(ExperimentalSettingsApi::class)
    val systemPrompt: Flow<String?> = flowSettings.getStringOrNullFlow(KEY_SYSTEM_PROMPT)

    fun setSystemPrompt(prompt: String) {
        settings[KEY_SYSTEM_PROMPT] = prompt
    }

    @OptIn(ExperimentalSettingsApi::class)
    val temperature: Flow<Float> = flowSettings.getFloatFlow(KEY_MODEL_TEMP, 0.7f)

    fun setTemperature(temp: Float) {
        settings[KEY_MODEL_TEMP] = temp
    }

    @OptIn(ExperimentalSettingsApi::class)
    val topK: Flow<Int> = flowSettings.getIntFlow(KEY_MODEL_TOP_K, 40)

    fun setTopK(topK: Int) {
        settings[KEY_MODEL_TOP_K] = topK
    }

    @OptIn(ExperimentalSettingsApi::class)
    val useGpu: Flow<Boolean> = flowSettings.getBooleanFlow(KEY_MODEL_USE_GPU, false)

    fun setUseGpu(useGpu: Boolean) {
        settings[KEY_MODEL_USE_GPU] = useGpu
    }

    @OptIn(ExperimentalSettingsApi::class)
    val isAgentic: Flow<Boolean> = flowSettings.getBooleanFlow(KEY_MODEL_IS_AGENTIC, false)

    fun setIsAgentic(isAgentic: Boolean) {
        settings[KEY_MODEL_IS_AGENTIC] = isAgentic
    }

    @OptIn(ExperimentalSettingsApi::class)
    val shouldShowIncognitoInfo: Flow<Boolean> = flowSettings.getBooleanFlow(KEY_SHOW_INCOGNITO_INFO, true)

    fun setShouldShowIncognitoInfo(show: Boolean) {
        settings[KEY_SHOW_INCOGNITO_INFO] = show
    }

    @OptIn(ExperimentalSettingsApi::class)
    val themeMode: Flow<ThemeMode> = flowSettings.getStringFlow(KEY_THEME_MODE, ThemeMode.System.name)
        .map { name -> 
            runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.System)
        }

    fun setThemeMode(mode: ThemeMode) {
        settings[KEY_THEME_MODE] = mode.name
    }

    @OptIn(ExperimentalSettingsApi::class)
    val isAdaptiveColorsEnabled: Flow<Boolean> = flowSettings.getBooleanFlow(KEY_ADAPTIVE_COLORS, true)

    fun setAdaptiveColorsEnabled(enabled: Boolean) {
        settings[KEY_ADAPTIVE_COLORS] = enabled
    }
}
