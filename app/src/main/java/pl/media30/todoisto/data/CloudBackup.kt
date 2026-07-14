package pl.media30.todoisto.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializacja/deserializacja danych do kopii w chmurze (JSON). Obejmuje
 * zadania, projekty i etykiety — czyli sedno danych użytkownika. Format jest
 * płaski i wersjonowany, żeby dało się go rozwijać.
 */
object CloudBackup {

    const val VERSION = 1

    fun toJson(tasks: List<Task>, projects: List<Project>, labels: List<Label>): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("tasks", JSONArray().apply { tasks.forEach { put(taskToJson(it)) } })
        root.put("projects", JSONArray().apply { projects.forEach { put(projectToJson(it)) } })
        root.put("labels", JSONArray().apply { labels.forEach { put(labelToJson(it)) } })
        return root.toString()
    }

    data class Parsed(val tasks: List<Task>, val projects: List<Project>, val labels: List<Label>)

    fun fromJson(json: String): Parsed {
        val root = JSONObject(json)
        val tasks = root.optJSONArray("tasks")?.let { arr ->
            (0 until arr.length()).map { taskFromJson(arr.getJSONObject(it)) }
        } ?: emptyList()
        val projects = root.optJSONArray("projects")?.let { arr ->
            (0 until arr.length()).map { projectFromJson(arr.getJSONObject(it)) }
        } ?: emptyList()
        val labels = root.optJSONArray("labels")?.let { arr ->
            (0 until arr.length()).map { labelFromJson(arr.getJSONObject(it)) }
        } ?: emptyList()
        return Parsed(tasks, projects, labels)
    }

    // ---- helpers ----
    private fun JSONObject.putNullableLong(key: String, v: Long?) { if (v == null) put(key, JSONObject.NULL) else put(key, v) }
    private fun JSONObject.putNullableInt(key: String, v: Int?) { if (v == null) put(key, JSONObject.NULL) else put(key, v) }
    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.optNullableInt(key: String): Int? = if (isNull(key)) null else optInt(key)

    private fun taskToJson(t: Task): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("notes", t.notes)
        put("isCompleted", t.isCompleted)
        put("priority", t.priority.name)
        putNullableLong("dueDate", t.dueDate)
        putNullableInt("dueTimeMinutes", t.dueTimeMinutes)
        putNullableInt("durationMinutes", t.durationMinutes)
        putNullableLong("deadline", t.deadline)
        putNullableLong("completedAt", t.completedAt)
        put("recurrence", t.recurrence?.name ?: JSONObject.NULL)
        putNullableLong("reminderAt", t.reminderAt)
        putNullableLong("projectId", t.projectId)
        putNullableLong("sectionId", t.sectionId)
        putNullableLong("parentId", t.parentId)
        putNullableLong("areaId", t.areaId)
        put("labelIds", JSONArray().apply { t.labelIds.forEach { put(it) } })
        put("attachments", JSONArray().apply { t.attachments.forEach { put(it) } })
        put("position", t.position)
        put("createdAt", t.createdAt)
    }

    private fun taskFromJson(o: JSONObject): Task = Task(
        id = o.optLong("id"),
        title = o.optString("title"),
        notes = o.optString("notes"),
        isCompleted = o.optBoolean("isCompleted"),
        priority = runCatching { Priority.valueOf(o.optString("priority", "P4")) }.getOrDefault(Priority.P4),
        dueDate = o.optNullableLong("dueDate"),
        dueTimeMinutes = o.optNullableInt("dueTimeMinutes"),
        durationMinutes = o.optNullableInt("durationMinutes"),
        deadline = o.optNullableLong("deadline"),
        completedAt = o.optNullableLong("completedAt"),
        recurrence = if (o.isNull("recurrence")) null else Recurrence.fromNameSafe(o.optString("recurrence")),
        reminderAt = o.optNullableLong("reminderAt"),
        projectId = o.optNullableLong("projectId"),
        sectionId = o.optNullableLong("sectionId"),
        parentId = o.optNullableLong("parentId"),
        areaId = o.optNullableLong("areaId"),
        labelIds = o.optJSONArray("labelIds")?.let { a -> (0 until a.length()).map { a.getLong(it) } } ?: emptyList(),
        attachments = o.optJSONArray("attachments")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        position = o.optInt("position"),
        createdAt = o.optLong("createdAt")
    )

    private fun projectToJson(p: Project): JSONObject = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("colorArgb", p.colorArgb)
        put("position", p.position); put("isFavorite", p.isFavorite); put("isArchived", p.isArchived)
        put("workEffortType", p.workEffortType?.name ?: JSONObject.NULL)
        putNullableLong("areaId", p.areaId)
    }

    private fun projectFromJson(o: JSONObject): Project = Project(
        id = o.optLong("id"),
        name = o.optString("name"),
        colorArgb = o.optLong("colorArgb", 0xFF9B6BFFL),
        position = o.optInt("position"),
        isFavorite = o.optBoolean("isFavorite"),
        isArchived = o.optBoolean("isArchived"),
        workEffortType = if (o.isNull("workEffortType")) null else runCatching { EffortType.valueOf(o.optString("workEffortType")) }.getOrNull(),
        areaId = o.optNullableLong("areaId")
    )

    private fun labelToJson(l: Label): JSONObject = JSONObject().apply {
        put("id", l.id); put("name", l.name); put("colorArgb", l.colorArgb); put("isFavorite", l.isFavorite)
    }

    private fun labelFromJson(o: JSONObject): Label = Label(
        id = o.optLong("id"),
        name = o.optString("name"),
        colorArgb = o.optLong("colorArgb", 0xFF4D6BFFL),
        isFavorite = o.optBoolean("isFavorite")
    )
}
