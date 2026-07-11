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
 * A single vibrant "liquid glass" scheme. Surfaces are intentionally
 * translucent — real fills come from [Modifier.glass] layered over
 * [GlassBackground], so most container colors are transparent here.
 */
private val GlassColors = darkColorScheme(
    primary = GlassAccent,
    onPrimary = Color.White,
    primaryContainer = Color.White.copy(alpha = 0.12f),
    onPrimaryContainer = GlassTextPrimary,
    secondary = GlassBlobMagenta,
    onSecondary = Color.White,
    tertiary = GlassBlobBlue,
    background = GlassBgMid,
    onBackground = GlassTextPrimary,
    surface = Color.Transparent,
    onSurface = GlassTextPrimary,
    surfaceVariant = Color.White.copy(alpha = 0.10f),
    onSurfaceVariant = GlassTextSecondary,
    outline = Color.White.copy(alpha = 0.25f)
)

@Composable
fun TodoistoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val activity = view.context as? Activity
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val window = activity.window
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = GlassColors,
        typography = Typography,
        content = content
    )
}
