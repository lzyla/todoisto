package pl.media30.todoisto.data

import java.time.LocalDate

/**
 * Jedno źródło „dziś" dla warstwy UI. Domyślnie zegar systemowy, ale można je
 * nadpisać (testy snapshotów), żeby renderowanie dat było w 100% deterministyczne
 * niezależnie od dnia uruchomienia.
 */
object AppClock {
    /** Nadpisane „dziś" (tylko testy). null = realny zegar systemowy. */
    var todayOverride: LocalDate? = null

    fun today(): LocalDate = todayOverride ?: LocalDate.now()
    fun todayEpochDay(): Long = today().toEpochDay()
}
