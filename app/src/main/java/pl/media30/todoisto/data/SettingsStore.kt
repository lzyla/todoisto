package pl.media30.todoisto.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Simple persisted app settings (theme, productivity goals). */
class SettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("todoisto_settings", Context.MODE_PRIVATE)

    /** Zapisuje bajty tła do wewnętrznego magazynu i zwraca ścieżkę pliku. */
    fun saveBackgroundBytes(bytes: ByteArray, name: String): String {
        val dir = java.io.File(appContext.filesDir, "backgrounds").apply { mkdirs() }
        val f = java.io.File(dir, name)
        f.writeBytes(bytes)
        return f.absolutePath
    }

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK, false))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _dailyGoal = MutableStateFlow(prefs.getInt(KEY_DAILY, 5))
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    private val _weeklyGoal = MutableStateFlow(prefs.getInt(KEY_WEEKLY, 25))
    val weeklyGoal: StateFlow<Int> = _weeklyGoal.asStateFlow()

    private val _photoBackground = MutableStateFlow(prefs.getBoolean(KEY_PHOTO_BG, false))
    val photoBackground: StateFlow<Boolean> = _photoBackground.asStateFlow()

    // Aktywny obszar: -1 = „Wszystko".
    private val _activeArea = MutableStateFlow(prefs.getLong(KEY_AREA, -1L))
    val activeArea: StateFlow<Long> = _activeArea.asStateFlow()
    fun setActiveArea(id: Long) {
        prefs.edit().putLong(KEY_AREA, id).apply()
        _activeArea.value = id
    }

    fun setDarkTheme(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, value).apply()
        _darkTheme.value = value
    }

    fun setPhotoBackground(value: Boolean) {
        prefs.edit().putBoolean(KEY_PHOTO_BG, value).apply()
        _photoBackground.value = value
    }

    fun isDemoSeeded(): Boolean = prefs.getBoolean(KEY_SEEDED, false)
    fun markDemoSeeded() { prefs.edit().putBoolean(KEY_SEEDED, true).apply() }

    // Klucz OpenAI — przechowywany lokalnie na urządzeniu (nigdy w repo).
    private val _openAiKey = MutableStateFlow(prefs.getString(KEY_OPENAI, "").orEmpty())
    val openAiKey: StateFlow<String> = _openAiKey.asStateFlow()
    fun setOpenAiKey(value: String) {
        val v = value.trim()
        prefs.edit().putString(KEY_OPENAI, v).apply()
        _openAiKey.value = v
    }

    // Klucz ADMIN (sk-admin-…) — do realnego kosztu z Costs API. Tylko lokalnie.
    private val _openAiAdminKey = MutableStateFlow(prefs.getString(KEY_OPENAI_ADMIN, "").orEmpty())
    val openAiAdminKey: StateFlow<String> = _openAiAdminKey.asStateFlow()
    fun setOpenAiAdminKey(value: String) {
        val v = value.trim()
        prefs.edit().putString(KEY_OPENAI_ADMIN, v).apply()
        _openAiAdminKey.value = v
    }

    // --- Statystyki: przesunięcia + histogram godzin otwierania zadań ---
    private val _deferCount = MutableStateFlow(prefs.getLong(KEY_DEFERS, 0L))
    val deferCount: StateFlow<Long> = _deferCount.asStateFlow()
    fun addDefer() {
        val v = _deferCount.value + 1
        prefs.edit().putLong(KEY_DEFERS, v).apply()
        _deferCount.value = v
    }

    private fun readHours(): List<Int> {
        val raw = prefs.getString(KEY_OPEN_HOURS, "").orEmpty()
        val parts = raw.split(",").mapNotNull { it.toIntOrNull() }
        return if (parts.size == 24) parts else List(24) { 0 }
    }
    private val _openHours = MutableStateFlow(readHours())
    val openHours: StateFlow<List<Int>> = _openHours.asStateFlow()
    fun recordTaskOpen(hour: Int) {
        val h = hour.coerceIn(0, 23)
        val list = _openHours.value.toMutableList()
        list[h] = list[h] + 1
        prefs.edit().putString(KEY_OPEN_HOURS, list.joinToString(",")).apply()
        _openHours.value = list
    }

    fun setGoals(daily: Int, weekly: Int) {
        prefs.edit().putInt(KEY_DAILY, daily).putInt(KEY_WEEKLY, weekly).apply()
        _dailyGoal.value = daily
        _weeklyGoal.value = weekly
    }

    // --- Własne tła (zdjęcia) ---
    private fun readPhotos(): List<String> =
        prefs.getString(KEY_CUSTOM_PHOTOS, "").orEmpty().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    private val _customPhotos = MutableStateFlow(readPhotos())
    val customPhotos: StateFlow<List<String>> = _customPhotos.asStateFlow()
    fun setCustomPhotos(paths: List<String>) {
        val v = paths.take(3)
        prefs.edit().putString(KEY_CUSTOM_PHOTOS, v.joinToString("\n")).apply()
        _customPhotos.value = v
    }

    /** Aktywne własne tło (ścieżka pliku); pusty = brak (gradient/scena). */
    private val _activeCustomBg = MutableStateFlow(prefs.getString(KEY_ACTIVE_CUSTOM, "").orEmpty())
    val activeCustomBg: StateFlow<String> = _activeCustomBg.asStateFlow()
    fun setActiveCustomBg(path: String) {
        prefs.edit().putString(KEY_ACTIVE_CUSTOM, path).apply()
        _activeCustomBg.value = path
    }

    /** Tła wg pory dnia (3 ścieżki: rano/dzień/wieczór) + flaga włączenia. */
    private val _phaseBackgrounds = MutableStateFlow(
        prefs.getString(KEY_PHASE_BG, "").orEmpty().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    )
    val phaseBackgrounds: StateFlow<List<String>> = _phaseBackgrounds.asStateFlow()
    private val _usePhaseBg = MutableStateFlow(prefs.getBoolean(KEY_USE_PHASE_BG, false))
    val usePhaseBg: StateFlow<Boolean> = _usePhaseBg.asStateFlow()
    fun setPhaseBackgrounds(paths: List<String>) {
        val v = paths.take(3)
        prefs.edit().putString(KEY_PHASE_BG, v.joinToString("\n")).apply()
        _phaseBackgrounds.value = v
    }
    fun setUsePhaseBg(value: Boolean) {
        prefs.edit().putBoolean(KEY_USE_PHASE_BG, value).apply()
        _usePhaseBg.value = value
    }

    // --- Ustawienia „Ogólne" ---

    /** Widok główny na starcie: "today" lub "upcoming". */
    private val _startView = MutableStateFlow(prefs.getString(KEY_START_VIEW, "today").orEmpty().ifBlank { "today" })
    val startView: StateFlow<String> = _startView.asStateFlow()
    fun setStartView(value: String) {
        prefs.edit().putString(KEY_START_VIEW, value).apply()
        _startView.value = value
    }

    /** Rozpoznawanie dat w szybkim dodawaniu. */
    private val _dateRecognition = MutableStateFlow(prefs.getBoolean(KEY_DATE_REC, true))
    val dateRecognition: StateFlow<Boolean> = _dateRecognition.asStateFlow()
    fun setDateRecognition(value: Boolean) {
        prefs.edit().putBoolean(KEY_DATE_REC, value).apply()
        _dateRecognition.value = value
    }

    /** Początek tygodnia: true = poniedziałek, false = niedziela. */
    private val _weekStartMonday = MutableStateFlow(prefs.getBoolean(KEY_WEEK_MON, true))
    val weekStartMonday: StateFlow<Boolean> = _weekStartMonday.asStateFlow()
    fun setWeekStartMonday(value: Boolean) {
        prefs.edit().putBoolean(KEY_WEEK_MON, value).apply()
        _weekStartMonday.value = value
    }

    /** Dźwięk przy ukończeniu zadania. */
    private val _completionSound = MutableStateFlow(prefs.getBoolean(KEY_DONE_SOUND, false))
    val completionSound: StateFlow<Boolean> = _completionSound.asStateFlow()
    fun setCompletionSound(value: Boolean) {
        prefs.edit().putBoolean(KEY_DONE_SOUND, value).apply()
        _completionSound.value = value
    }

    /** Przesunięcie w prawo = ukończ (true) lub odłóż (false); lewe robi to drugie. */
    private val _swipeRightCompletes = MutableStateFlow(prefs.getBoolean(KEY_SWIPE_RIGHT, true))
    val swipeRightCompletes: StateFlow<Boolean> = _swipeRightCompletes.asStateFlow()
    fun setSwipeRightCompletes(value: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_RIGHT, value).apply()
        _swipeRightCompletes.value = value
    }

    // --- Zużycie AI (skumulowane tokeny do liczenia kosztu) ---
    private val _aiPromptTokens = MutableStateFlow(prefs.getLong(KEY_AI_IN, 0L))
    val aiPromptTokens: StateFlow<Long> = _aiPromptTokens.asStateFlow()
    private val _aiCompletionTokens = MutableStateFlow(prefs.getLong(KEY_AI_OUT, 0L))
    val aiCompletionTokens: StateFlow<Long> = _aiCompletionTokens.asStateFlow()

    private val _aiImageCount = MutableStateFlow(prefs.getLong(KEY_AI_IMAGES, 0L))
    val aiImageCount: StateFlow<Long> = _aiImageCount.asStateFlow()

    fun addAiUsage(promptTokens: Int, completionTokens: Int) {
        val newIn = _aiPromptTokens.value + promptTokens
        val newOut = _aiCompletionTokens.value + completionTokens
        prefs.edit().putLong(KEY_AI_IN, newIn).putLong(KEY_AI_OUT, newOut).apply()
        _aiPromptTokens.value = newIn
        _aiCompletionTokens.value = newOut
    }

    fun addAiImages(count: Int) {
        val v = _aiImageCount.value + count
        prefs.edit().putLong(KEY_AI_IMAGES, v).apply()
        _aiImageCount.value = v
    }

    fun resetAiUsage() {
        prefs.edit().putLong(KEY_AI_IN, 0L).putLong(KEY_AI_OUT, 0L).putLong(KEY_AI_IMAGES, 0L).apply()
        _aiPromptTokens.value = 0L
        _aiCompletionTokens.value = 0L
        _aiImageCount.value = 0L
    }

    private companion object {
        const val KEY_DARK = "dark_theme"
        const val KEY_AI_IN = "ai_prompt_tokens"
        const val KEY_AI_OUT = "ai_completion_tokens"
        const val KEY_AI_IMAGES = "ai_image_count"
        const val KEY_DEFERS = "defer_count"
        const val KEY_OPEN_HOURS = "open_hours"
        const val KEY_DAILY = "daily_goal"
        const val KEY_WEEKLY = "weekly_goal"
        const val KEY_PHOTO_BG = "photo_background"
        const val KEY_START_VIEW = "start_view"
        const val KEY_DATE_REC = "date_recognition"
        const val KEY_WEEK_MON = "week_start_monday"
        const val KEY_DONE_SOUND = "completion_sound"
        const val KEY_SWIPE_RIGHT = "swipe_right_completes"
        // Wersjonowany klucz — bump wymusza jednorazowe ponowne zasianie u wszystkich
        // (v4: po dodaniu Obszarów baza jest przebudowywana, więc dosiewamy z obszarami).
        const val KEY_SEEDED = "demo_seeded_v4"
        const val KEY_AREA = "active_area"
        const val KEY_OPENAI = "openai_key"
        const val KEY_OPENAI_ADMIN = "openai_admin_key"
        const val KEY_CUSTOM_PHOTOS = "custom_photos"
        const val KEY_ACTIVE_CUSTOM = "active_custom_bg"
        const val KEY_PHASE_BG = "phase_backgrounds"
        const val KEY_USE_PHASE_BG = "use_phase_bg"
    }
}
