package pl.media30.todoisto.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalTime

fun main() {
    System.setProperty("apple.awt.application.name", "Todoisto")
    System.setProperty("apple.awt.application.appearance", "system")
    AppIcon.installDockIcon()
    application {
        val state = rememberWindowState(width = 1180.dp, height = 820.dp)
        val holder = remember { AppHolder() }
        Window(onCloseRequest = ::exitApplication, title = "Todoisto", state = state, icon = AppIcon.painter) {
            // Natywny pasek menu macOS ma każda aplikacja (Todoist też) — trzymamy go
            // minimalnym, a cała nawigacja jest w oknie, tak jak na Androidzie.
            MenuBar {
                Menu("Plik", mnemonic = 'P') {
                    Item("Nowe zadanie", shortcut = KeyShortcut(Key.N, meta = true)) { holder.state?.let { it.quickAddOpen = true } }
                    Item("Synchronizuj", shortcut = KeyShortcut(Key.S, meta = true)) { holder.state?.runSync(auto = false) }
                    Item("Ustawienia…", shortcut = KeyShortcut(Key.Comma, meta = true)) { holder.state?.let { it.showSettings = true } }
                    Separator()
                    Item("Zakończ", shortcut = KeyShortcut(Key.Q, meta = true)) { exitApplication() }
                }
                Menu("Widok", mnemonic = 'W') {
                    Item("Dzisiaj", shortcut = KeyShortcut(Key.One, meta = true)) { holder.state?.showView(AppView.Today) }
                    Item("Nadchodzące", shortcut = KeyShortcut(Key.Two, meta = true)) { holder.state?.showView(AppView.Upcoming) }
                    Item("Skrzynka", shortcut = KeyShortcut(Key.Three, meta = true)) { holder.state?.showView(AppView.Inbox) }
                    Item("Ukończone", shortcut = KeyShortcut(Key.Four, meta = true)) { holder.state?.showView(AppView.Completed) }
                    Separator()
                    Item("Pokaż/ukryj menu boczne", shortcut = KeyShortcut(Key.B, meta = true)) { holder.state?.let { it.setDrawer(!it.drawerOpen) } }
                }
            }
            App(holder)
        }
    }
}

/** Uchwyt, przez który natywne menu dociera do stanu aplikacji. */
class AppHolder { var state: AppState? = null }

@Composable
fun App(holder: AppHolder) {
    val scope = rememberCoroutineScope()
    val st = remember { AppState(TaskRepository(), AppSettings(), scope).also { holder.state = it } }
    st.settingsRev // subskrypcja zmian ustawień (motyw, tryb ciemny, tło…)
    val glass = glassFor(st.settings.themeId, st.settings.darkTheme)
    val scheme = if (glass.dark) darkColorScheme(primary = glass.accent, background = glass.bgBottom, surface = glass.surfaceStrong)
                 else lightColorScheme(primary = glass.accent, background = glass.bgBottom, surface = glass.surfaceStrong)

    // Auto-sync: przy starcie i co 2 minuty; po lokalnej zmianie z opóźnieniem 4 s.
    LaunchedEffect(st.settingsRev) {
        if (st.settings.autoSync && st.settings.signedIn) {
            st.runSync(auto = true)
            while (true) { delay(120_000); if (st.settings.autoSync && st.settings.signedIn) st.runSync(auto = true) }
        }
    }
    LaunchedEffect(st.dirty) { if (st.dirty > 0 && st.settings.autoSync && st.settings.signedIn) { delay(4000); st.runSync(auto = true) } }
    LaunchedEffect(st.toast) { if (st.toast != null) { delay(3200); st.toast = null } }
    LaunchedEffect(st.importResult) { st.importResult?.let { st.toast = it; st.importResult = null } }
    // Powiadomienia macOS o przypomnieniach (także z zadań ustawionych na telefonie).
    LaunchedEffect(Unit) {
        var lastCheck = System.currentTimeMillis(); val notified = mutableSetOf<Long>()
        while (true) {
            delay(30_000)
            val now = System.currentTimeMillis()
            st.repo.tasks.forEach { t ->
                val at = t.reminderAt
                if (!t.isCompleted && at != null && at in (lastCheck + 1)..now && t.id !in notified) { DesktopNotifier.notify("Przypomnienie", t.title); notified += t.id }
            }
            lastCheck = now
        }
    }

    CompositionLocalProvider(LocalGlass provides glass) {
        MaterialTheme(colorScheme = scheme) {
            Box(
                Modifier.fillMaxSize().onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        e.key == Key.Escape -> { st.closeTopmost(); true }
                        e.isMetaPressed && e.key == Key.N -> { st.quickAddOpen = true; true }
                        else -> false
                    }
                }
            ) {
                AppBackground(st)
                Row(Modifier.fillMaxSize()) {
                    AnimatedVisibility(st.drawerOpen) { DrawerPanel(st) }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        TopBar(st)
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            TaskListContent(st)
                            RoutinesPanel(st)
                            BottomDock(st)
                            Fab(st)
                            WeekBrief(st)
                        }
                    }
                }
                Overlays(st)
                Toast(st)
                if (!st.splashDone) SplashOverlay { st.splashDone = true }
            }
        }
    }
}

