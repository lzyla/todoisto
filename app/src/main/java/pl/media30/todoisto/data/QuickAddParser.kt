package pl.media30.todoisto.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Result of parsing a Quick-Add string. [title] is the leftover text with all
 * recognised tokens stripped out.
 */
data class ParsedTask(
    val title: String,
    val priority: Priority = Priority.P4,
    val dueDate: Long? = null,        // epoch-day
    val dueTimeMinutes: Int? = null,  // minutes from midnight
    val deadline: Long? = null,       // epoch-day
    val recurrence: Recurrence? = null,
    val projectName: String? = null,
    val labelNames: List<String> = emptyList()
)

/**
 * Natural-language Quick-Add parser (Polish), Todoist-style.
 *
 * Recognises, in one line:
 *  - priority:   p1 p2 p3 p4
 *  - project:    #nazwa
 *  - labels:     @etykieta (multiple)
 *  - time:       o 15:00 · 15:00 · o 9
 *  - recurrence: codziennie · co tydzień · co miesiąc · co rok · co poniedziałek
 *  - dates:      dziś/dzisiaj · jutro · pojutrze · w poniedziałek · pon..niedz
 *                za 3 dni · za tydzień · 2026-07-20 · 20.07 · 20.07.2026
 *  - deadline:   do <data>  (np. "do 2026-07-31")
 *
 * [today] is injected so the parser is deterministic and testable.
 */
class QuickAddParser(private val today: LocalDate = LocalDate.now()) {

    private val weekdays = mapOf(
        "poniedziałek" to DayOfWeek.MONDAY, "poniedzialek" to DayOfWeek.MONDAY, "pon" to DayOfWeek.MONDAY,
        "wtorek" to DayOfWeek.TUESDAY, "wt" to DayOfWeek.TUESDAY,
        "środa" to DayOfWeek.WEDNESDAY, "sroda" to DayOfWeek.WEDNESDAY, "śr" to DayOfWeek.WEDNESDAY, "sr" to DayOfWeek.WEDNESDAY,
        "czwartek" to DayOfWeek.THURSDAY, "czw" to DayOfWeek.THURSDAY,
        "piątek" to DayOfWeek.FRIDAY, "piatek" to DayOfWeek.FRIDAY, "pt" to DayOfWeek.FRIDAY,
        "sobota" to DayOfWeek.SATURDAY, "sob" to DayOfWeek.SATURDAY,
        "niedziela" to DayOfWeek.SUNDAY, "niedz" to DayOfWeek.SUNDAY, "nd" to DayOfWeek.SUNDAY
    )

    // Accusative forms as they appear after "w"/"we"/"co"
    private val weekdayAccusative = mapOf(
        "poniedziałek" to DayOfWeek.MONDAY, "poniedzialek" to DayOfWeek.MONDAY,
        "wtorek" to DayOfWeek.TUESDAY,
        "środę" to DayOfWeek.WEDNESDAY, "srode" to DayOfWeek.WEDNESDAY, "środe" to DayOfWeek.WEDNESDAY,
        "czwartek" to DayOfWeek.THURSDAY,
        "piątek" to DayOfWeek.FRIDAY, "piatek" to DayOfWeek.FRIDAY,
        "sobotę" to DayOfWeek.SATURDAY, "sobote" to DayOfWeek.SATURDAY,
        "niedzielę" to DayOfWeek.SUNDAY, "niedziele" to DayOfWeek.SUNDAY
    )

    private val timeRegex = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")
    private val isoDateRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
    private val dotDateRegex = Regex("""^(\d{1,2})\.(\d{1,2})(?:\.(\d{2,4}))?$""")

