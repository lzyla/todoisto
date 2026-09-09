package pl.media30.todoisto.desktop

import pl.media30.todoisto.data.Activity
import pl.media30.todoisto.data.DayContext
import pl.media30.todoisto.data.EffortType
import pl.media30.todoisto.data.EnergyCost
import pl.media30.todoisto.data.FreeSlot
import pl.media30.todoisto.data.Place
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.shared.CloudActivity
import pl.media30.todoisto.shared.CloudArea
import pl.media30.todoisto.shared.CloudBackupCodec
import pl.media30.todoisto.shared.CloudLabel
import pl.media30.todoisto.shared.CloudProject
import pl.media30.todoisto.shared.CloudSection
import pl.media30.todoisto.shared.CloudSnapshot
import pl.media30.todoisto.shared.CloudTask
import pl.media30.todoisto.shared.SnapshotMerge
import pl.media30.todoisto.shared.SupabaseSync
import java.io.File
import java.time.LocalDate
import java.util.prefs.Preferences

/** Kolory presetów (jak `PaletteColors` w Androidzie). */
val PaletteColors: List<Long> = listOf(
    0xFF9B6BFF, 0xFFC24DFF, 0xFF4D6BFF, 0xFF2DD4BF, 0xFF34D399, 0xFFFBBF24, 0xFFFB7185, 0xFFF87171
)

const val ACTIVITY_LABEL = "aktywność"
fun activityIdFromNotes(notes: String): Long? =
    if (notes.startsWith("activity:")) notes.removePrefix("activity:").toLongOrNull() else null

/** Konwersje Activity ↔ CloudActivity (enumy jako String w chmurze). */
fun Activity.toCloud() = CloudActivity(
    id = id, name = name, effortType = effortType.name, durationMinutes = durationMinutes, place = place.name,
    windowStartMin = windowStartMin, windowEndMin = windowEndMin, daysMask = daysMask, energyCost = energyCost.name,
    frequencyTarget = frequencyTarget, isActive = isActive, lastScheduledAt = lastScheduledAt,
    lastCompletedAt = lastCompletedAt, createdAt = createdAt
)
fun CloudActivity.toActivity() = Activity(
    id = id, name = name,
    effortType = runCatching { EffortType.valueOf(effortType) }.getOrDefault(EffortType.MENTAL),
    durationMinutes = durationMinutes,
    place = runCatching { Place.valueOf(place) }.getOrDefault(Place.HOME),
    windowStartMin = windowStartMin, windowEndMin = windowEndMin, daysMask = daysMask,
    energyCost = runCatching { EnergyCost.valueOf(energyCost) }.getOrDefault(EnergyCost.MED),
    frequencyTarget = frequencyTarget, isActive = isActive, lastScheduledAt = lastScheduledAt,
    lastCompletedAt = lastCompletedAt, createdAt = createdAt
)

/**
 * Repozytorium desktopu — trzyma pełną migawkę (zadania z podzadaniami, projekty,
 * sekcje, etykiety, obszary, aktywności) w tym samym formacie co chmura, zapisuje
 * ją lokalnie w `~/.todoisto/data.json` i synchronizuje przez wspólny moduł `shared`.
 * Semantyka operacji 1:1 z `TaskRepository` w Androidzie.
 */
class TaskRepository {
    private val dataFile = File(System.getProperty("user.home"), ".todoisto/data.json")
    private val _tasks = mutableListOf<CloudTask>()
    private val _projects = mutableListOf<CloudProject>()
    private val _labels = mutableListOf<CloudLabel>()
    private val _areas = mutableListOf<CloudArea>()
    private val _sections = mutableListOf<CloudSection>()
    private val _activities = mutableListOf<CloudActivity>()
    private var nextId = System.currentTimeMillis()

    /** Licznik zmian — UI obserwuje go, żeby się odświeżyć. */
    var revision: Int = 0
        private set

    // Widoczne zadania (bez nagrobków), z podzadaniami.
    val tasks: List<CloudTask> get() = _tasks.filter { !it.deleted }.sortedBy { it.position }
    val projects: List<CloudProject> get() = _projects.sortedBy { it.position }
    val labels: List<CloudLabel> get() = _labels.toList()
    val areas: List<CloudArea> get() = _areas.sortedBy { it.position }
    val sections: List<CloudSection> get() = _sections.sortedBy { it.position }
    val activities: List<Activity> get() = _activities.map { it.toActivity() }.sortedWith(compareBy({ !it.isActive }, { it.name.lowercase() }))

