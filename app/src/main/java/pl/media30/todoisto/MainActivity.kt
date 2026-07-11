package pl.media30.todoisto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.TodoViewModel
import pl.media30.todoisto.ui.screens.AddEditTaskScreen
import pl.media30.todoisto.ui.screens.TaskListScreen
import pl.media30.todoisto.ui.theme.TodoistoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TodoApplication
        setContent {
            TodoistoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoistoApp(
                        viewModel = viewModel(
                            factory = TodoViewModel.Factory(app.repository)
                        )
                    )
                }
            }
        }
    }
}

private object Routes {
    const val LIST = "list"
    const val ADD = "add"
    const val EDIT = "edit/{taskId}"
    fun edit(taskId: Long) = "edit/$taskId"
}

@Composable
fun TodoistoApp(viewModel: TodoViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            TaskListScreen(
                uiState = uiState,
                onSelectFilter = viewModel::setFilter,
                onToggle = viewModel::toggleCompleted,
                onTaskClick = { navController.navigate(Routes.edit(it.id)) },
                onAddClick = { navController.navigate(Routes.ADD) },
                onClearCompleted = viewModel::deleteCompleted
            )
        }
        composable(Routes.ADD) {
            AddEditTaskScreen(
                existing = null,
                onSave = { title, notes, priority, dueDate ->
                    viewModel.addTask(title, notes, priority, dueDate)
                    navController.popBackStack()
                },
                onDelete = null,
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            var task by remember { mutableStateOf<Task?>(null) }
            var loaded by remember { mutableStateOf(false) }

            LaunchedEffect(taskId) {
                task = viewModel.getTask(taskId)
                loaded = true
            }

            val current = task
            if (loaded && current == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else if (current != null) {
                AddEditTaskScreen(
                    existing = current,
                    onSave = { title, notes, priority, dueDate ->
                        viewModel.updateTask(
                            current.copy(
                                title = title,
                                notes = notes,
                                priority = priority,
                                dueDate = dueDate
                            )
                        )
                        navController.popBackStack()
                    },
                    onDelete = {
                        viewModel.deleteTask(current)
                        navController.popBackStack()
                    },
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}
