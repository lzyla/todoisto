package pl.media30.todoisto.ui.theme

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

// ---- Liquid-glass theme (two palettes: light = white backdrop with purple
// glass, dark = vivid purple backdrop with white glass) ----------------------

object GlassTheme {
    /** Toggled from settings; all palette getters below react to it. */
    var dark by mutableStateOf(false)
}

private val d get() = GlassTheme.dark

val GlassBgTop: Color get() = if (d) Color(0xFF8B5CFF) else Color(0xFFFFFFFF)
val GlassBgMid: Color get() = if (d) Color(0xFF7141E8) else Color(0xFFF6F1FF)
val GlassBgBottom: Color get() = if (d) Color(0xFF5527C4) else Color(0xFFEFE7FF)

val GlassBlobViolet: Color get() = if (d) Color(0xFFB388FF) else Color(0xFFA875FF)
val GlassBlobMagenta = Color(0xFFE96BFF)
val GlassBlobBlue = Color(0xFF7C9EFF)

val GlassAccent: Color get() = if (d) Color(0xFF9B6BFF) else Color(0xFF6B3FE0)
val GlassTextPrimary: Color get() = if (d) Color(0xFFFFFFFF) else Color(0xFF2A1655)
val GlassTextSecondary: Color get() = if (d) Color(0xFFEDE5FF) else Color(0xFF6E5A9E)

/** Tint used for the glass panel fills and hairlines. */
val GlassPanelTint: Color get() = if (d) Color.White else Color(0xFF8B5CFF)

/** Solid surface for drawers, sheets, menus and dialogs. */
val GlassSurface: Color get() = if (d) Color(0xFF6B3FE0) else Color(0xFFF6F0FF)

/**
 * Full-screen vibrant purple backdrop with soft glowing blobs.
 * Everything glassy is layered on top of this so the translucency reads.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GlassBgTop, GlassBgMid, GlassBgBottom)
                )
            )
    ) {
        Blob(
            color = GlassBlobMagenta,
            size = 340.dp,
            alignment = Alignment.TopStart,
            offsetX = (-90).dp,
            offsetY = (-70).dp
        )
        Blob(
            color = GlassBlobViolet,
            size = 380.dp,
            alignment = Alignment.TopEnd,
            offsetX = 120.dp,
            offsetY = (-110).dp
        )
        Blob(
            color = GlassBlobBlue,
            size = 320.dp,
            alignment = Alignment.BottomStart,
            offsetX = (-60).dp,
            offsetY = 120.dp
        )
        Blob(
            color = GlassBlobViolet,
            size = 300.dp,
            alignment = Alignment.BottomEnd,
            offsetX = 90.dp,
            offsetY = 100.dp
        )
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
            .blur(80.dp)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = if (GlassTheme.dark) 0.75f else 0.45f), Color.Transparent)
                ),
                CircleShape
            )
    )
}

/**
 * Frosted-glass surface treatment: soft drop shadow, translucent gradient fill
 * and a bright hairline border that catches the light.
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp)): Modifier {
    val dark = GlassTheme.dark
    val fillTop = if (dark) 0.30f else 0.16f
    val fillBottom = if (dark) 0.12f else 0.07f
    val borderStart = Color.White.copy(alpha = if (dark) 0.75f else 0.95f)
    val borderEnd = if (dark) Color.White.copy(alpha = 0.20f) else GlassPanelTint.copy(alpha = 0.35f)
    val shadowColor = if (dark) Color(0x59200A66) else Color(0x40551FC2)
    return this
        .shadow(elevation = if (dark) 12.dp else 10.dp, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    GlassPanelTint.copy(alpha = fillTop),
                    GlassPanelTint.copy(alpha = fillBottom)
                )
            )
        )
        .border(
            width = 1.5.dp,
            brush = Brush.linearGradient(listOf(borderStart, borderEnd)),
            shape = shape
        )
}
