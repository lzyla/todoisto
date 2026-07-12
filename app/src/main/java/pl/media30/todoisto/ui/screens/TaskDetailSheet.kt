package pl.media30.todoisto.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassPanelTint
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("pl"))

private val IMAGE_EXT = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
internal fun isImageUrl(url: String): Boolean =
    IMAGE_EXT.any { url.substringBefore('?').lowercase().endsWith(it) }

private fun domainOf(url: String): String =
    url.removePrefix("https://").removePrefix("http://").substringBefore('/')

/**
 * Arkusz szczegółów / edycji zadania (styl Zen, wg handoffu): otwierany
 * kliknięciem zadania na dowolnym ekranie, zmiany zapisywane natychmiast
 * przez [onPatch]. Sekcje: Priorytet · Projekt · Termin · Deadline ·
 * Powtarzanie · Czas trwania · Etykiety · Podzadania · Załączniki i linki.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onClose: () -> Unit
) {
    var newSubtask by remember { mutableStateOf("") }
    var newLink by remember { mutableStateOf("") }
    var datePickerFor by remember { mutableStateOf<DateTarget?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var lightboxUrl by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now().toEpochDay()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        // ── Nagłówek: check + edytowalny tytuł + duplikuj ────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ring = if (task.priority != Priority.P4) task.priority.color else GlassAccent
            GlassCheck(task.isCompleted, ring, 27.dp) { onPatch(task.copy(isCompleted = !task.isCompleted)) }
            Spacer(Modifier.width(6.dp))
            ZenTextField(
                value = task.title,
                onValueChange = { onPatch(task.copy(title = it)) },
                placeholder = "Tytuł zadania",
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Outlined.ContentCopy, "Duplikuj", tint = GlassTextSecondary, modifier = Modifier.size(19.dp))
            }
        }
        ZenTextField(
            value = task.notes,
            onValueChange = { onPatch(task.copy(notes = it)) },
            placeholder = "Notatki…",
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(start = 33.dp)
        )

        Section("Priorytet") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Priority.entries.forEach { p ->
                    Chip("P${p.ordinal + 1}", task.priority == p, p.color) { onPatch(task.copy(priority = p)) }
                }
            }
        }

        Section("Projekt") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DotChip("Skrzynka", GlassTextSecondary, task.projectId == null) { onPatch(task.copy(projectId = null)) }
                projects.filter { !it.isArchived }.forEach { pr ->
                    DotChip(pr.name, Color(pr.colorArgb), task.projectId == pr.id) { onPatch(task.copy(projectId = pr.id)) }
                }
            }
        }

        Section("Termin") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Dzisiaj", task.dueDate == today, GlassAccent) { onPatch(task.copy(dueDate = today)) }
                Chip("Jutro", task.dueDate == today + 1, GlassAccent) { onPatch(task.copy(dueDate = today + 1)) }
                val custom = task.dueDate?.takeIf { it != today && it != today + 1 }
                Chip(
                    custom?.let { "📅 " + LocalDate.ofEpochDay(it).format(dateFmt) } ?: "📅 Data",
                    custom != null,
                    GlassAccent
                ) { datePickerFor = DateTarget.Due }
                if (task.dueDate != null) {
                    Chip("✕", false, GlassAccent) { onPatch(task.copy(dueDate = null)) }
                }
            }
        }

        Section("Deadline") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    task.deadline?.let { "⏳ " + LocalDate.ofEpochDay(it).format(dateFmt) } ?: "⏳ Ustaw",
                    task.deadline != null,
                    Color(0xFFE0692C)
                ) { datePickerFor = DateTarget.Deadline }
                if (task.deadline != null) {
                    Chip("✕", false, GlassAccent) { onPatch(task.copy(deadline = null)) }
                }
            }
        }

        Section("Powtarzanie") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Nie", task.recurrence == null, GlassAccent) { onPatch(task.copy(recurrence = null)) }
                Recurrence.entries.forEach { r ->
                    Chip(r.label, task.recurrence == r, GlassAccent) {
                        onPatch(task.copy(recurrence = r, dueDate = task.dueDate ?: today))
                    }
                }
            }
        }

        Section("Czas trwania") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Brak", task.durationMinutes == null, GlassAccent) { onPatch(task.copy(durationMinutes = null)) }
                listOf(15, 30, 45, 60, 120).forEach { m ->
                    Chip(if (m >= 60) "${m / 60}h" else "$m min", task.durationMinutes == m, GlassAccent) {
                        onPatch(task.copy(durationMinutes = m))
                    }
                }
            }
        }

        if (labels.isNotEmpty()) Section("Etykiety") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEach { l ->
                    val sel = task.labelIds.contains(l.id)
                    Chip("@${l.name}", sel, Color(l.colorArgb)) {
                        onPatch(task.copy(labelIds = if (sel) task.labelIds - l.id else task.labelIds + l.id))
                    }
                }
            }
        }

        if (task.parentId == null) Section("Podzadania") {
            Column {
                subtasks.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        GlassCheck(sub.isCompleted, GlassAccent, 19.dp) { onToggleSubtask(sub) }
                        Spacer(Modifier.width(11.dp))
                        Text(
                            sub.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sub.isCompleted) GlassTextSecondary else GlassTextPrimary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZenTextField(
                        value = newSubtask,
                        onValueChange = { newSubtask = it },
                        placeholder = "Dodaj podzadanie…",
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Done,
                        onDone = {
                            if (newSubtask.isNotBlank()) { onAddSubtask(newSubtask); newSubtask = "" }
                        }
                    )
                    IconButton(onClick = {
                        if (newSubtask.isNotBlank()) { onAddSubtask(newSubtask); newSubtask = "" }
                    }) { Icon(Icons.Filled.Add, "Dodaj", tint = GlassAccent, modifier = Modifier.size(19.dp)) }
                }
            }
        }

        Section("Załączniki i linki") {
            Column {
                val images = task.attachments.filter { isImageUrl(it) }
                val links = task.attachments.filterNot { isImageUrl(it) }
                if (images.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        images.forEach { url ->
                            Box {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GlassPanelTint.copy(alpha = 0.10f))
                                        .bouncy(0.94f) { lightboxUrl = url }
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .background(Color(0x99000000), CircleShape)
                                        .bouncy(0.8f) { onPatch(task.copy(attachments = task.attachments - url)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Close, "Usuń", tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }
                }
                links.forEach { url ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(GlassPanelTint.copy(alpha = 0.07f), RoundedCornerShape(50))
                            .bouncy(0.97f) { previewUrl = url }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Link, null, tint = GlassAccent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            domainOf(url),
                            style = MaterialTheme.typography.labelMedium,
                            color = GlassTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.Close, "Usuń", tint = GlassTextSecondary,
                            modifier = Modifier
                                .size(15.dp)
                                .bouncy(0.8f) { onPatch(task.copy(attachments = task.attachments - url)) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZenTextField(
                        value = newLink,
                        onValueChange = { newLink = it },
                        placeholder = "Wklej link lub adres zdjęcia…",
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Done,
                        onDone = {
                            addLink(newLink) { onPatch(task.copy(attachments = task.attachments + it)) }
                            newLink = ""
                        }
                    )
                    IconButton(onClick = {
                        addLink(newLink) { onPatch(task.copy(attachments = task.attachments + it)) }
                        newLink = ""
                    }) { Icon(Icons.Filled.Add, "Dodaj", tint = GlassAccent, modifier = Modifier.size(19.dp)) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(50.dp)) {
                Text("Usuń", color = Priority.P1.color)
            }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(2f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassAccent, contentColor = Color.White)
            ) { Text("Gotowe", fontWeight = FontWeight.SemiBold) }
        }
    }

    datePickerFor?.let { target ->
        val initial = (if (target == DateTarget.Due) task.dueDate else task.deadline)
            ?.let { it * 24L * 60 * 60 * 1000 }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        onPatch(if (target == DateTarget.Due) task.copy(dueDate = day) else task.copy(deadline = day))
                    }
                    datePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePickerFor = null }) { Text("Anuluj") } }
        ) { DatePicker(state = state) }
    }

    previewUrl?.let { url -> LinkPreviewSheet(url = url, onDismiss = { previewUrl = null }) }
    lightboxUrl?.let { url -> ImageLightbox(url = url, onDismiss = { lightboxUrl = null }) }
}

private fun addLink(raw: String, add: (String) -> Unit) {
    val t = raw.trim()
    if (t.isEmpty()) return
    add(if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t")
}

private enum class DateTarget { Due, Deadline }

/** Mini-przeglądarka linku w arkuszu (WebView) z akcją „otwórz w przeglądarce". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkPreviewSheet(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GlassSurface
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Link, null, tint = GlassAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    domainOf(url),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GlassTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }) { Icon(Icons.Outlined.OpenInNew, "Otwórz w przeglądarce", tint = GlassTextSecondary, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl(url)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

/** Pełnoekranowy lightbox zdjęcia — zamykany dotknięciem. */
@Composable
private fun ImageLightbox(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .bouncy(1f, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }
    }
}

// ── Wspólne klocki stylu Zen ─────────────────────────────────────────────────

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = GlassTextSecondary
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) color.copy(alpha = 0.16f) else GlassPanelTint.copy(alpha = 0.07f),
                RoundedCornerShape(50)
            )
            .bouncy(0.9f, onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) color else GlassTextSecondary
        )
    }
}

@Composable
private fun DotChip(label: String, dot: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(
                if (selected) dot.copy(alpha = 0.16f) else GlassPanelTint.copy(alpha = 0.07f),
                RoundedCornerShape(50)
            )
            .bouncy(0.9f, onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) GlassTextPrimary else GlassTextSecondary
        )
    }
}

/** Bezramkowe pole tekstowe Zen (przezroczyste tło, bez podkreślenia). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    onDone: () -> Unit = {}
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = textStyle, color = GlassTextSecondary.copy(alpha = 0.7f)) },
        textStyle = textStyle.copy(color = GlassTextPrimary),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = GlassAccent,
            focusedTextColor = GlassTextPrimary,
            unfocusedTextColor = GlassTextPrimary
        ),
        modifier = modifier
    )
}
