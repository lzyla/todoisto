package pl.media30.todoisto.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.ui.theme.ThemePalette
import pl.media30.todoisto.ui.theme.ThemePalettes

// ─── Fonty jak w Androidzie: Sora (nagłówki/logo) + Manrope (treść) ─────────
val Sora: FontFamily by lazy {
    runCatching { FontFamily(listOf(400, 500, 600, 700, 800).map { w -> Font(resource = "font/sora.ttf", weight = FontWeight(w)) }) }
        .getOrDefault(FontFamily.SansSerif)
}
val Manrope: FontFamily by lazy {
    runCatching { FontFamily(listOf(400, 500, 600, 700, 800).map { w -> Font(resource = "font/manrope.ttf", weight = FontWeight(w)) }) }
        .getOrDefault(FontFamily.SansSerif)
}

/** Kolory bieżącego motywu (odpowiednik `GlassTheme` + `GlassColors` z Androida). */
data class Glass(
    val dark: Boolean,
    val palette: ThemePalette,
    val accent: Color,
    val tint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val surface: Color,        // karta „szkło"
    val surfaceStrong: Color,  // mocniejsza karta (dock, menu)
    val hair: Color,           // linia włoskowata
    val bgTop: Color,
    val bgBottom: Color,
    val field: Color           // tło pól tekstowych
) {
    val onAccent: Color get() = Color.White
    val danger: Color get() = Color(0xFFD1453B)
    val success: Color get() = Color(0xFF1F8A5B)
    val overdue: Color get() = Color(0xFFC2410C)
    val warn: Color get() = Color(0xFFEB8909)
}

fun glassFor(themeId: String, dark: Boolean): Glass {
    val p = ThemePalettes.byId(themeId)
    return if (!dark) Glass(
        dark = false, palette = p, accent = p.accentLight, tint = p.tintLight,
        textPrimary = Color(0xFF241844), textSecondary = Color(0xFF6E5F93),
        surface = Color.White.copy(alpha = 0.62f), surfaceStrong = Color.White.copy(alpha = 0.86f),
        hair = Color(0xFF241844).copy(alpha = 0.08f),
        bgTop = blend(Color(0xFFEDE6FA), p.tintLight, 0.35f), bgBottom = blend(Color(0xFFDCE4FB), p.tintLight, 0.2f),
        field = Color.White.copy(alpha = 0.75f)
    ) else Glass(
        dark = true, palette = p, accent = p.accentDark, tint = p.tintDark,
        textPrimary = Color(0xFFF1ECFF), textSecondary = Color(0xFFB4A9D6),
        surface = Color.White.copy(alpha = 0.07f), surfaceStrong = Color(0xFF2A2340).copy(alpha = 0.92f),
        hair = Color.White.copy(alpha = 0.10f),
        bgTop = blend(Color(0xFF1B1530), p.tintDark, 0.25f), bgBottom = Color(0xFF120E22),
        field = Color.White.copy(alpha = 0.08f)
    )
}

private fun blend(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f
)

val LocalGlass = staticCompositionLocalOf { glassFor("violet", false) }

/** Kolor priorytetu (P1–P3), P4 → akcent. */
@Composable
fun priorityColor(p: String): Color = when (p) {
    "P1" -> Color(0xFFD1453B); "P2" -> Color(0xFFEB8909); "P3" -> Color(0xFF246FE0); else -> LocalGlass.current.accent
}
fun priorityColorRaw(p: String): Color = when (p) {
    "P1" -> Color(0xFFD1453B); "P2" -> Color(0xFFEB8909); "P3" -> Color(0xFF246FE0); else -> Color(0xFF9E9E9E)
}

// ─── Modyfikatory „szkła" ───────────────────────────────────────────────────
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp), strong: Boolean = false, elevation: Dp = 0.dp): Modifier {
    val g = LocalGlass.current
    return this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, ambientColor = g.accent.copy(alpha = 0.25f), spotColor = g.accent.copy(alpha = 0.25f)) else Modifier)
        .clip(shape)
        .background(if (strong) g.surfaceStrong else g.surface)
        .border(1.dp, Color.White.copy(alpha = if (g.dark) 0.12f else 0.55f), shape)
}

/** Delikatny „żywy" gradient akcentu na kartach (jak `livingGradient`). */
@Composable
fun Modifier.livingGradient(shape: Shape = RoundedCornerShape(22.dp)): Modifier {
    val g = LocalGlass.current
    return this.clip(shape).background(Brush.linearGradient(listOf(g.accent.copy(alpha = 0.12f), g.tint.copy(alpha = 0.35f), Color.Transparent)))
}

/** Okrągły szklany przycisk (jak `CircleGlassButton`). */
@Composable
fun CircleGlassButton(icon: ImageVector, desc: String, size: Dp = 42.dp, tint: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        Modifier.size(size).glass(CircleShape, strong = true).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = if (enabled) (tint ?: g.textPrimary) else g.textSecondary, modifier = Modifier.size(size * 0.46f)) }
}