    fun project(id: Long?): CloudProject? = id?.let { pid -> _projects.firstOrNull { it.id == pid } }
    fun label(id: Long): CloudLabel? = _labels.firstOrNull { it.id == id }
    fun area(id: Long?): CloudArea? = id?.let { aid -> _areas.firstOrNull { it.id == aid } }
    fun task(id: Long): CloudTask? = _tasks.firstOrNull { it.id == id && !it.deleted }
    fun subtasks(parentId: Long): List<CloudTask> = _tasks.filter { it.parentId == parentId && !it.deleted }
    fun projectName(id: Long?): String = project(id)?.name ?: "Skrzynka"
    fun labelName(id: Long): String = label(id)?.name ?: "etykieta"

    init { load() }

    private fun newId(): Long = nextId++

    // ── Zadania ────────────────────────────────────────────────────────────

    fun add(task: CloudTask): CloudTask {
        val now = System.currentTimeMillis()
        val withId = task.copy(
            id = newId(), position = _tasks.size,
            createdAt = if (task.createdAt == 0L) now else task.createdAt, updatedAt = now
        )
        _tasks.add(withId); save(); return withId
    }

    /**
     * Szybkie dodawanie jak w Androidzie (`TodoViewModel.quickAdd`): parser języka
     * naturalnego (gdy włączony), #projekt tworzy projekt, @etykieta tworzy etykietę,
     * cykl bez daty → dziś, godzina → przypomnienie o czasie.
     */
    fun quickAdd(
        raw: String, dateRecognition: Boolean = true,
        viewProjectId: Long? = null, defaultDue: Long? = null, areaId: Long? = null
    ): CloudTask? {
        if (raw.isBlank()) return null
        val parsed = if (dateRecognition) QuickAddParser().parse(raw) else null
        val title = parsed?.title?.ifBlank { raw.trim() } ?: raw.trim()
        val projectId = when {
            parsed?.projectName != null -> ensureProject(parsed.projectName!!)
            viewProjectId != null -> viewProjectId
            else -> null
        }
        val labelIds = parsed?.labelNames?.map { ensureLabel(it) }?.filter { it != 0L } ?: emptyList()
        val recurrence = parsed?.recurrence
        val dueDate = parsed?.dueDate ?: (if (recurrence != null) LocalDate.now().toEpochDay() else defaultDue)
        val dueTime = parsed?.dueTimeMinutes
        val reminderAt = if (dueDate != null && dueTime != null) dueDate * 86_400_000L + dueTime * 60_000L else null
        return add(CloudTask(
            id = 0, title = title, priority = (parsed?.priority ?: Priority.P4).name,
            dueDate = dueDate, dueTimeMinutes = dueTime, durationMinutes = parsed?.durationMinutes,
            deadline = parsed?.deadline, recurrence = recurrence?.name, reminderAt = reminderAt,
            projectId = projectId, labelIds = labelIds, areaId = if (projectId == null) areaId else null
        ))
    }

    fun addSubtask(parentId: Long, projectId: Long?, title: String): CloudTask? {
        if (title.isBlank()) return null
        return add(CloudTask(id = 0, title = title.trim(), parentId = parentId, projectId = projectId))
    }

    /** Odhaczenie: cykliczne przechodzą na kolejny termin, inne kończą się (completedAt liczy do celów). */
    fun toggle(id: Long) {
        val i = _tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val t = _tasks[i]
        val now = System.currentTimeMillis()
        val rec = t.recurrence?.let { Recurrence.fromNameSafe(it) }
        _tasks[i] = when {
            rec != null && !t.isCompleted && t.dueDate != null -> t.copy(dueDate = rec.next(t.dueDate!!), completedAt = now, updatedAt = now)
            !t.isCompleted -> t.copy(isCompleted = true, completedAt = now, updatedAt = now)
            else -> t.copy(isCompleted = false, completedAt = null, updatedAt = now)
        }
        if (!t.isCompleted) activityIdFromNotes(t.notes)?.let { aid ->
            val ai = _activities.indexOfFirst { it.id == aid }
            if (ai >= 0) _activities[ai] = _activities[ai].copy(lastCompletedAt = now)
        }
        save()
    }

    fun update(task: CloudTask) {
        if (task.title.isBlank()) return
        val i = _tasks.indexOfFirst { it.id == task.id }
        if (i >= 0) { _tasks[i] = task.copy(title = task.title.trim(), notes = task.notes.trim(), updatedAt = System.currentTimeMillis()); save() }
    }

    fun reschedule(id: Long, epochDay: Long) { task(id)?.let { update(it.copy(dueDate = epochDay)) } }

