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

// ---- Liquid-glass palette -------------------------------------------------

val GlassBgTop = Color(0xFF1B0F38)
val GlassBgMid = Color(0xFF2C1863)
val GlassBgBottom = Color(0xFF140A2C)

val GlassBlobViolet = Color(0xFF8A5CFF)
val GlassBlobMagenta = Color(0xFFC24DFF)
val GlassBlobBlue = Color(0xFF4D6BFF)

val GlassAccent = Color(0xFF9B6BFF)
val GlassTextPrimary = Color(0xFFF4F1FF)
val GlassTextSecondary = Color(0xFFBEB4E8)

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
            .blur(90.dp)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent)),
                CircleShape
            )
    )
}

/**
 * Frosted-glass surface treatment: soft drop shadow, translucent gradient fill
 * and a bright hairline border that catches the light.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(22.dp),
    fillAlphaTop: Float = 0.16f,
    fillAlphaBottom: Float = 0.05f
): Modifier = this
    .shadow(elevation = 10.dp, shape = shape, spotColor = Color.Black, ambientColor = Color.Black)
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = fillAlphaTop),
                Color.White.copy(alpha = fillAlphaBottom)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.08f)
            )
        ),
        shape = shape
    )
