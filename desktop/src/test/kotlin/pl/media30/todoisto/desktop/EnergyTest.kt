package pl.media30.todoisto.desktop

import pl.media30.todoisto.shared.CloudTask
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class EnergyTest {
    private fun t(id: Long, p: String = "P4", dur: Int? = null, time: Int? = null) =
        CloudTask(id = id, title = "t$id", priority = p, durationMinutes = dur, dueTimeMinutes = time, dueDate = 100)

    @Test fun neutralDayIsMed() = assertEquals(EnergyLevel.MED, EnergyModel.estimate(EnergySignals()).level)

    @Test fun poorSleepGivesLow() {
        val e = EnergyModel.estimate(EnergySignals(sleepMinutes = 240, sleepBaselineMinutes = 420, hrv = 10.0, hrvBaseline = 14.0))
        assertEquals(EnergyLevel.LOW, e.level)
        assertTrue(e.reasons.any { it.startsWith("krótki sen") })
    }

    @Test fun manualWins() {
        val e = EnergyModel.estimate(EnergySignals(sleepMinutes = 200))
        assertEquals(EnergyLevel.HIGH, EnergyModel.resolve(EnergyLevel.HIGH, e))
    }

    @Test fun lowPutsLightFirstAndKeepsPinned() {
        val heavy = t(1, "P2", 120); val light = t(2, dur = 10); val pinned = t(3, "P2", 90, time = 600)
        val out = TaskLoad.reorder(listOf(heavy, pinned, light), EnergyLevel.LOW, today = 100)
        assertEquals(listOf(2L, 3L, 1L), out.map { it.id })
    }

    @Test fun highPutsHeavyFirst() {
        val out = TaskLoad.reorder(listOf(t(1, dur = 10), t(2, "P2", 120)), EnergyLevel.HIGH, today = 100)
        assertEquals(listOf(2L, 1L), out.map { it.id })
    }

    @Test fun heavyThreshold() {
        assertTrue(TaskLoad.isHeavy(t(1, "P2", 120)))
        assertTrue(!TaskLoad.isHeavy(t(2, dur = 30)))
    }

    @Test fun bubbleTexts() {
        assertEquals("Zacznij od lżejszych zadań. Przy kilku zadaniach skorzystaj z pomocy AI.", EnergyText.advice(EnergyLevel.LOW, 3))
        assertEquals("Krótki sen, podwyższone tętno", EnergyText.why(false, listOf("krótki sen (4h 0min)", "podwyższone tętno spoczynkowe")))
    }
}