    fun deferToTomorrow(id: Long) {
        val t = task(id) ?: return
        val today = LocalDate.now().toEpochDay()
        update(t.copy(dueDate = maxOf(t.dueDate ?: today, today) + 1))
    }

    /** „Przenieś zaległe na dzisiaj" (Asystent tygodnia / Todoist „Zmień termin"). */
    fun rescheduleOverdueToToday() {
        val today = LocalDate.now().toEpochDay()
        val now = System.currentTimeMillis()
        for (i in _tasks.indices) {
            val t = _tasks[i]
            if (!t.deleted && !t.isCompleted && t.dueDate != null && t.dueDate!! < today) _tasks[i] = t.copy(dueDate = today, updatedAt = now)
        }
        save()
    }

    /** Usuwa jako nagrobek (z podzadaniami) — usunięcie propaguje się przy synchronizacji. */
    fun delete(id: Long) {
        val now = System.currentTimeMillis()
        for (i in _tasks.indices) {
            val t = _tasks[i]
            if (t.id == id || t.parentId == id) _tasks[i] = t.copy(deleted = true, updatedAt = now)
        }
        save()
    }

    fun deleteCompleted() {
        val now = System.currentTimeMillis()
        for (i in _tasks.indices) if (_tasks[i].isCompleted && !_tasks[i].deleted) _tasks[i] = _tasks[i].copy(deleted = true, updatedAt = now)
        save()
    }

    fun duplicateTask(id: Long) {
        val original = task(id) ?: return
        val copy = add(original.copy(title = original.title + " (kopia)", isCompleted = false, completedAt = null))
        subtasks(id).forEach { add(it.copy(parentId = copy.id, isCompleted = false, completedAt = null)) }
    }

    // ── Projekty / sekcje / etykiety / obszary ─────────────────────────────

    fun addProject(name: String, colorArgb: Long, areaId: Long? = null): Long {
        if (name.isBlank()) return 0
        val p = CloudProject(id = newId(), name = name.trim(), colorArgb = colorArgb, position = _projects.size, areaId = areaId)
        _projects.add(p); save(); return p.id
    }
    fun updateProject(p: CloudProject) { val i = _projects.indexOfFirst { it.id == p.id }; if (i >= 0) { _projects[i] = p; save() } }
    fun deleteProject(id: Long) {
        val now = System.currentTimeMillis()
        for (i in _tasks.indices) if (_tasks[i].projectId == id) _tasks[i] = _tasks[i].copy(deleted = true, updatedAt = now)
        _sections.removeAll { it.projectId == id }
        _projects.removeAll { it.id == id }
        save()
    }
    fun setProjectArchived(id: Long, archived: Boolean) { project(id)?.let { updateProject(it.copy(isArchived = archived)) } }
    fun toggleProjectFavorite(id: Long) { project(id)?.let { updateProject(it.copy(isFavorite = !it.isFavorite)) } }
    fun duplicateProject(id: Long) {
        val original = project(id) ?: return
        val newId = addProject(original.name + " (kopia)", original.colorArgb, original.areaId)
        val sectionMap = mutableMapOf<Long, Long>()
        _sections.filter { it.projectId == id }.forEach { sectionMap[it.id] = addSection(newId, it.name) }
        val taskMap = mutableMapOf<Long, Long>()
        val projectTasks = tasks.filter { it.projectId == id && !it.isCompleted }
        projectTasks.filter { it.parentId == null }.forEach { t ->
            taskMap[t.id] = add(t.copy(projectId = newId, sectionId = t.sectionId?.let { sectionMap[it] }, completedAt = null)).id
        }
        projectTasks.filter { it.parentId != null }.forEach { t ->
            val np = taskMap[t.parentId] ?: return@forEach
            add(t.copy(projectId = newId, parentId = np, sectionId = null, completedAt = null))
        }
    }
    fun ensureProject(name: String): Long {
        val n = name.trim(); if (n.isEmpty()) return 0
        return _projects.firstOrNull { it.name.equals(n, ignoreCase = true) }?.id ?: addProject(n, PaletteColors[0])
    }

    fun addSection(projectId: Long, name: String): Long {
        if (name.isBlank()) return 0
        val s = CloudSection(id = newId(), projectId = projectId, name = name.trim(), position = _sections.count { it.projectId == projectId })
        _sections.add(s); save(); return s.id
    }
    fun deleteSection(id: Long) {
        _sections.removeAll { it.id == id }
        for (i in _tasks.indices) if (_tasks[i].sectionId == id) _tasks[i] = _tasks[i].copy(sectionId = null)
        save()
    }

