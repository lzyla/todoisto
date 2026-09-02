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
    val deleted: Boolean = false
)

data class CloudProject(
    val id: Long,
    val name: String,
    val colorArgb: Long = 0xFF9B6BFFL,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val workEffortType: String? = null,
    val areaId: Long? = null
)

data class CloudLabel(
    val id: Long,
    val name: String,
    val colorArgb: Long = 0xFF4D6BFFL,
    val isFavorite: Boolean = false
)

/** Migawka danych użytkownika (to, co ląduje w chmurze jako jeden blob). */
data class CloudSnapshot(
    val tasks: List<CloudTask> = emptyList(),
    val projects: List<CloudProject> = emptyList(),
    val labels: List<CloudLabel> = emptyList()
)
