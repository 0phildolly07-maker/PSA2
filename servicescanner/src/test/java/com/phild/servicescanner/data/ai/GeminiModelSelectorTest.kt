package com.phild.servicescanner.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelSelectorTest {

    @Test
    fun prefersCurrentFlashModels() {
        val listed = GeminiModelSelector.parseListedModels(
            """
            {
              "models": [
                {
                  "name": "models/gemini-2.5-flash",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-3.5-flash",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-embedding-001",
                  "supportedGenerationMethods": ["embedContent"]
                }
              ]
            }
            """.trimIndent(),
            "v1beta"
        )
        assertEquals("gemini-3.5-flash", listed.first().modelId)
        assertTrue(listed.none { it.modelId.contains("embed") })
    }

    @Test
    fun candidatesPutPreferredModelsFirstByRank() {
        val candidates = GeminiModelSelector.candidates(emptyList())
        assertEquals("gemini-3.5-flash", candidates.first().modelId)
    }
}
