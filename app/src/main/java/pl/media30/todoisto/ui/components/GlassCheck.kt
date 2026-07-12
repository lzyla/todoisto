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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onToggle: () -> Unit,
) {
    val fill by animateColorAsState(
        targetValue = if (checked) ringColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "checkFill",
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, ringColor, CircleShape)
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
