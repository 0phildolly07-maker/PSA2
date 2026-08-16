package com.philapp.psa2.utils

import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ServiceType

object FirestoreServiceMapper {

    fun parseTypes(raw: String?): List<ServiceType> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",", ";", "/").mapNotNull { parseTypeToken(it) }.distinct()
    }

    fun parseTypeToken(raw: String): ServiceType? {
        val token = raw.trim()
        if (token.isBlank()) return null

        ServiceType.values().find { it.name.equals(token, ignoreCase = true) }?.let { return it }
        ServiceType.values().find { it.getDisplayName().equals(token, ignoreCase = true) }?.let { return it }

        val compact = token.uppercase()
            .replace("&", "AND")
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')

        ServiceType.values().find { it.name == compact }?.let { return it }

        return when (compact) {
            "HOUSING", "HOMELESSNESS", "HOMELESS" -> ServiceType.HOUSING
            "PRACTICAL_SUPPORT", "PRACTICALSUPPORT" -> ServiceType.PRACTICAL
            "SPORT", "SPORTS", "FITNESS", "SPORT_FITNESS" -> ServiceType.SPORT_AND_FITNESS
            "MENTALHEALTH", "MENTAL" -> ServiceType.MENTAL_HEALTH
            "PEERSUPPORT", "PEER_SUPPORT_GROUP", "PEER_SUPPORT_GROUPS" -> ServiceType.PEER_SUPPORT
            "FOOD", "FOODBANK", "FOOD_BANK" -> ServiceType.FOOD_BANKS
            "COMMUNITY", "COMMUNITY_INTEREST", "COMMUNITY_INTEREST_GROUP" -> ServiceType.COMMUNITY_INTEREST_GROUPS
            "SKILL", "SKILLS", "SKILLBUILDING" -> ServiceType.SKILL_BUILDING
            "EMPLOYMENT_VOLUNTEERING", "VOLUNTEERING" -> ServiceType.EMPLOYMENT
            else -> null
        }
    }

    fun parseStatus(raw: String?): ServiceStatus {
        return when (raw?.trim()?.uppercase()) {
            "APPROVED" -> ServiceStatus.APPROVED
            "REJECTED" -> ServiceStatus.REJECTED
            else -> ServiceStatus.PENDING
        }
    }

    fun parseFeatures(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
}
