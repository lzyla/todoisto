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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.QuickAddParser
import java.time.LocalDate

// ─── Paleta (spójna z wersją mobilną) ────────────────────────────────────────
private val Accent = Color(0xFF6B3FE0)
private val TextPrimary = Color(0xFF241844)
private val TextSecondary = Color(0xFF6E5F93)
private val BgTop = Color(0xFFEDE6FA)
private val BgBottom = Color(0xFFDCE4FB)

fun main() = application {
    val state = rememberWindowState(width = 460.dp, height = 780.dp)
    Window(onCloseRequest = ::exitApplication, title = "Todoisto", state = state) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Accent)) {
            App()
        }
    }
}

@Composable
private fun App() {
    val repo = remember { TaskRepository().also(::seedDemo) }
    var tick by remember { mutableStateOf(0) }           // prosty odświeżacz stanu
    val tasks = remember(tick) { repo.tasks }
    var input by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(26.dp))
            Text("Dzisiaj", fontSize = 30.sp, fontWeight = FontWeight.W800, color = TextPrimary)
            Text(dateCaption(), fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(18.dp))

            // Szybkie dodawanie (reużywa QuickAddParser z wersji mobilnej)
            QuickAdd(
                text = input,
                onText = { input = it },
                onSubmit = {
                    val p = QuickAddParser().parse(input)
                    if (p.title.isNotBlank()) {
                        repo.add(
                            Task(
                                id = 0, title = p.title, priority = p.priority,
                                dueDate = p.dueDate, dueTimeMinutes = p.dueTimeMinutes,
                                durationMinutes = p.durationMinutes, recurrence = p.recurrence,
                                projectName = p.projectName, labelNames = p.labelNames
                            )
                        )
                        input = ""; tick++
                    }
                }
            )
            Spacer(Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(tasks, key = { it.id }) { task ->
                    TaskRow(task, onToggle = { repo.toggle(task.id); tick++ })
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun QuickAdd(text: String, onText: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text, onValueChange = onText,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Dodaj zadanie…  (np. Zadzwonić jutro o 15 #Praca p1)", fontSize = 13.sp) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
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
private fun TaskRow(task: Task, onToggle: () -> Unit) {
    val ring = if (task.priority != Priority.P4) task.priority.color else Accent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Kółko odhaczania — pierścień w kolorze priorytetu
        Box(
            Modifier.size(23.dp).clip(CircleShape)
                .background(if (task.isCompleted) ring else Color.Transparent)
                .border(if (task.priority != Priority.P4) 3.dp else 2.dp, ring, CircleShape)
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
            val meta = metaLine(task)
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.priority != Priority.P4) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(task.priority.color))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(meta, fontSize = 11.5.sp, fontWeight = FontWeight.W600, color = TextSecondary)
                }
            }
        }
        task.dueTimeMinutes?.let { m ->
            Text("%d:%02d".format(m / 60, m % 60), fontSize = 12.sp, fontWeight = FontWeight.W800, color = Accent)
        }
    }
}

private fun metaLine(task: Task): String {
    val parts = mutableListOf<String>()
    task.durationMinutes?.let { parts += if (it < 60) "$it min" else "${it / 60}h" }
    task.recurrence?.let {
        parts += when (it) {
            pl.media30.todoisto.data.Recurrence.DAILY -> "codziennie"
            pl.media30.todoisto.data.Recurrence.WEEKLY -> "co tydzień"
            pl.media30.todoisto.data.Recurrence.MONTHLY -> "co miesiąc"
            pl.media30.todoisto.data.Recurrence.YEARLY -> "co rok"
        }
    }
    task.projectName?.let { parts += "#$it" }
    task.labelNames.forEach { parts += "@$it" }
    return parts.joinToString("  ·  ")
}

private val MS = listOf("stycznia","lutego","marca","kwietnia","maja","czerwca","lipca","sierpnia","września","października","listopada","grudnia")
private fun dateCaption(): String {
    val d = LocalDate.now()
    return "${d.dayOfMonth} ${MS[d.monthValue - 1]}"
}
