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

    fun setGoals(daily: Int, weekly: Int) {
        prefs.edit().putInt(KEY_DAILY, daily).putInt(KEY_WEEKLY, weekly).apply()
        _dailyGoal.value = daily
        _weeklyGoal.value = weekly
    }

    private companion object {
        const val KEY_DARK = "dark_theme"
        const val KEY_DAILY = "daily_goal"
        const val KEY_WEEKLY = "weekly_goal"
        const val KEY_PHOTO_BG = "photo_background"
        // Wersjonowany klucz — bump wymusza jednorazowe ponowne zasianie u wszystkich
        // (v4: po dodaniu Obszarów baza jest przebudowywana, więc dosiewamy z obszarami).
        const val KEY_SEEDED = "demo_seeded_v4"
        const val KEY_AREA = "active_area"
    }
}
