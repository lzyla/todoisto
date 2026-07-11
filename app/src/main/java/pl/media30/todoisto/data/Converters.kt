package pl.media30.todoisto.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun priorityToInt(priority: Priority): Int = priority.ordinal

    @TypeConverter
    fun intToPriority(value: Int): Priority = Priority.fromOrdinalSafe(value)
}
