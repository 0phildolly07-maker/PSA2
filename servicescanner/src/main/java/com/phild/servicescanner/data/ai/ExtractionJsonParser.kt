package com.phild.servicescanner.data.ai

import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.ExtractionResult
import com.phild.servicescanner.domain.model.ServiceCategories
import com.phild.servicescanner.domain.repository.AiException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ExtractionJsonParser {

    fun parse(json: String): Result<ExtractionResult> {
        val trimmed = json.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
        if (trimmed.isEmpty()) {
            return Result.failure(AiException(AppError.InvalidAiResponse))
        }
        return try {
            val root = extractObject(trimmed)
            val activityArray = root.optJSONArray("activities")
            val parsedActivities = if (activityArray != null) {
                (0 until activityArray.length()).mapNotNull { index ->
                    activityArray.optJSONObject(index)?.let { parseService(it) }
                }
            } else {
                emptyList()
            }
            val services = if (activityArray != null) {
                parsedActivities.map { it.first }
            } else {
                listOf(parseService(root).first)
            }
            val uncertain = buildSet {
                addAll(root.stringList("uncertainFields"))
                parsedActivities.forEach { addAll(it.second) }
            }
            Result.success(
                ExtractionResult(
                    services = services,
                    uncertainFields = uncertain,
                    multipleActivitiesDetected = root.optBoolean("multipleActivitiesDetected", false) ||
                        services.size > 1
                )
            )
        } catch (_: JSONException) {
            Result.failure(AiException(AppError.JsonParsingFailed))
        } catch (_: Exception) {
            Result.failure(AiException(AppError.InvalidAiResponse))
        }
    }

    private fun parseService(root: JSONObject): Pair<ExtractedService, Set<String>> {
        val service = ExtractedService(
            serviceName = root.blankToNull("serviceName"),
            description = root.blankToNull("description"),
            category = ServiceCategories.formatSelected(root.stringList("category")),
            organisation = root.blankToNull("organisation"),
            venue = root.blankToNull("venue"),
            address = root.blankToNull("address"),
            postcode = root.blankToNull("postcode"),
            telephone = root.blankToNull("telephone"),
            contactName = root.blankToNull("contactName"),
            email = root.blankToNull("email"),
            website = root.blankToNull("website"),
            days = root.stringList("days"),
            times = root.blankToNull("times"),
            frequency = root.blankToNull("frequency"),
            cost = root.blankToNull("cost"),
            eligibility = root.blankToNull("eligibility"),
            referralProcess = root.blankToNull("referralProcess"),
            accessibility = root.blankToNull("accessibility"),
            areaCovered = root.blankToNull("areaCovered"),
            additionalNotes = root.blankToNull("additionalNotes")
        )
        return service to root.stringList("uncertainFields").toSet()
    }

    private fun extractObject(raw: String): JSONObject {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw JSONException("No JSON object")
        }
        return JSONObject(raw.substring(start, end + 1))
    }

    private fun JSONObject.blankToNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key) ?: return null
        val text = value.toString().trim()
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) return null
        return text
    }

    private fun JSONObject.stringList(key: String): List<String> {
        if (!has(key) || isNull(key)) return emptyList()
        return when (val value = opt(key)) {
            is JSONArray -> (0 until value.length()).mapNotNull { index ->
                if (value.isNull(index)) {
                    null
                } else {
                    value.opt(index)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                }
            }
            is String -> value.split(',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }
}
