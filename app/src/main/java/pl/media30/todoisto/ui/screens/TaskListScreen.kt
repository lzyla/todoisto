package pl.media30.todoisto.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.PaletteColors
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.AppView
import pl.media30.todoisto.ui.QuickAddOverrides
import pl.media30.todoisto.ui.SectionGroup
import pl.media30.todoisto.ui.SortMode
import pl.media30.todoisto.ui.TaskNode
import pl.media30.todoisto.ui.TodoUiState
import pl.media30.todoisto.ui.components.RoutinesSheetContent
import pl.media30.todoisto.ui.components.TaskRowZen
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassPanelTint
import pl.media30.todoisto.ui.theme.GlassRoutine
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.glass
import pl.media30.todoisto.ui.theme.glassClear
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("pl"))
private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl"))

@OptIn(ExperimentalMaterial3Api::class)
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
    routinesExpandedInitially: Boolean = false,
    weekTasks: List<Task> = emptyList()
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var showRoutines by remember { mutableStateOf(routinesExpandedInitially) }
    var showWeek by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }

    LaunchedEffect(quickAddPrefill) {
        if (!quickAddPrefill.isNullOrBlank()) showQuickAdd = true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = uiState.view,
                projects = projects,
                labels = labels,
                uiState = uiState,
                onSelect = {
                    onSelectView(it)
                    scope.launch { drawerState.close() }
                },
                onAddProject = { dialog = DialogKind.AddProject },
                onAddLabel = { dialog = DialogKind.AddLabel },
                onGoalsClick = { dialog = DialogKind.Goals }
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                // ── Górny pasek Zen: ☰ · tytuł · ✦ Tydzień · motyw · ⋮ ─────
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleGlassButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, "Menu", tint = GlassTextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        uiState.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = GlassTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        Modifier
                            .height(42.dp)
                            .glassClear(RoundedCornerShape(21.dp))
                            .bouncy(0.94f) { showWeek = true }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "Tydzień",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GlassTextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    CircleGlassButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            "Motyw",
                            tint = GlassTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        CircleGlassButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "Więcej", tint = GlassTextPrimary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Sortowanie: ${uiState.sortMode.label}") },
                                onClick = { menuOpen = false; sortMenuOpen = true }
                            )
                            val project = uiState.currentProject
                            if (project != null) {
                                DropdownMenuItem(
                                    text = { Text(if (project.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych") },
                                    onClick = { menuOpen = false; onToggleProjectFavorite(project.id) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dodaj sekcję") },
                                    onClick = { menuOpen = false; dialog = DialogKind.AddSection }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplikuj projekt") },
                                    onClick = { menuOpen = false; onDuplicateProject(project.id) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (project.isArchived) "Przywróć z archiwum" else "Archiwizuj projekt") },
                                    onClick = { menuOpen = false; onArchiveProject(project.id, !project.isArchived) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Usuń projekt") },
                                    onClick = { menuOpen = false; onDeleteProject(project.id) }
                                )
                            }
                            val label = uiState.currentLabel
                            if (label != null) {
                                DropdownMenuItem(
                                    text = { Text(if (label.isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych") },
                                    onClick = { menuOpen = false; onToggleLabelFavorite(label.id) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Usuń etykietę") },
                                    onClick = { menuOpen = false; onDeleteLabel(label.id) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Usuń ukończone") },
                                onClick = { menuOpen = false; onClearCompleted() }
                            )
                        }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text((if (mode == uiState.sortMode) "✓ " else "") + mode.label) },
                                    onClick = { sortMenuOpen = false; onSort(mode) }
                                )
                            }
                        }
                    }
                }

                // ── Treść ───────────────────────────────────────────────────
                Box(Modifier.weight(1f)) {
                    Crossfade(targetState = uiState.isEmpty, label = "listOrEmpty") { empty ->
                        if (empty) {
                            EmptyState(uiState.view, hasRoutines = uiState.routines.isNotEmpty())
                        } else {
                            ZenList(uiState, projects, labels, onToggle, onTaskClick)
                        }
                    }
                }
            }

            // ── FAB: pigułka „Dodaj zadanie" ────────────────────────────────
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 96.dp)
                    .height(52.dp)
                    .background(GlassAccent, RoundedCornerShape(26.dp))
                    .bouncy(0.94f) { showQuickAdd = true }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Dodaj zadanie",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // ── Dolny dock + kółko Rutyn ────────────────────────────────────
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    Box(
                        Modifier
                            .size(54.dp)
                            .glassClear(CircleShape)
                            .background(GlassRoutine, CircleShape)
                            .bouncy(0.9f) { showRoutines = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Autorenew, "Rutyny", tint = GlassTextPrimary, modifier = Modifier.size(19.dp))
                    }
                    if (uiState.routines.isNotEmpty()) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .size(19.dp)
                                .background(GlassAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${uiState.routines.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Row(
                    Modifier.glassClear(RoundedCornerShape(30.dp)).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DockTab("Dzisiaj", uiState.view == AppView.Today) { onSelectView(AppView.Today) }
                    DockTab("Nadchodzące", uiState.view == AppView.Upcoming) { onSelectView(AppView.Upcoming) }
                }
            }
        }
    }

    if (showQuickAdd) {
        QuickAddSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            initialText = quickAddPrefill.orEmpty(),
            projects = projects,
            onDismiss = {
                showQuickAdd = false
                onPrefillConsumed()
            },
            onAdd = { text, overrides ->
                onQuickAdd(text, overrides)
                showQuickAdd = false
                onPrefillConsumed()
            }
        )
    }

    if (showRoutines) {
        ModalBottomSheet(
            onDismissRequest = { showRoutines = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface
        ) {
            RoutinesSheetContent(
                routines = uiState.routines,
                doneCount = uiState.routinesDone,
                onComplete = onToggle,
                onAllDone = { showRoutines = false }
            )
        }
    }

    if (showWeek) {
        ModalBottomSheet(
            onDismissRequest = { showWeek = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface
        ) {
            WeekSheetContent(uiState = uiState, weekTasks = weekTasks)
        }
    }

    when (dialog) {
        DialogKind.AddProject -> NameColorDialog(
            title = "Nowy projekt",
            onDismiss = { dialog = null },
            onConfirm = { name, color -> onAddProject(name, color); dialog = null }
        )
        DialogKind.AddLabel -> NameColorDialog(
            title = "Nowa etykieta",
            onDismiss = { dialog = null },
            onConfirm = { name, color -> onAddLabel(name, color); dialog = null }
        )
        DialogKind.AddSection -> NameDialog(
            title = "Nowa sekcja",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                (uiState.view as? AppView.ProjectView)?.let { onAddSection(it.id, name) }
                dialog = null
            }
        )
        DialogKind.Goals -> GoalsDialog(
            daily = uiState.goalDaily,
            weekly = uiState.goalWeekly,
            onDismiss = { dialog = null },
            onConfirm = { d, w -> onSetGoals(d, w); dialog = null }
        )
        null -> {}
    }
}

