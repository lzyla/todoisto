package pl.media30.todoisto.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.components.GlassCheck
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassHair
import pl.media30.todoisto.ui.theme.GlassShadow
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTheme
import pl.media30.todoisto.ui.theme.GlassTint
import pl.media30.todoisto.ui.theme.glass
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

private val IMAGE_EXT = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
internal fun isImageUrl(url: String): Boolean =
    IMAGE_EXT.any { url.substringBefore('?').lowercase().endsWith(it) }

private fun domainOf(url: String): String =
    url.removePrefix("https://").removePrefix("http://").substringBefore('/')

private val TILE_COLORS = listOf(0xFF24292F, 0xFFA259FF, 0xFF246FE0, 0xFF12A150, 0xFFB33B00, 0xFF6B3FE0)
private fun tileColor(domain: String): Color = Color(TILE_COLORS[Math.floorMod(domain.hashCode(), TILE_COLORS.size)])

/**
 * Arkusz szczegółów zadania — 1:1 z prototypem: uchwyt, kółko + tytuł + notatki,
 * chipy metadanych, Priorytet jako ringi kolorów, Projekt/Termin/Deadline/
 * Powtarzanie/Czas trwania/Etykiety jako presety-pigułki, Załączniki i linki,
 * Podzadania z paskiem postępu, stopka Usuń / Gotowe. Zmiany zapisują się od razu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    projects: List<Project>,
    labels: List<Label>,
    subtasks: List<Task>,
    onPatch: (Task) -> Unit,
    onToggleSubtask: (Task) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    onAskAi: (String) -> Unit = {}
) {
    var newSubtask by remember { mutableStateOf("") }
    var newLink by remember { mutableStateOf("") }
    var showDuePicker by remember { mutableStateOf(false) }
    var showDlPicker by remember { mutableStateOf(false) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var lightboxUrl by remember { mutableStateOf<String?>(null) }
    var titleEdit by remember(task.id) { mutableStateOf(false) }

    val today = LocalDate.now().toEpochDay()
    val done = task.isCompleted

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)
    ) {
        // Chmurka AI — dymek asystenta na samej górze (z zapasem miejsca)
        Spacer(Modifier.height(6.dp))
        AutomationCard(task.title, task.notes, onAskAi)
        Spacer(Modifier.height(22.dp))

        // Nagłówek
        Row(verticalAlignment = Alignment.Top) {
            val ring = if (task.priority != Priority.P4) task.priority.color else GlassAccent
            GlassCheck(done, ring, 27.dp) { onPatch(task.copy(isCompleted = !done)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (titleEdit) {
                    BasicTextField(
                        value = task.title,
                        onValueChange = { onPatch(task.copy(title = it)) },
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, lineHeight = 23.sp),
                        cursorBrush = SolidColor(GlassAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        task.title, fontSize = 18.sp, fontWeight = FontWeight.W800, lineHeight = 23.sp,
                        color = if (done) GlassTextSecondary else GlassTextPrimary,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).bouncy(0.99f) { titleEdit = true }
                    )
                }
                BasicTextField(
                    value = task.notes,
                    onValueChange = { onPatch(task.copy(notes = it)) },
                    textStyle = TextStyle(fontSize = 13.sp, color = GlassTextSecondary),
                    cursorBrush = SolidColor(GlassAccent),
                    decorationBox = { inner ->
                        Box(Modifier.padding(top = 4.dp)) {
                            if (task.notes.isEmpty()) Text("Notatki…", fontSize = 13.sp, color = GlassTextSecondary.copy(alpha = 0.55f))
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(Modifier.size(34.dp).clip(CircleShape).bouncy(0.85f, onClick = onDuplicate), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ContentCopy, "Duplikuj", tint = GlassTextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        // Priorytet — karta z ringami kolorów
        val prioAccent = if (task.priority != Priority.P4) task.priority.color else GlassAccent
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .shadow(5.dp, RoundedCornerShape(18.dp), spotColor = prioAccent.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(18.dp))
                .background(GlassTint)
                .border(1.dp, prioAccent.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(prioAccent))
            Spacer(Modifier.width(8.dp))
            Text("PRIORYTET", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Priority.entries.forEach { p ->
                    val sel = task.priority == p
                    val dot by animateFloatAsState(if (sel) 1f else 0f, spring(dampingRatio = 0.45f), label = "prioDot")
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).border(2.dp, p.color, CircleShape)
                            .bouncy(0.82f) { onPatch(task.copy(priority = p)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(13.dp).scale(dot).clip(CircleShape).background(p.color))
                    }
                }
            }
        }

        DetailSection("Projekt", Color(0xFF4D6BFF)) {
            PresetChip("Skrzynka", task.projectId == null, Color(0xFF9E9E9E), dot = Color(0xFF9E9E9E)) { onPatch(task.copy(projectId = null)) }
            projects.filter { !it.isArchived }.forEach { pr ->
                PresetChip(pr.name, task.projectId == pr.id, Color(pr.colorArgb), dot = Color(pr.colorArgb)) { onPatch(task.copy(projectId = pr.id)) }
            }
        }

        DetailSection("Termin", Color(0xFF6B3FE0)) {
            val due = task.dueDate
            PresetChip("Dzisiaj", due != null && due <= today, Color(0xFF6B3FE0), calendarIcon = true) { onPatch(task.copy(dueDate = today)) }
            PresetChip("Jutro", due == today + 1, Color(0xFF6B3FE0), calendarIcon = true) { onPatch(task.copy(dueDate = today + 1)) }
            PresetChip("Za 3 dni", due == today + 3, Color(0xFF6B3FE0), calendarIcon = true) { onPatch(task.copy(dueDate = today + 3)) }
            PresetChip("Przyszły tydz.", due == today + 7, Color(0xFF6B3FE0), calendarIcon = true) { onPatch(task.copy(dueDate = today + 7)) }
            PresetChip("Bez terminu", due == null, Color(0xFF6B3FE0)) { onPatch(task.copy(dueDate = null, recurrence = null)) }
            PresetChip("Data…", due != null && due > today + 1 && due != today + 3 && due != today + 7, Color(0xFF6B3FE0)) { showDuePicker = true }
        }

        DetailSection("Deadline", Color(0xFFC24B1A)) {
            val friday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)).toEpochDay()
            val endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
            PresetChip("Brak", task.deadline == null, Color(0xFFC24B1A)) { onPatch(task.copy(deadline = null)) }
            PresetChip("Do jutra", task.deadline == today + 1, Color(0xFFC24B1A)) { onPatch(task.copy(deadline = today + 1)) }
            PresetChip("Do piątku", task.deadline == friday, Color(0xFFC24B1A)) { onPatch(task.copy(deadline = friday)) }
            PresetChip("Koniec mies.", task.deadline == endOfMonth, Color(0xFFC24B1A)) { onPatch(task.copy(deadline = endOfMonth)) }
            PresetChip("Data…", task.deadline != null && task.deadline != today + 1 && task.deadline != friday && task.deadline != endOfMonth, Color(0xFFC24B1A)) { showDlPicker = true }
        }

        DetailSection("Powtarzanie", Color(0xFF2DD4BF)) {
            PresetChip("Nie", task.recurrence == null, Color(0xFF6B3FE0)) { onPatch(task.copy(recurrence = null)) }
            Recurrence.entries.forEach { r ->
                PresetChip(r.label, task.recurrence == r, Color(0xFF6B3FE0)) {
                    onPatch(task.copy(recurrence = r, dueDate = task.dueDate ?: today))
                }
            }
        }

        DetailSection("Czas trwania", Color(0xFF34D399)) {
            PresetChip("Brak", task.durationMinutes == null, Color(0xFF6B3FE0)) { onPatch(task.copy(durationMinutes = null)) }
            listOf(15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1h", 120 to "2h").forEach { (v, t) ->
                PresetChip(t, task.durationMinutes == v, Color(0xFF6B3FE0)) { onPatch(task.copy(durationMinutes = v)) }
            }
        }

        if (labels.isNotEmpty()) {
            DetailSection("Etykiety", Color(0xFFC24DFF)) {
                labels.forEach { l ->
                    val on = task.labelIds.contains(l.id)
                    PresetChip("@${l.name}", on, Color(l.colorArgb)) {
                        onPatch(task.copy(labelIds = if (on) task.labelIds - l.id else task.labelIds + l.id))
                    }
                }
            }
        }

        // Załączniki i linki
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .shadow(5.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF4D6BFF).copy(alpha = 0.4f))
                .clip(RoundedCornerShape(18.dp)).background(GlassTint)
                .border(1.dp, Color(0xFF4D6BFF).copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4D6BFF)))
                Spacer(Modifier.width(8.dp))
                Text("ZAŁĄCZNIKI I LINKI", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = GlassTextSecondary)
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                task.attachments.forEach { url ->
                    if (isImageUrl(url)) {
                        Box {
                            AsyncImage(
                                model = url, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                                    .background(GlassTint)
                                    .bouncy(0.94f) { lightboxUrl = url }
                            )
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(2.dp).size(16.dp)
                                    .background(Color(0x99000000), CircleShape)
                                    .bouncy(0.8f) { onPatch(task.copy(attachments = task.attachments - url)) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Close, "Usuń", tint = Color.White, modifier = Modifier.size(9.dp)) }
                        }
                    } else {
                        val domain = domainOf(url)
                        Row(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(GlassTint)
                                .bouncy(0.95f) { previewUrl = url }
                                .padding(start = 8.dp, end = 13.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(tileColor(domain)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(domain.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.W800, color = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    url.substringAfterLast('/').ifBlank { domain },
                                    fontSize = 12.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp)
                                )
                                Text(domain, fontSize = 10.sp, fontWeight = FontWeight.W600, color = GlassTextSecondary)
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Close, "Usuń", tint = GlassTextSecondary,
                                modifier = Modifier.size(13.dp).bouncy(0.8f) { onPatch(task.copy(attachments = task.attachments - url)) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = newLink, onValueChange = { newLink = it },
                    textStyle = TextStyle(fontSize = 12.5.sp, color = GlassTextPrimary),
                    cursorBrush = SolidColor(GlassAccent),
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassTint)
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            if (newLink.isEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Link, null, tint = GlassTextSecondary, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Wklej link lub adres zdjęcia…", fontSize = 12.5.sp, color = GlassTextSecondary.copy(alpha = 0.6f))
                                }
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(GlassAccent)
                        .bouncy(0.85f) {
                            val t = newLink.trim()
                            if (t.isNotEmpty()) {
                                val u = if (t.startsWith("http")) t else "https://$t"
                                onPatch(task.copy(attachments = task.attachments + u))
                                newLink = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, "Dodaj", tint = Color.White, modifier = Modifier.size(15.dp)) }
            }
        }

        // Podzadania
        if (task.parentId == null) {
            Column(
                Modifier.fillMaxWidth().padding(top = 10.dp)
                    .shadow(5.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFFA47CFF).copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(18.dp)).background(GlassTint)
                    .border(1.dp, Color(0xFFA47CFF).copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFA47CFF)))
                    Spacer(Modifier.width(8.dp))
                    Text("PODZADANIA", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                    Text(
                        "${subtasks.count { it.isCompleted }}/${subtasks.size}",
                        fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(GlassTint)) {
                    val frac = if (subtasks.isEmpty()) 0f else subtasks.count { it.isCompleted }.toFloat() / subtasks.size
                    Box(
                        Modifier.fillMaxWidth(frac).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0))))
                    )
                }
                Spacer(Modifier.height(6.dp))
                subtasks.forEach { st ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassCheck(st.isCompleted, GlassAccent, 19.dp) { onToggleSubtask(st) }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            st.title, fontSize = 13.5.sp,
                            color = if (st.isCompleted) GlassTextSecondary else GlassTextPrimary,
                            textDecoration = if (st.isCompleted) TextDecoration.LineThrough else null
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = newSubtask, onValueChange = { newSubtask = it },
                        textStyle = TextStyle(fontSize = 13.sp, color = GlassTextPrimary),
                        cursorBrush = SolidColor(GlassAccent),
                        decorationBox = { inner ->
                            Box(Modifier.padding(vertical = 8.dp)) {
                                if (newSubtask.isEmpty()) Text("Dodaj podzadanie…", fontSize = 13.sp, color = GlassTextSecondary.copy(alpha = 0.6f))
                                inner()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .bouncy(0.85f) {
                                if (newSubtask.isNotBlank()) { onAddSubtask(newSubtask); newSubtask = "" }
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, "Dodaj", tint = GlassAccent, modifier = Modifier.size(16.dp)) }
                }
            }
        }

        // Stopka
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Usuń", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = Color(0xFFD1453B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                    .border(1.5.dp, Color(0x73D1453B), RoundedCornerShape(50))
                    .bouncy(0.96f, onClick = onDelete).padding(vertical = 12.dp)
            )
            Text(
                "Gotowe", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(2f).clip(RoundedCornerShape(50)).background(GlassAccent)
                    .bouncy(0.97f, onClick = onClose).padding(vertical = 12.dp)
            )
        }
    }

    if (showDuePicker) {
        GlassDatePicker(task.dueDate, { showDuePicker = false }) { onPatch(task.copy(dueDate = it)); showDuePicker = false }
    }
    if (showDlPicker) {
        GlassDatePicker(task.deadline, { showDlPicker = false }) { onPatch(task.copy(deadline = it)); showDlPicker = false }
    }
    previewUrl?.let { LinkBrowser(it) { previewUrl = null } }
    lightboxUrl?.let { ImageLightbox(it) { lightboxUrl = null } }
}

@Composable
private fun HairLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassHair))
}

/** #4b — mała chmurka „AI" u góry ustawień; po kliknięciu rozwija samouczek automatyzacji. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutomationCard(title: String, notes: String, onAskAi: (String) -> Unit) {
    val tip = remember(title, notes) { pl.media30.todoisto.data.AutomationAdvisor.advise(title, notes) }
    var expanded by remember { mutableStateOf(false) }

    // Dymek jak w Messengerze: awatar + chmurka „glass" z ogonkiem przy awatarze.
    val bubbleShape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    val cardShape = RoundedCornerShape(topStart = 8.dp, topEnd = 26.dp, bottomEnd = 26.dp, bottomStart = 26.dp)

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            // Awatar AI
            Box(
                Modifier.size(44.dp)
                    .shadow(10.dp, CircleShape, spotColor = GlassAccent.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(23.dp)) }
            Spacer(Modifier.width(11.dp))

            Column(Modifier.weight(1f)) {
                // Chmurka (zwinięta) — pytanie zachęcające
                if (!expanded) {
                    Column(
                        Modifier.glass(bubbleShape).bouncy(0.96f) { expanded = true }
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Text("Nie wiesz jak się zabrać?", fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                        Spacer(Modifier.height(3.dp))
                        Text("Stuknij — ogarniemy to razem ✨", fontSize = 12.5.sp, fontWeight = FontWeight.W600, color = GlassTextSecondary)
                    }
                }
                // Chmurka (rozwinięta) — „bąbel" się powiększa
                androidx.compose.animation.AnimatedVisibility(
                    visible = expanded,
                    enter = androidx.compose.animation.fadeIn(tween(140)) +
                        androidx.compose.animation.scaleIn(
                            spring(dampingRatio = 0.62f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                            initialScale = 0.5f,
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        ) + androidx.compose.animation.expandVertically(spring(dampingRatio = 0.78f)),
                    exit = androidx.compose.animation.fadeOut(tween(120)) +
                        androidx.compose.animation.scaleOut(tween(200), targetScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)) +
                        androidx.compose.animation.shrinkVertically(tween(200))
                ) {
                    Column(Modifier.glass(cardShape).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Ogarnijmy to razem", fontSize = 16.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                                Text(tip.headline, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = GlassAccent)
                            }
                            Box(Modifier.size(28.dp).clip(CircleShape).bouncy(0.9f) { expanded = false }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, "Zwiń", tint = GlassTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tip.tools.forEach { t ->
                                Text(
                                    t, fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassAccent,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassAccent.copy(alpha = 0.12f)).padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        tip.steps.forEachIndexed { i, step ->
                            Row(Modifier.padding(bottom = 10.dp)) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(GlassAccent),
                                    contentAlignment = Alignment.Center
                                ) { Text("${i + 1}", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.W800) }
                                Spacer(Modifier.width(11.dp))
                                Text(step, fontSize = 13.5.sp, lineHeight = 19.sp, color = GlassTextPrimary, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(GlassAccent)
                                .bouncy(0.97f) { onAskAi(tip.aiPrompt) }
                                .padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Zróbmy to z AI", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.W800)
                        }
                        Text(
                            "Zapyta AI Twoim kluczem i pokaże odpowiedź tutaj. Klucz ustawisz w menu.",
                            fontSize = 10.5.sp, color = GlassTextSecondary, modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailSection(title: String, accent: Color = GlassAccent, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp)
            .shadow(5.dp, RoundedCornerShape(18.dp), spotColor = accent.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(18.dp))
            .background(GlassTint)
            .border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = GlassTextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            content = { content() }
        )
    }
}

@Composable
private fun PresetChip(
    text: String,
    selected: Boolean,
    color: Color,
    dot: Color? = null,
    calendarIcon: Boolean = false,
    onClick: () -> Unit
) {
    val d = GlassTheme.dark
    val bg = if (selected) color.copy(alpha = if (d) 0.28f else 0.16f) else if (d) Color.White.copy(alpha = 0.08f) else Color(0x0F6B3FE0)
    val fg = if (selected) (if (d) lerp(color, Color.White, 0.4f) else color) else GlassTextSecondary
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).bouncy(0.92f, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dot))
            Spacer(Modifier.width(6.dp))
        }
        if (calendarIcon) {
            Icon(Icons.Outlined.CalendarToday, null, tint = fg, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.W700, color = fg)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassDatePicker(initialEpochDay: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay?.let { it * 86_400_000L })
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    ) { DatePicker(state = state) }
}

/**
 * Mini-przeglądarka linku (1:1 z prototypem): nagłówek z kaflem litery,
 * tytułem i domeną + otwórz/zamknij; treść (WebView); dolny pasek
 * wstecz · Udostępnij · Kopiuj.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkBrowser(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val domain = domainOf(url)
    var webView by remember { mutableStateOf<WebView?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GlassSurface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(tileColor(domain)), contentAlignment = Alignment.Center) {
                    Text(domain.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.W800, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(url.substringAfterLast('/').ifBlank { domain }, fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(domain, fontSize = 11.sp, fontWeight = FontWeight.W600, color = GlassTextSecondary)
                }
                Box(Modifier.size(34.dp).clip(CircleShape).bouncy(0.85f) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.OpenInNew, "Otwórz", tint = GlassTextSecondary, modifier = Modifier.size(15.dp))
                }
                Box(Modifier.size(34.dp).clip(CircleShape).bouncy(0.85f, onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, "Zamknij", tint = GlassTextSecondary, modifier = Modifier.size(14.dp))
                }
            }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl(url)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxWidth().height(460.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp).navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).bouncy(0.85f) { webView?.goBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", fontSize = 20.sp, color = GlassTextSecondary)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).bouncy(0.94f) {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
                        }, null))
                    }.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Share, null, tint = GlassTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Udostępnij", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).bouncy(0.94f) {
                        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("url", url))
                    }.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, tint = GlassTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kopiuj", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary)
                }
            }
        }
    }
}

/** Lightbox zdjęcia: scrim 90%, kadr 4:3 z zaokrągleniem, podpis + wskazówka. */
@Composable
private fun ImageLightbox(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(Color(0xE609031A))
                .bouncy(1f, onClick = onDismiss),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = url, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.82f).clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(url.substringAfterLast('/'), fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(4.dp))
            Text("Dotknij, aby zamknąć", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}
