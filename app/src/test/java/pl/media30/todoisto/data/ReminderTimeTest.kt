package pl.media30.todoisto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderTimeTest {
    private val waw = ZoneId.of("Europe/Warsaw")
    private fun at(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(waw).toLocalDateTime()

    @Test fun `quick add - godzina lokalna, nie UTC`() {
        val day = LocalDate.of(2026, 11, 3).toEpochDay()
        assertEquals(LocalDateTime.of(2026, 11, 3, 15, 0), at(Reminders.localMillis(day, 15 * 60, waw)))
        val summer = LocalDate.of(2026, 7, 1).toEpochDay()
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 30), at(Reminders.localMillis(summer, 9 * 60 + 30, waw)))
    }

    @Test fun `przelozenie terminu przesuwa przypomnienie i zachowuje godzine przez zmiane czasu`() {
        val d1 = LocalDate.of(2026, 10, 24).toEpochDay()  // przed zmianą czasu (25.10)
        val d2 = LocalDate.of(2026, 10, 27).toEpochDay()
        val rem = Reminders.localMillis(d1, 8 * 60, waw)
        assertEquals(LocalDateTime.of(2026, 10, 27, 8, 0), at(Reminders.shiftWithDueDate(rem, d1, d2, waw)!!))
    }

    @Test fun `bez zmiany terminu lub bez przypomnienia nic sie nie dzieje`() {
        assertNull(Reminders.shiftWithDueDate(null, 1, 2, waw))
        assertEquals(123L, Reminders.shiftWithDueDate(123L, 5, 5, waw))
        assertEquals(123L, Reminders.shiftWithDueDate(123L, null, 5, waw))
    }
}