    fun addLabel(name: String, colorArgb: Long): Long {
        if (name.isBlank()) return 0
        val l = CloudLabel(id = newId(), name = name.trim(), colorArgb = colorArgb)
        _labels.add(l); save(); return l.id
    }
    fun deleteLabel(id: Long) {
        _labels.removeAll { it.id == id }
        for (i in _tasks.indices) if (id in _tasks[i].labelIds) _tasks[i] = _tasks[i].copy(labelIds = _tasks[i].labelIds - id)
        save()
    }
    fun toggleLabelFavorite(id: Long) {
        val i = _labels.indexOfFirst { it.id == id }
        if (i >= 0) { _labels[i] = _labels[i].copy(isFavorite = !_labels[i].isFavorite); save() }
    }
    fun ensureLabel(name: String): Long {
        val n = name.trim(); if (n.isEmpty()) return 0
        return _labels.firstOrNull { it.name.equals(n, ignoreCase = true) }?.id ?: addLabel(n, 0xFF4D6BFF)
    }

    fun addArea(name: String, colorArgb: Long): Long {
        if (name.isBlank()) return 0
        val a = CloudArea(id = newId(), name = name.trim(), colorArgb = colorArgb, position = _areas.size)
        _areas.add(a); save(); return a.id
    }

    // ── Pula aktywności ────────────────────────────────────────────────────

    fun addActivity(a: Activity): Long {
        if (a.name.isBlank()) return 0
        val id = newId()
        _activities.add(a.copy(id = id, createdAt = if (a.createdAt == 0L) System.currentTimeMillis() else a.createdAt).toCloud())
        save(); return id
    }
    fun updateActivity(a: Activity) {
        val i = _activities.indexOfFirst { it.id == a.id }
        if (i >= 0) { _activities[i] = a.toCloud(); save() }
    }
    fun deleteActivity(id: Long) { _activities.removeAll { it.id == id }; save() }

    /** Zaakceptowana propozycja → zadanie na dziś z etykietą „aktywność". */
    fun materializeActivity(a: Activity, startMin: Int, todayEpochDay: Long): Long {
        val labelId = _labels.firstOrNull { it.name == ACTIVITY_LABEL }?.id ?: addLabel(ACTIVITY_LABEL, 0xFF2DD4BF)
        val t = add(CloudTask(
            id = 0, title = a.name, notes = "activity:${a.id}", priority = "P4", dueDate = todayEpochDay,
            dueTimeMinutes = startMin, durationMinutes = a.durationMinutes, labelIds = listOf(labelId)
        ))
        updateActivity(a.copy(lastScheduledAt = System.currentTimeMillis()))
        return t.id
    }

    fun buildDayContext(nowMinutes: Int, dayOfWeek: Int, nowMillis: Long, todayStartMillis: Long, weekStartMillis: Long): DayContext {
        val projEffort = _projects.associate { it.id to (it.workEffortType?.let { e -> runCatching { EffortType.valueOf(e) }.getOrNull() } ?: EffortType.MENTAL) }
        val all = tasks
        val doneToday = all.filter { it.isCompleted && (it.completedAt ?: 0L) >= todayStartMillis }
        var mentalMin = 0; var physMin = 0; var totalMin = 0; var budget = 100
        doneToday.forEach { t ->
            val dur = t.durationMinutes ?: 25
            totalMin += dur
            when (t.projectId?.let { projEffort[it] } ?: EffortType.MENTAL) { EffortType.PHYSICAL -> physMin += dur; else -> mentalMin += dur }
            budget -= 6 + dur / 10 + when (t.priority) { "P1" -> 14; "P2" -> 9; "P3" -> 4; else -> 2 }
        }
        val denom = (mentalMin + physMin).coerceAtLeast(1).toFloat()
        val weekCount = mutableMapOf<Long, Int>()
        all.filter { it.isCompleted && (it.completedAt ?: 0L) >= weekStartMillis }.forEach { t ->
            activityIdFromNotes(t.notes)?.let { aid -> weekCount[aid] = (weekCount[aid] ?: 0) + 1 }
        }
        val lastDone = _activities.mapNotNull { a -> a.lastCompletedAt?.let { a.id to it } }.toMap()
        return DayContext(
            completedCount = doneToday.size, totalWorkMinutes = totalMin,
            mentalShare = mentalMin / denom, physicalShare = physMin / denom,
            energyBudget = budget.coerceIn(0, 100), nowMinutes = nowMinutes, dayOfWeek = dayOfWeek,
            weekCountByActivity = weekCount, lastDoneByActivity = lastDone, nowMillis = nowMillis
        )
    }

