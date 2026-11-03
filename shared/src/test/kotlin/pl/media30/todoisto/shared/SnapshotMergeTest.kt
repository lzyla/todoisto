package pl.media30.todoisto.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotMergeTest {

    private fun task(id: Long, title: String, updatedAt: Long = 0, completed: Boolean = false) =
        CloudTask(id = id, title = title, updatedAt = updatedAt, isCompleted = completed, createdAt = 1000)

    @Test fun `dodane po obu stronach zostaja oba`() {
        val local = CloudSnapshot(tasks = listOf(task(1, "lokalne"), task(2, "wspolne")))
        val remote = CloudSnapshot(tasks = listOf(task(2, "wspolne"), task(3, "zdalne")))
        val merged = SnapshotMerge.merge(local, remote)
        assertEquals(setOf(1L, 2L, 3L), merged.tasks.map { it.id }.toSet())
    }

    // Uwaga: w realnych danych znaczniki modyfikacji są ZAWSZE >= createdAt
    // (nie da się edytować przed utworzeniem), więc testy używają takich wartości.
    @Test fun `konflikt - wygrywa nowsza wersja`() {
        val local = CloudSnapshot(tasks = listOf(task(1, "stara", updatedAt = 2100)))
        val remote = CloudSnapshot(tasks = listOf(task(1, "nowa", updatedAt = 2200)))
        val merged = SnapshotMerge.merge(local, remote)
        assertEquals("nowa", merged.tasks.single { it.id == 1L }.title)
    }

    @Test fun `konflikt - lokalna nowsza zostaje`() {
        val local = CloudSnapshot(tasks = listOf(task(1, "lokalna-nowa", updatedAt = 3000)))
        val remote = CloudSnapshot(tasks = listOf(task(1, "zdalna-stara", updatedAt = 2000)))
        val merged = SnapshotMerge.merge(local, remote)
        assertEquals("lokalna-nowa", merged.tasks.single { it.id == 1L }.title)
    }

    @Test fun `ukonczenie z completedAt wygrywa gdy brak updatedAt`() {
        val local = CloudSnapshot(tasks = listOf(task(1, "x", updatedAt = 0).copy(completedAt = 5000, isCompleted = true)))
        val remote = CloudSnapshot(tasks = listOf(task(1, "x", updatedAt = 0)))
        val merged = SnapshotMerge.merge(local, remote)
        assertTrue(merged.tasks.single { it.id == 1L }.isCompleted)
    }

    @Test fun `nagrobek (usuniecie) wygrywa nad zywa starsza wersja`() {
        // Usunięte na Macu (nagrobek, nowszy) vs wciąż żywe w chmurze (starsze).
        val local = CloudSnapshot(tasks = listOf(task(1, "x", updatedAt = 5000).copy(deleted = true)))
        val remote = CloudSnapshot(tasks = listOf(task(1, "x", updatedAt = 2000)))
        val merged = SnapshotMerge.merge(local, remote)
        assertTrue("usunięcie powinno się utrzymać", merged.tasks.single { it.id == 1L }.deleted)
    }

    @Test fun `kodek zachowuje nagrobek (round-trip)`() {
        val snap = CloudSnapshot(tasks = listOf(CloudTask(id = 9, title = "usuniete", deleted = true, updatedAt = 123)))
        val back = CloudBackupCodec.fromJson(CloudBackupCodec.toJson(snap))
        assertTrue(back.tasks.single().deleted)
    }

    @Test fun `kodek zachowuje pola i updatedAt (round-trip)`() {
        val snap = CloudSnapshot(
            tasks = listOf(CloudTask(id = 7, title = "Zadanie", priority = "P2", dueDate = 20000,
                dueTimeMinutes = 600, recurrence = "DAILY", labelIds = listOf(3, 4), updatedAt = 999, createdAt = 111)),
            projects = listOf(CloudProject(id = 1, name = "Praca", colorArgb = 0xFF4D6BFFL)),
            labels = listOf(CloudLabel(id = 3, name = "pilne"))
        )
        val back = CloudBackupCodec.fromJson(CloudBackupCodec.toJson(snap))
        val t = back.tasks.single()
        assertEquals("Zadanie", t.title)
        assertEquals("P2", t.priority)
        assertEquals("DAILY", t.recurrence)
        assertEquals(listOf(3L, 4L), t.labelIds)
        assertEquals(999L, t.updatedAt)
        assertEquals("Praca", back.projects.single().name)
        assertEquals("pilne", back.labels.single().name)
    }

    @Test fun `obszary i sekcje przechodza przez kodek i scalanie`() {
        val local = CloudSnapshot(areas = listOf(CloudArea(id = 1, name = "Dom")), sections = listOf(CloudSection(id = 5, projectId = 2, name = "Do zrobienia")))
        val remote = CloudSnapshot(areas = listOf(CloudArea(id = 1, name = "Dom (chmura)"), CloudArea(id = 2, name = "Praca")))
        val merged = SnapshotMerge.merge(local, remote)
        assertEquals(listOf("Dom (chmura)", "Praca"), merged.areas.map { it.name })   // chmura wygrywa, suma po id
        assertEquals("Do zrobienia", merged.sections.single().name)
        val back = CloudBackupCodec.fromJson(CloudBackupCodec.toJson(merged))
        assertEquals(2, back.areas.size)
        assertEquals(2L, back.sections.single().projectId)
    }

    @Test fun `stary JSON bez obszarow i sekcji nadal sie wczytuje`() {
        val back = CloudBackupCodec.fromJson("""{"version":1,"tasks":[],"projects":[],"labels":[]}""")
        assertTrue(back.areas.isEmpty() && back.sections.isEmpty())
    }
}
