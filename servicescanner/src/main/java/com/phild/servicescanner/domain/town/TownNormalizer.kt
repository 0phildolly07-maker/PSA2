package com.phild.servicescanner.domain.town

object TownNormalizer {
    private val skippedNames = setOf("anywhere", "nearby", "all")

    data class NormalizedTown(
        val slug: String,
        val displayName: String,
        val normalizedName: String
    )

    fun normalize(raw: String?): NormalizedTown? {
        val collapsed = raw
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()
        if (collapsed.isBlank()) return null

        val normalizedName = collapsed.lowercase()
        if (normalizedName in skippedNames) return null

        val slug = slugify(collapsed)
        if (slug.isBlank()) return null

        return NormalizedTown(
            slug = slug,
            displayName = titleCase(collapsed),
            normalizedName = normalizedName
        )
    }

    fun slugify(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
    }

    fun titleCase(name: String): String {
        return name.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }
    }
}
