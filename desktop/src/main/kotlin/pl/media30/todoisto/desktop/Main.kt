package pl.media30.todoisto.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.media30.todoisto.shared.CloudTask
import pl.media30.todoisto.data.QuickAddParser
import java.time.LocalDate

// ─── Paleta (spójna z wersją mobilną) ────────────────────────────────────────
private val Accent = Color(0xFF6B3FE0)
private val TextPrimary = Color(0xFF241844)
private val TextSecondary = Color(0xFF6E5F93)
private val BgTop = Color(0xFFEDE6FA)
private val BgBottom = Color(0xFFDCE4FB)

private fun priorityColor(p: String): Color = when (p) {
    "P1" -> Color(0xFFD1453B); "P2" -> Color(0xFFEB8909); "P3" -> Color(0xFF246FE0); else -> Accent
}

fun main() = application {
    val state = rememberWindowState(width = 460.dp, height = 800.dp)
    Window(onCloseRequest = ::exitApplication, title = "Todoisto", state = state) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Accent)) {
            App()
        }
    }
}

@Composable
private fun App() {
    val repo = remember { TaskRepository() }
    val settings = remember { SyncSettings() }
    val scope = rememberCoroutineScope()
    var tick by remember { mutableStateOf(0) }
    val tasks = remember(tick) { repo.tasks }
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(true) }
    var dirty by remember { mutableStateOf(0) }
    var view by remember { mutableStateOf("today") }        // "today" | "upcoming"
    var editing by remember { mutableStateOf<CloudTask?>(null) }

    val today = LocalDate.now().toEpochDay()
    val shown = remember(tasks, view) {
        when (view) {
            "upcoming" -> tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! > today }
            "done" -> tasks.filter { it.isCompleted }
            else -> tasks.filter { !it.isCompleted && (it.dueDate == null || it.dueDate!! <= today) }
        }
    }

    val credsReady = settings.configured && settings.email.isNotBlank() && password.isNotBlank()

    // Wspólna procedura synchronizacji (blokujący sync na wątku IO).
    fun runSync(auto: Boolean) {
        if (syncing) return
        if (!credsReady) { if (!auto) showSettings = true; return }
        syncing = true
        if (!auto) toast = "Synchronizuję…"
        scope.launch {
            val r = withContext(Dispatchers.IO) { sync(repo, settings, password) }
            toast = r.message; syncing = false; tick++
        }
    }

    // Auto-sync: przy starcie (gdy dane gotowe) i cyklicznie co 2 minuty.
    LaunchedEffect(credsReady, autoSync) {
        if (autoSync && credsReady) {
            runSync(auto = true)
            while (true) { delay(120_000); if (autoSync && credsReady && !syncing) runSync(auto = true) }
        }
    }
    // Sync wkrótce po lokalnej zmianie (debounce ~4 s).
    LaunchedEffect(dirty) {
        if (dirty > 0 && autoSync && credsReady) { delay(4000); if (!syncing) runSync(auto = true) }
    }
    // Powiadomienia macOS o przypomnieniach (także z zadań zsynchronizowanych z telefonu).
    LaunchedEffect(Unit) {
        var lastCheck = System.currentTimeMillis()
        val notified = mutableSetOf<Long>()
        while (true) {
            delay(30_000)
            val now = System.currentTimeMillis()
            repo.tasks.forEach { t ->
                val at = t.reminderAt
                if (!t.isCompleted && at != null && at in (lastCheck + 1)..now && t.id !in notified) {
                    DesktopNotifier.notify("Przypomnienie", t.title); notified += t.id
                }
            }
            lastCheck = now
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom)))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (view == "upcoming") "Nadchodzące" else "Dzisiaj", fontSize = 30.sp, fontWeight = FontWeight.W800, color = TextPrimary)
                    Text(dateCaption(), fontSize = 13.sp, color = TextSecondary)
                }
                CircleBtn(Icons.Outlined.CloudSync, "Synchronizuj", enabled = !syncing) { runSync(auto = false) }
                Spacer(Modifier.width(8.dp))
                CircleBtn(Icons.Outlined.Settings, "Ustawienia chmury") { showSettings = true }
            }
            Spacer(Modifier.height(16.dp))

            QuickAdd(input, { input = it }) {
                val p = QuickAddParser().parse(input)
                if (p.title.isNotBlank()) {
                    repo.add(CloudTask(
                        id = 0, title = p.title, priority = p.priority.name,
                        dueDate = p.dueDate, dueTimeMinutes = p.dueTimeMinutes,
                        durationMinutes = p.durationMinutes, recurrence = p.recurrence?.name
                    ))
                    input = ""; tick++; dirty++
                }
            }
            toast?.let {
                Spacer(Modifier.height(8.dp))
                val ok = it.contains("Zsynchronizowano")
                Text(
                    (if (syncing) "☁ " else "") + it,
                    fontSize = 12.sp, color = if (ok) Color(0xFF1F8A5B) else TextSecondary
                )
            }
            Spacer(Modifier.height(12.dp))
            // Zakładki widoku: Dziś / Nadchodzące
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewTab("Dziś", view == "today") { view = "today" }
                ViewTab("Nadchodzące", view == "upcoming") { view = "upcoming" }
                ViewTab("Ukończone", view == "done") { view = "done" }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(shown, key = { it.id }) { task ->
                    TaskRow(
                        task, repo,
                        onToggle = { repo.toggle(task.id); tick++; dirty++ },
                        onDelete = { repo.delete(task.id); tick++; dirty++ },
                        onOpen = { editing = task }
                    )
                }
                if (shown.isEmpty()) item {
                    Text(
                        when (view) {
                            "upcoming" -> "Brak nadchodzących zadań."
                            "done" -> "Nic jeszcze nieukończone."
                            else -> "Nic na dziś — odpocznij ✨"
                        },
                        fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 20.dp)
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }

        editing?.let { task ->
            TaskDetailDialog(
                task, repo,
                onSave = { repo.update(it); editing = null; tick++; dirty++ },
                onDelete = { repo.delete(task.id); editing = null; tick++; dirty++ },
                onClose = { editing = null }
            )
        }

        if (showSettings) {
            SettingsDialog(
                settings, password, { password = it },
                autoSync, { autoSync = it },
                onClose = { showSettings = false }
            )
        }
    }
}

