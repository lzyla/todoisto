package pl.media30.todoisto.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.media30.todoisto.data.AutomationAdvisor
import pl.media30.todoisto.shared.CloudTask

// ─── Asystent tygodnia (ikona ✦ w pasku) — jak `WeekBrief` ──────────────────
@Composable
fun BoxScope.WeekBrief(st: AppState) {
    val g = LocalGlass.current
    AnimatedVisibility(st.briefOpen, modifier = Modifier.align(Alignment.TopCenter), enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        var loading by remember { mutableStateOf(true) }
        var moved by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { loading = true; delay(900); loading = false }
        val today = st.today
        st.rev
        val tasks = st.repo.tasks.filter { it.parentId == null && !it.isCompleted }
        val overdue = tasks.count { it.dueDate != null && it.dueDate!! < today }
        val week = tasks.filter { it.recurrence == null && it.dueDate != null && it.dueDate!! in (today + 1)..(today + 7) }.sortedWith(compareBy({ it.priority }, { it.dueDate }))
        val byDay = week.groupBy { it.dueDate!! }
        val busiest = byDay.maxByOrNull { it.value.size }
        val freeDays = (1..7).count { byDay[today + it].isNullOrEmpty() }
        val insight = buildString {
            if (overdue > 0) append("Masz $overdue zaległych zadań. ")
            if (busiest != null) append("Najbardziej obciążony dzień to ${WEEKDAYS[java.time.LocalDate.ofEpochDay(busiest.key).dayOfWeek.value - 1]} (${busiest.value.size} zadań). ")
            if (freeDays > 0 && overdue > 0) append("Masz $freeDays wolnych dni na nadrobienie zaległości.")
            if (isEmpty()) append("Tydzień wygląda spokojnie — dobre okno na zadania z listy Pomysłów.")
        }
        Column(
            Modifier.padding(horizontal = 24.dp).widthIn(max = 640.dp).fillMaxWidth().glass(RoundedCornerShape(26.dp), strong = true, elevation = 24.dp).padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = g.accent, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Asystent tygodnia", fontSize = 16.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora, modifier = Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(50)).background(g.accent.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("AI", fontSize = 10.sp, fontWeight = FontWeight.W800, color = g.accent) }
            }
            Spacer(Modifier.height(12.dp))
            if (loading) {
                Text("Analizuję Twój tydzień…", fontSize = 12.5.sp, color = g.textSecondary, fontFamily = Manrope)
                Spacer(Modifier.height(8.dp))
                repeat(3) { Box(Modifier.fillMaxWidth(0.7f - it * 0.15f).height(10.dp).clip(RoundedCornerShape(50)).background(g.hair)); Spacer(Modifier.height(6.dp)) }
            } else {
                Text(insight, fontSize = 13.5.sp, color = g.textPrimary, fontFamily = Manrope, lineHeight = 19.sp)
                if (week.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("Najważniejsze w tym tygodniu", g.textSecondary)
                    Spacer(Modifier.height(6.dp))
                    week.take(4).forEach { t ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { st.openDetail(t.id); st.briefOpen = false }.padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Dot(priorityColor(t.priority), 8.dp); Spacer(Modifier.width(10.dp))
                            Text(t.title, fontSize = 13.5.sp, fontWeight = FontWeight.W600, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f), maxLines = 1)
                            val d = java.time.LocalDate.ofEpochDay(t.dueDate!!)
                            Text(if (t.dueDate == today + 1) "jutro" else "${WEEKDAYS_SHORT[d.dayOfWeek.value - 1]} ${d.dayOfMonth}", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
                        }
                    }
                }
                if (overdue > 0) {
                    Spacer(Modifier.height(12.dp))
                    PrimaryBtn(if (moved) "Przeniesiono ✓" else "Przenieś zaległe na dzisiaj", enabled = !moved) { st.moveOverdueToToday(); moved = true }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).width(44.dp).height(5.dp).clip(RoundedCornerShape(50)).background(g.hair).clickable { st.briefOpen = false })
        }
    }
}

