package pl.media30.todoisto.shared

/**
 * Bezpieczne scalanie dwóch migawek (lokalna + z chmury) — zamiast nadpisywać
 * całość, łączymy po `id`, żeby NIC NIE GINĘŁO:
 *  - zadanie tylko po jednej stronie → zostaje,
 *  - zadanie po obu stronach → wygrywa nowsze (znacznik czasu),
 *  - projekty/etykiety → suma po `id` (przy konflikcie preferujemy chmurę,
 *    bo projektami zwykle zarządza telefon).
 *
 * Uwaga: to scalanie „bez nagrobków" — świadome usunięcie zadania na jednym
 * urządzeniu może wrócić po synchronizacji z drugiego, które go jeszcze ma.
 * Prawdziwe usuwania (tombstones) to następny krok.
 */
object SnapshotMerge {

    /** Proxy „ostatniego dotknięcia": max z dostępnych znaczników czasu. */
    private fun touchedAt(t: CloudTask): Long =
        maxOf(t.updatedAt, t.completedAt ?: 0L, t.createdAt)

    fun merge(local: CloudSnapshot, remote: CloudSnapshot): CloudSnapshot {
        val tasks = LinkedHashMap<Long, CloudTask>()
        for (t in local.tasks) tasks[t.id] = t
        for (t in remote.tasks) {
            val cur = tasks[t.id]
            tasks[t.id] = if (cur == null) t
            else if (touchedAt(t) >= touchedAt(cur)) t else cur
        }

        val projects = LinkedHashMap<Long, CloudProject>()
        for (p in local.projects) projects[p.id] = p
        for (p in remote.projects) projects[p.id] = p   // konflikt → chmura wygrywa

        val labels = LinkedHashMap<Long, CloudLabel>()
        for (l in local.labels) labels[l.id] = l
        for (l in remote.labels) labels[l.id] = l

        val areas = LinkedHashMap<Long, CloudArea>()
        for (a in local.areas) areas[a.id] = a
        for (a in remote.areas) areas[a.id] = a

        val sections = LinkedHashMap<Long, CloudSection>()
        for (s in local.sections) sections[s.id] = s
        for (s in remote.sections) sections[s.id] = s

        // Aktywności: suma po id; przy konflikcie nowsza wg lastScheduledAt/lastCompletedAt/createdAt.
        val activities = LinkedHashMap<Long, CloudActivity>()
        fun touchedAct(a: CloudActivity) = maxOf(a.lastScheduledAt ?: 0L, a.lastCompletedAt ?: 0L, a.createdAt)
        for (a in local.activities) activities[a.id] = a
        for (a in remote.activities) {
            val cur = activities[a.id]
            activities[a.id] = if (cur == null || touchedAct(a) >= touchedAct(cur)) a else cur
        }

        return CloudSnapshot(
            tasks = tasks.values.toList(),
            projects = projects.values.toList(),
            labels = labels.values.toList(),
            areas = areas.values.toList(),
            sections = sections.values.toList(),
            activities = activities.values.toList()
        )
    }
}
