package pl.media30.todoisto.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.data.Task
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassBackground
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary
import pl.media30.todoisto.ui.theme.PrioColors
import pl.media30.todoisto.ui.theme.glass
import java.time.Instant
import java.time.ZoneId

/**
 * „Statystyki" — wizualizacje w kolorystyce apki: ukończenia wg dnia tygodnia,
 * rozkład priorytetów, pory otwierania zadań, przesunięcia i średni czas realizacji.
 */
@Composable
fun StatsScreen(
    tasks: List<Task>,
    deferCount: Long,
    openHours: List<Int>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val zone = ZoneId.systemDefault()

    val completed = tasks.filter { it.isCompleted && (it.completedAt ?: 0L) > 0L }
    // Ukończone wg dnia tygodnia (Pn..Nd)
    val weekday = IntArray(7)
    completed.forEach {
        val d = Instant.ofEpochMilli(it.completedAt!!).atZone(zone).dayOfWeek.value // 1=Pn..7=Nd
        weekday[d - 1]++
    }
    // Priorytety aktywnych zadań (P1..P4)
    val prio = IntArray(4)
    tasks.filter { !it.isCompleted && it.parentId == null }.forEach { prio[it.priority.ordinal]++ }
    // Średni czas realizacji (dni) z createdAt→completedAt
    val durs = completed.filter { it.createdAt > 0L && it.completedAt!! >= it.createdAt }
        .map { (it.completedAt!! - it.createdAt) / 86_400_000.0 }
    val avgDays = if (durs.isNotEmpty()) durs.average() else 0.0
    // Pory otwierania (4 przedziały)
    val hours = if (openHours.size == 24) openHours else List(24) { 0 }
    val parts = listOf(
        "Noc" to (0..5).sumOf { hours[it] },
        "Rano" to (6..11).sumOf { hours[it] },
        "Popoł." to (12..17).sumOf { hours[it] },
        "Wiecz." to (18..23).sumOf { hours[it] }
    )
    // Ostatnie 30 dni + passa (streak)
    fun dayOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
    val today = java.time.LocalDate.now().toEpochDay()
    val daysSet = completed.map { dayOf(it.completedAt!!) }.toHashSet()
    val last30 = (0..29).map { off -> val d = today - 29 + off; completed.count { dayOf(it.completedAt!!) == d } }
    var streak = 0
    if (daysSet.contains(today) || daysSet.contains(today - 1)) {
        var d = if (daysSet.contains(today)) today else today - 1
        while (daysSet.contains(d)) { streak++; d-- }
    }
    val bestStreak = run {
        val s = daysSet.sorted(); var b = 0; var r = 0; var prev = Long.MIN_VALUE
        for (x in s) { r = if (x == prev + 1) r + 1 else 1; if (r > b) b = r; prev = x }; b
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).glass(CircleShape).bouncy(0.9f, onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć", tint = GlassTextPrimary, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("Statystyki", fontSize = 24.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp).padding(top = 6.dp, bottom = 28.dp)
            ) {
                // Kafelki liczbowe
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Ukończone", "${completed.size}", Color(0xFF34D399), Modifier.weight(1f))
                    StatTile("Przesunięcia", "$deferCount", Color(0xFFFB7185), Modifier.weight(1f))
                    StatTile("Śr. czas", if (avgDays > 0) "%.1f d".format(avgDays) else "—", Color(0xFF6B8AFF), Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))

                // Passa (streak)
                Row(
                    Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔥", fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Passa ukończeń", fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                        Text(
                            if (streak > 0) "Domykasz zadania od $streak " + dni(streak) else "Zacznij dziś — domknij jedno zadanie",
                            fontSize = 12.sp, color = GlassTextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$streak", fontSize = 26.sp, fontWeight = FontWeight.W800, color = GlassAccent)
                        Text("rekord $bestStreak", fontSize = 11.sp, color = GlassTextSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))

                ChartCard("Ostatnie 30 dni") {
                    MiniBars(last30.map { it.toFloat() }, Brush.verticalGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0))))
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("30 dni temu", fontSize = 10.sp, color = GlassTextSecondary)
                        Text("dziś", fontSize = 10.sp, color = GlassTextSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))

                ChartCard("Ukończone wg dnia tygodnia") {
                    VBarChart(
                        listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").mapIndexed { i, l -> l to weekday[i].toFloat() },
                        Brush.verticalGradient(listOf(Color(0xFFA47CFF), Color(0xFF6B3FE0)))
                    )
                }
                Spacer(Modifier.height(16.dp))

                ChartCard("Priorytety (aktywne)") {
                    VBarChartColored(
                        listOf("P1", "P2", "P3", "P4").mapIndexed { i, l -> Triple(l, prio[i].toFloat(), PrioColors[i]) }
                    )
                }
                Spacer(Modifier.height(16.dp))

                ChartCard("Kiedy otwierasz zadania") {
                    VBarChart(
                        parts.map { it.first to it.second.toFloat() },
                        Brush.verticalGradient(listOf(Color(0xFF5EE7D0), Color(0xFF2DD4BF)))
                    )
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.glass(RoundedCornerShape(20.dp)).padding(vertical = 16.dp, horizontal = 12.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, maxLines = 1)
        Text(label, fontSize = 11.5.sp, color = GlassTextSecondary, maxLines = 1)
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun VBarChart(entries: List<Pair<String, Float>>, brush: Brush, area: Dp = 110.dp) {
    val max = (entries.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(1f)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { (label, v) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(v.toInt().toString(), fontSize = 10.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.fillMaxWidth(0.66f)
                        .height((area.value * (v / max)).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(5.dp))
                Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
            }
        }
    }
}

private fun dni(n: Int): String = when {
    n == 1 -> "dnia"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "dni"
    else -> "dni"
}

@Composable
private fun MiniBars(values: List<Float>, brush: Brush, area: Dp = 90.dp) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        values.forEach { v ->
            Box(
                Modifier.weight(1f)
                    .height((area.value * (v / max)).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun VBarChartColored(entries: List<Triple<String, Float, Color>>, area: Dp = 110.dp) {
    val max = (entries.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(1f)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { (label, v, c) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(v.toInt().toString(), fontSize = 10.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.fillMaxWidth(0.66f)
                        .height((area.value * (v / max)).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                        .background(c)
                )
                Spacer(Modifier.height(5.dp))
                Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.W700, color = GlassTextSecondary)
            }
        }
    }
}
