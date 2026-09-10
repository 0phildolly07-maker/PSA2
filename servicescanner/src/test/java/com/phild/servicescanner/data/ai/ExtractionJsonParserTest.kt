package com.phild.servicescanner.data.ai

import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.repository.AiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractionJsonParserTest {

    private val parser = ExtractionJsonParser()

    @Test
    fun validJsonMapsToExtractedService() {
        val result = parser.parse(SampleExtractionJson.DEFAULT)
        assertTrue(result.isSuccess)
        val extraction = result.getOrThrow()
        val service = extraction.service
        assertEquals("Burnley Walking Group", service.serviceName)
        assertEquals("Social Activities", service.category)
        assertEquals("01282 123456", service.telephone)
        assertEquals("Walk Coordinator", service.contactName)
        assertEquals(listOf("Tuesday"), service.days)
        assertEquals("10:00–12:00", service.times)
        assertEquals("Weekly", service.frequency)
        assertEquals("Free", service.cost)
        assertEquals(setOf("telephone"), extraction.uncertainFields)
        assertFalse(extraction.multipleActivitiesDetected)
    }

    @Test
    fun missingAndNullFieldsRemainEmpty() {
        val json = """
            {
              "serviceName": "Test Group",
              "postcode": null,
              "email": "",
              "days": [],
              "eligibility": null
            }
        """.trimIndent()
        val service = parser.parse(json).getOrThrow().service
        assertEquals("Test Group", service.serviceName)
        assertNull(service.postcode)
        assertNull(service.email)
        assertTrue(service.days.isEmpty())
        assertNull(service.eligibility)
        assertNull(service.referralProcess)
        assertNull(service.accessibility)
    }

    @Test
    fun invalidJsonReturnsError() {
        val result = parser.parse("this is not json")
        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as AiException).error
        assertEquals(AppError.JsonParsingFailed, error)
    }

    @Test
    fun timetableJsonMapsAllActivitiesAndContactMatch() {
        val activities = buildString {
            append("[")
            repeat(16) { index ->
                if (index > 0) append(",")
                val name = if (index == 0) "Get crafty – arts and crafts" else "Activity ${index + 1}"
                val contact = if (index == 0) "Bridget" else "Facilitator $index"
                val email = if (index == 0) "Bridget.holden@redroserecovery.org.uk" else null
                val phone = if (index == 0) "07483356858" else null
                append(
                    """
                    {
                      "serviceName": "$name",
                      "organisation": "Red Rose Recovery",
                      "days": ["Monday"],
                      "times": "10:00–11:45",
                      "frequency": "Weekly",
                      "venue": "St James Old School",
                      "address": "Cannon Street, Accrington",
                      "postcode": "BB5 2ER",
                      "areaCovered": "Accrington",
                      "contactName": "$contact",
                      "email": ${email?.let { "\"$it\"" } ?: "null"},
                      "telephone": ${phone?.let { "\"$it\"" } ?: "null"},
                      "uncertainFields": []
                    }
                    """.trimIndent()
                )
            }
            append("]")
        }
        val json = """{ "activities": $activities, "noReadableInformation": false }"""
        val extraction = parser.parse(json).getOrThrow()
        assertEquals(16, extraction.services.size)
        val crafty = extraction.services.first()
        assertEquals("Get crafty – arts and crafts", crafty.serviceName)
        assertEquals("Red Rose Recovery", crafty.organisation)
        assertEquals("Bridget", crafty.contactName)
        assertEquals("Bridget.holden@redroserecovery.org.uk", crafty.email)
        assertEquals("07483356858", crafty.telephone)
        assertEquals(listOf("Monday"), crafty.days)
        assertTrue(extraction.multipleActivitiesDetected)
    }

    @Test
    fun categoryArrayIsJoinedAndCanonicalized() {
        val json = """
            {
              "serviceName": "Walking Group",
              "category": ["Peer Support/Groups", "Social Activities", "Sport & Fitness"]
            }
        """.trimIndent()
        val service = parser.parse(json).getOrThrow().service
        assertEquals(
            "Peer Support/Groups, Social Activities, Sport & Fitness",
            service.category
        )
    }

    @Test
    fun legacyCategoryStringIsCanonicalized() {
        val json = """
            {
              "serviceName": "Walking Group",
              "category": "Social Activity"
            }
        """.trimIndent()
        val service = parser.parse(json).getOrThrow().service
        assertEquals("Social Activities", service.category)
    }

    @Test
    fun sampleTimetableJsonParsesTwoActivities() {
        val extraction = parser.parse(SampleExtractionJson.TIMETABLE).getOrThrow()
        assertEquals(2, extraction.services.size)
        assertEquals("Get crafty – arts and crafts", extraction.service.serviceName)
        assertEquals("Community café", extraction.services[1].serviceName)
    }

    @Test
    fun emptyResponseReturnsError() {
        val result = parser.parse("   ")
        assertTrue(result.isFailure)
        val error = (result.exceptionOrNull() as AiException).error
        assertEquals(AppError.InvalidAiResponse, error)
    }
}
