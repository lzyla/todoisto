package pl.media30.todoisto.desktop

import pl.media30.todoisto.data.Recurrence
import pl.media30.todoisto.shared.CloudBackupCodec
import pl.media30.todoisto.shared.CloudLabel
import pl.media30.todoisto.shared.CloudProject
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
    private val _projects = mutableListOf<CloudProject>()
    private val _labels = mutableListOf<CloudLabel>()
    private var nextId = System.currentTimeMillis()

    // Widoczne zadania: bez nagrobków (usunięte kryją się, ale zostają w migawce).
    val tasks: List<CloudTask> get() = _tasks.filter { !it.deleted }.sortedBy { it.position }
    val projects: List<CloudProject> get() = _projects.sortedBy { it.position }
    val labels: List<CloudLabel> get() = _labels.toList()
    fun projectName(id: Long?): String = id?.let { pid -> _projects.firstOrNull { it.id == pid }?.name } ?: "Skrzynka"
    fun labelName(id: Long): String = _labels.firstOrNull { it.id == id }?.name ?: "etykieta"

    init { load() }

    fun add(task: CloudTask): CloudTask {
        val now = System.currentTimeMillis()
        val withId = task.copy(id = nextId++, position = _tasks.size, createdAt = now, updatedAt = now)
        _tasks.add(withId); save(); return withId
    }

    fun toggle(id: Long) {
        val i = _tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val t = _tasks[i]
        val now = System.currentTimeMillis()
        // Zadanie cykliczne po odhaczeniu NIE znika — przechodzi na kolejny termin.
        val rec = t.recurrence?.let { runCatching { Recurrence.valueOf(it) }.getOrNull() }
        _tasks[i] = if (!t.isCompleted && rec != null && t.dueDate != null) {
            t.copy(dueDate = rec.next(t.dueDate!!), updatedAt = now)
        } else {
            t.copy(isCompleted = !t.isCompleted, completedAt = if (!t.isCompleted) now else null, updatedAt = now)
        }
        save()
    }

    /** Zapisuje edycję zadania (szczegóły). */
    fun update(task: CloudTask) {
        val i = _tasks.indexOfFirst { it.id == task.id }
        if (i >= 0) { _tasks[i] = task.copy(updatedAt = System.currentTimeMillis()); save() }
    }

    /** Usuwa jako nagrobek — zostaje w migawce, żeby usunięcie się zsynchronizowało. */
    fun delete(id: Long) {
        val i = _tasks.indexOfFirst { it.id == id }
        if (i >= 0) { _tasks[i] = _tasks[i].copy(deleted = true, updatedAt = System.currentTimeMillis()); save() }
    }

    /** Podmienia całe dane migawką z chmury (po scaleniu). */
    fun replaceAll(snapshot: CloudSnapshot) {
        _tasks.clear(); _tasks.addAll(snapshot.tasks)
        _projects.clear(); _projects.addAll(snapshot.projects)
        _labels.clear(); _labels.addAll(snapshot.labels)
        nextId = (_tasks.maxOfOrNull { it.id } ?: 0L) + 1
        save()
    }

    fun snapshot(): CloudSnapshot = CloudSnapshot(tasks = _tasks.toList(), projects = _projects.toList(), labels = _labels.toList())

    private fun load() {
        runCatching {
            if (dataFile.exists()) {
                val snap = CloudBackupCodec.fromJson(dataFile.readText())
                _tasks.clear(); _tasks.addAll(snap.tasks)
                _projects.clear(); _projects.addAll(snap.projects)
                _labels.clear(); _labels.addAll(snap.labels)
                purgeOldTombstones()
                nextId = (_tasks.maxOfOrNull { it.id } ?: 0L) + 1
            }
        }
        // Celowo BEZ danych przykładowych: prawdziwe zadania przychodzą z chmury
        // (albo dodajesz je sam). Demo zaśmiecałoby synchronizację z telefonem.
    }

    /** Kasuje stare nagrobki (>30 dni), żeby migawka nie puchła w nieskończoność. */
    private fun purgeOldTombstones() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        _tasks.removeAll { it.deleted && it.updatedAt in 1 until cutoff }
    }

    private fun save() {
        runCatching {
            dataFile.parentFile.mkdirs()
            dataFile.writeText(CloudBackupCodec.toJson(snapshot()))
        }
    }
}

