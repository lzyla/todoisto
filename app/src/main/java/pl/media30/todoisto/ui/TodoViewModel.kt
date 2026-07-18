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
    val noWindows: Boolean,
    /** Bieżąca pogoda (Open-Meteo) — wpływa na dobór aktywności dom/zewnątrz. */
    val weather: pl.media30.todoisto.data.WeatherNow? = null
)

/** Stan zapytania do AI (przycisk „Zapytaj AI"). */
data class AiAskState(
    val loading: Boolean = false,
    val answer: String? = null,
    val error: String? = null,
    val needsKey: Boolean = false
)

/** Generowanie propozycji tła przez AI (OpenAI Images). */
data class AiImagesState(
    val loading: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
    val needsKey: Boolean = false
)

/** Realny koszt z OpenAI Costs API (klucz Admin). */
data class AiCostState(
    val loading: Boolean = false,
    val amountUsd: Double? = null,
    val error: String? = null
)

enum class SortMode(val label: String, val hint: String) {
    SMART("Sprytne", "Termin, priorytet i kontekst razem"),
    PRIORITY("Priorytet", "Od P1 do P4, potem najbliższy termin"),
    DATE("Data", "Najbliższy termin na górze"),
    ALPHA("Alfabetycznie", "Od A do Z"),
    NEWEST("Najnowsze", "Ostatnio dodane na górze")
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
    private val settings: SettingsStore,
    private val cloud: pl.media30.todoisto.data.CloudStore? = null
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

