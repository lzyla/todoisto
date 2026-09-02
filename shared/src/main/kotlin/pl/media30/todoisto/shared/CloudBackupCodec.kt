package pl.media30.todoisto.shared

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializacja migawki do/z JSON — format **zgodny bajt-w-bajt** z blobem, który
 * zapisuje aplikacja Android (`CloudBackup`, version = 1). Dzięki temu Mac i
 * telefon czytają nawzajem swoje dane przez tę samą chmurę.
 */
object CloudBackupCodec {

    const val VERSION = 1

    fun toJson(s: CloudSnapshot): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("tasks", JSONArray().apply { s.tasks.forEach { put(taskToJson(it)) } })
        root.put("projects", JSONArray().apply { s.projects.forEach { put(projectToJson(it)) } })
        root.put("labels", JSONArray().apply { s.labels.forEach { put(labelToJson(it)) } })
        return root.toString()
    }

    fun fromJson(json: String): CloudSnapshot {
        val root = JSONObject(json)
        val tasks = root.optJSONArray("tasks")?.let { a -> (0 until a.length()).map { taskFromJson(a.getJSONObject(it)) } } ?: emptyList()
        val projects = root.optJSONArray("projects")?.let { a -> (0 until a.length()).map { projectFromJson(a.getJSONObject(it)) } } ?: emptyList()
        val labels = root.optJSONArray("labels")?.let { a -> (0 until a.length()).map { labelFromJson(a.getJSONObject(it)) } } ?: emptyList()
        return CloudSnapshot(tasks, projects, labels)
    }

    // ---- helpers (te same konwencje co w wersji Android) ----
    private fun JSONObject.putNullableLong(key: String, v: Long?) { if (v == null) put(key, JSONObject.NULL) else put(key, v) }
    private fun JSONObject.putNullableInt(key: String, v: Int?) { if (v == null) put(key, JSONObject.NULL) else put(key, v) }
    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.optNullableInt(key: String): Int? = if (isNull(key)) null else optInt(key)

    private fun taskToJson(t: CloudTask): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("notes", t.notes)
        put("isCompleted", t.isCompleted)
        put("priority", t.priority)
        putNullableLong("dueDate", t.dueDate)
        putNullableInt("dueTimeMinutes", t.dueTimeMinutes)
        putNullableInt("durationMinutes", t.durationMinutes)
        putNullableLong("deadline", t.deadline)
        putNullableLong("completedAt", t.completedAt)
        put("recurrence", t.recurrence ?: JSONObject.NULL)
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

    private fun taskFromJson(o: JSONObject): CloudTask = CloudTask(
        id = o.optLong("id"),
        title = o.optString("title"),
        notes = o.optString("notes"),
        isCompleted = o.optBoolean("isCompleted"),
        priority = o.optString("priority", "P4"),
        dueDate = o.optNullableLong("dueDate"),
        dueTimeMinutes = o.optNullableInt("dueTimeMinutes"),
        durationMinutes = o.optNullableInt("durationMinutes"),
        deadline = o.optNullableLong("deadline"),
        completedAt = o.optNullableLong("completedAt"),
        recurrence = if (o.isNull("recurrence")) null else o.optString("recurrence").ifBlank { null },
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

    private fun projectToJson(p: CloudProject): JSONObject = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("colorArgb", p.colorArgb)
        put("position", p.position); put("isFavorite", p.isFavorite); put("isArchived", p.isArchived)
        put("workEffortType", p.workEffortType ?: JSONObject.NULL)
        putNullableLong("areaId", p.areaId)
    }

    private fun projectFromJson(o: JSONObject): CloudProject = CloudProject(
        id = o.optLong("id"),
        name = o.optString("name"),
        colorArgb = o.optLong("colorArgb", 0xFF9B6BFFL),
        position = o.optInt("position"),
        isFavorite = o.optBoolean("isFavorite"),
        isArchived = o.optBoolean("isArchived"),
        workEffortType = if (o.isNull("workEffortType")) null else o.optString("workEffortType").ifBlank { null },
        areaId = o.optNullableLong("areaId")
    )

    private fun labelToJson(l: CloudLabel): JSONObject = JSONObject().apply {
        put("id", l.id); put("name", l.name); put("colorArgb", l.colorArgb); put("isFavorite", l.isFavorite)
    }

    private fun labelFromJson(o: JSONObject): CloudLabel = CloudLabel(
        id = o.optLong("id"),
        name = o.optString("name"),
        colorArgb = o.optLong("colorArgb", 0xFF4D6BFFL),
        isFavorite = o.optBoolean("isFavorite")
    )
}
