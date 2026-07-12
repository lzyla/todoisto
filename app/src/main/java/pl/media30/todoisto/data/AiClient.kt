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
}