    fun freeWindows(todayEpochDay: Long, fromMin: Int, dayStart: Int = 6 * 60, dayEnd: Int = 23 * 60, minLen: Int = 15): List<FreeSlot> {
        val busy = tasks.filter { !it.isCompleted && it.dueDate == todayEpochDay && it.dueTimeMinutes != null }
            .map { it.dueTimeMinutes!! to (it.dueTimeMinutes!! + (it.durationMinutes ?: 30)) }.sortedBy { it.first }
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

    // ── Migawka / dysk ─────────────────────────────────────────────────────

    fun replaceAll(snapshot: CloudSnapshot) {
        _tasks.clear(); _tasks.addAll(snapshot.tasks)
        _projects.clear(); _projects.addAll(snapshot.projects)
        _labels.clear(); _labels.addAll(snapshot.labels)
        _areas.clear(); _areas.addAll(snapshot.areas)
        _sections.clear(); _sections.addAll(snapshot.sections)
        _activities.clear(); _activities.addAll(snapshot.activities)
        bumpNextId()
        save()
    }

    fun snapshot(): CloudSnapshot = CloudSnapshot(
        tasks = _tasks.toList(), projects = _projects.toList(), labels = _labels.toList(),
        areas = _areas.toList(), sections = _sections.toList(), activities = _activities.toList()
    )

    private fun bumpNextId() {
        val maxId = listOf(
            _tasks.maxOfOrNull { it.id }, _projects.maxOfOrNull { it.id }, _labels.maxOfOrNull { it.id },
            _areas.maxOfOrNull { it.id }, _sections.maxOfOrNull { it.id }, _activities.maxOfOrNull { it.id }
        ).maxOf { it ?: 0L }
        nextId = maxOf(nextId, maxId + 1)
    }

    private fun load() {
        runCatching {
            if (dataFile.exists()) {
                val snap = CloudBackupCodec.fromJson(dataFile.readText())
                _tasks.addAll(snap.tasks); _projects.addAll(snap.projects); _labels.addAll(snap.labels)
                _areas.addAll(snap.areas); _sections.addAll(snap.sections); _activities.addAll(snap.activities)
                purgeOldTombstones()
                bumpNextId()
            }
        }
        // Celowo BEZ danych przykładowych — prawdziwe zadania przychodzą z chmury.
    }

    private fun purgeOldTombstones() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        _tasks.removeAll { it.deleted && it.updatedAt in 1 until cutoff }
    }

    private fun save() {
        revision++
        runCatching {
            dataFile.parentFile.mkdirs()
            dataFile.writeText(CloudBackupCodec.toJson(snapshot()))
        }
    }
}

/**
 * Ustawienia — te same klucze i wartości domyślne co `SettingsStore` w Androidzie
 * (plus dane chmury/profilu, które na telefonie żyją w `CloudStore`/`AccountStore`).
 * Przechowywane lokalnie w java.util.prefs.
 */
class AppSettings {
    private val prefs = Preferences.userRoot().node("pl/media30/todoisto/desktop")
    private fun str(k: String, d: String = "") = prefs.get(k, d)
    private fun bool(k: String, d: Boolean) = prefs.getBoolean(k, d)

    // Chmura (Supabase) + sesja
    var url: String get() = str("url"); set(v) = prefs.put("url", v.trim())
    var anonKey: String get() = str("anonKey"); set(v) = prefs.put("anonKey", v.trim())
    var email: String get() = str("email"); set(v) = prefs.put("email", v.trim())
    /** Hasło do chmury — zapamiętane, żeby auto-sync działał po restarcie (jak sesja na telefonie). */
    var cloudPassword: String get() = str("cloudPass"); set(v) = prefs.put("cloudPass", v)
    val configured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
    val signedIn: Boolean get() = configured && email.isNotBlank() && cloudPassword.isNotBlank()
    var autoSync: Boolean get() = bool("autoSync", true); set(v) = prefs.putBoolean("autoSync", v)

    // Profil (lokalny)
    var userName: String get() = str("userName"); set(v) = prefs.put("userName", v.trim())
    var avatarPath: String get() = str("avatar"); set(v) = prefs.put("avatar", v)

    // Wygląd
    var darkTheme: Boolean get() = bool("dark_theme", false); set(v) = prefs.putBoolean("dark_theme", v)
    var themeId: String get() = str("color_theme", "violet"); set(v) = prefs.put("color_theme", v)
    var photoBackground: Boolean get() = bool("photo_background", false); set(v) = prefs.putBoolean("photo_background", v)
    var customPhotos: List<String>
        get() = str("custom_photos").split("\n").filter { it.isNotBlank() }.take(3)
        set(v) = prefs.put("custom_photos", v.take(3).joinToString("\n"))
    var activeCustomBg: String get() = str("active_custom_bg"); set(v) = prefs.put("active_custom_bg", v)
    var drawerOpen: Boolean get() = bool("drawer_open", true); set(v) = prefs.putBoolean("drawer_open", v)
    /** Dock na dole: domyślnie wysuwa się po najechaniu myszą; true = zawsze widoczny. */
    var dockAlwaysVisible: Boolean get() = bool("dock_always", false); set(v) = prefs.putBoolean("dock_always", v)

