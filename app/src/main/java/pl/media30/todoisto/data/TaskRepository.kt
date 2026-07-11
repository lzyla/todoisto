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
     * date advances to the next occurrence and they stay active. [completedAt]
     * feeds the daily/weekly goal counters.
     */
    suspend fun toggleCompleted(task: Task) {
        val rec = task.recurrence
        if (rec != null && !task.isCompleted && task.dueDate != null) {
            taskDao.update(task.copy(dueDate = rec.next(task.dueDate), completedAt = System.currentTimeMillis()))
        } else if (!task.isCompleted) {
            taskDao.update(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        } else {
            taskDao.update(task.copy(isCompleted = false, completedAt = null))
        }
    }

    /** Duplicate a task together with its subtasks. */
    suspend fun duplicateTask(id: Long) {
        val original = taskDao.getTaskById(id) ?: return
        val copyId = taskDao.insert(
            original.copy(id = 0, title = original.title + " (kopia)", isCompleted = false, completedAt = null)
        )
        taskDao.getSubtasks(id).forEach { sub ->
            taskDao.insert(sub.copy(id = 0, parentId = copyId, isCompleted = false, completedAt = null))
        }
    }

    /** Duplicate a project with its sections and active tasks. */
    suspend fun duplicateProject(id: Long, sections: List<Section>, tasks: List<Task>) {
        val original = allProjectsSnapshot(id) ?: return
        val newProjectId = projectDao.insert(original.copy(id = 0, name = original.name + " (kopia)", isArchived = false))
        val sectionMap = mutableMapOf<Long, Long>()
        sections.filter { it.projectId == id }.forEach { sec ->
            sectionMap[sec.id] = sectionDao.insert(sec.copy(id = 0, projectId = newProjectId))
        }
        val taskMap = mutableMapOf<Long, Long>()
        val projectTasks = tasks.filter { it.projectId == id && !it.isCompleted }
        projectTasks.filter { it.parentId == null }.forEach { t ->
            taskMap[t.id] = taskDao.insert(
                t.copy(id = 0, projectId = newProjectId, sectionId = t.sectionId?.let { sectionMap[it] }, completedAt = null)
            )
        }
        projectTasks.filter { it.parentId != null }.forEach { t ->
            val newParent = taskMap[t.parentId] ?: return@forEach
            taskDao.insert(
                t.copy(id = 0, projectId = newProjectId, parentId = newParent, sectionId = null, completedAt = null)
            )
        }
    }

    private suspend fun allProjectsSnapshot(id: Long): Project? = projectDao.findById(id)

    suspend fun setProjectArchived(id: Long, archived: Boolean) {
        projectDao.findById(id)?.let { projectDao.update(it.copy(isArchived = archived)) }
    }

    suspend fun toggleProjectFavorite(id: Long) {
        projectDao.findById(id)?.let { projectDao.update(it.copy(isFavorite = !it.isFavorite)) }
    }

    suspend fun toggleLabelFavorite(id: Long) {
        labelDao.findById(id)?.let { labelDao.update(it.copy(isFavorite = !it.isFavorite)) }
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
