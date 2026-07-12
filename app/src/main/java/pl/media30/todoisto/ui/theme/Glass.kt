package pl.media30.todoisto.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.remember
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
    /** Tryb tła: false = gradient „mesh", true = malarska scena „foto" zależna od pory dnia. */
    var photo by mutableStateOf(false)
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

// Wielokolorowy gradient „mesh" zależny od pory dnia (bez kształtów):
// [base] — 4 pionowe przystanki; [meshL]/[meshR] — poprzeczna warstwa,
// dająca różnicę koloru między lewą a prawą krawędzią (efekt aurory).
private class BgPalette(
    val base: List<Color>,
    val meshL: Color, val meshR: Color, val meshTop: Color
)

private fun bgPalette(): BgPalette = when (GlassTheme.phase) {
    // Rano — ciepły świt: brzoskwinia → róż → liliowy → błękit (jaśniej)
    DayPhase.MORNING -> if (d) BgPalette(
        listOf(Color(0xFF5A3E6E), Color(0xFF46305E), Color(0xFF3A2A55), Color(0xFF2A2148)),
        Color(0xFFCC7A5E), Color(0xFF5E6FB0), Color(0xFFCC6E86)
    ) else BgPalette(
        listOf(Color(0xFFFFF0E4), Color(0xFFFFE2EC), Color(0xFFF2E6FF), Color(0xFFE1EFFF)),
        Color(0xFFFFD5B6), Color(0xFFB7D2FF), Color(0xFFFFC9D6)
    )
    // Południe — powietrzny dzień: błękit → cyjan → liliowy → mięta (jaśniej)
    DayPhase.NOON -> if (d) BgPalette(
        listOf(Color(0xFF33487E), Color(0xFF34406E), Color(0xFF2C3A66), Color(0xFF232E58)),
        Color(0xFF4E80C8), Color(0xFF7E6FC8), Color(0xFF4EA0C0)
    ) else BgPalette(
        listOf(Color(0xFFE7F4FF), Color(0xFFECF1FF), Color(0xFFF1EAFF), Color(0xFFE7FAFF)),
        Color(0xFFAED6FF), Color(0xFFD2BEFF), Color(0xFFBEF1FF)
    )
    // Wieczór — zmierzch: magenta → fiolet → indygo → róż (jaśniej)
    DayPhase.EVENING -> if (d) BgPalette(
        listOf(Color(0xFF7A3FB0), Color(0xFF5B35C4), Color(0xFF44239E), Color(0xFF361C7E)),
        Color(0xFFB44DD8), Color(0xFF6A5CD8), Color(0xFFD86EB8)
    ) else BgPalette(
        listOf(Color(0xFFF8E6FF), Color(0xFFEEE1FF), Color(0xFFE6E3FF), Color(0xFFF7E6F6)),
        Color(0xFFF0BEF6), Color(0xFFBEBEF6), Color(0xFFF6BEE4)
    )
}

// Malarska scena „foto" — niebo + poświata słońca/księżyca + pas horyzontu.
private class PhotoScene(
    val sky: List<Color>,   // pionowe niebo (góra→dół)
    val glow: Color,        // barwa poświaty słońca/księżyca
    val glowAt: Offset,     // względne (0..1) położenie poświaty
    val horizon: Color      // barwa pasa przy dole
)

private fun photoScene(): PhotoScene = when (GlassTheme.phase) {
    DayPhase.MORNING -> if (d) PhotoScene(
        listOf(Color(0xFF2A2148), Color(0xFF3A2A55), Color(0xFF5A3E6E), Color(0xFF7A4E5E)),
        Color(0xFFE8A06E), Offset(0.22f, 0.72f), Color(0xFF8A5A64)
    ) else PhotoScene(
        listOf(Color(0xFFBFE0FF), Color(0xFFDCE6FF), Color(0xFFFFE0D8), Color(0xFFFFD1B0)),
        Color(0xFFFFC98A), Offset(0.22f, 0.70f), Color(0xFFFFB98E)
    )
    DayPhase.NOON -> if (d) PhotoScene(
        listOf(Color(0xFF20305E), Color(0xFF2C3A66), Color(0xFF34518A), Color(0xFF4E77B0)),
        Color(0xFF9AC4F0), Offset(0.72f, 0.20f), Color(0xFF4E77A0)
    ) else PhotoScene(
        listOf(Color(0xFF8FC6FF), Color(0xFFB6DEFF), Color(0xFFDCEFFF), Color(0xFFEFF8FF)),
        Color(0xFFFFFBE8), Offset(0.74f, 0.18f), Color(0xFFCDEBFF)
    )
    DayPhase.EVENING -> if (d) PhotoScene(
        listOf(Color(0xFF251550), Color(0xFF3A1C7E), Color(0xFF5B2FA0), Color(0xFF8A3E80)),
        Color(0xFFE86EA8), Offset(0.78f, 0.74f), Color(0xFF8A3E6E)
    ) else PhotoScene(
        listOf(Color(0xFFB9C4FF), Color(0xFFD6C4FF), Color(0xFFF0C4E8), Color(0xFFFFC9C0)),
        Color(0xFFFFB06E), Offset(0.78f, 0.72f), Color(0xFFF0A0A6)
    )
}

