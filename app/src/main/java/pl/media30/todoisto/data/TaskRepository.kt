package pl.media30.todoisto.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {

    val allTasks: Flow<List<Task>> = dao.getAllTasks()

    suspend fun getTaskById(id: Long): Task? = dao.getTaskById(id)

    suspend fun insert(task: Task): Long = dao.insert(task)

    suspend fun update(task: Task) = dao.update(task)

    suspend fun delete(task: Task) = dao.delete(task)

    suspend fun toggleCompleted(task: Task) =
        dao.update(task.copy(isCompleted = !task.isCompleted))

    suspend fun deleteCompleted() = dao.deleteCompleted()
}
