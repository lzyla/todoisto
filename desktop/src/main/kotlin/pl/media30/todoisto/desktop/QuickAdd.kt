package pl.media30.todoisto.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.DesktopBackgrounds
import pl.media30.todoisto.data.QuickAddParser
import java.io.File
import java.time.LocalDate

/**
 * Panel „Nowe zadanie" (jak `QuickAddMorph` w Androidzie): pole z parserem języka
 * naturalnego, chipy rozpoznanych tokenów, skróty, skan z pliku, Anuluj / Dodaj.
 * Enter dodaje zadanie. Otwierany z FAB-a, ⌘N, „Mikrofon" i menu Plik.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BoxScope.QuickAddPanel(st: AppState) {
    val g = LocalGlass.current
    val focus = remember { FocusRequester() }
    var showDatePicker by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    fun close() { st.quickAddOpen = false; st.voiceHint = false; showDatePicker = false; st.qaReset() }
    fun submit() { if (st.quickAddText.isNotBlank()) { st.quickAdd(st.quickAddText, st.qaOverrides()); st.quickAddText = ""; close() } }
    fun insert(token: String) { st.quickAddText = (st.quickAddText.trimEnd() + " " + token).trimStart() }

    // Przyciemnienie zamyka.
    Box(Modifier.fillMaxSize().background(Color(0x4D1C0A42)).clickable(indication = null, interactionSource = null) { close() })
    Column(
        Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).widthIn(max = 640.dp).fillMaxWidth(0.62f).heightIn(max = 520.dp)
            .glass(RoundedCornerShape(29.dp), strong = true, elevation = 26.dp).livingGradient(RoundedCornerShape(29.dp))
            .clickable(indication = null, interactionSource = null) { }
            .padding(18.dp).verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nowe zadanie", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.weight(1f))
            Box(Modifier.size(30.dp).clip(CircleShape).background(g.accent.copy(alpha = 0.12f)).clickable { close() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Zamknij", tint = g.accent, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(g.field).border(1.dp, g.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (st.quickAddText.isEmpty()) Text("np. Raport jutro o 15:00 #Praca @pilne p1", fontSize = 15.sp, color = g.textSecondary.copy(alpha = 0.7f), fontFamily = Manrope)
            BasicTextField(
                value = st.quickAddText, onValueChange = { st.quickAddText = it }, singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = g.textPrimary, fontFamily = Manrope, fontWeight = FontWeight.W500),
                cursorBrush = SolidColor(g.accent),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && (e.key == Key.Enter || e.key == Key.NumPadEnter)) { submit(); true } else false
                }
            )
        }
        if (st.voiceHint) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(g.accent.copy(alpha = 0.10f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mic, null, tint = g.accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                Text("Dyktowanie: kursor jest w polu — naciśnij dwa razy klawisz 🎤 (lub fn) i mów. macOS wpisze tekst, Enter doda zadanie.", fontSize = 12.sp, color = g.textPrimary, fontFamily = Manrope)
            }
        }
        Spacer(Modifier.height(10.dp))
        // Rozpoznane tokeny
        val parsed = remember(st.quickAddText) { if (st.quickAddText.isBlank()) null else QuickAddParser().parse(st.quickAddText) }
        if (parsed == null) Text("Rozpoznam datę, godzinę, #projekt, @etykietę, priorytet i cykl.", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
        else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val today = st.today
            if (st.qaDue == null) parsed.dueDate?.let { QAChip(fmtDay(it, today), g.success) }
            parsed.dueTimeMinutes?.let { QAChip(hm(it), g.accent) }
            parsed.recurrence?.let { QAChip("⟳ ${it.label.lowercase()}", g.textSecondary) }
            if (st.qaPriority == null && parsed.priority.name != "P4") QAChip(parsed.priority.name, priorityColorRaw(parsed.priority.name))
            if (st.qaProject == null) parsed.projectName?.let { QAChip("#$it", Color(0xFF4D6BFF)) }
            parsed.labelNames.forEach { QAChip("@$it", Color(0xFFC24DFF)) }
            parsed.durationMinutes?.let { QAChip("$it min", g.textSecondary) }
            if (parsed.title.isNotBlank()) Text("→ „${parsed.title}”", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.height(12.dp))
        // Pasek jak w Todoist: wybór z listy ZASTĘPUJE poprzedni (bez dublowania p1 + p2).
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(st.qaDue?.let { fmtDay(it, st.today) } ?: "Termin", selected = st.qaDue != null || picker == "due", color = g.success, icon = Icons.Outlined.CalendarToday) { picker = if (picker == "due") null else "due" }
            Chip(st.qaPriority ?: "Priorytet", selected = st.qaPriority != null || picker == "prio", color = st.qaPriority?.let { priorityColorRaw(it) }) { picker = if (picker == "prio") null else "prio" }
            Chip(st.qaProject?.let { id -> st.repo.project(id)?.name?.let { "#$it" } } ?: "Projekt", selected = st.qaProject != null || picker == "proj", color = Color(0xFF4D6BFF)) { picker = if (picker == "proj") null else "proj" }
            Chip(if (st.qaLabels.isEmpty()) "Etykiety" else st.qaLabels.mapNotNull { st.repo.label(it)?.name }.joinToString(" ") { "@$it" }, selected = st.qaLabels.isNotEmpty() || picker == "lab", color = Color(0xFFC24DFF)) { picker = if (picker == "lab") null else "lab" }
            Chip("o 15:00") { insert("o 15:00") }
            Chip("codziennie", color = Color(0xFF2E9C4F)) { insert("codziennie") }
            Chip("co tydzień", color = Color(0xFF2E9C4F)) { insert("co tydzień") }
        }
        picker?.let { kind ->
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (kind) {
                    "due" -> {
                        Chip("Dzisiaj", selected = st.qaDue == st.today) { st.qaDue = st.today; picker = null }
                        Chip("Jutro", selected = st.qaDue == st.today + 1) { st.qaDue = st.today + 1; picker = null }
                        Chip("Weekend", selected = st.qaDue == nextWeekend(st.today)) { st.qaDue = nextWeekend(st.today); picker = null }
                        Chip("Dzień…", icon = Icons.Outlined.CalendarToday, selected = showDatePicker) { showDatePicker = !showDatePicker }
                        if (st.qaDue != null) Chip("Brak daty", color = g.textSecondary) { st.qaDue = null; picker = null; showDatePicker = false }
                    }
                    "prio" -> {
                        listOf("P1", "P2", "P3", "P4").forEach { pr -> Chip(pr, selected = st.qaPriority == pr, color = priorityColorRaw(pr)) { st.qaPriority = pr; picker = null } }
                        if (st.qaPriority != null) Chip("Wg tekstu", color = g.textSecondary) { st.qaPriority = null; picker = null }
                    }
                    "proj" -> {
                        Chip("Skrzynka", selected = st.qaProject == null, color = g.textSecondary) { st.qaProject = null; picker = null }
                        st.repo.projects.filter { !it.isArchived }.forEach { p -> Chip("#${p.name}", selected = st.qaProject == p.id, color = Color(p.colorArgb)) { st.qaProject = p.id; picker = null } }
                    }
                    else -> {
                        if (st.repo.labels.isEmpty()) Text("Brak etykiet — wpisz @nazwa w tekście.", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
                        st.repo.labels.forEach { l ->
                            val on = l.id in st.qaLabels
                            Chip("@${l.name}", selected = on, color = Color(l.colorArgb)) { st.qaLabels = if (on) st.qaLabels - l.id else st.qaLabels + l.id }
                        }
                    }
                }
            }
        }
        if (showDatePicker) {
            val picker2 = rememberDatePickerState(initialSelectedDateMillis = (st.qaDue ?: st.today) * 86_400_000L)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (g.dark) Color.White.copy(alpha = 0.06f) else Color.White)) {
                DatePicker(state = picker2, title = null, headline = null, showModeToggle = false,
                    colors = DatePickerDefaults.colors(containerColor = Color.Transparent, selectedDayContainerColor = g.accent, todayDateBorderColor = g.accent, todayContentColor = g.accent))
            }
            SecondaryBtn("Ustaw datę", Modifier.fillMaxWidth()) {
                picker2.selectedDateMillis?.let { st.qaDue = Math.floorDiv(it, 86_400_000L) }
                showDatePicker = false; picker = null
            }
        }
        Spacer(Modifier.height(14.dp))
        SectionHeader("Skan z kartki", g.textSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Zdjęcie z pliku…", icon = Icons.Outlined.Image) { pickImageFile()?.let { f -> DesktopBackgrounds.scaledJpeg(f)?.let { st.scanImage(it); close() } } }
            Chip("Zrób zdjęcie", icon = Icons.Outlined.PhotoCamera) { st.toast = "Na Macu: zrób zdjęcie telefonem (AirDrop/iCloud) i wybierz plik." }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("Anuluj", fontSize = 13.5.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { close() }.padding(horizontal = 14.dp, vertical = 10.dp))
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(g.accent.copy(alpha = if (st.quickAddText.isBlank()) 0.45f else 1f)).clickable { submit() }.padding(horizontal = 22.dp, vertical = 10.dp)
            ) { Text("Dodaj  ⏎", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = Color.White, fontFamily = Manrope) }
        }
    }
}

@Composable
private fun QAChip(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp)) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = color, fontFamily = Manrope)
    }
}

/** Natywne okno wyboru pliku obrazu (AWT — działa z Compose Desktop). */
fun pickImageFile(): File? {
    val fd = java.awt.FileDialog(null as java.awt.Frame?, "Wybierz zdjęcie", java.awt.FileDialog.LOAD)
    fd.setFilenameFilter { _, name -> name.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp") || it.endsWith(".heic") } }
    fd.isVisible = true
    val dir = fd.directory ?: return null; val file = fd.file ?: return null
    return File(dir, file).takeIf { it.exists() }
}

/** Wybór źródła skanu (jak `ScanChooserSheet`). */
@Composable
fun ScanChooserSheet(st: AppState) {
    val g = LocalGlass.current
    Sheet(onDismiss = { st.showScanChooser = false }, maxWidth = 440.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(g.accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Text("Skanuj kartkę", fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScanTile("Wybierz z pliku", "Zdjęcie listy lub notatki", Icons.Outlined.Image, Color(0xFFEB8909), Modifier.weight(1f)) {
                st.showScanChooser = false
                pickImageFile()?.let { f -> DesktopBackgrounds.scaledJpeg(f)?.let { st.scanImage(it) } ?: run { st.toast = "Nie udało się odczytać obrazu." } }
            }
            ScanTile("Zrób zdjęcie", "Telefonem → plik", Icons.Outlined.PhotoCamera, g.accent, Modifier.weight(1f)) {
                st.showScanChooser = false; st.toast = "Na Macu zrób zdjęcie telefonem i wybierz je z pliku (AirDrop / iCloud)."
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("AI wyciągnie zadania ze zdjęcia listy lub notatki.", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}

@Composable
private fun ScanTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.12f)).clickable { onClick() }.padding(16.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
        Text(sub, fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}
