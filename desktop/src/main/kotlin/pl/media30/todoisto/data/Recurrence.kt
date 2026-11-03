package pl.media30.todoisto.data

import java.time.LocalDate

/**
 * How a task repeats. The anchor (the task's due date) determines the weekday /
 * day-of-month; the frequency determines the step to the next occurrence.
 */
enum class Recurrence {
    DAILY, WEEKLY, MONTHLY, YEARLY;

    /** Next due epoch-day after [fromEpochDay] for this frequency. */
    fun next(fromEpochDay: Long): Long {
        val date = LocalDate.ofEpochDay(fromEpochDay)
        val nextDate = when (this) {
            DAILY -> date.plusDays(1)
            WEEKLY -> date.plusWeeks(1)
            MONTHLY -> date.plusMonths(1)
            YEARLY -> date.plusYears(1)
        }
        return nextDate.toEpochDay()
    }

    val label: String
        get() = when (this) {
            DAILY -> "Codziennie"
            WEEKLY -> "Co tydzień"
            MONTHLY -> "Co miesiąc"
            YEARLY -> "Co rok"
        }

    companion object {
        fun fromNameSafe(value: String?): Recurrence? =
            value?.let { name -> entries.firstOrNull { it.name == name } }
    }
}