    // Ogólne
    var startView: String get() = str("start_view", "today").ifBlank { "today" }; set(v) = prefs.put("start_view", v)
    var dateRecognition: Boolean get() = bool("date_recognition", true); set(v) = prefs.putBoolean("date_recognition", v)
    var weekStartMonday: Boolean get() = bool("week_start_monday", true); set(v) = prefs.putBoolean("week_start_monday", v)
    var completionSound: Boolean get() = bool("completion_sound", false); set(v) = prefs.putBoolean("completion_sound", v)
    var swipeRightCompletes: Boolean get() = bool("swipe_right_completes", true); set(v) = prefs.putBoolean("swipe_right_completes", v)
    var sortMode: String get() = str("sort_mode", "NEAREST"); set(v) = prefs.put("sort_mode", v)

    // Cele i obszar
    var dailyGoal: Int get() = prefs.getInt("daily_goal", 5); set(v) = prefs.putInt("daily_goal", v)
    var weeklyGoal: Int get() = prefs.getInt("weekly_goal", 25); set(v) = prefs.putInt("weekly_goal", v)
    var activeArea: Long get() = prefs.getLong("active_area", -1L); set(v) = prefs.putLong("active_area", v)

    // AI
    var openAiKey: String get() = str("openai_key"); set(v) = prefs.put("openai_key", v.trim())
    var openAiAdminKey: String get() = str("openai_admin_key"); set(v) = prefs.put("openai_admin_key", v.trim())
    var aiPromptTokens: Long get() = prefs.getLong("ai_prompt_tokens", 0); private set(v) = prefs.putLong("ai_prompt_tokens", v)
    var aiCompletionTokens: Long get() = prefs.getLong("ai_completion_tokens", 0); private set(v) = prefs.putLong("ai_completion_tokens", v)
    var aiImageCount: Long get() = prefs.getLong("ai_image_count", 0); private set(v) = prefs.putLong("ai_image_count", v)
    fun addAiUsage(prompt: Int, completion: Int) { aiPromptTokens += prompt; aiCompletionTokens += completion }
    fun addAiImages(n: Int) { aiImageCount += n }
    fun resetAiUsage() { aiPromptTokens = 0; aiCompletionTokens = 0; aiImageCount = 0 }

    // Gmail
    var gmailUser: String get() = str("gmail_user"); set(v) = prefs.put("gmail_user", v.trim())
    var gmailPass: String get() = str("gmail_app_pass"); set(v) = prefs.put("gmail_app_pass", v.trim())
    private var gmailDone: String get() = str("gmail_processed"); set(v) = prefs.put("gmail_processed", v)
    fun gmailProcessed(): Set<String> = gmailDone.split("|").filter { it.isNotBlank() }.toSet()
    fun gmailMarkProcessed(ids: Collection<String>) { gmailDone = (gmailProcessed() + ids).toList().takeLast(500).joinToString("|") }

    // Statystyki użycia
    var deferCount: Long get() = prefs.getLong("defer_count", 0); private set(v) = prefs.putLong("defer_count", v)
    fun addDefer() { deferCount += 1 }
    var openHours: List<Int>
        get() { val parts = str("open_hours").split(","); return if (parts.size == 24) parts.map { it.trim().toIntOrNull() ?: 0 } else List(24) { 0 } }
        private set(v) = prefs.put("open_hours", v.joinToString(","))
    fun recordTaskOpen(hour: Int) { val h = openHours.toMutableList(); h[hour.coerceIn(0, 23)]++; openHours = h }
    private var actualSum: Long get() = prefs.getLong("actual_sum_min", 0); set(v) = prefs.putLong("actual_sum_min", v)
    private var actualCount: Int get() = prefs.getInt("actual_count", 0); set(v) = prefs.putInt("actual_count", v)
    val avgActualMinutes: Int get() = if (actualCount > 0) (actualSum / actualCount).toInt() else 0
    fun markTaskOpened(id: Long) = prefs.putLong("opened_$id", System.currentTimeMillis())
    /** Realny czas od otwarcia do ukończenia (1..240 min) zasila średnią. */
    fun recordActualCompletion(id: Long) {
        val opened = prefs.getLong("opened_$id", 0L); if (opened == 0L) return
        val min = (System.currentTimeMillis() - opened) / 60_000
        if (min in 1..240) { actualSum += min; actualCount += 1 }
        prefs.remove("opened_$id")
    }

