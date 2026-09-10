package com.philapp.psa2

import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.utils.FirestoreServiceMapper
import com.philapp.psa2.utils.ServiceSearch
import com.philapp.psa2.utils.ServiceSort
import com.philapp.psa2.utils.generateServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceSearchTest {

    private val guidedWalk = sampleService(
        organizationName = "Newground Together",
        groupName = "Guided Walk - Ball Grove Park Colne",
        description = "Open one-hour walk suitable for all."
    )

    @Test
    fun walkingQueryMatchesWalkListings() {
        assertTrue(ServiceSearch.matches(guidedWalk, "walking"))
        assertTrue(ServiceSearch.matches(guidedWalk, "walk"))
        assertTrue(ServiceSearch.matches(guidedWalk, "guided walk"))
    }

    @Test
    fun queryCanMatchFeaturesTownAndSchedule() {
        val service = sampleService(
            groupName = "Community Cafe",
            town = "Burnley",
            schedule = "Tuesday 10:00",
            features = listOf("Coffee", "Social")
        )
        assertTrue(ServiceSearch.matches(service, "coffee"))
        assertTrue(ServiceSearch.matches(service, "burnley"))
        assertTrue(ServiceSearch.matches(service, "tuesday"))
    }

    @Test
    fun unrelatedQueryDoesNotMatch() {
        assertFalse(ServiceSearch.matches(guidedWalk, "acupuncture"))
    }

    @Test
    fun suggestedAlternativeForWalking() {
        assertEquals("walk", ServiceSearch.suggestedAlternative("walking"))
    }

    private fun sampleService(
        organizationName: String = "Org",
        groupName: String = "Group",
        description: String = "",
        town: String = "Nelson",
        schedule: String? = "Monday 09:00",
        features: List<String> = emptyList()
    ): Service {
        return Service(
            id = "test",
            organizationName = organizationName,
            groupName = groupName,
            location = "Test location",
            town = town,
            description = description,
            types = listOf(ServiceType.SPORT_AND_FITNESS),
            features = features,
            contact = ContactInfo(phone = "", email = ""),
            schedule = schedule,
            status = ServiceStatus.APPROVED
        )
    }
}

class ServiceIdsTest {
    @Test
    fun idIncludesTownWhenPresent() {
        val id = generateServiceId("The Nattershack", "The NatterShack Scheme", "Barrowford")
        assertTrue(id.contains("nattershack"))
        assertTrue(id.contains("barrowford"))
    }

    @Test
    fun idOmitsBlankTown() {
        val id = generateServiceId("Shelter", "Advice", "")
        assertEquals("shelter_advice", id)
    }
}

class FirestoreServiceMapperTest {
    @Test
    fun parsesDisplayNamesAndHousing() {
        val types = FirestoreServiceMapper.parseTypes(
            "Peer Support/Groups, Practical Support, Housing, Sport & Fitness"
        )
        assertTrue(types.contains(ServiceType.PEER_SUPPORT))
        assertTrue(types.contains(ServiceType.PRACTICAL))
        assertTrue(types.contains(ServiceType.HOUSING))
        assertTrue(types.contains(ServiceType.SPORT_AND_FITNESS))
    }

    @Test
    fun parsesStatusCasings() {
        assertEquals(ServiceStatus.APPROVED, FirestoreServiceMapper.parseStatus("Approved"))
        assertEquals(ServiceStatus.PENDING, FirestoreServiceMapper.parseStatus("pending"))
        assertEquals(ServiceStatus.REJECTED, FirestoreServiceMapper.parseStatus("REJECTED"))
    }

    @Test
    fun parsesFeatures() {
        val features = FirestoreServiceMapper.parseFeatures("Coffee, Social, Peer Support")
        assertEquals(listOf("Coffee", "Social", "Peer Support"), features)
    }
}

class ServiceSortTest {
    @Test
    fun mondaySortsBeforeFriday() {
        assertTrue(ServiceSort.daySortKey("Monday 10:00") < ServiceSort.daySortKey("Friday 10:00"))
    }
}
