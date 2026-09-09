package pl.media30.todoisto.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.media30.todoisto.data.Activity
import pl.media30.todoisto.data.ActivityPlanner
import pl.media30.todoisto.data.AiClient
import pl.media30.todoisto.data.AutomationAdvisor
import pl.media30.todoisto.data.DesktopBackgrounds
import pl.media30.todoisto.data.Suggestion
import pl.media30.todoisto.shared.CloudLabel
import pl.media30.todoisto.shared.CloudProject
import pl.media30.todoisto.shared.CloudTask
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Który zbiór zadań pokazuje ekran główny (1:1 z `AppView` w Androidzie). */
sealed class AppView {
    data object Today : AppView()
    data object Upcoming : AppView()
    data object Inbox : AppView()
    data object Completed : AppView()
    data class ProjectView(val id: Long) : AppView()
    data class LabelView(val id: Long) : AppView()
}

enum class SortMode(val label: String, val hint: String) {
    NEAREST("Najbliższe", "Najbliższy termin i godzina na górze"),
    PRIORITY("Priorytet", "Od P1 do P4, potem najbliższy termin"),
    ALPHA("Alfabetycznie", "Od A do Z"),
    NEWEST("Najnowsze", "Ostatnio dodane na górze")
}

data class TaskNode(val task: CloudTask, val subtasks: List<CloudTask> = emptyList())
data class SectionGroup(val sectionId: Long?, val name: String?, val nodes: List<TaskNode>)

data class UiState(
    val view: AppView = AppView.Today,
    val title: String = "Dzisiaj",
    val groups: List<SectionGroup> = emptyList(),
    val isEmpty: Boolean = true,
    val todayCount: Int = 0,
    val inboxCount: Int = 0,
    val doneToday: Int = 0,
    val doneWeek: Int = 0,
    val goalDaily: Int = 5,
    val goalWeekly: Int = 25,
    val currentProject: CloudProject? = null,
    val currentLabel: CloudLabel? = null,
    val routines: List<CloudTask> = emptyList(),
    val routinesDone: Int = 0,
    val estTodayMinutes: Int = 0
)

data class AiAskState(val loading: Boolean = false, val answer: String? = null, val error: String? = null, val needsKey: Boolean = false)
data class ScanState(val loading: Boolean = false, val summary: String = "", val tasks: List<String> = emptyList(), val suggestion: String = "", val plan: String = "", val error: String? = null, val needsKey: Boolean = false)
data class EstimateAiState(val loading: Boolean = false, val minutes: Int? = null, val error: String? = null, val needsKey: Boolean = false)
data class AiCostState(val loading: Boolean = false, val amountUsd: Double? = null, val error: String? = null)
data class CloudState(val busy: Boolean = false, val message: String? = null, val error: String? = null)
data class GmailState(val loading: Boolean = false, val info: String? = null, val error: String? = null)
data class FreeTimeState(val freeMinutes: Int, val suggestions: List<Suggestion>, val poolEmpty: Boolean, val noWindows: Boolean)

sealed class DialogKind {
    data object NewArea : DialogKind()
    data object NewProject : DialogKind()
    data object NewLabel : DialogKind()
    data class NewSection(val projectId: Long) : DialogKind()
    data object Goals : DialogKind()
    data object ApiKey : DialogKind()
    data object AdminKey : DialogKind()
}

/** Rutyna = nawyk cykliczny bez godziny, P3/P4 (jak w `TodoViewModel`). */
fun isRoutine(t: CloudTask) = t.recurrence != null && t.dueTimeMinutes == null && (t.priority == "P3" || t.priority == "P4")

/**
 * Stan aplikacji desktopowej — odpowiednik `TodoViewModel` + flag ekranów z
 * `MainActivity`. Jedna instancja na okno; UI czyta pola, akcje zmieniają repo
 * i podbijają `rev`, co odświeża kompozycję.
 */
class AppState(val repo: TaskRepository, val settings: AppSettings, val scope: CoroutineScope) {
    var rev by mutableStateOf(0); private set
    var settingsRev by mutableStateOf(0); private set
    var dirty by mutableStateOf(0); private set
    fun changed() { rev++; dirty++ }
    fun settingsChanged() { settingsRev++ }

