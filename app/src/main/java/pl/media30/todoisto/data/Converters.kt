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
}
