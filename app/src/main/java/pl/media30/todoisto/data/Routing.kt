package pl.media30.todoisto.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Przekazywanie zadań do narzędzi projektów (MVP: Notion), jednokierunkowo:
 * Todoisto → Edge Function `route-task` → Notion. W aplikacji NIE MA tokenu Notion —
 * tylko sesja Supabase użytkownika. Konfiguracja celów żyje w tabeli
 * `todoisto_destinations` (RLS), tu trzymamy jej lokalną kopię do decyzji offline.
 */
data class Destination(
    val projectId: Long,
    val notionDatabaseId: String,
    val statusValue: String,
    val statusProperty: String?,
    val dateProperty: String?,
    val enabledAt: Long
)

class RoutingStore(context: Context, private val cloud: CloudStore) {
    private val prefs = context.applicationContext.getSharedPreferences("todoisto_routing", Context.MODE_PRIVATE)
    private val _destinations = MutableStateFlow(load())
    val destinations: StateFlow<Map<Long, Destination>> = _destinations.asStateFlow()

    /** Czy zadanie ma trafić do kolejki (wywoływane przy każdym zapisie zadania). */
    fun shouldRoute(t: Task): Boolean {
        val pid = t.projectId ?: return false
        val d = _destinations.value[pid] ?: return false
        return t.parentId == null && !t.isCompleted && !t.deleted && t.createdAt >= d.enabledAt
    }

    private fun load(): Map<Long, Destination> = runCatching {
        val a = JSONArray(prefs.getString("dest", "[]"))
        (0 until a.length()).map { fromJson(a.getJSONObject(it)) }.associateBy { it.projectId }
    }.getOrDefault(emptyMap())

    private fun persist(m: Map<Long, Destination>) {
        val a = JSONArray(); m.values.forEach { a.put(toJson(it)) }
        prefs.edit().putString("dest", a.toString()).apply()
        _destinations.value = m
    }

    private fun toJson(d: Destination) = JSONObject().put("project_id", d.projectId).put("notion_database_id", d.notionDatabaseId)
        .put("status_value", d.statusValue).put("status_property", d.statusProperty ?: JSONObject.NULL)
        .put("date_property", d.dateProperty ?: JSONObject.NULL).put("enabled_at_ms", d.enabledAt)

    private fun fromJson(o: JSONObject) = Destination(
        projectId = o.getLong("project_id"),
        notionDatabaseId = o.optString("notion_database_id"),
        statusValue = o.optString("status_value", "To do"),
        statusProperty = o.optString("status_property").takeIf { it.isNotBlank() && it != "null" },
        dateProperty = o.optString("date_property").takeIf { it.isNotBlank() && it != "null" },
        enabledAt = if (o.has("enabled_at_ms")) o.getLong("enabled_at_ms")
                    else runCatching { java.time.OffsetDateTime.parse(o.getString("enabled_at")).toInstant().toEpochMilli() }.getOrDefault(Long.MAX_VALUE) // nieczytelna data = nie przekazuj (bezpieczniej niż zalać Notion starymi zadaniami)
    )

