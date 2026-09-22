package com.phild.servicescanner.domain.town

import com.phild.servicescanner.domain.model.TownRecord

object TownUpsert {
    const val FIELD_NAME = "name"
    const val FIELD_NORMALIZED_NAME = "normalizedName"
    const val FIELD_LATITUDE = "latitude"
    const val FIELD_LONGITUDE = "longitude"
    const val FIELD_SOURCE = "source"
    const val FIELD_UPDATED_AT = "updatedAt"

    /**
     * Fields to merge into an existing town document.
     * Never overwrites an existing display name or existing coordinates.
     */
    fun mergeFields(incoming: TownRecord, existing: TownRecord?): Map<String, Any> {
        val fields = mutableMapOf<String, Any>(
            FIELD_NORMALIZED_NAME to incoming.normalizedName
        )
        if (existing == null) {
            fields[FIELD_NAME] = incoming.name
            fields[FIELD_SOURCE] = incoming.source
            incoming.latitude?.let { fields[FIELD_LATITUDE] = it }
            incoming.longitude?.let { fields[FIELD_LONGITUDE] = it }
        } else {
            if (existing.latitude == null && incoming.latitude != null) {
                fields[FIELD_LATITUDE] = incoming.latitude
            }
            if (existing.longitude == null && incoming.longitude != null) {
                fields[FIELD_LONGITUDE] = incoming.longitude
            }
        }
        return fields
    }
}
