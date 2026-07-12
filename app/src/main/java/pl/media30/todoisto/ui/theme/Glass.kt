package pl.media30.todoisto.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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

/**
 * Zen liquid-glass — dopracowana receptura (tuning z prototypu Todoisto):
 *  - NISKIE wypełnienie (biel ~24% jasny / ~8% ciemny) → tło realnie prześwituje,
 *  - jasny frosted look (rozjaśnienie tła), nie mleczna biel,
 *  - specular highlight u góry + delikatny fioletowy cień u dołu,
 *  - miękki rozproszony cień pod elementem.
 *
 * Prawdziwe rozmycie tła (backdrop blur) na Androidzie 12+ najlepiej dodać
 * biblioteką Haze (dev.chrisbanes.haze): tło owiń w Modifier.haze(state),
 * a panele w Modifier.hazeChild(state). Modifier.glass() poniżej daje spójny
 * wygląd również bez Haze (flat translucent + specular rim), zgodnie z repo.
 */
object GlassTheme { var dark by mutableStateOf(false) }
private val d get() = GlassTheme.dark

// Tło — pionowy gradient trzech odcieni fioletu
val GlassBgTop:    Color get() = if (d) Color(0xFF6E45D9) else Color(0xFFFDFBFF)
val GlassBgMid:    Color get() = if (d) Color(0xFF5B35C4) else Color(0xFFF6F1FE)
val GlassBgBottom: Color get() = if (d) Color(0xFF44239E) else Color(0xFFEFE8FC)

val GlassAccent:        Color get() = if (d) AccentDark else AccentLight
val GlassTextPrimary:   Color get() = if (d) Color(0xFFFFFFFF) else Color(0xFF2A1655)
val GlassTextSecondary: Color get() = if (d) Color(0xFFE6DDFB) else Color(0xFF6E5A9E)
val GlassPanelTint:     Color get() = if (d) Color.White else Color(0xFF8B5CFF)
val GlassSurface:       Color get() = if (d) Color(0xFF5B35C4) else Color(0xFFF7F2FF)

// „Przydymiona" tafla dla docka i strefy Rutyn — celowo inny tryb niż glass zadań
val GlassRoutine: Color get() = if (d) Color(0x664A4066) else Color(0x61E2DBF0)

/**
 * Pełnoekranowe, wolno dryfujące tło 3D — „liquid" w liquid glass:
 * duże, gładkie szklane bryły w palecie marki, blur 100dp, animacja 24s reverse.
 */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "drift")
    val p by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "p",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GlassBgTop, GlassBgMid, GlassBgBottom)))
    ) {
        Lobe(Color(0xFFE96BFF), 360.dp, Alignment.TopStart,    (-90 + 40 * p).dp, (-70 + 24 * p).dp)
        Lobe(Color(0xFFA875FF), 400.dp, Alignment.TopEnd,      (110 - 36 * p).dp, (60 + 30 * p).dp)
        Lobe(Color(0xFF7C9EFF), 340.dp, Alignment.BottomStart, (-70 + 28 * p).dp, (40 - 30 * p).dp)
        Lobe(Color(0xFFA875FF), 330.dp, Alignment.BottomEnd,   (90 - 30 * p).dp,  (-110 + 22 * p).dp)
        content()
    }
}

@Composable
private fun BoxScope.Lobe(color: Color, size: Dp, align: Alignment, ox: Dp, oy: Dp) {
    Box(
        Modifier
            .align(align).offset(ox, oy).size(size).blur(100.dp)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = if (GlassTheme.dark) 0.45f else 0.30f), Color.Transparent)
                ),
                RoundedCornerShape(50),
            )
    )
}

/** Panel/karta w stylu liquid glass. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier {
    val dark = GlassTheme.dark
    val fill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.24f)
    val rimBright = Color.White.copy(alpha = if (dark) 0.55f else 0.85f)
    val rimFaint  = if (dark) Color.White.copy(alpha = 0.08f) else GlassPanelTint.copy(alpha = 0.16f)
    val shadow = if (dark) Color(0x66200A66) else Color(0x1A551FC2)
    return this
        .shadow(12.dp, shape, spotColor = shadow, ambientColor = shadow.copy(alpha = 0.4f))
        .clip(shape)
        .background(fill)
        .border(1.dp, Brush.linearGradient(listOf(rimBright, rimFaint, rimBright.copy(alpha = 0.25f))), shape)
}

/** Klarowna tafla „Control Center" — dock, przycisk Tydzień, kółko Rutyn. */
fun Modifier.glassClear(shape: Shape = RoundedCornerShape(50)): Modifier {
    val dark = GlassTheme.dark
    val shadow = if (dark) Color(0x55200A66) else Color(0x22551FC2)
    return this
        .shadow(10.dp, shape, spotColor = shadow, ambientColor = shadow)
        .clip(shape)
        .background(Color.White.copy(alpha = if (dark) 0.10f else 0.13f))
}
