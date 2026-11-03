package pl.media30.todoisto.data

import androidx.room.Entity

/**
 * Lokalny nagrobek usuniętego projektu/etykiety/sekcji/obszaru/aktywności.
 * Wysyłany w migawce, żeby usunięcie nie zostało „odkręcone" przez kopię
 * z drugiego urządzenia. Rodzaje: stałe z [pl.media30.todoisto.shared.CloudTombstone].
 */
@Entity(tableName = "tombstones", primaryKeys = ["kind", "refId"])
data class Tombstone(
    val kind: String,
    val refId: Long,
    val deletedAt: Long
)
