package pl.media30.todoisto.desktop

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.shared.CloudTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Szczegóły zadania 1:1 z `TaskDetailSheet` w Androidzie: bąbel AI, nagłówek z
 * kółkiem i edytowalnym tytułem/notatkami + Duplikuj, PRIORYTET, zwijane sekcje
 * (Projekt, Termin, Deadline, Powtarzanie, Czas trwania, Przypomnienie, Etykiety),
 * ZAŁĄCZNIKI I LINKI, PODZADANIA, stopka Usuń / Gotowe.
 * Każda zmiana zapisuje się natychmiast (`onPatch` → `updateTask`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(st: AppState, task: CloudTask) {
    val g = LocalGlass.current
    val today = st.today
    fun patch(t: CloudTask) = st.updateTask(t)
    var expanded by remember { mutableStateOf(st.detailAiExpanded) }
    var datePickerFor by remember { mutableStateOf<String?>(null) } // "due" | "deadline"
    var attachment by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    val done = task.isCompleted

    Sheet(onDismiss = { st.detailTaskId = null; st.dismissAi() }, maxWidth = 620.dp) {
        AutomationCard(st, task, expanded) { expanded = true }
        Spacer(Modifier.height(18.dp))

        // Nagłówek: kółko, tytuł (edytowalny), notatki, duplikuj
        Row(verticalAlignment = Alignment.Top) {
            GlassCheck(done, priorityColor(task.priority), 27.dp, emphasize = task.priority != "P4" && !done) { st.toggle(task) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicTextField(
                    value = task.title, onValueChange = { if (it.isNotBlank()) patch(task.copy(title = it)) },
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800, color = if (done) g.textSecondary else g.textPrimary, fontFamily = Sora, textDecoration = if (done) TextDecoration.LineThrough else null),
                    cursorBrush = SolidColor(g.accent), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Box {
                    if (task.notes.isEmpty()) Text("Notatki…", fontSize = 13.sp, color = g.textSecondary.copy(alpha = 0.7f), fontFamily = Manrope)
                    BasicTextField(
                        value = task.notes, onValueChange = { patch(task.copy(notes = it)) },
                        textStyle = TextStyle(fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope, lineHeight = 18.sp),
                        cursorBrush = SolidColor(g.accent), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(34.dp).clip(CircleShape).background(g.accent.copy(alpha = 0.12f)).clickable { st.duplicateTask(task.id); st.detailTaskId = null }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ContentCopy, "Duplikuj", tint = g.accent, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        // PRIORYTET
        Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(priorityColor(task.priority), 7.dp); Spacer(Modifier.width(10.dp))
            Text("PRIORYTET", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
            listOf("P1", "P2", "P3", "P4").forEach { p ->
                val c = priorityColorRaw(p); val sel = task.priority == p
                Box(Modifier.padding(start = 10.dp).size(26.dp).clip(CircleShape).border(2.5.dp, c, CircleShape).background(if (sel) c.copy(alpha = 0.18f) else Color.Transparent).clickable { patch(task.copy(priority = p)) }, contentAlignment = Alignment.Center) {
                    if (sel) Box(Modifier.size(13.dp).clip(CircleShape).background(c))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Zwijane sekcje
        val projects = st.repo.projects.filter { !it.isArchived }
        DetailSection("Projekt", Color(0xFF4D6BFF), st.repo.projectName(task.projectId)) {
            Chip("Skrzynka", task.projectId == null, Color(0xFF9E9E9E)) { patch(task.copy(projectId = null, sectionId = null)) }
            projects.forEach { p -> Chip(p.name, task.projectId == p.id, Color(p.colorArgb)) { patch(task.copy(projectId = p.id, sectionId = null)) } }
        }
        val termSummary = fmtDay(task.dueDate, today) + (task.dueTimeMinutes?.let { " ${hm(it)}" } ?: "")
        DetailSection("Termin", g.accent, termSummary) {
            Chip("Dzisiaj", task.dueDate == today, icon = Icons.Outlined.CalendarToday) { patch(task.copy(dueDate = today)) }
            Chip("Jutro", task.dueDate == today + 1, icon = Icons.Outlined.CalendarToday) { patch(task.copy(dueDate = today + 1)) }
            Chip("Za 3 dni", task.dueDate == today + 3, icon = Icons.Outlined.CalendarToday) { patch(task.copy(dueDate = today + 3)) }
            Chip("Przyszły tydz.", task.dueDate == today + 7, icon = Icons.Outlined.CalendarToday) { patch(task.copy(dueDate = today + 7)) }
            Chip("Bez terminu", task.dueDate == null) { patch(task.copy(dueDate = null, recurrence = null, dueTimeMinutes = null, reminderAt = null)) }
            Chip("Data…", datePickerFor == "due") { datePickerFor = if (datePickerFor == "due") null else "due" }
            // Godzina
            listOf(8 * 60, 9 * 60, 12 * 60, 15 * 60, 18 * 60).forEach { m -> Chip(hm(m), task.dueTimeMinutes == m, g.textSecondary) { patch(task.copy(dueTimeMinutes = m, dueDate = task.dueDate ?: today)) } }
            if (task.dueTimeMinutes != null) Chip("Bez godziny", false, g.textSecondary) { patch(task.copy(dueTimeMinutes = null)) }
        }
        if (datePickerFor != null) InlineDatePicker(initial = (if (datePickerFor == "due") task.dueDate else task.deadline) ?: today) { day ->
            patch(if (datePickerFor == "due") task.copy(dueDate = day) else task.copy(deadline = day)); datePickerFor = null
        }
        val friday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).toEpochDay()
        val monthEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
        DetailSection("Deadline", Color(0xFFC24B1A), fmtDay(task.deadline, today)) {
            Chip("Brak", task.deadline == null) { patch(task.copy(deadline = null)) }
            Chip("Do jutra", task.deadline == today + 1) { patch(task.copy(deadline = today + 1)) }
            Chip("Do piątku", task.deadline == friday) { patch(task.copy(deadline = friday)) }
            Chip("Koniec mies.", task.deadline == monthEnd) { patch(task.copy(deadline = monthEnd)) }
            Chip("Data…", datePickerFor == "deadline") { datePickerFor = if (datePickerFor == "deadline") null else "deadline" }
        }
        DetailSection("Powtarzanie", Color(0xFF2DD4BF), task.recurrence?.let { Recurrence.fromNameSafe(it)?.label } ?: "Nie") {
            Chip("Nie", task.recurrence == null) { patch(task.copy(recurrence = null)) }
            Recurrence.entries.forEach { r -> Chip(r.label, task.recurrence == r.name) { patch(task.copy(recurrence = r.name, dueDate = task.dueDate ?: today)) } }
        }
        DetailSection("Czas trwania", Color(0xFF34D399), fmtDuration(task.durationMinutes)) {
            Chip("Brak", task.durationMinutes == null) { patch(task.copy(durationMinutes = null)) }
            listOf(15, 30, 45, 60, 120).forEach { m -> Chip(fmtDuration(m), task.durationMinutes == m) { patch(task.copy(durationMinutes = m)) } }
        }
        val remSummary = task.reminderAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalTime().let { t -> "%d:%02d".format(t.hour, t.minute) } } ?: "Brak"
        DetailSection("Przypomnienie", Color(0xFFEB8909), remSummary) {
            val zone = java.time.ZoneId.systemDefault()
            fun atDue(offsetMin: Int): Long? = task.dueDate?.let { d -> task.dueTimeMinutes?.let { m -> LocalDate.ofEpochDay(d).atStartOfDay(zone).plusMinutes((m - offsetMin).toLong()).toInstant().toEpochMilli() } }
            Chip("Brak", task.reminderAt == null) { patch(task.copy(reminderAt = null)) }
            if (task.dueDate != null && task.dueTimeMinutes != null) {
                Chip("O czasie", task.reminderAt == atDue(0)) { patch(task.copy(reminderAt = atDue(0))) }
                Chip("15 min przed", task.reminderAt == atDue(15)) { patch(task.copy(reminderAt = atDue(15))) }
                Chip("1 godz. przed", task.reminderAt == atDue(60)) { patch(task.copy(reminderAt = atDue(60))) }
            } else if (task.dueDate != null) {
                val morning = LocalDate.ofEpochDay(task.dueDate!!).atStartOfDay(zone).plusHours(8).toInstant().toEpochMilli()
                Chip("Rano 8:00", task.reminderAt == morning) { patch(task.copy(reminderAt = morning)) }
            }
            Chip("Za godzinę", false) { patch(task.copy(reminderAt = System.currentTimeMillis() + 3_600_000)) }
        }
        val labels = st.repo.labels
        if (labels.isNotEmpty()) DetailSection("Etykiety", Color(0xFFC24DFF), if (task.labelIds.isEmpty()) "Brak" else task.labelIds.joinToString(", ") { "@" + st.repo.labelName(it) }) {
            labels.forEach { l -> Chip("@${l.name}", l.id in task.labelIds, Color(l.colorArgb)) { patch(task.copy(labelIds = if (l.id in task.labelIds) task.labelIds - l.id else task.labelIds + l.id)) } }
        }
        Text("Przypomnienie w miejscu (GPS) ustawisz na telefonie — Mac nie ma lokalizacji.", fontSize = 11.sp, color = g.textSecondary.copy(alpha = 0.8f), fontFamily = Manrope, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
        Spacer(Modifier.height(10.dp))

        // ZAŁĄCZNIKI I LINKI
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Dot(Color(0xFF4D6BFF), 7.dp); Spacer(Modifier.width(10.dp)); Text("ZAŁĄCZNIKI I LINKI", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = g.textPrimary, fontFamily = Manrope) }
            if (task.attachments.isNotEmpty()) Spacer(Modifier.height(8.dp))
            task.attachments.forEach { url ->
                val host = runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: url
                val name = url.substringAfterLast('/').ifBlank { host }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(g.field).clickable { openUrl(url) }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(listOf(0xFF24292F, 0xFFA259FF, 0xFF246FE0, 0xFF12A150, 0xFFB33B00, 0xFF6B3FE0)[Math.abs(url.hashCode()) % 6])), contentAlignment = Alignment.Center) {
                        Text(host.first().uppercase(), color = Color.White, fontWeight = FontWeight.W800, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(host, fontSize = 11.sp, color = g.textSecondary, fontFamily = Manrope)
                    }
                    Icon(Icons.Outlined.OpenInNew, "Otwórz", tint = g.textSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Close, "Usuń", tint = g.textSecondary, modifier = Modifier.size(15.dp).clickable { patch(task.copy(attachments = task.attachments - url)) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassField(attachment, { attachment = it }, "Wklej link lub adres zdjęcia…", Modifier.weight(1f), minHeight = 40.dp, textSize = 13, onEnter = { addAttachment(attachment, task, ::patch); attachment = "" })
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(30.dp).clip(CircleShape).background(g.accent).clickable { addAttachment(attachment, task, ::patch); attachment = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Dodaj", tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.height(10.dp))

        // PODZADANIA
        if (task.parentId == null) {
            st.rev
            val subs = st.repo.subtasks(task.id)
            val doneSubs = subs.count { it.isCompleted }
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(Color(0xFFA47CFF), 7.dp); Spacer(Modifier.width(10.dp))
                    Text("PODZADANIA", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
                    if (subs.isNotEmpty()) Text("$doneSubs/${subs.size}", fontSize = 12.sp, fontWeight = FontWeight.W800, color = g.textSecondary, fontFamily = Manrope)
                }
                if (subs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(g.hair)) {
                        Box(Modifier.fillMaxWidth(doneSubs.toFloat() / subs.size).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFA47CFF), g.accent))))
                    }
                }
                subs.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassCheck(s.isCompleted, g.accent, 19.dp) { st.toggle(s) }
                        Spacer(Modifier.width(10.dp))
                        Text(s.title, fontSize = 13.5.sp, color = if (s.isCompleted) g.textSecondary else g.textPrimary, textDecoration = if (s.isCompleted) TextDecoration.LineThrough else null, fontFamily = Manrope, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Close, "Usuń", tint = g.textSecondary, modifier = Modifier.size(14.dp).clickable { st.deleteTask(s.id) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassField(subtitle, { subtitle = it }, "Dodaj podzadanie…", Modifier.weight(1f), minHeight = 40.dp, textSize = 13, onEnter = { st.addSubtask(task.id, task.projectId, subtitle); subtitle = "" })
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(26.dp).clip(CircleShape).background(g.accent).clickable { st.addSubtask(task.id, task.projectId, subtitle); subtitle = "" }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, "Dodaj", tint = Color.White, modifier = Modifier.size(14.dp)) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Stopka
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.5.dp, g.danger.copy(alpha = 0.45f), RoundedCornerShape(50)).clickable { st.deleteTask(task.id); st.detailTaskId = null }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Text("Usuń", fontSize = 14.sp, fontWeight = FontWeight.W800, color = g.danger, fontFamily = Manrope)
            }
            Box(Modifier.weight(2f).clip(RoundedCornerShape(50)).background(g.accent).clickable { st.detailTaskId = null; st.dismissAi() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                Text("Gotowe", fontSize = 14.sp, fontWeight = FontWeight.W800, color = Color.White, fontFamily = Manrope)
            }
        }
    }
}

private fun addAttachment(raw: String, task: CloudTask, patch: (CloudTask) -> Unit) {
    val u = raw.trim(); if (u.isBlank()) return
    val url = if (u.startsWith("http://") || u.startsWith("https://")) u else "https://$u"
    patch(task.copy(attachments = task.attachments + url))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineDatePicker(initial: Long, onPick: (Long) -> Unit) {
    val g = LocalGlass.current
    val picker = rememberDatePickerState(initialSelectedDateMillis = initial * 86_400_000L)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(18.dp)).background(if (g.dark) Color.White.copy(alpha = 0.06f) else Color.White).padding(6.dp)) {
        DatePicker(state = picker, title = null, headline = null, showModeToggle = false,
            colors = DatePickerDefaults.colors(containerColor = Color.Transparent, selectedDayContainerColor = g.accent, todayDateBorderColor = g.accent, todayContentColor = g.accent))
        PrimaryBtn("OK") { picker.selectedDateMillis?.let { onPick(Math.floorDiv(it, 86_400_000L)) } }
    }
}

/** Zwijany wiersz wartości (jak `DetailSection`): kropka, tytuł, podsumowanie w kolorze, chevron. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DetailSection(title: String, accent: Color, summary: String, defaultExpanded: Boolean = false, content: @Composable () -> Unit) {
    val g = LocalGlass.current
    var open by remember(title) { mutableStateOf(defaultExpanded) }
    val rot by animateFloatAsState(if (open) 180f else 0f)
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(RoundedCornerShape(22.dp)).animateContentSize()) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(accent, 7.dp); Spacer(Modifier.width(10.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
            if (!open && summary.isNotBlank()) Text(summary, fontSize = 13.sp, fontWeight = FontWeight.W600, color = accent, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = g.textSecondary, modifier = Modifier.size(18.dp).rotate(rot))
        }
        if (open) FlowRow(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

// ─── Dialogi: Nowy obszar / projekt / etykieta / sekcja, Cele, Klucz AI ─────
@Composable
fun AppDialog(st: AppState, kind: DialogKind) {
    val g = LocalGlass.current
    when (kind) {
        DialogKind.NewArea -> NameColorDialog("Nowy obszar", "Nazwa obszaru", true, { st.dialog = null }) { n, c -> st.addArea(n, c); st.dialog = null }
        DialogKind.NewProject -> NameColorDialog("Nowy projekt", "Nazwa projektu", true, { st.dialog = null }) { n, c -> st.addProject(n, c); st.dialog = null }
        DialogKind.NewLabel -> NameColorDialog("Nowa etykieta", "Nazwa etykiety", true, { st.dialog = null }) { n, c -> st.addLabel(n, c); st.dialog = null }
        is DialogKind.NewSection -> NameColorDialog("Nowa sekcja", "Nazwa sekcji", false, { st.dialog = null }) { n, _ -> st.addSection(kind.projectId, n); st.dialog = null }
        DialogKind.Goals -> {
            var daily by remember { mutableStateOf(st.settings.dailyGoal.toString()) }
            var weekly by remember { mutableStateOf(st.settings.weeklyGoal.toString()) }
            Sheet(onDismiss = { st.dialog = null }, maxWidth = 400.dp) {
                Text("Cele produktywności", fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
                Text("Ile zadań chcesz domykać?", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                Spacer(Modifier.height(12.dp))
                Text("Dziennie", fontSize = 12.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope)
                GlassField(daily, { daily = it.filter(Char::isDigit).take(2) }, "5")
                Spacer(Modifier.height(8.dp))
                Text("Tygodniowo", fontSize = 12.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope)
                GlassField(weekly, { weekly = it.filter(Char::isDigit).take(3) }, "25")
                Spacer(Modifier.height(14.dp))
                PrimaryBtn("Zapisz cele") { st.setGoals(daily.toIntOrNull() ?: 5, weekly.toIntOrNull() ?: 25); st.dialog = null }
                DialogCancel { st.dialog = null }
            }
        }
        DialogKind.ApiKey -> KeyDialog(
            title = "Klucz AI (OpenAI)", hasKey = st.settings.openAiKey.isNotBlank(), placeholder = "sk-…",
            body = "Klucz utworzysz na platform.openai.com w sekcji API keys. Zostaje tylko na tym komputerze.",
            onSave = { st.settings.openAiKey = it; st.settingsChanged(); st.dialog = null }, onClose = { st.dialog = null }
        )
        DialogKind.AdminKey -> KeyDialog(
            title = "Klucz Admin (koszty)", hasKey = st.settings.openAiAdminKey.isNotBlank(), placeholder = "sk-admin-…",
            body = "Utwórz na platform.openai.com → Settings → Organization → Admin keys. Ma szersze uprawnienia niż zwykły klucz — zostaje tylko na tym komputerze.",
            onSave = { st.settings.openAiAdminKey = it; st.settingsChanged(); st.dialog = null; if (it.isBlank()) st.aiCost = null else st.refreshAiCost() }, onClose = { st.dialog = null }
        )
    }
}

@Composable
private fun NameColorDialog(title: String, placeholder: String, withColor: Boolean, onClose: () -> Unit, onCreate: (String, Long) -> Unit) {
    val g = LocalGlass.current
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PaletteColors[0]) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Sheet(onDismiss = onClose, maxWidth = 420.dp) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Spacer(Modifier.height(12.dp))
        GlassField(name, { name = it }, placeholder, focus = focus, onEnter = { if (name.isNotBlank()) onCreate(name, color) })
        if (withColor) {
            Spacer(Modifier.height(14.dp)); SectionHeader("Kolor", g.textSecondary); Spacer(Modifier.height(8.dp))
            ColorPicker(color) { color = it }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryBtn("Utwórz", enabled = name.isNotBlank()) { onCreate(name, color) }
        DialogCancel(onClose)
    }
}

@Composable
private fun KeyDialog(title: String, hasKey: Boolean, placeholder: String, body: String, onSave: (String) -> Unit, onClose: () -> Unit) {
    val g = LocalGlass.current
    var key by remember { mutableStateOf("") }
    Sheet(onDismiss = onClose, maxWidth = 460.dp) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Spacer(Modifier.height(8.dp))
        Text((if (hasKey) "Klucz jest ustawiony. Wklej nowy, aby zmienić.\n\n" else "") + body, fontSize = 13.sp, lineHeight = 18.sp, color = g.textSecondary, fontFamily = Manrope)
        Spacer(Modifier.height(12.dp))
        GlassField(key, { key = it }, placeholder, password = true)
        Spacer(Modifier.height(14.dp))
        PrimaryBtn("Zapisz", enabled = key.isNotBlank()) { onSave(key.trim()) }
        if (hasKey) { Spacer(Modifier.height(6.dp)); SecondaryBtn("Usuń", Modifier.fillMaxWidth(), g.danger) { onSave("") } }
        DialogCancel(onClose)
    }
}

@Composable
fun DialogCancel(onClose: () -> Unit) {
    val g = LocalGlass.current
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Text("Anuluj", fontSize = 13.5.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClose() }.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}
