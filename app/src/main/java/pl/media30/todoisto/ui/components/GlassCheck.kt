package pl.media30.todoisto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.media30.todoisto.ui.theme.GlassAccent

/**
 * Okrągły checkbox: pierścień w kolorze priorytetu (albo akcent),
 * wypełnia się kolorem + scale-in ptaszka przy zaznaczeniu.
 */
@Composable
fun GlassCheck(
    checked: Boolean,
    ringColor: Color = GlassAccent,
    size: Dp = 23.dp,
    /** Uwydatnij priorytet: delikatne tło + grubszy pierścień w kolorze priorytetu. */
    emphasize: Boolean = false,
    onToggle: () -> Unit,
) {
    val fill by animateColorAsState(
        // Nieodhaczone: przy priorytecie delikatne tło w jego kolorze, inaczej przezroczyste.
        targetValue = when {
            checked -> ringColor
            emphasize -> ringColor.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkFill",
    )
    val borderWidth = if (emphasize) 3.dp else 2.dp
    // „Pop" (przeskalowanie z odbiciem) + rozchodzący się pierścień przy zaznaczeniu.
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(checked) {
        if (checked) {
            burst.snapTo(0f)
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
        }
    }
    LaunchedEffect(checked) {
        if (checked) burst.animateTo(1f, androidx.compose.animation.core.tween(460))
    }
    Box(
        modifier = Modifier
            .size(size)
            // rozbłysk: pierścień wychodzący poza kółko i gasnący
            .drawBehind {
                val b = burst.value
                if (checked && b > 0f && b < 1f) {
                    drawCircle(
                        color = ringColor.copy(alpha = (1f - b) * 0.5f),
                        radius = this.size.minDimension / 2f * (1f + b * 0.9f),
                        style = Stroke(width = 2.dp.toPx() * (1f - b) + 0.5f)
                    )
                }
            }
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(CircleShape)
            .background(fill)
            .border(borderWidth, ringColor, CircleShape)
            .bouncy(scaleDown = 0.8f, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
        }
    }
}
