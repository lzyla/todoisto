package pl.media30.todoisto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("pl"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    existing: Task?,
    onSave: (title: String, notes: String, priority: Priority, dueDate: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: Priority.P4) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEditing = existing != null

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edytuj zadanie" else "Nowe zadanie",
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Zamknij", tint = GlassTextPrimary)
                    }
                },
                actions = {
                    if (isEditing && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usuń", tint = GlassTextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.06f),
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Co jest do zrobienia?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = glassFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notatki (opcjonalnie)") },
                minLines = 2,
                colors = glassFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel(Icons.Outlined.Flag, "Priorytet")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Priority.entries.forEach { p ->
                    PriorityChip(
                        priority = p,
                        selected = priority == p,
                        onClick = { priority = p }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel(Icons.Outlined.CalendarToday, "Termin")
            Spacer(Modifier.height(10.dp))
            if (dueDate == null) {
                GlassPill(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.Add, null, tint = GlassTextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ustaw termin", color = GlassTextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // Date is set: show only the date chip + a clear button — no redundant "set date".
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassPill(onClick = { showDatePicker = true }, accent = true) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            LocalDate.ofEpochDay(dueDate!!).format(dateFormatter),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    IconButton(onClick = { dueDate = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Usuń termin", tint = GlassTextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
            Button(
                onClick = { onSave(title, notes, priority, dueDate) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassAccent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.10f),
                    disabledContentColor = GlassTextSecondary.copy(alpha = 0.6f)
                )
            ) {
                Text(if (isEditing) "Zapisz zmiany" else "Dodaj zadanie", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = dueDate?.let { it * 24L * 60 * 60 * 1000 }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GlassAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = GlassTextPrimary
        )
    }
}

@Composable
private fun PriorityChip(priority: Priority, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) priority.color.copy(alpha = 0.28f)
                else Color.White.copy(alpha = 0.07f)
            )
            .border(
                1.dp,
                if (selected) priority.color else Color.White.copy(alpha = 0.18f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "P${priority.ordinal + 1}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) priority.color else GlassTextSecondary
        )
    }
}

@Composable
private fun GlassPill(
    onClick: () -> Unit,
    accent: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val bg = if (accent) {
        Brush.horizontalGradient(listOf(GlassAccent, GlassAccent.copy(alpha = 0.8f)))
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f))
        )
    }
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = if (accent) 0.35f else 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassTextPrimary,
    unfocusedTextColor = GlassTextPrimary,
    cursorColor = GlassAccent,
    focusedBorderColor = GlassAccent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
    focusedLabelColor = GlassAccent,
    unfocusedLabelColor = GlassTextSecondary,
    focusedContainerColor = Color.White.copy(alpha = 0.06f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
)
