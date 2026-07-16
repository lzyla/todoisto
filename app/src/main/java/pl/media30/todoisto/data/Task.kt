package pl.media30.todoisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single to-do item.
 *
 * Dates are stored as epoch-day (days since 1970-01-01); [dueTimeMinutes] is
 * minutes from midnight. [projectId] null means the Inbox. [parentId] non-null
 * marks a subtask. [labelIds] is a compact list persisted via a type converter.
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
    val dueTimeMinutes: Int? = null,
    val durationMinutes: Int? = null,
    val deadline: Long? = null,
    val completedAt: Long? = null,
    val recurrence: Recurrence? = null,
    val reminderAt: Long? = null,
    val projectId: Long? = null,
    val sectionId: Long? = null,
    val parentId: Long? = null,
    /** Obszar dla zadań bez projektu (Skrzynka); zadania z projektem dziedziczą obszar projektu. */
    val areaId: Long? = null,
    val labelIds: List<Long> = emptyList(),
    /** Attachment URLs — image links render as thumbnails, the rest as link rows. */
    val attachments: List<String> = emptyList(),
    /** Przypomnienie w miejscu: współrzędne + nazwa (promień 300 m). */
    val locLat: Double? = null,
    val locLon: Double? = null,
    val locName: String? = null,
    val position: Int = 0,
    val createdAt: Long = 0L
)
