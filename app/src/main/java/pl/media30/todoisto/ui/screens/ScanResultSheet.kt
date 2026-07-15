package pl.media30.todoisto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTint

/** Arkusz z inteligentną propozycją po zeskanowaniu zdjęcia (lista lub obiekt). */
@Composable
fun ScanResultSheet(
    state: pl.media30.todoisto.ui.TodoViewModel.ScanState,
    onAddTasks: (List<String>) -> Unit,
    onAddSuggestion: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(GlassAccent),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Text("Asystent zdjęcia", fontSize = 18.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
        }
        Spacer(Modifier.height(14.dp))

        when {
            state.loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = GlassAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Analizuję zdjęcie…", fontSize = 13.5.sp, color = GlassTextSecondary)
            }
            state.needsKey -> Text(
                "Dodaj klucz API (Ustawienia → Konto → Asystent AI), aby skanować zdjęcia.",
                fontSize = 13.sp, lineHeight = 19.sp, color = GlassTextSecondary
            )
            state.error != null -> Text(state.error, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFFEB4034))
            else -> {
                if (state.summary.isNotBlank()) {
                    Text(state.summary, fontSize = 13.5.sp, lineHeight = 19.sp, color = GlassTextSecondary)
                    Spacer(Modifier.height(14.dp))
                }
                when {
                    // Rozpoznana lista zadań.
                    state.tasks.isNotEmpty() -> {
                        Text("Znalezione zadania:", fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.tasks.forEach { t ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GlassTint).padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(GlassAccent))
                                    Spacer(Modifier.width(10.dp))
                                    Text(t, fontSize = 13.5.sp, fontWeight = FontWeight.W600, color = GlassTextPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        PrimaryBtn("Dodaj wszystkie (${state.tasks.size})") { onAddTasks(state.tasks) }
                    }
                    // Rozpoznany obiekt (np. książka) — propozycja + plan.
                    state.suggestion.isNotBlank() -> {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassAccent.copy(alpha = 0.12f)).padding(14.dp)) {
                            Text("Propozycja", fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 0.8.sp, color = GlassAccent)
                            Spacer(Modifier.height(4.dp))
                            Text(state.suggestion, fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                            if (state.plan.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(state.plan, fontSize = 13.sp, lineHeight = 19.sp, color = GlassTextSecondary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        PrimaryBtn("Dodaj to zadanie") { onAddSuggestion(state.suggestion, state.plan) }
                    }
                    else -> Text("Nie rozpoznałem nic konkretnego. Spróbuj z bliższym/ostrzejszym zdjęciem.", fontSize = 13.sp, color = GlassTextSecondary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Anuluj", fontSize = 13.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).bouncy(0.96f, onDismiss).padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun PrimaryBtn(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(GlassAccent).bouncy(0.97f, onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) { Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.W800) }
}
