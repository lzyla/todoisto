package pl.media30.todoisto.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

/** Profil zalogowanego użytkownika (dane trzymane lokalnie na urządzeniu). */
data class Account(val name: String, val email: String)

/**
 * Lokalne konto użytkownika — pełny workflow logowania/rejestracji BEZ serwera:
 * dane i zahashowane hasło (SHA-256 + sól) trzymamy w SharedPreferences. API jest
 * celowo takie jak przy prawdziwym backendzie (register/login/logout), więc w
 * przyszłości można podmienić implementację na wywołania sieciowe bez zmian w UI.
 */
class AccountStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("todoisto_account", Context.MODE_PRIVATE)

    private val _account = MutableStateFlow(readAccount())
    val account: StateFlow<Account?> = _account.asStateFlow()

    private val _loggedIn = MutableStateFlow(prefs.getBoolean(KEY_LOGGED_IN, false))
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    /** Czy istnieje już zarejestrowane konto (decyduje: ekran logowania vs rejestracji). */
    val hasAccount: Boolean get() = prefs.contains(KEY_EMAIL) && prefs.contains(KEY_HASH)

    private fun readAccount(): Account? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return Account(name, email)
    }

    /** Rejestracja konta. Zwraca komunikat błędu albo null przy sukcesie (i loguje). */
    fun register(name: String, email: String, password: String): String? {
        val n = name.trim(); val e = email.trim().lowercase()
        if (n.isBlank()) return "Podaj imię"
        if (!e.contains("@") || e.length < 5) return "Podaj poprawny e-mail"
        if (password.length < 4) return "Hasło musi mieć min. 4 znaki"
        val salt = randomSalt()
        prefs.edit()
            .putString(KEY_NAME, n)
            .putString(KEY_EMAIL, e)
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash(password, salt))
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        _account.value = Account(n, e)
        _loggedIn.value = true
        return null
    }

    /** Logowanie. Zwraca komunikat błędu albo null przy sukcesie. */
    fun login(email: String, password: String): String? {
        val storedEmail = prefs.getString(KEY_EMAIL, null) ?: return "Brak konta — zarejestruj się"
        val salt = prefs.getString(KEY_SALT, "").orEmpty()
        val storedHash = prefs.getString(KEY_HASH, "").orEmpty()
        val ok = email.trim().lowercase() == storedEmail && hash(password, salt) == storedHash
        if (!ok) return "Nieprawidłowy e-mail lub hasło"
        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        _loggedIn.value = true
        return null
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
        _loggedIn.value = false
    }

    /** Zmiana wyświetlanego imienia (edycja profilu). */
    fun updateName(name: String) {
        val n = name.trim()
        if (n.isBlank()) return
        prefs.edit().putString(KEY_NAME, n).apply()
        _account.value = _account.value?.copy(name = n)
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(12)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_LOGGED_IN = "logged_in"
    }
}
