package pl.media30.todoisto

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
        Project(1, "Fundacja", 0xFFC24DFF),
        Project(2, "Dom", 0xFF34D399)
    )
    private val labels = listOf(
        Label(1, "pilne", 0xFFF87171),
        Label(2, "email", 0xFF4D6BFF)
    )

    private val tasks = listOf(
        Task(1, "Zadzwonić do Beaty", "Ustalić budżet", false, Priority.P1, today, projectId = 1, labelIds = listOf(1)),
        Task(2, "Wysłać raport kwartalny", "", false, Priority.P2, today + 1, deadline = today + 3, projectId = 1),
        Task(3, "Podlać kwiaty", "", false, Priority.P4, today, recurrence = Recurrence.WEEKLY),
        Task(4, "Kupić mleko", "", false, Priority.P4, null, labelIds = listOf(2)),
        Task(5, "Przygotować prezentację", "", false, Priority.P3, today - 1)
    )

    @Test
    fun taskList() {
        val groups = listOf(SectionGroup(null, null, tasks.map { TaskNode(it, emptyList()) }))
        paparazzi.snapshot {
            TodoistoTheme {
                GlassBackground {
                    TaskListScreen(
                        uiState = TodoUiState(AppView.Today, "Dzisiaj", groups, isEmpty = false, todayCount = 3, inboxCount = 1),
                        projects = projects,
                        labels = labels,
                        onSelectView = {},
                        onToggle = {},
                        onTaskClick = {},
                        onQuickAdd = {},
                        onAddProject = { _, _ -> },
                        onDeleteProject = {},
                        onAddLabel = { _, _ -> },
                        onAddSection = { _, _ -> },
                        onClearCompleted = {}
                    )
                }
            }
        }
    }

    @Test
    fun editTask() {
        paparazzi.snapshot {
            TodoistoTheme {
                GlassBackground {
                    AddEditTaskScreen(
                        existing = Task(
                            id = 1, title = "Zadzwonić do Beaty", notes = "Ustalić budżet",
                            priority = Priority.P1, dueDate = today, deadline = today + 3,
                            recurrence = Recurrence.WEEKLY, projectId = 1, labelIds = listOf(1)
                        ),
                        projects = projects,
                        labels = labels,
                        subtasks = listOf(Task(10, "Przygotować pytania", parentId = 1)),
                        onSave = {},
                        onAddSubtask = {},
                        onToggleSubtask = {},
                        onDelete = {},
                        onClose = {}
                    )
                }
            }
        }
    }

    @Test
    fun newTask() {
        paparazzi.snapshot {
            TodoistoTheme {
                GlassBackground {
                    AddEditTaskScreen(
                        existing = null,
                        projects = projects,
                        labels = labels,
                        subtasks = emptyList(),
                        onSave = {},
                        onAddSubtask = {},
                        onToggleSubtask = {},
                        onDelete = null,
                        onClose = {}
                    )
                }
            }
        }
    }
}
