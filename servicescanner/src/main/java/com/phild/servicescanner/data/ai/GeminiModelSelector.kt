package com.phild.servicescanner.data.ai

import org.json.JSONObject

data class GeminiModelRef(
    val apiVersion: String,
    val modelId: String
)

object GeminiModelSelector {

    val preferredIds = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash-lite",
        "gemini-3-flash",
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-2.0-flash"
    )

    fun modelIdFromName(name: String): String {
        return name.removePrefix("models/").substringBefore("/").trim()
    }

    fun supportsGenerateContent(model: JSONObject): Boolean {
        val methods = model.optJSONArray("supportedGenerationMethods")
        val actions = model.optJSONArray("supportedActions")
        val values = buildList {
            if (methods != null) {
                for (i in 0 until methods.length()) add(methods.optString(i))
            }
            if (actions != null) {
                for (i in 0 until actions.length()) add(actions.optString(i))
            }
        }
        return values.any { it.equals("generateContent", ignoreCase = true) }
    }

    fun isUsableVisionModel(modelId: String): Boolean {
        val name = modelId.lowercase()
        if (name.isBlank()) return false
        val excluded = listOf(
            "embed", "tts", "imagen", "image-preview", "native-audio",
            "veo", "robotics", "computer-use", "exp-tts"
        )
        return excluded.none { it in name }
    }

    fun rank(modelId: String): Int {
        val name = modelId.lowercase()
        var score = 80
        if ("flash" in name) score -= 25
        if ("lite" in name) score += 2
        when {
            "3.5" in name -> score -= 20
            "3.1" in name -> score -= 16
            Regex("""gemini-3(?!\d)""").containsMatchIn(name) -> score -= 12
            "2.5" in name -> score -= 3
        }
        if ("preview" in name || "-exp" in name) score += 12
        if ("pro" in name && "flash" !in name) score += 8
        return score
    }

    fun parseListedModels(raw: String, apiVersion: String): List<GeminiModelRef> {
        val root = JSONObject(raw)
        val models = root.optJSONArray("models") ?: return emptyList()
        return (0 until models.length()).mapNotNull { index ->
            val model = models.optJSONObject(index) ?: return@mapNotNull null
            if (!supportsGenerateContent(model)) return@mapNotNull null
            val id = modelIdFromName(model.optString("name"))
            if (!isUsableVisionModel(id)) return@mapNotNull null
            GeminiModelRef(apiVersion = apiVersion, modelId = id)
        }.sortedBy { rank(it.modelId) }
    }

    fun candidates(
        listed: List<GeminiModelRef>,
        defaultVersion: String = "v1beta"
    ): List<GeminiModelRef> {
        val preferred = preferredIds.map { GeminiModelRef(defaultVersion, it) }
        val merged = (preferred + listed).distinctBy { "${it.apiVersion}:${it.modelId}" }
        return merged.sortedBy { rank(it.modelId) }
    }
}
