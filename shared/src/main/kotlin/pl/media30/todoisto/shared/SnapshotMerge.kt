package pl.media30.todoisto.shared

/**
 * Bezpieczne scalanie dwóch migawek (lokalna + z chmury) — zamiast nadpisywać
 * całość, łączymy po `id`, żeby nic nie ginęło:
 *  - rekord tylko po jednej stronie → zostaje,
 *  - rekord po obu stronach → wygrywa nowszy (`updatedAt`); przy remisie chmura
 *    (zachowuje dawne zachowanie dla danych bez znaczników czasu),
 *  - zadania usuwane są nagrobkiem w samym zadaniu (`deleted`),
 *  - pozostałe typy — nagrobkiem w [CloudSnapshot.tombstones]; nagrobek wygrywa
 *    z rekordem, chyba że rekord zmieniono PO usunięciu (świadome przywrócenie).
 */
object SnapshotMerge {

    /** Jak długo trzymamy nagrobki innych rekordów (dłużej niż zadań: rzadkie, małe). */
    const val TOMBSTONE_TTL_MS = 90L * 24 * 60 * 60 * 1000

    /** Proxy „ostatniego dotknięcia": max z dostępnych znaczników czasu. */
    private fun touchedAt(t: CloudTask): Long =
        maxOf(t.updatedAt, t.completedAt ?: 0L, t.createdAt)

    private fun touchedAct(a: CloudActivity): Long =
        maxOf(a.updatedAt, a.lastScheduledAt ?: 0L, a.lastCompletedAt ?: 0L, a.createdAt)

    fun merge(local: CloudSnapshot, remote: CloudSnapshot, now: Long = System.currentTimeMillis()): CloudSnapshot {
        val tasks = LinkedHashMap<Long, CloudTask>()
        for (t in local.tasks) tasks[t.id] = t
        for (t in remote.tasks) {
            val cur = tasks[t.id]
            tasks[t.id] = if (cur == null) t
            else if (touchedAt(t) >= touchedAt(cur)) t.copy(remoteUrl = t.remoteUrl ?: cur.remoteUrl)
            else cur.copy(remoteUrl = cur.remoteUrl ?: t.remoteUrl)
        }

        // Nagrobki: suma, przy duplikacie najpóźniejsze usunięcie; stare odcinamy.
        val tombs = LinkedHashMap<Pair<String, Long>, CloudTombstone>()
        for (tb in local.tombstones + remote.tombstones) {
            if (now - tb.deletedAt > TOMBSTONE_TTL_MS) continue
            val key = tb.kind to tb.id
            val cur = tombs[key]
            if (cur == null || tb.deletedAt > cur.deletedAt) tombs[key] = tb
        }
        fun alive(kind: String, id: Long, updatedAt: Long): Boolean {
            val tb = tombs[kind to id] ?: return true
            return updatedAt > tb.deletedAt
        }

        val projects = lww(local.projects, remote.projects, { it.id }, { it.updatedAt })
            .filter { alive(CloudTombstone.PROJECT, it.id, it.updatedAt) }
        val labels = lww(local.labels, remote.labels, { it.id }, { it.updatedAt })
            .filter { alive(CloudTombstone.LABEL, it.id, it.updatedAt) }
        val areas = lww(local.areas, remote.areas, { it.id }, { it.updatedAt })
            .filter { alive(CloudTombstone.AREA, it.id, it.updatedAt) }
        // Sekcje usuwanego projektu znikają razem z nim; innych „sierot" nie ruszamy
        // (ostrożnie: brak projektu w migawce nie musi znaczyć, że go usunięto).
        val liveProjectIds = projects.map { it.id }.toSet()
        val deletedProjectIds = tombs.values.filter { it.kind == CloudTombstone.PROJECT }.map { it.id }.toSet() - liveProjectIds
        val sections = lww(local.sections, remote.sections, { it.id }, { it.updatedAt })
            .filter { alive(CloudTombstone.SECTION, it.id, it.updatedAt) && it.projectId !in deletedProjectIds }
        val activities = lww(local.activities, remote.activities, { it.id }, ::touchedAct)
            .filter { alive(CloudTombstone.ACTIVITY, it.id, touchedAct(it)) }

        return CloudSnapshot(
            tasks = tasks.values.toList(),
            projects = projects,
            labels = labels,
            areas = areas,
            sections = sections,
            activities = activities,
            tombstones = tombs.values.toList()
        )
    }

    /** Last-writer-wins po id; remis → strona zdalna (jak dotychczas „chmura wygrywa"). */
    private fun <T> lww(local: List<T>, remote: List<T>, id: (T) -> Long, ts: (T) -> Long): List<T> {
        val out = LinkedHashMap<Long, T>()
        for (x in local) out[id(x)] = x
        for (x in remote) {
            val cur = out[id(x)]
            out[id(x)] = if (cur == null || ts(x) >= ts(cur)) x else cur
        }
        return out.values.toList()
    }
}
