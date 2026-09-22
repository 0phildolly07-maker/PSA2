package com.philapp.psa2

import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.utils.TownListMerger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TownListMergerTest {

    private val seed = listOf("Accrington", "Burnley", "Nelson")

    @Test
    fun seedTownsAreAlwaysPresent() {
        val towns = TownListMerger.visibleTowns(seed, services = emptyList())
        assertEquals(listOf("Accrington", "Burnley", "Nelson"), towns)
    }

    @Test
    fun pendingOnlyTownIsHidden() {
        val towns = TownListMerger.visibleTowns(
            seed,
            listOf(sampleService(town = "Preston", status = ServiceStatus.PENDING))
        )
        assertFalse(towns.contains("Preston"))
        assertTrue(towns.contains("Burnley"))
    }

    @Test
    fun approvedTownIsShown() {
        val towns = TownListMerger.visibleTowns(
            seed,
            listOf(sampleService(town = "Preston", status = ServiceStatus.APPROVED))
        )
        assertTrue(towns.contains("Preston"))
        assertTrue(towns.contains("Accrington"))
    }

    @Test
    fun rejectedTownIsHiddenAndDuplicatesCollapse() {
        val towns = TownListMerger.visibleTowns(
            seed,
            listOf(
                sampleService(id = "1", town = "preston", status = ServiceStatus.APPROVED),
                sampleService(id = "2", town = "PRESTON", status = ServiceStatus.APPROVED),
                sampleService(id = "3", town = "Blackpool", status = ServiceStatus.REJECTED)
            )
        )
        assertEquals(1, towns.count { it.equals("Preston", ignoreCase = true) })
        assertFalse(towns.contains("Blackpool"))
    }

    @Test
    fun listIsAlphabeticalAndKeepsSeedCasing() {
        val towns = TownListMerger.visibleTowns(
            seed,
            listOf(sampleService(town = "blackpool", status = ServiceStatus.APPROVED))
        )
        assertEquals(listOf("Accrington", "Blackpool", "Burnley", "Nelson"), towns)
    }

    private fun sampleService(
        id: String = "test",
        town: String,
        status: ServiceStatus
    ): Service {
        return Service(
            id = id,
            organizationName = "Org",
            groupName = "Group",
            location = "Venue",
            town = town,
            description = "",
            types = listOf(ServiceType.PEER_SUPPORT),
            features = emptyList(),
            contact = ContactInfo(phone = "", email = ""),
            status = status
        )
    }
}
