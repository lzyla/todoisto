package pl.media30.todoisto.data

/** Trzy poziomy energii do pracy — celowo grube: dokładność liczby i tak byłaby pozorna. */
enum class EnergyLevel(val label: String, val emoji: String) {
    LOW("Słabo", "😴"), MED("Średnio", "😐"), HIGH("Dobrze", "💪")
}

/**
 * Sygnały, z których szacujemy energię. Wszystko opcjonalne: brakujące sygnały
 * po prostu nie wpływają na wynik. Sen/tętno/HRV pochodzą (docelowo) z Google Health API,
 * obciążenie dnia poprzedniego liczymy z własnych zadań.
 */
data class EnergySignals(
    val sleepMinutes: Int? = null,
    val sleepBaselineMinutes: Int? = null,
    val restingHr: Double? = null,
    val restingHrBaseline: Double? = null,
    val hrv: Double? = null,
    val hrvBaseline: Double? = null,
    /** Suma minut zadań ukończonych wczoraj. */
    val prevDayWorkMinutes: Int = 0,
    /** Minuta doby (0..1439) ostatniego ukończonego wczoraj zadania; null = brak. */
    val prevDayLastWorkMinute: Int? = null
)

data class EnergyEstimate(val level: EnergyLevel, val score: Int, val reasons: List<String>)

/**
 * Czysta logika (bez Androida) — testowalna. Wynik jest tłumaczalny: każda korekta
 * dodaje powód, który pokazujemy użytkownikowi, a ręczna odpowiedź zawsze wygrywa.
 */
object EnergyModel {
    private const val START = 65 // neutralny dzień = „średnio”

    fun estimate(s: EnergySignals): EnergyEstimate {
        var score = START
        val why = mutableListOf<String>()

        s.sleepMinutes?.let { sleep ->
            val base = s.sleepBaselineMinutes ?: 420
            var d = ((sleep - base) / 60.0 * 12.0).coerceIn(-30.0, 10.0)
            if (sleep < 300) d -= 10.0
            score += d.toInt()
            val h = sleep / 60; val m = sleep % 60
            if (d <= -6) why += "krótki sen (${h}h ${m}min)"
            else if (d >= 5) why += "dobry sen (${h}h ${m}min)"
        }
        if (s.restingHr != null && s.restingHrBaseline != null) {
            val diff = s.restingHr - s.restingHrBaseline
            val d = -(diff * 3.0).coerceIn(0.0, 15.0)
            score += d.toInt()
            if (d <= -6) why += "podwyższone tętno spoczynkowe"
        }
        if (s.hrv != null && s.hrvBaseline != null && s.hrvBaseline > 0) {
            val ratio = s.hrv / s.hrvBaseline
            val d = when { ratio < 0.7 -> -18; ratio < 0.85 -> -12; ratio > 1.1 -> 5; else -> 0 }
            score += d
            if (d <= -12) why += "niska zmienność tętna (HRV)"
        }
        when {
            s.prevDayWorkMinutes > 480 -> { score -= 10; why += "bardzo pracowity wczorajszy dzień" }
            s.prevDayWorkMinutes > 360 -> { score -= 5; why += "pracowity wczorajszy dzień" }
        }
        s.prevDayLastWorkMinute?.let { m ->
            if (m >= 23 * 60 || m < 4 * 60) { score -= 8; why += "praca do późna wczoraj" }
        }
        score = score.coerceIn(0, 100)
        val level = when { score < 45 -> EnergyLevel.LOW; score < 70 -> EnergyLevel.MED; else -> EnergyLevel.HIGH }
        return EnergyEstimate(level, score, why)
    }

    /** Ręczna odpowiedź użytkownika zawsze wygrywa z szacunkiem. */
    fun resolve(manual: EnergyLevel?, estimate: EnergyEstimate): EnergyLevel = manual ?: estimate.level
}

/** „Koszt” zadania i przestawianie listy zależnie od energii. */
object TaskLoad {
    fun cost(t: Task, subtasks: Int = 0): Int {
        val dur = t.durationMinutes ?: 30
        val prio = when (t.priority) { Priority.P1 -> 3; Priority.P2 -> 2; Priority.P3 -> 1; Priority.P4 -> 0 }
        val notes = when { t.notes.length > 300 -> 2; t.notes.length > 120 -> 1; else -> 0 }
        return dur / 15 + prio + notes + (if (subtasks >= 4) 1 else 0)
    }

    fun isHeavy(t: Task, subtasks: Int = 0): Boolean = cost(t, subtasks) >= 6

    /** Zadania, których nie ruszamy: z godziną, z bliskim twardym terminem i dzisiejsze P1. */
    fun isPinned(t: Task, today: Long): Boolean =
        t.dueTimeMinutes != null ||
            (t.deadline != null && t.deadline <= today + 1) ||
            (t.priority == Priority.P1 && t.dueDate != null && t.dueDate <= today)

    /**
     * LOW: lekkie wyżej. HIGH: trudne wyżej. MED: bez zmian.
     * Przypięte zadania zostają na swoich miejscach, ruchome zajmują pozostałe sloty.
     * Sortowanie jest stabilne, więc przy równym koszcie zostaje dotychczasowa kolejność.
     */
    fun reorder(tasks: List<Task>, level: EnergyLevel?, today: Long, subCount: (Task) -> Int = { 0 }): List<Task> {
        if (level == null || level == EnergyLevel.MED) return tasks
        val movable = tasks.filter { !isPinned(it, today) }
        val sorted = if (level == EnergyLevel.LOW) movable.sortedBy { cost(it, subCount(it)) }
        else movable.sortedByDescending { cost(it, subCount(it)) }
        val it = sorted.iterator()
        return tasks.map { t -> if (isPinned(t, today)) t else it.next() }
    }
}
