package pl.media30.todoisto.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Arkusz/dialog w oknie: przyciemnienie + karta na środku (odpowiednik ModalBottomSheet). */
@Composable
fun Sheet(onDismiss: () -> Unit, maxWidth: Dp = 560.dp, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val g = LocalGlass.current
    Box(Modifier.fillMaxSize().background(Color(0x4D1C0A42)).clickable(indication = null, interactionSource = null) { onDismiss() }, contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth(0.92f).heightIn(max = 760.dp)
                .glass(RoundedCornerShape(28.dp), strong = true, elevation = 24.dp)
                .clickable(indication = null, interactionSource = null) { }
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
        ) { content() }
    }
    // Esc zamyka — obsługiwane na poziomie okna (Main.kt).
    Unit.let { g }
}

/** Pełnoekranowa nakładka (Ustawienia / Konto / Statystyki). */
@Composable
fun FullScreen(content: @Composable BoxScope.() -> Unit) {
    val g = LocalGlass.current
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(g.bgTop, g.bgBottom))).clickable(indication = null, interactionSource = null) { }) { content() }
}

/** Pole tekstowe w stylu szkła (jak `glassFieldColors`). */
@Composable
fun GlassField(
    value: String, onValue: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, singleLine: Boolean = true, password: Boolean = false,
    focus: FocusRequester? = null, onEnter: (() -> Unit)? = null, textSize: Int = 14, minHeight: Dp = 46.dp
) {
    val g = LocalGlass.current
    Box(
        modifier.heightIn(min = minHeight).clip(RoundedCornerShape(14.dp)).background(g.field)
            .border(1.dp, g.hair, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder, fontSize = textSize.sp, color = g.textSecondary.copy(alpha = 0.8f), fontFamily = Manrope)
        BasicTextField(
            value = value, onValueChange = onValue, singleLine = singleLine,
            textStyle = TextStyle(fontSize = textSize.sp, color = g.textPrimary, fontFamily = Manrope, fontWeight = FontWeight.W500),
            cursorBrush = SolidColor(g.accent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth()
                .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
                .onPreviewKeyEvent { e ->
                    if (onEnter != null && e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) { onEnter(); true } else false
                }
        )
    }
}

/** Główny przycisk (pełna szerokość, akcent). */
@Composable
fun PrimaryBtn(text: String, enabled: Boolean = true, icon: ImageVector? = null, color: Color? = null, onClick: () -> Unit) {
    val g = LocalGlass.current
    val bg = (color ?: g.accent).copy(alpha = if (enabled) 1f else 0.45f)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(bg).clickable(enabled = enabled) { onClick() }.padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W800, fontFamily = Manrope)
    }
}

/** Drugi przycisk (tinted). */
@Composable
fun SecondaryBtn(text: String, modifier: Modifier = Modifier, color: Color? = null, onClick: () -> Unit) {
    val g = LocalGlass.current
    val c = color ?: g.accent
    Row(
        modifier.clip(RoundedCornerShape(15.dp)).background(c.copy(alpha = 0.13f)).clickable { onClick() }.padding(vertical = 13.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) { Text(text, color = c, fontSize = 14.sp, fontWeight = FontWeight.W800, fontFamily = Manrope) }
}

/** Chip presetów (jak `PresetChip` / `QAChip`). */
@Composable
fun Chip(text: String, selected: Boolean = false, color: Color? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    val g = LocalGlass.current
    val c = color ?: g.accent
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) c.copy(alpha = 0.18f) else g.field)
            .border(1.dp, if (selected) c.copy(alpha = 0.6f) else g.hair, RoundedCornerShape(50))
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, tint = if (selected) c else g.textSecondary, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(5.dp)) }
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.W700, fontFamily = Manrope, color = if (selected) c else g.textPrimary)
    }
}

/** Nagłówek sekcji wersalikami (jak `SettingsSection` / nagłówki w szufladzie). */
@Composable
fun SectionHeader(text: String, color: Color? = null, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Text(text.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = color ?: g.accent, fontFamily = Manrope, modifier = modifier)
}