@Composable
private fun CircleBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = if (enabled) Accent else TextSecondary, modifier = Modifier.size(19.dp)) }
}

@Composable
private fun QuickAdd(text: String, onText: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text, onValueChange = onText, modifier = Modifier.weight(1f),
            placeholder = { Text("Dodaj zadanie…  (np. Zadzwonić jutro o 15 #Praca p1)", fontSize = 13.sp) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Accent).clickable { onSubmit() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Add, "Dodaj", tint = Color.White, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun ViewTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 13.sp, fontWeight = FontWeight.W800,
        color = if (selected) Color.White else TextSecondary,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) Accent else Color.White)
            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun TaskRow(task: CloudTask, repo: TaskRepository, onToggle: () -> Unit, onDelete: () -> Unit, onOpen: () -> Unit) {
    val hasPrio = task.priority != "P4"
    val ring = priorityColor(task.priority)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .clickable { onOpen() }.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(23.dp).clip(CircleShape)
                .background(if (task.isCompleted) ring else Color.Transparent)
                .border(if (hasPrio) 3.dp else 2.dp, ring, CircleShape)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) { if (task.isCompleted) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title, fontSize = 15.sp, fontWeight = FontWeight.W500,
                color = if (task.isCompleted) TextSecondary else TextPrimary,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
            val meta = metaLine(task, repo)
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasPrio) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(ring)); Spacer(Modifier.width(6.dp))
                    }
                    Text(meta, fontSize = 11.5.sp, fontWeight = FontWeight.W600, color = TextSecondary)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            task.dueTimeMinutes?.let { m ->
                Text("%d:%02d".format(m / 60, m % 60), fontSize = 12.sp, fontWeight = FontWeight.W800, color = Accent)
                Spacer(Modifier.height(6.dp))
            }
            Icon(
                Icons.Outlined.DeleteOutline, "Usuń zadanie", tint = TextSecondary,
                modifier = Modifier.size(17.dp).clip(CircleShape).clickable { onDelete() }
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    settings: SyncSettings, password: String, onPassword: (String) -> Unit,
    autoSync: Boolean, onAutoSync: (Boolean) -> Unit, onClose: () -> Unit
) {
    var url by remember { mutableStateOf(settings.url) }
    var key by remember { mutableStateOf(settings.anonKey) }
    var email by remember { mutableStateOf(settings.email) }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                settings.url = url.trim().trimEnd('/'); settings.anonKey = key.trim(); settings.email = email.trim(); onClose()
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Anuluj") } },
        title = { Text("Synchronizacja (Supabase)") },
        text = {
            Column {
                Text("Wpisz dane tego samego projektu Supabase, którego używa aplikacja na Androidzie — wtedy zadania synchronizują się między urządzeniami.",
                    fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(url, { url = it }, label = { Text("URL projektu (https://…supabase.co)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(key, { key = it }, label = { Text("Klucz anon (public)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, label = { Text("E-mail konta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(password, onPassword, label = { Text("Hasło") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoSync, onCheckedChange = onAutoSync)
                    Text("Synchronizuj automatycznie (w tle)", fontSize = 13.sp, color = TextPrimary)
                }
                Text("Dane logowania i hasło zostają tylko na tym Macu.", fontSize = 11.sp, color = TextSecondary)
            }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TaskDetailDialog(task: CloudTask, repo: TaskRepository, onSave: (CloudTask) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes) }
    var priority by remember { mutableStateOf(task.priority) }
    var projectId by remember { mutableStateOf(task.projectId) }
    var dueText by remember { mutableStateOf(task.dueDate?.let { LocalDate.ofEpochDay(it).toString() } ?: "") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                val due = dueText.trim().takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
                onSave(task.copy(title = title.trim(), notes = notes.trim(), priority = priority, projectId = projectId, dueDate = due))
            }) { Text("Zapisz") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Usuń", color = Color(0xFFD1453B)) }
                TextButton(onClick = onClose) { Text("Anuluj") }
            }
        },
        title = { Text("Szczegóły zadania") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Tytuł") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notatki") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Priorytet", fontSize = 12.sp, fontWeight = FontWeight.W700, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("P1", "P2", "P3", "P4").forEach { p ->
                        val c = priorityColor(p)
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).border(2.dp, c, CircleShape)
                                .background(if (p == priority) c else Color.Transparent)
                                .clickable { priority = p },
                            contentAlignment = Alignment.Center
                        ) { if (p == priority) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Projekt", fontSize = 12.sp, fontWeight = FontWeight.W700, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProjectChip("Skrzynka", projectId == null) { projectId = null }
                    repo.projects.forEach { p ->
                        ProjectChip(p.name, projectId == p.id) { projectId = p.id }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(dueText, { dueText = it }, label = { Text("Termin (RRRR-MM-DD, puste = brak)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

@Composable
private fun ProjectChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        name, fontSize = 12.sp, fontWeight = FontWeight.W700,
        color = if (selected) Color.White else Accent,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) Accent else Accent.copy(alpha = 0.12f))
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun metaLine(task: CloudTask, repo: TaskRepository): String {
    val parts = mutableListOf<String>()
    task.durationMinutes?.let { parts += if (it < 60) "$it min" else "${it / 60}h" }
    task.recurrence?.let {
        parts += when (it) {
            "DAILY" -> "codziennie"; "WEEKLY" -> "co tydzień"; "MONTHLY" -> "co miesiąc"; "YEARLY" -> "co rok"; else -> it.lowercase()
        }
    }
    task.projectId?.let { parts += "#" + repo.projectName(it) }
    task.labelIds.forEach { parts += "@" + repo.labelName(it) }
    return parts.joinToString("  ·  ")
}

private val MS = listOf("stycznia","lutego","marca","kwietnia","maja","czerwca","lipca","sierpnia","września","października","listopada","grudnia")
private fun dateCaption(): String { val d = LocalDate.now(); return "${d.dayOfMonth} ${MS[d.monthValue - 1]}" }
