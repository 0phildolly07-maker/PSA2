package com.phild.servicescanner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceCategoriesTest {

    @Test
    fun parseSelectedKeepsSlashInPeerSupportAndSplitsOnComma() {
        assertEquals(
            listOf(
                ServiceCategories.PEER_SUPPORT,
                ServiceCategories.SOCIAL_ACTIVITY,
                ServiceCategories.SPORT_AND_FITNESS
            ),
            ServiceCategories.parseSelected(
                "Peer Support/Groups, Social Activities, Sport & Fitness"
            )
        )
    }

    @Test
    fun formatSelectedUsesCanonicalOrder() {
        assertEquals(
            "Peer Support/Groups, Social Activities, Sport & Fitness",
            ServiceCategories.formatSelected(
                listOf("Sport & Fitness", "Peer Support/Groups", "Social Activities")
            )
        )
        assertNull(ServiceCategories.formatSelected(emptyList()))
    }

    @Test
    fun canonicalizesLegacyLabels() {
        assertEquals(ServiceCategories.SOCIAL_ACTIVITY, ServiceCategories.canonicalize("Social Activity"))
        assertEquals(ServiceCategories.RECOVERY, ServiceCategories.canonicalize("Substance Misuse Support"))
        assertEquals(ServiceCategories.COMMUNITY_ACTIVITY, ServiceCategories.canonicalize("Community Activity"))
        assertEquals(ServiceCategories.PEER_SUPPORT, ServiceCategories.canonicalize("Peer Support"))
    }

    @Test
    fun keepsDescriptionsForKnownTypes() {
        assertEquals(
            "Connect with others who share similar experiences",
            ServiceCategories.descriptionFor(ServiceCategories.PEER_SUPPORT)
        )
        assertEquals(
            "Join community groups and recreational activities",
            ServiceCategories.descriptionFor(ServiceCategories.SOCIAL_ACTIVITY)
        )
        assertNull(ServiceCategories.descriptionFor(ServiceCategories.OTHER))
        assertTrue(ServiceCategories.all.containsAll(
            listOf(
                "Peer Support/Groups",
                "Social Activities",
                "Mental Health Support",
                "Recovery",
                "Sport & Fitness",
                "Skill Building",
                "Employment & Volunteering",
                "Education & Training",
                "Practical Support",
                "Housing",
                "Food Banks",
                "Community Interest Groups",
                "Other"
            )
        ))
    }
}
