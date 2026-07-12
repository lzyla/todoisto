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
import dev.chrisbanes.haze.hazeChild
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

/** Wspólny stan Haze do prawdziwego rozmycia tła (backdrop blur) pod taflami. */
val LocalHazeState = androidx.compose.runtime.staticCompositionLocalOf<dev.chrisbanes.haze.HazeState?> { null }

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

/** --fill: wypełnienie tafli glass (bardziej przezroczyste — więcej „szkła"). */
val GlassFill: Color get() = if (d) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f)

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

/** dockBg / tafla Rutyn — w pełni kryjąca (żeby treść listy nie prześwitywała). */
val GlassDockBg: Color get() = if (d) Color(0xFF3B3357) else Color(0xFFF1ECFB)

/** Tło szuflady. */
val GlassDrawerBg: Color get() = if (d) Color(0xF02E204E) else Color(0xF5FCFAFF)

/** Pigułka rutyny. */
val GlassRoutinePill: Color get() = if (d) Color.White.copy(alpha = 0.10f) else Color(0xFFFBF9FF)

// Priorytety (PC z prototypu)
val PrioColors = listOf(Color(0xFFD1453B), Color(0xFFEB8909), Color(0xFF246FE0), Color(0xFF9E9E9E))

// Wielokolorowy gradient „mesh" zależny od pory dnia (bez kształtów):
// [base] — 4 pionowe przystanki; [meshL]/[meshR] — poprzeczna warstwa,
// dająca różnicę koloru między lewą a prawą krawędzią (efekt aurory).
private class BgPalette(
    val base: List<Color>,
    val meshL: Color, val meshR: Color, val meshTop: Color, val meshAccent: Color
)

// Wyraźnie różne barwy (żeby nie zlewały się z fioletem UI): turkus, róż/magenta,
// błękit, żółty, koral. Cztery żywe „bloby" nad jasną bazą (light) / głęboką (dark).
private fun bgPalette(): BgPalette = when (GlassTheme.phase) {
    // Rano — świt: koral + róż + żółty + turkus
    DayPhase.MORNING -> if (d) BgPalette(
        listOf(Color(0xFF4A3A5E), Color(0xFF3E2F54), Color(0xFF32284C), Color(0xFF261E42)),
        Color(0xFFD46E4E), Color(0xFFC85C90), Color(0xFFC0982E), Color(0xFF2EA68C)
    ) else BgPalette(
        listOf(Color(0xFFFFF3E6), Color(0xFFFFE8F0), Color(0xFFEAF0FF), Color(0xFFE6FBFF)),
        Color(0xFFFF8A5C), Color(0xFFFF80B4), Color(0xFFFFD24D), Color(0xFF3FD9C8)
    )
    // Południe — dzień: turkus + róż + błękit + żółty
    DayPhase.NOON -> if (d) BgPalette(
        listOf(Color(0xFF2A3A66), Color(0xFF2E3A60), Color(0xFF283458), Color(0xFF20294E)),
        Color(0xFF2EA69C), Color(0xFFC85CA8), Color(0xFF4E80C8), Color(0xFFC0A02E)
    ) else BgPalette(
        listOf(Color(0xFFEAF6FF), Color(0xFFF0ECFF), Color(0xFFFCEAFF), Color(0xFFEAFBFF)),
        Color(0xFF3FD9C8), Color(0xFFFF7FD0), Color(0xFF5FA8FF), Color(0xFFFFD24D)
    )
    // Wieczór — zmierzch: magenta/róż + błękit + koral + złoty
    DayPhase.EVENING -> if (d) BgPalette(
        listOf(Color(0xFF5A3A88), Color(0xFF48307E), Color(0xFF382470), Color(0xFF2C1C60)),
        Color(0xFFC24DA0), Color(0xFF5C6CD0), Color(0xFFC0664E), Color(0xFFC09A2E)
    ) else BgPalette(
        listOf(Color(0xFFFDEAF4), Color(0xFFEFE6FF), Color(0xFFE6ECFF), Color(0xFFFFEFE2)),
        Color(0xFFF06FB0), Color(0xFF7E86F0), Color(0xFFFF8A6E), Color(0xFFFFC24D)
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
    // Wolny obrót fazy (0..1 → 2π) — orbitujące „bloby" koloru dają morfizm.
    val ang = tr.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "bgAng"
    )
    drawBehind {
        val p = t.value          // 0..1 tam i z powrotem
        val drift = (p - 0.5f)   // -0.5..0.5
        val a = ang.value * 6.2832f
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
            val meshA = if (dark) 0.60f else 0.72f
            // Baza — neutralna, niskokontrastowa (żaden kolor nie „siedzi" na stałe na górze).
            val baseTop = if (dark) Color(0xFF241E3C) else Color(0xFFF3F1FA)
            val baseBot = if (dark) Color(0xFF1B1630) else Color(0xFFF6F2FB)
            drawRect(Brush.verticalGradient(listOf(baseTop, baseBot)))
            // Wielokolorowe „bloby" krążące po SZEROKICH elipsach — cała plansza się mieni.
            val big = maxOf(w, h)
            fun blob(color: Color, phase: Float, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float) {
                val x = (cx + rx * kotlin.math.cos(a + phase)) * w
                val y = (cy + ry * kotlin.math.sin(a + phase)) * h
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = Offset(x, y),
                        radius = big * (0.62f + 0.08f * kotlin.math.sin(a + phase))  // pulsujący promień
                    )
                )
            }
            blob(pal.meshL, 0f, 0.32f, 0.40f, 0.34f, 0.40f, meshA)
            blob(pal.meshR, 1.7f, 0.66f, 0.52f, 0.32f, 0.42f, meshA)
            blob(pal.meshTop, 3.3f, 0.50f, 0.44f, 0.40f, 0.46f, meshA * 0.95f)
            blob(pal.meshAccent, 4.9f, 0.48f, 0.55f, 0.36f, 0.44f, meshA * (0.75f + 0.25f * p))
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
            // refleks: górny pasek światła + delikatna górna poświata (jak w szkle)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (dark) 0.10f else 0.22f),
                    0.5f to Color.Transparent
                ),
                size = Size(size.width, size.height * 0.6f)
            )
            specular(if (dark) 0.22f else 0.5f)
            if (sheenOn) sheen(prog, if (dark) 0.09f else 0.14f)
        }
        .border(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.28f), shape)
}

