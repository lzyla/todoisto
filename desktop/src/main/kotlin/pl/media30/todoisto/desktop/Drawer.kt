package pl.media30.todoisto.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.EffortType
import java.io.File

/**
 * Szuflada z Androida (`DrawerContent`) jako stały panel boczny: konto, widoki,
 * Pula aktywności, Statystyki, szacowany czas, ★ Ulubione, Projekty, Etykiety,
 * Aktywności, Archiwum, Cele produktywności.
 */
@Composable
fun DrawerPanel(st: AppState) {
    val g = LocalGlass.current
    val ui = remember(st.rev, st.view, st.activeArea, st.settingsRev, st.sort) { st.buildUiState() }
    st.rev; st.settingsRev
    val repo = st.repo
    Column(
        Modifier.width(296.dp).fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp))
            .background(if (g.dark) Color(0xFF1B1530).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Konto i ustawienia
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { st.showSettings = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(st, 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = g.textPrimary)) { append("todoist") }
                    withStyle(SpanStyle(color = g.accent)) { append("o") }
                }, fontSize = 18.sp, fontWeight = FontWeight.W800, fontFamily = Sora)
                Text("Konto i ustawienia", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = g.textSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        // Obszar (jak wybór przestrzeni w Todoist na górze paska bocznego)
        AreaSwitcher(st, Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(10.dp))

        DrawerRow(Icons.Outlined.CalendarToday, "Dzisiaj", ui.todayCount, st.view == AppView.Today) { st.showView(AppView.Today) }
        DrawerRow(Icons.Outlined.DateRange, "Nadchodzące", null, st.view == AppView.Upcoming) { st.showView(AppView.Upcoming) }
        DrawerRow(Icons.Outlined.Inbox, "Skrzynka", ui.inboxCount, st.view == AppView.Inbox) { st.showView(AppView.Inbox) }
        DrawerRow(Icons.Outlined.CheckCircle, "Ukończone", null, st.view == AppView.Completed) { st.showView(AppView.Completed) }
        DrawerRow(Icons.Outlined.Bolt, "Pula aktywności", null, false) { st.showPool = true }
        DrawerRow(Icons.Outlined.Insights, "Statystyki", null, false) { st.showStats = true }

        // Szacowany czas na dziś
        val openCount = st.todayOpen().size
        val est = ui.estTodayMinutes
        val estText = when { est <= 0 -> "—"; est < 60 -> "~$est min"; est % 60 == 0 -> "~${est / 60}h"; else -> "~${est / 60}h ${est % 60}min" }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp).glass(RoundedCornerShape(16.dp)).clickable { st.estimateAi = EstimateAiState(); st.showEstimate = true }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = g.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("SZACOWANY CZAS NA DZIŚ", fontSize = 9.5.sp, fontWeight = FontWeight.W800, color = g.textSecondary, letterSpacing = 1.sp, fontFamily = Manrope)
                Text(if (openCount == 0) "Nic nie zaplanowane" else "$openCount ${plural(openCount, "zadanie", "zadania", "zadań")} · $estText", fontSize = 13.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = g.textSecondary, modifier = Modifier.size(18.dp))
        }

        // Ulubione
        val favP = repo.projects.filter { it.isFavorite && !it.isArchived }
        val favL = repo.labels.filter { it.isFavorite }
        if (favP.isNotEmpty() || favL.isNotEmpty()) {
            Row(Modifier.padding(start = 8.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = Color(0xFFF4B740), modifier = Modifier.size(13.dp)); Spacer(Modifier.width(6.dp))
                SectionHeader("Ulubione", g.textSecondary)
            }
            favP.forEach { p -> DotRow("#${p.name}", Color(p.colorArgb), st.view == AppView.ProjectView(p.id)) { st.showView(AppView.ProjectView(p.id)) } }
            favL.forEach { l -> DotRow("@${l.name}", Color(l.colorArgb), st.view == AppView.LabelView(l.id)) { st.showView(AppView.LabelView(l.id)) } }
        }

        // Projekty (w aktywnym obszarze)
        HeaderWithPlus("Projekty") { st.dialog = DialogKind.NewProject }
        val visibleProjects = repo.projects.filter { !it.isArchived && (st.activeArea == null || it.areaId == st.activeArea) }
        visibleProjects.forEach { p -> DotRow(p.name, Color(p.colorArgb), st.view == AppView.ProjectView(p.id)) { st.showView(AppView.ProjectView(p.id)) } }
        if (visibleProjects.isEmpty()) Hint("Brak projektów — dodaj plusem.")

        // Etykiety
        HeaderWithPlus("Etykiety") { st.dialog = DialogKind.NewLabel }
        if (repo.labels.isEmpty()) Hint("Brak etykiet")
        else FlowLabels(st)

        // Aktywności
        HeaderWithPlus("Aktywności", Icons.Outlined.Bolt) { st.editingActivity = null; st.showActivityForm = true }
        val acts = repo.activities
        if (acts.isEmpty()) Hint("Brak aktywności — dodaj pierwszą plusem.")
        else acts.take(6).forEach { a ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { st.editingActivity = a; st.showActivityForm = true }.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(effortIcon(a.effortType), null, tint = effortColor(a.effortType), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(10.dp))
                Text(a.name, fontSize = 13.sp, fontWeight = FontWeight.W600, color = if (a.isActive) g.textPrimary else g.textSecondary, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${a.durationMinutes}m", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
            }
        }

        // Archiwum
        val archived = repo.projects.filter { it.isArchived }
        if (archived.isNotEmpty()) {
            Row(Modifier.padding(start = 8.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Archive, null, tint = g.textSecondary, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(6.dp))
                SectionHeader("Archiwum", g.textSecondary)
            }
            archived.forEach { p -> DotRow(p.name, Color(p.colorArgb).copy(alpha = 0.5f), st.view == AppView.ProjectView(p.id), dim = true) { st.showView(AppView.ProjectView(p.id)) } }
        }

        Spacer(Modifier.weight(1f)); Spacer(Modifier.height(16.dp))

        // Cele produktywności
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).clickable { st.dialog = DialogKind.Goals }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = g.accent, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(8.dp))
                SectionHeader("Cele produktywności", g.textSecondary, Modifier.weight(1f))
                Icon(Icons.Outlined.Edit, null, tint = g.textSecondary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(10.dp))
            GoalBar("Dzisiaj", ui.doneToday, ui.goalDaily)
            Spacer(Modifier.height(8.dp))
            GoalBar("Tydzień", ui.doneWeek, ui.goalWeekly)
        }
    }
}