    // Nawigacja i widok
    var view by mutableStateOf<AppView>(if (settings.startView == "upcoming") AppView.Upcoming else AppView.Today)
    var sort by mutableStateOf(runCatching { SortMode.valueOf(settings.sortMode) }.getOrDefault(SortMode.NEAREST))
    var drawerOpen by mutableStateOf(settings.drawerOpen)
    var activeArea by mutableStateOf<Long?>(settings.activeArea.takeIf { it >= 0 })

    // Ekrany / arkusze / dialogi (flagi jak w MainActivity)
    var showSettings by mutableStateOf(false)
    var showAccount by mutableStateOf(false)
    var showStats by mutableStateOf(false)
    var showPool by mutableStateOf(false)
    var showEstimate by mutableStateOf(false)
    var showImport by mutableStateOf(false)
    var showSort by mutableStateOf(false)
    var showScanChooser by mutableStateOf(false)
    var briefOpen by mutableStateOf(false)
    var routOpen by mutableStateOf(false)
    var voiceHint by mutableStateOf(false)
    var quickAddOpen by mutableStateOf(false)
    var quickAddText by mutableStateOf("")
    var editingActivity by mutableStateOf<Activity?>(null)
    var showActivityForm by mutableStateOf(false)
    var detailTaskId by mutableStateOf<Long?>(null)
    var detailAiExpanded by mutableStateOf(false)
    var dialog by mutableStateOf<DialogKind?>(null)
    var reschedTask by mutableStateOf<CloudTask?>(null)
    var toast by mutableStateOf<String?>(null)
    var toastMillis by mutableStateOf(3200L)
    private var autoSyncWarned = false
    var splashDone by mutableStateOf(false)

    // Stany asynchroniczne
    var aiAsk by mutableStateOf<AiAskState?>(null)
    var scan by mutableStateOf<ScanState?>(null)
    var estimateAi by mutableStateOf(EstimateAiState())
    var aiCost by mutableStateOf<AiCostState?>(null)
    var cloud by mutableStateOf(CloudState())
    var gmail by mutableStateOf(GmailState())
    var freeTime by mutableStateOf<FreeTimeState?>(null)
    var syncing by mutableStateOf(false)
    var bgBusy by mutableStateOf(false)
    var importResult by mutableStateOf<String?>(null)
    private val dismissedActivityIds = mutableSetOf<Long>()
    private var lastSyncAt = 0L

    val today: Long get() = LocalDate.now().toEpochDay()

