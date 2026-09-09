package pl.media30.todoisto.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Activity
import pl.media30.todoisto.data.EffortType
import pl.media30.todoisto.data.EnergyCost
import pl.media30.todoisto.data.Place

/** Katalog aktywności — jak `ActivityPoolSheet`. */
@Composable
fun ActivityPoolSheet(st: AppState) {
    val g = LocalGlass.current
    st.rev
    val acts = st.repo.activities
    Sheet(onDismiss = { st.showPool = false }, maxWidth = 560.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, null, tint = g.accent, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp))
            Text("Pula aktywności", fontSize = 20.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(50)).background(g.accent).clickable { st.editingActivity = null; st.showActivityForm = true }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp))
                Text("Dodaj", fontSize = 12.5.sp, fontWeight = FontWeight.W800, color = Color.White, fontFamily = Manrope)
            }
        }
        Text("Importuj z Google Sheets", fontSize = 12.sp, fontWeight = FontWeight.W700, color = g.accent, fontFamily = Manrope, modifier = Modifier.padding(top = 6.dp).clickable { st.showImport = true })
        Spacer(Modifier.height(12.dp))
        if (acts.isEmpty()) Text("Pula jest pusta. Dodaj coś, co MOŻESZ robić w wolnym oknie — appka sama zaproponuje kiedy.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
        acts.forEach { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(RoundedCornerShape(18.dp)).clickable { st.editingActivity = a; st.showActivityForm = true }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IconTile(effortIcon(a.effortType), effortColor(a.effortType), 34.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.W700, color = if (a.isActive) g.textPrimary else g.textSecondary, fontFamily = Manrope)
                    Text(activityMeta(a), fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
                }
                if (!a.isActive) Box(Modifier.clip(RoundedCornerShape(50)).background(g.textSecondary.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("pauza", fontSize = 10.5.sp, fontWeight = FontWeight.W800, color = g.textSecondary) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Propozycję na wolne okno znajdziesz w menu ⋮ → „Czas wolny — propozycja”.", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}

fun activityMeta(a: Activity): String = buildList {
    add("${a.durationMinutes} min · ${a.place.label.lowercase()}")
    if (a.windowStartMin != null && a.windowEndMin != null) add("${hm(a.windowStartMin!!)}–${hm(a.windowEndMin!!)}")
    a.frequencyTarget?.let { add("$it×/tydz") }
}.joinToString(" · ")

/** Formularz aktywności — jak `ActivityFormSheet`. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActivityFormSheet(st: AppState) {
    val g = LocalGlass.current
    val editing = st.editingActivity
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var effort by remember { mutableStateOf(editing?.effortType ?: EffortType.PHYSICAL) }
    var duration by remember { mutableStateOf(editing?.durationMinutes ?: 30) }
    var place by remember { mutableStateOf(editing?.place ?: Place.HOME) }
    var days by remember { mutableStateOf(editing?.daysMask ?: 0b1111111) }
    var winStart by remember { mutableStateOf(editing?.windowStartMin) }
    var winEnd by remember { mutableStateOf(editing?.windowEndMin) }
    var energy by remember { mutableStateOf(editing?.energyCost ?: EnergyCost.MED) }
    var freq by remember { mutableStateOf(editing?.frequencyTarget) }
    var active by remember { mutableStateOf(editing?.isActive ?: true) }
    var more by remember { mutableStateOf(false) }
    fun close() { st.showActivityForm = false; st.editingActivity = null }
    fun assemble() = (editing ?: Activity(name = "", effortType = effort)).copy(
        name = name.trim(), effortType = effort, durationMinutes = duration, place = place, daysMask = days,
        windowStartMin = winStart, windowEndMin = winEnd, energyCost = energy, frequencyTarget = freq, isActive = active
    )
    Sheet(onDismiss = { close() }, maxWidth = 560.dp) {
        Text(if (editing == null) "Nowa aktywność" else "Aktywność", fontSize = 20.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Spacer(Modifier.height(12.dp))
        GlassField(name, { name = it }, "np. Trening w domu, Czytanie, Duolingo")
        Spacer(Modifier.height(14.dp))
        SectionHeader("Typ wysiłku", g.textSecondary); Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { EffortType.entries.forEach { e -> Chip(e.label, effort == e, effortColor(e)) { effort = e } } }
        Spacer(Modifier.height(12.dp))
        SectionHeader("Czas trwania", g.textSecondary); Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(15, 30, 45, 60, 90).forEach { m -> Chip(if (m < 60) "$m min" else "${m / 60}h" + (if (m % 60 != 0) " ${m % 60}m" else ""), duration == m) { duration = m } } }
        Spacer(Modifier.height(12.dp))
        Text(if (more) "Mniej ▲" else "Więcej ▼", fontSize = 12.5.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Manrope, modifier = Modifier.clickable { more = !more }.padding(4.dp))
        if (more) {
            Spacer(Modifier.height(10.dp))
            SectionHeader("Miejsce", g.textSecondary); Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Place.entries.forEach { p -> Chip(p.label, place == p) { place = p } } }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Dni tygodnia", g.textSecondary); Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("P", "W", "Ś", "C", "P", "S", "N").forEachIndexed { i, d ->
                    val on = days and (1 shl i) != 0
                    Box(Modifier.size(34.dp).clip(CircleShape).background(if (on) g.accent else g.field).border(1.dp, if (on) g.accent else g.hair, CircleShape).clickable { days = days xor (1 shl i) }, contentAlignment = Alignment.Center) {
                        Text(d, fontSize = 12.sp, fontWeight = FontWeight.W800, color = if (on) Color.White else g.textPrimary, fontFamily = Manrope)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Okno godzin", g.textSecondary); Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Od:", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.align(Alignment.CenterVertically))
                listOf(6, 8, 10, 12, 14, 16, 18, 20).forEach { h -> Chip("%02d:00".format(h), winStart == h * 60) { winStart = if (winStart == h * 60) null else h * 60 } }
            }
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Do:", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.align(Alignment.CenterVertically))
                listOf(9, 12, 15, 18, 20, 22, 23).forEach { h -> Chip("%02d:00".format(h), winEnd == h * 60) { winEnd = if (winEnd == h * 60) null else h * 60 } }
                if (winStart != null || winEnd != null) Chip("wyczyść") { winStart = null; winEnd = null }
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Koszt energii", g.textSecondary); Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { EnergyCost.entries.forEach { e -> Chip(e.label, energy == e) { energy = e } } }
            Spacer(Modifier.height(12.dp))
            SectionHeader("Cel częstotliwości", g.textSecondary); Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Chip("brak", freq == null) { freq = null }; (1..5).forEach { n -> Chip("$n×/tydz", freq == n) { freq = n } } }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (active) "Aktywna" else "Wstrzymana", fontSize = 14.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
                GlassToggle(active) { active = it }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (editing != null) SecondaryBtn("Usuń", Modifier.weight(1f), g.danger) { st.deleteActivity(editing.id); close() }
            Box(Modifier.weight(2f).clip(RoundedCornerShape(15.dp)).background(if (name.isBlank()) g.field else g.accent).clickable(enabled = name.isNotBlank()) { if (editing == null) st.addActivity(assemble()) else st.updateActivity(assemble()); close() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Text("Zapisz", fontSize = 14.sp, fontWeight = FontWeight.W800, color = if (name.isBlank()) g.textSecondary else Color.White, fontFamily = Manrope)
            }
        }
    }
}

/** Panel „Czas wolny" — jak `FreeTimePanel` (jedna propozycja z puli). */
@Composable
fun FreeTimeSheet(st: AppState, f: FreeTimeState) {
    val g = LocalGlass.current
    Sheet(onDismiss = { st.freeTime = null }, maxWidth = 480.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, null, tint = g.accent, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp))
            Text("Czas wolny", fontSize = 20.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.weight(1f))
            if (!f.noWindows && !f.poolEmpty) {
                val h = f.freeMinutes / 60; val m = f.freeMinutes % 60
                Text(when { h > 0 && m > 0 -> "~${h}h ${m}min wolnego"; h > 0 -> "~${h}h wolnego"; else -> "~${m}min wolnego" }, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope)
            }
        }
        Spacer(Modifier.height(14.dp))
        val s = f.suggestions.firstOrNull()
        when {
            f.poolEmpty -> EmptyHint("Pula jest pusta.", "Dodaj pierwszą aktywność, a appka wylosuje ją w wolnym oknie.", "Dodaj aktywność") { st.freeTime = null; st.editingActivity = null; st.showActivityForm = true }
            f.noWindows -> EmptyHint("Dziś brak wolnego czasu.", "Wszystkie okna zajęte zaplanowanymi zadaniami.")
            s == null -> EmptyHint("Na teraz nic nie pasuje.", "Skróć aktywności lub dodaj krótszy wariant do puli.")
            else -> Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).livingGradient(RoundedCornerShape(26.dp)).padding(18.dp)) {
                Text("PROPOZYCJA NA TERAZ", fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 1.4.sp, color = g.textSecondary, fontFamily = Manrope)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(effortIcon(s.activity.effortType), effortColor(s.activity.effortType), 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(s.activity.name, fontSize = 19.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
                        Text("${hm(s.startMin)} · ${s.activity.durationMinutes} min · ${s.activity.place.label.lowercase()}", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { PrimaryBtn("Dodaj do dziś") { st.acceptSuggestion(s) } }
                    Box(Modifier.size(52.dp).clip(CircleShape).background(g.accent.copy(alpha = 0.12f)).clickable { st.rerollSuggestion() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Refresh, "Wylosuj ponownie", tint = g.accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(head: String, sub: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(16.dp)) {
        Text(head, fontSize = 15.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
        Text(sub, fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
        if (action != null && onAction != null) { Spacer(Modifier.height(12.dp)); PrimaryBtn(action) { onAction() } }
    }
}

/** Import aktywności z arkusza Google — jak `ImportActivitiesDialog`. */
@Composable
fun ImportActivitiesDialog(st: AppState) {
    val g = LocalGlass.current
    var url by remember { mutableStateOf("") }
    Sheet(onDismiss = { st.showImport = false }, maxWidth = 480.dp) {
        Text("Import z Google Sheets", fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Spacer(Modifier.height(8.dp))
        Text("Wklej link do arkusza opublikowanego jako CSV (Plik → Udostępnij → Opublikuj w internecie → CSV) lub zwykły link do arkusza.\n\nKolumny: nazwa, minuty, wysiłek (fizyczny/umysłowy/regeneracja), miejsce, energia.", fontSize = 13.sp, lineHeight = 18.sp, color = g.textSecondary, fontFamily = Manrope)
        Spacer(Modifier.height(12.dp))
        GlassField(url, { url = it }, "https://docs.google.com/…")
        Spacer(Modifier.height(14.dp))
        PrimaryBtn("Importuj", enabled = url.isNotBlank()) { st.importActivities(url); st.showImport = false }
        DialogCancel { st.showImport = false }
    }
}
