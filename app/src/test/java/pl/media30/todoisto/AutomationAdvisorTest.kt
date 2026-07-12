package pl.media30.todoisto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.media30.todoisto.data.AutomationAdvisor

class AutomationAdvisorTest {

    @Test fun reportMatchesWritingTools() {
        val t = AutomationAdvisor.advise("Przygotować raport miesięczny")
        assertTrue(t.canAutomate)
        assertTrue(t.tools.any { it == "AI" })
        assertTrue(t.steps.isNotEmpty())
        assertTrue(t.aiPrompt.contains("raport", ignoreCase = true))
    }

    @Test fun neverNamesClaudeOrGpt() {
        listOf("Przygotować raport", "Zbudować stronę", "Odpisać na maila", "Kampania NGO").forEach { title ->
            val t = AutomationAdvisor.advise(title)
            val all = (t.tools + t.steps + t.headline).joinToString(" ").lowercase()
            assertFalse(all.contains("claude"))
            assertFalse(all.contains("gpt"))
            assertFalse(all.contains("codex"))
        }
    }

    @Test fun translationMatchesDeepL() {
        val t = AutomationAdvisor.advise("Przetłumaczyć ofertę na angielski")
        assertTrue(t.canAutomate)
        assertTrue(t.tools.contains("DeepL"))
    }

    @Test fun graphicsMatchesCanva() {
        val t = AutomationAdvisor.advise("Zaprojektować plakat na social media")
        assertTrue(t.tools.contains("Canva"))
    }

    @Test fun codeMatchesDevTools() {
        val t = AutomationAdvisor.advise("Naprawić bug w API i wdrożyć")
        assertTrue(t.tools.any { it == "VS Code" || it == "Vercel" || it == "GitHub" })
    }

    @Test fun meetingMatchesZoom() {
        val t = AutomationAdvisor.advise("Spotkanie z klientem na Zoomie")
        assertTrue(t.tools.contains("Zoom"))
    }

    @Test fun unknownStillOffersAiPrompt() {
        val t = AutomationAdvisor.advise("Podlać kwiaty")
        assertFalse(t.canAutomate)
        assertTrue(t.aiPrompt.isNotBlank())
        assertEquals(listOf("AI"), t.tools)
    }
}
