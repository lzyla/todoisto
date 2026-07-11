package pl.media30.todoisto

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.TaskFilter
import pl.media30.todoisto.ui.TodoUiState
import pl.media30.todoisto.ui.screens.AddEditTaskScreen
import pl.media30.todoisto.ui.screens.TaskListScreen
import pl.media30.todoisto.ui.theme.TodoistoTheme
import java.time.LocalDate

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private val today = LocalDate.now().toEpochDay()

    private val sampleTasks = listOf(
        Task(1, "Kupić prezent urodzinowy dla mamy", "Coś z jej listy życzeń", false, Priority.P1, today),
        Task(2, "Zrobić przegląd samochodu", "Serwis na ul. Kwiatowej", false, Priority.P2, today + 2),
        Task(3, "Dokończyć raport kwartalny", "", false, Priority.P3, today + 1),
        Task(4, "Zadzwonić do dentysty", "", false, Priority.P4, null),
        Task(5, "Wynieść śmieci", "", false, Priority.P4, today - 1),
        Task(6, "Odpowiedzieć na maile", "", true, Priority.P4, null)
    )

    @Test
    fun taskList() {
        paparazzi.snapshot {
            TodoistoTheme {
                TaskListScreen(
                    uiState = TodoUiState(
                        tasks = sampleTasks.filter { !it.isCompleted },
                        filter = TaskFilter.INBOX,
                        activeCount = 5
                    ),
                    onSelectFilter = {},
                    onToggle = {},
                    onTaskClick = {},
                    onAddClick = {},
                    onClearCompleted = {}
                )
            }
        }
    }

    @Test
    fun taskListDark() {
        paparazzi.snapshot {
            TodoistoTheme(darkTheme = true) {
                TaskListScreen(
                    uiState = TodoUiState(
                        tasks = sampleTasks.filter { !it.isCompleted },
                        filter = TaskFilter.TODAY,
                        activeCount = 5
                    ),
                    onSelectFilter = {},
                    onToggle = {},
                    onTaskClick = {},
                    onAddClick = {},
                    onClearCompleted = {}
                )
            }
        }
    }

    @Test
    fun addTask() {
        paparazzi.snapshot {
            TodoistoTheme {
                AddEditTaskScreen(
                    existing = Task(
                        id = 1,
                        title = "Kupić prezent urodzinowy dla mamy",
                        notes = "Coś z jej listy życzeń",
                        priority = Priority.P1,
                        dueDate = today
                    ),
                    onSave = { _, _, _, _ -> },
                    onDelete = {},
                    onClose = {}
                )
            }
        }
    }
}
