package pl.media30.todoisto.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassPanelTint
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskScreen(
    existing: Task?,
    projects: List<Project>,
    labels: List<Label>,
    subtasks: List<Task>,
    onSave: (Task) -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (Task) -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.P4) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var deadline by remember { mutableStateOf(existing?.deadline) }
    var recurrence by remember { mutableStateOf(existing?.recurrence) }
    var durationMinutes by remember { mutableStateOf(existing?.durationMinutes) }
    var projectId by remember { mutableStateOf(existing?.projectId) }
    var selectedLabels by remember { mutableStateOf(existing?.labelIds?.toSet() ?: emptySet()) }
    var newSubtask by remember { mutableStateOf("") }

    var datePickerFor by remember { mutableStateOf<DateTarget?>(null) }
    var projectMenu by remember { mutableStateOf(false) }

    val isEditing = existing != null

    fun assemble(): Task = (existing ?: Task(title = "")).copy(
        title = title.trim(),
        notes = notes.trim(),
        priority = priority,
        dueDate = dueDate,
        deadline = deadline,
        recurrence = recurrence,
        durationMinutes = durationMinutes,
        projectId = projectId,
        labelIds = selectedLabels.toList()
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edytuj zadanie" else "Nowe zadanie", fontWeight = FontWeight.Bold, color = GlassTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Zamknij", tint = GlassTextPrimary) }
                },
                actions = {
                    if (isEditing && onDuplicate != null) {
                        IconButton(onClick = onDuplicate) { Icon(Icons.Outlined.ContentCopy, "Duplikuj", tint = GlassTextPrimary) }
                    }
                    if (isEditing && onDelete != null) {
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Usuń", tint = GlassTextPrimary) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GlassPanelTint.copy(alpha = 0.08f),
                    titleContentColor = GlassTextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Co jest do zrobienia?") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(22.dp), colors = glassFieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notatki (opcjonalnie)") }, minLines = 2,
                shape = RoundedCornerShape(22.dp), colors = glassFieldColors(), modifier = Modifier.fillMaxWidth()
            )

            // Project
            Spacer(Modifier.height(24.dp))
            SectionLabel(Icons.Outlined.Folder, "Projekt")
            Spacer(Modifier.height(10.dp))
            Box {
                Pill(onClick = { projectMenu = true }) {
                    val current = projects.firstOrNull { it.id == projectId }
                    Box(Modifier.size(10.dp).clip(CircleShape).background(current?.let { Color(it.colorArgb) } ?: GlassTextSecondary))
                    Spacer(Modifier.width(8.dp))
                    Text(current?.name ?: "Skrzynka", color = GlassTextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
                DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    DropdownMenuItem(text = { Text("Skrzynka") }, onClick = { projectId = null; projectMenu = false })
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = { projectId = p.id; projectMenu = false })
                    }
                }
            }

            // Priority
            Spacer(Modifier.height(24.dp))
            SectionLabel(Icons.Outlined.Flag, "Priorytet")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Priority.entries.forEach { p ->
                    SelectChip("P${p.ordinal + 1}", priority == p, p.color) { priority = p }
                }
            }

            // Due date
            Spacer(Modifier.height(24.dp))
            SectionLabel(Icons.Outlined.CalendarToday, "Termin")
            Spacer(Modifier.height(10.dp))
            DateRow(
                value = dueDate,
                emptyLabel = "Ustaw termin",
                onPick = { datePickerFor = DateTarget.Due },
                onClear = { dueDate = null }
            )

            // Recurrence
            Spacer(Modifier.height(24.dp))
            SectionLabel(Icons.Outlined.Repeat, "Powtarzanie")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectChip("Nie", recurrence == null, Color(0xFF6B3FE0)) { recurrence = null }
                Recurrence.entries.forEach { r ->
                    SelectChip(r.label, recurrence == r, Color(0xFF6B3FE0)) { recurrence = r }
                }
            }

            // Duration
            Spacer(Modifier.height(24.dp))
            SectionLabel(Icons.Outlined.Timer, "Czas trwania")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectChip("Brak", durationMinutes == null, GlassAccent) { durationMinutes = null }
                listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                    SelectChip(durationLabel(minutes), durationMinutes == minutes, GlassAccent) {
                        durationMinutes = minutes
                    }
                }
            }

            // Deadline
            Spacer(Modifier.height(24.dp))
            SectionLabel(Icons.Outlined.Flag, "Deadline (nieprzekraczalny)")
            Spacer(Modifier.height(10.dp))
            DateRow(
                value = deadline,
                emptyLabel = "Ustaw deadline",
                accentColor = Color(0xFFFF8A5B),
                onPick = { datePickerFor = DateTarget.Deadline },
                onClear = { deadline = null }
            )

            // Labels
            if (labels.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionLabel(Icons.Outlined.Sell, "Etykiety")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    labels.forEach { l ->
                        val sel = selectedLabels.contains(l.id)
                        SelectChip(l.name, sel, Color(l.colorArgb)) {
                            selectedLabels = if (sel) selectedLabels - l.id else selectedLabels + l.id
                        }
                    }
                }
            }

            // Subtasks (only for saved tasks)
            if (isEditing) {
                Spacer(Modifier.height(24.dp))
                SectionLabel(Icons.Outlined.AccountTree, "Podzadania")
                Spacer(Modifier.height(10.dp))
                Column(Modifier.animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))) {
                subtasks.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape)
                                .then(
                                    if (sub.isCompleted) Modifier.background(GlassAccent)
                                    else Modifier.border(2.dp, GlassTextSecondary, CircleShape)
                                )
                                .clickable { onToggleSubtask(sub) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sub.isCompleted) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(sub.title, color = if (sub.isCompleted) GlassTextSecondary else GlassTextPrimary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubtask, onValueChange = { newSubtask = it },
                        placeholder = { Text("Dodaj podzadanie") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newSubtask.isNotBlank()) { onAddSubtask(newSubtask); newSubtask = "" }
                        }),
                        shape = RoundedCornerShape(22.dp), colors = glassFieldColors(), modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { if (newSubtask.isNotBlank()) { onAddSubtask(newSubtask); newSubtask = "" } }) {
                        Icon(Icons.Filled.Add, "Dodaj", tint = GlassAccent)
                    }
                }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSave(assemble()) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassAccent, contentColor = Color.White,
                    disabledContainerColor = GlassPanelTint.copy(alpha = 0.18f),
                    disabledContentColor = GlassTextSecondary.copy(alpha = 0.7f)
                )
            ) {
                Text(if (isEditing) "Zapisz zmiany" else "Dodaj zadanie", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    datePickerFor?.let { target ->
        val initial = (if (target == DateTarget.Due) dueDate else deadline)?.let { it * 24L * 60 * 60 * 1000 }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val epochDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        if (target == DateTarget.Due) dueDate = epochDay else deadline = epochDay
                    }
                    datePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePickerFor = null }) { Text("Anuluj") } }
        ) { DatePicker(state = state) }
    }
}

