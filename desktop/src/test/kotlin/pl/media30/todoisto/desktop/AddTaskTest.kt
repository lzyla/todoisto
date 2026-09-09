package pl.media30.todoisto.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.media30.todoisto.data.QuickAddParser
import pl.media30.todoisto.shared.CloudTask
import java.io.File
import java.time.LocalDate

/** Ścieżka „dodaj zadanie" na desktopie — od parsera do repozytorium i zapisu na dysk. */
class AddTaskTest {
    private lateinit var home: File

    @Before fun tempHome() {
        home = File.createTempFile("todoisto-home", "").apply { delete(); mkdirs() }
        System.setProperty("user.home", home.absolutePath)
    }

    @Test fun `plain title is added and visible`() {
        val repo = TaskRepository()
        val p = QuickAddParser().parse("Kupić mleko")
        assertEquals("Kupić mleko", p.title)
        val t = repo.add(CloudTask(id = 0, title = p.title, priority = p.priority.name, dueDate = p.dueDate))
        assertTrue(t.id > 0)
        assertEquals(listOf("Kupić mleko"), repo.tasks.map { it.title })
        // zapis na dysk i ponowny odczyt
        assertEquals(1, TaskRepository().tasks.size)
    }

    @Test fun `natural language sets date priority project`() {
        val repo = TaskRepository()
        val p = QuickAddParser().parse("Zadzwonić jutro o 15 p1")
        assertEquals("Zadzwonić", p.title)
        assertEquals(LocalDate.now().plusDays(1).toEpochDay(), p.dueDate)
        assertEquals(15 * 60, p.dueTimeMinutes)
        assertEquals("P1", p.priority.name)
        repo.add(CloudTask(id = 0, title = p.title, priority = p.priority.name, dueDate = p.dueDate, dueTimeMinutes = p.dueTimeMinutes))
        assertNotNull(repo.tasks.single().dueDate)
    }

    @Test fun `unknown hashtag does not blank the title`() {
        val p = QuickAddParser().parse("#Praca raport dla szefa")
        assertTrue(p.title.isNotBlank())
    }

    @Test fun `overdue reschedule moves to today`() {
        val repo = TaskRepository()
        val today = LocalDate.now().toEpochDay()
        repo.add(CloudTask(id = 0, title = "stare", dueDate = today - 3))
        repo.rescheduleOverdueToToday()
        assertEquals(today, repo.tasks.single().dueDate)
    }
}
