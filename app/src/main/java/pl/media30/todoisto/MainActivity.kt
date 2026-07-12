package pl.media30.todoisto

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.media30.todoisto.ui.TodoViewModel
import pl.media30.todoisto.ui.screens.TaskDetailSheet
import pl.media30.todoisto.ui.screens.TaskListScreen
import pl.media30.todoisto.ui.theme.GlassBackground
import pl.media30.todoisto.ui.theme.GlassSurface
import pl.media30.todoisto.ui.theme.GlassTheme
import pl.media30.todoisto.ui.theme.TodoistoTheme

class MainActivity : ComponentActivity() {

    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = extractSharedText(intent)
        val app = application as TodoApplication
        setContent {
            val dark by app.settings.darkTheme.collectAsState()
            val photoBg by app.settings.photoBackground.collectAsState()
            GlassTheme.dark = dark
            GlassTheme.photo = photoBg
            GlassTheme.phase = pl.media30.todoisto.ui.theme.dayPhaseFromClock()
            TodoistoTheme {
                GlassBackground {
                    TodoistoApp(
                        viewModel = viewModel(factory = TodoViewModel.Factory(app.repository, app.settings)),
                        quickAddPrefill = sharedText,
                        onPrefillConsumed = { sharedText = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractSharedText(intent)?.let { sharedText = it }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("text/") != true) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        return when {
            subject.isNotEmpty() && text.isNotEmpty() -> "$subject $text"
            text.isNotEmpty() -> text
            subject.isNotEmpty() -> subject
            else -> null
        }
    }
}

/**
 * Jednoekranowa nawigacja w duchu prototypu: lista + nakładki (arkusz
 * szczegółów zadania, Quick Add, Rutyny, Tydzień) zamiast osobnych ekranów.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoistoApp(
    viewModel: TodoViewModel,
    quickAddPrefill: String? = null,
    onPrefillConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()
    val photoBg by viewModel.photoBackground.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val freeTime by viewModel.freeTime.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val activeAreaId by viewModel.activeArea.collectAsState()
    val todayOpen by viewModel.todayOpen.collectAsState()
    val openAiKey by viewModel.openAiKey.collectAsState()
    val aiAsk by viewModel.aiAsk.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var detailTaskId by remember { mutableStateOf<Long?>(null) }
    var showPool by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showEstimate by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var detailAiExpanded by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<pl.media30.todoisto.data.Activity?>(null) }

    val importResult by viewModel.importResult.collectAsState()
    androidx.compose.runtime.LaunchedEffect(importResult) {
        importResult?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearImportResult()
        }
    }

    TaskListScreen(
        uiState = uiState,
        projects = projects,
        labels = labels,
        isDarkTheme = darkTheme,
        quickAddPrefill = quickAddPrefill,
        onPrefillConsumed = onPrefillConsumed,
        onSelectView = viewModel::setView,
        onToggle = viewModel::toggleCompleted,
        onTaskClick = { detailTaskId = it.id; detailAiExpanded = false; viewModel.dismissAi() },
        onQuickAdd = viewModel::quickAdd,
        onAddProject = viewModel::addProject,
        onDeleteProject = viewModel::deleteProject,
        onDuplicateProject = viewModel::duplicateProject,
        onArchiveProject = viewModel::archiveProject,
        onToggleProjectFavorite = viewModel::toggleProjectFavorite,
        onToggleLabelFavorite = viewModel::toggleLabelFavorite,
        onDeleteLabel = viewModel::deleteLabel,
        onAddLabel = viewModel::addLabel,
        onAddSection = viewModel::addSection,
        onClearCompleted = viewModel::deleteCompleted,
        onSort = viewModel::setSort,
        onSetGoals = viewModel::setGoals,
        onToggleTheme = { viewModel.setDarkTheme(!darkTheme) },
        onMoveOverdueToToday = viewModel::moveOverdueToToday,
        onOpenActivityPool = { showPool = true },
        onFreeTime = { viewModel.suggestFreeTime() },
        isPhotoBackground = photoBg,
        onTogglePhotoBackground = { viewModel.setPhotoBackground(!photoBg) },
        activities = activities,
        onAddActivityQuick = { showPool = true },
        onSharePlan = {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Plan dnia — Todoisto")
                putExtra(android.content.Intent.EXTRA_TEXT, viewModel.buildDayPlanText())
            }
            context.startActivity(android.content.Intent.createChooser(send, "Wyślij plan dnia"))
        },
        onDeferToTomorrow = viewModel::deferToTomorrow,
        onOpenAutomation = { task ->
            detailTaskId = task.id
            detailAiExpanded = true
            // Klik w ikonę AI = od razu odpowiedź (jak „Zróbmy to z AI").
            val tip = pl.media30.todoisto.data.AutomationAdvisor.advise(task.title, task.notes)
            viewModel.askAi(tip.aiPrompt)
        },
        areas = areas,
        activeAreaId = activeAreaId,
        onSelectArea = viewModel::setActiveArea,
        onAddArea = viewModel::addArea,
        onOpenEstimate = { showEstimate = true },
        hasApiKey = openAiKey.isNotBlank(),
        onSetApiKey = viewModel::setOpenAiKey,
        onOpenSettings = { showSettings = true },
        weekTasks = allTasks
    )

    // Ekran ustawień (liquid glass) — nakładka pełnoekranowa nad listą.
    if (showSettings) {
        pl.media30.todoisto.ui.screens.SettingsScreen(
            dark = darkTheme,
            photo = photoBg,
            hasApiKey = openAiKey.isNotBlank(),
            dailyGoal = uiState.goalDaily,
            weeklyGoal = uiState.goalWeekly,
            onBack = { showSettings = false },
            onToggleDark = { viewModel.setDarkTheme(!darkTheme) },
            onTogglePhoto = { viewModel.setPhotoBackground(!photoBg) },
            onSetApiKey = viewModel::setOpenAiKey,
            onSetGoals = viewModel::setGoals,
            onOpenPool = { showSettings = false; showPool = true },
            onOpenImport = { showSettings = false; showImport = true }
        )
    }

    // Panel „Czas wolny"
    freeTime?.let { ft ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissFreeTime() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            pl.media30.todoisto.ui.screens.FreeTimePanel(
                state = ft,
                onAccept = viewModel::acceptSuggestion,
                onReroll = viewModel::rerollSuggestion,
                onAddFirst = { viewModel.dismissFreeTime(); editingActivity = null; showForm = true }
            )
        }
    }

    // Katalog puli
    if (showPool) {
        ModalBottomSheet(
            onDismissRequest = { showPool = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            pl.media30.todoisto.ui.screens.ActivityPoolSheet(
                activities = activities,
                onAdd = { editingActivity = null; showForm = true },
                onEdit = { editingActivity = it; showForm = true },
                onImport = { showImport = true }
            )
        }
    }

    // Formularz aktywności
    if (showForm) {
        ModalBottomSheet(
            onDismissRequest = { showForm = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            pl.media30.todoisto.ui.screens.ActivityFormSheet(
                existing = editingActivity,
                onSave = { a -> if (editingActivity == null) viewModel.addActivity(a) else viewModel.updateActivity(a) },
                onDelete = editingActivity?.let { a -> { viewModel.deleteActivity(a.id); showForm = false } },
                onClose = { showForm = false }
            )
        }
    }

    if (showEstimate) {
        ModalBottomSheet(
            onDismissRequest = { showEstimate = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            pl.media30.todoisto.ui.screens.EstimateBreakdownSheet(items = todayOpen, projects = projects)
        }
    }

    if (showImport) {
        ImportActivitiesDialog(
            onDismiss = { showImport = false },
            onImport = { url -> viewModel.importActivitiesFromCsv(url); showImport = false }
        )
    }

    val detailTask = detailTaskId?.let { id -> allTasks.firstOrNull { it.id == id } }
    if (detailTask != null) {
        ModalBottomSheet(
            onDismissRequest = { detailTaskId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            TaskDetailSheet(
                task = detailTask,
                projects = projects,
                labels = labels,
                subtasks = allTasks.filter { it.parentId == detailTask.id },
                onPatch = viewModel::updateTask,
                onToggleSubtask = viewModel::toggleCompleted,
                onAddSubtask = { viewModel.addSubtask(detailTask.id, detailTask.projectId, it) },
                onDuplicate = {
                    viewModel.duplicateTask(detailTask.id)
                    detailTaskId = null
                },
                onDelete = {
                    viewModel.deleteTask(detailTask)
                    detailTaskId = null
                },
                onClose = { detailTaskId = null; viewModel.dismissAi() },
                onAskAi = viewModel::askAi,
                aiExpanded = detailAiExpanded,
                aiState = aiAsk
            )
        }
    }

}

/** Dialog importu aktywności z opublikowanego arkusza Google (link CSV). */
@Composable
private fun ImportActivitiesDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onImport(url) }, enabled = url.isNotBlank()) {
                androidx.compose.material3.Text("Importuj")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Anuluj") }
        },
        title = { androidx.compose.material3.Text("Import z Google Sheets") },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(
                    "Wklej link do arkusza opublikowanego jako CSV (Plik → Udostępnij → Opublikuj w internecie → CSV) lub zwykły link do arkusza.\n\nKolumny: nazwa, minuty, wysiłek (fizyczny/umysłowy/regeneracja), miejsce, energia.",
                    fontSize = 12.5.sp
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { androidx.compose.material3.Text("https://docs.google.com/…") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
