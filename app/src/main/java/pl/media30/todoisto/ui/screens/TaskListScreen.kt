package pl.media30.todoisto.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.PaletteColors
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.AppView
import pl.media30.todoisto.ui.QuickAddOverrides
import pl.media30.todoisto.ui.SortMode
import pl.media30.todoisto.ui.TaskNode
import pl.media30.todoisto.ui.TodoUiState
import pl.media30.todoisto.ui.components.GlassCheck
import pl.media30.todoisto.ui.components.RoutinesPanel
import pl.media30.todoisto.ui.components.TaskRowZen
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.zenMetaLine
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassDockBg
import pl.media30.todoisto.ui.theme.GlassDrawerBg
import pl.media30.todoisto.ui.theme.GlassFill
import pl.media30.todoisto.ui.theme.GlassHair
import pl.media30.todoisto.ui.theme.GlassInputBg
import pl.media30.todoisto.ui.theme.GlassRim
import pl.media30.todoisto.ui.theme.GlassShadow
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.GlassTheme
import pl.media30.todoisto.ui.theme.GlassTint
import pl.media30.todoisto.ui.theme.LocalHazeState
import pl.media30.todoisto.ui.theme.Sora
import pl.media30.todoisto.ui.theme.controlCenterGlass
import pl.media30.todoisto.ui.theme.glass
import pl.media30.todoisto.ui.theme.glassBlur
import pl.media30.todoisto.ui.theme.taskTile
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val PL = Locale.forLanguageTag("pl")
private val EASE = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val MS = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")

