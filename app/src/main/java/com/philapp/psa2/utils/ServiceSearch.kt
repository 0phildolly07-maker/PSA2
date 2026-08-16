package com.philapp.psa2.utils

import com.philapp.psa2.model.Service

/**
 * Public search matching: every query word must match, with simple stemming
 * (walking → walk) so related words still hit.
 */
object ServiceSearch {

    fun matches(service: Service, query: String): Boolean {
        if (query.isBlank()) return true
        val haystack = searchableText(service)
        return tokenize(query).all { token -> tokenMatches(haystack, token) }
    }

    fun searchableText(service: Service): String {
        return listOf(
            service.organizationName,
            service.groupName,
            service.description,
            service.location,
            service.town,
            service.schedule.orEmpty(),
            service.features.joinToString(" "),
            service.types.joinToString(" ") { it.getDisplayName() },
            service.contact?.phone.orEmpty(),
            service.contact?.email.orEmpty()
        ).joinToString(" ")
    }

    fun tokenize(query: String): List<String> {
        return query.lowercase()
            .split(Regex("[\\s,./+]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun tokenMatches(haystack: String, token: String): Boolean {
        val normalizedHaystack = normalize(haystack)
        return expandToken(token).any { variant ->
            normalizedHaystack.contains(normalize(variant))
        }
    }

    fun expandToken(token: String): Set<String> {
        val t = token.lowercase().trim()
        if (t.isBlank()) return emptySet()
        val variants = mutableSetOf(t)
        synonyms[t]?.let { variants.addAll(it) }
        stem(t)?.let { stem ->
            variants.add(stem)
            synonyms[stem]?.let { variants.addAll(it) }
        }
        return variants
    }

    /**
     * If a query is likely to miss stemmed listings, suggest a shorter term.
     */
    fun suggestedAlternative(query: String): String? {
        val tokens = tokenize(query)
        if (tokens.size != 1) return null
        val token = tokens.first()
        val stem = stem(token)
        return if (stem != null && stem != token) stem else null
    }

    private fun stem(token: String): String? {
        if (token.length <= 4) return null
        return when {
            token.endsWith("ies") && token.length > 5 -> token.dropLast(3) + "y"
            token.endsWith("ing") && token.length > 5 -> token.dropLast(3)
            token.endsWith("ers") && token.length > 5 -> token.dropLast(1)
            token.endsWith("es") && token.length > 4 -> token.dropLast(2)
            token.endsWith("s") && !token.endsWith("ss") -> token.dropLast(1)
            else -> null
        }
    }

    private fun normalize(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]+"), " ")
    }

    private val synonyms = mapOf(
        "walk" to setOf("walk", "walks", "walking", "walker"),
        "walks" to setOf("walk", "walks", "walking"),
        "walking" to setOf("walk", "walks", "walking"),
        "group" to setOf("group", "groups"),
        "groups" to setOf("group", "groups"),
        "natter" to setOf("natter", "nattershack"),
        "housing" to setOf("housing", "homeless", "homelessness", "shelter")
    )
}