@Composable
fun Avatar(st: AppState, size: androidx.compose.ui.unit.Dp) {
    val g = LocalGlass.current
    st.settingsRev
    val path = st.settings.avatarPath
    val bmp = remember(path) { if (path.isBlank()) null else runCatching { javax.imageio.ImageIO.read(File(path)).toComposeImageBitmap() }.getOrNull() }
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFA47CFF), g.accent))), contentAlignment = Alignment.Center) {
        when {
            bmp != null -> Image(bmp, "Zdjęcie profilowe", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            st.settings.userName.isNotBlank() -> Text(st.settings.userName.first().uppercase(), color = Color.White, fontWeight = FontWeight.W800, fontSize = (size.value * 0.42f).sp, fontFamily = Sora)
            else -> Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
private fun DrawerRow(icon: ImageVector, label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) g.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) g.accent else g.textSecondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.5.sp, fontWeight = if (selected) FontWeight.W800 else FontWeight.W600, color = if (selected) g.accent else g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
        if (count != null && count > 0) Box(Modifier.clip(RoundedCornerShape(50)).background(g.accent.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Manrope)
        }
    }
}

@Composable
private fun DotRow(label: String, color: Color, selected: Boolean, dim: Boolean = false, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) g.accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color, 10.dp); Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.5.sp, fontWeight = if (selected) FontWeight.W800 else FontWeight.W600, color = if (dim) g.textSecondary else g.textPrimary, fontFamily = Manrope, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HeaderWithPlus(title: String, icon: ImageVector? = null, onPlus: () -> Unit) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 18.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = g.textSecondary, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(6.dp)) }
        SectionHeader(title, g.textSecondary, Modifier.weight(1f))
        Box(Modifier.size(22.dp).clip(CircleShape).background(g.accent.copy(alpha = 0.14f)).clickable { onPlus() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, "Dodaj", tint = g.accent, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun Hint(text: String) { val g = LocalGlass.current; Text(text, fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp)) }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowLabels(st: AppState) {
    FlowRow(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        st.repo.labels.forEach { l ->
            val c = Color(l.colorArgb); val sel = st.view == AppView.LabelView(l.id)
            Box(Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = if (sel) 0.35f else 0.16f)).clickable { st.showView(AppView.LabelView(l.id)) }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("@${l.name}", fontSize = 12.sp, fontWeight = FontWeight.W700, color = c, fontFamily = Manrope)
            }
        }
    }
}

@Composable
private fun GoalBar(label: String, done: Int, goal: Int) {
    val g = LocalGlass.current
    val frac = if (goal > 0) (done.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.width(62.dp))
        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(50)).background(g.hair)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFA47CFF), g.accent))))
        }
        Spacer(Modifier.width(8.dp))
        Text("$done/$goal", fontSize = 11.sp, fontWeight = FontWeight.W800, color = g.textSecondary, fontFamily = Manrope)
    }
}

fun effortIcon(e: EffortType): ImageVector = when (e) { EffortType.PHYSICAL -> Icons.Outlined.FitnessCenter; EffortType.MENTAL -> Icons.Outlined.Psychology; EffortType.RELAX -> Icons.Outlined.Spa }
fun effortColor(e: EffortType): Color = when (e) { EffortType.PHYSICAL -> Color(0xFFFB7185); EffortType.MENTAL -> Color(0xFF6B8AFF); EffortType.RELAX -> Color(0xFF2DD4BF) }