    // Tła na dysku
    val backgroundsDir: File get() = File(System.getProperty("user.home"), ".todoisto/backgrounds").apply { mkdirs() }
    fun saveBackgroundBytes(bytes: ByteArray, name: String): String {
        val f = File(backgroundsDir, name); f.writeBytes(bytes); return f.absolutePath
    }
}

/**
 * Powiadomienia macOS (pasek menu) dla przypomnień — także z zadań ustawionych
 * na telefonie. Wszystko w try/catch, żeby nigdy nie wywalić aplikacji.
 */
object DesktopNotifier {
    private var tray: java.awt.TrayIcon? = null

    private fun ensure() {
        if (tray != null || !java.awt.SystemTray.isSupported()) return
        runCatching {
            val t = java.awt.TrayIcon(AppIcon.image(16), "Todoisto").apply { isImageAutoSize = true }
            java.awt.SystemTray.getSystemTray().add(t); tray = t
        }
    }

    fun notify(title: String, text: String) {
        ensure()
        runCatching { tray?.displayMessage(title, text, java.awt.TrayIcon.MessageType.INFO) }
    }
}

/** Wynik synchronizacji do pokazania w UI. */
data class SyncResult(val ok: Boolean, val message: String)

/** Logowanie/rejestracja w chmurze — zapisuje dane w ustawieniach, gdy się uda. */
fun cloudAuth(settings: AppSettings, email: String, password: String, register: Boolean): SyncResult {
    if (!settings.configured) return SyncResult(false, "Najpierw wklej URL i klucz projektu.")
    val r = if (register) SupabaseSync.signUp(settings.url, settings.anonKey, email.trim(), password)
            else SupabaseSync.signIn(settings.url, settings.anonKey, email.trim(), password)
    return if (r.token != null && r.userId != null) {
        settings.email = email; settings.cloudPassword = password
        SyncResult(true, "Zalogowano w chmurze.")
    } else SyncResult(false, r.error ?: "Nie udało się zalogować.")
}

/**
 * Pełna synchronizacja: logowanie → pobierz z chmury → SCAL z lokalnym (nic nie
 * ginie) → wyślij. Ta sama tabela i format co na telefonie.
 */
fun sync(repo: TaskRepository, settings: AppSettings): SyncResult {
    if (!settings.configured) return SyncResult(false, "Uzupełnij URL i klucz anon Supabase.")
    if (!settings.signedIn) return SyncResult(false, "Zaloguj się w chmurze.")
    val auth = SupabaseSync.signIn(settings.url, settings.anonKey, settings.email, settings.cloudPassword)
    if (auth.token == null || auth.userId == null) return SyncResult(false, auth.error ?: "Logowanie nie powiodło się.")
    val remote = SupabaseSync.pull(settings.url, settings.anonKey, auth.token!!, auth.userId!!)
    val merged = if (remote != null) {
        runCatching { SnapshotMerge.merge(repo.snapshot(), CloudBackupCodec.fromJson(remote)) }.getOrDefault(repo.snapshot())
    } else repo.snapshot()
    repo.replaceAll(merged)
    val err = SupabaseSync.push(settings.url, settings.anonKey, auth.token!!, auth.userId!!, CloudBackupCodec.toJson(merged))
    return if (err == null) SyncResult(true, "Zsynchronizowano z chmurą.")
    else SyncResult(false, "Scalone lokalnie, ale wysyłka nie wyszła: $err")
}

/** Tylko wysyłka (jak „Wyślij kopię do chmury" na telefonie). */
fun cloudBackup(repo: TaskRepository, settings: AppSettings): SyncResult {
    if (!settings.signedIn) return SyncResult(false, "Zaloguj się w chmurze.")
    val auth = SupabaseSync.signIn(settings.url, settings.anonKey, settings.email, settings.cloudPassword)
    if (auth.token == null || auth.userId == null) return SyncResult(false, auth.error ?: "Logowanie nie powiodło się.")
    val err = SupabaseSync.push(settings.url, settings.anonKey, auth.token!!, auth.userId!!, CloudBackupCodec.toJson(repo.snapshot()))
    return if (err == null) SyncResult(true, "Wysłano kopię do chmury.") else SyncResult(false, err)
}

