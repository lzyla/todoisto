package pl.media30.todoisto.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val sectionDao: SectionDao,
    private val labelDao: LabelDao
) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allProjects: Flow<List<Project>> = projectDao.getAll()
    val allSections: Flow<List<Section>> = sectionDao.getAll()
    val allLabels: Flow<List<Label>> = labelDao.getAll()

    // --- tasks ---
    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)
    suspend fun getSubtasks(parentId: Long): List<Task> = taskDao.getSubtasks(parentId)
    suspend fun insert(task: Task): Long = taskDao.insert(task)
    suspend fun update(task: Task) = taskDao.update(task)
    suspend fun deleteWithSubtasks(id: Long) = taskDao.deleteWithSubtasks(id)
    suspend fun deleteCompleted() = taskDao.deleteCompleted()

    /**
     * Toggle completion. Recurring tasks are never "finished": instead their due
     * date advances to the next occurrence and they stay active.
     */
    suspend fun toggleCompleted(task: Task) {
        val rec = task.recurrence
        if (rec != null && !task.isCompleted && task.dueDate != null) {
            taskDao.update(task.copy(dueDate = rec.next(task.dueDate)))
        } else {
            taskDao.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    // --- projects ---
    suspend fun insertProject(project: Project): Long = projectDao.insert(project)
    suspend fun updateProject(project: Project) = projectDao.update(project)
    suspend fun deleteProject(id: Long) {
        taskDao.deleteByProject(id)
        sectionDao.deleteByProject(id)
        projectDao.deleteById(id)
    }

    /** Resolve a project by name, creating it if it does not exist yet. */
    suspend fun ensureProject(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return 0
        return projectDao.findByName(trimmed)?.id ?: projectDao.insert(Project(name = trimmed))
    }

    // --- sections ---
    suspend fun insertSection(section: Section): Long = sectionDao.insert(section)
    suspend fun deleteSection(id: Long) = sectionDao.deleteById(id)

    // --- labels ---
    suspend fun insertLabel(label: Label): Long = labelDao.insert(label)
    suspend fun deleteLabel(id: Long) = labelDao.deleteById(id)

    suspend fun ensureLabel(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return 0
        return labelDao.findByName(trimmed)?.id ?: labelDao.insert(Label(name = trimmed))
    }
}
