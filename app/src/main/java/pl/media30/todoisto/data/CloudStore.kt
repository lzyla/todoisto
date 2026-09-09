package pl.media30.todoisto.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Konfiguracja i sesja chmury (Supabase). Trzymamy lokalnie: URL projektu,
 * klucz anon (publiczny, przeznaczony do klienta), token sesji i e-mail konta
 * w chmurze. To warstwa OPCJONALNA — apka działa też bez niej (konto lokalne).
 */
class CloudStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("todoisto_cloud", Context.MODE_PRIVATE)

    private val _url = MutableStateFlow(prefs.getString(KEY_URL, "").orEmpty())
    val url: StateFlow<String> = _url.asStateFlow()

    private val _anonKey = MutableStateFlow(prefs.getString(KEY_ANON, "").orEmpty())
    val anonKey: StateFlow<String> = _anonKey.asStateFlow()

    private val _cloudEmail = MutableStateFlow(prefs.getString(KEY_EMAIL, "").orEmpty())
    val cloudEmail: StateFlow<String> = _cloudEmail.asStateFlow()

    private val _accessToken = MutableStateFlow(prefs.getString(KEY_TOKEN, "").orEmpty())
    val accessToken: StateFlow<String> = _accessToken.asStateFlow()

    val userId: String get() = prefs.getString(KEY_UID, "").orEmpty()
    val refreshToken: String get() = prefs.getString(KEY_REFRESH, "").orEmpty()
    val expiresAt: Long get() = prefs.getLong(KEY_EXPIRES, 0L)
    /** Token wygasł lub wygaśnie w ciągu minuty (0 = stara sesja bez daty → traktuj jak wygasłą). */
    val tokenStale: Boolean get() = expiresAt < System.currentTimeMillis() + 60_000
    val url0: String get() = _url.value
    val anonKey0: String get() = _anonKey.value
    val token0: String get() = _accessToken.value

    val configured: Boolean get() = _url.value.isNotBlank() && _anonKey.value.isNotBlank()
    val signedIn: Boolean get() = _accessToken.value.isNotBlank()

    fun setConfig(url: String, anonKey: String) {
        val u = url.trim().trimEnd('/')
        val k = anonKey.trim()
        prefs.edit().putString(KEY_URL, u).putString(KEY_ANON, k).apply()
        _url.value = u; _anonKey.value = k
    }

    fun setSession(email: String, token: String, userId: String, refreshToken: String? = null, expiresAt: Long = 0L) {
        prefs.edit().putString(KEY_EMAIL, email.trim()).putString(KEY_TOKEN, token).putString(KEY_UID, userId)
            .putString(KEY_REFRESH, refreshToken ?: this.refreshToken).putLong(KEY_EXPIRES, expiresAt).apply()
        _cloudEmail.value = email.trim(); _accessToken.value = token
    }

    fun signOut() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).remove(KEY_UID).remove(KEY_REFRESH).remove(KEY_EXPIRES).apply()
        _accessToken.value = ""; _cloudEmail.value = ""
    }

    private companion object {
        const val KEY_URL = "url"
        const val KEY_ANON = "anon"
        const val KEY_EMAIL = "cloud_email"
        const val KEY_TOKEN = "access_token"
        const val KEY_UID = "user_id"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
    }
}
