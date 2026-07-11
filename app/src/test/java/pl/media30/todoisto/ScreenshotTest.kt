package pl.media30.todoisto

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.AppView
import pl.media30.todoisto.ui.SectionGroup
import pl.media30.todoisto.ui.TaskNode
import pl.media30.todoisto.ui.TodoUiState
import pl.media30.todoisto.ui.screens.AddEditTaskScreen
import pl.media30.todoisto.ui.screens.TaskListScreen
import pl.media30.todoisto.ui.theme.GlassBackground
import pl.media30.todoisto.ui.theme.GlassTheme
import pl.media30.todoisto.ui.theme.TodoistoTheme
import java.time.LocalDate

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private val today = LocalDate.now().toEpochDay()

    private val projects = listOf(
        Project(1, "Fundacja", 0xFFC24DFF, isFavorite = true),
        Project(2, "Dom", 0xFF34D399)
    )
    private val labels = listOf(
        Label(1, "pilne", 0xFFF87171),
        Label(2, "email", 0xFF4D6BFF)
    )

    private val tasks = listOf(
        Task(1, "Zadzwonić do Beaty", "Ustalić budżet", false, Priority.P1, today, durationMinutes = 30, projectId = 1, labelIds = listOf(1)),
        Task(2, "Wysłać raport kwartalny", "", false, Priority.P2, today + 1, deadline = today + 3, projectId = 1),
        Task(4, "Kupić mleko", "", false, Priority.P4, null, labelIds = listOf(2)),
        Task(5, "Przygotować prezentację", "", false, Priority.P3, today - 1, durationMinutes = 120)
    )

    private val routines = listOf(
        Task(3, "Podlać kwiaty", "", false, Priority.P4, today, recurrence = Recurrence.WEEKLY),
        Task(6, "Medytacja", "", false, Priority.P4, today, recurrence = Recurrence.DAILY),
        Task(7, "Czytanie 20 stron", "", false, Priority.P4, today, recurrence = Recurrence.DAILY)
    )

    private fun uiState() = TodoUiState(
        view = AppView.Today,
        title = "Dzisiaj",
        groups = listOf(SectionGroup(null, null, tasks.map { TaskNode(it, emptyList()) })),
        isEmpty = false,
        todayCount = 3,
        inboxCount = 1,
        doneToday = 3,
        doneWeek = 12,
        routines = routines,
        routinesDone = 2
    )

    @Composable
    private fun listScreen(routinesExpanded: Boolean = false) {
        TaskListScreen(
            routinesExpandedInitially = routinesExpanded,
            uiState = uiState(),
            projects = projects,
            labels = labels,
            isDarkTheme = GlassTheme.dark,
            quickAddPrefill = null,
            onPrefillConsumed = {},
            onSelectView = {},
            onToggle = {},
            onTaskClick = {},
            onQuickAdd = {},
            onAddProject = { _, _ -> },
            onDeleteProject = {},
            onDuplicateProject = {},
            onArchiveProject = { _, _ -> },
            onToggleProjectFavorite = {},
            onToggleLabelFavorite = {},
            onDeleteLabel = {},
            onAddLabel = { _, _ -> },
            onAddSection = { _, _ -> },
            onClearCompleted = {},
            onSort = {},
            onSetGoals = { _, _ -> },
            onToggleTheme = {}
        )
    }

    @Test
    fun taskListLight() {
        GlassTheme.dark = false
        paparazzi.snapshot {
            TodoistoTheme { GlassBackground { listScreen() } }
        }
    }

    @Test
    fun taskListRoutinesExpanded() {
        GlassTheme.dark = false
        paparazzi.snapshot {
            TodoistoTheme { GlassBackground { listScreen(routinesExpanded = true) } }
        }
    }

    @Test
    fun taskListDark() {
        GlassTheme.dark = true
        paparazzi.snapshot {
            TodoistoTheme { GlassBackground { listScreen() } }
        }
        GlassTheme.dark = false
    }

    @Test
    fun editTask() {
        GlassTheme.dark = false
        paparazzi.snapshot {
            TodoistoTheme {
                GlassBackground {
                    AddEditTaskScreen(
                        existing = Task(
                            id = 1, title = "Zadzwonić do Beaty", notes = "Ustalić budżet",
                            priority = Priority.P1, dueDate = today, deadline = today + 3,
                            recurrence = Recurrence.WEEKLY, durationMinutes = 30,
                            projectId = 1, labelIds = listOf(1)
                        ),
                        projects = projects,
                        labels = labels,
                        subtasks = listOf(Task(10, "Przygotować pytania", parentId = 1)),
                        onSave = {},
                        onAddSubtask = {},
                        onToggleSubtask = {},
                        onDuplicate = {},
                        onDelete = {},
                        onClose = {}
                    )
                }
            }
        }
    }
}