/** Zamyka najwyższą warstwę (klawisz Esc). */
fun AppState.closeTopmost() {
    when {
        dialog != null -> dialog = null
        reschedTask != null -> reschedTask = null
        scan != null -> scan = null
        showSort -> showSort = false
        showScanChooser -> showScanChooser = false
        showEstimate -> showEstimate = false
        showImport -> showImport = false
        showActivityForm -> showActivityForm = false
        showPool -> showPool = false
        freeTime != null -> freeTime = null
        detailTaskId != null -> { detailTaskId = null; dismissAi() }
        quickAddOpen -> { quickAddOpen = false; voiceHint = false }
        briefOpen -> briefOpen = false
        routOpen -> routOpen = false
        showAccount -> showAccount = false
        showStats -> showStats = false
        showSettings -> showSettings = false
    }
}

// ─── Tło: gradient mesh / scena wg pory dnia / własne zdjęcie ───────────────
@Composable
private fun AppBackground(st: AppState) {
    val g = LocalGlass.current
    st.settingsRev
    val custom = st.settings.activeCustomBg
    val bmp: ImageBitmap? = remember(custom) {
        if (custom.isBlank()) null else runCatching { javax.imageio.ImageIO.read(File(custom)).toComposeImageBitmap() }.getOrNull()
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(g.bgTop, g.bgBottom))))
    when {
        bmp != null -> {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(if (g.dark) Color(0x66000000) else Color(0x33FFFFFF)))
        }
        st.settings.photoBackground -> {
            val h = LocalTime.now().hour
            val colors = when {
                h < 11 -> listOf(Color(0xFFFFE3C4), Color(0xFFF7C1D9), g.bgBottom)
                h < 17 -> listOf(Color(0xFFCFE7FF), Color(0xFFE9E1FF), g.bgBottom)
                else -> listOf(Color(0xFF3B2A6B), Color(0xFF7A4CB6), Color(0xFF1B1530))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
        }
        else -> {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.accent.copy(alpha = if (g.dark) 0.35f else 0.28f), Color.Transparent), center = Offset(200f, 120f), radius = 900f)))
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.tint.copy(alpha = if (g.dark) 0.5f else 0.9f), Color.Transparent), center = Offset(1400f, 900f), radius = 1100f)))
        }
    }
}

