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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import java.time.LocalTime

/**
 * Tokeny wprost z prototypu Todoisto.dc.html (funkcja colors() + domyślne
 * props: szkloWypelnienie .20, szkloRant 1.0, szkloCien .14, tapeta szklo3d —
 * przy tapecie ≠ brak jasny motyw używa ciemniejszego atramentu #1B0A3E).
 */
/** Pora dnia sterująca kolorem tła (rano / południe / wieczór). */
enum class DayPhase(val label: String) { MORNING("Rano"), NOON("Południe"), EVENING("Wieczór") }

fun dayPhaseFromClock(hour: Int = LocalTime.now().hour): DayPhase = when (hour) {
    in 5..10 -> DayPhase.MORNING
    in 11..16 -> DayPhase.NOON
    else -> DayPhase.EVENING
}

object GlassTheme {
    var dark by mutableStateOf(false)
    /** Ustawiane przy starcie z zegara; wpływa tylko na tło (nie na motyw tekstu). */
    var phase by mutableStateOf(DayPhase.NOON)
}
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

// Tło strony: gradient + „szkło 3D", z paletą zależną od pory dnia.
private class BgPalette(
    val top: Color, val mid: Color, val bottom: Color,
    val lobe1: Color, val lobe2: Color, val lobe3: Color,
    val streakAlpha: Float, val glow: Color
)

private fun bgPalette(): BgPalette = when (GlassTheme.phase) {
    // Rano — ciepły świt: brzoskwinia, róż, delikatny błękit
    DayPhase.MORNING -> if (d) BgPalette(
        Color(0xFF4A3A6E), Color(0xFF3E2E5E), Color(0xFF2A1B45),
        Color(0xFFFFC79E), Color(0xFFFF9FB8), Color(0xFFA9C8FF), 0.20f, Color(0xFFFFE0A8)
    ) else BgPalette(
        Color(0xFFFFF7F1), Color(0xFFFDF1EC), Color(0xFFF6EFFB),
        Color(0xFFFFD9B0), Color(0xFFFFC2D2), Color(0xFFCFE4FF), 0.48f, Color(0xFFFFEABF)
    )
    // Południe — jasny, powietrzny dzień: błękit nieba, cyjan, liliowy
    DayPhase.NOON -> if (d) BgPalette(
        Color(0xFF34407E), Color(0xFF2C3A6E), Color(0xFF212C58),
        Color(0xFF8FC4FF), Color(0xFFB9A6FF), Color(0xFF9EE9FF), 0.20f, Color(0xFFB4FFEB)
    ) else BgPalette(
        Color(0xFFFDFCFF), Color(0xFFF1F8FF), Color(0xFFEBF3FF),
        Color(0xFFBFE0FF), Color(0xFFD6C2FF), Color(0xFFC7F5FF), 0.50f, Color(0xFFB4FFEB)
    )
    // Wieczór — fioletowy zmierzch (bazowy klimat prototypu)
    DayPhase.EVENING -> if (d) BgPalette(
        Color(0xFF6E45D9), Color(0xFF5B35C4), Color(0xFF3B1D8C),
        Color(0xFFE0C4FF), Color(0xFFA875FF), Color(0xFF9696FF), 0.22f, Color(0xFFFFC4E8)
    ) else BgPalette(
        Color(0xFFFDFBFF), Color(0xFFFAF6FE), Color(0xFFF6F0FE),
        Color(0xFFE7B6FF), Color(0xFFC9A6FF), Color(0xFFB3B0FF), 0.50f, Color(0xFFFFC4E8)
    )
}

/**
 * Pełnoekranowe tło: sam czysty gradient zależny od pory dnia — bez kształtów
 * (wg prototypu: body{background:linear-gradient(...)}). Diagonalny kierunek
 * 165° jak w źródle.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    val pal = bgPalette()
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(pal.top, pal.mid, pal.bottom),
                    start = Offset(0f, 0f),
                    end = Offset(220f, 1000f)
                )
            )
    ) {
        content()
    }
}

// Miękki, „nie-czarny" cień (spec Apple: 0 25px 60px rgba(30,30,40,.16))
private val SoftShadow: Color get() = if (d) Color(0x59140542) else Color(0x291E1E28)

/**
 * Górny refleks (2px pasek światła u samej krawędzi) — wspólny dla tafli.
 * Rysowany w obrębie [shape] (po .clip), więc podąża za zaokrągleniem.
 */
private fun DrawScope.specular(topAlpha: Float) {
    drawRect(
        Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = topAlpha), Color.Transparent)
        ),
        size = Size(size.width, 2.dp.toPx())
    )
}

/** Powoli przesuwający się ukośny połysk (warstwa odbić światła). */
private fun DrawScope.sheen(progress: Float, alpha: Float) {
    val band = size.width * 0.32f
    val x = size.width * progress
    rotate(degrees = 18f, pivot = Offset(size.width / 2f, size.height / 2f)) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
                startX = x - band, endX = x + band
            ),
            topLeft = Offset(x - band, -size.height * 0.25f),
            size = Size(band * 2f, size.height * 1.5f)
        )
    }
}

/**
 * Tafla glass (karta/tafla/przycisk) w duchu Apple Liquid Glass:
 * miękki rozproszony cień, półprzezroczyste wypełnienie (tło prześwituje),
 * gradientowy rant (jasny u góry → nikły u dołu), górny refleks świetlny
 * oraz powoli dryfujący ukośny połysk. [sheenOn] wyłącza połysk dla drobnych
 * elementów (rytm listy), by nie mnożyć animacji.
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), sheenOn: Boolean = true): Modifier = composed {
    val dark = GlassTheme.dark
    val prog = if (sheenOn) {
        val tr = rememberInfiniteTransition(label = "sheen")
        tr.animateFloat(
            initialValue = -0.35f, targetValue = 1.35f,
            animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
            label = "sx"
        ).value
    } else -2f
    this
        .shadow(20.dp, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.5f))
        .clip(shape)
        .background(GlassFill)
        .drawWithContent {
            drawContent()
            specular(if (dark) 0.55f else 0.85f)
            if (sheenOn) sheen(prog, if (dark) 0.10f else 0.16f)
        }
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(GlassRim, GlassRim.copy(alpha = 0.10f), GlassRim.copy(alpha = 0.30f))
            ),
            shape
        )
}

/**
 * Kafelek zadania (wg prototypu vC): zaokrąglony (20dp), prawie niewidoczny
 * w spoczynku — leży bezpośrednio na gradiencie, delikatny rant sugeruje taflę,
 * dotyk (bouncy) materializuje szkło.
 */
fun Modifier.taskTile(): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return this
        .clip(shape)
        .background(if (d) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.14f))
        .border(1.dp, if (d) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.30f), shape)
}

/** Tafla „Control Center" (dock, Tydzień, kółko Rutyn) — gradientowa, jaśniejsza. */
fun Modifier.controlCenterGlass(shape: Shape = RoundedCornerShape(50)): Modifier = composed {
    val dark = GlassTheme.dark
    val tr = rememberInfiniteTransition(label = "ccSheen")
    val prog by tr.animateFloat(
        initialValue = -0.35f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "ccx"
    )
    this
        .shadow(16.dp, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.5f))
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = if (dark) 0.16f else 0.32f),
                    Color(0xFFD2E6FA).copy(alpha = if (dark) 0.05f else 0.10f)
                )
            )
        )
        .drawWithContent {
            drawContent()
            specular(if (dark) 0.6f else 0.95f)
            sheen(prog, if (dark) 0.10f else 0.18f)
        }
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.12f))), shape)
}