private fun weekdayName(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(JTextStyle.FULL, PL)

private fun dateCaption(date: LocalDate): String {
    val month = date.month.getDisplayName(JTextStyle.FULL, PL)
    // dopełniacz: lipiec → lipca (przybliżenie przez format "d MMMM" locale PL)
    return "${weekdayName(date)}, " + java.time.format.DateTimeFormatter.ofPattern("d MMMM", PL).format(date)
}

private fun shortDate(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return "${d.dayOfMonth} ${MS[d.monthValue - 1]}"
}

@Composable
fun TaskListScreen(
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>,
    isDarkTheme: Boolean,
    quickAddPrefill: String?,
    onPrefillConsumed: () -> Unit,
    onSelectView: (AppView) -> Unit,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onQuickAdd: (String, QuickAddOverrides) -> Unit,
    onAddProject: (String, Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onDuplicateProject: (Long) -> Unit,
    onArchiveProject: (Long, Boolean) -> Unit,
    onToggleProjectFavorite: (Long) -> Unit,
    onToggleLabelFavorite: (Long) -> Unit,
    onDeleteLabel: (Long) -> Unit,
    onAddLabel: (String, Long) -> Unit,
    onAddSection: (Long, String) -> Unit,
    onClearCompleted: () -> Unit,
    onSort: (SortMode) -> Unit,
    onSetGoals: (Int, Int) -> Unit,
    onToggleTheme: () -> Unit,
    onMoveOverdueToToday: () -> Unit = {},
    onOpenActivityPool: () -> Unit = {},
    onFreeTime: () -> Unit = {},
    isPhotoBackground: Boolean = false,
    onTogglePhotoBackground: () -> Unit = {},
    activities: List<pl.media30.todoisto.data.Activity> = emptyList(),
    onAddActivityQuick: () -> Unit = {},
    onSharePlan: () -> Unit = {},
    onDeferToTomorrow: (Task) -> Unit = {},
    onOpenAutomation: (Task) -> Unit = {},
    areas: List<pl.media30.todoisto.data.Area> = emptyList(),
    activeAreaId: Long? = null,
    onSelectArea: (Long?) -> Unit = {},
    onAddArea: (String, Long) -> Unit = { _, _ -> },
    onOpenEstimate: () -> Unit = {},
    hasApiKey: Boolean = false,
    onSetApiKey: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    swipeRightCompletes: Boolean = true,
    onReorder: (List<Long>) -> Unit = {},
    onScanNote: (ByteArray) -> Unit = {},
    routinesExpandedInitially: Boolean = false,
    weekTasks: List<Task> = emptyList()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var qaOpen by remember { mutableStateOf(false) }
    var qaText by remember { mutableStateOf("") }
    var routOpen by remember { mutableStateOf(routinesExpandedInitially) }
    var briefOpen by remember { mutableStateOf(false) }
    var briefLoading by remember { mutableStateOf(false) }
    var briefLoaded by remember { mutableStateOf(false) }
    var briefActionDone by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    // Skan kartki: aparat → zdjęcie → AI wyciąga zadania. (Null w podglądzie/Paparazzi.)
    val hasRegistry = androidx.activity.compose.LocalActivityResultRegistryOwner.current != null
    val noteCamera = if (hasRegistry) androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            val out = java.io.ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
            onScanNote(out.toByteArray())
        }
    } else null
    var scanConfirm by remember { mutableStateOf(false) }
    if (scanConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { scanConfirm = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { scanConfirm = false; noteCamera?.launch(null) }) { Text("Zrób zdjęcie (~0,01 $)") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { scanConfirm = false }) { Text("Anuluj") } },
            title = { Text("Zeskanować kartkę?") },
            text = { Text("Zdjęcie zostanie wysłane do AI (gpt-4o-mini), które odczyta zadania. Szacowany koszt ≈ 0,01 $ z Twojego konta OpenAI.", fontSize = 12.5.sp) }
        )
    }

    // Wstecz (systemowe) zamyka kolejno otwarte nakładki — intuicyjna nawigacja.
    androidx.activity.compose.BackHandler(enabled = qaOpen) { qaOpen = false }
    androidx.activity.compose.BackHandler(enabled = briefOpen) { briefOpen = false }
    androidx.activity.compose.BackHandler(enabled = routOpen) { routOpen = false }
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    LaunchedEffect(quickAddPrefill) {
        if (!quickAddPrefill.isNullOrBlank()) {
            qaText = quickAddPrefill
            qaOpen = true
        }
    }
    LaunchedEffect(briefOpen) {
        if (briefOpen && !briefLoaded) {
            briefLoading = true
            kotlinx.coroutines.delay(1200)
            briefLoading = false
            briefLoaded = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = uiState.view,
                projects = projects,
                labels = labels,
                uiState = uiState,
                isDark = isDarkTheme,
                onSelect = { onSelectView(it); scope.launch { drawerState.close() } },
                onAddProject = { dialog = DialogKind.NewProject },
                onAddLabel = { dialog = DialogKind.NewLabel },
                onGoals = { dialog = DialogKind.Goals },
                onToggleDark = onToggleTheme,
                onOpenActivityPool = { onOpenActivityPool(); scope.launch { drawerState.close() } },
                isPhotoBackground = isPhotoBackground,
                onTogglePhotoBackground = onTogglePhotoBackground,
                activities = activities,
                onAddActivity = { onAddActivityQuick(); scope.launch { drawerState.close() } },
                onOpenEstimate = { onOpenEstimate(); scope.launch { drawerState.close() } },
                hasApiKey = hasApiKey,
                onOpenApiKey = { dialog = DialogKind.ApiKey },
                onOpenSettings = { onOpenSettings(); scope.launch { drawerState.close() } }
            )
        }
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalSwipeRightCompletes provides swipeRightCompletes
        ) {
        Box(Modifier.fillMaxSize()) {
            // Inset status bara liczony TU (obok paska pigułek), a nie w ZenContent —
            // wewnątrz Boxa z haze() kontekst insetów się zeruje i nagłówek wjeżdżał
            // na pasek. Przekazujemy gotową wartość do treści.
            val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars
                .asPaddingValues().calculateTopPadding()
            // ── Treść — zaczyna się i PRZYCINA tuż pod paskiem pigułek, więc tekst
            //    nigdy nie wjeżdża na przyciski (pozostaje pod nimi). ───────────
            Box(Modifier.fillMaxSize().padding(top = statusTop + 60.dp).haze(hazeState)) {
                ZenContent(uiState, projects, labels, onToggle, onTaskClick, onDeferToTomorrow, onOpenAutomation, 0.dp, onReorder)
            }

            // ── ⋮ akcje widoku — nakładka pod paskiem ─────────────────────────
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 54.dp, end = 8.dp)) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).bouncy(0.9f) { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.MoreVert, "Więcej", tint = GlassTextSecondary, modifier = Modifier.size(18.dp))
                }
                GlassMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                    GlassMenuItem("Sortowanie: ${uiState.sortMode.label}") { menuOpen = false; sortMenuOpen = true }
                    uiState.currentProject?.let { project ->
                        GlassMenuItem(if (project.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych") { menuOpen = false; onToggleProjectFavorite(project.id) }
                        GlassMenuItem("Dodaj sekcję") { menuOpen = false; dialog = DialogKind.NewSection }
                        GlassMenuItem("Duplikuj projekt") { menuOpen = false; onDuplicateProject(project.id) }
                        GlassMenuItem(if (project.isArchived) "Przywróć z archiwum" else "Archiwizuj projekt") { menuOpen = false; onArchiveProject(project.id, !project.isArchived) }
                        GlassMenuItem("Usuń projekt") { menuOpen = false; onDeleteProject(project.id) }
                    }
                    uiState.currentLabel?.let { label ->
                        GlassMenuItem(if (label.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych") { menuOpen = false; onToggleLabelFavorite(label.id) }
                        GlassMenuItem("Usuń etykietę") { menuOpen = false; onDeleteLabel(label.id) }
                    }
                    GlassMenuItem("Zeskanuj kartkę (AI)") { menuOpen = false; scanConfirm = true }
                    GlassMenuItem("Wyślij plan dnia") { menuOpen = false; onSharePlan() }
                    GlassMenuItem("Usuń ukończone") { menuOpen = false; onClearCompleted() }
                }
                GlassMenu(expanded = sortMenuOpen, onDismiss = { sortMenuOpen = false }) {
                    SortMode.entries.forEach { mode ->
                        GlassMenuItem((if (mode == uiState.sortMode) "✓ " else "") + mode.label) { sortMenuOpen = false; onSort(mode) }
                    }
                }
            }

            // ── Pasek górny — nakładka; pigułki rozmywają treść pod sobą (liquid glass) ─
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleGlassButton({ scope.launch { drawerState.open() } }) {
                    Icon(Icons.Filled.Menu, "Menu", tint = GlassTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AreaSwitcher(areas, activeAreaId, onSelectArea) { dialog = DialogKind.NewArea }
                    Row(
                        Modifier
                            .height(42.dp)
                            .glassBlur(RoundedCornerShape(21.dp))
                            .bouncy(0.94f) { briefOpen = !briefOpen }
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tydzień", fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                    }
                }
                Spacer(Modifier.weight(1f))
                CircleGlassButton(onFreeTime) {
                    Icon(Icons.Outlined.Bolt, "Czas wolny", tint = GlassAccent, modifier = Modifier.size(19.dp))
                }
            }

            // ── Panel Rutyn nad dockiem ──────────────────────────────────────
            if (uiState.view == AppView.Today) {
                AnimatedVisibility(
                    visible = routOpen,
                    enter = slideInVertically(tween(450, easing = EASE)) { it / 4 } + fadeIn(tween(350)),
                    exit = slideOutVertically(tween(350, easing = EASE)) { it / 4 } + fadeOut(tween(250)),
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 104.dp)
                ) {
                    RoutinesPanel(
                        routines = uiState.routines,
                        doneCount = uiState.routinesDone,
                        onComplete = onToggle,
                        onCollapse = { routOpen = false }
                    )
                }
            }

            // ── Dock: kółko Rutyn + Dzisiaj/Nadchodzące ─────────────────────
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.view == AppView.Today) {
                    Box {
                        Box(
                            Modifier.size(54.dp).controlCenterGlass(CircleShape).bouncy(0.9f) { routOpen = !routOpen },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.routines.isEmpty() && uiState.routinesDone > 0) {
                                Icon(Icons.Outlined.CheckCircle, "Rutyny zrobione", tint = GlassAccent, modifier = Modifier.size(19.dp))
                            } else {
                                Icon(Icons.Outlined.Autorenew, "Rutyny", tint = GlassTextPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                        if (uiState.routines.isNotEmpty()) {
                            // Odznaka z białą obwódką (halo) — czytelnie odcina się od kółka.
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(5.dp, (-5).dp)
                                    .size(20.dp).background(Color.White, CircleShape).padding(2.dp)
                                    .background(GlassAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${uiState.routines.size}", color = Color.White, fontSize = 10.sp,
                                    fontWeight = FontWeight.W800, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
                Row(Modifier.controlCenterGlass(RoundedCornerShape(30.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DockTab(Icons.Outlined.CalendarToday, "Dzisiaj", uiState.view == AppView.Today) { onSelectView(AppView.Today) }
                    DockTab(Icons.Outlined.DateRange, "Nadchodzące", uiState.view == AppView.Upcoming) { onSelectView(AppView.Upcoming) }
                }
            }

            // ── Scrim Quick Add ─────────────────────────────────────────────
            if (qaOpen) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color(0x4D1C0A42))
                        .clickable(remember { MutableInteractionSource() }, null) { qaOpen = false }
                )
            }

            // ── Morfujący FAB Quick Add ─────────────────────────────────────
            QuickAddMorph(
                open = qaOpen,
                text = qaText,
                onText = { qaText = it },
                projects = projects,
                labels = labels,
                onToggleOpen = {
                    qaOpen = !qaOpen
                    if (!qaOpen) onPrefillConsumed()
                },
                onSubmit = {
                    if (qaText.isNotBlank()) {
                        onQuickAdd(qaText, QuickAddOverrides())
                        qaText = ""
                        qaOpen = false
                        onPrefillConsumed()
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )

            // ── Asystent tygodnia (zjeżdża z góry) ──────────────────────────
            if (briefOpen) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x5218083C))
                        .clickable(remember { MutableInteractionSource() }, null) { briefOpen = false }
                )
            }
            AnimatedVisibility(
                visible = briefOpen,
                enter = slideInVertically(tween(550, easing = EASE)) { -it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(400, easing = EASE)) { -it } + fadeOut(tween(250)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                WeekBrief(
                    loading = briefLoading,
                    weekTasks = weekTasks,
                    uiState = uiState,
                    actionDone = briefActionDone,
                    onAction = { briefActionDone = true; onMoveOverdueToToday() },
                    onItemClick = { onTaskClick(it); briefOpen = false },
                    onClose = { briefOpen = false }
                )
            }
        }
        }
    }

    when (dialog) {
        DialogKind.NewArea -> NameColorDialog("Nowy obszar", "Nazwa obszaru", true, { dialog = null }) { n, c -> onAddArea(n, c); dialog = null }
        DialogKind.NewProject -> NameColorDialog("Nowy projekt", "Nazwa projektu", true, { dialog = null }) { n, c -> onAddProject(n, c); dialog = null }
        DialogKind.NewLabel -> NameColorDialog("Nowa etykieta", "Nazwa etykiety", true, { dialog = null }) { n, c -> onAddLabel(n, c); dialog = null }
        DialogKind.NewSection -> NameColorDialog("Nowa sekcja", "Nazwa sekcji", false, { dialog = null }) { n, _ ->
            (uiState.view as? AppView.ProjectView)?.let { onAddSection(it.id, n) }
            dialog = null
        }
        DialogKind.Goals -> GoalsDialog(uiState.goalDaily, uiState.goalWeekly, { dialog = null }) { d, w -> onSetGoals(d, w); dialog = null }
        DialogKind.ApiKey -> ApiKeyDialog(hasApiKey, { dialog = null }) { k -> onSetApiKey(k); dialog = null }
        null -> {}
    }
}

