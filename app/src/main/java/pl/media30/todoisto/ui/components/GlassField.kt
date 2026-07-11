package pl.media30.todoisto.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/** Text-field colours tuned for the translucent glass surfaces. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassTextPrimary,
    unfocusedTextColor = GlassTextPrimary,
    cursorColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = GlassTextSecondary,
    focusedPlaceholderColor = GlassTextSecondary,
    unfocusedPlaceholderColor = GlassTextSecondary,
    focusedContainerColor = Color.White.copy(alpha = 0.16f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.10f)
)