private enum class DialogKind { AddProject, AddLabel, AddSection, Goals }

@Composable
private fun DockTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) GlassAccent else Color.Transparent,
        label = "dockBg"
    )
    Row(
        Modifier
            .background(bg, RoundedCornerShape(24.dp))
            .bouncy(0.92f, onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) Color.White else GlassTextSecondary
        )
    }
}

@Composable
private fun CircleGlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(42.dp).glass(CircleShape).bouncy(0.9f, onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

// ---- Lista Zen ---------------------------------------------------------------

/** Panel = wspólna tafla glass; wewnątrz bezramkowe wiersze z separatorami. */
private data class ZenSection(val key: String, val title: String?, val nodes: List<TaskNode>)

@Composable
private fun ZenList(
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    val today = LocalDate.now().toEpochDay()
    val sections: List<ZenSection> = when (uiState.view) {
        AppView.Today -> {
            val nodes = uiState.groups.firstOrNull()?.nodes.orEmpty()
            val buckets = linkedMapOf<String, MutableList<TaskNode>>()
            nodes.forEach { n ->
                val m = n.task.dueTimeMinutes
                val bucket = when {
                    m == null -> "W ciągu dnia"
                    m < 12 * 60 -> "Rano"
                    m < 17 * 60 -> "Po południu"
                    else -> "Wieczorem"
                }
                buckets.getOrPut(bucket) { mutableListOf() }.add(n)
            }
            listOf("W ciągu dnia", "Rano", "Po południu", "Wieczorem")
                .mapNotNull { name -> buckets[name]?.let { ZenSection("today-$name", name, it) } }
        }
        AppView.Upcoming -> {
            val nodes = uiState.groups.firstOrNull()?.nodes.orEmpty()
            val byDay = nodes.groupBy { it.task.dueDate ?: Long.MAX_VALUE }.toSortedMap()
            byDay.map { (day, dayNodes) ->
                val title = when {
                    day == Long.MAX_VALUE -> "Bez terminu"
                    day == today + 1 -> "Jutro"
                    day <= today + 7 -> LocalDate.ofEpochDay(day).format(dayFmt)
                        .replaceFirstChar { it.uppercase() }
                    else -> "Później"
                }
                ZenSection("day-$day", title, dayNodes)
            }.let { list ->
                // scal wiele grup "Później" w jedną
                val later = list.filter { it.title == "Później" }
                if (later.size > 1) {
                    list.filter { it.title != "Później" } +
                        ZenSection("later", "Później", later.flatMap { it.nodes })
                } else list
            }
        }
        else -> uiState.groups.map { g -> ZenSection("sec-${g.sectionId}", g.name, g.nodes) }
    }

    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 170.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        sections.forEach { section ->
            if (section.nodes.isEmpty() && section.title == null) return@forEach
            item(key = section.key) {
                val isOpen = expanded.getOrDefault(section.key, true)
                Column(
                    Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                        placementSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                ) {
                    if (section.title != null) {
                        SectionHeader(
                            title = section.title,
                            count = section.nodes.size,
                            open = isOpen,
                            onClick = { expanded[section.key] = !isOpen }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    AnimatedVisibility(
                        visible = isOpen || section.title == null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ZenPanel(section.nodes, uiState, projects, labels, onToggle, onTaskClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, open: Boolean, onClick: () -> Unit) {
    val chevron by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .bouncy(0.97f, onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = GlassTextSecondary
        )
        Spacer(Modifier.width(8.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = GlassTextSecondary.copy(alpha = 0.7f))
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.KeyboardArrowUp,
            null,
            tint = GlassTextSecondary,
            modifier = Modifier.size(16.dp).rotate(chevron)
        )
    }
}

@Composable
private fun ZenPanel(
    nodes: List<TaskNode>,
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        nodes.forEachIndexed { index, node ->
            if (index > 0) {
                HorizontalDivider(color = GlassPanelTint.copy(alpha = 0.14f), thickness = 1.dp)
            }
            TaskRowZen(
                task = node.task,
                metaLine = metaLineFor(node, uiState, projects, labels),
                metaColor = metaColorFor(node.task),
                onToggle = { onToggle(node.task) },
                onOpen = { onTaskClick(node.task) }
            )
            node.subtasks.forEach { sub ->
                TaskRowZen(
                    task = sub,
                    metaLine = "",
                    compact = true,
                    onToggle = { onToggle(sub) },
                    onOpen = { onTaskClick(sub) }
                )
            }
        }
    }
}

private fun metaLineFor(
    node: TaskNode,
    uiState: TodoUiState,
    projects: List<Project>,
    labels: List<Label>
): String {
    val task = node.task
    val today = LocalDate.now().toEpochDay()
    val parts = mutableListOf<String>()

    task.dueDate?.let { due ->
        val showDate = when (uiState.view) {
            AppView.Today -> due < today
            AppView.Upcoming -> false
            else -> true
        }
        if (showDate) {
            parts += when {
                due < today -> "Zaległe"
                due == today -> "Dzisiaj"
                due == today + 1 -> "Jutro"
                else -> LocalDate.ofEpochDay(due).format(shortFmt)
            }
        }
    }
    task.durationMinutes?.let { m ->
        parts += if (m >= 60) {
            if (m % 60 == 0) "${m / 60}h" else "${m / 60}h ${m % 60}min"
        } else "$m min"
    }
    if (task.recurrence != null) parts += "🔁"
    task.deadline?.let { parts += "do " + LocalDate.ofEpochDay(it).format(shortFmt) }
    if (task.priority != Priority.P4) parts += "P${task.priority.ordinal + 1}"
    if (uiState.view !is AppView.ProjectView) {
        projects.firstOrNull { it.id == task.projectId }?.let { parts += "#${it.name}" }
    }
    labels.filter { task.labelIds.contains(it.id) }.forEach { parts += "@${it.name}" }
    if (node.subtasks.isNotEmpty()) {
        parts += "${node.subtasks.count { it.isCompleted }}/${node.subtasks.size}"
    }
    return parts.joinToString(" · ")
}

@Composable
private fun metaColorFor(task: Task): Color {
    val today = LocalDate.now().toEpochDay()
    val overdue = !task.isCompleted && task.dueDate != null && task.dueDate < today
    return if (overdue) Color(0xFFE0564A) else GlassTextSecondary
}

// ---- Panel „Tydzień" -----------------------------------------------------------

@Composable
private fun WeekSheetContent(uiState: TodoUiState, weekTasks: List<Task>) {
    val today = LocalDate.now()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tydzień", style = MaterialTheme.typography.titleLarge, color = GlassTextPrimary)
        }
        Spacer(Modifier.height(12.dp))
        GoalRow("Dzisiaj", uiState.doneToday, uiState.goalDaily)
        Spacer(Modifier.height(6.dp))
        GoalRow("Tydzień", uiState.doneWeek, uiState.goalWeekly)

        // Wnioski asystenta (heurystyki liczone lokalnie)
        val active = weekTasks.filter { it.parentId == null && !it.isCompleted }
        val overdue = active.count { it.dueDate != null && it.dueDate < today.toEpochDay() }
        val counts = (0..6).map { off -> active.count { it.dueDate == today.plusDays(off.toLong()).toEpochDay() } }
        val busiest = counts.withIndex().maxByOrNull { it.value }
        val freeDays = counts.count { it == 0 }
        val insights = buildList {
            if (overdue > 0) add("Masz $overdue zaległych — zacznij dzień od ich przejrzenia.")
            if (busiest != null && busiest.value >= 3) {
                val name = if (busiest.index == 0) "dzisiaj" else if (busiest.index == 1) "jutro"
                else today.plusDays(busiest.index.toLong()).format(dayFmt)
                add("Najbardziej obciążony dzień: $name (${busiest.value} zadań).")
            }
            if (freeDays > 0) add("W tym tygodniu masz $freeDays ${if (freeDays == 1) "wolny dzień" else "wolne dni"}.")
            if (uiState.doneWeek >= uiState.goalWeekly) add("Cel tygodniowy osiągnięty 🎉")
            else add("Do celu tygodniowego brakuje ${uiState.goalWeekly - uiState.doneWeek} zadań.")
        }
        if (insights.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(GlassRoutine, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                insights.forEach { line ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 3.dp)) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(13.dp).padding(top = 1.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(line, style = MaterialTheme.typography.labelMedium, color = GlassTextPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            (0..6).forEach { offset ->
                val date = today.plusDays(offset.toLong())
                val epoch = date.toEpochDay()
                val dayTasks = weekTasks.filter {
                    it.parentId == null && !it.isCompleted && it.dueDate == epoch
                }
                if (offset > 0) HorizontalDivider(color = GlassPanelTint.copy(alpha = 0.14f), thickness = 1.dp)
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        when (offset) {
                            0 -> "Dzisiaj"
                            1 -> "Jutro"
                            else -> date.format(dayFmt).replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GlassTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (dayTasks.isEmpty()) "wolne" else "${dayTasks.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dayTasks.isEmpty()) GlassTextSecondary.copy(alpha = 0.6f) else GlassAccent
                    )
                }
                dayTasks.take(2).forEach { t ->
                    Text(
                        "• ${t.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

// ---- Szuflada ------------------------------------------------------------------

@Composable
private fun DrawerContent(
    current: AppView,
    projects: List<Project>,
    labels: List<Label>,
    uiState: TodoUiState,
    onSelect: (AppView) -> Unit,
    onAddProject: () -> Unit,
    onAddLabel: () -> Unit,
    onGoalsClick: () -> Unit
) {
    val activeProjects = projects.filter { !it.isArchived }
    val archived = projects.filter { it.isArchived }
    val favorites: List<Pair<AppView, Triple<Color, String, ImageVector?>>> =
        projects.filter { it.isFavorite && !it.isArchived }
            .map { AppView.ProjectView(it.id) as AppView to Triple(Color(it.colorArgb), it.name, null as ImageVector?) } +
        labels.filter { it.isFavorite }
            .map { AppView.LabelView(it.id) as AppView to Triple(Color(it.colorArgb), it.name, Icons.Outlined.Sell as ImageVector?) }

    ModalDrawerSheet(
        drawerContainerColor = GlassSurface,
        modifier = Modifier.fillMaxWidth(0.84f)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Todoisto", style = MaterialTheme.typography.headlineSmall, color = GlassTextPrimary)
            Spacer(Modifier.height(14.dp))

            GoalsCard(uiState, onClick = onGoalsClick)
            Spacer(Modifier.height(12.dp))

            DrawerItem(Icons.Outlined.Today, "Dzisiaj", uiState.todayCount, current == AppView.Today) { onSelect(AppView.Today) }
            DrawerItem(Icons.Outlined.DateRange, "Nadchodzące", 0, current == AppView.Upcoming) { onSelect(AppView.Upcoming) }
            DrawerItem(Icons.Outlined.Inbox, "Skrzynka", uiState.inboxCount, current == AppView.Inbox) { onSelect(AppView.Inbox) }
            DrawerItem(Icons.Outlined.CheckCircle, "Ukończone", 0, current == AppView.Completed) { onSelect(AppView.Completed) }

            if (favorites.isNotEmpty()) {
                DrawerHeader("Ulubione", null)
                favorites.forEach { (view, meta) ->
                    DrawerDot(meta.first, meta.second, current == view, icon = meta.third ?: Icons.Filled.Star) {
                        onSelect(view)
                    }
                }
            }

            DrawerHeader("Projekty", onAddProject)
            activeProjects.forEach { p ->
                DrawerDot(Color(p.colorArgb), p.name, current == AppView.ProjectView(p.id)) { onSelect(AppView.ProjectView(p.id)) }
            }
            if (activeProjects.isEmpty()) DrawerHint("Brak projektów")

            DrawerHeader("Etykiety", onAddLabel)
            labels.forEach { l ->
                DrawerDot(Color(l.colorArgb), l.name, current == AppView.LabelView(l.id), icon = Icons.Outlined.Sell) {
                    onSelect(AppView.LabelView(l.id))
                }
            }
            if (labels.isEmpty()) DrawerHint("Brak etykiet")

            if (archived.isNotEmpty()) {
                DrawerHeader("Archiwum", null)
                archived.forEach { p ->
                    DrawerDot(Color(p.colorArgb).copy(alpha = 0.5f), p.name, current == AppView.ProjectView(p.id)) {
                        onSelect(AppView.ProjectView(p.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsCard(uiState: TodoUiState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassPanelTint.copy(alpha = 0.10f))
            .bouncy(scaleDown = 0.97f, onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            "CELE PRODUKTYWNOŚCI",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = GlassTextSecondary
        )
        Spacer(Modifier.height(8.dp))
        GoalRow("Dzisiaj", uiState.doneToday, uiState.goalDaily)
        Spacer(Modifier.height(6.dp))
        GoalRow("Tydzień", uiState.doneWeek, uiState.goalWeekly)
    }
}

@Composable
private fun GoalRow(label: String, done: Int, goal: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = GlassTextPrimary, modifier = Modifier.width(56.dp))
        LinearProgressIndicator(
            progress = { if (goal <= 0) 0f else (done.toFloat() / goal).coerceIn(0f, 1f) },
            color = GlassAccent,
            trackColor = GlassPanelTint.copy(alpha = 0.15f),
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50))
        )
        Spacer(Modifier.width(10.dp))
        Text("$done/$goal", style = MaterialTheme.typography.labelMedium, color = GlassTextSecondary)
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) GlassPanelTint.copy(alpha = 0.16f) else Color.Transparent,
        label = "drawerBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .bouncy(scaleDown = 0.97f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) GlassAccent else GlassTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = GlassTextPrimary, modifier = Modifier.weight(1f))
        if (count > 0) Text("$count", color = GlassTextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DrawerDot(color: Color, label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) GlassPanelTint.copy(alpha = 0.16f) else Color.Transparent,
        label = "drawerBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .bouncy(scaleDown = 0.97f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        } else {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = GlassTextPrimary)
    }
}

@Composable
private fun DrawerHeader(title: String, onAdd: (() -> Unit)?) {
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = GlassTextSecondary, modifier = Modifier.weight(1f))
        if (onAdd != null) {
            IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Dodaj", tint = GlassTextSecondary, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun DrawerHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = GlassTextSecondary.copy(alpha = 0.6f), modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
}

// ---- Quick Add -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    sheetState: androidx.compose.material3.SheetState,
    initialText: String,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onAdd: (String, QuickAddOverrides) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassSurface
    ) {
        QuickAddContent(initialText = initialText, projects = projects, onAdd = onAdd)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun QuickAddContent(
    initialText: String,
    projects: List<Project>,
    onAdd: (String, QuickAddOverrides) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val parsed = remember(text) { QuickAddParser().parse(text) }

    var manualDue by remember { mutableStateOf<Long?>(null) }
    var manualPriority by remember { mutableStateOf<Priority?>(null) }
    var manualProject by remember { mutableStateOf<Project?>(null) }
    var manualRecurrence by remember { mutableStateOf<Recurrence?>(null) }
    var projectMenu by remember { mutableStateOf(false) }
    var recurrenceMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val today = LocalDate.now().toEpochDay()
    val effDue = manualDue ?: parsed.dueDate
    val effPriority = manualPriority ?: parsed.priority
    val effRecurrence = manualRecurrence ?: parsed.recurrence
    val activeProjects = projects.filter { !it.isArchived }

    fun overrides() = QuickAddOverrides(
        dueDate = manualDue,
        priority = manualPriority,
        projectId = manualProject?.id,
        recurrence = manualRecurrence
    )

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Co jest do zrobienia?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            shape = RoundedCornerShape(22.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAdd(text, overrides()) }),
            colors = glassFieldColors()
        )

        Spacer(Modifier.height(14.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChoiceChip("Dzisiaj", selected = effDue == today) {
                manualDue = if (manualDue == today) null else today
            }
            ChoiceChip("Jutro", selected = effDue == today + 1) {
                manualDue = if (manualDue == today + 1) null else today + 1
            }
            val customDate = effDue?.takeIf { it != today && it != today + 1 }
            ChoiceChip(
                text = customDate?.let { "📅 " + LocalDate.ofEpochDay(it).format(shortFmt) } ?: "📅 Data",
                selected = customDate != null
            ) { showDatePicker = true }
        }

        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Priority.entries.forEach { p ->
                ChoiceChip(
                    text = "P${p.ordinal + 1}",
                    selected = effPriority == p,
                    tint = p.color
                ) { manualPriority = if (manualPriority == p) null else p }
            }
        }

        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                ChoiceChip(
                    text = "# " + (manualProject?.name ?: parsed.projectName ?: "Skrzynka"),
                    selected = manualProject != null || parsed.projectName != null
                ) { projectMenu = true }
                DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Skrzynka") },
                        onClick = { manualProject = null; projectMenu = false }
                    )
                    activeProjects.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = { manualProject = p; projectMenu = false }
                        )
                    }
                }
            }
            Box {
                ChoiceChip(
                    text = "🔁 " + (effRecurrence?.label ?: "Powtarzaj"),
                    selected = effRecurrence != null
                ) { recurrenceMenu = true }
                DropdownMenu(expanded = recurrenceMenu, onDismissRequest = { recurrenceMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Nie powtarzaj") },
                        onClick = { manualRecurrence = null; recurrenceMenu = false }
                    )
                    Recurrence.entries.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.label) },
                            onClick = { manualRecurrence = r; recurrenceMenu = false }
                        )
                    }
                }
            }
        }

        val extras = parsed.dueTimeMinutes != null || parsed.durationMinutes != null ||
            parsed.labelNames.isNotEmpty() || parsed.deadline != null
        if (extras) {
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                parsed.dueTimeMinutes?.let { PreviewChip("🕒 %02d:%02d".format(it / 60, it % 60)) }
                parsed.durationMinutes?.let { PreviewChip("⏱ $it min") }
                parsed.labelNames.forEach { PreviewChip("@ $it") }
                parsed.deadline?.let { PreviewChip("⏳ do " + LocalDate.ofEpochDay(it).format(shortFmt)) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(GlassAccent, RoundedCornerShape(26.dp))
                .bouncy(0.96f) { if (text.isNotBlank()) onAdd(text, overrides()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Dodaj", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }

    if (showDatePicker) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = effDue?.let { it * 24L * 60 * 60 * 1000 }
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        manualDue = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") } }
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    tint: Color = GlassAccent,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) tint.copy(alpha = 0.16f) else GlassPanelTint.copy(alpha = 0.07f),
        label = "choiceBg"
    )
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .bouncy(scaleDown = 0.9f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) tint else GlassTextSecondary
        )
    }
}

