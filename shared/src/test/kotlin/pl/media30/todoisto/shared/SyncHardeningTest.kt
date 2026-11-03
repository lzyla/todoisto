package pl.media30.todoisto.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy poprawek: tożsamość rekordów, nagrobki nie-zadań, edycje projektów, pobieranie. */
class SyncHardeningTest {

    private val now = 10_000_000_000L

    @Test fun `Ids - zakres bezpieczny dla JS i ponad starymi id`() {
        repeat(10_000) {
            val id = Ids.next()
            assertTrue(id >= (1L shl 41))
            assertTrue(id < (1L shl 53))
            assertTrue(id > 1_900_000_000_000L) // > milisekundowe id z Maca
        }
        assertEquals(10_000, (1..10_000).map { Ids.next() }.toSet().size)
    }

    @Test fun `zmiana nazwy projektu na telefonie nie jest cofana przez starsza chmure`() {
        val local = CloudSnapshot(projects = listOf(CloudProject(1, "Nowa nazwa", updatedAt = 5000)))
        val remote = CloudSnapshot(projects = listOf(CloudProject(1, "Stara nazwa", updatedAt = 1000)))
        assertEquals("Nowa nazwa", SnapshotMerge.merge(local, remote, now).projects.single().name)
    }

    @Test fun `remis bez znacznikow - jak dawniej wygrywa chmura`() {
        val local = CloudSnapshot(labels = listOf(CloudLabel(1, "lokalna")))
        val remote = CloudSnapshot(labels = listOf(CloudLabel(1, "zdalna")))
        assertEquals("zdalna", SnapshotMerge.merge(local, remote, now).labels.single().name)
    }

    @Test fun `usuniety projekt nie wraca z drugiego urzadzenia`() {
        val del = now - 1000
        val local = CloudSnapshot(tombstones = listOf(CloudTombstone(CloudTombstone.PROJECT, 7, del)))
        val remote = CloudSnapshot(
            projects = listOf(CloudProject(7, "Stary", updatedAt = del - 500)),
            sections = listOf(CloudSection(70, projectId = 7, name = "Sekcja"))
        )
        val m = SnapshotMerge.merge(local, remote, now)
        assertTrue(m.projects.isEmpty())
        assertTrue("sekcje usuniętego projektu też znikają", m.sections.isEmpty())
        assertEquals(1, m.tombstones.size)
    }

    @Test fun `edycja po usunieciu przywraca rekord`() {
        val local = CloudSnapshot(tombstones = listOf(CloudTombstone(CloudTombstone.LABEL, 3, now - 5000)))
        val remote = CloudSnapshot(labels = listOf(CloudLabel(3, "pilne", updatedAt = now - 100)))
        assertEquals(1, SnapshotMerge.merge(local, remote, now).labels.size)
    }

    @Test fun `stare nagrobki wygasaja`() {
        val old = now - SnapshotMerge.TOMBSTONE_TTL_MS - 1
        val m = SnapshotMerge.merge(CloudSnapshot(tombstones = listOf(CloudTombstone("area", 1, old))), CloudSnapshot(), now)
        assertTrue(m.tombstones.isEmpty())
    }

    @Test fun `remoteUrl nie ginie gdy wygrywa wersja bez linku`() {
        val local = CloudSnapshot(tasks = listOf(CloudTask(1, "x", createdAt = 1, updatedAt = 100, remoteUrl = "https://notion.so/p")))
        val remote = CloudSnapshot(tasks = listOf(CloudTask(1, "x (edycja na Macu)", createdAt = 1, updatedAt = 200)))
        val t = SnapshotMerge.merge(local, remote, now).tasks.single()
        assertEquals("x (edycja na Macu)", t.title)
        assertEquals("https://notion.so/p", t.remoteUrl)
    }

    @Test fun `kodek - nowe pola round-trip i zgodnosc wstecz`() {
        val s = CloudSnapshot(
            tasks = listOf(CloudTask(Ids.next(), "t", remoteUrl = "https://notion.so/abc")),
            projects = listOf(CloudProject(1, "p", updatedAt = 42)),
            tombstones = listOf(CloudTombstone(CloudTombstone.SECTION, 9, 77))
        )
        val back = CloudBackupCodec.fromJson(CloudBackupCodec.toJson(s))
        assertEquals(s.tasks.single().id, back.tasks.single().id)
        assertEquals("https://notion.so/abc", back.tasks.single().remoteUrl)
        assertEquals(42L, back.projects.single().updatedAt)
        assertEquals(s.tombstones, back.tombstones)

        // Stary blob (bez nowych pól) czyta się bez błędów.
        val legacy = """{"version":1,"tasks":[{"id":5,"title":"a","createdAt":1}],"projects":[{"id":1,"name":"p"}],"labels":[]}"""
        val l = CloudBackupCodec.fromJson(legacy)
        assertNull(l.tasks.single().remoteUrl)
        assertEquals(0L, l.projects.single().updatedAt)
        assertTrue(l.tombstones.isEmpty())
    }

    @Test fun `pobranie - brak kopii vs blad sa rozroznione`() {
        assertEquals(SupabaseSync.PullResult.Empty, SupabaseSync.parsePullResponse(200, "[]"))
        assertTrue(SupabaseSync.parsePullResponse(200, """[{"data":{"version":1}}]""") is SupabaseSync.PullResult.Found)
        assertTrue(SupabaseSync.parsePullResponse(500, "oops") is SupabaseSync.PullResult.Failed)
        assertTrue(SupabaseSync.parsePullResponse(401, "{}") is SupabaseSync.PullResult.Failed)
        assertTrue(SupabaseSync.parsePullResponse(200, "<html>") is SupabaseSync.PullResult.Failed)
        assertFalse(SupabaseSync.parsePullResponse(200, "[]") is SupabaseSync.PullResult.Failed)
    }
}