// ─── Karta AI w szczegółach zadania — jak `AutomationCard` ──────────────────
@Composable
fun AutomationCard(st: AppState, task: CloudTask, expanded: Boolean, onExpand: () -> Unit) {
    val g = LocalGlass.current
    val tip = remember(task.title, task.notes) { AutomationAdvisor.advise(task.title, task.notes) }
    val ai = st.aiAsk
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).livingGradient(RoundedCornerShape(22.dp)).clickable { if (!expanded) { onExpand(); if (ai == null) st.askAi(tip.aiPrompt) } }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFA47CFF), g.accent))), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (expanded) tip.headline else "Nie wiesz jak się zabrać?", fontSize = 14.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
                Text(if (expanded) tip.tools.joinToString(" · ") else "Stuknij — ogarniemy to razem ✨", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope)
            }
        }
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            when {
                ai == null -> {
                    tip.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", fontSize = 13.sp, color = g.textPrimary, fontFamily = Manrope, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 4.dp)) }
                    Spacer(Modifier.height(8.dp))
                    PrimaryBtn("Zróbmy to z AI", icon = Icons.Outlined.AutoAwesome) { st.askAi(tip.aiPrompt) }
                }
                ai.loading -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), color = g.accent, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Myślę nad najlepszym sposobem…", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope) }
                ai.needsKey -> Text("Dodaj klucz w Ustawieniach (Asystent AI → Klucz API), żeby zapytać AI na żywo.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
                ai.error != null -> Text("Nie udało się zapytać AI: ${ai.error}", fontSize = 13.sp, color = g.danger, fontFamily = Manrope)
                ai.answer != null -> Box(Modifier.fillMaxWidth().heightIn(max = 380.dp).clip(RoundedCornerShape(14.dp)).background(g.field).padding(12.dp).verticalScroll(rememberScrollState())) {
                    AiText(cleanMarkdown(ai.answer))
                }
            }
        }
    }
}

/** Czyści markdown jak `cleanMarkdown` w Androidzie. */
fun cleanMarkdown(s: String): String = s.lines().map { line ->
    line.replace("**", "").trimStart('#').trimStart().let { l -> if (l.startsWith("- ") || l.startsWith("* ")) "• " + l.drop(2) else l }
}.joinToString("\n")

private val URL_RE = Regex("""\[([^\]]+)]\((https?://[^\s)]+)\)|(https?://[^\s)]+)""")

/** Tekst odpowiedzi AI z klikalnymi linkami (jak `aiAnnotated`). */
@Composable
fun AiText(text: String) {
    val g = LocalGlass.current
    val linkColor = Color(0xFF2563EB)
    val annotated: AnnotatedString = remember(text) {
        buildAnnotatedString {
            var last = 0
            URL_RE.findAll(text).forEach { m ->
                append(text.substring(last, m.range.first))
                val label = m.groups[1]?.value ?: m.groups[3]?.value ?: ""
                val url = m.groups[2]?.value ?: m.groups[3]?.value ?: ""
                pushStringAnnotation("URL", url)
                withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.W700, textDecoration = TextDecoration.Underline)) { append(label) }
                pop()
                last = m.range.last + 1
            }
            append(text.substring(last))
        }
    }
    ClickableText(annotated, style = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp, color = g.textPrimary, fontFamily = Manrope)) { off ->
        annotated.getStringAnnotations("URL", off, off).firstOrNull()?.let { openUrl(it.item) }
    }
}

fun openUrl(url: String) { runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) } }

