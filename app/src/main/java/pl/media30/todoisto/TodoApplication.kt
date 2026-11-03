package pl.media30.todoisto

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.media30.todoisto.data.DemoSeeder
import pl.media30.todoisto.data.SettingsStore
import pl.media30.todoisto.data.TaskRepository
import pl.media30.todoisto.data.TodoDatabase

class TodoApplication : Application() {
    private val db by lazy { TodoDatabase.getInstance(this) }
    val repository: TaskRepository by lazy {
        TaskRepository(db.taskDao(), db.projectDao(), db.sectionDao(), db.labelDao(), db.activityDao(), db.areaDao(), db.tombstoneDao()).also { repo ->
            // Każdy zapis zadania → alarm zgodny z danymi; dane z innego urządzenia → odtwórz alarmy.
            repo.onReminderChanged = { t -> runCatching { pl.media30.todoisto.data.Reminders.sync(this, t) } }
            repo.routingPolicy = { t -> routing.shouldRoute(t) }
            repo.onRoutingQueued = { pl.media30.todoisto.data.RouteWorker.enqueue(this) }
            repo.onImported = { appScope.launch { runCatching { pl.media30.todoisto.data.Reminders.rescheduleAll(this@TodoApplication) } } }
        }
    }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val account: pl.media30.todoisto.data.AccountStore by lazy { pl.media30.todoisto.data.AccountStore(this) }
    val cloud: pl.media30.todoisto.data.CloudStore by lazy { pl.media30.todoisto.data.CloudStore(this) }
    val routing: pl.media30.todoisto.data.RoutingStore by lazy { pl.media30.todoisto.data.RoutingStore(this, cloud) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Konto demo (zalogowane) + bogate dane demonstracyjne przy pierwszym uruchomieniu.
        account.seedDemoIfEmpty()
        appScope.launch { DemoSeeder.seedIfEmpty(db, settings) }
        // Przypomnienia w miejscu: cykliczny worker + sprawdzenie na starcie.
        runCatching { pl.media30.todoisto.data.GeoReminders.schedule(this) }
        appScope.launch {
            runCatching {
                pl.media30.todoisto.data.GeoReminders.check(this@TodoApplication, db.taskDao().getWithLocation())
            }
            // Przypomnienia czasowe — odtwórz przyszłe alarmy (np. po aktualizacji apki).
            runCatching { pl.media30.todoisto.data.Reminders.rescheduleAll(this@TodoApplication) }
            // Przekazywanie do Notion: odśwież konfigurację i dokończ zaległą kolejkę.
            if (cloud.signedIn) runCatching { routing.refresh() }
            if (repository.pendingRoutes().isNotEmpty()) pl.media30.todoisto.data.RouteWorker.enqueue(this@TodoApplication)
            // Codzienna lokalna kopia JSON (siatka bezpieczeństwa niezależna od chmury).
            pl.media30.todoisto.data.DbSafety.dailyJsonBackup(this@TodoApplication, repository)
        }
    }
}
