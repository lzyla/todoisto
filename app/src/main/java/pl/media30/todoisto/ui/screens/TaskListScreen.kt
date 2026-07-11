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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DrawerValue
import kotlinx.coroutines.launch
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.PaletteColors
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.AppView
import pl.media30.todoisto.ui.SectionGroup
import pl.media30.todoisto.ui.TodoUiState
import pl.media30.todoisto.ui.components.TaskItem
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.components.glassFieldColors
import pl.media30.todoisto.ui.theme.GlassAccent
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
    onSelectView: (AppView) -> Unit,
    onToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onQuickAdd: (String) -> Unit,
    onAddProject: (String, Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onAddLabel: (String, Long) -> Unit,
    onAddSection: (Long, String) -> Unit,
    onClearCompleted: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = uiState.view,
                projects = projects,
                labels = labels,
                todayCount = uiState.todayCount,
                inboxCount = uiState.inboxCount,
                onSelect = {
                    onSelectView(it)
                    scope.launch { drawerState.close() }
                },
                onAddProject = { dialog = DialogKind.AddProject },
                onAddLabel = { dialog = DialogKind.AddLabel }
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
                        containerColor = Color.White.copy(alpha = 0.06f),
                        titleContentColor = GlassTextPrimary
                    ),
                    actions = {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "Więcej", tint = GlassTextPrimary)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (uiState.view is AppView.ProjectView) {
                                DropdownMenuItem(
                                    text = { Text("Dodaj sekcję") },
                                    onClick = { menuOpen = false; dialog = DialogKind.AddSection }
                                )
                                DropdownMenuItem(
                                    text = { Text("Usuń projekt") },
                                    onClick = { menuOpen = false; onDeleteProject((uiState.view as AppView.ProjectView).id) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Usuń ukończone") },
                                onClick = { menuOpen = false; onClearCompleted() }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showQuickAdd = true },
                    shape = RoundedCornerShape(50),
                    containerColor = Color.White,
                    contentColor = Color(0xFF6B3FE0),
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Dodaj zadanie", fontWeight = FontWeight.SemiBold) }
                )
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                Crossfade(targetState = uiState.isEmpty, label = "listOrEmpty") { empty ->
                    if (empty) {
                        EmptyState(uiState.view)
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
            onDismiss = { showQuickAdd = false },
            onAdd = { text ->
                onQuickAdd(text)
                showQuickAdd = false
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
        null -> {}
    }
}

private enum class DialogKind { AddProject, AddLabel, AddSection }

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
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
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
    todayCount: Int,
    inboxCount: Int,
    onSelect: (AppView) -> Unit,
    onAddProject: () -> Unit,
    onAddLabel: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF6B3FE0),
        modifier = Modifier.fillMaxWidth(0.82f)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Todoisto", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = GlassTextPrimary)
            Spacer(Modifier.height(20.dp))

            DrawerItem(Icons.Outlined.Today, "Dzisiaj", todayCount, current == AppView.Today) { onSelect(AppView.Today) }
            DrawerItem(Icons.Outlined.DateRange, "Nadchodzące", 0, current == AppView.Upcoming) { onSelect(AppView.Upcoming) }
            DrawerItem(Icons.Outlined.Inbox, "Skrzynka", inboxCount, current == AppView.Inbox) { onSelect(AppView.Inbox) }
            DrawerItem(Icons.Outlined.CheckCircle, "Ukończone", 0, current == AppView.Completed) { onSelect(AppView.Completed) }

            DrawerHeader("Projekty", onAddProject)
            projects.forEach { p ->
                DrawerDot(Color(p.colorArgb), p.name, current == AppView.ProjectView(p.id)) { onSelect(AppView.ProjectView(p.id)) }
            }
            if (projects.isEmpty()) DrawerHint("Brak projektów")

            DrawerHeader("Etykiety", onAddLabel)
            labels.forEach { l ->
                DrawerDot(Color(l.colorArgb), l.name, current == AppView.LabelView(l.id), icon = Icons.Outlined.Sell) {
                    onSelect(AppView.LabelView(l.id))
                }
            }
            if (labels.isEmpty()) DrawerHint("Brak etykiet")
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
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
        Icon(icon, null, tint = if (selected) Color.White else GlassTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (selected) Color.White else GlassTextPrimary, modifier = Modifier.weight(1f))
        if (count > 0) Text("$count", color = GlassTextSecondary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DrawerDot(color: Color, label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
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
        Text(label, color = if (selected) Color.White else GlassTextPrimary)
    }
}

@Composable
private fun DrawerHeader(title: String, onAdd: () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GlassTextSecondary, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Dodaj", tint = GlassTextSecondary, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun DrawerHint(text: String) {
    Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = GlassTextSecondary.copy(alpha = 0.6f), modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
}

// ---- Quick Add -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun QuickAddSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val parsed = remember(text) { QuickAddParser().parse(text) }
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pl")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF6B3FE0)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("np. Zadzwonić do Beaty jutro o 15:00 #fundacja p1") },
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
                containerColor = Color.White,
                contentColor = Color(0xFF6B3FE0),
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Dodaj", fontWeight = FontWeight.SemiBold) }
            )
        }
    }
}

@Composable
private fun PreviewChip(text: String) {
    Text(
        text,
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        color = GlassTextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
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
        containerColor = Color(0xFF6B3FE0),
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
                                .border(if (c == color) 3.dp else 0.dp, Color.White, CircleShape)
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
        containerColor = Color(0xFF6B3FE0),
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
private fun EmptyState(view: AppView) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CheckCircle,
                null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(72.dp).padding(bottom = 12.dp)
            )
            Text(
                text = when (view) {
                    AppView.Completed -> "Brak ukończonych zadań"
                    AppView.Today -> "Nic na dzisiaj 🎉"
                    AppView.Upcoming -> "Brak nadchodzących zadań"
                    AppView.Inbox -> "Skrzynka pusta"
                    else -> "Brak zadań tutaj"
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = GlassTextSecondary
            )
        }
    }
}
