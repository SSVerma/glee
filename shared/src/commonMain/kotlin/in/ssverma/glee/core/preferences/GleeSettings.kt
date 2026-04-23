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