/** Ustawienia Supabase + sesja — przechowywane lokalnie (java.util.prefs). */
class SyncSettings {
    private val prefs = Preferences.userRoot().node("pl/media30/todoisto/desktop")
    var url: String get() = prefs.get("url", ""); set(v) = prefs.put("url", v)
    var anonKey: String get() = prefs.get("anonKey", ""); set(v) = prefs.put("anonKey", v)
    var email: String get() = prefs.get("email", ""); set(v) = prefs.put("email", v)
    var openAiKey: String get() = prefs.get("openai", ""); set(v) = prefs.put("openai", v)
    var themeId: String get() = prefs.get("theme", "violet"); set(v) = prefs.put("theme", v)
    var gmailUser: String get() = prefs.get("gmailUser", ""); set(v) = prefs.put("gmailUser", v)
    var gmailPass: String get() = prefs.get("gmailPass", ""); set(v) = prefs.put("gmailPass", v)
    private var gmailDone: String get() = prefs.get("gmailDone", ""); set(v) = prefs.put("gmailDone", v)
    fun gmailProcessed(): Set<String> = gmailDone.split("|").filter { it.isNotBlank() }.toSet()
    fun gmailMarkProcessed(ids: Collection<String>) {
        gmailDone = (gmailProcessed() + ids).toList().takeLast(500).joinToString("|")
    }
    val configured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
}

/**
 * Powiadomienia macOS (pasek menu) dla przypomnień. Działa też dla zadań z
 * `reminderAt` ustawionym na telefonie — po synchronizacji Mac przypomni.
 * Wszystko w try/catch, żeby nigdy nie wywalić aplikacji.
 */
object DesktopNotifier {
    private var tray: java.awt.TrayIcon? = null

    private fun ensure() {
        if (tray != null || !java.awt.SystemTray.isSupported()) return
        runCatching {
            val img = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            g.color = java.awt.Color(0x6B, 0x3F, 0xE0); g.fillOval(2, 2, 12, 12); g.dispose()
            val t = java.awt.TrayIcon(img, "Todoisto").apply { isImageAutoSize = true }
            java.awt.SystemTray.getSystemTray().add(t); tray = t
        }
    }

    fun notify(title: String, text: String) {
        ensure()
        runCatching { tray?.displayMessage(title, text, java.awt.TrayIcon.MessageType.INFO) }
    }
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

/** Gmail: maile z gwiazdką → zadania (IMAP, hasło do aplikacji). */
suspend fun fetchGmail(repo: TaskRepository, settings: SyncSettings): String {
    val user = settings.gmailUser; val pass = settings.gmailPass
    if (user.isBlank() || pass.isBlank()) return "Uzupełnij dane Gmail w Ustawieniach."
    return try {
        val mails = pl.media30.todoisto.data.GmailClient.fetchStarred(user, pass)
        val done = settings.gmailProcessed()
        val fresh = mails.filter { it.messageId !in done }
        fresh.forEach { m ->
            repo.add(CloudTask(id = 0, title = m.subject.take(200), notes = "✉️ Od: ${m.from}", attachments = listOf(m.gmailLink)))
        }
        settings.gmailMarkProcessed(fresh.map { it.messageId })
        if (fresh.isEmpty()) "Brak nowych maili z gwiazdką." else "Dodano ${fresh.size} zadań z Gmaila ⭐"
    } catch (e: Exception) {
        pl.media30.todoisto.data.GmailClient.friendlyError(e)
    }
}
