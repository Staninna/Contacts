package org.fossify.contacts.fork.data

import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Birthdays and anniversaries as Android stores them: "1990-04-12", or "--04-12" when the
 * year isn't known. Gives a localized date, the days until the next one and the age it
 * marks, so the details screen can say "In 12 days · turns 36".
 */
object ContactDates {
    data class Info(val formatted: String, val daysUntil: Int, val turning: Int?)

    fun parse(
        value: String,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
        monthDayPattern: String = "d MMMM",
    ): Info? {
        val v = value.trim()
        val (year, monthDay) = runCatching {
            if (v.startsWith("--")) {
                null to MonthDay.parse(v)
            } else {
                val date = LocalDate.parse(v.take(10))
                date.year to MonthDay.from(date)
            }
        }.getOrNull() ?: return null

        val formatted = if (year != null) {
            LocalDate.of(year, monthDay.month, monthDay.dayOfMonth)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
        } else {
            // A month and day in the locale's own order, e.g. "12 April" or "April 12".
            LocalDate.of(2000, monthDay.month, monthDay.dayOfMonth).format(DateTimeFormatter.ofPattern(monthDayPattern, locale))
        }

        val next = nextOccurrence(monthDay, today)
        val days = ChronoUnit.DAYS.between(today, next).toInt()
        val turning = year?.let { next.year - it }?.takeIf { it in 1..150 }
        return Info(formatted, days, turning)
    }

    private fun nextOccurrence(monthDay: MonthDay, today: LocalDate): LocalDate {
        // Feb 29 falls on Mar 1 in years without it.
        fun inYear(year: Int) = if (monthDay.isValidYear(year)) monthDay.atYear(year) else LocalDate.of(year, 3, 1)
        val thisYear = inYear(today.year)
        return if (thisYear.isBefore(today)) inYear(today.year + 1) else thisYear
    }
}
