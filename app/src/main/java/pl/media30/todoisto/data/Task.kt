package pl.media30.todoisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single to-do item.
 *
 * @param dueDate stored as epoch-day (days since 1970-01-01), or null when no due date.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.P4,
    val dueDate: Long? = null,
    val createdAt: Long = 0L
)
