package pl.media30.todoisto.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun priorityToInt(priority: Priority): Int = priority.ordinal

    @TypeConverter
    fun intToPriority(value: Int): Priority = Priority.fromOrdinalSafe(value)

    @TypeConverter
    fun recurrenceToString(recurrence: Recurrence?): String? = recurrence?.name

    @TypeConverter
    fun stringToRecurrence(value: String?): Recurrence? = Recurrence.fromNameSafe(value)

    @TypeConverter
    fun longsToString(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun stringToLongs(value: String): List<Long> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull { it.toLongOrNull() }

    // Newline-separated — URLs never contain raw newlines.
    @TypeConverter
    fun stringsToString(value: List<String>): String = value.joinToString("\n")

    @TypeConverter
    fun stringToStrings(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("\n").filter { it.isNotBlank() }

    // --- Pula aktywności ---
    @TypeConverter
    fun effortToString(value: EffortType?): String? = value?.name

    @TypeConverter
    fun stringToEffort(value: String?): EffortType? =
        value?.let { runCatching { EffortType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun placeToString(value: Place): String = value.name

    @TypeConverter
    fun stringToPlace(value: String): Place =
        runCatching { Place.valueOf(value) }.getOrDefault(Place.HOME)

    @TypeConverter
    fun energyToString(value: EnergyCost): String = value.name

    @TypeConverter
    fun stringToEnergy(value: String): EnergyCost =
        runCatching { EnergyCost.valueOf(value) }.getOrDefault(EnergyCost.MED)
}