/** Nagłówek ekranu z przyciskiem „Wróć". */
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, trailing: RowScopeless = {}) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleGlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Wróć") { onBack() }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.weight(1f))
        trailing()
    }
}
typealias RowScopeless = @Composable () -> Unit

/** Kółko-checkbox jak `GlassCheck` (pierścień w kolorze priorytetu; `emphasize` = mocniejszy). */
@Composable
fun GlassCheck(checked: Boolean, ring: Color, size: Dp = 23.dp, emphasize: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (checked) ring else if (emphasize) ring.copy(alpha = 0.16f) else Color.Transparent)
            .border(if (emphasize) 3.dp else 2.dp, ring, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(size * 0.6f)) }
}

/** Przełącznik jak `GlassToggle` (40×24). */
@Composable
fun GlassToggle(on: Boolean, onToggle: (Boolean) -> Unit) {
    val g = LocalGlass.current
    Box(
        Modifier.width(40.dp).height(24.dp).clip(RoundedCornerShape(50))
            .background(if (on) g.accent else g.textSecondary.copy(alpha = 0.32f)).clickable { onToggle(!on) }.padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) { Box(Modifier.size(18.dp).clip(CircleShape).background(Color.White)) }
}

/** Kropka koloru. */
@Composable
fun Dot(color: Color, size: Dp = 9.dp) { Box(Modifier.size(size).clip(CircleShape).background(color)) }

/** Ikona w kwadraciku z odcieniem (38 dp) — jak w rzędach ustawień i menu ⋮. */
@Composable
fun IconTile(icon: ImageVector, tint: Color, size: Dp = 38.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Rząd nawigacyjny/przełącznika w kartach ustawień. */
@Composable
fun SettingRow(icon: ImageVector, tint: Color, title: String, sub: String?, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope)
            if (sub != null) Text(sub, fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
        }
        trailing()
    }
}

@Composable
fun RowDivider() { val g = LocalGlass.current; Box(Modifier.fillMaxWidth().padding(start = 66.dp, end = 16.dp).height(1.dp).background(g.hair)) }

/** Wybór koloru z palety. */
@Composable
fun ColorPicker(selected: Long, onSelect: (Long) -> Unit) {
    val g = LocalGlass.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PaletteColors.forEach { c ->
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(Color(c))
                    .then(if (c == selected) Modifier.border(3.dp, g.textPrimary, CircleShape) else Modifier)
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center
            ) { if (c == selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)) }
        }
    }
}

// ─── Daty po polsku ─────────────────────────────────────────────────────────
val MS_SHORT = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")
val MONTHS_GEN = listOf("stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca", "lipca", "sierpnia", "września", "października", "listopada", "grudnia")
val WEEKDAYS = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")
val WEEKDAYS_SHORT = listOf("pon", "wt", "śr", "czw", "pt", "sob", "nd")

fun shortDate(epochDay: Long): String { val d = java.time.LocalDate.ofEpochDay(epochDay); return "${d.dayOfMonth} ${MS_SHORT[d.monthValue - 1]}" }
fun dateCaption(d: java.time.LocalDate = java.time.LocalDate.now()): String = "${WEEKDAYS[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]}"
fun weekdayShort(epochDay: Long): String = WEEKDAYS_SHORT[java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1]
fun fmtDay(epochDay: Long?, today: Long = java.time.LocalDate.now().toEpochDay()): String = when (epochDay) {
    null -> "Brak"; today -> "Dzisiaj"; today + 1 -> "Jutro"; else -> shortDate(epochDay)
}
fun fmtDuration(min: Int?): String = when {
    min == null || min <= 0 -> "Brak"; min < 60 -> "$min min"; min % 60 == 0 -> "${min / 60}h"; else -> "${min / 60}h ${min % 60}min"
}
fun hm(min: Int): String = "%d:%02d".format(min / 60, min % 60)
fun plural(n: Int, one: String, few: String, many: String): String = when {
    n == 1 -> one; n % 10 in 2..4 && n % 100 !in 12..14 -> few; else -> many
}
fun nextWeekend(today: Long): Long { var d = java.time.LocalDate.ofEpochDay(today).plusDays(1); while (d.dayOfWeek != java.time.DayOfWeek.SATURDAY) d = d.plusDays(1); return d.toEpochDay() }
