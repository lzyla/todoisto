package pl.media30.todoisto.desktop

import org.json.JSONObject
import pl.media30.todoisto.shared.CloudTask
import pl.media30.todoisto.shared.SupabaseSync
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** Trzy poziomy energii do pracy — jak na Androidzie (celowo grube). */
enum class EnergyLevel(val label: String, val emoji: String) {
    LOW("Na luzie", "😌"), MED("Przyzwoicie", "🙂"), HIGH("Zdziałasz dużo", "😄")
}

data class EnergySignals(
    val sleepMinutes: Int? = null,
    val sleepBaselineMinutes: Int? = null,
    val restingHr: Double? = null,
    val restingHrBaseline: Double? = null,
    val hrv: Double? = null,
    val hrvBaseline: Double? = null,
    val prevDayWorkMinutes: Int = 0,
    val prevDayLastWorkMinute: Int? = null
)

data class EnergyEstimate(val level: EnergyLevel, val score: Int, val reasons: List<String>)

/** Ta sama logika co `EnergyModel` w aplikacji na telefon (progi i wagi 1:1). */
object EnergyModel {
    private const val START = 65

    fun estimate(s: EnergySignals): EnergyEstimate {
        var score = START
        val why = mutableListOf<String>()
        s.sleepMinutes?.let { sleep ->
            val base = s.sleepBaselineMinutes ?: 420
            var d = ((sleep - base) / 60.0 * 12.0).coerceIn(-30.0, 10.0)
            if (sleep < 300) d -= 10.0
            score += d.toInt()
            val h = sleep / 60; val m = sleep % 60
            if (d <= -6) why += "krótki sen (${h}h ${m}min)"
            else if (d >= 5) why += "dobry sen (${h}h ${m}min)"
        }
        if (s.restingHr != null && s.restingHrBaseline != null) {
            val d = -((s.restingHr - s.restingHrBaseline) * 3.0).coerceIn(0.0, 15.0)
            score += d.toInt()
            if (d <= -6) why += "podwyższone tętno spoczynkowe"
        }
        if (s.hrv != null && s.hrvBaseline != null && s.hrvBaseline > 0) {
            val ratio = s.hrv / s.hrvBaseline
            val d = when { ratio < 0.7 -> -18; ratio < 0.85 -> -12; ratio > 1.1 -> 5; else -> 0 }
            score += d
            if (d <= -12) why += "niska zmienność tętna (HRV)"
        }
        when {
            s.prevDayWorkMinutes > 480 -> { score -= 10; why += "bardzo pracowity wczorajszy dzień" }
            s.prevDayWorkMinutes > 360 -> { score -= 5; why += "pracowity wczorajszy dzień" }
        }
        s.prevDayLastWorkMinute?.let { m ->
            if (m >= 23 * 60 || m < 4 * 60) { score -= 8; why += "praca do późna wczoraj" }
        }
        score = score.coerceIn(0, 100)
        val level = when { score < 45 -> EnergyLevel.LOW; score < 70 -> EnergyLevel.MED; else -> EnergyLevel.HIGH }
        return EnergyEstimate(level, score, why)
    }

    fun resolve(manual: EnergyLevel?, estimate: EnergyEstimate): EnergyLevel = manual ?: estimate.level
}

/** „Koszt” zadania i przestawianie listy Dziś zależnie od energii. */
object TaskLoad {
    fun cost(t: CloudTask, subtasks: Int = 0): Int {
        val dur = t.durationMinutes ?: 30
        val prio = when (t.priority) { "P1" -> 3; "P2" -> 2; "P3" -> 1; else -> 0 }
        val notes = when { t.notes.length > 300 -> 2; t.notes.length > 120 -> 1; else -> 0 }
        return dur / 15 + prio + notes + (if (subtasks >= 4) 1 else 0)
    }

    fun isHeavy(t: CloudTask, subtasks: Int = 0) = cost(t, subtasks) >= 6

    fun isPinned(t: CloudTask, today: Long): Boolean =
        t.dueTimeMinutes != null ||
            (t.deadline != null && t.deadline!! <= today + 1) ||
            (t.priority == "P1" && t.dueDate != null && t.dueDate!! <= today)

    fun reorder(tasks: List<CloudTask>, level: EnergyLevel?, today: Long, subCount: (CloudTask) -> Int = { 0 }): List<CloudTask> {
        if (level == null || level == EnergyLevel.MED) return tasks
        val movable = tasks.filter { !isPinned(it, today) }
        val sorted = if (level == EnergyLevel.LOW) movable.sortedBy { cost(it, subCount(it)) }
        else movable.sortedByDescending { cost(it, subCount(it)) }
        val it = sorted.iterator()
        return tasks.map { t -> if (isPinned(t, today)) t else it.next() }
    }
}

