package pl.media30.todoisto.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Section
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.data.TaskRepository
import java.time.LocalDate

/** Which collection of tasks the main screen is showing. */
sealed class AppView {
    data object Today : AppView()
    data object Upcoming : AppView()
    data object Inbox : AppView()
    data object Completed : AppView()
    data class ProjectView(val id: Long) : AppView()
    data class LabelView(val id: Long) : AppView()
}

/** A top-level task together with its subtasks. */
data class TaskNode(
    val task: Task,
    val subtasks: List<Task> = emptyList()
)

/** A section header with the task nodes under it (id null = "no section"). */
data class SectionGroup(
    val sectionId: Long?,
    val name: String?,
    val nodes: List<TaskNode>
)

data class TodoUiState(
    val view: AppView = AppView.Today,
    val title: String = "Dzisiaj",
    val groups: List<SectionGroup> = emptyList(),
    val isEmpty: Boolean = true,
    val todayCount: Int = 0,
    val inboxCount: Int = 0
)

class TodoViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _view = MutableStateFlow<AppView>(AppView.Today)

    val allTasks: StateFlow<List<Task>> =
        repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projects: StateFlow<List<Project>> =
        repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val labels: StateFlow<List<Label>> =
        repository.allLabels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<TodoUiState> =
        combine(
            repository.allTasks,
            repository.allSections,
            _view,
            repository.allProjects,
            repository.allLabels
        ) { tasks, sections, view, projects, labels ->
            buildState(tasks, sections, view, projects, labels)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())

    private fun buildState(
        tasks: List<Task>,
        sections: List<Section>,
        view: AppView,
        projects: List<Project>,
        labels: List<Label>
    ): TodoUiState {
        val today = LocalDate.now().toEpochDay()
        val topLevel = tasks.filter { it.parentId == null }

        val matching = topLevel.filter { t ->
            when (view) {
                AppView.Today -> !t.isCompleted && t.dueDate != null && t.dueDate <= today
                AppView.Upcoming -> !t.isCompleted && t.dueDate != null && t.dueDate > today
                AppView.Inbox -> !t.isCompleted && t.projectId == null
                AppView.Completed -> t.isCompleted
                is AppView.ProjectView -> !t.isCompleted && t.projectId == view.id
                is AppView.LabelView -> !t.isCompleted && t.labelIds.contains(view.id)
            }
        }

        fun nodeOf(t: Task) = TaskNode(t, tasks.filter { it.parentId == t.id })

        val groups: List<SectionGroup> = if (view is AppView.ProjectView) {
            val projectSections = sections.filter { it.projectId == view.id }.sortedBy { it.position }
            val bySection = matching.groupBy { it.sectionId }
            buildList {
                val noSection = bySection[null].orEmpty().map(::nodeOf)
                if (noSection.isNotEmpty()) add(SectionGroup(null, null, noSection))
                // Keep every defined section visible, even when empty, so structure shows.
                projectSections.forEach { sec ->
                    add(SectionGroup(sec.id, sec.name, bySection[sec.id].orEmpty().map(::nodeOf)))
                }
            }
        } else {
            listOf(SectionGroup(null, null, matching.map(::nodeOf)))
        }

        val title = when (view) {
            AppView.Today -> "Dzisiaj"
            AppView.Upcoming -> "Nadchodzące"
            AppView.Inbox -> "Skrzynka"
            AppView.Completed -> "Ukończone"
            is AppView.ProjectView -> projects.firstOrNull { it.id == view.id }?.name ?: "Projekt"
            is AppView.LabelView -> "@" + (labels.firstOrNull { it.id == view.id }?.name ?: "etykieta")
        }

        return TodoUiState(
            view = view,
            title = title,
            groups = groups,
            isEmpty = matching.isEmpty(),
            todayCount = topLevel.count { !it.isCompleted && it.dueDate != null && it.dueDate <= today },
            inboxCount = topLevel.count { !it.isCompleted && it.projectId == null }
        )
    }

    fun setView(view: AppView) { _view.value = view }

    // --- Quick Add (natural language) ---
    fun quickAdd(raw: String) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            val parsed = QuickAddParser().parse(raw)
            val title = parsed.title.ifBlank { raw.trim() }

            val currentView = _view.value
            val projectId = when {
                parsed.projectName != null -> repository.ensureProject(parsed.projectName!!)
                currentView is AppView.ProjectView -> currentView.id
                else -> null
            }
            val labelIds = parsed.labelNames.map { repository.ensureLabel(it) }.filter { it != 0L }

            val reminderAt = if (parsed.dueDate != null && parsed.dueTimeMinutes != null) {
                (parsed.dueDate!! * 24L * 60 * 60 * 1000) + parsed.dueTimeMinutes!! * 60L * 1000
            } else null

            repository.insert(
                Task(
                    title = title,
                    priority = parsed.priority,
                    dueDate = parsed.dueDate,
                    dueTimeMinutes = parsed.dueTimeMinutes,
                    deadline = parsed.deadline,
                    recurrence = parsed.recurrence,
                    reminderAt = reminderAt,
                    projectId = projectId,
                    labelIds = labelIds,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addTask(task: Task) {
        if (task.title.isBlank()) return
        viewModelScope.launch { repository.insert(task.copy(createdAt = System.currentTimeMillis())) }
    }

    fun updateTask(task: Task) {
        if (task.title.isBlank()) return
        viewModelScope.launch { repository.update(task.copy(title = task.title.trim(), notes = task.notes.trim())) }
    }

    fun addSubtask(parentId: Long, projectId: Long?, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Task(title = title.trim(), parentId = parentId, projectId = projectId, createdAt = System.currentTimeMillis())
            )
        }
    }

    fun toggleCompleted(task: Task) = viewModelScope.launch { repository.toggleCompleted(task) }
    fun deleteTask(task: Task) = viewModelScope.launch { repository.deleteWithSubtasks(task.id) }
    fun deleteCompleted() = viewModelScope.launch { repository.deleteCompleted() }

    // --- projects / sections / labels ---
    fun addProject(name: String, colorArgb: Long) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertProject(Project(name = name.trim(), colorArgb = colorArgb)) }
    }

    fun deleteProject(id: Long) = viewModelScope.launch {
        if (_view.value == AppView.ProjectView(id)) _view.value = AppView.Today
        repository.deleteProject(id)
    }

    fun addSection(projectId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertSection(Section(projectId = projectId, name = name.trim())) }
    }

    fun addLabel(name: String, colorArgb: Long) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertLabel(Label(name = name.trim(), colorArgb = colorArgb)) }
    }

    fun deleteLabel(id: Long) = viewModelScope.launch {
        if (_view.value == AppView.LabelView(id)) _view.value = AppView.Today
        repository.deleteLabel(id)
    }

    suspend fun getTask(id: Long): Task? = repository.getTaskById(id)

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
                return TodoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
