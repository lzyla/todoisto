package pl.media30.todoisto.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.AutomationAdvisor
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.shared.CloudTask
import java.time.LocalDate

/**
 * Lista zadań 1:1 z `ZenContent` w Androidzie: nagłówek widoku, sekcje ZALEGŁE /
 * DZISIAJ (zwijane) z kubełkami Rano / Po południu / Wieczorem, Nadchodzące
 * pogrupowane po dniach, sekcje projektu, puste stany.
 */
@Composable
fun TaskListContent(st: AppState) {
    val g = LocalGlass.current
    val ui = remember(st.rev, st.view, st.sort, st.activeArea, st.settingsRev) { st.buildUiState() }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    val today = st.today
    val nodes = ui.groups.flatMap { it.nodes }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 170.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (ui.view) {
            AppView.Today -> {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp)) {
                        Text("Dziś", fontSize = 30.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, letterSpacing = (-0.6).sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = g.textSecondary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${nodes.size} ${plural(nodes.size, "zadanie", "zadania", "zadań")}  ·  ${dateCaption()}", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                        }
                    }
                }
                val overdue = nodes.filter { it.task.dueDate != null && it.task.dueDate!! < today }
                val todayNodes = nodes.filter { it !in overdue }
                if (nodes.isEmpty()) {
                    item {
                        if (ui.routines.isNotEmpty()) EmptyState("Zostały tylko rutyny", "Główna lista pusta — sprawdź rutyny przyciskiem ⟳ na dole.")
                        else EmptyState("Wszystko zrobione", "Dodaj kolejne zadanie plusem w rogu.")
                    }
                }
                if (overdue.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SectionToggle("ZALEGŁE", overdue.size, g.overdue, collapsed["overdue"] != true, Modifier.weight(1f)) { collapsed["overdue"] = !(collapsed["overdue"] ?: false) }
                            Text(
                                "Zmień termin → dziś", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = g.overdue, fontFamily = Manrope,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(g.overdue.copy(alpha = 0.10f)).clickable { st.moveOverdueToToday() }.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (collapsed["overdue"] != true) items(overdue, key = { "o${it.task.id}" }) { n -> TaskTile(st, n, showDue = true, showProject = true) }
                }
                if (todayNodes.isNotEmpty()) {
                    item { SectionToggle("DZISIAJ", todayNodes.size, g.textSecondary, collapsed["today"] != true) { collapsed["today"] = !(collapsed["today"] ?: false) } }
                    if (collapsed["today"] != true) {
                        val buckets = listOf("Rano", "Po południu", "Wieczorem")
                        fun bucketOf(t: CloudTask) = when { t.dueTimeMinutes == null || t.dueTimeMinutes!! < 12 * 60 -> "Rano"; t.dueTimeMinutes!! < 17 * 60 -> "Po południu"; else -> "Wieczorem" }
                        buckets.forEach { b ->
                            val inBucket = todayNodes.filter { bucketOf(it.task) == b }
                            if (inBucket.isNotEmpty()) {
                                item { BucketToggle(b, collapsed[b] != true) { collapsed[b] = !(collapsed[b] ?: false) } }
                                if (collapsed[b] != true) items(inBucket, key = { "t${it.task.id}" }) { n -> TaskTile(st, n, showDue = false, showProject = true) }
                            }
                        }
                    }
                }
            }
            AppView.Upcoming -> {
                item {
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Text("najbliższe 7 dni", fontSize = 13.sp, fontWeight = FontWeight.W600, color = g.textSecondary, fontFamily = Manrope)
                        Text("Nadchodzące", fontSize = 30.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Sora, letterSpacing = (-0.6).sp)
                    }
                }
                val todayDate = LocalDate.now()
                for (off in 1..7) {
                    val d = todayDate.plusDays(off.toLong()); val epoch = d.toEpochDay()
                    val dayNodes = nodes.filter { it.task.dueDate == epoch }
                    item {
                        Row(Modifier.padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
                            Text(if (off == 1) "Jutro" else WEEKDAYS[d.dayOfWeek.value - 1].replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
                            Spacer(Modifier.width(8.dp))
                            Text("${d.dayOfMonth} ${MS_SHORT[d.monthValue - 1]}", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
                        }
                    }
                    if (dayNodes.isEmpty()) item { Text("Nic zaplanowanego", fontSize = 12.5.sp, color = g.textSecondary.copy(alpha = 0.7f), fontFamily = Manrope, modifier = Modifier.padding(start = 4.dp)) }
                    else items(dayNodes, key = { "u${it.task.id}" }) { n -> TaskTile(st, n, showDue = false, showProject = true) }
                }
                val later = nodes.filter { it.task.dueDate != null && it.task.dueDate!! > todayDate.plusDays(7).toEpochDay() }
                if (later.isNotEmpty()) {
                    item { Text("Później", fontSize = 15.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.padding(top = 10.dp)) }
                    items(later, key = { "l${it.task.id}" }) { n -> TaskTile(st, n, showDue = true, showProject = true) }
                }
            }
            else -> {
                val dotColor = ui.currentProject?.let { Color(it.colorArgb) } ?: ui.currentLabel?.let { Color(it.colorArgb) } ?: g.accent
                item {
                    Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dot(dotColor, 12.dp); Spacer(Modifier.width(10.dp))
                        Text(ui.title, fontSize = 26.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Sora, letterSpacing = (-0.52).sp)
                        Spacer(Modifier.width(10.dp))
                        val n = nodes.size
                        Text("$n ${plural(n, "zadanie", "zadania", "zadań")}", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
                    }
                }
                if (ui.isEmpty) item {
                    when (ui.view) {
                        AppView.Inbox -> EmptyState("Skrzynka pusta", "Zadania bez terminu i projektu trafią tutaj.")
                        AppView.Completed -> EmptyState("Brak ukończonych", "Odhaczone zadania pojawią się w tym miejscu.")
                        is AppView.ProjectView -> EmptyState("Projekt jest pusty", "Dodaj pierwsze zadanie plusem w rogu.")
                        else -> EmptyState("Brak zadań z tą etykietą", "Oznacz zadania tą etykietą, aby je tu zebrać.")
                    }
                }
                val inProject = ui.view is AppView.ProjectView
                ui.groups.forEach { grp ->
                    if (grp.name != null) {
                        val key = "sec${grp.sectionId}"
                        item { SectionToggle(grp.name.uppercase(), grp.nodes.size, g.textSecondary, collapsed[key] != true) { collapsed[key] = !(collapsed[key] ?: false) } }
                        if (collapsed[key] != true) items(grp.nodes, key = { "s${it.task.id}" }) { n -> TaskTile(st, n, showDue = true, showProject = !inProject) }
                    } else items(grp.nodes, key = { "g${it.task.id}" }) { n -> TaskTile(st, n, showDue = true, showProject = !inProject) }
                }
            }
        }
    }
}

