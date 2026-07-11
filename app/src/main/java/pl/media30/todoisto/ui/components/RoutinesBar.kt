package pl.media30.todoisto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTheme

// Muted, neutral shades — deliberately desaturated and distinct from both the
// glass task cards and the priority colors: the "routines zone" reads as a
// different mode before you read a single word.
private val zoneBg: Color get() = if (GlassTheme.dark) Color(0xD94A4066) else Color(0xD9ECE9F2)
private val zoneBorder: Color get() = if (GlassTheme.dark) Color(0xFF6A5F8A) else Color(0xFFD6D0E2)
private val pillBg: Color get() = if (GlassTheme.dark) Color(0xCC574D75) else Color(0xE6F8F6FB)

/**
 * Collapsed-by-default routines zone docked under the Today list.
 * Habit loop, not obligation: pills instead of task rows, its own check
 * animation, live "n z m" counter, auto-collapse when everything is done.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesBar(
    routines: List<Task>,
    doneCount: Int,
    onComplete: (Task) -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    val total = routines.size + doneCount
    if (total == 0) return
    val allDone = routines.isEmpty()

    var expanded by remember { mutableStateOf(initiallyExpanded) }
    // Close the loop without user action once the last routine is checked off.
    LaunchedEffect(allDone) { if (allDone) expanded = false }

    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = Color(0x40352359), ambientColor = Color(0x26352359))
            .clip(RoundedCornerShape(26.dp))
            .background(zoneBg)
            .border(1.dp, zoneBorder, RoundedCornerShape(26.dp))
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bouncy(scaleDown = 0.98f) { if (!allDone) expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (allDone) "✅ Rutyny zrobione" else "🔁 Rutyny",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GlassTextPrimary
            )
            Spacer(Modifier.width(10.dp))
            if (!allDone) {
                Text(
                    text = "$doneCount z $total zrobione",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassTextSecondary
                )
            }
            Spacer(Modifier.weight(1f))
            if (!allDone) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                    tint = GlassTextSecondary,
                    modifier = Modifier.rotate(chevron)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && !allDone,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                routines.forEach { task ->
                    androidx.compose.runtime.key(task.id) {
                        RoutinePill(task = task, onComplete = { onComplete(task) })
                    }
                }
            }
        }
    }
}

/**
 * A habit pill: ring + name. Checking it plays its own celebration — the ring
 * fills with a springy pop and the whole pill shrinks away — subtly different
 * from the task-card checkbox on purpose.
 */
@Composable
private fun RoutinePill(task: Task, onComplete: () -> Unit) {
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(leaving) {
        if (leaving) {
            delay(320)
            onComplete()
        }
    }
    val ringScale by animateFloatAsState(
        targetValue = if (leaving) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium),
        label = "ringPop"
    )

    AnimatedVisibility(
        visible = !leaving,
        exit = scaleOut(targetScale = 0.6f) + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillBg)
                .border(1.dp, zoneBorder, RoundedCornerShape(50))
                .bouncy(scaleDown = 0.88f) { leaving = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size((16 * ringScale).dp)
                    .clip(CircleShape)
                    .then(
                        if (leaving) Modifier.background(GlassTextSecondary)
                        else Modifier.border(2.dp, GlassTextSecondary, CircleShape)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.labelMedium,
                color = GlassTextPrimary
            )
        }
    }
}
