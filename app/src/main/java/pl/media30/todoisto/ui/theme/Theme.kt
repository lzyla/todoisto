package pl.media30.todoisto.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Liquid-glass scheme. Reads [GlassTheme.dark], so recomposes when the theme
 * toggles. Surfaces are transparent — real fills come from [Modifier.glass]
 * layered over [GlassBackground]; menus/dialogs use the surfaceContainer set.
 */
@Composable
private fun glassColorScheme() = darkColorScheme(
    primary = GlassAccent,
    onPrimary = Color.White,
    primaryContainer = GlassPanelTint.copy(alpha = 0.14f),
    onPrimaryContainer = GlassTextPrimary,
    secondary = Color(0xFFE96BFF),
    onSecondary = Color.White,
    tertiary = Color(0xFF7C9EFF),
    background = GlassBgMid,
    onBackground = GlassTextPrimary,
    surface = Color.Transparent,
    onSurface = GlassTextPrimary,
    surfaceVariant = GlassPanelTint.copy(alpha = 0.12f),
    onSurfaceVariant = GlassTextSecondary,
    outline = GlassPanelTint.copy(alpha = 0.35f),
    surfaceContainer = GlassSurface,
    surfaceContainerHigh = GlassSurface,
    surfaceContainerHighest = GlassSurface,
    surfaceContainerLow = GlassSurface,
    surfaceContainerLowest = GlassSurface
)

@Composable
fun TodoistoTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val activity = view.context as? Activity
    val dark = GlassTheme.dark
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val window = activity.window
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = glassColorScheme(),
        typography = Typography,
        content = content
    )
}