/** Nagłówek sekcji „ZALEGŁE · 3" z chevronem (zwijany). */
@Composable
private fun SectionToggle(label: String, count: Int, color: Color, expanded: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val rot by animateFloatAsState(if (expanded) 0f else -90f)
    Row(modifier.clip(RoundedCornerShape(12.dp)).clickable { onToggle() }.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label · $count", fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.2.sp, color = color, fontFamily = Manrope)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = color, modifier = Modifier.size(16.dp).rotate(rot))
    }
}

@Composable
private fun BucketToggle(label: String, expanded: Boolean, onToggle: () -> Unit) {
    val g = LocalGlass.current
    val rot by animateFloatAsState(if (expanded) 0f else -90f)
    Row(Modifier.clip(RoundedCornerShape(10.dp)).clickable { onToggle() }.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.W700, letterSpacing = 1.3.sp, color = g.textSecondary, fontFamily = Manrope)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = g.textSecondary, modifier = Modifier.size(14.dp).rotate(rot))
    }
}

@Composable
private fun EmptyState(head: String, sub: String) {
    val g = LocalGlass.current
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(74.dp).glass(CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = g.accent, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(head, fontSize = 16.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Text(sub, fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}

/**
 * Kafelek zadania z podzadaniami (`ZenRowWithSubs` + `TaskRowZen`): kółko z
 * priorytetem, tytuł, meta (data · czas · ⟳ · @etykiety · #projekt · n/m · ↗),
 * badge „⚡AI", godzina po prawej, „⏳ Napięty deadline", akcje: ukończ / przełóż / usuń.
 */
@Composable
private fun TaskTile(st: AppState, node: TaskNode, showDue: Boolean, showProject: Boolean) {
    val g = LocalGlass.current
    val t = node.task
    val today = st.today
    val tip = remember(t.title, t.notes) { AutomationAdvisor.advise(t.title, t.notes) }
    val tightDeadline = (t.durationMinutes ?: 0) >= 60 && t.deadline != null && (t.deadline!! - today) in 0..2 && !t.isCompleted
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp), elevation = 2.dp).clickable { st.openDetail(t.id) }) {
        if (tightDeadline || (tip.canAutomate && !t.isCompleted)) Row(Modifier.padding(start = 14.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (tightDeadline) Badge("⏳ Napięty deadline", Color(0xFFB45309), Color(0x33F59E0B))
            if (tip.canAutomate && !t.isCompleted) Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFC94D)).clickable { st.openAutomation(t) }.padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFF3B2A00), modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp)); Text("AI", fontSize = 10.sp, fontWeight = FontWeight.W800, color = Color(0xFF3B2A00), fontFamily = Manrope) }
        }
        TaskRow(st, t, today, showDue, showProject, node.subtasks.size, node.subtasks.count { it.isCompleted })
        node.subtasks.forEach { s -> TaskRow(st, s, today, showDue = false, showProject = false, subTotal = 0, subDone = 0, compact = true) }
    }
}

