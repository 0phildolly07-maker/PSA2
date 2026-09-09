package com.phild.servicescanner.domain.model

object ServiceCategories {
    const val PEER_SUPPORT = "Peer Support"
    const val SOCIAL_ACTIVITY = "Social Activity"
    const val MENTAL_HEALTH_SUPPORT = "Mental Health Support"
    const val SUBSTANCE_MISUSE_SUPPORT = "Substance Misuse Support"
    const val EMPLOYMENT_VOLUNTEERING = "Employment & Volunteering"
    const val EDUCATION_TRAINING = "Education & Training"
    const val PRACTICAL_SUPPORT = "Practical Support"
    const val COMMUNITY_ACTIVITY = "Community Activity"
    const val OTHER = "Other"

    val all = listOf(
        PEER_SUPPORT,
        SOCIAL_ACTIVITY,
        MENTAL_HEALTH_SUPPORT,
        SUBSTANCE_MISUSE_SUPPORT,
        EMPLOYMENT_VOLUNTEERING,
        EDUCATION_TRAINING,
        PRACTICAL_SUPPORT,
        COMMUNITY_ACTIVITY,
        OTHER
    )
}
