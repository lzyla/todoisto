package pl.media30.todoisto.shared

/**
 * Neutralne modele danych do synchronizacji chmurowej — bez Compose i bez
 * Androida (priorytet/rekurencja jako String, dokładnie jak w blobie backupu).
 * Dzięki temu ten sam moduł działa i na Androidzie, i na desktopie, a format
 * jest zgodny z tym, co zapisuje już aplikacja mobilna.
 */
data class CloudTask(
    val id: Long,
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "P4",
    val dueDate: Long? = null,
    val dueTimeMinutes: Int? = null,
    val durationMinutes: Int? = null,
    val deadline: Long? = null,
    val completedAt: Long? = null,
    val recurrence: String? = null,
    val reminderAt: Long? = null,
    val projectId: Long? = null,
    val sectionId: Long? = null,
    val parentId: Long? = null,
    val areaId: Long? = null,
    val labelIds: List<Long> = emptyList(),
    val attachments: List<String> = emptyList(),
    val position: Int = 0,
    val createdAt: Long = 0L,
    /**
     * Znacznik ostatniej modyfikacji (ms) — używany przy scalaniu, żeby przy
     * konflikcie wygrała nowsza wersja. Pole dodatkowe: wersja Android go nie
     * zapisuje, więc dla zadań z telefonu bywa 0 — scalanie ma wtedy fallback
     * na completedAt/createdAt.
     */
    val updatedAt: Long = 0L,
    /**
     * Nagrobek (tombstone): true = zadanie usunięte. Zostaje w migawce jako
     * ślad, żeby usunięcie propagowało się przy synchronizacji (a nie wracało
     * z drugiego urządzenia). Pole dodatkowe, zgodne wstecz.
     */
    val deleted: Boolean = false,
    /** Link do kopii zadania w narzędziu docelowym (np. strona w Notion), gdy zostało przekazane. */
    val remoteUrl: String? = null
)

data class CloudProject(
    val id: Long,
    val name: String,
    val colorArgb: Long = 0xFF9B6BFFL,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val workEffortType: String? = null,
    val areaId: Long? = null,
    val updatedAt: Long = 0L
)

data class CloudLabel(
    val id: Long,
    val name: String,
    val colorArgb: Long = 0xFF4D6BFFL,
    val isFavorite: Boolean = false,
    val updatedAt: Long = 0L
)

/** Obszar (np. Praca / Dom) — grupuje projekty i zadania bez projektu. */
data class CloudArea(
    val id: Long,
    val name: String,
    val colorArgb: Long = 0xFF9B6BFFL,
    val position: Int = 0,
    val updatedAt: Long = 0L
)

/** Sekcja w projekcie (jak w Todoist). */
data class CloudSection(
    val id: Long,
    val projectId: Long,
    val name: String,
    val position: Int = 0,
    val updatedAt: Long = 0L
)

/** Aktywność z „Puli aktywności" (pomysły na wolny czas). Enumy jako String. */
data class CloudActivity(
    val id: Long,
    val name: String,
    val effortType: String = "PHYSICAL",
    val durationMinutes: Int = 30,
    val place: String = "HOME",
    val windowStartMin: Int? = null,
    val windowEndMin: Int? = null,
    val daysMask: Int = 0b1111111,
    val energyCost: String = "MED",
    val frequencyTarget: Int? = null,
    val isActive: Boolean = true,
    val lastScheduledAt: Long? = null,
    val lastCompletedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * Nagrobek dla rekordów innych niż zadania (zadania mają własne pole `deleted`).
 * Usunięcie projektu/etykiety/sekcji/obszaru/aktywności na jednym urządzeniu
 * musi wygrać z kopią, która wciąż leży na drugim — inaczej rekord „wraca".
 */
data class CloudTombstone(
    val kind: String,
    val id: Long,
    val deletedAt: Long
) {
    companion object {
        const val PROJECT = "project"
        const val LABEL = "label"
        const val SECTION = "section"
        const val AREA = "area"
        const val ACTIVITY = "activity"
    }
}

/**
 * Migawka danych użytkownika (to, co ląduje w chmurze jako jeden blob).
 * `areas` i `sections` to pola dodatkowe (zgodne wstecz — starsze wersje je
 * ignorują, a brak w JSON = pusta lista).
 */
data class CloudSnapshot(
    val tasks: List<CloudTask> = emptyList(),
    val projects: List<CloudProject> = emptyList(),
    val labels: List<CloudLabel> = emptyList(),
    val areas: List<CloudArea> = emptyList(),
    val sections: List<CloudSection> = emptyList(),
    val activities: List<CloudActivity> = emptyList(),
    val tombstones: List<CloudTombstone> = emptyList()
)
