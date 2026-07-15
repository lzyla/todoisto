package pl.media30.todoisto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.media30.todoisto.ui.components.bouncy
import pl.media30.todoisto.ui.theme.GlassAccent
import pl.media30.todoisto.ui.theme.GlassDockBg
import pl.media30.todoisto.ui.theme.GlassRim
import pl.media30.todoisto.ui.theme.GlassTextPrimary
import pl.media30.todoisto.ui.theme.GlassTextSecondary

/** „Chmurka" z odpowiedzią AI — zjeżdża z góry ekranu (jak asystent tygodnia). */
@Composable
fun AiBubble(loading: Boolean, answer: String?, error: String?, needsKey: Boolean, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .shadow(28.dp, RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(GlassDockBg)
            .border(1.dp, GlassRim.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Asystent AI", fontSize = 17.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).bouncy(0.9f, onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Zamknij", tint = GlassTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        AiContent(loading, answer, error, needsKey)
    }
}

/** Arkusz z odpowiedzią „Zapytaj AI" (OpenAI) — wariant dolny (zapasowy). */
@Composable
fun AiAnswerSheet(loading: Boolean, answer: String?, error: String?, needsKey: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = GlassAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Asystent AI", fontSize = 19.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
        }
        Spacer(Modifier.height(16.dp))
        AiContent(loading, answer, error, needsKey)
    }
}

@Composable
private fun AiContent(loading: Boolean, answer: String?, error: String?, needsKey: Boolean) {
    Column {
        when {
            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = GlassAccent, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Myślę nad najlepszym sposobem…", fontSize = 13.5.sp, color = GlassTextSecondary)
            }
            needsKey -> Column {
                Text("Brak klucza OpenAI", fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Otwórz menu (☰) → Klucz AI (OpenAI) i wklej swój klucz z platform.openai.com. " +
                    "Klucz zostaje tylko na tym urządzeniu.",
                    fontSize = 13.sp, lineHeight = 19.sp, color = GlassTextSecondary
                )
            }
            error != null -> Column {
                Text("Nie udało się zapytać AI", fontSize = 15.sp, fontWeight = FontWeight.W800, color = GlassTextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(error, fontSize = 13.sp, lineHeight = 19.sp, color = GlassTextSecondary)
            }
            answer != null -> Box(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                // Linki narzędzi: niebieskie, podkreślone; klik otwiera stronę www.
                Text(aiAnnotated(answer, Color(0xFF2563EB)), fontSize = 13.5.sp, lineHeight = 20.sp, color = GlassTextPrimary)
            }
        }
    }
}

private val mdLinkRegex = Regex("""\[([^\]]+)]\((https?://[^\s)]+)\)""")
private val urlRegex = Regex("""https?://[^\s)\]]+""")

/** Znane narzędzia produktywności → strona www (do auto-linkowania nazw). */
private val toolUrls: Map<String, String> = linkedMapOf(
    "Google Calendar" to "https://calendar.google.com",
    "Kalendarz Google" to "https://calendar.google.com",
    "Google Sheets" to "https://sheets.google.com",
    "Arkusze Google" to "https://sheets.google.com",
    "Google Keep" to "https://keep.google.com",
    "Google Drive" to "https://drive.google.com",
    "Google Tasks" to "https://tasks.google.com",
    "Microsoft To Do" to "https://to-do.office.com",
    "Microsoft To-Do" to "https://to-do.office.com",
    "Notion" to "https://notion.so",
    "Todoist" to "https://todoist.com",
    "Zapier" to "https://zapier.com",
    "IFTTT" to "https://ifttt.com",
    "Trello" to "https://trello.com",
    "Asana" to "https://asana.com",
    "ClickUp" to "https://clickup.com",
    "Slack" to "https://slack.com",
    "Obsidian" to "https://obsidian.md",
    "Evernote" to "https://evernote.com",
    "Airtable" to "https://airtable.com",
    "ChatGPT" to "https://chat.openai.com",
    "Calendly" to "https://calendly.com",
    "Toggl" to "https://toggl.com",
    "TickTick" to "https://ticktick.com",
    "Habitica" to "https://habitica.com",
    "Canva" to "https://canva.com",
    "Loom" to "https://loom.com",
    "Grammarly" to "https://grammarly.com",
    "Miro" to "https://miro.com",
    "Gmail" to "https://mail.google.com",
    "Pomofocus" to "https://pomofocus.io",
    "Make" to "https://make.com"
)

private val toolRegex = Regex(
    "(?i)\\b(" + toolUrls.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } + ")\\b"
)

/** Do fragmentu tekstu (bez linków) dokleja auto-linki znanych narzędzi. */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendWithToolLinks(chunk: String, styles: TextLinkStyles) {
    var i = 0
    while (i < chunk.length) {
        val m = toolRegex.find(chunk, i)
        if (m == null) { append(chunk.substring(i)); break }
        if (m.range.first > i) append(chunk.substring(i, m.range.first))
        val name = m.value
        val url = toolUrls.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
        if (url != null) withLink(LinkAnnotation.Url(url, styles)) { append(name) } else append(name)
        i = m.range.last + 1
    }
}

/**
 * Odpowiedź AI z KLIKALNYMI linkami: linki Markdown [nazwa](url), gołe adresy
 * http(s) ORAZ nazwy znanych narzędzi (nawet bez podanego adresu) — wszystkie
 * niebieskie i podkreślone; klik otwiera stronę (domyślny UriHandler).
 */
internal fun aiAnnotated(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    val linkStyles = TextLinkStyles(
        SpanStyle(color = linkColor, fontWeight = FontWeight.W700, textDecoration = TextDecoration.Underline)
    )
    var i = 0
    while (i < text.length) {
        val md = mdLinkRegex.find(text, i)
        val url = urlRegex.find(text, i)
        val useMd = md != null && (url == null || md.range.first <= url.range.first)
        val next = if (useMd) md else url
        if (next == null) { appendWithToolLinks(text.substring(i), linkStyles); break }
        if (next.range.first > i) appendWithToolLinks(text.substring(i, next.range.first), linkStyles)
        if (useMd && md != null) {
            withLink(LinkAnnotation.Url(md.groupValues[2], linkStyles)) { append(md.groupValues[1]) }
        } else {
            withLink(LinkAnnotation.Url(next.value, linkStyles)) { append(next.value) }
        }
        i = next.range.last + 1
    }
}
