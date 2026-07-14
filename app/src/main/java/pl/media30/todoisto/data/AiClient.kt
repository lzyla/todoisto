package pl.media30.todoisto.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimalny klient OpenAI (Chat Completions) — bez dodatkowych bibliotek.
 * Klucz podaje użytkownik w ustawieniach; żądania idą wprost z urządzenia do
 * api.openai.com. Zwraca gotową odpowiedź tekstową albo rzuca wyjątek z opisem.
 */
/** Odpowiedź AI wraz z liczbą zużytych tokenów (do liczenia kosztu). */
data class AiResult(val text: String, val promptTokens: Int, val completionTokens: Int)

object AiClient {

    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private const val MODEL = "gpt-4o-mini"
    // Cennik gpt-4o-mini (USD za 1 mln tokenów): wejście 0.15, wyjście 0.60.
    const val PRICE_IN_PER_1M = 0.15
    const val PRICE_OUT_PER_1M = 0.60
    private const val SYSTEM =
        "Jesteś asystentem produktywności. Odpowiadaj po polsku, zwięźle i konkretnie, " +
        "krok po kroku. Skup się na tym, które narzędzie wybrać i jak zautomatyzować zadanie. " +
        "Na końcu podaj jeden gotowy prompt do skopiowania."

    suspend fun ask(apiKey: String, prompt: String): AiResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Brak klucza API" }
        val body = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.4)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", SYSTEM))
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }.toString()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 40000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }
                .getOrDefault("Błąd $code")
            throw RuntimeException(msg)
        }
        val obj = JSONObject(text)
        val content = obj.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
        val usage = obj.optJSONObject("usage")
        AiResult(
            text = content,
            promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
            completionTokens = usage?.optInt("completion_tokens", 0) ?: 0
        )
    }

    /**
     * Generuje [count] propozycji tła (OpenAI Images, dall-e-3) — zwraca listę
     * obrazów jako bajty PNG. Woła się kluczem użytkownika; każdy obraz to
     * osobne żądanie (dall-e-3 obsługuje n=1). Uwaga: to operacja płatna.
     */
    private fun generateOne(apiKey: String, prompt: String): ByteArray {
        // Model obrazów: gpt-image-1 (aktualny). Uwaga: NIE wysyłamy
        // "response_format" — gpt-image-1 go nie przyjmuje i zawsze zwraca b64_json.
        // Rozmiar 1024x1536 to pionowy format wspierany przez gpt-image-1
        // (1024x1792 było tylko dla dall-e-3, który na tym koncie nie istnieje).
        val body = JSONObject().apply {
            put("model", "gpt-image-1")
            put("prompt", prompt)
            put("n", 1)
            put("size", "1024x1536")
        }.toString()
        val conn = (URL("https://api.openai.com/v1/images/generations").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrDefault("Błąd $code")
            throw RuntimeException(msg)
        }
        val item = JSONObject(text).getJSONArray("data").getJSONObject(0)
        val b64 = item.optString("b64_json", "")
        if (b64.isNotEmpty()) {
            return android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        }
        val imageUrl = item.optString("url", "")
        if (imageUrl.isNotEmpty()) {
            return downloadBytes(imageUrl)
        }
        throw RuntimeException("Brak danych obrazu w odpowiedzi API")
    }

    /** Pobiera surowe bajty spod adresu URL (obraz zwrócony przez API). */
    private fun downloadBytes(fromUrl: String): ByteArray {
        val conn = (URL(fromUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 120000
        }
        val code = conn.responseCode
        if (code !in 200..299) throw RuntimeException("Nie udało się pobrać obrazu (kod $code)")
        return conn.inputStream.use { it.readBytes() }
    }

    suspend fun generateBackgrounds(apiKey: String, count: Int = 3): List<ByteArray> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Brak klucza API" }
        val prompt =
            "Minimalistyczne, spokojne pionowe tło (wallpaper na telefon) do aplikacji z zadaniami. " +
            "Miękki, rozmyty gradient, delikatne organiczne kształty, paleta fiolet-róż-żółty, dużo wolnej przestrzeni. " +
            "Bez tekstu, bez logo, bez ludzi."
        (0 until count).map { generateOne(apiKey, prompt) }
    }

    /** 3 REALISTYCZNE zdjęcia wg pory dnia (świt, dzień, zmierzch) — do tła zmiennego. */
    suspend fun generatePhaseBackgrounds(apiKey: String): List<ByteArray> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Brak klucza API" }
        val base = "Realistyczne, wysokiej jakości zdjęcie krajobrazu, pionowe (wallpaper na telefon), spokojne, " +
            "dużo wolnej przestrzeni u góry. Bez tekstu, bez logo, bez ludzi. Pora: "
        listOf(
            base + "wczesny poranek, złoty świt, miękkie mgły, ciepłe światło.",
            base + "jasne południe, czyste niebo, pełne słońce, żywe barwy.",
            base + "wieczór, zmierzch/zachód słońca, ciepła złota godzina, delikatny fiolet nieba."
        ).map { generateOne(apiKey, it) }
    }

    /** Odczytuje zadania ze zdjęcia kartki (OpenAI Vision, gpt-4o-mini). Zwraca listę tytułów. */
    suspend fun extractTasksFromImage(apiKey: String, jpegBytes: ByteArray): List<String> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Brak klucza API" }
        val dataUrl = "data:image/jpeg;base64," + android.util.Base64.encodeToString(jpegBytes, android.util.Base64.NO_WRAP)
        val userContent = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text",
                "Na zdjęciu jest odręczna lub wydrukowana lista rzeczy do zrobienia. " +
                "Wypisz zadania, jedno na linię, bez numeracji, bez wypunktowań, tylko treść zadań po polsku. " +
                "Jeśli nie ma zadań, zwróć pustą odpowiedź."))
            put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
        }
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("temperature", 0.2)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", userContent))
            })
        }.toString()
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrDefault("Błąd $code")
            throw RuntimeException(msg)
        }
        val answer = JSONObject(text).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
        answer.split("\n")
            .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
            .filter { it.isNotEmpty() && it.length in 2..200 }
    }

    /**
     * Realny koszt z panelu OpenAI (Costs API) — wymaga klucza ADMIN (sk-admin-…),
     * bo zwykły klucz nie ma dostępu do rozliczeń. Sumuje dzienne kubełki od
     * [startTimeUnix] (sekundy) do teraz. Zwraca kwotę w USD.
     */
    suspend fun fetchCostsUsd(adminKey: String, startTimeUnix: Long): Double = withContext(Dispatchers.IO) {
        require(adminKey.isNotBlank()) { "Brak klucza Admin" }
        val url = URL("https://api.openai.com/v1/organization/costs?start_time=$startTimeUnix&bucket_width=1d&limit=31")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 40000
            setRequestProperty("Authorization", "Bearer $adminKey")
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val msg = runCatching { JSONObject(text).getJSONObject("error").getString("message") }
                .getOrDefault("Błąd $code — czy to klucz Admin?")
            throw RuntimeException(msg)
        }
        val data = JSONObject(text).optJSONArray("data") ?: JSONArray()
        var total = 0.0
        for (i in 0 until data.length()) {
            val results = data.getJSONObject(i).optJSONArray("results") ?: continue
            for (j in 0 until results.length()) {
                total += results.getJSONObject(j).optJSONObject("amount")?.optDouble("value", 0.0) ?: 0.0
            }
        }
        total
    }
}
