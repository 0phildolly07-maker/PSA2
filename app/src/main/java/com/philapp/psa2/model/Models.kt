package com.philapp.psa2.model

data class Service(
    val id: String,
    val organizationName: String,
    val groupName: String,
    val location: String,
    val town: String = "",
    val description: String,
    val types: List<ServiceType>,
    val features: List<String>,
    val contact: ContactInfo? = null,
    val schedule: String? = null,
    val status: ServiceStatus = ServiceStatus.PENDING,
    val isDuplicate: Boolean = false,
    val websiteUrl: String? = null
)

data class ContactInfo(
    val phone: String,
    val email: String
)

enum class ServiceType {
    SOCIAL,
    MENTAL_HEALTH,
    PEER_SUPPORT,
    SPORT_AND_FITNESS,
    RECOVERY,
    SKILL_BUILDING,
    EDUCATION,
    FOOD_BANKS,
    PRACTICAL,
    EMPLOYMENT,
    COMMUNITY_INTEREST_GROUPS,
    HOUSING;

    fun getDisplayName(): String {
        return when (this) {
            SOCIAL -> "Social"
            MENTAL_HEALTH -> "Mental Health"
            PEER_SUPPORT -> "Peer Support"
            SPORT_AND_FITNESS -> "Sport & Fitness"
            RECOVERY -> "Recovery"
            SKILL_BUILDING -> "Skill Building"
            EDUCATION -> "Education"
            FOOD_BANKS -> "Food Banks"
            PRACTICAL -> "Practical Support"
            EMPLOYMENT -> "Employment & Volunteering"
            COMMUNITY_INTEREST_GROUPS -> "Community Interest Groups"
            HOUSING -> "Housing"
        }
    }
}

enum class ServiceStatus {
    PENDING,
    APPROVED,
    REJECTED
}

// Shared area list for consistent town selection across the app
object AreaList {
    val Lancashire_Areas = listOf(
        "Accrington",
        "Bacup",
        "Barrowford",
        "Blackburn",
        "Brierfield",
        "Burnley",
        "Colne",
        "Earby",
        "Haslingden",
        "Lancashire Wide",
        "Nelson",
        "Rawtenstall",
        "Rossendale"
    ).sorted() // Keep the list alphabetically sorted
} 