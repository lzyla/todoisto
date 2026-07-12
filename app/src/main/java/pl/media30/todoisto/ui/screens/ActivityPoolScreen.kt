package pl.media30.todoisto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Activity
import pl.media30.todoisto.data.EffortType
import pl.media30.todoisto.data.EnergyCost
import pl.media30.todoisto.data.Place
import pl.media30.todoisto.data.dayBit
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTint
import pl.media30.todoisto.ui.theme.glass

private fun hm(min: Int) = "%d:%02d".format(min / 60, min % 60)

private fun metaLine(a: Activity): String {
    val parts = mutableListOf("${a.durationMinutes} min", a.place.label.lowercase())
    if (a.windowStartMin != null && a.windowEndMin != null) parts += "${hm(a.windowStartMin)}–${hm(a.windowEndMin)}"
    a.frequencyTarget?.let { parts += "${it}×/tydz" }
    return parts.joinToString(" · ")
}

/** Katalog puli — lista aktywności na taflach glass. */
@Composable
fun ActivityPoolSheet(
    activities: List<Activity>,
    onAdd: () -> Unit,
    onEdit: (Activity) -> Unit,
    onImport: () -> Unit = {}
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, null, tint = GlassAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pula aktywności", fontSize = 20.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(GlassAccent).bouncy(0.92f, onAdd).padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Dodaj", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.W800)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Importuj z Google Sheets",
            fontSize = 12.sp, fontWeight = FontWeight.W700, color = GlassAccent,
            modifier = Modifier.clip(RoundedCornerShape(50)).bouncy(0.96f, onImport).padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        if (activities.isEmpty()) {
            Text(
                "Pula jest pusta. Dodaj coś, co MOŻESZ robić w wolnym oknie — appka sama zaproponuje kiedy.",
                fontSize = 13.sp, color = GlassTextSecondary, modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activities.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp))
                            .bouncy(0.98f) { onEdit(a) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(a.effortType.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                a.name, fontSize = 15.sp, fontWeight = FontWeight.W700,
                                color = if (a.isActive) GlassTextPrimary else GlassTextSecondary
                            )
                            Text(metaLine(a), fontSize = 11.5.sp, color = GlassTextSecondary)
                        }
                        if (!a.isActive) Text("pauza", fontSize = 11.sp, color = GlassTextSecondary,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassTint).padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        }
    }
}

/** Formularz aktywności — rdzeń (nazwa/typ/czas) + zwijana sekcja „Więcej". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityFormSheet(
    existing: Activity?,
    onSave: (Activity) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var effort by remember { mutableStateOf(existing?.effortType ?: EffortType.PHYSICAL) }
    var duration by remember { mutableStateOf(existing?.durationMinutes ?: 30) }
    var place by remember { mutableStateOf(existing?.place ?: Place.HOME) }
    var energy by remember { mutableStateOf(existing?.energyCost ?: EnergyCost.MED) }
    var daysMask by remember { mutableStateOf(existing?.daysMask ?: 0b1111111) }
    var winStart by remember { mutableStateOf(existing?.windowStartMin) }
    var winEnd by remember { mutableStateOf(existing?.windowEndMin) }
    var freq by remember { mutableStateOf(existing?.frequencyTarget) }
    var active by remember { mutableStateOf(existing?.isActive ?: true) }
    var more by remember { mutableStateOf(false) }

    fun assemble() = (existing ?: Activity(name = "", effortType = effort)).copy(
        name = name.trim(), effortType = effort, durationMinutes = duration, place = place,
        energyCost = energy, daysMask = daysMask, windowStartMin = winStart, windowEndMin = winEnd,
        frequencyTarget = freq, isActive = active
    )

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)
    ) {
        Text(if (existing == null) "Nowa aktywność" else "Aktywność", fontSize = 20.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            placeholder = { Text("np. Trening w domu, Czytanie, Duolingo") },
            singleLine = true, shape = RoundedCornerShape(16.dp), colors = glassFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Section("Typ wysiłku")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EffortType.entries.forEach { t ->
                Chip("${t.emoji} ${t.label}", effort == t, GlassAccent) { effort = t }
            }
        }

        Section("Czas trwania")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 45, 60, 90).forEach { m ->
                Chip(if (m >= 60) "${m / 60}h${if (m % 60 != 0) " ${m % 60}m" else ""}" else "$m min", duration == m, GlassAccent) { duration = m }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().bouncy(0.99f) { more = !more }, verticalAlignment = Alignment.CenterVertically) {
            Text(if (more) "Mniej ▲" else "Więcej ▼", fontSize = 12.5.sp, fontWeight = FontWeight.W800, color = GlassAccent)
        }

        if (more) {
            Section("Miejsce")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Place.entries.forEach { p -> Chip(p.label, place == p, GlassAccent) { place = p } }
            }

            Section("Dni tygodnia")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("P", "W", "Ś", "C", "P", "S", "N").forEachIndexed { i, d ->
                    val bit = dayBit(i + 1)
                    val on = daysMask and bit != 0
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (on) GlassAccent else GlassTint)
                            .bouncy(0.85f) { daysMask = daysMask xor bit },
                        contentAlignment = Alignment.Center
                    ) { Text(d, fontSize = 12.sp, fontWeight = FontWeight.W800, color = if (on) Color.White else GlassTextSecondary) }
                }
            }

            Section("Okno godzin")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HourPicker("Od", winStart) { winStart = it }
                HourPicker("Do", winEnd) { winEnd = it }
                if (winStart != null || winEnd != null) Chip("wyczyść", false, GlassAccent) { winStart = null; winEnd = null }
            }

            Section("Koszt energii")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnergyCost.entries.forEach { e -> Chip(e.label, energy == e, GlassAccent) { energy = e } }
            }

            Section("Cel częstotliwości")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("brak", freq == null, GlassAccent) { freq = null }
                listOf(1, 2, 3, 4, 5).forEach { n -> Chip("${n}×/tydz", freq == n, GlassAccent) { freq = n } }
            }

            Section("Aktywna")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (active) "Aktywna" else "Wstrzymana", fontSize = 13.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                Switch(checked = active, onCheckedChange = { active = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = GlassAccent))
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (existing != null && onDelete != null) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(GlassTint).bouncy(0.97f, onDelete)
                        .padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center
                ) { Text("Usuń", color = Color(0xFFD1453B), fontWeight = FontWeight.W700) }
            }
            Row(
                Modifier.weight(2f).clip(RoundedCornerShape(16.dp))
                    .background(if (name.isBlank()) GlassTint else GlassAccent)
                    .bouncy(0.97f) { if (name.isNotBlank()) { onSave(assemble()); onClose() } }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) { Text("Zapisz", color = if (name.isBlank()) GlassTextSecondary else Color.White, fontWeight = FontWeight.W700) }
        }
    }
}

@Composable
private fun HourPicker(label: String, value: Int?, onPick: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip("$label: ${value?.let { hm(it) } ?: "--"}", value != null, GlassAccent) { open = true }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (6..23).forEach { h ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("%02d:00".format(h)) },
                    onClick = { onPick(h * 60); open = false }
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Chip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) color.copy(alpha = 0.18f) else GlassTint)
            .bouncy(0.9f, onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = if (selected) color else GlassTextSecondary)
    }
}
