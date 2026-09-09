package com.phild.servicescanner.data.firebase

import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.ServiceCategories

object PeerSupportFirestoreMapper {

    const val FIELD_ORGANISATION = "Organisation Name"
    const val FIELD_GROUP_NAME = "Group/Program Name"
    const val FIELD_LOCATION = "Location Details"
    const val FIELD_TOWN = "Town"
    const val FIELD_DESCRIPTION = "Group Description"
    const val FIELD_SERVICE_TYPE = "Service Type"
    const val FIELD_TYPES = "types"
    const val FIELD_FEATURES = "Features"
    const val FIELD_CONTACT = "Contact Information"
    const val FIELD_SESSION_TIMES = "Session Times"
    const val FIELD_STATUS = "Status"
    const val FIELD_WEBSITE = "Website URL"

    const val STATUS_PENDING = "PENDING"

    private val typeNames = setOf(
        "SOCIAL",
        "MENTAL_HEALTH",
        "PEER_SUPPORT",
        "SPORT_AND_FITNESS",
        "RECOVERY",
        "SKILL_BUILDING",
        "EDUCATION",
        "FOOD_BANKS",
        "PRACTICAL",
        "EMPLOYMENT",
        "COMMUNITY_INTEREST_GROUPS",
        "HOUSING"
    )

    fun documentId(organisation: String, groupName: String, town: String = ""): String {
        val parts = listOf(organisation, groupName, town)
            .map { it.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim() }
            .filter { it.isNotBlank() }

        return parts.joinToString("_")
            .replace(Regex("\\s+"), "-")
            .lowercase()
            .take(500)
    }

    fun isComplete(service: ExtractedService): Boolean {
        return !service.organisation.isNullOrBlank() && !service.serviceName.isNullOrBlank()
    }

    fun locationDetails(service: ExtractedService): String {
        return listOfNotNull(
            service.venue?.trim()?.takeIf { it.isNotEmpty() },
            service.address?.trim()?.takeIf { it.isNotEmpty() },
            service.postcode?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(", ")
    }

    fun contactInformation(service: ExtractedService): String {
        val name = service.contactName?.trim().orEmpty()
        val phone = service.telephone?.trim().orEmpty()
        return when {
            name.isNotEmpty() && phone.isNotEmpty() -> "$name - $phone"
            phone.isNotEmpty() -> phone
            name.isNotEmpty() -> name
            else -> ""
        }
    }

    fun sessionTimes(service: ExtractedService): String {
        val days = service.days.joinToString(", ") { it.trim() }.trim()
        val times = service.times?.trim().orEmpty()
        val frequency = service.frequency?.trim().orEmpty()
        return when {
            days.isNotEmpty() && times.isNotEmpty() && frequency.isNotEmpty() ->
                "$days $times ($frequency)"
            times.isNotEmpty() && frequency.isNotEmpty() ->
                "$times ($frequency)"
            days.isNotEmpty() && times.isNotEmpty() ->
                "$days $times"
            times.isNotEmpty() -> times
            days.isNotEmpty() && frequency.isNotEmpty() ->
                "$days ($frequency)"
            days.isNotEmpty() -> days
            frequency.isNotEmpty() -> frequency
            else -> ""
        }
    }

    fun serviceTypes(category: String?): List<String> {
        if (category.isNullOrBlank()) return emptyList()
        return category.split(",", ";", "/")
            .mapNotNull { mapTypeToken(it) }
            .distinct()
    }

    fun toFirestoreMap(service: ExtractedService): Map<String, Any>? {
        if (!isComplete(service)) return null
        val types = serviceTypes(service.category)
        return mapOf(
            FIELD_ORGANISATION to service.organisation!!.trim(),
            FIELD_GROUP_NAME to service.serviceName!!.trim(),
            FIELD_LOCATION to locationDetails(service),
            FIELD_TOWN to service.areaCovered?.trim().orEmpty(),
            FIELD_DESCRIPTION to service.description?.trim().orEmpty(),
            FIELD_SERVICE_TYPE to types.joinToString(", "),
            FIELD_TYPES to types,
            FIELD_FEATURES to "",
            FIELD_CONTACT to contactInformation(service),
            FIELD_SESSION_TIMES to sessionTimes(service),
            FIELD_STATUS to STATUS_PENDING,
            FIELD_WEBSITE to service.website?.trim().orEmpty()
        )
    }

    fun duplicateKey(organisation: String, groupName: String, location: String): String {
        return listOf(organisation, groupName, location)
            .joinToString("|") { it.trim().lowercase() }
    }

    private fun mapTypeToken(raw: String): String? {
        val token = raw.trim()
        if (token.isBlank()) return null

        val compact = token.uppercase()
            .replace("&", "AND")
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')

        if (compact in typeNames) return compact
        typeNames.find { it.equals(token, ignoreCase = true) }?.let { return it }

        return when (compact) {
            "PEER", "PEERSUPPORT", "PEER_SUPPORT_GROUP", "PEER_SUPPORT_GROUPS" -> "PEER_SUPPORT"
            "SOCIAL_ACTIVITY", "SOCIAL_ACTIVITIES" -> "SOCIAL"
            "MENTAL", "MENTALHEALTH", "MENTAL_HEALTH_SUPPORT" -> "MENTAL_HEALTH"
            "SUBSTANCE_MISUSE_SUPPORT", "SUBSTANCE_MISUSE", "RECOVERY_BASED" -> "RECOVERY"
            "EMPLOYMENT_VOLUNTEERING", "VOLUNTEERING" -> "EMPLOYMENT"
            "EDUCATION_TRAINING", "EDUCATION_AND_TRAINING", "TRAINING" -> "EDUCATION"
            "PRACTICAL_SUPPORT", "PRACTICALSUPPORT" -> "PRACTICAL"
            "COMMUNITY_ACTIVITY", "COMMUNITY", "COMMUNITY_INTEREST", "COMMUNITY_INTEREST_GROUP" ->
                "COMMUNITY_INTEREST_GROUPS"
            "SPORT", "SPORTS", "FITNESS", "SPORT_FITNESS" -> "SPORT_AND_FITNESS"
            "FOOD", "FOODBANK", "FOOD_BANK" -> "FOOD_BANKS"
            "HOUSING", "HOMELESSNESS", "HOMELESS" -> "HOUSING"
            "SKILL", "SKILLS", "SKILLBUILDING" -> "SKILL_BUILDING"
            "OTHER" -> null
            else -> when (token) {
                ServiceCategories.PEER_SUPPORT -> "PEER_SUPPORT"
                ServiceCategories.SOCIAL_ACTIVITY -> "SOCIAL"
                ServiceCategories.MENTAL_HEALTH_SUPPORT -> "MENTAL_HEALTH"
                ServiceCategories.SUBSTANCE_MISUSE_SUPPORT -> "RECOVERY"
                ServiceCategories.EMPLOYMENT_VOLUNTEERING -> "EMPLOYMENT"
                ServiceCategories.EDUCATION_TRAINING -> "EDUCATION"
                ServiceCategories.PRACTICAL_SUPPORT -> "PRACTICAL"
                ServiceCategories.COMMUNITY_ACTIVITY -> "COMMUNITY_INTEREST_GROUPS"
                else -> null
            }
        }
    }
}
