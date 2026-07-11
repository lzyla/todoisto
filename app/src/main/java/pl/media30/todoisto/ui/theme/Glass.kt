package pl.media30.todoisto.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---- Liquid-glass theme ------------------------------------------------------
// Modeled after Apple's Liquid Glass material: elements are uniformly
// translucent (content shows through — no gradient fills on controls), with a
// specular highlight around the rim as if lit from the top-left, soft diffuse
// shadows for depth, and slow fluid motion in the backdrop.

object GlassTheme {
    /** Toggled from settings; all palette getters below react to it. */
    var dark by mutableStateOf(false)
}

private val d get() = GlassTheme.dark

val GlassBgTop: Color get() = if (d) Color(0xFF6E45D9) else Color(0xFFFDFBFF)
val GlassBgMid: Color get() = if (d) Color(0xFF5B35C4) else Color(0xFFF6F1FE)
val GlassBgBottom: Color get() = if (d) Color(0xFF44239E) else Color(0xFFEFE8FC)

val GlassBlobViolet: Color get() = if (d) Color(0xFFB388FF) else Color(0xFFA875FF)
val GlassBlobMagenta = Color(0xFFE96BFF)
val GlassBlobBlue = Color(0xFF7C9EFF)

val GlassAccent: Color get() = if (d) Color(0xFF9B6BFF) else Color(0xFF6B3FE0)
val GlassTextPrimary: Color get() = if (d) Color(0xFFFFFFFF) else Color(0xFF2A1655)
val GlassTextSecondary: Color get() = if (d) Color(0xFFE6DDFB) else Color(0xFF6E5A9E)

/** Tint used for the glass panel fills and hairlines. */
val GlassPanelTint: Color get() = if (d) Color.White else Color(0xFF8B5CFF)

/** Solid surface for drawers, sheets, menus and dialogs. */
val GlassSurface: Color get() = if (d) Color(0xFF5B35C4) else Color(0xFFF7F2FF)

/**
 * Full-screen backdrop: a gentle vertical gradient with soft colour glows that
 * drift very slowly — the "liquid" in liquid glass. The gradient lives here,
 * on the background only; the elements above stay flat and translucent.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val drift = rememberInfiniteTransition(label = "liquidDrift")
    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftT"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GlassBgTop, GlassBgMid, GlassBgBottom)))
    ) {
        Blob(GlassBlobMagenta, 360.dp, Alignment.TopStart, (-100 + 40 * t).dp, (-80 + 24 * t).dp)
        Blob(GlassBlobViolet, 400.dp, Alignment.TopEnd, (130 - 36 * t).dp, (-120 + 30 * t).dp)
        Blob(GlassBlobBlue, 340.dp, Alignment.BottomStart, (-70 + 28 * t).dp, (130 - 30 * t).dp)
        Blob(GlassBlobViolet, 320.dp, Alignment.BottomEnd, (100 - 30 * t).dp, (110 - 22 * t).dp)
        content()
    }
}

@Composable
private fun BoxScope.Blob(
    color: Color,
    size: Dp,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .blur(100.dp)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = if (GlassTheme.dark) 0.45f else 0.22f), Color.Transparent)
                ),
                CircleShape
            )
    )
}

/**
 * Liquid-glass surface: soft diffuse shadow underneath, a single flat
 * translucent fill (the background shows through), and a specular rim —
 * brightest at the top-left as if catching the light — instead of any
 * gradient fill on the element itself.
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    val dark = GlassTheme.dark
    val fill = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.42f)
    val rimBright = Color.White.copy(alpha = if (dark) 0.65f else 1.0f)
    val rimFaint = if (dark) Color.White.copy(alpha = 0.10f) else GlassPanelTint.copy(alpha = 0.18f)
    val shadowColor = if (dark) Color(0x66200A66) else Color(0x33551FC2)
    return this
        .shadow(elevation = 14.dp, shape = shape, spotColor = shadowColor, ambientColor = shadowColor.copy(alpha = 0.35f))
        .clip(shape)
        .background(fill)
        .border(
            width = 1.2.dp,
            brush = Brush.linearGradient(listOf(rimBright, rimFaint, rimBright.copy(alpha = 0.25f))),
            shape = shape
        )
}
