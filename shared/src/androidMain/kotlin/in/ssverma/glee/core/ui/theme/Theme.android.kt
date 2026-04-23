package `in`.ssverma.glee.core.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetSystemAppearance(isLight: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
        }
    }
}

@Composable
actual fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        DarkColorScheme
    }
}

@Composable
actual fun dynamicLightColorScheme(): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        LightColorScheme
    }
}
