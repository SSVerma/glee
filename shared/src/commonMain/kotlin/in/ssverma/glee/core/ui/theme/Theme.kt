package `in`.ssverma.glee.core.ui.theme

import `in`.ssverma.glee.features.chat.domain.model.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GleeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // For Android wallpaper-based colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && isDynamicColorSupported() -> {
            if (darkTheme) dynamicDarkColorScheme() else dynamicLightColorScheme()
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SetSystemAppearance(!darkTheme)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = GleeTypography,
        content = content
    )
}

@Composable
expect fun SetSystemAppearance(isLight: Boolean)

@Composable
expect fun isDynamicColorSupported(): Boolean

@Composable
expect fun dynamicDarkColorScheme(): ColorScheme

@Composable
expect fun dynamicLightColorScheme(): ColorScheme
