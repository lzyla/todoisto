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
            GlassTheme.dark = dark
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

    var detailTaskId by remember { mutableStateOf<Long?>(null) }

    TaskListScreen(
        uiState = uiState,
        projects = projects,
        labels = labels,
        isDarkTheme = darkTheme,
        quickAddPrefill = quickAddPrefill,
        onPrefillConsumed = onPrefillConsumed,
        onSelectView = viewModel::setView,
        onToggle = viewModel::toggleCompleted,
        onTaskClick = { detailTaskId = it.id },
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
        weekTasks = allTasks
    )

    val detailTask = detailTaskId?.let { id -> allTasks.firstOrNull { it.id == id } }
    if (detailTask != null) {
        ModalBottomSheet(
            onDismissRequest = { detailTaskId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GlassSurface
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
                onClose = { detailTaskId = null }
            )
        }
    }
}