/** Tylko pobranie + scalenie (jak „Pobierz z chmury"). */
fun cloudRestore(repo: TaskRepository, settings: AppSettings): SyncResult {
    if (!settings.signedIn) return SyncResult(false, "Zaloguj się w chmurze.")
    val auth = SupabaseSync.signIn(settings.url, settings.anonKey, settings.email, settings.cloudPassword)
    if (auth.token == null || auth.userId == null) return SyncResult(false, auth.error ?: "Logowanie nie powiodło się.")
    val remote = SupabaseSync.pull(settings.url, settings.anonKey, auth.token!!, auth.userId!!)
        ?: return SyncResult(false, "Brak kopii w chmurze albo błąd pobierania.")
    val merged = runCatching { SnapshotMerge.merge(repo.snapshot(), CloudBackupCodec.fromJson(remote)) }
        .getOrElse { return SyncResult(false, "Nie udało się odczytać kopii.") }
    repo.replaceAll(merged)
    return SyncResult(true, "Pobrano z chmury (${merged.tasks.count { !it.deleted && it.parentId == null }} zadań).")
}

/** Gmail: maile z gwiazdką → zadania (IMAP, hasło do aplikacji). */
suspend fun fetchGmail(repo: TaskRepository, settings: AppSettings): SyncResult {
    val user = settings.gmailUser; val pass = settings.gmailPass
    if (user.isBlank() || pass.isBlank()) return SyncResult(false, "Najpierw zapisz e-mail i hasło do aplikacji.")
    return try {
        val mails = pl.media30.todoisto.data.GmailClient.fetchStarred(user, pass)
        val done = settings.gmailProcessed()
        val fresh = mails.filter { it.messageId !in done }
        fresh.forEach { m ->
            repo.add(CloudTask(id = 0, title = m.subject.take(200), notes = "✉️ Od: ${m.from}", attachments = listOf(m.gmailLink)))
        }
        settings.gmailMarkProcessed(fresh.map { it.messageId })
        SyncResult(true, if (fresh.isEmpty()) "Brak nowych maili z gwiazdką." else "Dodano ${fresh.size} zadań z Gmaila ⭐")
    } catch (e: Exception) {
        SyncResult(false, pl.media30.todoisto.data.GmailClient.friendlyError(e))
    }
}

/** Import aktywności z opublikowanego arkusza Google (CSV). */
fun importActivitiesFromCsv(repo: TaskRepository, url: String): String {
    if (url.isBlank()) return "Wklej link do arkusza."
    return try {
        val target = pl.media30.todoisto.data.ActivityCsvParser.normalizeSheetUrl(url)
        val conn = (java.net.URL(target).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 12000; readTimeout = 12000; instanceFollowRedirects = true
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val acts = pl.media30.todoisto.data.ActivityCsvParser.parse(text)
        acts.forEach { repo.addActivity(it) }
        if (acts.isEmpty()) "Nie znaleziono aktywności w arkuszu" else "Zaimportowano ${acts.size} aktywności"
    } catch (e: Exception) {
        "Błąd importu: ${e.message ?: "sprawdź link i połączenie"}"
    }
}

/** Tekstowy plan dnia (do skopiowania / wysłania). */
fun buildDayPlanText(repo: TaskRepository): String {
    val today = LocalDate.now(); val todayEpoch = today.toEpochDay()
    val open = repo.tasks.filter { it.parentId == null && !it.isCompleted && it.dueDate != null && it.dueDate!! <= todayEpoch }
        .sortedWith(compareBy({ it.dueTimeMinutes ?: 9999 }, { it.priority }))
    val fmt = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.forLanguageTag("pl"))
    val sb = StringBuilder("Plan dnia — ${today.format(fmt)}\n\n")
    if (open.isEmpty()) sb.append("Brak zaplanowanych zadań na dziś.\n")
    else open.forEach { t ->
        val time = t.dueTimeMinutes?.let { "%d:%02d ".format(it / 60, it % 60) } ?: ""
        val dur = t.durationMinutes?.let { " (${it} min)" } ?: ""
        val proj = t.projectId?.let { repo.project(it)?.name }?.let { " #$it" } ?: ""
        val prio = if (t.priority != "P4") " ${t.priority}" else ""
        val overdue = if (t.dueDate!! < todayEpoch) " [zaległe]" else ""
        sb.append("• ${time}${t.title}${dur}${proj}${prio}${overdue}\n")
    }
    val est = open.sumOf { it.durationMinutes ?: 20 }
    if (est > 0) sb.append("\nSzacowany czas: ${est / 60}h ${est % 60}min\n")
    sb.append("\n— wygenerowano w Todoisto")
    return sb.toString()
}