    private suspend fun http(method: String, path: String, body: String? = null, prefer: String? = null): Pair<Int, String> = withContext(Dispatchers.IO) {
        val token = cloud.freshToken() ?: return@withContext 401 to "Zaloguj się w chmurze (Ustawienia → Konto)."
        val conn = (URL("${cloud.url0}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 20000; readTimeout = 40000
            setRequestProperty("apikey", cloud.anonKey0)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            if (body != null) doOutput = true
        }
        if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        code to ((if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty())
    }

    /** Pobiera konfigurację z Supabase. Zwraca błąd albo null. */
    suspend fun refresh(): String? = runCatching {
        if (!cloud.signedIn) return "Zaloguj się w chmurze."
        val (code, text) = http("GET", "/rest/v1/todoisto_destinations?select=*&enabled=is.true&kind=eq.notion")
        if (code !in 200..299) return "Nie pobrano konfiguracji ($code). Czy migracja 20261103000002 jest wgrana?"
        val a = JSONArray(text)
        persist((0 until a.length()).map { fromJson(a.getJSONObject(it)) }.associateBy { it.projectId })
        null
    }.getOrElse { it.message ?: "Błąd sieci." }

    /** Zapisuje (upsert) cel dla projektu. Nowy cel dotyczy zadań tworzonych od teraz. */
    suspend fun save(projectId: Long, notionLink: String, statusValue: String, statusProperty: String?, dateProperty: String?): String? = runCatching {
        val dbId = parseNotionDatabaseId(notionLink) ?: return "To nie wygląda na link do bazy Notion."
        val row = JSONObject().put("project_id", projectId).put("kind", "notion").put("notion_database_id", dbId)
            .put("status_value", statusValue.ifBlank { "To do" })
            .put("status_property", statusProperty?.ifBlank { null } ?: JSONObject.NULL)
            .put("date_property", dateProperty?.ifBlank { null } ?: JSONObject.NULL)
            .put("enabled", true)
        val (code, text) = http("POST", "/rest/v1/todoisto_destinations?on_conflict=user_id,project_id",
            JSONArray().put(row).toString(), prefer = "resolution=merge-duplicates,return=representation")
        if (code !in 200..299) return "Nie zapisano ($code): ${text.take(200)}"
        refresh()
    }.getOrElse { it.message ?: "Błąd sieci." }

    suspend fun remove(projectId: Long): String? = runCatching {
        val (code, text) = http("DELETE", "/rest/v1/todoisto_destinations?project_id=eq.$projectId")
        if (code !in 200..299) return "Nie usunięto ($code): ${text.take(200)}"
        persist(_destinations.value - projectId)
        null
    }.getOrElse { it.message ?: "Błąd sieci." }

    /** Wynik jednego wywołania Edge Function. */
    sealed class Outcome {
        data class Sent(val url: String) : Outcome()
        data class Permanent(val message: String) : Outcome()
        data class Retry(val message: String) : Outcome()
    }

    suspend fun send(task: Task): Outcome = runCatching {
        val payload = JSONObject()
            .put("projectId", task.projectId.toString())
            .put("task", JSONObject()
                .put("id", task.id.toString()).put("title", task.title).put("notes", task.notes)
                .put("dueDate", task.dueDate ?: JSONObject.NULL).put("dueTimeMinutes", task.dueTimeMinutes ?: JSONObject.NULL)
                .put("priority", task.priority.name))
        val (code, text) = http("POST", "/functions/v1/route-task", payload.toString())
        val o = runCatching { JSONObject(text) }.getOrNull()
        when {
            code in 200..299 && o?.optString("remoteUrl")?.isNotBlank() == true -> Outcome.Sent(o.getString("remoteUrl"))
            code == 401 -> Outcome.Permanent(o?.optString("error")?.ifBlank { null } ?: "Sesja w chmurze wygasła.")
            o?.optBoolean("retryable", code >= 500) == true || code >= 500 || code == 429 -> Outcome.Retry(o?.optString("error") ?: "HTTP $code")
            else -> Outcome.Permanent(o?.optString("error")?.ifBlank { null } ?: "HTTP $code")
        }
    }.getOrElse { Outcome.Retry(it.message ?: "Brak sieci.") }

    companion object {
        /** Id bazy z linku Notion — ostatnie 32 znaki hex ostatniego segmentu ścieżki. */
        fun parseNotionDatabaseId(input: String): String? {
            val segment = input.trim().split('?', '#')[0].trimEnd('/').substringAfterLast('/')
            val tail = segment.replace("-", "").takeLast(32)
            return tail.lowercase().takeIf { Regex("^[0-9a-f]{32}$").matches(it) }
        }
    }
}

/**
 * Kolejka wysyłki (outbox): zadania z routeState = "pending". Działa przy sieci,
 * z wykładniczym ponawianiem; idempotencję gwarantuje Edge Function (rejestr +
 * deduplikacja po kolumnie „Todoisto ID" w Notion), więc ponowienie nie robi duplikatów.
 */
class RouteWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as pl.media30.todoisto.TodoApplication
        val repo = app.repository
        var retry = false
        for (t in repo.pendingRoutes()) {
            when (val r = app.routing.send(t)) {
                is RoutingStore.Outcome.Sent -> repo.setRouteState(t.id, "sent", r.url)
                is RoutingStore.Outcome.Permanent -> repo.setRouteState(t.id, "error:" + r.message.take(300))
                is RoutingStore.Outcome.Retry -> retry = true
            }
        }
        return if (retry && runAttemptCount < 12) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "route_tasks"
        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<RouteWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // APPEND_OR_REPLACE: zadanie dodane w trakcie biegu workera też zostanie obsłużone.
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}
