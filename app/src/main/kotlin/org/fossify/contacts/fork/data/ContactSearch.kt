package org.fossify.contacts.fork.data

import java.text.Normalizer
import java.util.Locale

/**
 * Search and sectioning for the contact list, kept free of Android types so it can be
 * unit tested. A query matches when every word in it is found somewhere in the contact:
 * "an smi" finds "Anna Smith", accents are ignored ("jose" finds "José"), and a query of
 * digits matches phone numbers whatever their formatting.
 */
object ContactSearch {

    data class Candidate(
        val key: Int,
        val name: String,
        val extra: List<String>,
        val phoneNumbers: List<String>,
    )

    data class Match(val key: Int, val score: Int, val matchedPhone: String?)

    private val combiningMarks = "\\p{M}+".toRegex()
    private val nonDigits = "[^0-9]".toRegex()

    fun normalize(text: String): String =
        combiningMarks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    fun digitsOf(text: String) = nonDigits.replace(text, "")

    /** Candidates that match [query], best first. Earlier matches and name matches rank higher. */
    fun search(query: String, candidates: List<Candidate>): List<Match> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val words = normalize(q).split(' ').filter { it.isNotEmpty() }
        val queryDigits = digitsOf(q)
        val looksLikeNumber = queryDigits.length >= 2 && q.all { it.isDigit() || it in "+-() ./" }

        return candidates.mapNotNull { c ->
            val name = normalize(c.name)
            val nameWords = name.split(' ', '-', '.').filter { it.isNotEmpty() }
            val extras = c.extra.map { normalize(it) }

            var score = 0
            val allWordsMatch = words.all { w ->
                when {
                    nameWords.any { it.startsWith(w) } -> { score += 100; true }
                    name.contains(w) -> { score += 50; true }
                    extras.any { it.contains(w) } -> { score += 20; true }
                    else -> false
                }
            }
            if (name.startsWith(words.firstOrNull() ?: "")) score += 40

            var phone: String? = null
            if (looksLikeNumber) {
                // "0612…" should find "+31 6 12…": also try the query without its national trunk 0.
                val trunkless = queryDigits.trimStart('0')
                phone = c.phoneNumbers.firstOrNull { number ->
                    val digits = digitsOf(number)
                    digits.contains(queryDigits) || (trunkless.length >= 2 && trunkless != queryDigits && digits.contains(trunkless))
                }
            }

            when {
                allWordsMatch -> Match(c.key, score, phone)
                phone != null -> Match(c.key, 10, phone)
                else -> null
            }
        }.sortedByDescending { it.score }
    }

    /** The letter a contact is filed under in the list: A–Z (accents folded) or "#". */
    fun sectionOf(label: String): String {
        val first = label.trim().firstOrNull() ?: return "#"
        if (!first.isLetter()) return "#"
        val folded = normalize(first.toString()).firstOrNull() ?: return "#"
        return folded.uppercaseChar().toString()
    }
}