/** Prompt „Ułatw z AI” — ten sam co na telefonie. */
fun easePrompt(t: CloudTask): String =
    "Zadanie: „" + t.title + "”." +
        (if (t.notes.isNotBlank() && !t.notes.startsWith("activity:")) " Notatki: " + t.notes.take(600) + "." else "") +
        " Mam dziś mniej energii. Zaproponuj, jak je zrealizować możliwie lekko: 1) rozbij na 4–7 małych kroków po 5–15 minut," +
        " pierwszy ma być bardzo łatwy (rozruch), 2) wskaż, co można pominąć, uprościć lub zlecić AI/narzędziu," +
        " 3) na końcu podaj gotowy prompt do skopiowania, który zrobi najcięższą część za mnie. Kroki jako numerowana lista."

/** Krótkie teksty chmurki (identyczne jak w Androidzie). */
object EnergyText {
    fun headline(l: EnergyLevel) = when (l) { EnergyLevel.LOW -> "Dziś na luzie"; EnergyLevel.HIGH -> "Zdziałasz dużo"; else -> "Przyzwoicie" }

    fun why(manual: Boolean, reasons: List<String>): String =
        if (manual) "Wybrane przez Ciebie"
        else reasons.take(2).map { it.replace(Regex("\\s*\\(.*?\\)"), "").replace(" spoczynkowe", "").lowercase() }
            .joinToString(", ").replaceFirstChar { it.uppercase() }

    fun advice(l: EnergyLevel, heavy: Int) = when (l) {
        EnergyLevel.LOW -> "Zacznij od lżejszych zadań." + when {
            heavy == 1 -> " Przy jednym zadaniu skorzystaj z pomocy AI."
            heavy > 1 -> " Przy kilku zadaniach skorzystaj z pomocy AI."
            else -> ""
        }
        EnergyLevel.HIGH -> "Zacznij od trudnych zadań."
        else -> "Kolejność bez zmian."
    }
}

/**
 * Google Health przez Edge Function `health-sync` (jak `HealthStore` na telefonie). Klucze Google
 * zostają po stronie Supabase; tu tylko wywołania z tokenem zalogowanego użytkownika.
 */
class HealthClient(private val settings: AppSettings) {
    sealed class Result {
        data class Ok(val signals: EnergySignals?) : Result()
        object NotConnected : Result()
        object Expired : Result()
        data class Error(val message: String) : Result()
    }

    private fun call(action: String): Pair<Int, String> {
        if (!settings.signedIn) return 401 to "{\"error\":\"Zaloguj się w chmurze (Ustawienia → Konto).\"}"
        val auth = SupabaseSync.signIn(settings.url, settings.anonKey, settings.email, settings.cloudPassword)
        val token = auth.token ?: return 401 to JSONObject().put("error", auth.error ?: "Logowanie nie powiodło się.").toString()
        val conn = (URL("${settings.url}/functions/v1/health-sync").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("apikey", settings.anonKey)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(JSONObject().put("action", action).toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        return code to ((if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty())
    }

    private fun err(text: String, code: Int) = runCatching { JSONObject(text).getString("error") }.getOrNull() ?: "Błąd $code"

    /** Adres zgody Google (do otwarcia w przeglądarce) albo komunikat błędu. */
    fun startUrl(): Pair<String?, String?> = runCatching {
        val (code, text) = call("start")
        if (code in 200..299) JSONObject(text).getString("url") to null else null to err(text, code)
    }.getOrElse { null to (it.message ?: "Błąd sieci.") }

    fun sync(): Result = runCatching {
        val (code, text) = call("sync")
        if (code !in 200..299) return Result.Error(err(text, code))
        val o = JSONObject(text)
        if (!o.optBoolean("connected")) {
            settings.setHealth(false, null)
            return if (o.optString("reason") == "expired") Result.Expired else Result.NotConnected
        }
        val s = o.getJSONObject("signals")
        settings.setHealth(true, s.toString())
        Result.Ok(parse(s))
    }.getOrElse { Result.Error(it.message ?: "Błąd sieci.") }

    fun disconnect() { runCatching { call("disconnect") }; settings.setHealth(false, null) }

    companion object {
        private fun JSONObject.intOrNull(k: String) = if (isNull(k) || !has(k)) null else optDouble(k).takeIf { !it.isNaN() }?.toInt()
        private fun JSONObject.dblOrNull(k: String) = if (isNull(k) || !has(k)) null else optDouble(k).takeIf { !it.isNaN() }

        /** Sygnały ważne tylko w dniu, którego dotyczą. */
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
