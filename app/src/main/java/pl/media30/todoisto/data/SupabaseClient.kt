package pl.media30.todoisto.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimalny klient Supabase (REST, bez SDK) — logowanie/rejestracja (GoTrue)
 * oraz kopia danych w tabeli (PostgREST). Wywołania idą wprost z urządzenia do
 * projektu użytkownika. Klucz anon jest publiczny (klienta), token sesji trzyma
 * CloudStore. NIE testowane na żywo z tego środowiska — patrz instrukcja setupu.
 */
object SupabaseClient {

    private const val TABLE = "todoisto_backups"

    data class AuthResult(val token: String?, val userId: String?, val email: String?, val error: String?)

    suspend fun signUp(url: String, anonKey: String, email: String, password: String): AuthResult =
        auth("$url/auth/v1/signup", anonKey, email, password)

    suspend fun signIn(url: String, anonKey: String, email: String, password: String): AuthResult =
        auth("$url/auth/v1/token?grant_type=password", anonKey, email, password)

    private suspend fun auth(endpoint: String, anonKey: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("email", email.trim()).put("password", password).toString()
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching {
                    val o = JSONObject(text)
                    o.optString("msg").ifBlank { o.optString("error_description").ifBlank { o.optString("error", "Błąd $code") } }
                }.getOrDefault("Błąd $code")
                return@withContext AuthResult(null, null, null, msg)
            }
            val o = JSONObject(text)
            val token = o.optString("access_token", "")
            val user = o.optJSONObject("user") ?: o
            val uid = user.optString("id", "")
            val mail = user.optString("email", email.trim())
            if (token.isBlank()) {
                // Rejestracja z włączonym potwierdzeniem e-mail: brak tokenu od razu.
                AuthResult(null, uid.ifBlank { null }, mail, "Sprawdź e-mail i potwierdź konto, potem zaloguj się.")
            } else {
                AuthResult(token, uid.ifBlank { null }, mail, null)
            }
        }

    /** Wysyła kopię danych (upsert po user_id). Zwraca null przy sukcesie lub komunikat błędu. */
    suspend fun pushBackup(url: String, anonKey: String, token: String, userId: String, backupJson: String): String? =
        withContext(Dispatchers.IO) {
            val row = JSONObject().put("user_id", userId).put("data", JSONObject(backupJson))
            val body = JSONArray().put(row).toString()
            val conn = (URL("$url/rest/v1/$TABLE?on_conflict=user_id").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 20000; readTimeout = 40000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) return@withContext null
            val text = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            runCatching { JSONObject(text).optString("message").ifBlank { "Błąd $code" } }.getOrDefault("Błąd $code")
        }

    /** Pobiera kopię danych (JSON) albo null gdy brak/błąd. */
    suspend fun pullBackup(url: String, anonKey: String, token: String, userId: String): String? =
        withContext(Dispatchers.IO) {
            val conn = (URL("$url/rest/v1/$TABLE?user_id=eq.$userId&select=data").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 20000; readTimeout = 40000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return@withContext null
            val arr = runCatching { JSONArray(text) }.getOrNull() ?: return@withContext null
            if (arr.length() == 0) return@withContext null
            arr.getJSONObject(0).optJSONObject("data")?.toString()
        }
}