    // ── Stan UI (1:1 z `TodoViewModel.buildState`) ─────────────────────────
    fun buildUiState(): UiState {
        val today = today
        val tasks = repo.tasks
        val projects = repo.projects
        val archivedIds = projects.filter { it.isArchived }.map { it.id }.toSet()
        val projAreaById = projects.associate { it.id to it.areaId }
        val topLevelAll = tasks.filter { it.parentId == null }
        val area = activeArea
        val topLevel = if (area == null) topLevelAll else topLevelAll.filter { t -> (t.projectId?.let { projAreaById[it] } ?: t.areaId) == area }
        val inArchived = { t: CloudTask -> t.projectId != null && archivedIds.contains(t.projectId) }
        val v = view
        val matching = topLevel.filter { t ->
            when (v) {
                AppView.Today -> !t.isCompleted && t.dueDate != null && t.dueDate!! <= today && !inArchived(t) && !isRoutine(t)
                AppView.Upcoming -> !t.isCompleted && t.dueDate != null && t.dueDate!! > today && !inArchived(t)
                AppView.Inbox -> !t.isCompleted && !inArchived(t)
                AppView.Completed -> t.isCompleted && !inArchived(t)
                is AppView.ProjectView -> !t.isCompleted && t.projectId == v.id
                is AppView.LabelView -> !t.isCompleted && t.labelIds.contains(v.id) && !inArchived(t)
            }
        }
        val sorted = when (sort) {
            SortMode.NEAREST -> matching.sortedWith(compareBy({ it.dueDate ?: Long.MAX_VALUE }, { it.dueTimeMinutes ?: Int.MAX_VALUE }))
            SortMode.PRIORITY -> matching.sortedWith(compareBy({ it.priority }, { it.dueDate ?: Long.MAX_VALUE }))
            SortMode.ALPHA -> matching.sortedBy { it.title.lowercase() }
            SortMode.NEWEST -> matching.sortedByDescending { it.createdAt }
        }
        fun nodeOf(t: CloudTask) = TaskNode(t, tasks.filter { it.parentId == t.id })
        val groups: List<SectionGroup> = if (v is AppView.ProjectView) {
            val projectSections = repo.sections.filter { it.projectId == v.id }
            val bySection = sorted.groupBy { it.sectionId }
            buildList {
                val noSection = bySection[null].orEmpty().map(::nodeOf)
                if (noSection.isNotEmpty()) add(SectionGroup(null, null, noSection))
                projectSections.forEach { sec -> add(SectionGroup(sec.id, sec.name, bySection[sec.id].orEmpty().map(::nodeOf))) }
            }
        } else listOf(SectionGroup(null, null, sorted.map(::nodeOf)))

        val title = when (v) {
            AppView.Today -> "Dzisiaj"; AppView.Upcoming -> "Nadchodzące"; AppView.Inbox -> "Skrzynka"; AppView.Completed -> "Ukończone"
            is AppView.ProjectView -> repo.project(v.id)?.name ?: "Projekt"
            is AppView.LabelView -> "@" + (repo.label(v.id)?.name ?: "etykieta")
        }
        val zone = ZoneId.systemDefault()
        val todayDate = LocalDate.now()
        val weekStart = todayDate.with(TemporalAdjusters.previousOrSame(if (settings.weekStartMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY))
        var doneToday = 0; var doneWeek = 0; var routinesDone = 0
        tasks.forEach { t ->
            val at = t.completedAt ?: return@forEach
            val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
            if (date == todayDate) doneToday++
            if (!date.isBefore(weekStart) && !date.isAfter(todayDate)) doneWeek++
            if (date == todayDate && isRoutine(t)) routinesDone++
        }
        val routines = if (v == AppView.Today) topLevel.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! <= today && !inArchived(it) && isRoutine(it) }.sortedBy { it.title.lowercase() } else emptyList()
        return UiState(
            view = v, title = title, groups = groups, isEmpty = sorted.isEmpty(),
            todayCount = topLevel.count { !it.isCompleted && it.dueDate != null && it.dueDate!! <= today && !inArchived(it) },
            inboxCount = topLevel.count { !it.isCompleted && it.projectId == null },
            doneToday = doneToday, doneWeek = doneWeek, goalDaily = settings.dailyGoal, goalWeekly = settings.weeklyGoal,
            currentProject = (v as? AppView.ProjectView)?.let { repo.project(it.id) },
            currentLabel = (v as? AppView.LabelView)?.let { repo.label(it.id) },
            routines = routines, routinesDone = routinesDone,
            estTodayMinutes = topLevel.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! <= today && !inArchived(it) && !isRoutine(it) }.sumOf { it.durationMinutes ?: 20 }
        )
    }

    /** Dzisiejsze otwarte zadania (bez rutyn) — do rozbicia szacowanego czasu. */
    fun todayOpen(): List<CloudTask> {
        val today = today
        val projects = repo.projects
        val projArea = projects.associate { it.id to it.areaId }
        val archived = projects.filter { it.isArchived }.map { it.id }.toSet()
        val area = activeArea
        return repo.tasks.filter { t ->
            t.parentId == null && !t.isCompleted && t.dueDate != null && t.dueDate!! <= today &&
                !(t.projectId != null && archived.contains(t.projectId)) && !isRoutine(t) &&
                (area == null || (t.projectId?.let { projArea[it] } ?: t.areaId) == area)
        }.sortedWith(compareBy({ it.dueTimeMinutes ?: 9999 }, { it.priority }))
    }

    // ── Akcje na zadaniach ─────────────────────────────────────────────────
    fun quickAdd(raw: String): CloudTask? {
        val v = view
        val t = repo.quickAdd(
            raw, dateRecognition = settings.dateRecognition,
            viewProjectId = (v as? AppView.ProjectView)?.id,
            defaultDue = if (v == AppView.Today) today else null,
            areaId = activeArea
        ) ?: return null
        changed()
        toast = "Dodano: ${t.title}"
        // Zadanie bez daty poza Skrzynką/projektem byłoby niewidoczne → pokaż Skrzynkę.
        if (t.dueDate == null && v != AppView.Inbox && v !is AppView.ProjectView && v !is AppView.LabelView) view = AppView.Inbox
        return t
    }
    fun toggle(task: CloudTask) {
        if (!task.isCompleted) settings.recordActualCompletion(task.id)
        repo.toggle(task.id); changed()
    }
    fun updateTask(task: CloudTask) { repo.update(task); changed() }
    fun deleteTask(id: Long) { repo.delete(id); changed() }
    fun duplicateTask(id: Long) { repo.duplicateTask(id); changed() }
    fun addSubtask(parentId: Long, projectId: Long?, title: String) { repo.addSubtask(parentId, projectId, title); changed() }
    fun reschedule(task: CloudTask, epochDay: Long) { settings.addDefer(); repo.reschedule(task.id, epochDay); changed() }
    fun deferToTomorrow(task: CloudTask) { settings.addDefer(); repo.deferToTomorrow(task.id); changed() }
    fun moveOverdueToToday() { repo.rescheduleOverdueToToday(); changed() }
    fun deleteCompleted() { repo.deleteCompleted(); changed() }
    fun openDetail(id: Long) {
        detailTaskId = id; detailAiExpanded = false; aiAsk = null
        settings.recordTaskOpen(LocalTime.now().hour); settings.markTaskOpened(id)
    }
    fun openAutomation(task: CloudTask) {
        detailTaskId = task.id; detailAiExpanded = true
        askAi(AutomationAdvisor.advise(task.title, task.notes).aiPrompt)
    }
    fun changeSort(m: SortMode) { sort = m; settings.sortMode = m.name }
    // Historia widoków (strzałki ‹ › w pasku, jak w Todoist).
    private val history = mutableListOf<AppView>(view)
    private var historyIndex by mutableStateOf(0)
    val canGoBack: Boolean get() = historyIndex > 0
    val canGoForward: Boolean get() = historyIndex < history.size - 1
    fun showView(v: AppView) {
        if (v == view) return
        while (history.size - 1 > historyIndex) history.removeAt(history.size - 1)
        history.add(v); historyIndex = history.size - 1
        view = v
    }
    fun goBack() { if (canGoBack) { historyIndex--; view = history[historyIndex] } }
    fun goForward() { if (canGoForward) { historyIndex++; view = history[historyIndex] } }
    fun selectArea(id: Long?) { activeArea = id; settings.activeArea = id ?: -1L }
    fun setDrawer(open: Boolean) { drawerOpen = open; settings.drawerOpen = open }

    // ── Projekty / etykiety / obszary / sekcje ─────────────────────────────
    fun addProject(name: String, color: Long) { repo.addProject(name, color, activeArea); changed() }
    fun deleteProject(id: Long) { if (view == AppView.ProjectView(id)) view = AppView.Today; repo.deleteProject(id); changed() }
    fun duplicateProject(id: Long) { repo.duplicateProject(id); changed() }
    fun archiveProject(id: Long, archived: Boolean) { if (archived && view == AppView.ProjectView(id)) view = AppView.Today; repo.setProjectArchived(id, archived); changed() }
    fun toggleProjectFavorite(id: Long) { repo.toggleProjectFavorite(id); changed() }
    fun toggleLabelFavorite(id: Long) { repo.toggleLabelFavorite(id); changed() }
    fun addSection(projectId: Long, name: String) { repo.addSection(projectId, name); changed() }
    fun addLabel(name: String, color: Long) { repo.addLabel(name, color); changed() }
    fun deleteLabel(id: Long) { if (view == AppView.LabelView(id)) view = AppView.Today; repo.deleteLabel(id); changed() }
    fun addArea(name: String, color: Long) { val id = repo.addArea(name, color); changed(); if (id != 0L) selectArea(id) }
    fun setGoals(daily: Int, weekly: Int) { settings.dailyGoal = daily.coerceIn(1, 999); settings.weeklyGoal = weekly.coerceIn(1, 9999); settingsChanged() }

    // ── AI ─────────────────────────────────────────────────────────────────
    fun askAi(prompt: String) {
        val key = settings.openAiKey
        if (key.isBlank()) { aiAsk = AiAskState(needsKey = true); return }
        aiAsk = AiAskState(loading = true)
        scope.launch {
            aiAsk = try {
                val r = AiClient.ask(key, prompt)
                settings.addAiUsage(r.promptTokens, r.completionTokens); settingsChanged()
                AiAskState(answer = r.text)
            } catch (e: Exception) { AiAskState(error = e.message ?: "Błąd połączenia") }
        }
    }
    fun dismissAi() { aiAsk = null }

    fun scanImage(bytes: ByteArray) {
        val key = settings.openAiKey
        if (key.isBlank()) { scan = ScanState(needsKey = true); return }
        scan = ScanState(loading = true)
        scope.launch {
            scan = try {
                val r = AiClient.analyzeImageSmart(key, bytes)
                ScanState(summary = r.summary, tasks = r.tasks, suggestion = r.suggestion, plan = r.plan)
            } catch (e: Exception) { ScanState(error = e.message ?: "Błąd skanowania") }
        }
    }
    fun addScanTasks(titles: List<String>) { titles.forEach { quickAdd(it) }; scan = null }
    fun addScanSuggestion(title: String, plan: String) {
        if (title.isNotBlank()) { repo.add(CloudTask(id = 0, title = title.trim(), notes = plan.trim())); changed() }
        scan = null
    }

    fun estimateTodayWithAi() {
        val key = settings.openAiKey
        val titles = todayOpen().map { it.title }
        if (key.isBlank()) { estimateAi = EstimateAiState(needsKey = true); return }
        if (titles.isEmpty()) { estimateAi = EstimateAiState(minutes = 0); return }
        estimateAi = EstimateAiState(loading = true)
        scope.launch {
            estimateAi = try {
                val r = AiClient.estimateMinutes(key, titles, settings.avgActualMinutes)
                settings.addAiUsage(r.promptTokens, r.completionTokens); settingsChanged()
                EstimateAiState(minutes = r.minutes)
            } catch (e: Exception) { EstimateAiState(error = e.message ?: "Błąd połączenia") }
        }
    }

    fun refreshAiCost() {
        val adminKey = settings.openAiAdminKey
        if (adminKey.isBlank()) { aiCost = null; return }
        val startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond()
        aiCost = AiCostState(loading = true)
        scope.launch {
            aiCost = try { AiCostState(amountUsd = AiClient.fetchCostsUsd(adminKey, startOfMonth)) }
            catch (e: Exception) { AiCostState(error = e.message ?: "Błąd połączenia") }
        }
    }

    // ── Chmura ─────────────────────────────────────────────────────────────
    fun setCloudConfig(url: String, anon: String) {
        settings.url = url; settings.anonKey = anon; settingsChanged()
        cloud = CloudState(message = "Zapisano konfigurację projektu.")
    }
    fun cloudSignIn(email: String, password: String) = auth(email, password, false)
    fun cloudSignUp(email: String, password: String) = auth(email, password, true)
    private fun auth(email: String, password: String, register: Boolean) {
        cloud = CloudState(busy = true)
        scope.launch {
            val r = withContext(Dispatchers.IO) { cloudAuth(settings, email, password, register) }
            settingsChanged()
            cloud = if (r.ok) CloudState(message = r.message) else CloudState(error = r.message)
            if (r.ok) runSync(auto = true)
        }
    }
    fun cloudSignOut() { settings.email = ""; settings.cloudPassword = ""; settingsChanged(); cloud = CloudState(message = "Wylogowano z chmury.") }
    fun cloudBackup() {
        cloud = CloudState(busy = true)
        scope.launch { val r = withContext(Dispatchers.IO) { cloudBackup(repo, settings) }; cloud = if (r.ok) CloudState(message = r.message) else CloudState(error = r.message) }
    }
    fun cloudRestore() {
        cloud = CloudState(busy = true)
        scope.launch { val r = withContext(Dispatchers.IO) { cloudRestore(repo, settings) }; rev++; cloud = if (r.ok) CloudState(message = r.message) else CloudState(error = r.message) }
    }
    /** Pełny sync (pull → scal → push). Auto = cicho, bez komunikatów o błędach. */
    fun runSync(auto: Boolean) {
        if (syncing) return
        if (!settings.signedIn) { if (!auto) { showSettings = true; showAccount = true }; return }
        if (auto && System.currentTimeMillis() - lastSyncAt < 10_000) return
        syncing = true
        if (!auto) toast = "Synchronizuję…"
        scope.launch {
            val r = withContext(Dispatchers.IO) { sync(repo, settings) }
            lastSyncAt = System.currentTimeMillis()
            syncing = false; rev++
            if (!auto || r.ok) toast = r.message
            else if (!autoSyncWarned) { autoSyncWarned = true; toastMillis = 8000; toast = "Synchronizacja nie działa: ${r.message}" }
            if (!auto) cloud = if (r.ok) CloudState(message = r.message) else CloudState(error = r.message)
        }
    }

    // ── Gmail ──────────────────────────────────────────────────────────────
    fun setGmailCreds(user: String, pass: String) {
        settings.gmailUser = user; settings.gmailPass = pass; settingsChanged()
        gmail = GmailState(info = if (user.isBlank()) "Rozłączono z Gmailem." else "Zapisano dane Gmaila.")
    }
    fun syncGmail() {
        gmail = GmailState(loading = true)
        scope.launch {
            val r = withContext(Dispatchers.IO) { fetchGmail(repo, settings) }
            changed()
            gmail = if (r.ok) GmailState(info = r.message) else GmailState(error = r.message)
        }
    }

    // ── Pula aktywności / czas wolny ───────────────────────────────────────
    fun addActivity(a: Activity) { repo.addActivity(a); changed() }
    fun updateActivity(a: Activity) { repo.updateActivity(a); changed() }
    fun deleteActivity(id: Long) { repo.deleteActivity(id); changed() }
    fun importActivities(url: String) {
        scope.launch { importResult = withContext(Dispatchers.IO) { importActivitiesFromCsv(repo, url) }; changed() }
    }
    fun suggestFreeTime() { dismissedActivityIds.clear(); freeTime = computeFreeTime(emptyList()) }
    fun rerollSuggestion() {
        dismissedActivityIds += freeTime?.suggestions?.map { it.activity.id }.orEmpty()
        val fresh = computeFreeTime(dismissedActivityIds.toList())
        freeTime = if (fresh.suggestions.isEmpty() && !fresh.poolEmpty && !fresh.noWindows) { dismissedActivityIds.clear(); computeFreeTime(emptyList()) } else fresh
    }
    fun acceptSuggestion(s: Suggestion) { repo.materializeActivity(s.activity, s.startMin, today); changed(); freeTime = null }
    private fun computeFreeTime(exclude: List<Long>): FreeTimeState {
        val now = LocalTime.now(); val todayDate = LocalDate.now()
        val nowMin = now.hour * 60 + now.minute
        val acts = repo.activities.filter { it.isActive && it.id !in exclude }
        val slots = repo.freeWindows(today, nowMin)
        val zone = ZoneId.systemDefault()
        val ctx = repo.buildDayContext(
            nowMinutes = nowMin, dayOfWeek = todayDate.dayOfWeek.value, nowMillis = System.currentTimeMillis(),
            todayStartMillis = todayDate.atStartOfDay(zone).toInstant().toEpochMilli(),
            weekStartMillis = todayDate.with(TemporalAdjusters.previousOrSame(if (settings.weekStartMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY)).atStartOfDay(zone).toInstant().toEpochMilli()
        )
        val suggestions = ActivityPlanner.plan(acts, ctx, slots, count = 1)
        return FreeTimeState(slots.sumOf { it.length }, suggestions, repo.activities.none { it.isActive }, slots.isEmpty())
    }

    // ── Tła ────────────────────────────────────────────────────────────────
    fun addPresetBackgrounds() {
        if (bgBusy) return
        bgBusy = true
        scope.launch {
            val paths = withContext(Dispatchers.IO) {
                (0 until 3).map { i ->
                    runCatching {
                        val seed = kotlin.random.Random.nextInt(1, 1_000_000)
                        val photo = DesktopBackgrounds.fetchRandomPhoto(1024, 1536, seed)
                        settings.saveBackgroundBytes(DesktopBackgrounds.withColorMask(photo, kotlin.random.Random.nextInt(DesktopBackgrounds.maskCount)), "photo_${System.currentTimeMillis()}_$i.jpg")
                    }.getOrElse {
                        settings.saveBackgroundBytes(DesktopBackgrounds.gradientPng(1080, 1920, kotlin.random.Random.nextInt(100)), "preset_${System.currentTimeMillis()}_$i.png")
                    }
                }
            }
            settings.customPhotos = paths
            paths.firstOrNull()?.let { settings.activeCustomBg = it; settings.photoBackground = false }
            bgBusy = false; settingsChanged()
        }
    }
    fun addCustomPhoto(file: File) {
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                DesktopBackgrounds.scaledJpeg(file)?.let { settings.saveBackgroundBytes(it, "up_${System.currentTimeMillis()}.jpg") }
            } ?: return@launch
            settings.customPhotos = (settings.customPhotos + path).takeLast(3)
            settings.activeCustomBg = path; settings.photoBackground = false
            settingsChanged()
        }
    }
    fun setActiveCustomBg(path: String) { settings.activeCustomBg = path; if (path.isNotBlank()) settings.photoBackground = false; settingsChanged() }
    fun removeCustomPhoto(path: String) {
        settings.customPhotos = settings.customPhotos.filterNot { it == path }
        if (settings.activeCustomBg == path) settings.activeCustomBg = ""
        settingsChanged()
    }
}