    // --- Własne tła (zdjęcia / AI) ---
    val customPhotos: StateFlow<List<String>> = settings.customPhotos
    val activeCustomBg: StateFlow<String> = settings.activeCustomBg
    val phaseBackgrounds: StateFlow<List<String>> = settings.phaseBackgrounds
    val usePhaseBg: StateFlow<Boolean> = settings.usePhaseBg
    fun setUsePhaseBg(value: Boolean) = settings.setUsePhaseBg(value)
    fun setCustomPhotos(paths: List<String>) = settings.setCustomPhotos(paths)
    /** Import wgranego zdjęcia z galerii (Uri) — dekodowanie/zapis w tle. */
    fun addCustomPhotoFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val path = withContext(kotlinx.coroutines.Dispatchers.IO) {
                settings.importBackgroundFromUri(uri)
            } ?: return@launch
            val list = (settings.customPhotos.value + path).takeLast(3)
            settings.setCustomPhotos(list)
            // Od razu ustaw wgrane zdjęcie jako aktywne tło — widoczny efekt bez
            // dodatkowego dotykania kafelka.
            settings.setActiveCustomBg(path)
            settings.setUsePhaseBg(false)
            settings.setPhotoBackground(false)
        }
    }

    /** Zmienia kolejność własnych zdjęć (drag and drop kafelków). */
    fun reorderCustomPhotos(paths: List<String>) = settings.setCustomPhotos(paths)
    fun setActiveCustomBg(path: String) {
        settings.setActiveCustomBg(path)
        if (path.isNotBlank()) { settings.setPhotoBackground(false); settings.setUsePhaseBg(false) }
    }
    fun removeCustomPhoto(path: String) {
        settings.setCustomPhotos(settings.customPhotos.value.filterNot { it == path })
        if (settings.activeCustomBg.value == path) settings.setActiveCustomBg("")
    }

    private val _aiImages = MutableStateFlow<AiImagesState?>(null)
    val aiImages: StateFlow<AiImagesState?> = _aiImages.asStateFlow()
    fun dismissAiImages() { _aiImages.value = null }

    /** Realistyczne tła wg pory dnia (AI) — 3 zdjęcia, przełączane wg zegara. */
    fun generateAiPhaseBackgrounds() {
        val key = settings.openAiKey.value
        if (key.isBlank()) { _aiImages.value = AiImagesState(needsKey = true); return }
        _aiImages.value = AiImagesState(loading = true)
        viewModelScope.launch {
            _aiImages.value = try {
                val imgs = pl.media30.todoisto.data.AiClient.generatePhaseBackgrounds(key)
                val paths = imgs.mapIndexed { i, bytes ->
                    settings.saveBackgroundBytes(bytes, "phase_${System.currentTimeMillis()}_$i.png")
                }
                settings.addAiImages(imgs.size)
                settings.setPhaseBackgrounds(paths)
                settings.setUsePhaseBg(true)
                settings.setActiveCustomBg("")
                settings.setPhotoBackground(false)
                AiImagesState(loading = false, done = true)
            } catch (e: Exception) {
                AiImagesState(loading = false, error = friendlyImageError(e.message))
            }
        }
    }

    // --- Inteligentny skan zdjęcia (lista LUB obiekt, np. książka) ---
    data class ScanState(
        val loading: Boolean = false,
        val summary: String = "",
        val tasks: List<String> = emptyList(),
        val suggestion: String = "",
        val plan: String = "",
        val error: String? = null,
        val needsKey: Boolean = false
    )
    private val _scan = MutableStateFlow<ScanState?>(null)
    val scan: StateFlow<ScanState?> = _scan.asStateFlow()
    fun dismissScan() { _scan.value = null }

    /** Analizuje zdjęcie i pokazuje propozycję (nie dodaje od razu). */
    fun scanNoteImage(jpegBytes: ByteArray) {
        val key = settings.openAiKey.value
        if (key.isBlank()) { _scan.value = ScanState(needsKey = true); return }
        _scan.value = ScanState(loading = true)
        viewModelScope.launch {
            _scan.value = try {
                val r = pl.media30.todoisto.data.AiClient.analyzeImageSmart(key, jpegBytes)
                ScanState(summary = r.summary, tasks = r.tasks, suggestion = r.suggestion, plan = r.plan)
            } catch (e: Exception) {
                ScanState(error = e.message ?: "Błąd skanowania")
            }
        }
    }

    /** Dodaje zadania rozpoznane z listy. */
    fun addScanTasks(titles: List<String>) {
        titles.forEach { quickAdd(it) }
        _scan.value = null
    }

    /** Dodaje jedno proponowane zadanie (np. „Przeczytać książkę…"), plan → notatka. */
    fun addScanSuggestion(title: String, plan: String) {
        if (title.isBlank()) { _scan.value = null; return }
        viewModelScope.launch {
            repository.insert(pl.media30.todoisto.data.Task(title = title.trim(), notes = plan.trim(), createdAt = System.currentTimeMillis()))
            _scan.value = null
        }
    }

    // --- Chmura (Supabase) ---
    data class CloudState(
        val configured: Boolean = false,
        val signedIn: Boolean = false,
        val email: String = "",
        val busy: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
    private fun cloudSnapshot(busy: Boolean = false, message: String? = null, error: String? = null) = CloudState(
        configured = cloud?.configured == true,
        signedIn = cloud?.signedIn == true,
        email = cloud?.cloudEmail?.value.orEmpty(),
        busy = busy, message = message, error = error
    )
    private val _cloud = MutableStateFlow(cloudSnapshot())
    val cloudState: StateFlow<CloudState> = _cloud.asStateFlow()

    fun setCloudConfig(url: String, anonKey: String) {
        cloud?.setConfig(url, anonKey)
        _cloud.value = cloudSnapshot(message = "Zapisano konfigurację projektu.")
    }

    fun cloudSignUp(email: String, password: String) = cloudAuth(email, password, register = true)
    fun cloudSignIn(email: String, password: String) = cloudAuth(email, password, register = false)

    private fun cloudAuth(email: String, password: String, register: Boolean) {
        val c = cloud ?: return
        if (!c.configured) { _cloud.value = cloudSnapshot(error = "Najpierw wklej URL i klucz projektu."); return }
        _cloud.value = cloudSnapshot(busy = true)
        viewModelScope.launch {
            val r = if (register) pl.media30.todoisto.data.SupabaseClient.signUp(c.url0, c.anonKey0, email, password)
                    else pl.media30.todoisto.data.SupabaseClient.signIn(c.url0, c.anonKey0, email, password)
            when {
                r.token != null && r.userId != null -> {
                    c.setSession(r.email ?: email, r.token, r.userId)
                    _cloud.value = cloudSnapshot(message = "Zalogowano w chmurze.")
                }
                r.error != null -> _cloud.value = cloudSnapshot(error = r.error)
                else -> _cloud.value = cloudSnapshot(error = "Nie udało się zalogować.")
            }
        }
    }

    fun cloudSignOut() {
        cloud?.signOut()
        _cloud.value = cloudSnapshot(message = "Wylogowano z chmury.")
    }

    /** Wysyła kopię zadań do chmury. */
    fun cloudBackup() {
        val c = cloud ?: return
        if (!c.signedIn) { _cloud.value = cloudSnapshot(error = "Zaloguj się w chmurze."); return }
        _cloud.value = cloudSnapshot(busy = true)
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            val err = pl.media30.todoisto.data.SupabaseClient.pushBackup(c.url0, c.anonKey0, c.token0, c.userId, json)
            _cloud.value = if (err == null) cloudSnapshot(message = "Wysłano kopię do chmury.")
                           else cloudSnapshot(error = err)
        }
    }

    /** Pobiera kopię z chmury i wczytuje ją lokalnie. */
    fun cloudRestore() {
        val c = cloud ?: return
        if (!c.signedIn) { _cloud.value = cloudSnapshot(error = "Zaloguj się w chmurze."); return }
        _cloud.value = cloudSnapshot(busy = true)
        viewModelScope.launch {
            val json = pl.media30.todoisto.data.SupabaseClient.pullBackup(c.url0, c.anonKey0, c.token0, c.userId)
            if (json == null) { _cloud.value = cloudSnapshot(error = "Brak kopii w chmurze albo błąd pobierania."); return@launch }
            val count = runCatching { repository.importBackupJson(json) }.getOrDefault(0)
            _cloud.value = cloudSnapshot(message = "Pobrano z chmury ($count zadań).")
        }
    }

    fun clearCloudMessage() { _cloud.value = cloudSnapshot() }

    // --- Szacowanie czasu przez AI (porównanie z sumą zadań) ---
    data class EstimateAiState(val loading: Boolean = false, val minutes: Int? = null, val error: String? = null, val needsKey: Boolean = false)
    private val _estimateAi = MutableStateFlow(EstimateAiState())
    val estimateAi: StateFlow<EstimateAiState> = _estimateAi.asStateFlow()
    fun clearEstimateAi() { _estimateAi.value = EstimateAiState() }

    fun estimateTodayWithAi(titles: List<String>) {
        val key = settings.openAiKey.value
        if (key.isBlank()) { _estimateAi.value = EstimateAiState(needsKey = true); return }
        if (titles.isEmpty()) { _estimateAi.value = EstimateAiState(minutes = 0); return }
        _estimateAi.value = EstimateAiState(loading = true)
        viewModelScope.launch {
            _estimateAi.value = try {
                val r = pl.media30.todoisto.data.AiClient.estimateMinutes(key, titles, settings.avgActualMinutes.value)
                settings.addAiUsage(r.promptTokens, r.completionTokens)
                EstimateAiState(minutes = r.minutes)
            } catch (e: Exception) {
                EstimateAiState(error = e.message ?: "Błąd połączenia")
            }
        }
    }

    // --- Gmail → zadania (maile z gwiazdką) ---
    data class GmailState(val loading: Boolean = false, val info: String? = null, val error: String? = null)
    private val _gmail = MutableStateFlow(GmailState())
    val gmailState: StateFlow<GmailState> = _gmail.asStateFlow()
    val gmailUser: StateFlow<String> = settings.gmailUser
    fun setGmailCreds(user: String, pass: String) {
        settings.setGmailCreds(user, pass)
        _gmail.value = GmailState(info = if (user.isBlank()) "Rozłączono z Gmailem." else "Zapisano dane Gmaila.")
    }

    /** Pobiera maile z gwiazdką i zamienia nowe na zadania w Skrzynce. */
    fun syncGmail() {
        val user = settings.gmailUser.value; val pass = settings.gmailPass.value
        if (user.isBlank() || pass.isBlank()) { _gmail.value = GmailState(error = "Najpierw zapisz e-mail i hasło do aplikacji."); return }
        _gmail.value = GmailState(loading = true)
        viewModelScope.launch {
            _gmail.value = try {
                val mails = pl.media30.todoisto.data.GmailClient.fetchStarred(user, pass)
                val done = settings.gmailProcessed()
                val fresh = mails.filter { it.messageId !in done }
                fresh.forEach { m ->
                    repository.insert(pl.media30.todoisto.data.Task(
                        title = m.subject.take(200),
                        notes = "✉️ Od: ${m.from}",
                        attachments = listOf(m.gmailLink),
                        createdAt = System.currentTimeMillis()
                    ))
                }
                settings.gmailMarkProcessed(fresh.map { it.messageId })
                GmailState(info = if (fresh.isEmpty()) "Brak nowych maili z gwiazdką." else "Dodano ${fresh.size} zadań z Gmaila ⭐")
            } catch (e: Exception) {
                GmailState(error = pl.media30.todoisto.data.GmailClient.friendlyError(e))
            }
        }
    }

    private val _bgBusy = MutableStateFlow(false)
    val bgBusy: StateFlow<Boolean> = _bgBusy.asStateFlow()

    /**
     * Losuje 3 tła — prawdziwe zdjęcia (Lorem Picsum) z nałożoną kolorową maską
     * w barwach apki. Każde kliknięcie daje inny zestaw i od razu ustawia
     * pierwsze jako aktywne tło. Gdy brak internetu / błąd — fallback do
     * lokalnego gradientu, żeby zawsze coś się wydarzyło.
     */
    fun addPresetBackgrounds() {
        if (_bgBusy.value) return
        viewModelScope.launch {
            _bgBusy.value = true
            val paths = withContext(kotlinx.coroutines.Dispatchers.IO) {
                (0 until 3).map { i ->
                    runCatching {
                        val seed = kotlin.random.Random.nextInt(1, 1_000_000)
                        val photo = pl.media30.todoisto.data.PhotoBackgrounds.fetchRandomPhoto(1024, 1536, seed)
                        val maskIdx = kotlin.random.Random.nextInt(pl.media30.todoisto.data.PhotoBackgrounds.maskCount)
                        val masked = pl.media30.todoisto.data.PhotoBackgrounds.withColorMask(photo, maskIdx)
                        settings.saveBackgroundBytes(masked, "photo_${System.currentTimeMillis()}_$i.jpg")
                    }.getOrElse {
                        // offline / błąd → lokalny gradient (bez sieci)
                        val colors = pl.media30.todoisto.data.PresetBackgrounds.randomPalettes(1).first()
                        val bytes = pl.media30.todoisto.data.PresetBackgrounds.gradientPng(1080, 1920, colors)
                        settings.saveBackgroundBytes(bytes, "preset_${System.currentTimeMillis()}_$i.png")
                    }
                }
            }
            settings.setCustomPhotos(paths)
            // Od razu ustaw pierwsze jako aktywne tło (widoczny efekt losowania).
            paths.firstOrNull()?.let {
                settings.setActiveCustomBg(it)
                settings.setUsePhaseBg(false)
                settings.setPhotoBackground(false)
            }
            _bgBusy.value = false
        }
    }

    /** Zamienia surowy błąd OpenAI o brakujących uprawnieniach na czytelną podpowiedź. */
    private fun friendlyImageError(msg: String?): String {
        val m = msg ?: "Błąd generowania"
        val lower = m.lowercase()
        return if (lower.contains("scope") || lower.contains("permission") || lower.contains("images.request")) {
            "Twój klucz OpenAI nie ma uprawnień do generowania obrazów. Użyj klucza bez ograniczeń albo z dostępem do Images (rola Writer/Owner). Na razie działa opcja Wstaw 3 gotowe tła bez AI oraz wgrywanie własnych zdjęć."
        } else m
    }

    /** Generuje 3 propozycje tła przez AI i zapisuje je jako własne zdjęcia. */
    fun generateAiBackgrounds() {
        val key = settings.openAiKey.value
        if (key.isBlank()) { _aiImages.value = AiImagesState(needsKey = true); return }
        _aiImages.value = AiImagesState(loading = true)
        viewModelScope.launch {
            _aiImages.value = try {
                val imgs = pl.media30.todoisto.data.AiClient.generateBackgrounds(key, 3)
                val paths = imgs.mapIndexed { i, bytes ->
                    settings.saveBackgroundBytes(bytes, "ai_${System.currentTimeMillis()}_$i.png")
                }
                settings.addAiImages(imgs.size)
                settings.setCustomPhotos(paths)
                AiImagesState(loading = false, done = true)
            } catch (e: Exception) {
                AiImagesState(loading = false, error = friendlyImageError(e.message))
            }
        }
    }

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
                val result = pl.media30.todoisto.data.AiClient.ask(key, prompt)
                settings.addAiUsage(result.promptTokens, result.completionTokens)
                AiAskState(loading = false, answer = result.text)
            } catch (e: Exception) {
                AiAskState(loading = false, error = e.message ?: "Błąd połączenia")
            }
        }
    }

    // --- Zużycie AI (tokeny + koszt) ---
    val aiPromptTokens: StateFlow<Long> = settings.aiPromptTokens
    val aiCompletionTokens: StateFlow<Long> = settings.aiCompletionTokens
    val aiImageCount: StateFlow<Long> = settings.aiImageCount
    fun resetAiUsage() = settings.resetAiUsage()

    // --- Realny koszt z OpenAI (klucz Admin) ---
    val openAiAdminKey: StateFlow<String> = settings.openAiAdminKey
    fun setOpenAiAdminKey(value: String) {
        settings.setOpenAiAdminKey(value)
        if (value.isBlank()) _aiCost.value = null else refreshAiCost()
    }

    private val _aiCost = MutableStateFlow<AiCostState?>(null)
    val aiCost: StateFlow<AiCostState?> = _aiCost.asStateFlow()

    /** Pobiera realny koszt tego miesiąca z Costs API (kubełki dzienne od 1. dnia). */
    fun refreshAiCost() {
        val adminKey = settings.openAiAdminKey.value
        if (adminKey.isBlank()) { _aiCost.value = null; return }
        val startOfMonth = LocalDate.now().withDayOfMonth(1)
            .atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond()
        _aiCost.value = AiCostState(loading = true)
        viewModelScope.launch {
            _aiCost.value = try {
                AiCostState(loading = false, amountUsd = pl.media30.todoisto.data.AiClient.fetchCostsUsd(adminKey, startOfMonth))
            } catch (e: Exception) {
                AiCostState(loading = false, error = e.message ?: "Błąd połączenia")
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

    /** Ręczna kolejność (drag&drop): zapisz nowy porządek zadań po id. */
    fun reorderTasks(orderedIds: List<Long>) = viewModelScope.launch { repository.reorderTasks(orderedIds) }

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
                AppView.Inbox -> !t.isCompleted && t.parentId == null && !inArchived(t)
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
        viewModelScope.launch {
            repository.update(task.copy(title = task.title.trim(), notes = task.notes.trim()))
            // Przypomnienie czasowe: ustaw/odwołaj dokładny alarm systemowy.
            val at = task.reminderAt
            if (at != null && !task.isCompleted) pl.media30.todoisto.data.Reminders.schedule(settings.appCtx, task.id, task.title, at)
            else pl.media30.todoisto.data.Reminders.cancel(settings.appCtx, task.id)
        }
    }

    fun addSubtask(parentId: Long, projectId: Long?, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                Task(title = title.trim(), parentId = parentId, projectId = projectId, createdAt = System.currentTimeMillis())
            )
        }
    }

    fun toggleCompleted(task: Task) = viewModelScope.launch {
        if (!task.isCompleted) {
            settings.recordActualCompletion(task.id) // liczymy realny czas przy ukończeniu
            pl.media30.todoisto.data.Reminders.cancel(settings.appCtx, task.id) // alarm już niepotrzebny
        }
        repository.toggleCompleted(task)
    }
    fun markTaskOpened(id: Long) = settings.markTaskOpened(id)
    val avgActualMinutes: StateFlow<Int> = settings.avgActualMinutes

    // --- Statystyki ---
    val deferCount: StateFlow<Long> = settings.deferCount
    val openHours: StateFlow<List<Int>> = settings.openHours
    fun recordTaskOpen() = settings.recordTaskOpen(java.time.LocalTime.now().hour)

    /** Przełożenie zaległego (lub dowolnego) zadania na wskazany dzień (epochDay). */
    fun rescheduleTask(task: Task, epochDay: Long) = viewModelScope.launch {
        settings.addDefer()
        repository.update(task.copy(dueDate = epochDay))
    }

    /** Swipe w lewo: odłóż zadanie na jutro (zachowuje godzinę). */
    fun deferToTomorrow(task: Task) = viewModelScope.launch {
        settings.addDefer()
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

    private var lastWeather: pl.media30.todoisto.data.WeatherNow? = null

    fun suggestFreeTime() = viewModelScope.launch {
        dismissedActivityIds.clear()
        // Pogoda (Open-Meteo, bez klucza) z ostatniej znanej lokalizacji — jeśli brak
        // zgody/sieci, planner działa jak dotąd.
        lastWeather = runCatching {
            pl.media30.todoisto.data.LocationHelper.lastKnown(settings.appCtx)?.let { (lat, lon) ->
                pl.media30.todoisto.data.WeatherClient.fetch(lat, lon)
            }
        }.getOrNull()
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
        val ctx = currentDayContext().copy(weatherBad = lastWeather?.isBad)
        val suggestions = pl.media30.todoisto.data.ActivityPlanner.plan(acts, ctx, slots, count = 1)
        return FreeTimeState(
            freeMinutes = slots.sumOf { it.length },
            suggestions = suggestions,
            poolEmpty = activities.value.none { it.isActive },
            noWindows = slots.isEmpty(),
            weather = lastWeather
        )
    }

    class Factory(
        private val repository: TaskRepository,
        private val settings: SettingsStore,
        private val cloud: pl.media30.todoisto.data.CloudStore? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
                return TodoViewModel(repository, settings, cloud) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
