package pl.media30.todoisto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Ta sama reguła co parseDatabaseId w Edge Function (supabase/functions/route-task/notion.ts). */
class RoutingTest {
    private val id = "0123456789abcdef0123456789abcdef"

    @Test fun `id bazy z linku Notion`() {
        assertEquals(id, RoutingStore.parseNotionDatabaseId("https://www.notion.so/ws/Kanban-Vocative-$id?v=ffffffffffffffffffffffffffffffff"))
        assertEquals(id, RoutingStore.parseNotionDatabaseId("https://www.notion.so/ws/Bad-$id"))
        assertEquals(id, RoutingStore.parseNotionDatabaseId("01234567-89ab-cdef-0123-456789abcdef"))
        assertEquals(id, RoutingStore.parseNotionDatabaseId(id.uppercase()))
        assertNull(RoutingStore.parseNotionDatabaseId("https://www.notion.so/nic"))
        assertNull(RoutingStore.parseNotionDatabaseId(""))
    }
}
