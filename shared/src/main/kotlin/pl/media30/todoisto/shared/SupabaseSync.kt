package pl.media30.todoisto.shared

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klient Supabase (REST, bez SDK) — ta sama tabela i format co w aplikacji
 * Android (`todoisto_backups`, blob w kolumnie `data`). Funkcje są blokujące;
 * wywołuj je z wątku w tle. Dane logowania trzyma warstwa wyżej.
 */
object SupabaseSync {

    private const val TABLE = "todoisto_backups"

    data class Auth(val token: String?, val userId: String?, val email: String?, val error: String?)

    fun signUp(url: String, anonKey: String, email: String, password: String): Auth =
        auth("$url/auth/v1/signup", anonKey, email, password)

    fun signIn(url: String, anonKey: String, email: String, password: String): Auth =
        auth("$url/auth/v1/token?grant_type=password", anonKey, email, password)

    private fun auth(endpoint: String, anonKey: String, email: String, password: String): Auth {
        return try {
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
                return Auth(null, null, null, msg)
            }
            val o = JSONObject(text)
            val token = o.optString("access_token", "")
            val user = o.optJSONObject("user") ?: o
            val uid = user.optString("id", "")
            val mail = user.optString("email", email.trim())
            if (token.isBlank()) Auth(null, uid.ifBlank { null }, mail, "Sprawdź e-mail i potwierdź konto, potem zaloguj się.")
            else Auth(token, uid.ifBlank { null }, mail, null)
        } catch (e: Exception) {
            Auth(null, null, null, e.message ?: "Brak połączenia z Supabase.")
        }
    }

    /** Upsert migawki po user_id. Zwraca null przy sukcesie albo komunikat błędu. */
    fun push(url: String, anonKey: String, token: String, userId: String, backupJson: String): String? {
        return try {
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
            if (code in 200..299) null
            else {
                val text = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                runCatching { JSONObject(text).optString("message").ifBlank { "Błąd $code" } }.getOrDefault("Błąd $code")
            }
        } catch (e: Exception) {
            e.message ?: "Brak połączenia z Supabase."
        }
    }

    /** Pobiera blob migawki (JSON) albo null gdy brak/błąd. */
    fun pull(url: String, anonKey: String, token: String, userId: String): String? {
        return try {
            val conn = (URL("$url/rest/v1/$TABLE?user_id=eq.$userId&select=data").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 20000; readTimeout = 40000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) return null
            val arr = runCatching { JSONArray(text) }.getOrNull() ?: return null
            if (arr.length() == 0) return null
            arr.getJSONObject(0).optJSONObject("data")?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
