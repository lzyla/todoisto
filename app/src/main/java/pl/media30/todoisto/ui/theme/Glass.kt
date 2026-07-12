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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.unit.dp

/**
 * Tokeny wprost z prototypu Todoisto.dc.html (funkcja colors() + domyślne
 * props: szkloWypelnienie .20, szkloRant 1.0, szkloCien .14, tapeta szklo3d —
 * przy tapecie ≠ brak jasny motyw używa ciemniejszego atramentu #1B0A3E).
 */
object GlassTheme { var dark by mutableStateOf(false) }
private val d get() = GlassTheme.dark

val GlassTextPrimary: Color get() = if (d) Color(0xFFFFFFFF) else Color(0xFF1B0A3E)
val GlassTextSecondary: Color get() = if (d) Color(0xFFDCD0F8) else Color(0xFF3F2A78)
val GlassAccent: Color get() = if (d) Color(0xFFB99CFF) else Color(0xFF6B3FE0)

/** --fill: wypełnienie tafli glass. */
val GlassFill: Color get() = if (d) Color.White.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.20f)

/** --rim: obwódka szkła. */
val GlassRim: Color get() = if (d) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.95f)

/** --tint: delikatne tło aktywne/chipy. */
val GlassTint: Color get() = if (d) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.22f)

/** --hair: włosowe separatory. */
val GlassHair: Color get() = if (d) Color.White.copy(alpha = 0.16f) else Color(0x331E0A50)

/** --shd: kolor cienia. */
val GlassShadow: Color get() = if (d) Color(0x5716054A) else Color(0x24551FC2)

/** --srf: powierzchnia arkuszy/dialogów (bez realnego blura – prawie kryjąca). */
val GlassSurface: Color get() = if (d) Color(0xF23A1E84) else Color(0xF7FCFAFF)

/** --inbg: tło pól tekstowych. */
val GlassInputBg: Color get() = if (d) Color.White.copy(alpha = 0.10f) else Color(0x128B5CFF)

/** dockBg / tafla Rutyn — przydymiona (bez realnego backdrop-blur kryjąca mocniej). */
val GlassDockBg: Color get() = if (d) Color(0xF2453C63) else Color(0xF2E7E1F2)

/** Tło szuflady. */
val GlassDrawerBg: Color get() = if (d) Color(0xF02E204E) else Color(0xF5FCFAFF)

/** Pigułka rutyny. */
val GlassRoutinePill: Color get() = if (d) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.30f)

// Priorytety (PC z prototypu)
val PrioColors = listOf(Color(0xFFD1453B), Color(0xFFEB8909), Color(0xFF246FE0), Color(0xFF9E9E9E))

// Tło strony: gradient + „szkło 3D"
private val BgTop: Color get() = if (d) Color(0xFF6E45D9) else Color(0xFFFDFBFF)
private val BgMid: Color get() = if (d) Color(0xFF5B35C4) else Color(0xFFFAF6FE)
private val BgBottom: Color get() = if (d) Color(0xFF3B1D8C) else Color(0xFFF7F0FE)

/**
 * Pełnoekranowe tło „szkło 3D": duże miękkie bryły szkła (biel → fiolet)
 * z wolnym dryfem (20–26s, alternate) + ukośna smuga światła + miętowa
 * poświata — wiernie wg warstw prototypu.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "g3d")
    val pA by t.animateFloat(0f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val pB by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val pC by t.animateFloat(1f, 0f, infiniteRepeatable(tween(23_000, easing = LinearEasing), RepeatMode.Reverse), label = "c")

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom)))
    ) {
        val w = maxWidth
        val h = maxHeight
        val whiteA = if (d) 0.35f else 0.75f
        // bryła 1 — lewy górny róg
        Box(
            Modifier
                .offset(x = w * -0.18f + w * 0.10f * pA, y = h * -0.04f + h * 0.03f * pA)
                .size(w * 0.86f, h * 0.46f)
                .blur(28.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = whiteA),
                            Color(0xFFE0C4FF).copy(alpha = 0.55f),
                            Color(0xFF9660F0).copy(alpha = 0.50f),
                            Color(0xFF6E40D2).copy(alpha = 0.55f)
                        )
                    ),
                    RoundedCornerShape(50)
                )
        )
        // bryła 2 — prawa strona
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = w * 0.20f - w * 0.09f * pB, y = h * 0.34f + h * 0.04f * pB)
                .size(w * 0.86f, h * 0.48f)
                .blur(28.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = whiteA - 0.05f),
                            Color(0xFFD6B2FF).copy(alpha = 0.50f),
                            Color(0xFFA875FF).copy(alpha = 0.50f),
                            Color(0xFF7848D7).copy(alpha = 0.50f)
                        )
                    ),
                    RoundedCornerShape(50)
                )
        )
        // bryła 3 — lewy dół
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = w * -0.10f + w * 0.07f * pC, y = h * 0.14f - h * 0.07f * pC)
                .size(w * 0.78f, h * 0.44f)
                .blur(32.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFCEF0FF).copy(alpha = 0.60f),
                            Color(0xFF9696FF).copy(alpha = 0.45f),
                            Color(0xFF7850DC).copy(alpha = 0.50f)
                        )
                    ),
                    RoundedCornerShape(50)
                )
        )
        // ukośna smuga światła
        Box(
            Modifier
                .offset(x = w * 0.08f, y = h * 0.08f - h * 0.05f * pB)
                .size(w * 0.60f, h * 0.70f)
                .blur(36.dp)
                .background(
                    Brush.linearGradient(
                        0.34f to Color.Transparent,
                        0.50f to Color.White.copy(alpha = if (d) 0.22f else 0.50f),
                        0.62f to Color.Transparent
                    )
                )
        )
        // miętowa poświata prawy dół
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = w * -0.12f, y = h * -0.10f + h * 0.05f * pA)
                .size(w * 0.34f, h * 0.20f)
                .blur(48.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFB4FFEB).copy(alpha = 0.5f), Color.Transparent)), CircleShape)
        )
        content()
    }
}

/**
 * Tafla glass (karta/przycisk): fill + solid rim + cień
 * (inset-highlighty prototypu aproksymowane jasną obwódką).
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(10.dp, shape, spotColor = GlassShadow, ambientColor = GlassShadow)
    .clip(shape)
    .background(GlassFill)
    .border(1.dp, GlassRim, shape)

/** Tafla „Control Center" (dock, Tydzień, kółko Rutyn) — gradientowa, jaśniejsza. */
fun Modifier.controlCenterGlass(shape: Shape = RoundedCornerShape(50)): Modifier = this
    .shadow(12.dp, shape, spotColor = Color(0x291E0A50), ambientColor = Color(0x291E0A50))
    .clip(shape)
    .background(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (d) 0.14f else 0.30f), Color(0xFFD2E6FA).copy(alpha = if (d) 0.05f else 0.10f))
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.5f), shape)