@Composable
private fun PreviewChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = GlassTextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(GlassPanelTint.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

// ---- Dialogi -------------------------------------------------------------------

@Composable
private fun NameColorDialog(title: String, onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PaletteColors.options.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = { Text(title, color = GlassTextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaletteColors.options.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(
                                    if (c == color) Modifier.padding(0.dp) else Modifier
                                )
                                .clickable { color = c }
                        ) {
                            if (c == color) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, color) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun NameDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = { Text(title, color = GlassTextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa") },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = glassFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun GoalsDialog(daily: Int, weekly: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var dailyText by remember { mutableStateOf(daily.toString()) }
    var weeklyText by remember { mutableStateOf(weekly.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = { Text("Cele produktywności", color = GlassTextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = dailyText,
                    onValueChange = { dailyText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Cel dzienny (zadania)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(22.dp),
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = weeklyText,
                    onValueChange = { weeklyText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Cel tygodniowy (zadania)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(22.dp),
                    colors = glassFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = dailyText.toIntOrNull() ?: daily
                val w = weeklyText.toIntOrNull() ?: weekly
                onConfirm(d.coerceAtLeast(1), w.coerceAtLeast(1))
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun EmptyState(view: AppView, hasRoutines: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CheckCircle,
                null,
                tint = GlassPanelTint.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp).padding(bottom = 12.dp)
            )
            Text(
                text = when {
                    view == AppView.Today && hasRoutines -> "Zostały tylko rutyny 🔁"
                    view == AppView.Today -> "Nic na dzisiaj 🎉"
                    view == AppView.Completed -> "Brak ukończonych zadań"
                    view == AppView.Upcoming -> "Brak nadchodzących zadań"
                    view == AppView.Inbox -> "Skrzynka pusta"
                    else -> "Brak zadań tutaj"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = GlassTextSecondary
            )
        }
    }
}
