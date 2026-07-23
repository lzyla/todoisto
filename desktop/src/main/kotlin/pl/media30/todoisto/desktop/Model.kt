package pl.media30.todoisto.desktop

import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Recurrence
import java.time.LocalDate

/**
 * Lekki model zadania dla desktopu — bez Room (ta warstwa jest androidowa).
 * Reużywamy natomiast czyste typy domenowe: Priority, Recurrence, QuickAddParser.
 */
data class Task(
    val id: Long,
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.P4,
    val dueDate: Long? = null,          // epoch-day
    val dueTimeMinutes: Int? = null,    // minuty od północy
    val durationMinutes: Int? = null,
    val recurrence: Recurrence? = null,
    val projectName: String? = null,
    val labelNames: List<String> = emptyList()
)

/** Prosta pamięciowa „baza" zadań (desktop). Faza 2: SQLite/DataStore. */
class TaskRepository {
    private var nextId = 1L
    private val _tasks = mutableListOf<Task>()

    val tasks: List<Task> get() = _tasks.toList()

    fun add(task: Task): Task {
        val withId = task.copy(id = nextId++)
        _tasks.add(withId)
        return withId
    }

    fun toggle(id: Long) {
        val i = _tasks.indexOfFirst { it.id == id }
        if (i >= 0) _tasks[i] = _tasks[i].copy(isCompleted = !_tasks[i].isCompleted)
    }

    fun delete(id: Long) { _tasks.removeAll { it.id == id } }
}

/** Dane demo — jak w wersji androidowej, żeby ekran od razu żył. */
fun seedDemo(repo: TaskRepository) {
    val today = LocalDate.now().toEpochDay()
    listOf(
        Task(0, "Przygotować raport miesięczny", "Wysłać do Anny przed spotkaniem", priority = Priority.P1, dueDate = today, dueTimeMinutes = 600, durationMinutes = 90, projectName = "Praca", labelNames = listOf("pilne")),
        Task(0, "Stand-up zespołu", priority = Priority.P4, dueDate = today, dueTimeMinutes = 570, recurrence = Recurrence.DAILY, projectName = "Praca"),
        Task(0, "Nadać paczkę na poczcie", priority = Priority.P4, dueDate = today, projectName = "Dom"),
        Task(0, "Przegląd pull requestów", priority = Priority.P3, dueDate = today, dueTimeMinutes = 14 * 60, durationMinutes = 45, projectName = "Praca"),
        Task(0, "Kupić prezent dla Zosi", priority = Priority.P2, dueDate = today, dueTimeMinutes = 15 * 60, projectName = "Dom", labelNames = listOf("zakupy")),
        Task(0, "Trening — siłownia", priority = Priority.P4, dueDate = today, dueTimeMinutes = 18 * 60 + 30, recurrence = Recurrence.WEEKLY, projectName = "Zdrowie")
    ).forEach { repo.add(it) }
}
