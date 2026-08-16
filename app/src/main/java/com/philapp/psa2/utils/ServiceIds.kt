package com.philapp.psa2.utils

/**
 * Canonical, stable ID for a Service across the app and Firestore.
 *
 * Rule: ID is derived from organisation + group name + town (when present),
 * and should match the Firestore document ID.
 */
fun generateServiceId(
    organizationName: String,
    groupName: String,
    town: String = ""
): String {
    val parts = listOf(organizationName, groupName, town)
        .map { it.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim() }
        .filter { it.isNotBlank() }

    return parts.joinToString("_")
        .replace(Regex("\\s+"), "-")
        .lowercase()
        .take(500)
}
