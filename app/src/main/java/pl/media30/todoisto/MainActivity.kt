package pl.media30.todoisto

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.media30.todoisto.ui.TodoViewModel
import pl.media30.todoisto.ui.screens.AddEditTaskScreen
import pl.media30.todoisto.ui.screens.TaskListScreen
import pl.media30.todoisto.ui.theme.GlassBackground
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

private object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{taskId}"
    fun edit(taskId: Long) = "edit/$taskId"
}

@Composable
fun TodoistoApp(
    viewModel: TodoViewModel,
    quickAddPrefill: String? = null,
    onPrefillConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            TaskListScreen(
                uiState = uiState,
                projects = projects,
                labels = labels,
                isDarkTheme = darkTheme,
                quickAddPrefill = quickAddPrefill,
                onPrefillConsumed = onPrefillConsumed,
                onSelectView = viewModel::setView,
                onToggle = viewModel::toggleCompleted,
                onTaskClick = { navController.navigate(Routes.edit(it.id)) },
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
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            val existing = allTasks.firstOrNull { it.id == taskId }
            val subtasks = allTasks.filter { it.parentId == taskId }

            if (existing == null) {
                androidx.compose.runtime.LaunchedEffect(taskId) { navController.popBackStack() }
            } else {
                AddEditTaskScreen(
                    existing = existing,
                    projects = projects,
                    labels = labels,
                    subtasks = subtasks,
                    onSave = { updated ->
                        viewModel.updateTask(updated)
                        navController.popBackStack()
                    },
                    onAddSubtask = { viewModel.addSubtask(taskId, existing.projectId, it) },
                    onToggleSubtask = { viewModel.toggleCompleted(it) },
                    onDuplicate = {
                        viewModel.duplicateTask(taskId)
                        navController.popBackStack()
                    },
                    onDelete = {
                        viewModel.deleteTask(existing)
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
