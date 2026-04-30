package `in`.ssverma.glee.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun isDynamicColorSupported(): Boolean = false

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme = DarkColorScheme

@Composable
actual fun dynamicLightColorScheme(): ColorScheme = LightColorScheme

@Composable
actual fun SetSystemAppearance(isLight: Boolean) {
    // Relying on Compose Multiplatform's built-in handling
}