/**
 * Tafla szkła z PRAWDZIWYM rozmyciem tła (Haze), gdy dostępny [LocalHazeState] —
 * treść pod spodem jest rozmyta (liquid glass). Bez stanu Haze spada do [glass].
 */
fun Modifier.glassBlur(shape: Shape = RoundedCornerShape(24.dp)): Modifier = composed {
    val haze = LocalHazeState.current ?: return@composed this.then(Modifier.glass(shape))
    val dark = GlassTheme.dark
    // backgroundColor MUSI być podany — inaczej Haze rzuca wyjątek na ścieżce
    // awaryjnej (brak RenderEffect: layoutlib/Paparazzi oraz API < 31).
    // Ton zbliżony do bazy mesh-gradientu, żeby fallback ładnie się zlewał.
    val hazeStyle = dev.chrisbanes.haze.HazeStyle(
        backgroundColor = if (dark) Color(0xFF241E3C) else Color(0xFFF3F1FA),
        tints = emptyList()
    )
    this
        .shadow(16.dp, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.5f))
        .clip(shape)
        .hazeChild(state = haze, style = hazeStyle)
        // lekki „mleczny" nalot na rozmyciu — czytelność tekstu przy zachowaniu przezroczystości
        .background(if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.16f))
        .drawWithContent {
            drawContent()
            specular(if (dark) 0.20f else 0.42f)
        }
        .border(1.dp, if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.40f), shape)
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
        .shadow(18.dp, shape, spotColor = SoftShadow, ambientColor = SoftShadow.copy(alpha = 0.6f))
        .clip(shape)
        // szklana tafla: półprzezroczysta (tło prześwituje), ale wciąż czytelna
        .background(if (dark) Color(0x99473C63) else Color(0xC2FFFFFF))
        .drawWithContent {
            drawContent()
            // refleksy: górna poświata + pasek światła + dryfujący połysk
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (dark) 0.12f else 0.30f),
                    0.55f to Color.Transparent
                ),
                size = Size(size.width, size.height * 0.7f)
            )
            specular(if (dark) 0.25f else 0.55f)
            sheen(prog, if (dark) 0.09f else 0.15f)
        }
        .border(1.dp, if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.5f), shape)
}
