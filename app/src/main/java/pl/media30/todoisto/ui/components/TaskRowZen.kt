package pl.media30.todoisto.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/**
 * Wiersz zadania w stylu „Zen" — minimalny, bezramkowy: leży na wspólnej
 * matowej tafli listy (patrz ZenList), oddzielony cienką linią. Kółko po lewej,
 * tytuł, jednoliniowe metadane, godzina po prawej. Kliknięcie wiersza → edycja,
 * kliknięcie kółka → odhaczenie.
 */
@Composable
fun TaskRowZen(
    task: Task,
    metaLine: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    metaColor: Color = GlassTextSecondary,
) {
    val ring = if (task.priority != Priority.P4) task.priority.color else GlassAccent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .bouncy(scaleDown = 0.98f, onClick = onOpen)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
            .padding(
                start = if (compact) 38.dp else 6.dp,
                end = 6.dp,
                top = if (compact) 8.dp else 13.dp,
                bottom = if (compact) 8.dp else 13.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        GlassCheck(
            checked = task.isCompleted,
            ringColor = ring,
            size = if (compact) 19.dp else 23.dp,
            onToggle = onToggle,
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) GlassTextSecondary else GlassTextPrimary,
            )
            if (metaLine.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(metaLine, style = MaterialTheme.typography.labelMedium, color = metaColor)
            }
        }
        task.dueTimeMinutes?.let { m ->
            Spacer(Modifier.width(8.dp))
            Text(
                "%02d:%02d".format(m / 60, m % 60),
                style = MaterialTheme.typography.labelMedium,
                color = GlassAccent,
            )
        }
    }
}
