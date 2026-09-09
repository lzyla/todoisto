package pl.media30.todoisto.data

import pl.media30.todoisto.shared.CloudActivity
import pl.media30.todoisto.shared.CloudArea
import pl.media30.todoisto.shared.CloudLabel
import pl.media30.todoisto.shared.CloudSection
import pl.media30.todoisto.shared.CloudProject
import pl.media30.todoisto.shared.CloudSnapshot
import pl.media30.todoisto.shared.CloudTask

/**
 * Mostek między encjami Room (androidowymi) a neutralnymi modelami z modułu
 * `shared`. Dzięki temu Android i desktop dzielą JEDEN kodek i logikę scalania,
 * a format chmury jest z definicji identyczny.
 */
object CloudMapper {

    fun toCloud(t: Task) = CloudTask(
        id = t.id, title = t.title, notes = t.notes, isCompleted = t.isCompleted,
        priority = t.priority.name, dueDate = t.dueDate, dueTimeMinutes = t.dueTimeMinutes,
        durationMinutes = t.durationMinutes, deadline = t.deadline, completedAt = t.completedAt,
        recurrence = t.recurrence?.name, reminderAt = t.reminderAt, projectId = t.projectId,
        sectionId = t.sectionId, parentId = t.parentId, areaId = t.areaId,
        labelIds = t.labelIds, attachments = t.attachments, position = t.position,
        createdAt = t.createdAt, updatedAt = t.updatedAt, deleted = t.deleted
    )

    fun fromCloud(c: CloudTask) = Task(
        id = c.id, title = c.title, notes = c.notes, isCompleted = c.isCompleted,
        priority = runCatching { Priority.valueOf(c.priority) }.getOrDefault(Priority.P4),
        dueDate = c.dueDate, dueTimeMinutes = c.dueTimeMinutes, durationMinutes = c.durationMinutes,
        deadline = c.deadline, completedAt = c.completedAt,
        recurrence = c.recurrence?.let { Recurrence.fromNameSafe(it) },
        reminderAt = c.reminderAt, projectId = c.projectId, sectionId = c.sectionId,
        parentId = c.parentId, areaId = c.areaId, labelIds = c.labelIds, attachments = c.attachments,
        position = c.position, createdAt = c.createdAt, updatedAt = c.updatedAt, deleted = c.deleted
    )

    fun projectToCloud(p: Project) = CloudProject(
        id = p.id, name = p.name, colorArgb = p.colorArgb, position = p.position,
        isFavorite = p.isFavorite, isArchived = p.isArchived,
        workEffortType = p.workEffortType?.name, areaId = p.areaId
    )

    fun projectFromCloud(c: CloudProject) = Project(
        id = c.id, name = c.name, colorArgb = c.colorArgb, position = c.position,
        isFavorite = c.isFavorite, isArchived = c.isArchived,
        workEffortType = c.workEffortType?.let { runCatching { EffortType.valueOf(it) }.getOrNull() },
        areaId = c.areaId
    )

    fun labelToCloud(l: Label) = CloudLabel(id = l.id, name = l.name, colorArgb = l.colorArgb, isFavorite = l.isFavorite)
    fun labelFromCloud(c: CloudLabel) = Label(id = c.id, name = c.name, colorArgb = c.colorArgb, isFavorite = c.isFavorite)

    fun areaToCloud(a: Area) = CloudArea(id = a.id, name = a.name, colorArgb = a.colorArgb, position = a.position)
    fun areaFromCloud(c: CloudArea) = Area(id = c.id, name = c.name, colorArgb = c.colorArgb, position = c.position)

    fun sectionToCloud(s: Section) = CloudSection(id = s.id, projectId = s.projectId, name = s.name, position = s.position)
    fun sectionFromCloud(c: CloudSection) = Section(id = c.id, projectId = c.projectId, name = c.name, position = c.position)

    fun activityToCloud(a: Activity) = CloudActivity(
        id = a.id, name = a.name, effortType = a.effortType.name, durationMinutes = a.durationMinutes,
        place = a.place.name, windowStartMin = a.windowStartMin, windowEndMin = a.windowEndMin,
        daysMask = a.daysMask, energyCost = a.energyCost.name, frequencyTarget = a.frequencyTarget,
        isActive = a.isActive, lastScheduledAt = a.lastScheduledAt, lastCompletedAt = a.lastCompletedAt,
        createdAt = a.createdAt
    )

    fun activityFromCloud(c: CloudActivity) = Activity(
        id = c.id, name = c.name,
        effortType = runCatching { EffortType.valueOf(c.effortType) }.getOrDefault(EffortType.MENTAL),
        durationMinutes = c.durationMinutes,
        place = runCatching { Place.valueOf(c.place) }.getOrDefault(Place.HOME),
        windowStartMin = c.windowStartMin, windowEndMin = c.windowEndMin, daysMask = c.daysMask,
        energyCost = runCatching { EnergyCost.valueOf(c.energyCost) }.getOrDefault(EnergyCost.MED),
        frequencyTarget = c.frequencyTarget, isActive = c.isActive,
        lastScheduledAt = c.lastScheduledAt, lastCompletedAt = c.lastCompletedAt, createdAt = c.createdAt
    )

    fun snapshot(
        tasks: List<Task>, projects: List<Project>, labels: List<Label>,
        areas: List<Area> = emptyList(), sections: List<Section> = emptyList(),
        activities: List<Activity> = emptyList()
    ) = CloudSnapshot(
        tasks = tasks.map(::toCloud), projects = projects.map(::projectToCloud), labels = labels.map(::labelToCloud),
        areas = areas.map(::areaToCloud), sections = sections.map(::sectionToCloud),
        activities = activities.map(::activityToCloud)
    )
}
