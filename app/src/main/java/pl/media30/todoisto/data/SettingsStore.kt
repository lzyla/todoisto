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

    /**
     * Importuje zdjęcie z [uri] (np. z galerii): dekoduje, skaluje i zapisuje
     * jako własny plik JPEG. Zwraca ścieżkę albo null przy błędzie. Dzięki temu
     * upload nie zależy od surowego kopiowania strumienia (które bywa zawodne).
     */
    fun importBackgroundFromUri(uri: android.net.Uri): String? {
        // Najpierw dekodowanie + skalowanie; gdy się nie uda — surowa kopia strumienia.
        val bytes = NoteScan.bytesFromUri(appContext, uri, 1600) ?: runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) return null
        return saveBackgroundBytes(bytes, "up_${System.currentTimeMillis()}.jpg")
    }

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK, false))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    // Motyw kolorystyczny (id z ThemePalettes; domyślnie fiolet).
    private val _themeId = MutableStateFlow(prefs.getString(KEY_THEME, "violet").orEmpty())
    val themeId: StateFlow<String> = _themeId.asStateFlow()
    fun setThemeId(id: String) {
        prefs.edit().putString(KEY_THEME, id).apply()
        _themeId.value = id
    }

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

    // --- Realny czas wykonania (heurystyka: otwarto → ukończono), średnia na zadanie ---
    private fun computeAvgActual(): Int {
        val c = prefs.getInt(KEY_ACT_COUNT, 0)
        return if (c <= 0) 0 else (prefs.getLong(KEY_ACT_SUM, 0L) / c).toInt()
    }
    private val _avgActualMinutes = MutableStateFlow(computeAvgActual())
    val avgActualMinutes: StateFlow<Int> = _avgActualMinutes.asStateFlow()
    val actualSamples: Int get() = prefs.getInt(KEY_ACT_COUNT, 0)

    /** Zapamiętuje moment pierwszego otwarcia zadania (do pomiaru czasu). */
    fun markTaskOpened(id: Long) {
        val k = "opened_$id"
        if (!prefs.contains(k)) prefs.edit().putLong(k, System.currentTimeMillis()).apply()
    }

    /** Po ukończeniu liczy realny czas (otwarto→teraz) i dokłada do średniej. */
    fun recordActualCompletion(id: Long) {
        val k = "opened_$id"
        val opened = prefs.getLong(k, 0L)
        prefs.edit().remove(k).apply()
        if (opened <= 0L) return
        val mins = ((System.currentTimeMillis() - opened) / 60_000L).toInt()
        if (mins !in 1..240) return // odrzuć śmieciowe wartości (otwarte dawno temu)
        val sum = prefs.getLong(KEY_ACT_SUM, 0L) + mins
        val cnt = prefs.getInt(KEY_ACT_COUNT, 0) + 1
        prefs.edit().putLong(KEY_ACT_SUM, sum).putInt(KEY_ACT_COUNT, cnt).apply()
        _avgActualMinutes.value = (sum / cnt).toInt()
    }

    /** Zasiew statystyk demo (godziny otwarć, przesunięcia, śr. czas, zużycie AI). */
    fun seedDemoStats() {
        // Rozkład godzin otwierania — piki rano (8–9) i wieczorem (19–21).
        val hours = IntArray(24)
        val profile = mapOf(6 to 4, 7 to 12, 8 to 41, 9 to 38, 10 to 22, 11 to 18, 12 to 15,
            13 to 20, 14 to 17, 15 to 24, 16 to 19, 17 to 21, 18 to 28, 19 to 44, 20 to 39, 21 to 33, 22 to 16, 23 to 7)
        profile.forEach { (h, c) -> hours[h] = c }
        prefs.edit()
            .putString(KEY_OPEN_HOURS, hours.joinToString(","))
            .putLong(KEY_DEFERS, 37)
            .putLong(KEY_ACT_SUM, 1792).putInt(KEY_ACT_COUNT, 64)   // ~28 min/zadanie
            .putLong(KEY_AI_IN, 42150).putLong(KEY_AI_OUT, 15320).putLong(KEY_AI_IMAGES, 4)
            .apply()
        _openHours.value = readHours()
        _deferCount.value = 37
        _avgActualMinutes.value = computeAvgActual()
        _aiPromptTokens.value = 42150; _aiCompletionTokens.value = 15320; _aiImageCount.value = 4
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
        const val KEY_THEME = "color_theme"
        const val KEY_AI_IN = "ai_prompt_tokens"
        const val KEY_AI_OUT = "ai_completion_tokens"
        const val KEY_AI_IMAGES = "ai_image_count"
        const val KEY_DEFERS = "defer_count"
        const val KEY_ACT_SUM = "actual_sum_min"
        const val KEY_ACT_COUNT = "actual_count"
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
        const val KEY_SEEDED = "demo_seeded_v5"
        const val KEY_AREA = "active_area"
        const val KEY_OPENAI = "openai_key"
        const val KEY_OPENAI_ADMIN = "openai_admin_key"
        const val KEY_CUSTOM_PHOTOS = "custom_photos"
        const val KEY_ACTIVE_CUSTOM = "active_custom_bg"
        const val KEY_PHASE_BG = "phase_backgrounds"
        const val KEY_USE_PHASE_BG = "use_phase_bg"
    }
}
