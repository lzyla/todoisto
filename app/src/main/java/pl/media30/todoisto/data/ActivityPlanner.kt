package pl.media30.todoisto.data

import kotlin.random.Random

/** Wolne okno w ciągu dnia (minuty od północy). */
data class FreeSlot(val startMin: Int, val endMin: Int) {
    val length: Int get() = endMin - startMin
}

/** Sygnały z dnia, na których opiera się dobór. */
data class DayContext(
    val completedCount: Int,
    val totalWorkMinutes: Int,
    val mentalShare: Float,          // 0..1 udział pracy umysłowej (z workEffortType projektów)
    val physicalShare: Float,        // 0..1 udział pracy fizycznej
    val energyBudget: Int,           // 0..100, „resztki energii dnia"
    val nowMinutes: Int,
    val dayOfWeek: Int,              // 1=pon..7=niedz
    val weekCountByActivity: Map<Long, Int> = emptyMap(), // ukończone w tym tyg. per activityId
    val lastDoneByActivity: Map<Long, Long?> = emptyMap(),// lastCompletedAt per activityId
    val nowMillis: Long = 0L
)

/** Propozycja: aktywność + slot + proponowana godzina startu. */
data class Suggestion(
    val activity: Activity,
    val slot: FreeSlot,
    val startMin: Int
)

/**
 * Silnik doboru — czysta logika (bez Androida/Room), testowalna jak QuickAddParser.
 * Losowanie WAŻONE: pasujące aktywności mają większą szansę, niepasujące małą, ale
 * nie zerową (element zaskoczenia). Model deterministyczny i tłumaczalny — bez LLM.
 */
object ActivityPlanner {

    /** Waga kandydata w danym slocie (0 = twardy odrzut). */
    fun weight(a: Activity, ctx: DayContext, slot: FreeSlot): Double {
        // durationFit — twardy warunek
        if (a.durationMinutes > slot.length) return 0.0

        val mental = ctx.mentalShare.toDouble()
        val phys = ctx.physicalShare.toDouble()

        // typMatch — po dniu umysłowym premiuj fizyczne/regenerację (i odwrotnie)
        val typMatch = when (a.effortType) {
            EffortType.PHYSICAL -> (1.0 + mental * 1.0 - phys * 0.5)
            EffortType.MENTAL -> (1.0 + phys * 1.0 - mental * 0.5)
            EffortType.RELAX -> (1.0 + (mental + phys) * 0.5)
        }.coerceAtLeast(0.15)

        // windowMatch — nakładanie się slotu z preferowanym oknem
        val windowMatch = if (a.windowStartMin == null || a.windowEndMin == null) 1.0
        else if (overlaps(slot, a.windowStartMin, a.windowEndMin)) 1.5 else 0.3

        // dayMatch — dziś w daysMask
        val dayMatch = if (a.daysMask and dayBit(ctx.dayOfWeek) != 0) 1.5 else 0.5

        // freqDebt — dług względem celu częstotliwości
        val freqDebt = a.frequencyTarget?.let { target ->
            if (target <= 0) 1.0 else {
                val done = ctx.weekCountByActivity[a.id] ?: 0
                val debt = (target - done).toDouble() / target
                if (debt > 0) 1.0 + debt else 0.6
            }
        } ?: 1.0

        // freshness — świeżość (dawno nierobiona → premia)
        val lastDone = ctx.lastDoneByActivity[a.id] ?: a.lastCompletedAt
        val now = if (ctx.nowMillis > 0) ctx.nowMillis else java.lang.System.currentTimeMillis()
        val freshness = if (lastDone == null) 1.0 else {
            val days = ((now - lastDone) / 86_400_000L).toInt()
            when {
                days <= 1 -> 0.5
                days >= 7 -> 1.5
                else -> 1.0
            }
        }

        // energyFit — koszt vs resztki energii dnia
        val b = ctx.energyBudget
        val energyFit = when (a.energyCost) {
            EnergyCost.HIGH -> if (b < 40) 0.4 else if (b > 70) 1.2 else 0.9
            EnergyCost.MED -> if (b < 25) 0.7 else 1.0
            EnergyCost.LOW -> if (b < 40) 1.2 else 1.0
        }.let { if (a.effortType == EffortType.RELAX && b < 40) it * 1.4 else it }

        return typMatch * windowMatch * dayMatch * freqDebt * freshness * energyFit
    }

    /** Proponowana godzina startu w slocie z uwzględnieniem okna aktywności. */
    fun proposedStart(a: Activity, slot: FreeSlot): Int {
        val desired = a.windowStartMin?.coerceAtLeast(slot.startMin) ?: slot.startMin
        val latest = slot.endMin - a.durationMinutes
        return desired.coerceIn(slot.startMin, latest.coerceAtLeast(slot.startMin))
    }

    /**
     * Zwraca do [count] propozycji, losując proporcjonalnie do wag (bez powtórzeń).
     * [random] wstrzykiwany dla testów.
     */
    fun plan(
        activities: List<Activity>,
        ctx: DayContext,
        slots: List<FreeSlot>,
        count: Int = 2,
        random: Random = Random.Default
    ): List<Suggestion> {
        if (activities.isEmpty() || slots.isEmpty()) return emptyList()
        val sortedSlots = slots.sortedBy { it.startMin }

        // Dla każdej aktywności wybierz najlepszy slot (najwyższa waga).
        data class Cand(val a: Activity, val slot: FreeSlot, val w: Double)
        val cands = activities.filter { it.isActive }.mapNotNull { a ->
            val best = sortedSlots
                .map { s -> s to weight(a, ctx, s) }
                .filter { it.second > 0.0 }
                .maxByOrNull { it.second }
            best?.let { Cand(a, it.first, it.second) }
        }

        // Losuj RÓŻNE aktywności proporcjonalnie do wag.
        val chosen = mutableListOf<Cand>()
        repeat(count) {
            val avail = cands.filter { c -> chosen.none { it.a.id == c.a.id } }
            if (avail.isEmpty()) return@repeat
            val picked = weightedPick(avail.map { it.a to it.w }, random) ?: return@repeat
            chosen += avail.first { it.a.id == picked.id }
        }

        // Ułóż je w slotach; jeśli dwie trafiają w to samo okno — sekwencyjnie.
        val slotCursor = mutableMapOf<FreeSlot, Int>()
        return chosen.map { c ->
            val floor = slotCursor[c.slot] ?: c.slot.startMin
            val desired = maxOf(proposedStart(c.a, c.slot), floor)
            val latest = (c.slot.endMin - c.a.durationMinutes).coerceAtLeast(c.slot.startMin)
            val start = desired.coerceIn(c.slot.startMin, latest)
            slotCursor[c.slot] = start + c.a.durationMinutes + 10
            Suggestion(c.a, c.slot, start)
        }
    }

    /** Jedno losowanie z puli ważonych; null gdy suma wag = 0. */
    fun weightedPick(pool: List<Pair<Activity, Double>>, random: Random): Activity? {
        val total = pool.sumOf { it.second }
        if (total <= 0.0) return null
        var r = random.nextDouble(total)
        for ((a, w) in pool) {
            r -= w
            if (r <= 0.0) return a
        }
        return pool.last().first
    }

    private fun overlaps(slot: FreeSlot, ws: Int, we: Int): Boolean =
        slot.startMin < we && slot.endMin > ws
}
