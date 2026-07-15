package pl.media30.todoisto.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/**
 * Wiersz zadania (wariant zen z prototypu): kółko 23px, tytuł 15/500,
 * jednoliniowe metadane 11.5/600, godzina po prawej w akcencie.
 * Leży bezpośrednio na matowej tafli listy, oddzielony włosową linią.
 */
@Composable
fun TaskRowZen(
    task: Task,
    metaLine: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    /** 0..1 — animowane przekreślenie rysowane od lewej (moment ukończenia). */
    strikeProgress: Float = 0f,
    /** Kółko pokazane jako odhaczone zanim baza się zaktualizuje. */
    forceChecked: Boolean = false
) {
    val ring = if (task.priority != Priority.P4) task.priority.color else GlassAccent
    val done = task.isCompleted
    val strikeColor = GlassTextSecondary
    // Kliknięcie i efekt dotyku obsługuje kafelek (taskTile); wiersz jest tylko treścią.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
            .padding(
                start = if (compact) 40.dp else 10.dp,
                end = 10.dp,
                top = if (compact) 9.dp else 13.dp,
                bottom = if (compact) 9.dp else 13.dp
            ),
        verticalAlignment = Alignment.Top
    ) {
        GlassCheck(
            checked = done || forceChecked,
            ringColor = ring,
            size = if (compact) 19.dp else 23.dp,
            onToggle = onToggle
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = if (compact) 13.5.sp else 15.sp,
                fontWeight = FontWeight.W500,
                lineHeight = if (compact) 18.sp else 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done || forceChecked) GlassTextSecondary else GlassTextPrimary,
                modifier = Modifier.padding(top = 1.dp).drawWithContent {
                    drawContent()
                    // Kreska ukończenia rysowana od lewej — jedno pociągnięcie.
                    if (strikeProgress > 0f) {
                        drawLine(
                            color = strikeColor,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width * strikeProgress, size.height / 2f),
                            strokeWidth = 1.6.dp.toPx()
                        )
                    }
                }
            )
            if (metaLine.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    metaLine,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.W600,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else task.dueTimeMinutes?.let { m ->
            Spacer(Modifier.width(8.dp))
            Text(
                "%d:%02d".format(m / 60, m % 60),
                fontSize = 12.sp,
                fontWeight = FontWeight.W800,
                color = GlassAccent,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

/** Buduje jednoliniowe metadane wg prototypu: max 3 chipy + podzadania + ↗domeny. */
fun zenMetaLine(
    dueChip: String?,
    timeShown: Boolean,
    durationMin: Int?,
    recurrenceLabel: String?,
    deadline: String?,
    priority: Priority,
    projectName: String?,
    labelNames: List<String>,
    subDone: Int,
    subTotal: Int,
    linkDomains: List<String>
): String {
    // Priorytet NIE jest tekstem — pokazuje go kolor kółka. Projekt pokazujemy zawsze.
    val chips = mutableListOf<String>()
    dueChip?.let { chips += it }
    if (durationMin != null) chips += "$durationMin min"
    recurrenceLabel?.let { chips += "⟳ $it" }
    deadline?.let { chips += it }
    labelNames.forEach { chips += "@$it" }
    val parts = chips.take(3).toMutableList()
    projectName?.let { parts += "#$it" }
    if (subTotal > 0) parts += "$subDone/$subTotal"
    linkDomains.forEach { parts += "↗ $it" }
    return parts.joinToString("  ·  ")
}
