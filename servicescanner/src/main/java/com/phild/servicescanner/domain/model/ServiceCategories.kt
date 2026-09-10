package com.phild.servicescanner.domain.model

data class ServiceCategoryOption(
    val label: String,
    val description: String = ""
)

object ServiceCategories {
    const val PEER_SUPPORT = "Peer Support/Groups"
    const val SOCIAL_ACTIVITY = "Social Activities"
    const val MENTAL_HEALTH_SUPPORT = "Mental Health Support"
    const val RECOVERY = "Recovery"
    const val SPORT_AND_FITNESS = "Sport & Fitness"
    const val SKILL_BUILDING = "Skill Building"
    const val EMPLOYMENT_VOLUNTEERING = "Employment & Volunteering"
    const val EDUCATION_TRAINING = "Education & Training"
    const val PRACTICAL_SUPPORT = "Practical Support"
    const val HOUSING = "Housing"
    const val FOOD_BANKS = "Food Banks"
    const val COMMUNITY_ACTIVITY = "Community Interest Groups"
    const val OTHER = "Other"

    val options = listOf(
        ServiceCategoryOption(PEER_SUPPORT, "Connect with others who share similar experiences"),
        ServiceCategoryOption(SOCIAL_ACTIVITY, "Join community groups and recreational activities"),
        ServiceCategoryOption(MENTAL_HEALTH_SUPPORT, "Access counseling and support services"),
        ServiceCategoryOption(RECOVERY, "Find recovery-focused groups and support"),
        ServiceCategoryOption(SPORT_AND_FITNESS, "Join sports activities and fitness programs"),
        ServiceCategoryOption(SKILL_BUILDING, "Learn practical and creative skills"),
        ServiceCategoryOption(EMPLOYMENT_VOLUNTEERING, "Find work opportunities and volunteer positions"),
        ServiceCategoryOption(EDUCATION_TRAINING, "Discover courses and skill development programs"),
        ServiceCategoryOption(PRACTICAL_SUPPORT, "Get help with daily needs and resources"),
        ServiceCategoryOption(HOUSING, "Get help with housing, homelessness, and eviction"),
        ServiceCategoryOption(FOOD_BANKS, "Access emergency food support and essential supplies"),
        ServiceCategoryOption(COMMUNITY_ACTIVITY, "Join hobby and interest-based groups"),
        ServiceCategoryOption(OTHER)
    )

    val all = options.map { it.label }

    val promptList = options.joinToString("\n") { option ->
        if (option.description.isBlank()) {
            "- ${option.label}"
        } else {
            "- ${option.label}: ${option.description}"
        }
    }

    fun descriptionFor(label: String): String? =
        options.find { it.label.equals(label, ignoreCase = true) }?.description?.takeIf { it.isNotBlank() }

    fun parseSelected(category: String?): List<String> {
        if (category.isNullOrBlank()) return emptyList()
        return category.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { canonicalize(it) }
            .distinct()
    }

    fun formatSelected(selected: Collection<String>): String? {
        val canonical = selected.map { canonicalize(it.trim()) }.filter { it.isNotEmpty() }.distinct()
        val ordered = all.filter { it in canonical } + canonical.filter { it !in all }
        return ordered.joinToString(", ").ifEmpty { null }
    }

    fun canonicalize(token: String): String {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return trimmed
        all.find { it.equals(trimmed, ignoreCase = true) }?.let { return it }

        val compact = trimmed.uppercase()
            .replace("&", "AND")
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')

        return when (compact) {
            "PEER_SUPPORT", "PEER", "PEERSUPPORT", "PEER_SUPPORT_GROUP", "PEER_SUPPORT_GROUPS" ->
                PEER_SUPPORT
            "SOCIAL", "SOCIAL_ACTIVITY", "SOCIAL_ACTIVITIES" -> SOCIAL_ACTIVITY
            "MENTAL_HEALTH", "MENTAL", "MENTALHEALTH", "MENTAL_HEALTH_SUPPORT" ->
                MENTAL_HEALTH_SUPPORT
            "RECOVERY", "SUBSTANCE_MISUSE_SUPPORT", "SUBSTANCE_MISUSE", "RECOVERY_BASED" -> RECOVERY
            "SPORT_AND_FITNESS", "SPORT", "SPORTS", "FITNESS", "SPORT_FITNESS" -> SPORT_AND_FITNESS
            "SKILL_BUILDING", "SKILL", "SKILLS", "SKILLBUILDING" -> SKILL_BUILDING
            "EMPLOYMENT", "EMPLOYMENT_VOLUNTEERING", "VOLUNTEERING" -> EMPLOYMENT_VOLUNTEERING
            "EDUCATION", "EDUCATION_TRAINING", "EDUCATION_AND_TRAINING", "TRAINING" ->
                EDUCATION_TRAINING
            "PRACTICAL", "PRACTICAL_SUPPORT", "PRACTICALSUPPORT" -> PRACTICAL_SUPPORT
            "HOUSING", "HOMELESSNESS", "HOMELESS" -> HOUSING
            "FOOD_BANKS", "FOOD", "FOODBANK", "FOOD_BANK" -> FOOD_BANKS
            "COMMUNITY_INTEREST_GROUPS", "COMMUNITY_ACTIVITY", "COMMUNITY",
            "COMMUNITY_INTEREST", "COMMUNITY_INTEREST_GROUP" -> COMMUNITY_ACTIVITY
            "OTHER" -> OTHER
            else -> trimmed
        }
    }
}
