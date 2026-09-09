package com.phild.servicescanner.data.local

import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.HistoryEntry
import org.json.JSONArray
import org.json.JSONObject

object HistoryJson {

    fun toJson(entry: HistoryEntry): String {
        return JSONObject()
            .put("id", entry.id)
            .put("createdAtEpochMs", entry.createdAtEpochMs)
            .put("serviceName", entry.serviceName)
            .put("uncertainFields", JSONArray(entry.uncertainFields.toList()))
            .put("multipleActivitiesDetected", entry.multipleActivitiesDetected)
            .put("isTimetable", entry.isTimetable)
            .put("imagePath", entry.imagePath)
            .put("documentPath", entry.documentPath)
            .put("service", serviceToJson(entry.service))
            .put("services", JSONArray(entry.services.map { serviceToJson(it) }))
            .toString()
    }

    fun fromJson(json: String): HistoryEntry {
        val root = JSONObject(json)
        val services = servicesFromJson(root)
        return HistoryEntry(
            id = root.getString("id"),
            createdAtEpochMs = root.getLong("createdAtEpochMs"),
            serviceName = root.optStringOrNull("serviceName"),
            service = services.firstOrNull() ?: ExtractedService(),
            services = services,
            uncertainFields = root.optJSONArray("uncertainFields").toStringList().toSet(),
            multipleActivitiesDetected = root.optBoolean("multipleActivitiesDetected", false),
            isTimetable = root.optBoolean("isTimetable", false),
            imagePath = root.optStringOrNull("imagePath"),
            documentPath = root.optStringOrNull("documentPath")
        )
    }

    fun serviceToJson(service: ExtractedService): JSONObject {
        return JSONObject()
            .put("serviceName", service.serviceName)
            .put("description", service.description)
            .put("category", service.category)
            .put("organisation", service.organisation)
            .put("venue", service.venue)
            .put("address", service.address)
            .put("postcode", service.postcode)
            .put("telephone", service.telephone)
            .put("contactName", service.contactName)
            .put("email", service.email)
            .put("website", service.website)
            .put("days", JSONArray(service.days))
            .put("times", service.times)
            .put("frequency", service.frequency)
            .put("cost", service.cost)
            .put("eligibility", service.eligibility)
            .put("referralProcess", service.referralProcess)
            .put("accessibility", service.accessibility)
            .put("areaCovered", service.areaCovered)
            .put("additionalNotes", service.additionalNotes)
    }

    private fun servicesFromJson(root: JSONObject): List<ExtractedService> {
        val array = root.optJSONArray("services")
        if (array != null && array.length() > 0) {
            return (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { serviceFromJson(it) }
            }
        }
        return listOf(serviceFromJson(root.optJSONObject("service") ?: JSONObject()))
    }

    fun serviceFromJson(root: JSONObject): ExtractedService {
        return ExtractedService(
            serviceName = root.optStringOrNull("serviceName"),
            description = root.optStringOrNull("description"),
            category = root.optStringOrNull("category"),
            organisation = root.optStringOrNull("organisation"),
            venue = root.optStringOrNull("venue"),
            address = root.optStringOrNull("address"),
            postcode = root.optStringOrNull("postcode"),
            telephone = root.optStringOrNull("telephone"),
            contactName = root.optStringOrNull("contactName"),
            email = root.optStringOrNull("email"),
            website = root.optStringOrNull("website"),
            days = root.optJSONArray("days").toStringList(),
            times = root.optStringOrNull("times"),
            frequency = root.optStringOrNull("frequency"),
            cost = root.optStringOrNull("cost"),
            eligibility = root.optStringOrNull("eligibility"),
            referralProcess = root.optStringOrNull("referralProcess"),
            accessibility = root.optStringOrNull("accessibility"),
            areaCovered = root.optStringOrNull("areaCovered"),
            additionalNotes = root.optStringOrNull("additionalNotes")
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return opt(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", true) }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            if (isNull(index)) null else opt(index)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