private enum class DateTarget { Due, Deadline }

private fun durationLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m} min"
    }
}

@Composable
private fun DateRow(
    value: Long?,
    emptyLabel: String,
    accentColor: Color = Color(0xFF5E2ED6),
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f))
        },
        label = "dateRow"
    ) { current ->
        if (current == null) {
            Pill(onClick = onPick) {
                Icon(Icons.Filled.Add, null, tint = GlassTextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(emptyLabel, color = GlassTextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(onClick = onPick, accent = accentColor) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        LocalDate.ofEpochDay(current).format(dateFormatter),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = onClear) { Icon(Icons.Filled.Close, "Wyczyść", tint = GlassTextSecondary) }
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GlassAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = GlassTextPrimary)
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val bg by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.16f) else GlassPanelTint.copy(alpha = 0.07f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) color else GlassTextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipText"
    )
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) color else GlassPanelTint.copy(alpha = 0.30f), shape)
            .bouncy(scaleDown = 0.9f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
private fun Pill(onClick: () -> Unit, accent: Color? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(50)
    // Flat translucent fill + specular rim: no gradients on elements.
    val bg = accent ?: GlassPanelTint.copy(alpha = 0.12f)
    val rim = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (accent != null) 0.75f else 0.9f),
            GlassPanelTint.copy(alpha = 0.15f)
        )
    )
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, rim, shape)
            .bouncy(scaleDown = 0.93f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() }
    )
}