    fun parse(raw: String): ParsedTask {
        val tokens = raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val leftover = mutableListOf<String>()

        var priority = Priority.P4
        var dueDate: Long? = null
        var dueTime: Int? = null
        var deadline: Long? = null
        var recurrence: Recurrence? = null
        var projectName: String? = null
        val labels = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val lower = token.lowercase()

            // --- structured prefixes ---
            if (token.startsWith("#") && token.length > 1) {
                projectName = token.drop(1); i++; continue
            }
            if (token.startsWith("@") && token.length > 1) {
                labels += token.drop(1); i++; continue
            }
            if (lower.matches(Regex("^p[1-4]$"))) {
                priority = Priority.entries[lower[1].digitToInt() - 1]; i++; continue
            }

            // --- time: "o 15:00", "15:00", "o 9" ---
            if (lower == "o" && i + 1 < tokens.size) {
                val t = parseTime(tokens[i + 1].lowercase())
                if (t != null) { dueTime = t; i += 2; continue }
            }
            if (timeRegex.matches(lower)) {
                dueTime = parseTime(lower); i++; continue
            }

            // --- recurrence: "codziennie", "co <unit>" ---
            if (lower == "codziennie") { recurrence = Recurrence.DAILY; dueDate = dueDate ?: today.toEpochDay(); i++; continue }
            if (lower == "co" && i + 1 < tokens.size) {
                val n = tokens[i + 1].lowercase()
                val rec = when (n) {
                    "dzień", "dzien" -> Recurrence.DAILY
                    "tydzień", "tydzien" -> Recurrence.WEEKLY
                    "miesiąc", "miesiac" -> Recurrence.MONTHLY
                    "rok" -> Recurrence.YEARLY
                    else -> null
                }
                if (rec != null) {
                    recurrence = rec
                    if (dueDate == null) dueDate = today.toEpochDay()
                    i += 2; continue
                }
                val wd = weekdays[n] ?: weekdayAccusative[n]
                if (wd != null) {
                    recurrence = Recurrence.WEEKLY
                    dueDate = nextWeekday(wd)
                    i += 2; continue
                }
            }

            // --- deadline: "do <date>" ---
            if (lower == "do" && i + 1 < tokens.size) {
                val d = parseDateToken(tokens[i + 1].lowercase())
                if (d != null) { deadline = d; i += 2; continue }
            }

            // --- relative dates ---
            when (lower) {
                "dziś", "dzis", "dzisiaj" -> { dueDate = today.toEpochDay(); i++; continue }
                "jutro" -> { dueDate = today.plusDays(1).toEpochDay(); i++; continue }
                "pojutrze" -> { dueDate = today.plusDays(2).toEpochDay(); i++; continue }
            }

            // "za N dni/tydzień/tygodnie/miesiąc"
            if (lower == "za" && i + 2 < tokens.size) {
                val amount = tokens[i + 1].toIntOrNull()
                val unit = tokens[i + 2].lowercase()
                if (amount != null) {
                    val date = when (unit) {
                        "dzień", "dzien", "dni" -> today.plusDays(amount.toLong())
                        "tydzień", "tydzien", "tygodnie", "tygodni" -> today.plusWeeks(amount.toLong())
                        "miesiąc", "miesiac", "miesiące", "miesiecy", "miesięcy" -> today.plusMonths(amount.toLong())
                        else -> null
                    }
                    if (date != null) { dueDate = date.toEpochDay(); i += 3; continue }
                }
            }

            // "w/we <weekday>" or bare weekday
            if ((lower == "w" || lower == "we") && i + 1 < tokens.size) {
                val wd = weekdayAccusative[tokens[i + 1].lowercase()] ?: weekdays[tokens[i + 1].lowercase()]
                if (wd != null) { dueDate = nextWeekday(wd); i += 2; continue }
            }
            val bareWeekday = weekdays[lower]
            if (bareWeekday != null) { dueDate = nextWeekday(bareWeekday); i++; continue }

            // explicit dates
            val explicit = parseDateToken(lower)
            if (explicit != null) { dueDate = explicit; i++; continue }

            leftover += token
            i++
        }

        return ParsedTask(
            title = leftover.joinToString(" ").trim(),
            priority = priority,
            dueDate = dueDate,
            dueTimeMinutes = dueTime,
            deadline = deadline,
            recurrence = recurrence,
            projectName = projectName,
            labelNames = labels.distinct()
        )
    }

    private fun parseTime(token: String): Int? {
        timeRegex.find(token)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            return h * 60 + m
        }
        // bare hour after "o", e.g. "9"
        token.toIntOrNull()?.let { if (it in 0..23) return it * 60 }
        return null
    }

    private fun parseDateToken(token: String): Long? {
        isoDateRegex.find(token)?.let {
            return runCatching {
                LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
                    .toEpochDay()
            }.getOrNull()
        }
        dotDateRegex.find(token)?.let {
            val day = it.groupValues[1].toInt()
            val month = it.groupValues[2].toInt()
            val yearRaw = it.groupValues[3]
            val year = when {
                yearRaw.isEmpty() -> inferYear(month, day)
                yearRaw.length == 2 -> 2000 + yearRaw.toInt()
                else -> yearRaw.toInt()
            }
            return runCatching { LocalDate.of(year, month, day).toEpochDay() }.getOrNull()
        }
        return null
    }

    /** If day/month already passed this year, roll to next year. */
    private fun inferYear(month: Int, day: Int): Int {
        val candidate = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return today.year
        return if (candidate.isBefore(today)) today.year + 1 else today.year
    }

    private fun nextWeekday(target: DayOfWeek): Long {
        var date = today
        // "next" occurrence including today
        var guard = 0
        while (date.dayOfWeek != target && guard < 7) { date = date.plusDays(1); guard++ }
        return date.toEpochDay()
    }
}
