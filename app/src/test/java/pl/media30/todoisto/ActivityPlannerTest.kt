package pl.media30.todoisto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.media30.todoisto.data.Activity
import pl.media30.todoisto.data.ActivityPlanner
import pl.media30.todoisto.data.DayContext
import pl.media30.todoisto.data.EffortType
import pl.media30.todoisto.data.FreeSlot
import kotlin.random.Random

class ActivityPlannerTest {

    private fun ctx(mental: Float = 0.5f, physical: Float = 0.5f, budget: Int = 80, dow: Int = 3) =
        DayContext(
            completedCount = 4, totalWorkMinutes = 200,
            mentalShare = mental, physicalShare = physical, energyBudget = budget,
            nowMinutes = 17 * 60, dayOfWeek = dow, nowMillis = 1_000_000_000_000L
        )

    private fun activity(id: Long, type: EffortType, dur: Int = 30) =
        Activity(id = id, name = "A$id", effortType = type, durationMinutes = dur)

    private val eveningSlot = FreeSlot(17 * 60, 20 * 60) // 3h okno

    @Test
    fun mentalDayFavoursPhysicalOverMental() {
        val c = ctx(mental = 1f, physical = 0f)
        val wPhys = ActivityPlanner.weight(activity(1, EffortType.PHYSICAL), c, eveningSlot)
        val wMental = ActivityPlanner.weight(activity(2, EffortType.MENTAL), c, eveningSlot)
        assertTrue("po dniu umysłowym fizyczne > umysłowe", wPhys > wMental)
    }

    @Test
    fun physicalDayFavoursMental() {
        val c = ctx(mental = 0f, physical = 1f)
        val wPhys = ActivityPlanner.weight(activity(1, EffortType.PHYSICAL), c, eveningSlot)
        val wMental = ActivityPlanner.weight(activity(2, EffortType.MENTAL), c, eveningSlot)
        assertTrue("po dniu fizycznym umysłowe > fizyczne", wMental > wPhys)
    }

    @Test
    fun durationLongerThanSlotIsRejected() {
        val w = ActivityPlanner.weight(activity(1, EffortType.RELAX, dur = 240), ctx(), eveningSlot)
        assertEquals(0.0, w, 0.0001)
    }

    @Test
    fun windowMatchBoostsInsideWindow() {
        val inWin = activity(1, EffortType.RELAX).copy(windowStartMin = 17 * 60, windowEndMin = 21 * 60)
        val outWin = activity(2, EffortType.RELAX).copy(windowStartMin = 6 * 60, windowEndMin = 9 * 60)
        val wIn = ActivityPlanner.weight(inWin, ctx(), eveningSlot)
        val wOut = ActivityPlanner.weight(outWin, ctx(), eveningSlot)
        assertTrue("okno pasujące ma wyższą wagę", wIn > wOut)
    }

    @Test
    fun freqDebtRaisesUnderTarget() {
        val a = activity(1, EffortType.PHYSICAL).copy(frequencyTarget = 3)
        val cLow = ctx().copy(weekCountByActivity = mapOf(1L to 0))  // dług
        val cMet = ctx().copy(weekCountByActivity = mapOf(1L to 3))  // cel osiągnięty
        assertTrue(ActivityPlanner.weight(a, cLow, eveningSlot) > ActivityPlanner.weight(a, cMet, eveningSlot))
    }

    @Test
    fun highEnergyCostDampenedWhenTired() {
        val a = activity(1, EffortType.PHYSICAL).copy(energyCost = pl.media30.todoisto.data.EnergyCost.HIGH)
        val tired = ActivityPlanner.weight(a, ctx(budget = 20), eveningSlot)
        val fresh = ActivityPlanner.weight(a, ctx(budget = 90), eveningSlot)
        assertTrue("zmęczenie tłumi HIGH-cost", fresh > tired)
    }

    @Test
    fun planReturnsRequestedCountDeterministically() {
        val activities = listOf(
            activity(1, EffortType.PHYSICAL),
            activity(2, EffortType.MENTAL),
            activity(3, EffortType.RELAX)
        )
        val slots = listOf(FreeSlot(9 * 60, 11 * 60), FreeSlot(17 * 60, 20 * 60))
        val out = ActivityPlanner.plan(activities, ctx(), slots, count = 2, random = Random(42))
        assertEquals(2, out.size)
        assertTrue("różne aktywności", out[0].activity.id != out[1].activity.id)
        out.forEach { assertTrue(it.startMin >= it.slot.startMin && it.startMin + it.activity.durationMinutes <= it.slot.endMin) }
    }

    @Test
    fun coldStartStillPlans() {
        // brak historii, budżet pełny, share 0 → nadal działa (typ/okno/dzień)
        val c = DayContext(0, 0, 0f, 0f, 100, 12 * 60, 2, nowMillis = 1_000_000_000_000L)
        val out = ActivityPlanner.plan(listOf(activity(1, EffortType.PHYSICAL)), c, listOf(eveningSlot), 1, Random(1))
        assertEquals(1, out.size)
    }
}
