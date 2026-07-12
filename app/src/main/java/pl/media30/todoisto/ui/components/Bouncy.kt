package pl.media30.todoisto.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState

/**
 * bouncy() — uniwersalny efekt naciśnięcia (podpis dotykowy Todoisto):
 * skala spada przy dotknięciu, spring z powrotem. Stosuj na wszystkich
 * elementach klikalnych (karty 0.97, pigułki 0.9, checkbox 0.8).
 */
fun Modifier.bouncy(scaleDown: Float = 0.95f, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "bouncy",
    )
    this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