// ─── Pasek górny jak w Androidzie: ☰ · Obszar ▾ · … · ✦ · ⋮ ─────────────────
@Composable
private fun TopBar(st: AppState) {
    val g = LocalGlass.current
    var menuOpen by remember { mutableStateOf(false) }
    val ui = remember(st.rev, st.view, st.sort, st.activeArea, st.settingsRev) { st.buildUiState() }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircleGlassButton(Icons.Filled.Menu, "Menu") { st.setDrawer(!st.drawerOpen) }
        AreaSwitcher(st)
        if (st.syncing) Text("☁ synchronizuję…", fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
        Spacer(Modifier.weight(1f))
        CircleGlassButton(Icons.Outlined.AutoAwesome, "Podsumowanie tygodnia", tint = g.accent) { st.briefOpen = !st.briefOpen }
        Box {
            CircleGlassButton(Icons.Filled.MoreVert, "Więcej") { menuOpen = true }
            if (menuOpen) Popup(alignment = Alignment.TopEnd, onDismissRequest = { menuOpen = false }, offset = IntOffset(0, 48)) {
                Column(Modifier.width(250.dp).glass(RoundedCornerShape(22.dp), strong = true, elevation = 18.dp).padding(8.dp)) {
                    GlassMenuItem("Sortowanie: ${st.sort.label}", Icons.Outlined.Autorenew) { menuOpen = false; st.showSort = true }
                    GlassMenuItem("Skanuj kartkę", Icons.Outlined.PhotoCamera) { menuOpen = false; st.showScanChooser = true }
                    GlassMenuItem("Czas wolny — propozycja", Icons.Outlined.Bolt) { menuOpen = false; st.suggestFreeTime() }
                    GlassMenuItem("Kopiuj plan dnia", Icons.Outlined.ContentCopy) { menuOpen = false; copyToClipboard(buildDayPlanText(st.repo)); st.toast = "Plan dnia skopiowany do schowka" }
                    ui.currentProject?.let { p ->
                        GlassMenuDivider()
                        GlassMenuItem(if (p.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych", Icons.Outlined.Star) { menuOpen = false; st.toggleProjectFavorite(p.id) }
                        GlassMenuItem("Dodaj sekcję", Icons.Filled.Add) { menuOpen = false; st.dialog = DialogKind.NewSection(p.id) }
                        GlassMenuItem("Duplikuj projekt", Icons.Outlined.ContentCopy) { menuOpen = false; st.duplicateProject(p.id) }
                        GlassMenuItem(if (p.isArchived) "Przywróć z archiwum" else "Archiwizuj projekt", Icons.Outlined.Archive) { menuOpen = false; st.archiveProject(p.id, !p.isArchived) }
                        GlassMenuItem("Usuń projekt", Icons.Outlined.Delete, danger = true) { menuOpen = false; st.deleteProject(p.id) }
                    }
                    ui.currentLabel?.let { l ->
                        GlassMenuDivider()
                        GlassMenuItem(if (l.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych", Icons.Outlined.Star) { menuOpen = false; st.toggleLabelFavorite(l.id) }
                        GlassMenuItem("Usuń etykietę", Icons.Outlined.Delete, danger = true) { menuOpen = false; st.deleteLabel(l.id) }
                    }
                }
            }
        }
    }
}

fun copyToClipboard(text: String) {
    runCatching { java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null) }
}

@Composable
fun GlassMenuItem(text: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    val g = LocalGlass.current
    val c = if (danger) g.danger else g.accent
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, c, 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.W700, color = if (danger) g.danger else g.textPrimary, fontFamily = Manrope)
    }
}

@Composable
fun GlassMenuDivider() { val g = LocalGlass.current; Box(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp).height(1.dp).background(g.hair)) }

/** Przełącznik obszaru („Wszystko" / obszary / + Nowy obszar) — jak `AreaSwitcher`. */
@Composable
private fun AreaSwitcher(st: AppState) {
    val g = LocalGlass.current
    var open by remember { mutableStateOf(false) }
    st.rev
    val active = st.repo.area(st.activeArea)
    Box {
        Row(
            Modifier.height(42.dp).glass(RoundedCornerShape(50), strong = true).clickable { open = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dot(active?.let { Color(it.colorArgb) } ?: g.accent)
            Spacer(Modifier.width(8.dp))
            Text(active?.name ?: "Wszystko", fontSize = 12.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = g.textSecondary, modifier = Modifier.size(18.dp))
        }
        if (open) Popup(onDismissRequest = { open = false }, offset = IntOffset(0, 48)) {
            Column(Modifier.width(232.dp).glass(RoundedCornerShape(20.dp), strong = true, elevation = 18.dp).padding(8.dp)) {
                AreaMenuItem("Wszystko", null, st.activeArea == null) { open = false; st.selectArea(null) }
                st.repo.areas.forEach { a -> AreaMenuItem(a.name, Color(a.colorArgb), st.activeArea == a.id) { open = false; st.selectArea(a.id) } }
                GlassMenuDivider()
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { open = false; st.dialog = DialogKind.NewArea }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, null, tint = g.accent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                    Text("Nowy obszar", fontSize = 13.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Manrope)
                }
            }
        }
    }
}

@Composable
private fun AreaMenuItem(name: String, dot: Color?, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) g.accent.copy(alpha = 0.12f) else Color.Transparent).clickable { onClick() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Dot(dot ?: g.accent); Spacer(Modifier.width(10.dp))
        Text(name, fontSize = 13.5.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = g.accent, modifier = Modifier.size(16.dp))
    }
}

