package pl.media30.todoisto.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Bieżąca pogoda w miejscu użytkownika (Open-Meteo — darmowe, bez klucza). */
data class WeatherNow(
    val tempC: Double,
    val code: Int,
    /** Deszcz/śnieg/burza — aktywności na zewnątrz tracą sens. */
    val isBad: Boolean,
    val emoji: String,
    val label: String
)

object WeatherClient {

    /** Kody WMO Open-Meteo → prosta kategoria. */
    private fun describe(code: Int): Triple<Boolean, String, String> = when (code) {
        0 -> Triple(false, "☀️", "słonecznie")
        1, 2 -> Triple(false, "🌤", "przejaśnienia")
        3 -> Triple(false, "☁️", "pochmurno")
        45, 48 -> Triple(false, "🌫", "mgła")
        in 51..57 -> Triple(true, "🌦", "mżawka")
        in 61..67 -> Triple(true, "🌧", "deszcz")
        in 71..77 -> Triple(true, "🌨", "śnieg")
        in 80..82 -> Triple(true, "🌧", "przelotne opady")
        85, 86 -> Triple(true, "🌨", "śnieżyce")
        in 95..99 -> Triple(true, "⛈", "burza")
        else -> Triple(false, "🌡", "pogoda")
    }

    suspend fun fetch(lat: Double, lon: Double): WeatherNow = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code&timezone=auto"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 6000; readTimeout = 6000
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw RuntimeException("Pogoda: błąd $code")
        val cur = JSONObject(text).getJSONObject("current")
        val wmo = cur.optInt("weather_code", 0)
        val (bad, emoji, label) = describe(wmo)
        WeatherNow(tempC = cur.optDouble("temperature_2m", 0.0), code = wmo, isBad = bad, emoji = emoji, label = label)
    }
}
