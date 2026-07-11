package pl.media30.todoisto.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.PaletteColors
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.AppView
import pl.media30.todoisto.ui.SectionGroup
import pl.media30.todoisto.ui.SortMode
import pl.media30.todoisto.ui.TodoUiState
import pl.media30.todoisto.ui.components.RoutinesBar
import pl.media30.todoisto.ui.components.TaskItem
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassPanelTint
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onQuickAdd: (String) -> Unit,
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
    routinesExpandedInitially: Boolean = false
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
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
                isDarkTheme = isDarkTheme,
                onSelect = {
                    onSelectView(it)
                    scope.launch { drawerState.close() }
                },
                onAddProject = { dialog = DialogKind.AddProject },
                onAddLabel = { dialog = DialogKind.AddLabel },
                onGoalsClick = { dialog = DialogKind.Goals },
                onToggleTheme = onToggleTheme
            )
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(uiState.title, fontWeight = FontWeight.Bold, color = GlassTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.AutoMirrored.Filled.List, "Menu", tint = GlassTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GlassPanelTint.copy(alpha = 0.08f),
                        titleContentColor = GlassTextPrimary
                    ),
                    actions = {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "Więcej", tint = GlassTextPrimary)
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
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showQuickAdd = true },
                    shape = RoundedCornerShape(50),
                    containerColor = GlassAccent,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Dodaj zadanie", fontWeight = FontWeight.SemiBold) }
                )
            },
            bottomBar = {
                if (uiState.view == AppView.Today) {
                    RoutinesBar(
                        routines = uiState.routines,
                        doneCount = uiState.routinesDone,
                        onComplete = onToggle,
                        initiallyExpanded = routinesExpandedInitially
                    )
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                Crossfade(targetState = uiState.isEmpty, label = "listOrEmpty") { empty ->
                    if (empty) {
                        EmptyState(uiState.view, hasRoutines = uiState.routines.isNotEmpty())
                    } else {
                        TaskList(uiState.groups, labels, onToggle, onTaskClick)
                    }
                }
            }
        }
    }

    if (showQuickAdd) {
        QuickAddSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            initialText = quickAddPrefill.orEmpty(),
            onDismiss = {
                showQuickAdd = false
                onPrefillConsumed()
            },
            onAdd = { text ->
                onQuickAdd(text)
                showQuickAdd = false
                onPrefillConsumed()
            }
        )
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
private fun TaskList(
    groups: List<SectionGroup>,
    labels: List<Label>,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groups.forEach { group ->
            if (group.name != null) {
                item(key = "sec-${group.sectionId}") {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                    )
                }
            }
            items(group.nodes, key = { it.task.id }) { node ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.animateItem(
                        placementSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    )
                ) {
                    TaskItem(
                        task = node.task,
                        labels = labels.filter { node.task.labelIds.contains(it.id) },
                        onToggle = { onToggle(node.task) },
                        onClick = { onTaskClick(node.task) },
                        subtaskDone = node.subtasks.count { it.isCompleted },
                        subtaskTotal = node.subtasks.size,
                        modifier = Modifier.fillMaxWidth()
                    )
                    node.subtasks.forEach { sub ->
                        TaskItem(
                            task = sub,
                            labels = emptyList(),
                            onToggle = { onToggle(sub) },
                            onClick = { onTaskClick(sub) },
                            compact = true,
                            modifier = Modifier.fillMaxWidth().padding(start = 28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---- Drawer ----------------------------------------------------------------

@Composable
private fun DrawerContent(
    current: AppView,
    projects: List<Project>,
    labels: List<Label>,
    uiState: TodoUiState,
    isDarkTheme: Boolean,
    onSelect: (AppView) -> Unit,
    onAddProject: () -> Unit,
    onAddLabel: () -> Unit,
    onGoalsClick: () -> Unit,
    onToggleTheme: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Todoisto",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GlassTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Zmień motyw",
                        tint = GlassTextSecondary
                    )
                }
            }

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
            .border(1.dp, GlassPanelTint.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .bouncy(scaleDown = 0.97f, onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            "Cele produktywności",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
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
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GlassTextSecondary, modifier = Modifier.weight(1f))
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun QuickAddSheet(
    sheetState: androidx.compose.material3.SheetState,
    initialText: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val parsed = remember(text) { QuickAddParser().parse(text) }
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GlassSurface
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("np. Zadzwonić do Beaty jutro o 15:00 2h #fundacja p1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAdd(text) }),
                colors = glassFieldColors()
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                parsed.dueDate?.let { PreviewChip("📅 " + dueText(it, fmt)) }
                parsed.dueTimeMinutes?.let { PreviewChip("🕒 %02d:%02d".format(it / 60, it % 60)) }
                parsed.durationMinutes?.let { PreviewChip("⏱ " + durationText(it)) }
                if (parsed.recurrence != null) PreviewChip("🔁 " + parsed.recurrence!!.label)
                if (parsed.priority.ordinal < 3) PreviewChip("🚩 P${parsed.priority.ordinal + 1}")
                parsed.projectName?.let { PreviewChip("# $it") }
                parsed.labelNames.forEach { PreviewChip("@ $it") }
                parsed.deadline?.let { PreviewChip("⏳ do " + dueText(it, fmt)) }
            }
            Spacer(Modifier.height(16.dp))
            ExtendedFloatingActionButton(
                onClick = { if (text.isNotBlank()) onAdd(text) },
                shape = RoundedCornerShape(50),
                containerColor = GlassAccent,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Dodaj", fontWeight = FontWeight.SemiBold) }
            )
        }
    }
}

private fun durationText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
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
            .background(GlassPanelTint.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun dueText(epochDay: Long, fmt: DateTimeFormatter): String {
    val today = LocalDate.now().toEpochDay()
    return when (epochDay) {
        today -> "Dzisiaj"
        today + 1 -> "Jutro"
        else -> LocalDate.ofEpochDay(epochDay).format(fmt)
    }
}

// ---- Dialogs ---------------------------------------------------------------

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
                                .border(if (c == color) 3.dp else 0.dp, GlassTextPrimary, CircleShape)
                                .clickable { color = c }
                        )
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
