package pl.media30.todoisto.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassPanelTint
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/** Text-field colours tuned for the translucent glass surfaces. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassTextPrimary,
    unfocusedTextColor = GlassTextPrimary,
    cursorColor = GlassAccent,
    focusedBorderColor = GlassAccent,
    unfocusedBorderColor = GlassPanelTint.copy(alpha = 0.40f),
    focusedLabelColor = GlassAccent,
    unfocusedLabelColor = GlassTextSecondary,
    focusedPlaceholderColor = GlassTextSecondary,
    unfocusedPlaceholderColor = GlassTextSecondary,
    focusedContainerColor = GlassPanelTint.copy(alpha = 0.10f),
    unfocusedContainerColor = GlassPanelTint.copy(alpha = 0.06f)
)
