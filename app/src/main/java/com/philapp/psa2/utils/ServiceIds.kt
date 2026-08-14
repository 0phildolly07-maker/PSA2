package com.philapp.psa2.utils

/**
 * Canonical, stable ID for a Service across the app and Firestore.
 *
 * Rule: ID is derived from (organisationName + groupName), and must match the Firestore document ID.
 */
fun generateServiceId(organizationName: String, groupName: String): String {
    val cleanOrg = organizationName.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()
    val cleanGroup = groupName.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim()

    return "${cleanOrg}_${cleanGroup}"
        .replace(Regex("\\s+"), "-")
        .lowercase()
        .take(500)
}

