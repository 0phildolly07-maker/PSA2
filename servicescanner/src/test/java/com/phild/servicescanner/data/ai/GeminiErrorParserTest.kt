package com.phild.servicescanner.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiErrorParserTest {

    @Test
    fun extractsStatusAndMessage() {
        val raw = """
            {
              "error": {
                "code": 404,
                "message": "models/gemini-2.5-flash is not found",
                "status": "NOT_FOUND"
              }
            }
        """.trimIndent()
        assertEquals(
            "NOT_FOUND: models/gemini-2.5-flash is not found",
            GeminiErrorParser.messageFromBody(raw)
        )
    }

    @Test
    fun stripsApiKeysFromMessage() {
        val raw = "Invalid key AQ.AbEXAMPLEKEYVALUE1234567890abcdef"
        assertEquals("Invalid key [key]", GeminiErrorParser.sanitize(raw))
    }

    @Test
    fun retriesMissingModel() {
        assertTrue(GeminiErrorParser.shouldRetryWithFallbackModel(404, null))
        assertTrue(
            GeminiErrorParser.shouldRetryWithFallbackModel(
                400,
                "model gemini-2.5-flash is not found"
            )
        )
        assertFalse(GeminiErrorParser.shouldRetryWithFallbackModel(400, "Invalid JSON payload"))
        assertFalse(GeminiErrorParser.shouldRetryWithFallbackModel(403, null))
    }
}
