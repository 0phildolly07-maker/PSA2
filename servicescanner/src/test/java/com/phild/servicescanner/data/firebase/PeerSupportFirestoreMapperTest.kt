package com.phild.servicescanner.data.firebase

import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.ServiceCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerSupportFirestoreMapperTest {

    @Test
    fun mapsPeerSupportFieldsAndOmitsIgnoredFields() {
        val service = ExtractedService(
            organisation = "Red Rose Recovery",
            serviceName = "Here & Now",
            description = "Peer support with lived experience facilitators.",
            category = ServiceCategories.PEER_SUPPORT,
            venue = "St.James Old School Building",
            address = "Accrington",
            postcode = "BB5 1AA",
            areaCovered = "Accrington",
            contactName = "Gemma",
            telephone = "07483915707",
            email = "gemma@example.com",
            website = "https://redroserecovery.org.uk",
            days = listOf("Tuesday"),
            times = "12:30-13:30",
            frequency = "Weekly",
            cost = "£3",
            eligibility = "Open to all",
            referralProcess = "Self referral",
            accessibility = "Step-free",
            additionalNotes = "Bring ID"
        )

        val fields = PeerSupportFirestoreMapper.toFirestoreMap(service)!!

        assertEquals("Red Rose Recovery", fields[PeerSupportFirestoreMapper.FIELD_ORGANISATION])
        assertEquals("Here & Now", fields[PeerSupportFirestoreMapper.FIELD_GROUP_NAME])
        assertEquals(
            "St.James Old School Building, Accrington, BB5 1AA",
            fields[PeerSupportFirestoreMapper.FIELD_LOCATION]
        )
        assertEquals("Accrington", fields[PeerSupportFirestoreMapper.FIELD_TOWN])
        assertEquals(
            "Peer support with lived experience facilitators.",
            fields[PeerSupportFirestoreMapper.FIELD_DESCRIPTION]
        )
        assertEquals("PEER_SUPPORT", fields[PeerSupportFirestoreMapper.FIELD_SERVICE_TYPE])
        assertEquals(listOf("PEER_SUPPORT"), fields[PeerSupportFirestoreMapper.FIELD_TYPES])
        assertEquals("", fields[PeerSupportFirestoreMapper.FIELD_FEATURES])
        assertEquals("Gemma - 07483915707", fields[PeerSupportFirestoreMapper.FIELD_CONTACT])
        assertEquals("Tuesday 12:30-13:30 (Weekly)", fields[PeerSupportFirestoreMapper.FIELD_SESSION_TIMES])
        assertEquals("PENDING", fields[PeerSupportFirestoreMapper.FIELD_STATUS])
        assertEquals("https://redroserecovery.org.uk", fields[PeerSupportFirestoreMapper.FIELD_WEBSITE])

        assertFalse(fields.containsKey("Email"))
        assertFalse(fields.containsKey("email"))
        assertFalse(fields.containsKey("Cost"))
        assertFalse(fields.containsKey("Eligibility"))
        assertFalse(fields.containsKey("Referral Process"))
        assertFalse(fields.containsKey("Accessibility"))
        assertFalse(fields.containsKey("Additional Notes"))
        assertFalse(fields.values.contains("gemma@example.com"))
        assertFalse(fields.values.contains("£3"))
        assertFalse(fields.values.contains("Open to all"))
        assertFalse(fields.values.contains("Self referral"))
        assertFalse(fields.values.contains("Step-free"))
        assertFalse(fields.values.contains("Bring ID"))
    }

    @Test
    fun mapsReviewCategoriesToPeerSupportEnumNames() {
        assertEquals(listOf("SOCIAL"), PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.SOCIAL_ACTIVITY))
        assertEquals(
            listOf("MENTAL_HEALTH"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.MENTAL_HEALTH_SUPPORT)
        )
        assertEquals(
            listOf("RECOVERY"),
            PeerSupportFirestoreMapper.serviceTypes("Substance Misuse Support")
        )
        assertEquals(
            listOf("RECOVERY"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.RECOVERY)
        )
        assertEquals(
            listOf("EMPLOYMENT"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.EMPLOYMENT_VOLUNTEERING)
        )
        assertEquals(
            listOf("EDUCATION"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.EDUCATION_TRAINING)
        )
        assertEquals(
            listOf("PRACTICAL"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.PRACTICAL_SUPPORT)
        )
        assertEquals(
            listOf("COMMUNITY_INTEREST_GROUPS"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.COMMUNITY_ACTIVITY)
        )
        assertTrue(PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.OTHER).isEmpty())
        assertEquals(
            listOf("PEER_SUPPORT"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.PEER_SUPPORT)
        )
        assertEquals(
            listOf("SPORT_AND_FITNESS"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.SPORT_AND_FITNESS)
        )
        assertEquals(
            listOf("SKILL_BUILDING"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.SKILL_BUILDING)
        )
        assertEquals(
            listOf("HOUSING"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.HOUSING)
        )
        assertEquals(
            listOf("FOOD_BANKS"),
            PeerSupportFirestoreMapper.serviceTypes(ServiceCategories.FOOD_BANKS)
        )
    }

    @Test
    fun mapsMultipleSelectedServiceTypes() {
        val service = ExtractedService(
            organisation = "Red Rose Recovery",
            serviceName = "Here & Now",
            category = "Peer Support/Groups, Social Activities, Sport & Fitness"
        )
        val fields = PeerSupportFirestoreMapper.toFirestoreMap(service)!!
        assertEquals(
            "PEER_SUPPORT, SOCIAL, SPORT_AND_FITNESS",
            fields[PeerSupportFirestoreMapper.FIELD_SERVICE_TYPE]
        )
        assertEquals(
            listOf("PEER_SUPPORT", "SOCIAL", "SPORT_AND_FITNESS"),
            fields[PeerSupportFirestoreMapper.FIELD_TYPES]
        )
    }

    @Test
    fun documentIdMatchesPeerSupportRule() {
        assertEquals(
            "red-rose-recovery_here-now_accrington",
            PeerSupportFirestoreMapper.documentId("Red Rose Recovery", "Here & Now", "Accrington")
        )
    }

    @Test
    fun incompleteServicesAreNotMapped() {
        assertNull(
            PeerSupportFirestoreMapper.toFirestoreMap(
                ExtractedService(organisation = "Red Rose Recovery")
            )
        )
        assertNull(
            PeerSupportFirestoreMapper.toFirestoreMap(
                ExtractedService(serviceName = "Here & Now")
            )
        )
    }

    @Test
    fun contactFallsBackToPhoneOrName() {
        assertEquals(
            "07483915707",
            PeerSupportFirestoreMapper.contactInformation(
                ExtractedService(telephone = "07483915707")
            )
        )
        assertEquals(
            "Gemma",
            PeerSupportFirestoreMapper.contactInformation(
                ExtractedService(contactName = "Gemma")
            )
        )
    }
}