@Composable
private fun Badge(text: String, fg: Color, bg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) { Text(text, fontSize = 10.5.sp, fontWeight = FontWeight.W800, color = fg, fontFamily = Manrope) }
}

@Composable
private fun TaskRow(st: AppState, t: CloudTask, today: Long, showDue: Boolean, showProject: Boolean, subTotal: Int, subDone: Int, compact: Boolean = false) {
    val g = LocalGlass.current
    val done = t.isCompleted
    val ring = priorityColor(t.priority)
    val hasPrio = t.priority != "P4"
    val isOverdue = t.dueDate != null && t.dueDate!! < today && !done
    var hover by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = if (compact) 40.dp else 12.dp, end = 10.dp, top = if (compact) 8.dp else 12.dp, bottom = if (compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        GlassCheck(done, ring, if (compact) 19.dp else 23.dp, emphasize = hasPrio && !done) { st.toggle(t) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title, fontSize = if (compact) 13.5.sp else 15.sp, fontWeight = FontWeight.W500, lineHeight = if (compact) 18.sp else 20.sp,
                color = if (done) g.textSecondary else g.textPrimary, textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis, fontFamily = Manrope
            )
            val parts = mutableListOf<Pair<String, Color?>>()
            if (t.dueDate != null && (showDue || isOverdue) && t.dueDate != today) parts += shortDate(t.dueDate!!) to (if (isOverdue) g.overdue else null)
            t.durationMinutes?.let { parts += "$it min" to null }
            t.recurrence?.let { parts += "⟳ ${Recurrence.fromNameSafe(it)?.label?.lowercase() ?: it.lowercase()}" to null }
            t.deadline?.let { parts += "do ${shortDate(it)}" to g.warn }
            t.labelIds.forEach { parts += "@${st.repo.labelName(it)}" to null }
            val chips = parts.take(3).toMutableList()
            if (showProject && t.projectId != null) chips += "#${st.repo.projectName(t.projectId)}" to null
            if (subTotal > 0) chips += "$subDone/$subTotal" to null
            t.attachments.mapNotNull { runCatching { java.net.URI(it).host?.removePrefix("www.") }.getOrNull() }.distinct().forEach { chips += "↗ $it" to null }
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasPrio && !done) { Dot(ring, 7.dp); Spacer(Modifier.width(6.dp)) }
                    Text(
                        buildString { chips.forEachIndexed { i, (txt, _) -> if (i > 0) append("  ·  "); append(txt) } },
                        fontSize = 11.5.sp, fontWeight = FontWeight.W600, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (isOverdue) g.overdue else g.textSecondary
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            t.dueTimeMinutes?.let { Text(hm(it), fontSize = 12.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Manrope, modifier = Modifier.padding(top = 3.dp)) }
            if (!compact) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Odpowiednik gestów przesuwania: przełóż (kalendarz) i usuń.
                if (!done) SmallAction(Icons.Outlined.DateRange, "Przełóż", g.warn) { st.reschedTask = t }
                SmallAction(Icons.Outlined.DeleteOutline, "Usuń", g.textSecondary) { st.deleteTask(t.id) }
            }
        }
    }
    Unit.let { hover }
}

