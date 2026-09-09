package com.phild.servicescanner.data.ai

import org.json.JSONObject

object GeminiErrorParser {

    fun messageFromBody(raw: String): String? {
        if (raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val error = root.optJSONObject("error") ?: return@runCatching sanitize(raw)
            val status = error.optString("status").trim().ifBlank { null }
            val message = error.optString("message").trim().ifBlank { null }
            val combined = listOfNotNull(status, message).joinToString(": ")
            sanitize(combined).takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun sanitize(text: String): String {
        return text
            .replace(Regex("""AIza[0-9A-Za-z_\-]{8,}"""), "[key]")
            .replace(Regex("""AQ\.[0-9A-Za-z_\-]{8,}"""), "[key]")
            .replace(Regex("""(?i)(key|api[_-]?key)=([^&\s]+)"""), "$1=[key]")
            .trim()
            .take(280)
    }

    fun shouldRetryWithFallbackModel(statusCode: Int, message: String?): Boolean {
        if (statusCode == 404) return true
        if (statusCode >= 500) return true
        val lower = message.orEmpty().lowercase()
        return "not found" in lower ||
            "not supported" in lower ||
            "unknown model" in lower ||
            "is not available" in lower
    }
}
