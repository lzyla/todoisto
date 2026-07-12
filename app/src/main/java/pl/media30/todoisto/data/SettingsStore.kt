package pl.media30.todoisto.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Simple persisted app settings (theme, productivity goals). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("todoisto_settings", Context.MODE_PRIVATE)

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

    fun setGoals(daily: Int, weekly: Int) {
        prefs.edit().putInt(KEY_DAILY, daily).putInt(KEY_WEEKLY, weekly).apply()
        _dailyGoal.value = daily
        _weeklyGoal.value = weekly
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

    private companion object {
        const val KEY_DARK = "dark_theme"
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
    }
}
