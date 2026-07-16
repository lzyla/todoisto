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
        TaskRepository(db.taskDao(), db.projectDao(), db.sectionDao(), db.labelDao(), db.activityDao(), db.areaDao())
    }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val account: pl.media30.todoisto.data.AccountStore by lazy { pl.media30.todoisto.data.AccountStore(this) }
    val cloud: pl.media30.todoisto.data.CloudStore by lazy { pl.media30.todoisto.data.CloudStore(this) }

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
        }
    }
}
