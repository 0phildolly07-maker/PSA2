package com.philapp.psa2.model

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
    COMMUNITY_INTEREST_GROUPS;

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
        }
    }
} 