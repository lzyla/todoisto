package pl.media30.todoisto.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Google Health (dawniej Fitbit) przez Edge Function `health-sync`. Aplikacja NIE ma tu żadnych
 * kluczy Google: zgoda OAuth wraca do funkcji, token odświeżania zostaje po stronie Supabase.
 * Aplikacja dostaje tylko gotowe sygnały (sen, tętno spoczynkowe, HRV + ich bazowe poziomy).
 */
class HealthStore(private val cloud: CloudStore, private val settings: SettingsStore) {

    sealed class Result {
        data class Ok(val signals: EnergySignals?) : Result()
        object NotConnected : Result()
        object Expired : Result()
        data class Error(val message: String) : Result()
    }

    private suspend fun call(action: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val token = cloud.freshToken() ?: return@withContext 401 to "Zaloguj się w chmurze (Ustawienia → Konto)."
        val conn = (URL("${cloud.url0}/functions/v1/health-sync").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("apikey", cloud.anonKey0)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(JSONObject().put("action", action).toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        code to ((if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty())
    }

    /** Adres zgody Google do otwarcia w przeglądarce (albo komunikat błędu). */
    suspend fun startUrl(): Pair<String?, String?> = runCatching {
        val (code, text) = call("start")
        if (code in 200..299) JSONObject(text).getString("url") to null
        else null to (runCatching { JSONObject(text).getString("error") }.getOrNull() ?: "Błąd $code")
    }.getOrElse { null to (it.message ?: "Błąd sieci.") }

    suspend fun sync(): Result = runCatching {
        val (code, text) = call("sync")
        if (code !in 200..299) return Result.Error(runCatching { JSONObject(text).getString("error") }.getOrNull() ?: "Błąd $code")
        val o = JSONObject(text)
        if (!o.optBoolean("connected")) {
            settings.setHealth(connected = false, signalsJson = null)
            return if (o.optString("reason") == "expired") Result.Expired else Result.NotConnected
        }
        val s = o.getJSONObject("signals")
        settings.setHealth(connected = true, signalsJson = s.toString())
        Result.Ok(parse(s))
    }.getOrElse { Result.Error(it.message ?: "Błąd sieci.") }

    suspend fun disconnect() { runCatching { call("disconnect") }; settings.setHealth(connected = false, signalsJson = null) }

    companion object {
        private fun JSONObject.intOrNull(k: String) = if (isNull(k) || !has(k)) null else optDouble(k).takeIf { !it.isNaN() }?.toInt()
        private fun JSONObject.dblOrNull(k: String) = if (isNull(k) || !has(k)) null else optDouble(k).takeIf { !it.isNaN() }

        /** Sygnały ważne tylko w dniu, którego dotyczą (stare = brak wpływu na energię). */
        fun parse(o: JSONObject, today: LocalDate = LocalDate.now()): EnergySignals? {
            if (o.optString("date") != today.toString()) return null
            return EnergySignals(
                sleepMinutes = o.intOrNull("sleepMinutes"), sleepBaselineMinutes = o.intOrNull("sleepBaselineMinutes"),
                restingHr = o.dblOrNull("restingHr"), restingHrBaseline = o.dblOrNull("restingHrBaseline"),
                hrv = o.dblOrNull("hrv"), hrvBaseline = o.dblOrNull("hrvBaseline")
            )
        }
    }
}
