package `in`.ssverma.glee.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
actual fun isDynamicColorSupported(): Boolean = false

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme = darkColorScheme()

@Composable
actual fun dynamicLightColorScheme(): ColorScheme = lightColorScheme()
