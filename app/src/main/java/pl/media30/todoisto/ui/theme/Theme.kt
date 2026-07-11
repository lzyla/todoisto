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
    primaryContainer = Color(0xFF8B5CFF).copy(alpha = 0.14f),
    onPrimaryContainer = GlassTextPrimary,
    secondary = GlassBlobMagenta,
    onSecondary = Color.White,
    tertiary = GlassBlobBlue,
    background = GlassBgMid,
    onBackground = GlassTextPrimary,
    surface = Color.Transparent,
    onSurface = GlassTextPrimary,
    surfaceVariant = Color(0xFF8B5CFF).copy(alpha = 0.12f),
    onSurfaceVariant = GlassTextSecondary,
    outline = Color(0xFF8B5CFF).copy(alpha = 0.35f),
    // Menus, dialogs and pickers draw on these — keep them light lavender, not gray.
    surfaceContainer = Color(0xFFF6F0FF),
    surfaceContainerHigh = Color(0xFFF2EAFF),
    surfaceContainerHighest = Color(0xFFEDE3FF),
    surfaceContainerLow = Color(0xFFF9F5FF),
    surfaceContainerLowest = Color(0xFFFFFFFF)
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = GlassColors,
        typography = Typography,
        content = content
    )
}
