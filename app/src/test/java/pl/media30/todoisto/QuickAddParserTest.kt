package pl.media30.todoisto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.media30.todoisto.data.Priority
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.data.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate

class QuickAddParserTest {

    // Fixed reference: Saturday, 11 Jul 2026
    private val today = LocalDate.of(2026, 7, 11)
    private val parser = QuickAddParser(today)

    @Test
    fun plainTitle() {
        val r = parser.parse("Kupić mleko")
        assertEquals("Kupić mleko", r.title)
        assertEquals(Priority.P4, r.priority)
        assertNull(r.dueDate)
    }

    @Test
    fun priority() {
        val r = parser.parse("Zapłacić rachunki p1")
        assertEquals("Zapłacić rachunki", r.title)
        assertEquals(Priority.P1, r.priority)
    }

    @Test
    fun projectAndLabels() {
        val r = parser.parse("Raport #praca @pilne @fundacja")
        assertEquals("Raport", r.title)
        assertEquals("praca", r.projectName)
        assertEquals(listOf("pilne", "fundacja"), r.labelNames)
    }

    @Test
    fun tomorrowWithTime() {
        val r = parser.parse("Spotkanie jutro o 15:00")
        assertEquals("Spotkanie", r.title)
        assertEquals(today.plusDays(1).toEpochDay(), r.dueDate)
        assertEquals(15 * 60, r.dueTimeMinutes)
    }

    @Test
    fun relativeDays() {
        val r = parser.parse("Oddać książkę za 3 dni")
        assertEquals("Oddać książkę", r.title)
        assertEquals(today.plusDays(3).toEpochDay(), r.dueDate)
    }

    @Test
    fun weekday() {
        val r = parser.parse("Trening w poniedziałek")
        assertEquals("Trening", r.title)
        assertEquals(DayOfWeek.MONDAY, LocalDate.ofEpochDay(r.dueDate!!).dayOfWeek)
    }

    @Test
    fun recurringWeekly() {
        val r = parser.parse("Podlać kwiaty co tydzień")
        assertEquals("Podlać kwiaty", r.title)
        assertEquals(Recurrence.WEEKLY, r.recurrence)
        assertEquals(today.toEpochDay(), r.dueDate)
    }

    @Test
    fun recurringWeekday() {
        val r = parser.parse("Wywóz śmieci co poniedziałek")
        assertEquals("Wywóz śmieci", r.title)
        assertEquals(Recurrence.WEEKLY, r.recurrence)
        assertEquals(DayOfWeek.MONDAY, LocalDate.ofEpochDay(r.dueDate!!).dayOfWeek)
    }

    @Test
    fun isoDateAndDeadline() {
        val r = parser.parse("Wniosek 2026-07-20 do 2026-07-31")
        assertEquals("Wniosek", r.title)
        assertEquals(LocalDate.of(2026, 7, 20).toEpochDay(), r.dueDate)
        assertEquals(LocalDate.of(2026, 7, 31).toEpochDay(), r.deadline)
    }

    @Test
    fun dottedDateInfersNextYear() {
        val r = parser.parse("Urodziny 05.01")
        assertEquals("Urodziny", r.title)
        assertEquals(LocalDate.of(2027, 1, 5).toEpochDay(), r.dueDate)
    }

    @Test
    fun everythingTogether() {
        val r = parser.parse("Przygotować prezentację #praca @ważne jutro o 9:30 p2")
        assertEquals("Przygotować prezentację", r.title)
        assertEquals("praca", r.projectName)
        assertEquals(listOf("ważne"), r.labelNames)
        assertEquals(today.plusDays(1).toEpochDay(), r.dueDate)
        assertEquals(9 * 60 + 30, r.dueTimeMinutes)
        assertEquals(Priority.P2, r.priority)
    }
}
