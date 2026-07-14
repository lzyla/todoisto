package pl.media30.todoisto.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val sectionDao: SectionDao,
    private val labelDao: LabelDao,
    private val activityDao: ActivityDao,
    private val areaDao: AreaDao
) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allProjects: Flow<List<Project>> = projectDao.getAll()
    val allSections: Flow<List<Section>> = sectionDao.getAll()
    val allLabels: Flow<List<Label>> = labelDao.getAll()
    val allActivities: Flow<List<Activity>> = activityDao.getAll()
    val allAreas: Flow<List<Area>> = areaDao.getAll()

    // --- kopia w chmurze (eksport/import JSON) ---
    suspend fun exportBackupJson(): String =
        CloudBackup.toJson(taskDao.getAllTasks().first(), projectDao.getAll().first(), labelDao.getAll().first())

    /** Wczytuje kopię: wstawia z REPLACE (nadpisuje po id). */
    suspend fun importBackupJson(json: String): Int {
        val parsed = CloudBackup.fromJson(json)
        parsed.labels.forEach { labelDao.insert(it) }
        parsed.projects.forEach { projectDao.insert(it) }
        parsed.tasks.forEach { taskDao.insert(it) }
        return parsed.tasks.size
    }

    // --- areas (obszary) ---
    suspend fun insertArea(area: Area): Long = areaDao.insert(area)
    suspend fun updateArea(area: Area) = areaDao.update(area)
    suspend fun deleteArea(id: Long) = areaDao.deleteById(id)

    // --- tasks ---
    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)
    suspend fun getSubtasks(parentId: Long): List<Task> = taskDao.getSubtasks(parentId)
    suspend fun insert(task: Task): Long = taskDao.insert(task)
    suspend fun update(task: Task) = taskDao.update(task)
    /** Zapisuje ręczną kolejność: pozycja = indeks na liście. */
    suspend fun reorderTasks(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> taskDao.updatePosition(id, index) }
    }
    suspend fun deleteWithSubtasks(id: Long) = taskDao.deleteWithSubtasks(id)
    suspend fun deleteCompleted() = taskDao.deleteCompleted()

    /**
     * Toggle completion. Recurring tasks are never "finished": instead their due
     * date advances to the next occurrence and they stay active. [completedAt]
     * feeds the daily/weekly goal counters.
     */
    suspend fun toggleCompleted(task: Task) {
        val rec = task.recurrence
        val now = System.currentTimeMillis()
        if (rec != null && !task.isCompleted && task.dueDate != null) {
            taskDao.update(task.copy(dueDate = rec.next(task.dueDate), completedAt = now))
        } else if (!task.isCompleted) {
            taskDao.update(task.copy(isCompleted = true, completedAt = now))
        } else {
            taskDao.update(task.copy(isCompleted = false, completedAt = null))
        }
        // Domknięcie pętli częstotliwości: ślad "activity:<id>" w notatce.
        if (!task.isCompleted) activityIdFromNotes(task.notes)?.let { aid ->
            activityDao.findById(aid)?.let { activityDao.update(it.copy(lastCompletedAt = now)) }
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

    // --- Pula aktywności ---
    suspend fun insertActivity(activity: Activity): Long =
        activityDao.insert(activity.copy(createdAt = if (activity.createdAt == 0L) System.currentTimeMillis() else activity.createdAt))
    suspend fun updateActivity(activity: Activity) = activityDao.update(activity)
    suspend fun deleteActivity(id: Long) = activityDao.deleteById(id)

    private suspend fun ensureActivityLabel(): Long =
        labelDao.findByName(ACTIVITY_LABEL)?.id
            ?: labelDao.insert(Label(name = ACTIVITY_LABEL, colorArgb = 0xFF2DD4BF))

    /** Zaakceptowana propozycja → zwykły Task (z godziną, etykietą i śladem pochodzenia). */
    suspend fun materializeActivity(activity: Activity, startMin: Int, todayEpochDay: Long): Long {
        val labelId = ensureActivityLabel()
        val id = taskDao.insert(
            Task(
                title = activity.name,
                notes = "activity:${activity.id}",
                priority = Priority.P4,
                dueDate = todayEpochDay,
                dueTimeMinutes = startMin,
                durationMinutes = activity.durationMinutes,
                labelIds = listOf(labelId),
                createdAt = System.currentTimeMillis()
            )
        )
        activityDao.update(activity.copy(lastScheduledAt = System.currentTimeMillis()))
        return id
    }

    /**
     * DayContext z istniejących zadań. [projects] i [tasks] podawane ze snapshotów,
     * żeby uniknąć dodatkowych zapytań. [now] = LocalDateTime „teraz".
     */
    fun buildDayContext(
        tasks: List<Task>,
        projects: List<Project>,
        nowMinutes: Int,
        dayOfWeek: Int,
        nowMillis: Long,
        todayStartMillis: Long,
        weekStartMillis: Long
    ): DayContext {
        val projEffort = projects.associate { it.id to (it.workEffortType ?: EffortType.MENTAL) }
        val doneToday = tasks.filter { it.isCompleted && (it.completedAt ?: 0L) >= todayStartMillis }
        var mentalMin = 0; var physMin = 0; var totalMin = 0
        var budget = 100
        doneToday.forEach { t ->
            val dur = t.durationMinutes ?: 25
            totalMin += dur
            when (t.projectId?.let { projEffort[it] } ?: EffortType.MENTAL) {
                EffortType.PHYSICAL -> physMin += dur
                else -> mentalMin += dur
            }
            budget -= 6 + dur / 10 + when (t.priority) {
                Priority.P1 -> 14; Priority.P2 -> 9; Priority.P3 -> 4; Priority.P4 -> 2
            }
        }
        val denom = (mentalMin + physMin).coerceAtLeast(1).toFloat()

        val weekCount = mutableMapOf<Long, Int>()
        tasks.filter { it.isCompleted && (it.completedAt ?: 0L) >= weekStartMillis }.forEach { t ->
            activityIdFromNotes(t.notes)?.let { aid -> weekCount[aid] = (weekCount[aid] ?: 0) + 1 }
        }

        return DayContext(
            completedCount = doneToday.size,
            totalWorkMinutes = totalMin,
            mentalShare = mentalMin / denom,
            physicalShare = physMin / denom,
            energyBudget = budget.coerceIn(0, 100),
            nowMinutes = nowMinutes,
            dayOfWeek = dayOfWeek,
            weekCountByActivity = weekCount,
            nowMillis = nowMillis
        )
    }

    /**
     * Wolne okna dziś: luki między zadaniami z godziną w zakresie [dayStart,dayEnd],
     * tylko od [fromMin] w przód, o długości ≥ [minLen].
     */
    fun freeWindows(
        tasks: List<Task>,
        todayEpochDay: Long,
        fromMin: Int,
        dayStart: Int = 6 * 60,
        dayEnd: Int = 23 * 60,
        minLen: Int = 15
    ): List<FreeSlot> {
        val busy = tasks
            .filter { !it.isCompleted && it.dueDate == todayEpochDay && it.dueTimeMinutes != null }
            .map { it.dueTimeMinutes!! to (it.dueTimeMinutes!! + (it.durationMinutes ?: 30)) }
            .sortedBy { it.first }
        val slots = mutableListOf<FreeSlot>()
        var cursor = maxOf(dayStart, fromMin)
        for ((s, e) in busy) {
            if (s > cursor) slots += FreeSlot(cursor, minOf(s, dayEnd))
            cursor = maxOf(cursor, e)
            if (cursor >= dayEnd) break
        }
        if (cursor < dayEnd) slots += FreeSlot(cursor, dayEnd)
        return slots.filter { it.length >= minLen }
    }

    companion object {
        const val ACTIVITY_LABEL = "aktywność"
        fun activityIdFromNotes(notes: String): Long? =
            if (notes.startsWith("activity:")) notes.removePrefix("activity:").toLongOrNull() else null
    }
}
