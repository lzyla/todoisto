package pl.media30.todoisto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.glass
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl"))

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .glass(shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(
            checked = task.isCompleted,
            color = task.priority.color,
            onToggle = onToggle
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) GlassTextSecondary.copy(alpha = 0.6f) else GlassTextPrimary
            )
            if (task.notes.isNotBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (task.dueDate != null || task.priority != Priority.P4) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    task.dueDate?.let { epochDay -> DueDateChip(epochDay = epochDay) }
                    if (task.priority != Priority.P4) {
                        Spacer(Modifier.width(8.dp))
                        PriorityTag(priority = task.priority)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean, color: Color, onToggle: () -> Unit) {
    val ring = if (color == Priority.P4.color) MaterialTheme.colorScheme.primary else color
    Row(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(
                    Brush.verticalGradient(listOf(ring, ring.copy(alpha = 0.7f)))
                )
                else Modifier
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(2.dp, ring, CircleShape)
            )
            .clickable(onClick = onToggle),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Ukończone",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DueDateChip(epochDay: Long) {
    val today = LocalDate.now().toEpochDay()
    val date = LocalDate.ofEpochDay(epochDay)
    val (label, tint) = when {
        epochDay < today -> "Zaległe" to Color(0xFFFF6B6B)
        epochDay == today -> "Dzisiaj" to MaterialTheme.colorScheme.primary
        epochDay == today + 1 -> "Jutro" to MaterialTheme.colorScheme.primary
        else -> date.format(dateFormatter) to GlassTextSecondary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun PriorityTag(priority: Priority) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(priority.color.copy(alpha = 0.20f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "P${priority.ordinal + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = priority.color
        )
    }
}