private enum class DialogKind { NewArea, NewProject, NewLabel, NewSection, Goals, ApiKey }

@Composable
private fun ApiKeyDialog(hasKey: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) { Text("Zapisz") }
        },
        dismissButton = {
            Row {
                if (hasKey) androidx.compose.material3.TextButton(onClick = { onSave("") }) { Text("Usuń") }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
        title = { Text("Klucz AI (OpenAI)") },
        text = {
            Column {
                Text(
                    (if (hasKey) "Klucz jest ustawiony. Wklej nowy, aby zmienić.\n\n" else "") +
                    "Klucz utworzysz na platform.openai.com → API keys. Zostaje tylko na tym urządzeniu.",
                    fontSize = 12.5.sp
                )
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = key, onValueChange = { key = it }, singleLine = true,
                    placeholder = { Text("sk-…") }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

// ─── Elementy paska i docka ──────────────────────────────────────────────────

@Composable
private fun AreaSwitcher(
    areas: List<pl.media30.todoisto.data.Area>,
    activeAreaId: Long?,
    onSelect: (Long?) -> Unit,
    onAdd: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val active = areas.firstOrNull { it.id == activeAreaId }
    val dotColor = active?.let { Color(it.colorArgb) } ?: GlassAccent
    Box {
        Row(
            Modifier
                .height(42.dp)
                .glassBlur(RoundedCornerShape(21.dp))
                .bouncy(0.94f) { open = true }
                .padding(start = 13.dp, end = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(7.dp))
            Text(active?.name ?: "Wszystko", fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, maxLines = 1, softWrap = false)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = GlassTextSecondary, modifier = Modifier.size(15.dp).padding(start = 2.dp))
        }
        if (open) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 48.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
            ) {
                val visible = remember { androidx.compose.animation.core.MutableTransitionState(false) }
                visible.targetState = true
                AnimatedVisibility(
                    visibleState = visible,
                    enter = fadeIn(tween(160)) + androidx.compose.animation.scaleIn(
                        tween(240, easing = EASE), initialScale = 0.82f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.2f, 0f)
                    ) + expandVertically(tween(240, easing = EASE)),
                    exit = fadeOut(tween(120))
                ) {
                    Column(
                        Modifier.width(232.dp)
                            .shadow(26.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassDockBg)
                            .border(1.dp, GlassRim.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        AreaMenuItem("Wszystko", null, activeAreaId == null) { open = false; onSelect(null) }
                        areas.forEach { a ->
                            AreaMenuItem(a.name, Color(a.colorArgb), activeAreaId == a.id) { open = false; onSelect(a.id) }
                        }
                        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(1.dp).background(GlassHair))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .bouncy(0.97f) { open = false; onAdd() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, null, tint = GlassAccent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Nowy obszar", fontSize = 14.sp, fontWeight = FontWeight.W700, color = GlassAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaMenuItem(name: String, dot: Color?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) GlassAccent.copy(alpha = 0.14f) else Color.Transparent)
            .bouncy(0.97f, onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(11.dp).clip(CircleShape).background(dot ?: GlassTextSecondary.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {}
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 14.sp, fontWeight = if (selected) FontWeight.W800 else FontWeight.W600, color = GlassTextPrimary, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = GlassAccent, modifier = Modifier.size(16.dp))
    }
}

/** Menu w stylu glass (jak pigułka „Tydzień") — zamiast kryjącego DropdownMenu. */
@Composable
private fun GlassMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    if (!expanded) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.ui.window.Popup(
        alignment = Alignment.TopEnd,
        offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 38.dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        val vis = remember { androidx.compose.animation.core.MutableTransitionState(false) }
        vis.targetState = true
        AnimatedVisibility(
            visibleState = vis,
            enter = fadeIn(tween(160)) + androidx.compose.animation.scaleIn(
                tween(220, easing = EASE), initialScale = 0.85f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
            ) + expandVertically(tween(220, easing = EASE)),
            exit = fadeOut(tween(120))
        ) {
            Column(
                Modifier.width(236.dp)
                    .shadow(20.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    // Menu leży wprost na treści (bez przyciemnienia), więc frosted,
                    // czytelne szkło — treść pod spodem NIE prześwituje.
                    .background(GlassSurface)
                    .border(1.dp, GlassRim.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                    .padding(vertical = 8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun GlassMenuItem(text: String, onClick: () -> Unit) {
    Text(
        text, fontSize = 14.sp, fontWeight = FontWeight.W600, color = GlassTextPrimary,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .bouncy(0.98f, onClick).padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun CircleGlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(42.dp).glassBlur(CircleShape).bouncy(0.9f, onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

@Composable
private fun DockTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) GlassAccent else Color.Transparent, tween(350), label = "dockBg")
    val fg by animateColorAsState(if (selected) Color.White else GlassTextPrimary, tween(350), label = "dockFg")
    Row(
        Modifier.clip(RoundedCornerShape(24.dp)).background(bg).bouncy(0.92f, onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.W800, color = fg, maxLines = 1, softWrap = false)
    }
}

// ─── Treść widoków ───────────────────────────────────────────────────────────

@Composable
private fun ZenContent(
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onDefer: (Task) -> Unit,
    onOpenAutomation: (Task) -> Unit,
    statusTop: androidx.compose.ui.unit.Dp,
    onReorder: (List<Long>) -> Unit = {}
) {
    val today = LocalDate.now()
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    // Bucket pory dnia (dla widoku Dzisiaj).
    fun bucketOf(t: Task): String {
        val m = t.dueTimeMinutes
        return when {
            m == null -> "Rano"
            m < 12 * 60 -> "Rano"
            m < 17 * 60 -> "Po południu"
            else -> "Wieczorem"
        }
    }

    // Przeciąganie zadań (drag&drop) BEZ trybów — działa wprost na liście Dzisiaj.
    // Kolejność wg position (utrzymana przez przeciąganie), grupy wg pory dnia.
    val flatNodes = uiState.groups.flatMap { it.nodes }
    val flatIds = flatNodes.map { it.task.id }
    val orderedNodes = remember { androidx.compose.runtime.mutableStateListOf<TaskNode>() }
    androidx.compose.runtime.LaunchedEffect(flatIds) {
        orderedNodes.clear()
        orderedNodes.addAll(flatNodes.sortedBy { it.task.position })
    }
    val lazyState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
        val f = orderedNodes.indexOfFirst { it.task.id == from.key }
        val t = orderedNodes.indexOfFirst { it.task.id == to.key }
        // Przenosimy tylko w obrębie tej samej pory dnia — bez „przeskoków" między grupami.
        if (f in orderedNodes.indices && t in orderedNodes.indices &&
            bucketOf(orderedNodes[f].task) == bucketOf(orderedNodes[t].task)) {
            orderedNodes.add(t, orderedNodes.removeAt(f))
        }
    }

    // Sam kontener treści jest już odsunięty i przycięty pod paskiem pigułek
    // (patrz padding Boxa z haze()), więc tu wystarczy drobny zapas u góry.
    LazyColumn(state = lazyState, contentPadding = PaddingValues(start = 18.dp, top = statusTop + 10.dp, end = 18.dp, bottom = 170.dp)) {
        when (uiState.view) {
            AppView.Today -> {
                item(key = "hdr") {
                    Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dzisiaj", style = MaterialTheme.typography.headlineSmall, color = GlassTextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(dateCaption(today), fontSize = 12.5.sp, color = GlassTextSecondary)
                        Spacer(Modifier.height(7.dp))
                        Box(Modifier.size(34.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(GlassAccent.copy(alpha = 0.5f)))
                    }
                }
                if (orderedNodes.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            if (uiState.routines.isNotEmpty()) "Zostały tylko rutyny" else "Wszystko zrobione",
                            if (uiState.routines.isNotEmpty()) "Główna lista pusta — sprawdź rutyny przyciskiem ⟳ na dole." else "Dodaj kolejne zadanie plusem w rogu."
                        )
                    }
                } else {
                    // Grupy w STAŁEJ kolejności (żeby nagłówki były ciągłe i miały
                    // unikalne klucze); w obrębie grupy kolejność wg przeciągania.
                    listOf("Rano", "Po południu", "Wieczorem").forEach { b ->
                        val inBucket = orderedNodes.filter { bucketOf(it.task) == b }
                        if (inBucket.isNotEmpty()) {
                            item(key = "sec-$b") {
                                val isCol = collapsed[b] == true
                                val chev by animateFloatAsState(if (isCol) -90f else 0f, tween(400, easing = EASE), label = "chev")
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(12.dp))
                                        .bouncy(0.98f) { collapsed[b] = !isCol }.padding(horizontal = 6.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(b.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.54.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = GlassTextSecondary, modifier = Modifier.size(13.dp).rotate(chev))
                                }
                            }
                            if (collapsed[b] != true) {
                                inBucket.forEach { node ->
                                    item(key = node.task.id) {
                                        ReorderableItem(reorderState, key = node.task.id) { dragging ->
                                            val scale by animateFloatAsState(if (dragging) 1.03f else 1f, tween(180), label = "dragScale")
                                            val elevation by animateDpAsState(if (dragging) 12.dp else 0.dp, tween(180), label = "dragElev")
                                            Column(
                                                Modifier
                                                    .padding(vertical = 4.dp)
                                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                                    .shadow(elevation, RoundedCornerShape(20.dp))
                                                    .longPressDraggableHandle(onDragStopped = { onReorder(orderedNodes.map { it.task.id }) })
                                            ) {
                                                ZenRowWithSubs(node, uiState, projects, labels, onToggle, onTaskClick, onDefer, onOpenAutomation)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AppView.Upcoming -> {
                item(key = "hdr") {
                    Column(Modifier.padding(top = 10.dp, start = 4.dp, bottom = 4.dp)) {
                        Text("najbliższe 7 dni", fontSize = 13.sp, fontWeight = FontWeight.W600, color = GlassTextSecondary)
                        Text("Nadchodzące", style = MaterialTheme.typography.headlineMedium, color = GlassTextPrimary)
                    }
                }
                val nodes = uiState.groups.firstOrNull()?.nodes.orEmpty()
                (1..7).forEach { off ->
                    val date = today.plusDays(off.toLong())
                    val epoch = date.toEpochDay()
                    val dayNodes = nodes.filter { it.task.dueDate == epoch }
                    item(key = "day-$off") {
                        Row(Modifier.padding(top = 18.dp, start = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                            Text(
                                if (off == 1) "Jutro" else weekdayName(date).replaceFirstChar { it.uppercase() },
                                fontSize = 14.5.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${date.dayOfMonth} ${MS[date.monthValue - 1]}", fontSize = 12.sp, color = GlassTextSecondary)
                        }
                        if (dayNodes.isEmpty()) {
                            Text(
                                "Nic zaplanowanego", fontSize = 12.5.sp,
                                color = GlassTextSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                dayNodes.forEachIndexed { i, node ->
                                    ZenRowWithSubs(node, uiState, projects, labels, onToggle, onTaskClick, onDefer, onOpenAutomation)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                item(key = "hdr") {
                    Row(Modifier.padding(top = 14.dp, start = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = uiState.currentProject?.let { Color(it.colorArgb) }
                            ?: uiState.currentLabel?.let { Color(it.colorArgb) }
                            ?: GlassAccent
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(dotColor))
                        Spacer(Modifier.width(10.dp))
                        Text(uiState.title, style = MaterialTheme.typography.titleLarge, color = GlassTextPrimary)
                        Spacer(Modifier.width(10.dp))
                        val n = uiState.groups.sumOf { it.nodes.size }
                        Text(
                            "$n " + when { n == 1 -> "zadanie"; n in 2..4 -> "zadania"; else -> "zadań" },
                            fontSize = 12.5.sp, fontWeight = FontWeight.W600, color = GlassTextSecondary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                if (uiState.isEmpty) {
                    item(key = "empty") {
                        val (h, s) = when (uiState.view) {
                            AppView.Inbox -> "Skrzynka pusta" to "Zadania bez terminu i projektu trafią tutaj."
                            AppView.Completed -> "Brak ukończonych" to "Odhaczone zadania pojawią się w tym miejscu."
                            is AppView.ProjectView -> "Projekt jest pusty" to "Dodaj pierwsze zadanie plusem w rogu."
                            else -> "Brak zadań z tą etykietą" to "Oznacz zadania tą etykietą, aby je tu zebrać."
                        }
                        EmptyState(h, s)
                    }
                } else {
                    uiState.groups.forEach { group ->
                        if (group.name != null) {
                            zenSection(group.name, group.nodes, collapsed, uiState, projects, labels, onToggle, onTaskClick, onDefer, onOpenAutomation)
                        } else {
                            item(key = "flat-${group.sectionId}") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    group.nodes.forEach { node ->
                                        ZenRowWithSubs(node, uiState, projects, labels, onToggle, onTaskClick, onDefer, onOpenAutomation)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.zenSection(
    name: String,
    nodes: List<TaskNode>,
    collapsed: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onDefer: (Task) -> Unit,
    onOpenAutomation: (Task) -> Unit
) {
    item(key = "sec-$name") {
        val isCollapsed = collapsed[name] == true
        val chev by animateFloatAsState(if (isCollapsed) -90f else 0f, tween(400, easing = EASE), label = "chev")
        Column(Modifier.padding(top = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .bouncy(0.98f) { collapsed[name] = !isCollapsed }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name.uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.W800, letterSpacing = 1.54.sp,
                    color = GlassTextSecondary, modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown, null,
                    tint = GlassTextSecondary,
                    modifier = Modifier.size(13.dp).rotate(chev)
                )
            }
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically(tween(500, easing = EASE)) + fadeIn(),
                exit = shrinkVertically(tween(500, easing = EASE)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    nodes.forEachIndexed { i, node ->
                        ZenRowWithSubs(node, uiState, projects, labels, onToggle, onTaskClick, onDefer, onOpenAutomation)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZenRowWithSubs(
    node: TaskNode,
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onDefer: (Task) -> Unit,
    onOpenAutomation: (Task) -> Unit
) {
    val task = node.task
    val todayEpoch = LocalDate.now().toEpochDay()
    val dueChip: String? = task.dueDate?.let { due ->
        val show = when (uiState.view) {
            AppView.Today -> due < todayEpoch
            AppView.Upcoming -> false
            else -> true
        }
        if (!show) null else when {
            due < todayEpoch -> "Zaległe"
            due == todayEpoch -> "Dzisiaj"
            due == todayEpoch + 1 -> "Jutro"
            else -> shortDate(due)
        }
    }
    val meta = zenMetaLine(
        dueChip = dueChip,
        timeShown = task.dueTimeMinutes != null,
        durationMin = task.durationMinutes,
        recurrenceLabel = task.recurrence?.label?.lowercase(),
        deadline = task.deadline?.let { "do " + shortDate(it) },
        priority = task.priority,
        projectName = if (uiState.view is AppView.ProjectView) null else projects.firstOrNull { it.id == task.projectId }?.name,
        labelNames = labels.filter { task.labelIds.contains(it.id) }.map { it.name },
        subDone = node.subtasks.count { it.isCompleted },
        subTotal = node.subtasks.size,
        linkDomains = task.attachments.map { it.removePrefix("https://").removePrefix("http://").substringBefore('/') }
    )
    // #4 — znacznik dla długiego zadania ze zbliżającym się deadline'em
    val longTask = (task.durationMinutes ?: 0) >= 60
    val dlSoon = task.deadline?.let { it - todayEpoch in 0..2 } == true
    val deadlineWarn = longTask && dlSoon && !task.isCompleted
    // #4b — czy zadanie da się przyspieszyć/zautomatyzować (marker „⚡ AI")
    val automatable = remember(task.title, task.notes) {
        !task.isCompleted && pl.media30.todoisto.data.AutomationAdvisor.advise(task.title, task.notes).canAutomate
    }

    // #3 — swipe: w prawo = ukończ, w lewo = odłóż na jutro.
    // Traktujemy to jako AKCJĘ, nie „dismiss": po odpaleniu wracamy do środka
    // (confirmValueChange = false), a lista sama zaktualizuje się z danych.
    // key(task.id) — stabilny stan swipe'u mimo zmian listy (bez pomyłki wierszy).
    androidx.compose.runtime.key(task.id) {
        val view = LocalView.current
        // Konfigurowalne przesunięcia: prawe = ukończ lub odłóż (ustawienia).
        val rightCompletes = LocalSwipeRightCompletes.current
        val dismiss = androidx.compose.material3.rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                val toEnd = value == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
                val toStart = value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
                val complete = (toEnd && rightCompletes) || (toStart && !rightCompletes)
                val defer = (toStart && rightCompletes) || (toEnd && !rightCompletes)
                when {
                    complete -> view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM).also { onToggle(task) }
                    defer -> view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK).also { onDefer(task) }
                }
                false // akcja odpalona — kafelek wraca na miejsce
            },
            positionalThreshold = { it * 0.45f }
        )
        androidx.compose.material3.SwipeToDismissBox(
            state = dismiss,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)),
            backgroundContent = { SwipeBg(dismiss.dismissDirection, rightCompletes) }
        ) {
            Column(Modifier.fillMaxWidth().taskTile { onTaskClick(task) }) {
                if (deadlineWarn) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp)) {
                        Text(
                            "⏳ Napięty deadline",
                            fontSize = 10.5.sp, fontWeight = FontWeight.W800, color = Color(0xFFB45309),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x33F59E0B)).padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
                // Ikonka AI po prawej, na wysokości zadania; godzina pod spodem
                val timeStr = task.dueTimeMinutes?.let { "%d:%02d".format(it / 60, it % 60) }
                TaskRowZen(
                    task, meta, { onToggle(task) }, { onTaskClick(task) },
                    trailing = if (automatable) {
                        {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Delikatny „oddech" odznaki AI — subtelna zachęta do dotknięcia.
                                val aiPulse = rememberInfiniteTransition(label = "aiPulse")
                                val aiScale by aiPulse.animateFloat(
                                    1f, 1.045f,
                                    infiniteRepeatable(tween(1600, easing = EASE), RepeatMode.Reverse),
                                    label = "aiScale"
                                )
                                Row(
                                    Modifier
                                        .graphicsLayer { scaleX = aiScale; scaleY = aiScale }
                                        .clip(RoundedCornerShape(50)).background(Color(0xFFFFC94D))
                                        .bouncy(0.9f) { onOpenAutomation(task) }.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFF5B3D00), modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("AI", fontSize = 10.5.sp, fontWeight = FontWeight.W800, color = Color(0xFF5B3D00))
                                }
                                if (timeStr != null) Text(timeStr, fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassAccent)
                            }
                        }
                    } else null
                )
                node.subtasks.forEach { sub ->
                    TaskRowZen(sub, "", { onToggle(sub) }, { onTaskClick(sub) }, compact = true)
                }
            }
        }
    }
}

/** Tło ujawniane podczas swipe: zielony „ukończ" (→) / bursztynowy „jutro" (←). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBg(dir: androidx.compose.material3.SwipeToDismissBoxValue, rightCompletes: Boolean = true) {
    // W spoczynku NIE rysujemy nic (przezroczyste tło pod kafelkiem).
    if (dir == androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
        Box(Modifier.fillMaxSize()); return
    }
    val toEnd = dir == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
    // Ta strona ukańcza, jeśli (prawo i prawe=ukończ) lub (lewo i prawe=odłóż).
    val isComplete = toEnd == rightCompletes
    val color = if (isComplete) Color(0xFF1F8A5B) else Color(0xFFEB8909)
    Row(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (toEnd) Arrangement.Start else Arrangement.End
    ) {
        Icon(
            if (isComplete) Icons.Filled.Check else Icons.Outlined.DateRange,
            null, tint = Color.White, modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(if (isComplete) "Ukończ" else "Na jutro", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W800)
    }
}

/** Preferencja: przesunięcie w prawo ukańcza (true) lub odkłada (false). */
val LocalSwipeRightCompletes = androidx.compose.runtime.staticCompositionLocalOf { true }

@Composable
private fun EmptyState(head: String, sub: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(74.dp).glass(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, null, tint = GlassAccent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(head, fontSize = 16.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(sub, fontSize = 13.sp, color = GlassTextSecondary, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

// ─── Morfujący FAB Quick Add ─────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddMorph(
    open: Boolean,
    text: String,
    onText: (String) -> Unit,
    projects: List<Project>,
    labels: List<Label>,
    onToggleOpen: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraintsFix(modifier.navigationBarsPadding().imePadding()) { maxW ->
        val width by animateDpAsState(if (open) maxW - 28.dp else 58.dp, tween(500, easing = EASE), label = "qaW")
        val height by animateDpAsState(if (open) 360.dp else 58.dp, tween(500, easing = EASE), label = "qaH")
        val bottomPad by animateDpAsState(if (open) 16.dp else 84.dp, tween(500, easing = EASE), label = "qaB")
        val bg by animateColorAsState(if (open) GlassSurface else GlassAccent, tween(400), label = "qaBg")
        val rot by animateFloatAsState(if (open) 45f else 0f, tween(500, easing = EASE), label = "qaRot")
        val parsed = remember(text) { QuickAddParser().parse(text) }

        Box(
            Modifier
                .padding(end = 14.dp, bottom = bottomPad)
                .size(width, height)
                .then(
                    // Otwarty panel = czytelne frosted szkło (treść listy pod spodem NIE
                    // prześwituje); zamknięty = FAB akcentowy.
                    if (open) Modifier
                        .shadow(22.dp, RoundedCornerShape(29.dp))
                        .clip(RoundedCornerShape(29.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassRim.copy(alpha = 0.6f), RoundedCornerShape(29.dp))
                    else Modifier.clip(RoundedCornerShape(29.dp)).background(GlassAccent)
                )
        ) {
            // Plus / X
            Box(
                Modifier.align(Alignment.TopEnd).size(58.dp)
                    .bouncy(0.88f, onClick = onToggleOpen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add, if (open) "Zamknij" else "Dodaj zadanie",
                    tint = if (open) GlassTextSecondary else Color.White,
                    modifier = Modifier.size(22.dp).rotate(rot)
                )
            }
            if (open) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 46.dp)) {
                        Text("Nowe zadanie", fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                    }
                    Spacer(Modifier.height(10.dp))
                    BasicTextField(
                        value = text,
                        onValueChange = onText,
                        textStyle = TextStyle(fontSize = 13.5.sp, color = GlassTextPrimary, fontWeight = FontWeight.W600),
                        cursorBrush = SolidColor(GlassAccent),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GlassInputBg)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                if (text.isEmpty()) {
                                    Text("np. Raport jutro o 15:00 #Praca @pilne p1", fontSize = 13.5.sp, color = GlassTextSecondary.copy(alpha = 0.55f))
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // Rozpoznane tokeny / podpowiedź
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (text.isBlank()) {
                            Text("Rozpoznam datę, godzinę, #projekt, @etykietę, priorytet i cykl.", fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.75f))
                        } else {
                            parsed.dueDate?.let { QAChip(if (it == LocalDate.now().toEpochDay()) "Dzisiaj" else if (it == LocalDate.now().toEpochDay() + 1) "Jutro" else shortDate(it), Color(0xFF1F8A5B)) }
                            parsed.dueTimeMinutes?.let { QAChip("%d:%02d".format(it / 60, it % 60), GlassAccent) }
                            parsed.recurrence?.let { QAChip("⟳ " + it.label.lowercase(), GlassTextSecondary) }
                            if (parsed.priority != Priority.P4) QAChip("P${parsed.priority.ordinal + 1}", parsed.priority.color)
                            parsed.projectName?.let { QAChip("#$it", Color(0xFF4D6BFF)) }
                            parsed.labelNames.forEach { QAChip("@$it", Color(0xFFC24DFF)) }
                            parsed.durationMinutes?.let { QAChip("$it min", GlassTextSecondary) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Tokeny-skróty (wstawiają tekst)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val addToken: (String) -> Unit = { tok ->
                            onText((text.trimEnd() + " " + tok + " ").trimStart())
                        }
                        QAToken("dzisiaj", GlassAccent) { addToken("dzisiaj") }
                        QAToken("jutro", GlassAccent) { addToken("jutro") }
                        QAToken("o 15:00", GlassAccent) { addToken("o 15:00") }
                        QAToken("codziennie", Color(0xFF2E9C4F)) { addToken("codziennie") }
                        QAToken("co tydzień", Color(0xFF2E9C4F)) { addToken("co tydzień") }
                        QAToken("P1", Color(0xFFD1453B)) { addToken("p1") }
                        QAToken("P2", Color(0xFFC6740A)) { addToken("p2") }
                        projects.filter { !it.isArchived }.take(2).forEach { p ->
                            QAToken("#${p.name}", Color(p.colorArgb)) { addToken("#${p.name}") }
                        }
                        labels.take(1).forEach { l ->
                            QAToken("@${l.name}", Color(l.colorArgb)) { addToken("@${l.name}") }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Anuluj", fontSize = 12.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary,
                            modifier = Modifier.clip(RoundedCornerShape(50)).bouncy(0.95f, onClick = onToggleOpen).padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Dodaj", fontSize = 12.5.sp, fontWeight = FontWeight.W800, color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GlassAccent.copy(alpha = if (text.isBlank()) 0.45f else 1f))
                                .bouncy(0.95f, onClick = onSubmit)
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Pomocniczy BoxWithConstraints przekazujący maxWidth. */
@Composable
private fun BoxWithConstraintsFix(modifier: Modifier, content: @Composable (androidx.compose.ui.unit.Dp) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) { content(maxWidth) }
}

@Composable
private fun QAChip(text: String, color: Color) {
    val eff = if (GlassTheme.dark) androidx.compose.ui.graphics.lerp(color, Color.White, 0.5f) else color
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.W800, color = eff,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = if (GlassTheme.dark) 0.24f else 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun QAToken(text: String, color: Color, onClick: () -> Unit) {
    val eff = if (GlassTheme.dark) androidx.compose.ui.graphics.lerp(color, Color.White, 0.5f) else color
    Text(
        text, fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = eff,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (GlassTheme.dark) 0.24f else 0.12f))
            .bouncy(0.92f, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    )
}

// ─── Asystent tygodnia ───────────────────────────────────────────────────────

@Composable
private fun WeekBrief(
    loading: Boolean,
    weekTasks: List<Task>,
    uiState: TodoUiState,
    actionDone: Boolean,
    onAction: () -> Unit,
    onItemClick: (Task) -> Unit,
    onClose: () -> Unit
) {
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(GlassSurface)
            .border(1.dp, GlassRim.copy(alpha = 0.6f), RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(9.dp))
            Text("Asystent tygodnia", fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, modifier = Modifier.weight(1f))
            Text(
                "AI", fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 0.8.sp, color = GlassAccent,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassTint).padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(13.dp))
        if (loading) {
            ShimmerBar(0.78f); Spacer(Modifier.height(10.dp))
            ShimmerBar(0.92f); Spacer(Modifier.height(10.dp))
            ShimmerBar(0.64f); Spacer(Modifier.height(8.dp))
            Text("Analizuję Twój tydzień…", fontSize = 11.5.sp, color = GlassTextSecondary.copy(alpha = 0.8f))
        } else {
            Text("NAJWAŻNIEJSZE W TYM TYGODNIU", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = GlassTextSecondary)
            Spacer(Modifier.height(9.dp))
            val todayEpoch = today.toEpochDay()
            val items = weekTasks
                .filter { it.parentId == null && !it.isCompleted && it.recurrence == null && it.dueDate != null && it.dueDate!! > todayEpoch && it.dueDate!! <= todayEpoch + 7 }
                .sortedWith(compareBy({ it.priority.ordinal }, { it.dueDate }))
                .take(4)
            items.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).bouncy(0.98f) { onItemClick(t) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(if (t.priority != Priority.P4) t.priority.color else GlassAccent))
                    Spacer(Modifier.width(11.dp))
                    Text(t.title, fontSize = 13.5.sp, fontWeight = FontWeight.W600, color = GlassTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    val d = LocalDate.ofEpochDay(t.dueDate!!)
                    Text(
                        if (t.dueDate == todayEpoch + 1) "jutro" else weekdayName(d).take(3) + " " + d.dayOfMonth,
                        fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val overdue = weekTasks.count { it.parentId == null && !it.isCompleted && it.dueDate != null && it.dueDate!! < todayEpoch }
            val counts = (1..7).map { off -> weekTasks.count { it.parentId == null && !it.isCompleted && it.dueDate == todayEpoch + off } }
            val busiest = counts.withIndex().maxByOrNull { it.value }
            val insight = buildString {
                if (overdue > 0) append("Masz $overdue zaległych zadań. ")
                if (busiest != null && busiest.value >= 2) {
                    val d = today.plusDays((busiest.index + 1).toLong())
                    append("Najbardziej obciążony dzień to ${weekdayName(d)} (${busiest.value} zadań). ")
                }
                val free = counts.count { it == 0 }
                if (free > 0) append("Masz $free wolnych dni na nadrobienie zaległości.")
                if (isEmpty()) append("Tydzień wygląda spokojnie — dobre okno na zadania z listy Pomysłów.")
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GlassTint)
                    .border(1.dp, GlassHair, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(insight, fontSize = 12.5.sp, lineHeight = 19.sp, color = GlassTextPrimary)
                if (overdue > 0) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        if (actionDone) "Przeniesiono ✓" else "Przenieś zaległe na dzisiaj",
                        fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GlassAccent.copy(alpha = if (actionDone) 0.55f else 1f))
                            .bouncy(0.95f) { if (!actionDone) onAction() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(44.dp, 5.dp)
                .clip(RoundedCornerShape(3.dp)).background(GlassHair)
                .bouncy(0.9f, onClick = onClose)
        )
    }
}

@Composable
private fun ShimmerBar(widthFraction: Float) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
    Box(
        Modifier.fillMaxWidth(widthFraction).height(14.dp).clip(RoundedCornerShape(7.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(GlassTint, GlassHair, GlassTint),
                    startX = -260f + 780f * x,
                    endX = 260f + 780f * x
                )
            )
    )
}

// ─── Szuflada ────────────────────────────────────────────────────────────────

@Composable
private fun DrawerContent(
    current: AppView,
    projects: List<Project>,
    labels: List<Label>,
    uiState: TodoUiState,
    isDark: Boolean,
    onSelect: (AppView) -> Unit,
    onAddProject: () -> Unit,
    onAddLabel: () -> Unit,
    onGoals: () -> Unit,
    onToggleDark: () -> Unit,
    onOpenActivityPool: () -> Unit,
    isPhotoBackground: Boolean,
    onTogglePhotoBackground: () -> Unit,
    activities: List<pl.media30.todoisto.data.Activity>,
    onAddActivity: () -> Unit,
    onOpenEstimate: () -> Unit,
    hasApiKey: Boolean,
    onOpenApiKey: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = GlassDrawerBg,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
        modifier = Modifier.width(296.dp)
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 20.dp)) {
            // Logo
            Row(Modifier.padding(start = 12.dp, bottom = 16.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(11.dp))
                Text(buildAnnotatedLogo(), fontSize = 19.sp)
            }

            DrawerRow(Icons.Outlined.CalendarToday, "Dzisiaj", uiState.todayCount, current == AppView.Today) { onSelect(AppView.Today) }
            DrawerRow(Icons.Outlined.DateRange, "Nadchodzące", null, current == AppView.Upcoming) { onSelect(AppView.Upcoming) }
            DrawerRow(Icons.Outlined.Inbox, "Skrzynka", uiState.inboxCount, current == AppView.Inbox) { onSelect(AppView.Inbox) }
            DrawerRow(Icons.Outlined.CheckCircle, "Ukończone", null, current == AppView.Completed) { onSelect(AppView.Completed) }
            DrawerRow(Icons.Outlined.Bolt, "Pula aktywności", null, false, onOpenActivityPool)
            DrawerRow(Icons.Outlined.Settings, "Ustawienia", null, false, onOpenSettings)

            // Szacowanie: ile pracy zostało na dziś (suma czasów zadań) — klik → rozbicie
            EstimateCard(uiState.todayCount, uiState.estTodayMinutes, onOpenEstimate)

            // Ulubione
            val favs = projects.filter { it.isFavorite && !it.isArchived }.map { Triple(AppView.ProjectView(it.id) as AppView, "#${it.name}", Color(it.colorArgb)) } +
                labels.filter { it.isFavorite }.map { Triple(AppView.LabelView(it.id) as AppView, "@${it.name}", Color(it.colorArgb)) }
            if (favs.isNotEmpty()) {
                Row(Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFF4B740), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("ULUBIONE", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = GlassTextSecondary)
                }
                favs.forEach { (view, name, color) ->
                    DrawerDotRow(color, name, current == view) { onSelect(view) }
                }
            }

            // Projekty
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("PROJEKTY", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                Box(Modifier.size(24.dp).clip(CircleShape).bouncy(0.85f, onClick = onAddProject), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, "Nowy projekt", tint = GlassTextSecondary, modifier = Modifier.size(13.dp))
                }
            }
            projects.filter { !it.isArchived }.forEach { p ->
                DrawerDotRow(Color(p.colorArgb), p.name, current == AppView.ProjectView(p.id)) { onSelect(AppView.ProjectView(p.id)) }
            }

            // Etykiety (pigułki)
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ETYKIETY", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                Box(Modifier.size(24.dp).clip(CircleShape).bouncy(0.85f, onClick = onAddLabel), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, "Nowa etykieta", tint = GlassTextSecondary, modifier = Modifier.size(13.dp))
                }
            }
            FlowRowLabels(labels, current, onSelect)

            // Aktywności (pula) — lista + dodawanie
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, null, tint = GlassAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(7.dp))
                Text("AKTYWNOŚCI", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                Box(Modifier.size(24.dp).clip(CircleShape).bouncy(0.85f, onClick = onAddActivity), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, "Dodaj aktywność", tint = GlassTextSecondary, modifier = Modifier.size(13.dp))
                }
            }
            if (activities.isEmpty()) {
                Text(
                    "Brak aktywności — dodaj pierwszą plusem.",
                    fontSize = 12.sp, color = GlassTextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            } else {
                activities.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .bouncy(0.98f, onClick = onAddActivity).padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(effortIcon(a.effortType), null, tint = effortColor(a.effortType), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(a.name, fontSize = 13.5.sp, fontWeight = FontWeight.W600, color = GlassTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${a.durationMinutes}m", fontSize = 11.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
                    }
                }
            }

            // Archiwum
            val archived = projects.filter { it.isArchived }
            if (archived.isNotEmpty()) {
                Row(Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Archive, null, tint = GlassTextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("ARCHIWUM", fontSize = 10.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.26.sp, color = GlassTextSecondary)
                }
                archived.forEach { p ->
                    DrawerDotRow(Color(p.colorArgb).copy(alpha = 0.5f), p.name, current == AppView.ProjectView(p.id), dimmed = true) { onSelect(AppView.ProjectView(p.id)) }
                }
            }

            Spacer(Modifier.weight(1f).height(16.dp))

            // Cele produktywności — na samym dole
            Column(
                Modifier.padding(horizontal = 8.dp).padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp)).background(GlassTint)
                    .bouncy(0.98f, onClick = onGoals).padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("CELE PRODUKTYWNOŚCI", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.69.sp, color = GlassTextSecondary, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Edit, null, tint = GlassTextSecondary, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.height(9.dp))
                GoalBar("Dzisiaj", uiState.doneToday, uiState.goalDaily)
                Spacer(Modifier.height(8.dp))
                GoalBar("Tydzień", uiState.doneWeek, uiState.goalWeekly)
            }

            // Motyw, tło i klucz AI przeniesione do Ustawień (☰ → Ustawienia).
        }
    }
}

/** Szacowanie: ile zadań i ile czasu zostało dziś do zrobienia. Klik → rozbicie na zadania. */
@Composable
private fun EstimateCard(count: Int, minutes: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp)).background(GlassTint).bouncy(0.98f, onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = GlassAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("SZACOWANY CZAS NA DZIŚ", fontSize = 9.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.9.sp, color = GlassTextSecondary)
            Spacer(Modifier.height(3.dp))
            Text(
                if (count == 0) "Nic nie zaplanowane" else "$count " + when { count == 1 -> "zadanie"; count in 2..4 -> "zadania"; else -> "zadań" } + " · " + estTime(minutes),
                fontSize = 14.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GlassTextSecondary, modifier = Modifier.size(18.dp))
    }
}

private fun estTime(minutes: Int): String = when {
    minutes <= 0 -> "—"
    minutes < 60 -> "~${minutes} min"
    minutes % 60 == 0 -> "~${minutes / 60}h"
    else -> "~${minutes / 60}h ${minutes % 60}min"
}

/** #4a — rozbicie „Szacowanego czasu na dziś" na poszczególne zadania z minutami. */
@Composable
fun EstimateBreakdownSheet(items: List<Task>, projects: List<Project>) {
    val total = items.sumOf { it.durationMinutes ?: 20 }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = GlassAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Szacowany czas na dziś", fontSize = 19.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, modifier = Modifier.weight(1f))
            Text(estTime(total), fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassAccent)
        }
        Spacer(Modifier.height(14.dp))
        if (items.isEmpty()) {
            Text("Nic nie zaplanowane na dziś.", fontSize = 13.sp, color = GlassTextSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { t ->
                    val mins = t.durationMinutes ?: 20
                    val est = t.durationMinutes == null
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassTint).padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (t.priority != Priority.P4) t.priority.color else GlassAccent))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = 14.sp, fontWeight = FontWeight.W600, color = GlassTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            projects.firstOrNull { it.id == t.projectId }?.let {
                                Text("#${it.name}", fontSize = 11.sp, color = GlassTextSecondary)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            (if (est) "≈" else "") + "$mins min",
                            fontSize = 12.5.sp, fontWeight = FontWeight.W800,
                            color = if (est) GlassTextSecondary else GlassTextPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "≈ = brak ustawionego czasu, przyjęto 20 min.",
                fontSize = 11.sp, color = GlassTextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun buildAnnotatedLogo() = androidx.compose.ui.text.buildAnnotatedString {
    pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = Sora, fontWeight = FontWeight.W800, color = GlassTextPrimary, letterSpacing = (-0.38).sp))
    append("todoist")
    pushStyle(androidx.compose.ui.text.SpanStyle(color = GlassAccent))
    append("o")
}

@Composable
private fun DrawerRow(icon: ImageVector, label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) GlassTint else Color.Transparent, label = "drawBg")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg)
            .bouncy(0.98f, onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = GlassAccent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary, modifier = Modifier.weight(1f))
        if (count != null && count > 0) Text("$count", fontSize = 12.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary)
    }
}

@Composable
private fun DrawerDotRow(color: Color, label: String, selected: Boolean, dimmed: Boolean = false, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) GlassTint else Color.Transparent, label = "drawBg")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg)
            .bouncy(0.98f, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(horizontal = 3.dp).size(11.dp).clip(RoundedCornerShape(6.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Text(
            label, fontSize = 14.sp, fontWeight = FontWeight.W600,
            color = if (dimmed) GlassTextSecondary else GlassTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowLabels(labels: List<Label>, current: AppView, onSelect: (AppView) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        labels.forEach { l ->
            val col = Color(l.colorArgb)
            val eff = if (GlassTheme.dark) androidx.compose.ui.graphics.lerp(col, Color.White, 0.4f) else col
            Text(
                "@${l.name}", fontSize = 11.5.sp, fontWeight = FontWeight.W800, color = eff,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(col.copy(alpha = if (GlassTheme.dark) 0.22f else 0.12f))
                    .bouncy(0.92f) { onSelect(AppView.LabelView(l.id)) }
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            )
        }
        if (labels.isEmpty()) {
            Text("Brak etykiet", fontSize = 12.sp, color = GlassTextSecondary.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun GoalBar(label: String, done: Int, goal: Int) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary, modifier = Modifier.weight(1f))
            Text("$done / $goal", fontSize = 11.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        val target = (done.toFloat() / goal).coerceIn(0f, 1f)
        val frac by animateFloatAsState(target, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), label = "goalFrac")
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(GlassHair)) {
            Box(
                Modifier.fillMaxWidth(frac.coerceIn(0.001f, 1f)).height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0))))
            )
        }
    }
}

// ─── Dialogi (Nowy projekt/etykieta/sekcja · Cele ze stepperami) ─────────────

@Composable
private fun DialogScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier.width(300.dp).clip(RoundedCornerShape(28.dp)).background(GlassSurface)
                .border(1.dp, GlassRim.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) { content() }
    }
}

@Composable
private fun NameColorDialog(
    title: String,
    placeholder: String,
    hasColor: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PaletteColors.options.first()) }
    DialogScrim(onDismiss) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GlassTextPrimary)
            Spacer(Modifier.height(15.dp))
            BasicTextField(
                value = name, onValueChange = { name = it },
                textStyle = TextStyle(fontSize = 14.sp, color = GlassTextPrimary, fontWeight = FontWeight.W600),
                cursorBrush = SolidColor(GlassAccent),
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassInputBg)
                            .border(1.dp, GlassHair, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (name.isEmpty()) Text(placeholder, fontSize = 14.sp, color = GlassTextSecondary.copy(alpha = 0.55f))
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (hasColor) {
                Spacer(Modifier.height(15.dp))
                Text("KOLOR", fontSize = 11.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.46.sp, color = GlassTextSecondary)
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaletteColors.options.forEach { c ->
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(Color(c)).bouncy(0.85f) { color = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (c == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(17.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Anuluj", fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).bouncy(0.95f, onClick = onDismiss).padding(vertical = 11.dp)
                )
                Text(
                    "Utwórz", fontSize = 13.sp, fontWeight = FontWeight.W800, color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1.6f).clip(RoundedCornerShape(50))
                        .background(GlassAccent.copy(alpha = if (name.isBlank()) 0.45f else 1f))
                        .bouncy(0.96f) { if (name.isNotBlank()) onConfirm(name.trim(), color) }
                        .padding(vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalsDialog(daily: Int, weekly: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var day by remember { mutableStateOf(daily) }
    var week by remember { mutableStateOf(weekly) }
    DialogScrim(onDismiss) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("Cele produktywności", style = MaterialTheme.typography.titleMedium, color = GlassTextPrimary)
            }
            Spacer(Modifier.height(16.dp))
            StepperRow("Dziennie", "zadań na dzień", day, { day = (day - 1).coerceAtLeast(1) }, { day = (day + 1).coerceAtMost(20) })
            Spacer(Modifier.height(12.dp))
            StepperRow("Tygodniowo", "zadań na tydzień", week, { week = (week - 5).coerceAtLeast(5) }, { week = (week + 5).coerceAtMost(80) })
            Spacer(Modifier.height(16.dp))
            Text(
                "Zapisz cele", fontSize = 13.sp, fontWeight = FontWeight.W800, color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(GlassAccent)
                    .bouncy(0.97f) { onConfirm(day, week) }.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun StepperRow(title: String, sub: String, value: Int, onDec: () -> Unit, onInc: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassTint).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
            Text(sub, fontSize = 11.sp, color = GlassTextSecondary)
        }
        StepBtn("−", onDec)
        Text(
            "$value", fontFamily = Sora, fontSize = 19.sp, fontWeight = FontWeight.W800, color = GlassAccent,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(48.dp)
        )
        StepBtn("+", onInc)
    }
}

@Composable
private fun StepBtn(sign: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(GlassFill).border(1.dp, GlassRim, CircleShape)
            .bouncy(0.88f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(sign, fontSize = 16.sp, fontWeight = FontWeight.W700, color = GlassTextPrimary)
    }
}