// ─── Dock na dole: Rutyny · Dziś · Nadchodz. · Mikrofon ─────────────────────
@Composable
private fun BoxScope.BottomDock(st: AppState) {
    val ui = remember(st.rev, st.view, st.activeArea) { st.buildUiState() }
    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).glass(RoundedCornerShape(26.dp), strong = true, elevation = 16.dp).padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val routIcon = if (ui.routines.isEmpty() && ui.routinesDone > 0) Icons.Outlined.CheckCircle else Icons.Outlined.Autorenew
        val total = ui.doneToday + ui.todayCount
        val frac = if (total > 0) ui.doneToday.toFloat() / total else 0f
        DockItem(routIcon, "Rutyny", selected = st.routOpen, badge = ui.routines.size, ring = if (st.view == AppView.Today) frac else null) { st.routOpen = !st.routOpen }
        DockItem(Icons.Outlined.CalendarToday, "Dziś", selected = st.view == AppView.Today) { st.showView(AppView.Today) }
        DockItem(Icons.Outlined.DateRange, "Nadchodz.", selected = st.view == AppView.Upcoming) { st.showView(AppView.Upcoming) }
        DockItem(Icons.Filled.Mic, "Mikrofon", selected = st.voiceHint) { st.voiceHint = true; st.quickAddOpen = true }
    }
}

@Composable
private fun DockItem(icon: ImageVector, label: String, selected: Boolean, badge: Int = 0, ring: Float? = null, onClick: () -> Unit) {
    val g = LocalGlass.current
    val color = if (selected) g.accent else g.textSecondary
    Column(Modifier.width(72.dp).clip(RoundedCornerShape(18.dp)).clickable { onClick() }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (ring != null && ring > 0f) Canvas(Modifier.size(28.dp)) {
                drawArc(if (ring >= 1f) Color(0xFFF4B740) else g.accent, -90f, 360f * ring, false, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            Icon(icon, label, tint = color, modifier = Modifier.size(19.dp))
            if (badge > 0) Box(Modifier.align(Alignment.TopEnd).size(15.dp).clip(CircleShape).background(g.accent).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Text("$badge", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.W800)
            }
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.W800, color = color, fontFamily = Manrope, maxLines = 1)
    }
}

// ─── FAB + panel szybkiego dodawania ────────────────────────────────────────
@Composable
private fun BoxScope.Fab(st: AppState) {
    val g = LocalGlass.current
    if (!st.quickAddOpen) Box(
        Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 92.dp).size(64.dp)
            .shadow(20.dp, CircleShape, ambientColor = g.accent, spotColor = g.accent).clip(CircleShape).background(g.accent).clickable { st.quickAddOpen = true },
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Filled.Add, "Dodaj zadanie", tint = Color.White, modifier = Modifier.size(30.dp)) }
    if (st.quickAddOpen) QuickAddPanel(st)
}

// ─── Nakładki: arkusze, dialogi, pełne ekrany ───────────────────────────────
@Composable
private fun Overlays(st: AppState) {
    if (st.showSettings) SettingsScreen(st)
    if (st.showAccount) AccountScreen(st)
    if (st.showStats) StatsScreen(st)
    st.detailTaskId?.let { id -> st.rev; val t = st.repo.task(id); if (t != null) TaskDetailSheet(st, t) else st.detailTaskId = null }
    if (st.showSort) SortSheet(st)
    if (st.showScanChooser) ScanChooserSheet(st)
    st.scan?.let { ScanResultSheet(st, it) }
    if (st.showEstimate) EstimateSheet(st)
    if (st.showPool) ActivityPoolSheet(st)
    if (st.showActivityForm) ActivityFormSheet(st)
    st.freeTime?.let { FreeTimeSheet(st, it) }
    if (st.showImport) ImportActivitiesDialog(st)
    st.reschedTask?.let { ReschedSheet(st, it) }
    st.dialog?.let { AppDialog(st, it) }
}

@Composable
private fun BoxScope.Toast(st: AppState) {
    val g = LocalGlass.current
    AnimatedVisibility(st.toast != null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        Box(Modifier.glass(RoundedCornerShape(50), strong = true, elevation = 14.dp).padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text(st.toast ?: "", fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = g.textPrimary, fontFamily = Manrope)
        }
    }
}

/** Ekran startowy jak na telefonie: gradient + logo + „Todoisto". */
@Composable
private fun SplashOverlay(onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (shown) 1f else 0.82f, tween(500))
    val alpha by animateFloatAsState(if (leaving) 0f else 1f, tween(380))
    LaunchedEffect(Unit) { shown = true; delay(980); leaving = true; delay(400); onDone() }
    Box(Modifier.fillMaxSize().alpha(alpha).background(Brush.linearGradient(listOf(Color(0xFFB558F6), Color(0xFF5B2BE0)))).clickable(indication = null, interactionSource = null) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(AppIcon.foregroundPainter, null, Modifier.size(168.dp).scale(scale))
            Text("Todoisto", fontSize = 26.sp, fontWeight = FontWeight.W800, color = Color.White, fontFamily = Sora)
        }
    }
}
