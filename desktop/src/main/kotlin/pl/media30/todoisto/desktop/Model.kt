package pl.media30.todoisto.desktop

import pl.media30.todoisto.shared.CloudBackupCodec
import pl.media30.todoisto.shared.CloudSnapshot
import pl.media30.todoisto.shared.CloudTask
import pl.media30.todoisto.shared.SnapshotMerge
import pl.media30.todoisto.shared.SupabaseSync
import java.io.File
import java.time.LocalDate
import java.util.prefs.Preferences

/**
 * Repozytorium desktopu — trzyma pełne [CloudTask] (bez utraty pól przy
 * synchronizacji), zapisuje lokalnie na dysk w tym samym formacie co chmura,
 * i synchronizuje z Supabase przez wspólny moduł `shared`.
 */
class TaskRepository {
    private val dataFile = File(System.getProperty("user.home"), ".todoisto/data.json")
    private val _tasks = mutableListOf<CloudTask>()
    private var nextId = System.currentTimeMillis()

    val tasks: List<CloudTask> get() = _tasks.sortedBy { it.position }

    init { load() }

    fun add(task: CloudTask): CloudTask {
        val now = System.currentTimeMillis()
        val withId = task.copy(id = nextId++, position = _tasks.size, createdAt = now, updatedAt = now)
        _tasks.add(withId); save(); return withId
    }

    fun toggle(id: Long) {
        val i = _tasks.indexOfFirst { it.id == id }
        if (i >= 0) {
            val t = _tasks[i]
            val now = System.currentTimeMillis()
            _tasks[i] = t.copy(
                isCompleted = !t.isCompleted,
                completedAt = if (!t.isCompleted) now else null,
                updatedAt = now
            )
            save()
        }
    }

    fun delete(id: Long) { _tasks.removeAll { it.id == id }; save() }

    /** Podmienia całe dane migawką z chmury (sync = ostatni zapis wygrywa). */
    fun replaceAll(snapshot: CloudSnapshot) {
        _tasks.clear(); _tasks.addAll(snapshot.tasks)
        nextId = (_tasks.maxOfOrNull { it.id } ?: 0L) + 1
        save()
    }

    fun snapshot(): CloudSnapshot = CloudSnapshot(tasks = _tasks.toList())

    private fun load() {
        runCatching {
            if (dataFile.exists()) {
                val snap = CloudBackupCodec.fromJson(dataFile.readText())
                _tasks.clear(); _tasks.addAll(snap.tasks)
                nextId = (_tasks.maxOfOrNull { it.id } ?: 0L) + 1
            }
        }
        if (_tasks.isEmpty()) { seedDemo(); save() }
    }

    private fun save() {
        runCatching {
            dataFile.parentFile.mkdirs()
            dataFile.writeText(CloudBackupCodec.toJson(snapshot()))
        }
    }

    private fun seedDemo() {
        val today = LocalDate.now().toEpochDay()
        var pos = 0
        fun t(title: String, prio: String, min: Int?, dur: Int?, rec: String?, proj: Long?, labels: List<Long>) =
            CloudTask(id = nextId++, title = title, priority = prio, dueDate = today, dueTimeMinutes = min,
                durationMinutes = dur, recurrence = rec, projectId = proj, labelIds = labels, position = pos++)
        _tasks.addAll(listOf(
            t("Przygotować raport miesięczny", "P1", 600, 90, null, 1, listOf(1)),
            t("Stand-up zespołu", "P4", 570, null, "DAILY", 1, emptyList()),
            t("Nadać paczkę na poczcie", "P4", null, null, null, 2, emptyList()),
            t("Przegląd pull requestów", "P3", 14 * 60, 45, null, 1, emptyList()),
            t("Kupić prezent dla Zosi", "P2", 15 * 60, null, null, 2, listOf(2)),
            t("Trening — siłownia", "P4", 18 * 60 + 30, null, "WEEKLY", 3, emptyList())
        ))
    }
}

/** Ustawienia Supabase + sesja — przechowywane lokalnie (java.util.prefs). */
class SyncSettings {
    private val prefs = Preferences.userRoot().node("pl/media30/todoisto/desktop")
    var url: String get() = prefs.get("url", ""); set(v) = prefs.put("url", v)
    var anonKey: String get() = prefs.get("anonKey", ""); set(v) = prefs.put("anonKey", v)
    var email: String get() = prefs.get("email", ""); set(v) = prefs.put("email", v)
    val configured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
}

/** Wynik synchronizacji do pokazania w UI. */
data class SyncResult(val ok: Boolean, val message: String)

/**
 * Pełna synchronizacja: logowanie → pobierz z chmury (jeśli jest, nadpisz
 * lokalne) → wyślij aktualny stan. Tak samo jak backup/restore w Androidzie,
 * przez tę samą tabelę, więc dane latają Mac ↔ telefon.
 */
fun sync(repo: TaskRepository, settings: SyncSettings, password: String): SyncResult {
    if (!settings.configured) return SyncResult(false, "Uzupełnij URL i klucz anon Supabase.")
    val auth = SupabaseSync.signIn(settings.url, settings.anonKey, settings.email, password)
    if (auth.token == null || auth.userId == null) {
        return SyncResult(false, auth.error ?: "Logowanie nie powiodło się.")
    }
    // Bezpieczne scalanie: pobierz z chmury i POŁĄCZ z lokalnym (nic nie ginie),
    // zamiast nadpisywać którąkolwiek stronę.
    val remote = SupabaseSync.pull(settings.url, settings.anonKey, auth.token!!, auth.userId!!)
    val merged = if (remote != null) {
        runCatching { SnapshotMerge.merge(repo.snapshot(), CloudBackupCodec.fromJson(remote)) }
            .getOrDefault(repo.snapshot())
    } else repo.snapshot()
    repo.replaceAll(merged)
    val err = SupabaseSync.push(settings.url, settings.anonKey, auth.token!!, auth.userId!!, CloudBackupCodec.toJson(merged))
    return if (err == null) SyncResult(true, "Zsynchronizowano z chmurą ⭐")
    else SyncResult(false, "Scalone lokalnie, ale wysyłka nie wyszła: $err")
}
