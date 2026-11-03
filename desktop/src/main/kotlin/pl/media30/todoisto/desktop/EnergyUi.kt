package pl.media30.todoisto.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import pl.media30.todoisto.data.Recurrence

/**
 * Buźka energii w pasku (tylko w „Dziś") + krótka chmurka: nagłówek, jedna linia powodów,
 * krótka rada i — gdy energia niska, a są ciężkie zadania — „Ułatw z AI”. Jak na telefonie.
 */
@Composable
fun EnergyFace(st: AppState, ui: UiState) {
    val g = LocalGlass.current
    val lvl = ui.energyLevel ?: return
    Box {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(g.accent.copy(alpha = 0.10f)).clickable { st.energyOpen = true },
            contentAlignment = Alignment.Center
        ) { Text(lvl.emoji, fontSize = 18.sp) }
        if (st.energyOpen) Popup(alignment = Alignment.TopEnd, onDismissRequest = { st.energyOpen = false }, offset = IntOffset(0, 44)) {
            Column(Modifier.width(260.dp).glass(RoundedCornerShape(22.dp), strong = true, elevation = 18.dp).padding(vertical = 10.dp)) {
                Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(lvl.emoji, fontSize = 38.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(EnergyText.headline(lvl), fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Sora)
                }
                val why = EnergyText.why(ui.energyManual, ui.energyReasons)
                if (why.isNotBlank()) Text(why, fontSize = 13.sp, lineHeight = 17.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp))
                val heavy = ui.heavyTasks.size
                Text(EnergyText.advice(lvl, heavy), fontSize = 13.5.sp, lineHeight = 18.sp, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp))
                Column(Modifier.padding(horizontal = 8.dp)) {
                    if (lvl == EnergyLevel.LOW && heavy > 0) GlassMenuItem("Ułatw z AI ($heavy)", Icons.Outlined.AutoAwesome) { st.energyOpen = false; st.easeOpen = true }
                    if (ui.energyManual) GlassMenuItem("Wg danych", Icons.Outlined.Autorenew) { st.energyOpen = false; st.setEnergy(null) }
                    else {
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EnergyLevel.entries.forEach { l ->
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (l == lvl) g.accent.copy(alpha = 0.18f) else g.field)
                                        .clickable { st.energyOpen = false; st.setEnergy(l) }.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(l.emoji, fontSize = 18.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** „Ułatw z AI”: wybierz ciężkie zadanie → AI rozbija je na lekkie kroki. */
@Composable
fun EaseSheet(st: AppState) {
    val g = LocalGlass.current
    val ui = remember(st.rev, st.view, st.activeArea, st.settingsRev) { st.buildUiState() }
    Sheet(onDismiss = { st.easeOpen = false }, maxWidth = 460.dp) {
        Text("Ułatw z AI", fontSize = 17.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Text("Wybierz zadanie — AI rozbije je na małe, lekkie kroki.", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
        Spacer(Modifier.height(10.dp))
        if (ui.heavyTasks.isEmpty()) Text("Dziś nie ma ciężkich zadań.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
        ui.heavyTasks.forEach { t ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(g.field)
                    .clickable { st.easeOpen = false; st.easeWithAi(t) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(t.title, fontSize = 14.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.AutoAwesome, null, tint = g.accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** „Moje rutyny”: wszystkie rutyny w jednym miejscu — dotknięcie otwiera zadanie do edycji. */
@Composable
fun RoutinesEditSheet(st: AppState) {
    val g = LocalGlass.current
    val ui = remember(st.rev, st.activeArea, st.settingsRev) { st.buildUiState() }
    Sheet(onDismiss = { st.routEditOpen = false }, maxWidth = 460.dp) {
        Text("Moje rutyny", fontSize = 17.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Text("Dotknij, żeby zmienić nazwę, cykl albo usunąć.", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
        Spacer(Modifier.height(10.dp))
        if (ui.allRoutines.isEmpty()) Text("Brak rutyn. Dodaj zadanie „codziennie” bez godziny (P3/P4).", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
        ui.allRoutines.forEach { r ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(g.field)
                    .clickable { st.routEditOpen = false; st.openDetail(r.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(r.title, fontSize = 14.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(r.recurrence?.let { Recurrence.fromNameSafe(it)?.label?.lowercase() }.orEmpty(), fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
            }
        }
        Spacer(Modifier.height(10.dp))
        SecondaryBtn("Nowa rutyna", Modifier.fillMaxWidth()) {
            st.routEditOpen = false; st.quickAddText = "codziennie "; st.quickAddOpen = true
        }
    }
}
