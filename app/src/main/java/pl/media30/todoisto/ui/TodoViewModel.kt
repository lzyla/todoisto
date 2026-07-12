package pl.media30.todoisto.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.media30.todoisto.data.Label
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.Project
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.data.Section
import pl.media30.todoisto.data.SettingsStore
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.data.TaskRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Which collection of tasks the main screen is showing. */
sealed class AppView {
    data object Today : AppView()
    data object Upcoming : AppView()
    data object Inbox : AppView()
    data object Completed : AppView()
    data class ProjectView(val id: Long) : AppView()
    data class LabelView(val id: Long) : AppView()
}

/** Manual selections from the Quick Add sheet; they win over parsed tokens. */
data class QuickAddOverrides(
    val dueDate: Long? = null,
    val priority: Priority? = null,
    val projectId: Long? = null,
    val recurrence: Recurrence? = null
)

/** Stan panelu „Czas wolny". */
data class FreeTimeState(
    val freeMinutes: Int,
    val suggestions: List<pl.media30.todoisto.data.Suggestion>,
    val poolEmpty: Boolean,
    val noWindows: Boolean
)

/** Stan zapytania do AI (przycisk „Zapytaj AI"). */
data class AiAskState(
    val loading: Boolean = false,
    val answer: String? = null,
    val error: String? = null,
    val needsKey: Boolean = false
)

enum class SortMode(val label: String) {
    SMART("Sprytne"),
    PRIORITY("Priorytet"),
    DATE("Data"),
    ALPHA("Alfabetycznie"),
    NEWEST("Najnowsze")
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
    val inboxCount: Int = 0,
    val sortMode: SortMode = SortMode.SMART,
    val doneToday: Int = 0,
    val doneWeek: Int = 0,
    val goalDaily: Int = 5,
    val goalWeekly: Int = 25,
    val currentProject: Project? = null,
    val currentLabel: Label? = null,
    /** Habit-style tasks for the Today routines bar (recurring, P3/P4, no fixed time). */
    val routines: List<Task> = emptyList(),
    /** Routines already completed today (their due date has advanced). */
    val routinesDone: Int = 0,
    /** Szacowany czas potrzebny na dokończenie dzisiejszych zadań (suma durationMinutes; brak = 20 min). */
    val estTodayMinutes: Int = 0
)

private data class Sources(
    val tasks: List<Task>,
    val sections: List<Section>,
    val projects: List<Project>,
    val labels: List<Label>
)

private data class Prefs(
    val sort: SortMode,
    val goalDaily: Int,
    val goalWeekly: Int
)