/**
 * Pełnoekranowe tło. Backdrop jest osobnym „rodzeństwem" [content], więc jego
 * powolna animacja (morfizm) NIE rekomponuje treści aplikacji. Dwa tryby:
 * gradient „mesh" (domyślny) lub malarska scena „foto" zależna od pory dnia.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().glassBackdrop())
        content()
    }
}

/** Animowany backdrop (drift w fazie rysowania — bez rekompozycji treści). */
private fun Modifier.glassBackdrop(): Modifier = composed {
    val dark = GlassTheme.dark
    val photo = GlassTheme.photo
    val pal = bgPalette()
    val scene = photoScene()
    val tr = rememberInfiniteTransition(label = "bg")
    val t = tr.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bgT"
    )
    drawBehind {
        val p = t.value          // 0..1 tam i z powrotem
        val drift = (p - 0.5f)   // -0.5..0.5
        val w = size.width; val h = size.height
        if (photo) {
            // niebo
            drawRect(Brush.verticalGradient(scene.sky))
            // poświata słońca/księżyca — dryfuje delikatnie w poziomie
            val gx = (scene.glowAt.x + drift * 0.10f) * w
            val gy = scene.glowAt.y * h
            val r = maxOf(w, h) * (0.55f + 0.05f * p)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(scene.glow.copy(alpha = if (dark) 0.55f else 0.85f), Color.Transparent),
                    center = Offset(gx, gy), radius = r
                )
            )
            // pas horyzontu przy dole
            drawRect(
                Brush.verticalGradient(
                    0.62f to Color.Transparent, 1f to scene.horizon.copy(alpha = if (dark) 0.5f else 0.6f)
                )
            )
        } else {
            val meshA = if (dark) 0.55f else 0.58f
            drawRect(Brush.verticalGradient(pal.base))
            // pozioma warstwa mesh — środek dryfuje w lewo/prawo (morfizm)
            val cx = 0.5f + drift * 0.6f
            drawRect(
                Brush.horizontalGradient(
                    0f to pal.meshL.copy(alpha = meshA),
                    cx.coerceIn(0.15f, 0.85f) to Color.Transparent,
                    1f to pal.meshR.copy(alpha = meshA)
                )
            )
            // górna poświata — koniec wektora dryfuje (aurora oddycha)
            drawRect(
                Brush.linearGradient(
                    colors = listOf(pal.meshTop.copy(alpha = meshA), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(w * (0.5f + drift * 0.4f), h * (0.9f + drift * 0.3f))
                )
            )
        }
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
        .shadow(18.dp, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.5f))
        .clip(shape)
        .background(GlassFill)
        .drawWithContent {
            drawContent()
            // subtelny górny połysk zamiast twardej, tłoczonej krawędzi
            if (sheenOn) sheen(prog, if (dark) 0.08f else 0.12f)
        }
        .border(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.22f), shape)
}

/**
 * Kafelek zadania (wg prototypu vC): w spoczynku prawie niewidoczny — leży
 * bezpośrednio na gradiencie (ledwie muśnięcie bieli + włosowy rant). Dotknięcie
 * MATERIALIZUJE szkło: wypełnienie rośnie, pojawia się rant i miękki cień, a cała
 * tafla delikatnie się unosi (scale 1.02, jak w prototypie: transform:scale(1.02)
 * + inset highlight + box-shadow). Jeden impuls haptyczny.
 */
fun Modifier.taskTile(onClick: () -> Unit): Modifier = composed {
    val dark = GlassTheme.dark
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(20.dp)
    val fill by animateColorAsState(
        targetValue = if (pressed) {
            // szkło po dotknięciu — bardzo przezroczyste, tło mocno prześwituje
            if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)
        } else {
            // spoczynek — praktycznie niewidoczny
            if (dark) Color.White.copy(alpha = 0.015f) else Color.White.copy(alpha = 0.02f)
        },
        animationSpec = tween(280), label = "tileFill"
    )
    val rim by animateColorAsState(
        targetValue = if (pressed) {
            if (dark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.40f)
        } else {
            if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.07f)
        },
        animationSpec = tween(280), label = "tileRim"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 1.02f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "tileScale"
    )
    val elev by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (pressed) 16.dp else 0.dp,
        animationSpec = tween(280), label = "tileElev"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(elev, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.5f))
        .clip(shape)
        .background(fill)
        .border(1.dp, rim, shape)
        .clickable(interactionSource = interaction, indication = null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        }
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
        .shadow(14.dp, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.5f))
        .clip(shape)
        .background(Color.White.copy(alpha = if (dark) 0.12f else 0.24f))
        .drawWithContent {
            drawContent()
            // delikatny przesuwający się połysk — bez twardej, tłoczonej obwódki
            sheen(prog, if (dark) 0.08f else 0.13f)
        }
        .border(1.dp, if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.20f), shape)
}