@Composable
private fun SmallAction(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(26.dp).clip(CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint.copy(alpha = 0.75f), modifier = Modifier.size(15.dp))
    }
}

// ─── Panel rutyn (jak `RoutinesPanel`) ──────────────────────────────────────
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BoxScope.RoutinesPanel(st: AppState) {
    val g = LocalGlass.current
    val ui = remember(st.rev, st.view, st.activeArea) { st.buildUiState() }
    val visible = st.routOpen && st.view == AppView.Today
    AnimatedVisibility(visible, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp, start = 14.dp, end = 14.dp), enter = slideInVertically { it / 4 } + fadeIn(), exit = slideOutVertically { it / 4 } + fadeOut()) {
        val total = ui.routines.size + ui.routinesDone
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), strong = true, elevation = 22.dp).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Autorenew, null, tint = g.accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                Text("Rutyny", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
                Spacer(Modifier.width(10.dp))
                Text(if (ui.routines.isEmpty()) "wszystkie zrobione" else "${ui.routinesDone} z $total zrobione", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.weight(1f))
                Box(Modifier.size(28.dp).clip(CircleShape).background(g.accent.copy(alpha = 0.12f)).clickable { st.routOpen = false }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Zwiń", tint = g.accent, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (ui.routines.isEmpty()) Text(if (total > 0) "Nawyki na dziś odhaczone 💜" else "Brak rutyn. Dodaj zadanie „codziennie” bez godziny (P3/P4), np. „Trening codziennie”.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.routines.forEach { r ->
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(g.accent.copy(alpha = 0.10f)).border(1.dp, g.accent.copy(alpha = 0.25f), RoundedCornerShape(50)).clickable { st.toggle(r) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(18.dp).clip(CircleShape).border(2.5.dp, g.accent, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(r.title, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope)
                    }
                }
            }
        }
    }
}

// ─── Arkusz „Termin" (jak `ReschedSheet`) ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReschedSheet(st: AppState, task: CloudTask) {
    val g = LocalGlass.current
    val today = st.today
    val picker = rememberDatePickerState(initialSelectedDateMillis = (task.dueDate ?: today) * 86_400_000L)
    fun pick(day: Long) { st.reschedule(task, day); st.reschedTask = null }
    Sheet(onDismiss = { st.reschedTask = null }, maxWidth = 460.dp) {
        Text("Termin", fontSize = 17.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Text(task.title, fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp))
        ReschedRow(Icons.Outlined.WbSunny, "Dziś", weekdayShort(today)) { pick(today) }
        ReschedRow(Icons.Outlined.WbSunny, "Jutro", weekdayShort(today + 1)) { pick(today + 1) }
        ReschedRow(Icons.Outlined.Weekend, "Następny weekend", weekdayShort(nextWeekend(today))) { pick(nextWeekend(today)) }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (g.dark) Color.White.copy(alpha = 0.06f) else Color.White)) {
            DatePicker(
                state = picker, title = null, headline = null, showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = Color.Transparent, selectedDayContainerColor = g.accent, todayDateBorderColor = g.accent, todayContentColor = g.accent)
            )
        }
        Spacer(Modifier.height(10.dp))
        PrimaryBtn("Zapisz") { picker.selectedDateMillis?.let { pick(Math.floorDiv(it, 86_400_000L)) } }
    }
}

@Composable
private fun ReschedRow(icon: ImageVector, label: String, day: String, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(if (g.dark) Color.White.copy(alpha = 0.06f) else Color.White).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = g.accent, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
        Text(day, fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}

// ─── Sortowanie (jak `SortSheet`) ───────────────────────────────────────────
@Composable
fun SortSheet(st: AppState) {
    val g = LocalGlass.current
    Sheet(onDismiss = { st.showSort = false }, maxWidth = 420.dp) {
        Text("Sortuj zadania", fontSize = 17.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Spacer(Modifier.height(10.dp))
        SortMode.entries.forEach { m ->
            val sel = m == st.sort
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) g.accent else g.field).clickable { st.changeSort(m); st.showSort = false }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(m.label, fontSize = 14.sp, fontWeight = FontWeight.W800, color = if (sel) Color.White else g.textPrimary, fontFamily = Manrope)
                    Text(m.hint, fontSize = 11.5.sp, color = if (sel) Color.White.copy(alpha = 0.85f) else g.textSecondary, fontFamily = Manrope)
                }
                if (sel) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
