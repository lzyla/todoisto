package pl.media30.todoisto.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassHair
import pl.media30.todoisto.ui.theme.GlassInputBg
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/** Pole tekstowe wg prototypu: border --hair, tło --inbg, radius 14. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassTextPrimary,
    unfocusedTextColor = GlassTextPrimary,
    cursorColor = GlassAccent,
    focusedBorderColor = GlassAccent,
    unfocusedBorderColor = GlassHair,
    focusedLabelColor = GlassAccent,
    unfocusedLabelColor = GlassTextSecondary,
    focusedPlaceholderColor = GlassTextSecondary.copy(alpha = 0.55f),
    unfocusedPlaceholderColor = GlassTextSecondary.copy(alpha = 0.55f),
    focusedContainerColor = GlassInputBg,
    unfocusedContainerColor = GlassInputBg
)
