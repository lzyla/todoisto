package pl.media30.todoisto.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

/**
 * bouncy() — podpis dotykowy Todoisto w duchu Apple Liquid Glass:
 * przy naciśnięciu tafla minimalnie się kurczy (scale), delikatnie „ugina"
 * (rotationX ~0.6°, perspektywa) i lekko przygasa; klik daje 1 impuls haptyczny.
 * Domyślna skala .985 zgodnie ze specyfikacją (karty), mocniejsza dla drobnych
 * elementów (pigułki 0.9, checkbox 0.8).
 */
fun Modifier.bouncy(scaleDown: Float = 0.985f, onClick: () -> Unit): Modifier = composed {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "bScale",
    )
    val tilt by animateFloatAsState(
        targetValue = if (pressed) 0.6f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "bTilt",
    )
    val dim by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bDim",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = tilt
            alpha = dim
            cameraDistance = 12f * density
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
        )
}
