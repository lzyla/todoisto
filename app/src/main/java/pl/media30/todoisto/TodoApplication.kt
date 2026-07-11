package pl.media30.todoisto

import android.app.Application
import pl.media30.todoisto.data.TaskRepository
import pl.media30.todoisto.data.TodoDatabase

class TodoApplication : Application() {
    val repository: TaskRepository by lazy {
        val db = TodoDatabase.getInstance(this)
        TaskRepository(db.taskDao(), db.projectDao(), db.sectionDao(), db.labelDao())
    }
}
