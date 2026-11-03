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

@Composable
private fun glassColorScheme() = darkColorScheme(
    primary = GlassAccent,
    onPrimary = Color.White,
    primaryContainer = GlassTint,
    onPrimaryContainer = GlassTextPrimary,
    secondary = Color(0xFFC24DFF),
    onSecondary = Color.White,
    tertiary = Color(0xFF4D6BFF),
    background = Color.Transparent,
    onBackground = GlassTextPrimary,
    surface = Color.Transparent,
    onSurface = GlassTextPrimary,
    surfaceVariant = GlassTint,
    onSurfaceVariant = GlassTextSecondary,
    outline = GlassHair,
    surfaceContainer = GlassSurface,
    surfaceContainerHigh = GlassSurface,
    surfaceContainerHighest = GlassSurface,
    surfaceContainerLow = GlassSurface,
    surfaceContainerLowest = GlassSurface
)

@Composable
fun TodoistoTheme(content: @Composable () -> Unit) {
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
