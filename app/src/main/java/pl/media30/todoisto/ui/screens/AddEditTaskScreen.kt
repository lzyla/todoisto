package pl.media30.todoisto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edytuj zadanie" else "Nowe zadanie",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Zamknij",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (isEditing && onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Usuń",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notatki (opcjonalnie)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(icon = { Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size18()) }, text = "Priorytet")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text("P${p.ordinal + 1}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = p.color.copy(alpha = 0.18f),
                            selectedLabelColor = p.color
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel(icon = { Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size18()) }, text = "Termin")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        dueDate?.let { LocalDate.ofEpochDay(it).format(dateFormatter) }
                            ?: "Ustaw termin"
                    )
                }
                if (dueDate != null) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { dueDate = null }) { Text("Wyczyść") }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSave(title, notes, priority, dueDate) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
private fun SectionLabel(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), fontWeight = FontWeight.SemiBold)
    }
}

private fun Modifier.size18(): Modifier = this.then(Modifier.height(18.dp).width(18.dp))
