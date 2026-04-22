package `in`.ssverma.glee.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1C1C), // soft matte black
    onPrimary = Color(0xFFFFFFFF),

    secondary = Color(0xFF8A817C), // muted warm gray (hint of brown)
    onSecondary = Color(0xFFFFFFFF),

    tertiary = Color(0xFFC2410C), // deep burnt orange (very controlled)
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFF8F7F5), // warm snow (not blue-white)
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),

    primaryContainer = Color(0xFFEAEAEA),
    onPrimaryContainer = Color(0xFF1C1C1C),

    outline = Color(0xFFE2E2E0),
    outlineVariant = Color(0xFFEDECE9)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8E6E3), // soft white (not pure)
    onPrimary = Color(0xFF111111),

    secondary = Color(0xFFA8A29E), // desaturated warm gray
    onSecondary = Color(0xFF111111),

    tertiary = Color(0xFFEA580C), // slightly lifted burnt orange
    onTertiary = Color(0xFF2A1400),

    background = Color(0xFF0F0F0F), // matte black
    surface = Color(0xFF171717),
    onBackground = Color(0xFFE8E6E3),
    onSurface = Color(0xFFE8E6E3),

    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color(0xFFE8E6E3),

    outline = Color(0xFF2F2F2F),
    outlineVariant = Color(0xFF1F1F1F)
)
