package pl.media30.todoisto.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Task::class, Project::class, Section::class, Label::class, Activity::class, Area::class, Tombstone::class],
    version = TodoDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun sectionDao(): SectionDao
    abstract fun labelDao(): LabelDao
    abstract fun activityDao(): ActivityDao
    abstract fun areaDao(): AreaDao
    abstract fun tombstoneDao(): TombstoneDao

    companion object {
        const val VERSION = 9
        const val NAME = "todoisto.db"

        /**
         * 8 → 9: znaczniki zmian dla nie-zadań (scalanie „nowszy wygrywa"),
         * tabela nagrobków, pola przekazania do Notion. Same ADD COLUMN /
         * CREATE TABLE — żadne istniejące dane nie są przepisywane.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("projects", "sections", "labels", "areas", "activities")) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `remoteUrl` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `routeState` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tombstones` (`kind` TEXT NOT NULL, `refId` INTEGER NOT NULL, " +
                        "`deletedAt` INTEGER NOT NULL, PRIMARY KEY(`kind`, `refId`))"
                )
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_8_9)

        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getInstance(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val app = context.applicationContext
                    // Kopia pliku bazy PRZED jakąkolwiek migracją — gdyby coś poszło
                    // nie tak, dane da się odzyskać (Ustawienia → Dane i kopie).
                    DbSafety.backupBeforeMigration(app, NAME, VERSION)
                    Room.databaseBuilder(app, TodoDatabase::class.java, NAME)
                        .addMigrations(*MIGRATIONS)
                        // Kasowanie dozwolone WYŁĄCZNIE dla prehistorycznych wersji (sprzed
                        // eksportu schematu). Brak migracji dla wersji ≥ 8 = błąd, a nie
                        // ciche wyczyszczenie danych.
                        .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }
    }
}
