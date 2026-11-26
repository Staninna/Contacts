package org.fossify.contacts.fork

import org.fossify.contacts.fork.data.ContactSearch
import org.fossify.contacts.fork.data.ContactSearch.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchTest {
    private val people = listOf(
        Candidate(1, "Anna Smith", listOf("Acme"), listOf("+31 6 1234 5678")),
        Candidate(2, "José Álvarez", listOf("jose@example.org"), emptyList()),
        Candidate(3, "Bob Annapolis", emptyList(), listOf("020-555 0199")),
        Candidate(4, "Zoë", listOf("Nickname Zed"), emptyList()),
    )

    private fun keys(q: String) = ContactSearch.search(q, people).map { it.key }

    @Test fun `every word must match, in any order`() {
        assertEquals(listOf(1), keys("smi an"))
        assertEquals(emptyList<Int>(), keys("anna jones"))
    }

    @Test fun `accents are ignored both ways`() {
        assertEquals(listOf(2), keys("jose alv"))
        assertEquals(listOf(4), keys("zoe"))
        assertEquals(listOf(2), keys("Álvarez"))
    }

    @Test fun `a name that starts with the query ranks first`() {
        assertEquals(listOf(1, 3), keys("ann"))
    }

    @Test fun `numbers match whatever their formatting`() {
        val match = ContactSearch.search("0612345", people).single()
        assertEquals(1, match.key)
        assertEquals("+31 6 1234 5678", match.matchedPhone)
        assertEquals(listOf(3), keys("5550199"))
    }

    @Test fun `extra fields like company and email match`() {
        assertEquals(listOf(1), keys("acme"))
        assertEquals(listOf(2), keys("example.org"))
        assertEquals(listOf(4), keys("zed"))
    }

    @Test fun `blank query finds nothing`() {
        assertTrue(ContactSearch.search("   ", people).isEmpty())
    }

    @Test fun `sections fold accents and file symbols under hash`() {
        assertEquals("E", ContactSearch.sectionOf("Émile"))
        assertEquals("#", ContactSearch.sectionOf("+31 6"))
        assertEquals("#", ContactSearch.sectionOf(""))
        assertEquals("Z", ContactSearch.sectionOf("zoë"))
    }

    @Test fun `a name match carries no phone`() {
        assertNull(ContactSearch.search("smith", people).single().matchedPhone)
    }
}
