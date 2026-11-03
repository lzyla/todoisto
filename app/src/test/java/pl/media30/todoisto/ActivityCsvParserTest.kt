package pl.media30.todoisto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.media30.todoisto.data.ActivityCsvParser
import pl.media30.todoisto.data.EffortType
import pl.media30.todoisto.data.EnergyCost
import pl.media30.todoisto.data.Place

class ActivityCsvParserTest {

    @Test
    fun parsesBasicRowsWithHeader() {
        val csv = """
            nazwa,minuty,wysiłek,miejsce,energia
            Spacer,30,fizyczny,na zewnątrz,niski
            Czytanie,45,regeneracja,dom,niski
        """.trimIndent()
        val acts = ActivityCsvParser.parse(csv)
        assertEquals(2, acts.size)
        assertEquals("Spacer", acts[0].name)
        assertEquals(30, acts[0].durationMinutes)
        assertEquals(EffortType.PHYSICAL, acts[0].effortType)
        assertEquals(Place.OUTSIDE, acts[0].place)
        assertEquals(EnergyCost.LOW, acts[0].energyCost)
        assertEquals(EffortType.RELAX, acts[1].effortType)
        assertEquals(Place.HOME, acts[1].place)
    }

    @Test
    fun handlesSemicolonAndNoHeader() {
        val csv = "Trening;60;fiz;zew;wysoki\nNauka;20;umysłowy;dom;średni"
        val acts = ActivityCsvParser.parse(csv)
        assertEquals(2, acts.size)
        assertEquals(60, acts[0].durationMinutes)
        assertEquals(EnergyCost.HIGH, acts[0].energyCost)
        assertEquals(EffortType.MENTAL, acts[1].effortType)
        assertEquals(EnergyCost.MED, acts[1].energyCost)
    }

    @Test
    fun defaultsAndSkipsEmpty() {
        val csv = "Bieganie\n\n   \nInny,abc"
        val acts = ActivityCsvParser.parse(csv)
        assertEquals(2, acts.size)
        assertEquals(30, acts[0].durationMinutes) // brak → default 30
        assertEquals(EffortType.RELAX, acts[0].effortType)
        assertEquals(30, acts[1].durationMinutes) // "abc" → default 30
    }

    @Test
    fun normalizesGoogleEditUrlToCsvExport() {
        val edit = "https://docs.google.com/spreadsheets/d/ABC123/edit#gid=456"
        val norm = ActivityCsvParser.normalizeSheetUrl(edit)
        assertTrue(norm.contains("/export?format=csv"))
        assertTrue(norm.contains("gid=456"))
    }

    @Test
    fun leavesPublishedCsvUrlUntouched() {
        val pub = "https://docs.google.com/spreadsheets/d/e/2PACX-abc/pub?output=csv"
        assertEquals(pub, ActivityCsvParser.normalizeSheetUrl(pub))
    }
}
