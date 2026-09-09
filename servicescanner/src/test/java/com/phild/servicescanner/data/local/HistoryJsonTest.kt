package com.phild.servicescanner.data.local

import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryJsonTest {

    @Test
    fun roundTripPreservesNullFields() {
        val original = HistoryEntry(
            id = "abc",
            createdAtEpochMs = 1_700_000_000_000,
            serviceName = "Test",
            service = ExtractedService(
                serviceName = "Test",
                postcode = null,
                email = null,
                days = emptyList()
            )
        )
        val restored = HistoryJson.fromJson(HistoryJson.toJson(original))
        assertEquals(original.id, restored.id)
        assertEquals("Test", restored.service.serviceName)
        assertNull(restored.service.postcode)
        assertNull(restored.service.email)
        assertEquals(emptyList<String>(), restored.service.days)
        assertEquals(1, restored.services.size)
        assertEquals(false, restored.isTimetable)
    }

    @Test
    fun oldSingleServiceJsonStillLoads() {
        val json = """
            {
              "id": "old",
              "createdAtEpochMs": 1,
              "serviceName": "Legacy Group",
              "uncertainFields": [],
              "multipleActivitiesDetected": false,
              "imagePath": null,
              "documentPath": null,
              "service": {
                "serviceName": "Legacy Group",
                "days": ["Friday"]
              }
            }
        """.trimIndent()
        val restored = HistoryJson.fromJson(json)
        assertEquals("Legacy Group", restored.service.serviceName)
        assertEquals(listOf("Friday"), restored.service.days)
        assertEquals(1, restored.services.size)
        assertEquals(false, restored.isTimetable)
    }

    @Test
    fun timetableServicesRoundTrip() {
        val original = HistoryEntry(
            id = "tt",
            createdAtEpochMs = 2,
            serviceName = "Red Rose Recovery",
            service = ExtractedService(serviceName = "Get crafty – arts and crafts"),
            services = listOf(
                ExtractedService(serviceName = "Get crafty – arts and crafts", organisation = "Red Rose Recovery"),
                ExtractedService(serviceName = "Community café", organisation = "Red Rose Recovery")
            ),
            isTimetable = true
        )
        val restored = HistoryJson.fromJson(HistoryJson.toJson(original))
        assertEquals(2, restored.services.size)
        assertEquals(true, restored.isTimetable)
        assertEquals("Community café", restored.services[1].serviceName)
        assertEquals("Get crafty – arts and crafts", restored.service.serviceName)
    }
}