class TodoViewModel(
    private val repository: TaskRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _view = MutableStateFlow<AppView>(
        if (settings.startView.value == "upcoming") AppView.Upcoming else AppView.Today
    )
    private val _sort = MutableStateFlow(SortMode.SMART)

    val darkTheme: StateFlow<Boolean> = settings.darkTheme
    fun setDarkTheme(value: Boolean) = settings.setDarkTheme(value)

    // --- Ustawienia „Ogólne" ---
    val startView: StateFlow<String> = settings.startView
    fun setStartView(value: String) = settings.setStartView(value)
    val dateRecognition: StateFlow<Boolean> = settings.dateRecognition
    fun setDateRecognition(value: Boolean) = settings.setDateRecognition(value)
    val weekStartMonday: StateFlow<Boolean> = settings.weekStartMonday
    fun setWeekStartMonday(value: Boolean) = settings.setWeekStartMonday(value)
    val completionSound: StateFlow<Boolean> = settings.completionSound
    fun setCompletionSound(value: Boolean) = settings.setCompletionSound(value)
    val swipeRightCompletes: StateFlow<Boolean> = settings.swipeRightCompletes
    fun setSwipeRightCompletes(value: Boolean) = settings.setSwipeRightCompletes(value)

    val photoBackground: StateFlow<Boolean> = settings.photoBackground
    fun setPhotoBackground(value: Boolean) = settings.setPhotoBackground(value)

    // --- Integracja AI (OpenAI) ---
    val openAiKey: StateFlow<String> = settings.openAiKey
    fun setOpenAiKey(value: String) = settings.setOpenAiKey(value)

    private val _aiAsk = MutableStateFlow<AiAskState?>(null)
    val aiAsk: StateFlow<AiAskState?> = _aiAsk.asStateFlow()
    fun dismissAi() { _aiAsk.value = null }

    /** „Zapytaj AI" na żywo: woła OpenAI kluczem użytkownika i pokazuje odpowiedź w apce. */
    fun askAi(prompt: String) {
        val key = settings.openAiKey.value
        if (key.isBlank()) {
            _aiAsk.value = AiAskState(loading = false, needsKey = true)
            return
        }
        _aiAsk.value = AiAskState(loading = true)
        viewModelScope.launch {
            _aiAsk.value = try {
                AiAskState(loading = false, answer = pl.media30.todoisto.data.AiClient.ask(key, prompt))
            } catch (e: Exception) {
                AiAskState(loading = false, error = e.message ?: "Błąd połączenia")
            }
        }
    }

    /** Tekstowy plan dnia — do wysłania/wklejenia np. do Claude (share sheet). */
    fun buildDayPlanText(): String {
        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val open = allTasks.value.filter {
            it.parentId == null && !it.isCompleted && it.dueDate != null && it.dueDate!! <= todayEpoch
        }.sortedWith(compareBy({ it.dueTimeMinutes ?: 9999 }, { it.priority.ordinal }))
        val projById = projects.value.associateBy { it.id }
        val fmt = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.forLanguageTag("pl"))
        val sb = StringBuilder()
        sb.append("Plan dnia — ${today.format(fmt)}\n\n")
        if (open.isEmpty()) {
            sb.append("Brak zaplanowanych zadań na dziś.\n")
        } else {
            open.forEach { t ->
                val time = t.dueTimeMinutes?.let { "%d:%02d ".format(it / 60, it % 60) } ?: ""
                val dur = t.durationMinutes?.let { " (${it} min)" } ?: ""
                val proj = t.projectId?.let { projById[it]?.name }?.let { " #$it" } ?: ""
                val prio = if (t.priority.ordinal < 3) " P${t.priority.ordinal + 1}" else ""
                val overdue = if (t.dueDate!! < todayEpoch) " [zaległe]" else ""
                sb.append("• ${time}${t.title}${dur}${proj}${prio}${overdue}\n")
            }
        }
        val est = uiState.value.estTodayMinutes
        if (est > 0) sb.append("\nSzacowany czas: ${est / 60}h ${est % 60}min\n")
        sb.append("\n— wygenerowano w Todoisto")
        return sb.toString()
    }
    fun setGoals(daily: Int, weekly: Int) = settings.setGoals(daily, weekly)
    fun setSort(mode: SortMode) { _sort.value = mode }

    val allTasks: StateFlow<List<Task>> =
        repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val projects: StateFlow<List<Project>> =
        repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val labels: StateFlow<List<Label>> =
        repository.allLabels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activities: StateFlow<List<pl.media30.todoisto.data.Activity>> =
        repository.allActivities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val areas: StateFlow<List<pl.media30.todoisto.data.Area>> =
        repository.allAreas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Aktywny obszar (null = „Wszystko"). */
    val activeArea: StateFlow<Long?> =
        settings.activeArea.map { if (it < 0) null else it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setActiveArea(id: Long?) = settings.setActiveArea(id ?: -1L)
    fun addArea(name: String, color: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertArea(pl.media30.todoisto.data.Area(name = name.trim(), colorArgb = color, position = areas.value.size))
        }
    }

    private val _freeTime = MutableStateFlow<FreeTimeState?>(null)
    val freeTime: StateFlow<FreeTimeState?> = _freeTime.asStateFlow()
    private val dismissedActivityIds = mutableSetOf<Long>()

    /** Dzisiejsze otwarte zadania (bez rutyn) do rozbicia „Szacowanego czasu" — z filtrem obszaru. */
    val todayOpen: StateFlow<List<Task>> =
        combine(repository.allTasks, repository.allProjects, settings.activeArea) { tasks, projects, area ->
            val today = LocalDate.now().toEpochDay()
            val projArea = projects.associate { it.id to it.areaId }
            val archived = projects.filter { it.isArchived }.map { it.id }.toSet()
            tasks.filter { t ->
                t.parentId == null && !t.isCompleted && t.dueDate != null && t.dueDate <= today &&
                    !(t.projectId != null && archived.contains(t.projectId)) &&
                    !(t.recurrence != null && t.dueTimeMinutes == null && t.priority.ordinal >= 2) &&
                    (area < 0 || (t.projectId?.let { projArea[it] } ?: t.areaId) == area)
            }.sortedWith(compareBy({ it.dueTimeMinutes ?: 9999 }, { it.priority.ordinal }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val sources = combine(
        repository.allTasks, repository.allSections, repository.allProjects, repository.allLabels
    ) { tasks, sections, projects, labels -> Sources(tasks, sections, projects, labels) }

    private val prefsFlow = combine(_sort, settings.dailyGoal, settings.weeklyGoal) { s, d, w -> Prefs(s, d, w) }

    val uiState: StateFlow<TodoUiState> =
        combine(sources, _view, prefsFlow, settings.activeArea) { src, view, prefs, area ->
            buildState(src, view, prefs, area.takeIf { it >= 0 })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())

    private fun buildState(src: Sources, view: AppView, prefs: Prefs, activeArea: Long?): TodoUiState {
        val today = LocalDate.now().toEpochDay()
        val archivedIds = src.projects.filter { it.isArchived }.map { it.id }.toSet()
        val projAreaById = src.projects.associate { it.id to it.areaId }
        val topLevelAll = src.tasks.filter { it.parentId == null }
        // Filtr obszaru: zadanie z projektem dziedziczy obszar projektu; bez projektu ma własny areaId.
        val topLevel = if (activeArea == null) topLevelAll else topLevelAll.filter { t ->
            (t.projectId?.let { projAreaById[it] } ?: t.areaId) == activeArea
        }

        val inArchived = { t: Task -> t.projectId != null && archivedIds.contains(t.projectId) }

        // Routine = recurring habit: no fixed time, priority P3/P4. P1/P2 and
        // timed recurring tasks are scheduled events and stay in the main list.
        val isRoutine = { t: Task ->
            t.recurrence != null && t.dueTimeMinutes == null && t.priority.ordinal >= 2
        }

        val matching = topLevel.filter { t ->
            when (view) {
                AppView.Today -> !t.isCompleted && t.dueDate != null && t.dueDate <= today && !inArchived(t) && !isRoutine(t)
                AppView.Upcoming -> !t.isCompleted && t.dueDate != null && t.dueDate > today && !inArchived(t)
                AppView.Inbox -> !t.isCompleted && t.projectId == null
                AppView.Completed -> t.isCompleted && !inArchived(t)
                is AppView.ProjectView -> !t.isCompleted && t.projectId == view.id
                is AppView.LabelView -> !t.isCompleted && t.labelIds.contains(view.id) && !inArchived(t)
            }
        }

        val sorted = when (prefs.sort) {
            SortMode.SMART -> matching
            SortMode.PRIORITY -> matching.sortedWith(compareBy({ it.priority.ordinal }, { it.dueDate ?: Long.MAX_VALUE }))
            SortMode.DATE -> matching.sortedWith(compareBy(nullsLast()) { it.dueDate })
            SortMode.ALPHA -> matching.sortedBy { it.title.lowercase() }
            SortMode.NEWEST -> matching.sortedByDescending { it.createdAt }
        }

        fun nodeOf(t: Task) = TaskNode(t, src.tasks.filter { it.parentId == t.id })

        val groups: List<SectionGroup> = if (view is AppView.ProjectView) {
            val projectSections = src.sections.filter { it.projectId == view.id }.sortedBy { it.position }
            val bySection = sorted.groupBy { it.sectionId }
            buildList {
                val noSection = bySection[null].orEmpty().map(::nodeOf)
                if (noSection.isNotEmpty()) add(SectionGroup(null, null, noSection))
                projectSections.forEach { sec ->
                    add(SectionGroup(sec.id, sec.name, bySection[sec.id].orEmpty().map(::nodeOf)))
                }
            }
        } else {
            listOf(SectionGroup(null, null, sorted.map(::nodeOf)))
        }

        val title = when (view) {
            AppView.Today -> "Dzisiaj"
            AppView.Upcoming -> "Nadchodzące"
            AppView.Inbox -> "Skrzynka"
            AppView.Completed -> "Ukończone"
            is AppView.ProjectView -> src.projects.firstOrNull { it.id == view.id }?.name ?: "Projekt"
            is AppView.LabelView -> "@" + (src.labels.firstOrNull { it.id == view.id }?.name ?: "etykieta")
        }

        // Goal counters from completedAt timestamps
        val zone = ZoneId.systemDefault()
        val todayDate = LocalDate.now()
        val monday = todayDate.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(
                if (settings.weekStartMonday.value) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
            )
        )
        var doneToday = 0
        var doneWeek = 0
        var routinesDone = 0
        src.tasks.forEach { t ->
            val at = t.completedAt ?: return@forEach
            val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
            if (date == todayDate) doneToday++
            if (!date.isBefore(monday) && !date.isAfter(todayDate)) doneWeek++
            if (date == todayDate && isRoutine(t)) routinesDone++
        }

        val routines = if (view == AppView.Today) {
            topLevel
                .filter { !it.isCompleted && it.dueDate != null && it.dueDate <= today && !inArchived(it) && isRoutine(it) }
                .sortedBy { it.title.lowercase() }
        } else emptyList()

        return TodoUiState(
            view = view,
            title = title,
            groups = groups,
            isEmpty = sorted.isEmpty(),
            todayCount = topLevel.count { !it.isCompleted && it.dueDate != null && it.dueDate <= today && !inArchived(it) },
            inboxCount = topLevel.count { !it.isCompleted && it.projectId == null },
            sortMode = prefs.sort,
            doneToday = doneToday,
            doneWeek = doneWeek,
            goalDaily = prefs.goalDaily,
            goalWeekly = prefs.goalWeekly,
            currentProject = (view as? AppView.ProjectView)?.let { v -> src.projects.firstOrNull { it.id == v.id } },
            currentLabel = (view as? AppView.LabelView)?.let { v -> src.labels.firstOrNull { it.id == v.id } },
            routines = routines,
            routinesDone = routinesDone,
            estTodayMinutes = topLevel
                .filter { !it.isCompleted && it.dueDate != null && it.dueDate <= today && !inArchived(it) && !isRoutine(it) }
                .sumOf { it.durationMinutes ?: 20 }
        )
    }

    fun setView(view: AppView) { _view.value = view }

    // --- Quick Add (natural language + manual pickers) ---
    fun quickAdd(raw: String, overrides: QuickAddOverrides = QuickAddOverrides()) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            // Rozpoznawanie dat/terminów wg ustawień: gdy wyłączone, tekst trafia
            // dosłownie jako tytuł (bez auto-wykrywania daty, godziny, projektu itd.).
            val parsed = if (settings.dateRecognition.value) QuickAddParser().parse(raw) else null
            val title = parsed?.title?.ifBlank { raw.trim() } ?: raw.trim()

            val currentView = _view.value
            val projectId = when {
                overrides.projectId != null -> overrides.projectId
                parsed?.projectName != null -> repository.ensureProject(parsed.projectName!!)
                currentView is AppView.ProjectView -> currentView.id
                else -> null
            }
            val labelIds = parsed?.labelNames?.map { repository.ensureLabel(it) }?.filter { it != 0L } ?: emptyList()

            val recurrence = overrides.recurrence ?: parsed?.recurrence
            val dueDate = overrides.dueDate ?: parsed?.dueDate
                ?: if (recurrence != null) LocalDate.now().toEpochDay() else null

            val dueTimeMinutes = parsed?.dueTimeMinutes
            val reminderAt = if (dueDate != null && dueTimeMinutes != null) {
                (dueDate * 24L * 60 * 60 * 1000) + dueTimeMinutes * 60L * 1000
            } else null

            repository.insert(
                Task(
                    title = title,
                    priority = overrides.priority ?: parsed?.priority ?: Priority.P4,
                    dueDate = dueDate,
                    dueTimeMinutes = dueTimeMinutes,
                    durationMinutes = parsed?.durationMinutes,
                    deadline = parsed?.deadline,
                    recurrence = recurrence,
                    reminderAt = reminderAt,
                    projectId = projectId,
                    labelIds = labelIds,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
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

    /** Swipe w lewo: odłóż zadanie na jutro (zachowuje godzinę). */
    fun deferToTomorrow(task: Task) = viewModelScope.launch {
        val base = maxOf(task.dueDate ?: LocalDate.now().toEpochDay(), LocalDate.now().toEpochDay())
        repository.update(task.copy(dueDate = base + 1))
    }

    /** Akcja Asystenta tygodnia: przenosi wszystkie zaległe zadania na dziś. */
    fun moveOverdueToToday() = viewModelScope.launch {
        val today = LocalDate.now().toEpochDay()
        allTasks.value
            .filter { !it.isCompleted && it.dueDate != null && it.dueDate < today }
            .forEach { repository.update(it.copy(dueDate = today)) }
    }
    fun deleteTask(task: Task) = viewModelScope.launch { repository.deleteWithSubtasks(task.id) }
    fun deleteCompleted() = viewModelScope.launch { repository.deleteCompleted() }
    fun duplicateTask(id: Long) = viewModelScope.launch { repository.duplicateTask(id) }

    // --- projects / sections / labels ---
    fun addProject(name: String, colorArgb: Long) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertProject(Project(name = name.trim(), colorArgb = colorArgb)) }
    }

    fun deleteProject(id: Long) = viewModelScope.launch {
        if (_view.value == AppView.ProjectView(id)) _view.value = AppView.Today
        repository.deleteProject(id)
    }

    fun duplicateProject(id: Long) = viewModelScope.launch {
        repository.duplicateProject(id, repository.allSections.first(), repository.allTasks.first())
    }

    fun archiveProject(id: Long, archived: Boolean) = viewModelScope.launch {
        if (archived && _view.value == AppView.ProjectView(id)) _view.value = AppView.Today
        repository.setProjectArchived(id, archived)
    }

    fun toggleProjectFavorite(id: Long) = viewModelScope.launch { repository.toggleProjectFavorite(id) }
    fun toggleLabelFavorite(id: Long) = viewModelScope.launch { repository.toggleLabelFavorite(id) }

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

    // --- Pula aktywności ---
    fun addActivity(activity: pl.media30.todoisto.data.Activity) =
        viewModelScope.launch { if (activity.name.isNotBlank()) repository.insertActivity(activity) }

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()
    fun clearImportResult() { _importResult.value = null }

    /** #10 — import aktywności z opublikowanego arkusza Google (link CSV). */
    fun importActivitiesFromCsv(url: String) = viewModelScope.launch {
        if (url.isBlank()) return@launch
        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val target = pl.media30.todoisto.data.ActivityCsvParser.normalizeSheetUrl(url)
                val conn = (java.net.URL(target).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 12000; readTimeout = 12000; instanceFollowRedirects = true
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val acts = pl.media30.todoisto.data.ActivityCsvParser.parse(text)
                acts.forEach { repository.insertActivity(it) }
                if (acts.isEmpty()) "Nie znaleziono aktywności w arkuszu" else "Zaimportowano ${acts.size} aktywności"
            } catch (e: Exception) {
                "Błąd importu: ${e.message ?: "sprawdź link i połączenie"}"
            }
        }
        _importResult.value = result
    }

    fun updateActivity(activity: pl.media30.todoisto.data.Activity) =
        viewModelScope.launch { repository.updateActivity(activity) }

    fun deleteActivity(id: Long) = viewModelScope.launch { repository.deleteActivity(id) }

    fun setProjectEffort(project: Project, effort: pl.media30.todoisto.data.EffortType?) =
        viewModelScope.launch { repository.updateProject(project.copy(workEffortType = effort)) }

    fun suggestFreeTime() = viewModelScope.launch {
        dismissedActivityIds.clear()
        _freeTime.value = computeFreeTime(emptyList())
    }

    fun dismissFreeTime() { _freeTime.value = null }

    /** Zaakceptowana propozycja → zadanie na dziś; panel się zamyka (potwierdzeniem jest wpis na liście). */
    fun acceptSuggestion(s: pl.media30.todoisto.data.Suggestion) = viewModelScope.launch {
        repository.materializeActivity(s.activity, s.startMin, LocalDate.now().toEpochDay())
        _freeTime.value = null
    }

    /** Wygeneruj ponownie inną propozycję (ikonka „odśwież", bez słów). Po wyczerpaniu puli zawija się. */
    fun rerollSuggestion() = viewModelScope.launch {
        dismissedActivityIds += _freeTime.value?.suggestions?.map { it.activity.id }.orEmpty()
        val fresh = computeFreeTime(dismissedActivityIds.toList())
        if (fresh.suggestions.isEmpty() && !fresh.poolEmpty && !fresh.noWindows) {
            dismissedActivityIds.clear()
            _freeTime.value = computeFreeTime(emptyList())
        } else {
            _freeTime.value = fresh
        }
    }

    private fun currentDayContext(): pl.media30.todoisto.data.DayContext {
        val now = java.time.LocalTime.now()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStart = today.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(
                if (settings.weekStartMonday.value) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
            )
        ).atStartOfDay(zone).toInstant().toEpochMilli()
        return repository.buildDayContext(
            tasks = allTasks.value,
            projects = projects.value,
            nowMinutes = now.hour * 60 + now.minute,
            dayOfWeek = today.dayOfWeek.value,
            nowMillis = System.currentTimeMillis(),
            todayStartMillis = todayStart,
            weekStartMillis = weekStart
        )
    }

    private fun computeFreeTime(exclude: List<Long>): FreeTimeState {
        val now = java.time.LocalTime.now()
        val today = LocalDate.now()
        val nowMin = now.hour * 60 + now.minute
        val acts = activities.value.filter { it.isActive && it.id !in exclude }
        val slots = repository.freeWindows(allTasks.value, today.toEpochDay(), nowMin)
        val suggestions = pl.media30.todoisto.data.ActivityPlanner.plan(acts, currentDayContext(), slots, count = 1)
        return FreeTimeState(
            freeMinutes = slots.sumOf { it.length },
            suggestions = suggestions,
            poolEmpty = activities.value.none { it.isActive },
            noWindows = slots.isEmpty()
        )
    }

    class Factory(
        private val repository: TaskRepository,
        private val settings: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
                return TodoViewModel(repository, settings) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
