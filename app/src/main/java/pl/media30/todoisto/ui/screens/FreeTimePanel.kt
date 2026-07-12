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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
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
import pl.media30.todoisto.data.Suggestion
import pl.media30.todoisto.ui.FreeTimeState
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTint
import pl.media30.todoisto.ui.theme.glass

private fun hm(min: Int) = "%d:%02d".format(min / 60, min % 60)

private fun freeSummary(minutes: Int): String {
    val h = minutes / 60; val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "~${h}h ${m}min wolnego"
        h > 0 -> "~${h}h wolnego"
        else -> "~${m}min wolnego"
    }
}

/** Panel „Czas wolny" — propozycje z puli w wolne okna dnia (rodzina panelu „Tydzień"). */
@Composable
fun FreeTimePanel(
    state: FreeTimeState,
    onAccept: (Suggestion) -> Unit,
    onSwap: (Suggestion) -> Unit,
    onDismiss: (Suggestion) -> Unit,
    onAddFirst: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bolt, null, tint = GlassAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Czas wolny", fontSize = 20.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
            Spacer(Modifier.width(10.dp))
            if (!state.noWindows && !state.poolEmpty)
                Text(freeSummary(state.freeMinutes), fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.poolEmpty -> EmptyHint(
                "Pula jest pusta.",
                "Dodaj pierwszą aktywność, a appka zaproponuje ją w wolnym oknie.",
                "Dodaj aktywność", onAddFirst
            )
            state.noWindows -> EmptyHint("Dziś brak wolnego czasu.", "Wszystkie okna zajęte zaplanowanymi zadaniami.", null, null)
            state.suggestions.isEmpty() -> EmptyHint("Na teraz nic nie pasuje.", "Skróć aktywności lub dodaj krótszy wariant do puli.", null, null)
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.suggestions.forEach { s -> SuggestionCard(s, onAccept, onSwap, onDismiss) }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    s: Suggestion,
    onAccept: (Suggestion) -> Unit,
    onSwap: (Suggestion) -> Unit,
    onDismiss: (Suggestion) -> Unit
) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.activity.effortType.emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.activity.name, fontSize = 16.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                Text(
                    "${hm(s.startMin)} · ${s.activity.durationMinutes} min · ${s.activity.place.label.lowercase()}",
                    fontSize = 12.5.sp, color = GlassTextSecondary
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Dodaj do dziś", accent = true, modifier = Modifier.weight(1.4f)) { onAccept(s) }
            Action("Zamień", accent = false, modifier = Modifier.weight(1f)) { onSwap(s) }
            Action("Nie dziś", accent = false, modifier = Modifier.weight(1f)) { onDismiss(s) }
        }
    }
}

@Composable
private fun Action(label: String, accent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (accent) GlassAccent else GlassTint)
            .bouncy(0.95f, onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.W800, color = if (accent) Color.White else GlassTextPrimary)
    }
}

@Composable
private fun EmptyHint(head: String, sub: String, action: String?, onAction: (() -> Unit)?) {
    Column {
        Text(head, fontSize = 15.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(sub, fontSize = 13.sp, color = GlassTextSecondary)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(GlassAccent).bouncy(0.95f, onAction)
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) { Text(action, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.W800) }
        }
    }
}
