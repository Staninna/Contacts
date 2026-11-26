package org.fossify.contacts.fork

import org.fossify.contacts.fork.data.ContactDates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class ContactDatesTest {
    private val today = LocalDate.of(2026, 11, 27)

    @Test fun `full date gives days until and the age it marks`() {
        val info = ContactDates.parse("1990-12-09", today, Locale.UK)!!
        assertEquals(12, info.daysUntil)
        assertEquals(36, info.turning)
        assertEquals("9 December 1990", info.formatted)
    }

    @Test fun `today is zero days away`() {
        assertEquals(0, ContactDates.parse("2000-11-27", today, Locale.UK)!!.daysUntil)
    }

    @Test fun `a date already past this year rolls to next year`() {
        val info = ContactDates.parse("1985-03-01", today, Locale.UK)!!
        assertEquals(LocalDate.of(2027, 3, 1).toEpochDay() - today.toEpochDay(), info.daysUntil.toLong())
        assertEquals(42, info.turning)
    }

    @Test fun `no year means no age`() {
        val info = ContactDates.parse("--04-12", today, Locale.UK, monthDayPattern = "d MMMM")!!
        assertNull(info.turning)
        assertEquals("12 April", info.formatted)
    }

    @Test fun `leap day birthdays land on March 1 in other years`() {
        val info = ContactDates.parse("2000-02-29", today, Locale.UK)!!
        assertEquals(LocalDate.of(2027, 3, 1).toEpochDay() - today.toEpochDay(), info.daysUntil.toLong())
    }

    @Test fun `garbage is ignored`() {
        assertNull(ContactDates.parse("someday", today))
    }
}
