package pl.media30.todoisto.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassDockBg
import pl.media30.todoisto.ui.theme.GlassRoutinePill
import pl.media30.todoisto.ui.theme.GlassShadow
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTint

/**
 * Tafla Rutyn dokowana nad dolnym dockiem (wg prototypu): przydymione tło,
 * nagłówek ⟳ Rutyny + licznik + strzałka zwijania, pigułki nawyków.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesPanel(
    routines: List<Task>,
    doneCount: Int,
    onComplete: (Task) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = routines.size + doneCount
    val allDone = routines.isEmpty()
    LaunchedEffect(allDone) { if (allDone && total > 0) { delay(600); onCollapse() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(GlassDockBg)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 13.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Autorenew, null, tint = GlassAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(9.dp))
            Text("Rutyny", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
            Spacer(Modifier.width(9.dp))
            Text(
                if (allDone) "wszystkie zrobione" else "$doneCount z $total zrobione",
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
                color = GlassTextSecondary,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(GlassTint.copy(alpha = 0.0f))
                    .bouncy(0.85f, onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, "Zwiń", tint = GlassTextSecondary, modifier = Modifier.size(15.dp))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            routines.forEach { task ->
                androidx.compose.runtime.key(task.id) {
                    RoutinePill(task) { onComplete(task) }
                }
            }
            if (allDone) {
                Text("Nawyki na dziś odhaczone 💜", fontSize = 12.5.sp, color = GlassTextSecondary)
            }
        }
    }
}

/**
 * Pigułka rutyny — dwufazowa animacja z prototypu: dotknięcie wypełnia ring
 * akcentem (pop ×1.25, 340ms), potem cała pigułka kurczy się i znika (760ms).
 */
@Composable
private fun RoutinePill(task: Task, onComplete: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(phase) {
        when (phase) {
            1 -> { delay(340); phase = 2 }
            2 -> { delay(420); onComplete() }
        }
    }
    val ringScale by animateFloatAsState(
        if (phase == 1) 1.25f else 1f,
        spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessMedium),
        label = "ringPop"
    )
    val pillScale by animateFloatAsState(if (phase == 2) 0.6f else 1f, label = "pillSc")
    val pillAlpha by animateFloatAsState(if (phase == 2) 0f else 1f, label = "pillOp")

    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = pillScale; scaleY = pillScale; alpha = pillAlpha }
            .clip(RoundedCornerShape(50))
            .background(GlassRoutinePill)
            .bouncy(0.9f) { if (phase == 0) phase = 1 }
            .padding(start = 9.dp, end = 15.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(18.dp)
                .scale(ringScale)
                .clip(CircleShape)
                .then(
                    if (phase > 0) Modifier.background(GlassAccent)
                    else Modifier.border(2.5.dp, GlassAccent, CircleShape)
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(task.title, fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary)
    }
}
