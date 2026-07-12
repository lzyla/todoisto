package pl.media30.todoisto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
import pl.media30.todoisto.data.Activity
import pl.media30.todoisto.data.EffortType
import pl.media30.todoisto.data.EnergyCost
import pl.media30.todoisto.data.FreeSlot
import pl.media30.todoisto.data.Place
import pl.media30.todoisto.data.Suggestion
import pl.media30.todoisto.ui.FreeTimeState
import pl.media30.todoisto.ui.screens.ActivityFormSheet
import pl.media30.todoisto.ui.screens.ActivityPoolSheet
import pl.media30.todoisto.ui.screens.FreeTimePanel
import pl.media30.todoisto.ui.screens.TaskDetailSheet
import pl.media30.todoisto.ui.screens.TaskListScreen
import pl.media30.todoisto.ui.theme.GlassBackground
import pl.media30.todoisto.ui.theme.GlassSurface
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
        Project(1, "Praca", 0xFF4D6BFF, isFavorite = true),
        Project(2, "Dom", 0xFF2DD4BF),
        Project(3, "Zdrowie", 0xFFFB7185)
    )
    private val labels = listOf(
        Label(1, "pilne", 0xFFC24DFF),
        Label(2, "zakupy", 0xFFEB8909)
    )

    private val tasks = listOf(
        Task(1, "Przygotować raport miesięczny", "Wysłać do Anny przed spotkaniem zarządu", false, Priority.P1, today, dueTimeMinutes = 600, projectId = 1, labelIds = listOf(1)),
        Task(2, "Stand-up zespołu", "", false, Priority.P4, today, dueTimeMinutes = 570, recurrence = Recurrence.DAILY, projectId = 1),
        Task(3, "Nadać paczkę na poczcie", "", false, Priority.P4, today, projectId = 2),
        Task(4, "Przegląd pull requestów", "", false, Priority.P3, today, dueTimeMinutes = 14 * 60, durationMinutes = 45, projectId = 1),
        Task(5, "Kupić prezent dla Zosi", "", false, Priority.P2, today, dueTimeMinutes = 15 * 60, deadline = today + 3, projectId = 2, labelIds = listOf(2), attachments = listOf("https://images.app/tort-jednorozec.jpg")),
        Task(6, "Trening — siłownia", "", false, Priority.P4, today, dueTimeMinutes = 18 * 60 + 30, recurrence = Recurrence.WEEKLY, projectId = 3)
    )

    private val routines = listOf(
        Task(7, "Nauka japońskiego — 15 min", "", false, Priority.P4, today, recurrence = Recurrence.DAILY),
        Task(8, "Podlać kwiaty", "", false, Priority.P4, today, recurrence = Recurrence.DAILY)
    )

    private fun uiState() = TodoUiState(
        view = AppView.Today,
        title = "Dzisiaj",
        groups = listOf(SectionGroup(null, null, tasks.map { TaskNode(it, if (it.id == 1L) listOf(Task(20, "Zebrać dane", isCompleted = true, parentId = 1), Task(21, "Wykres sprzedaży", parentId = 1)) else emptyList()) })),
        isEmpty = false,
        todayCount = 6,
        inboxCount = 2,
        doneToday = 2,
        doneWeek = 14,
        routines = routines,
        routinesDone = 1
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
            onQuickAdd = { _, _ -> },
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
            onToggleTheme = {},
            weekTasks = tasks
        )
    }

    @Test
    fun taskListLight() {
        GlassTheme.dark = false
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.NOON
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen() } } }
    }

    @Test
    fun bgMorning() {
        GlassTheme.dark = false
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.MORNING
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen() } } }
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.NOON
    }

    @Test
    fun bgEvening() {
        GlassTheme.dark = false
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.EVENING
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen() } } }
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.NOON
    }

    @Test
    fun photoMorning() {
        GlassTheme.dark = false
        GlassTheme.photo = true
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.MORNING
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen() } } }
        GlassTheme.photo = false
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.NOON
    }

    @Test
    fun photoEvening() {
        GlassTheme.dark = false
        GlassTheme.photo = true
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.EVENING
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen() } } }
        GlassTheme.photo = false
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.NOON
    }

    @Test
    fun taskListRoutinesOpen() {
        GlassTheme.dark = false
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen(routinesExpanded = true) } } }
    }

    @Test
    fun taskListDark() {
        GlassTheme.dark = true
        paparazzi.snapshot { TodoistoTheme { GlassBackground { listScreen() } } }
        GlassTheme.dark = false
    }

    @Test
    fun taskDetailSheet() {
        GlassTheme.dark = false
        paparazzi.snapshot {
            TodoistoTheme {
                GlassBackground {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .background(GlassSurface, androidx.compose.foundation.shape.RoundedCornerShape(34.dp))
                            .padding(top = 14.dp)
                    ) {
                        TaskDetailSheet(
                            task = Task(
                                id = 5, title = "Kupić prezent dla Zosi", notes = "Inspiracja z internetu",
                                priority = Priority.P2, dueDate = today, deadline = today + 3,
                                durationMinutes = 30, projectId = 2, labelIds = listOf(2),
                                attachments = listOf("https://allegro.pl/oferta/tort-jednorozec")
                            ),
                            projects = projects,
                            labels = labels,
                            subtasks = listOf(Task(10, "Zapytać Kasię o rozmiar", parentId = 5, isCompleted = true), Task(11, "Zamówić do czwartku", parentId = 5)),
                            onPatch = {},
                            onToggleSubtask = {},
                            onAddSubtask = {},
                            onDuplicate = {},
                            onDelete = {},
                            onClose = {}
                        )
                    }
                }
            }
        }
    }

    private val poolActivities = listOf(
        Activity(1, "Spacer w parku", EffortType.PHYSICAL, durationMinutes = 30, place = Place.OUTSIDE, energyCost = EnergyCost.LOW, frequencyTarget = 4),
        Activity(2, "Czytanie książki", EffortType.RELAX, durationMinutes = 45, place = Place.HOME, energyCost = EnergyCost.LOW),
        Activity(3, "Nauka hiszpańskiego", EffortType.MENTAL, durationMinutes = 20, place = Place.HOME, energyCost = EnergyCost.MED, frequencyTarget = 3),
        Activity(4, "Trening siłowy", EffortType.PHYSICAL, durationMinutes = 60, place = Place.OUTSIDE, windowStartMin = 17 * 60, windowEndMin = 21 * 60, energyCost = EnergyCost.HIGH)
    )

    @Composable
    private fun sheet(content: @Composable () -> Unit) {
        GlassBackground {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .background(GlassSurface, androidx.compose.foundation.shape.RoundedCornerShape(34.dp))
                    .padding(top = 14.dp)
            ) { content() }
        }
    }

    @Test
    fun freeTimePanel() {
        GlassTheme.dark = false
        GlassTheme.phase = pl.media30.todoisto.ui.theme.DayPhase.NOON
        val suggestions = listOf(
            Suggestion(poolActivities[0], FreeSlot(15 * 60, 16 * 60 + 30), 15 * 60)
        )
        paparazzi.snapshot {
            TodoistoTheme {
                sheet {
                    FreeTimePanel(
                        state = FreeTimeState(freeMinutes = 90, suggestions = suggestions, poolEmpty = false, noWindows = false),
                        onAccept = {}, onReroll = {}, onAddFirst = {}
                    )
                }
            }
        }
    }

    @Test
    fun activityPoolSheet() {
        GlassTheme.dark = false
        paparazzi.snapshot {
            TodoistoTheme {
                sheet { ActivityPoolSheet(activities = poolActivities, onAdd = {}, onEdit = {}) }
            }
        }
    }

    @Test
    fun activityFormSheet() {
        GlassTheme.dark = false
        paparazzi.snapshot {
            TodoistoTheme {
                sheet {
                    ActivityFormSheet(existing = poolActivities[0], onSave = {}, onDelete = {}, onClose = {})
                }
            }
        }
    }

    @Test
    fun freeTimePanelDark() {
        GlassTheme.dark = true
        paparazzi.snapshot {
            TodoistoTheme {
                sheet { ActivityPoolSheet(activities = poolActivities, onAdd = {}, onEdit = {}) }
            }
        }
        GlassTheme.dark = false
    }
}
