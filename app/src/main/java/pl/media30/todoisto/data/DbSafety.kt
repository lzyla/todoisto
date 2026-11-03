package pl.media30.todoisto.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Lokalne siatki bezpieczeństwa danych (niezależne od chmury):
 *  1. kopia PLIKU bazy tuż przed migracją schematu (gdy wersja na dysku ≠ wersja w kodzie),
 *  2. codzienna kopia JSON (ta sama migawka co do chmury),
 *  3. lista kopii do wglądu / przywrócenia w Ustawieniach.
 * Kopie leżą w pamięci aplikacji — chronią przed błędem aktualizacji, NIE przed
 * utratą telefonu (na to: chmura + eksport do pliku).
 */
object DbSafety {

    private const val DB_BACKUPS = "db-backups"
    private const val JSON_BACKUPS = "json-backups"
    private const val KEEP_DB = 5
    private const val KEEP_JSON = 14

    fun dbBackupDir(ctx: Context) = File(ctx.filesDir, DB_BACKUPS).apply { mkdirs() }
    fun jsonBackupDir(ctx: Context) = File(ctx.filesDir, JSON_BACKUPS).apply { mkdirs() }

    /** Wersja schematu zapisana w pliku (PRAGMA user_version) albo null, gdy bazy nie ma. */
    fun onDiskVersion(ctx: Context, name: String): Int? {
        val f = ctx.getDatabasePath(name)
        if (!f.exists()) return null
        return runCatching {
            SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        }.getOrNull()
    }

    /**
     * Kopiuje plik bazy (z -wal/-shm) zanim Room zacznie migrację. Nie rzuca
     * wyjątków — brak kopii nie może zablokować startu aplikacji.
     */
    fun backupBeforeMigration(ctx: Context, name: String, codeVersion: Int) {
        runCatching {
            val disk = onDiskVersion(ctx, name) ?: return
            if (disk == codeVersion) return
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val dir = File(dbBackupDir(ctx), "v$disk-to-v$codeVersion-$stamp").apply { mkdirs() }
            val main = ctx.getDatabasePath(name)
            for (suffix in listOf("", "-wal", "-shm")) {
                val src = File(main.path + suffix)
                if (src.exists()) src.copyTo(File(dir, src.name), overwrite = true)
            }
            prune(dbBackupDir(ctx), KEEP_DB)
        }
    }

    /** Raz dziennie zapisuje migawkę JSON (te same dane co w chmurze). */
    suspend fun dailyJsonBackup(ctx: Context, repo: TaskRepository) {
        runCatching {
            val f = File(jsonBackupDir(ctx), "todoisto-${LocalDate.now()}.json")
            if (f.exists()) return
            f.writeText(repo.exportBackupJson())
            prune(jsonBackupDir(ctx), KEEP_JSON)
        }
    }

    fun listJsonBackups(ctx: Context): List<File> =
        jsonBackupDir(ctx).listFiles { f -> f.name.endsWith(".json") }?.sortedByDescending { it.name }.orEmpty()

    fun listDbBackups(ctx: Context): List<File> =
        dbBackupDir(ctx).listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }.orEmpty()

    private fun prune(dir: File, keep: Int) {
        dir.listFiles()?.sortedByDescending { it.name }?.drop(keep)?.forEach { it.deleteRecursively() }
    }
}
