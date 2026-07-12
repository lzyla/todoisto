package pl.media30.todoisto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassRoutine
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTheme

private val pillBg: Color get() = if (GlassTheme.dark) Color(0xCC574D75) else Color(0xE6F8F6FB)
private val pillBorder: Color get() = if (GlassTheme.dark) Color(0xFF6A5F8A) else Color(0xFFD6D0E2)

/**
 * Zawartość arkusza Rutyn (otwieranego kółkiem przy docku). Nawyki jako
 * pigułki na przydymionej tafli [GlassRoutine] — celowo inny tryb niż zadania.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesSheetContent(
    routines: List<Task>,
    doneCount: Int,
    onComplete: (Task) -> Unit,
    onAllDone: () -> Unit = {},
) {
    val total = routines.size + doneCount
    val allDone = routines.isEmpty()
    LaunchedEffect(allDone) { if (allDone && total > 0) { delay(900); onAllDone() } }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (allDone) "✅ Rutyny zrobione" else "🔁 Rutyny",
                style = MaterialTheme.typography.titleLarge,
                color = GlassTextPrimary,
            )
            Spacer(Modifier.width(12.dp))
            if (!allDone) {
                Text(
                    "$doneCount z $total zrobione",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassTextSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassRoutine)
                .padding(14.dp)
        ) {
            if (allDone) {
                Text(
                    "Wszystkie nawyki na dziś odhaczone 💜",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextSecondary,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
}

/**
 * Pigułka nawyku: ring + nazwa. Odhaczenie gra własną animację (sprężysty
 * „pop" ringu i skurczenie pigułki) — subtelnie inną niż checkbox zadań.
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
        label = "ringPop",
    )

    AnimatedVisibility(
        visible = !leaving,
        exit = scaleOut(targetScale = 0.6f) + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillBg)
                .border(1.dp, pillBorder, RoundedCornerShape(50))
                .bouncy(scaleDown = 0.88f) { leaving = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                color = GlassTextPrimary,
            )
        }
    }
}
