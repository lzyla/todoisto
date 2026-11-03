package pl.media30.todoisto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.media30.todoisto.data.*

class EnergyModelTest {
    private fun t(id: Long, dur: Int? = null, p: Priority = Priority.P4, time: Int? = null, deadline: Long? = null, due: Long? = 100L) =
        Task(id = id, title = "t$id", durationMinutes = dur, priority = p, dueTimeMinutes = time, deadline = deadline, dueDate = due)

    @Test fun neutralDayIsMedium() =
        assertEquals(EnergyLevel.MED, EnergyModel.estimate(EnergySignals()).level)

    @Test fun shortSleepLowersEnergyAndExplainsWhy() {
        val e = EnergyModel.estimate(EnergySignals(sleepMinutes = 4 * 60 + 30, sleepBaselineMinutes = 7 * 60))
        assertEquals(EnergyLevel.LOW, e.level)
        assertTrue(e.reasons.any { it.contains("krótki sen") })
    }

    @Test fun goodSleepRaisesEnergy() =
        assertEquals(EnergyLevel.HIGH, EnergyModel.estimate(EnergySignals(sleepMinutes = 8 * 60 + 30, sleepBaselineMinutes = 7 * 60)).level)

    @Test fun hrvAndRestingHrDragDown() {
        val e = EnergyModel.estimate(EnergySignals(hrv = 30.0, hrvBaseline = 50.0, restingHr = 66.0, restingHrBaseline = 58.0))
        assertEquals(EnergyLevel.LOW, e.level)
    }

    @Test fun lateHeavyPreviousDayCosts() {
        val e = EnergyModel.estimate(EnergySignals(prevDayWorkMinutes = 500, prevDayLastWorkMinute = 23 * 60 + 30))
        assertEquals(65 - 18, e.score)
    }

    @Test fun manualAlwaysWins() {
        val low = EnergyModel.estimate(EnergySignals(sleepMinutes = 240, sleepBaselineMinutes = 420))
        assertEquals(EnergyLevel.HIGH, EnergyModel.resolve(EnergyLevel.HIGH, low))
        assertEquals(EnergyLevel.LOW, EnergyModel.resolve(null, low))
    }

    @Test fun heavyDetection() {
        assertTrue(TaskLoad.isHeavy(t(1, dur = 90)))
        assertTrue(TaskLoad.isHeavy(t(2, dur = 60, p = Priority.P2)))
        assertFalse(TaskLoad.isHeavy(t(3, dur = 20)))
    }

    @Test fun lowEnergyPutsLightFirstButKeepsPinnedInPlace() {
        val timed = t(1, dur = 60, time = 9 * 60)           // przypięte: godzina
        val heavy = t(2, dur = 120)
        val light = t(3, dur = 15)
        val urgent = t(4, dur = 120, deadline = 101L)         // przypięte: twardy termin jutro
        val out = TaskLoad.reorder(listOf(timed, heavy, urgent, light), EnergyLevel.LOW, today = 100L)
        assertEquals(listOf(1L, 3L, 4L, 2L), out.map { it.id })
    }

    @Test fun highEnergyPutsHardFirst() {
        val out = TaskLoad.reorder(listOf(t(1, dur = 15), t(2, dur = 120)), EnergyLevel.HIGH, today = 100L)
        assertEquals(listOf(2L, 1L), out.map { it.id })
    }

    @Test fun mediumLeavesOrderUntouched() {
        val src = listOf(t(1, dur = 15), t(2, dur = 120))
        assertEquals(src, TaskLoad.reorder(src, EnergyLevel.MED, today = 100L))
    }
}
