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
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.data.TaskRepository
import java.time.LocalDate

enum class TaskFilter(val title: String) {
    INBOX("Wszystkie"),
    TODAY("Dzisiaj"),
    UPCOMING("Nadchodzące"),
    COMPLETED("Ukończone")
}

data class TodoUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.INBOX,
    val activeCount: Int = 0
)

class TodoViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.INBOX)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    val uiState: StateFlow<TodoUiState> =
        combine(repository.allTasks, _filter) { tasks, filter ->
            val today = LocalDate.now().toEpochDay()
            val filtered = when (filter) {
                TaskFilter.INBOX -> tasks.filter { !it.isCompleted }
                TaskFilter.TODAY -> tasks.filter {
                    !it.isCompleted && it.dueDate != null && it.dueDate <= today
                }
                TaskFilter.UPCOMING -> tasks.filter {
                    !it.isCompleted && it.dueDate != null && it.dueDate > today
                }
                TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
            }
            TodoUiState(
                tasks = filtered,
                filter = filter,
                activeCount = tasks.count { !it.isCompleted }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodoUiState()
        )

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun addTask(title: String, notes: String, priority: Priority, dueDate: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insert(
                Task(
                    title = trimmed,
                    notes = notes.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateTask(task: Task) {
        if (task.title.isBlank()) return
        viewModelScope.launch {
            repository.update(task.copy(title = task.title.trim(), notes = task.notes.trim()))
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch { repository.toggleCompleted(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    fun deleteCompleted() {
        viewModelScope.launch { repository.deleteCompleted() }
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
