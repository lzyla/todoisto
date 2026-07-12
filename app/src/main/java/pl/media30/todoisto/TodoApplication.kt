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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Dane demonstracyjne przy pierwszym uruchomieniu (żeby od razu było widać w akcji).
        appScope.launch { DemoSeeder.seedIfEmpty(db, settings) }
    }
}
