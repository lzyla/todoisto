package pl.media30.todoisto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassPanelTint
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
    labels: List<Label>,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtaskDone: Int = 0,
    subtaskTotal: Int = 0,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(if (compact) 22.dp else 28.dp)
    val base = if (compact) {
        modifier
            .clip(shape)
            .background(GlassPanelTint.copy(alpha = 0.10f))
            .border(1.dp, GlassPanelTint.copy(alpha = 0.30f), shape)
    } else {
        modifier.glass(shape = shape)
    }
    Row(
        modifier = base
            .bouncy(scaleDown = 0.97f, onClick = onClick)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(
            checked = task.isCompleted,
            color = task.priority.color,
            size = if (compact) 22.dp else 26.dp,
            onToggle = onToggle
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) GlassTextSecondary.copy(alpha = 0.6f) else GlassTextPrimary
            )
            if (task.notes.isNotBlank() && !compact) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val hasMeta = task.dueDate != null || task.deadline != null ||
                task.recurrence != null || task.priority != Priority.P4 ||
                labels.isNotEmpty() || subtaskTotal > 0
            if (hasMeta && !compact) {
                Spacer(Modifier.height(8.dp))
                MetaRow(task, labels, subtaskDone, subtaskTotal)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MetaRow(task: Task, labels: List<Label>, subtaskDone: Int, subtaskTotal: Int) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.wrapContentHeight()
    ) {
        task.dueDate?.let { Chip(dueLabel(it), dueTint(it), Icons.Outlined.CalendarToday) }
        if (task.recurrence != null) Chip("Cykl", GlassAccent, Icons.Outlined.Repeat)
        task.deadline?.let { Chip("do " + LocalDate.ofEpochDay(it).format(dateFormatter), Color(0xFFB33B00), Icons.Outlined.Flag) }
        if (task.priority != Priority.P4) Chip("P${task.priority.ordinal + 1}", task.priority.color, null)
        if (subtaskTotal > 0) Chip("$subtaskDone/$subtaskTotal", GlassTextSecondary, null)
        labels.forEach { Chip(it.name, Color(it.colorArgb), Icons.Outlined.Sell) }
    }
}

@Composable
private fun Chip(text: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun CheckCircle(checked: Boolean, color: Color, size: androidx.compose.ui.unit.Dp, onToggle: () -> Unit) {
    val ring = if (color == Priority.P4.color) GlassAccent else color
    val fill by animateColorAsState(
        targetValue = if (checked) ring else ring.copy(alpha = 0.08f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkFill"
    )
    Row(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, ring, CircleShape)
            .bouncy(scaleDown = 0.8f, onClick = onToggle),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Ukończone",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun dueLabel(epochDay: Long): String {
    val today = LocalDate.now().toEpochDay()
    return when {
        epochDay < today -> "Zaległe"
        epochDay == today -> "Dzisiaj"
        epochDay == today + 1 -> "Jutro"
        else -> LocalDate.ofEpochDay(epochDay).format(dateFormatter)
    }
}

@Composable
private fun dueTint(epochDay: Long): Color {
    val today = LocalDate.now().toEpochDay()
    return when {
        epochDay < today -> Color(0xFFB3261E)
        epochDay <= today + 1 -> GlassAccent
        else -> GlassTextSecondary
    }
}