// ─── Asystent zdjęcia — jak `ScanResultSheet` ──────────────────────────────
@Composable
fun ScanResultSheet(st: AppState, s: ScanState) {
    val g = LocalGlass.current
    Sheet(onDismiss = { st.scan = null }, maxWidth = 520.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(g.accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(10.dp))
            Text("Asystent zdjęcia", fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        }
        Spacer(Modifier.height(14.dp))
        when {
            s.loading -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), color = g.accent, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Analizuję zdjęcie…", fontSize = 13.5.sp, color = g.textSecondary, fontFamily = Manrope) }
            s.needsKey -> Text("Dodaj klucz API (Ustawienia → Asystent AI), aby skanować zdjęcia.", fontSize = 13.5.sp, color = g.textSecondary, fontFamily = Manrope)
            s.error != null -> Text(s.error, fontSize = 13.5.sp, color = g.danger, fontFamily = Manrope)
            else -> {
                if (s.summary.isNotBlank()) { Text(s.summary, fontSize = 13.5.sp, lineHeight = 19.sp, color = g.textSecondary, fontFamily = Manrope); Spacer(Modifier.height(12.dp)) }
                when {
                    s.tasks.isNotEmpty() -> {
                        Text("Znalezione zadania:", fontSize = 12.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
                        Spacer(Modifier.height(6.dp))
                        s.tasks.forEach { t ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(g.field).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Dot(g.accent, 7.dp); Spacer(Modifier.width(10.dp)); Text(t, fontSize = 13.5.sp, fontWeight = FontWeight.W600, color = g.textPrimary, fontFamily = Manrope)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        PrimaryBtn("Dodaj wszystkie (${s.tasks.size})") { st.addScanTasks(s.tasks) }
                    }
                    s.suggestion.isNotBlank() -> {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(g.accent.copy(alpha = 0.12f)).padding(14.dp)) {
                            Text("Propozycja", fontSize = 11.sp, fontWeight = FontWeight.W800, color = g.accent, fontFamily = Manrope)
                            Text(s.suggestion, fontSize = 15.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Manrope)
                            if (s.plan.isNotBlank()) Text(s.plan, fontSize = 13.sp, lineHeight = 19.sp, color = g.textSecondary, fontFamily = Manrope)
                        }
                        Spacer(Modifier.height(12.dp))
                        PrimaryBtn("Dodaj to zadanie") { st.addScanSuggestion(s.suggestion, s.plan) }
                    }
                    else -> Text("Nie rozpoznałem nic konkretnego. Spróbuj z bliższym/ostrzejszym zdjęciem.", fontSize = 13.5.sp, color = g.textSecondary, fontFamily = Manrope)
                }
                Spacer(Modifier.height(10.dp))
                Text("Anuluj", fontSize = 13.sp, fontWeight = FontWeight.W700, color = g.textSecondary, fontFamily = Manrope, modifier = Modifier.align(Alignment.CenterHorizontally).clickable { st.scan = null }.padding(8.dp))
            }
        }
    }
}

// ─── Szacowany czas na dziś + szacunek AI — jak `EstimateBreakdownSheet` ────
@Composable
fun EstimateSheet(st: AppState) {
    val g = LocalGlass.current
    st.rev
    val open = st.todayOpen()
    val sum = open.sumOf { it.durationMinutes ?: 20 }
    val e = st.estimateAi
    Sheet(onDismiss = { st.showEstimate = false }, maxWidth = 520.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, null, tint = g.accent, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
            Text("Szacowany czas na dziś", fontSize = 18.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EstTile("SUMA ZADAŃ", fmtDuration(sum).replace("Brak", "—"), "${open.size} ${plural(open.size, "zadanie", "zadania", "zadań")} · brak czasu = 20 min", Modifier.weight(1f)) {}
            EstTile("SZACUNEK AI", when { e.loading -> "…"; e.minutes != null -> fmtDuration(e.minutes).replace("Brak", "0 min"); else -> "—" },
                when { e.needsKey -> "Brak klucza AI (Ustawienia)"; e.error != null -> e.error; e.minutes != null -> "wg AI i Twojej średniej"; else -> "Dotknij, aby policzyć" }, Modifier.weight(1f)) { st.estimateTodayWithAi() }
        }
        if (st.settings.avgActualMinutes > 0) { Spacer(Modifier.height(8.dp)); Text("Twoja historyczna średnia: ~${st.settings.avgActualMinutes} min na zadanie.", fontSize = 12.sp, color = g.textSecondary, fontFamily = Manrope) }
        Spacer(Modifier.height(14.dp))
        SectionHeader("Rozbicie", g.textSecondary)
        Spacer(Modifier.height(6.dp))
        if (open.isEmpty()) Text("Nic nie zaplanowane na dziś.", fontSize = 13.sp, color = g.textSecondary, fontFamily = Manrope)
        open.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(priorityColor(t.priority), 7.dp); Spacer(Modifier.width(10.dp))
                Text(t.title, fontSize = 13.5.sp, color = g.textPrimary, fontFamily = Manrope, modifier = Modifier.weight(1f), maxLines = 1)
                Text(t.durationMinutes?.let { "$it min" } ?: "~20 min", fontSize = 12.sp, fontWeight = FontWeight.W700, color = if (t.durationMinutes == null) g.textSecondary else g.textPrimary, fontFamily = Manrope)
            }
        }
    }
}

@Composable
private fun EstTile(label: String, value: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val g = LocalGlass.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(g.field).clickable { onClick() }.padding(14.dp)) {
        Text(label, fontSize = 9.5.sp, fontWeight = FontWeight.W800, letterSpacing = 1.sp, color = g.textSecondary, fontFamily = Manrope)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.W800, color = g.textPrimary, fontFamily = Sora)
        Text(sub, fontSize = 11.5.sp, color = g.textSecondary, fontFamily = Manrope)
    }
}
