package pl.media30.todoisto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Trzeci typ obiektu w Todoisto: coś, co MOŻESZ zrobić w wolnym oknie.
 * Sama aktywność nie pojawia się na liście — dopiero zaakceptowana propozycja
 * materializuje się jako zwykły [Task] (patrz TaskRepository.materializeActivity).
 */
@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val effortType: EffortType,
    val durationMinutes: Int = 30,
    val place: Place = Place.HOME,
    val windowStartMin: Int? = null,   // minuty od północy; null = cały dzień
    val windowEndMin: Int? = null,
    val daysMask: Int = 0b1111111,     // bit per dzień tyg. (bit0=pon .. bit6=niedz)
    val energyCost: EnergyCost = EnergyCost.MED,
    val frequencyTarget: Int? = null,  // np. 3 = cel 3×/tydzień; null = brak celu
    val isActive: Boolean = true,
    val lastScheduledAt: Long? = null,
    val lastCompletedAt: Long? = null,
    val createdAt: Long = 0L
)

enum class EffortType(val label: String, val emoji: String) {
    PHYSICAL("Fizyczny", "💪"),
    MENTAL("Umysłowy", "🧠"),
    RELAX("Regeneracja", "🌿")
}

enum class Place(val label: String) { HOME("Dom"), OUTSIDE("Na zewnątrz") }

enum class EnergyCost(val label: String) { LOW("Niski"), MED("Średni"), HIGH("Wysoki") }

/** Bit dnia tygodnia dla [Activity.daysMask]; dayOfWeek 1=pon..7=niedz. */
fun dayBit(dayOfWeek: Int): Int = 1 shl ((dayOfWeek - 1).coerceIn(0, 6))
