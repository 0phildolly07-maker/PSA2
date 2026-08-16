package com.philapp.psa2.utils

import android.location.Location
import com.philapp.psa2.model.Service

object ServiceSort {

    private val days = listOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    )

    fun compareByTownThenDay(a: Service, b: Service): Int {
        val townCompare = a.town.ifBlank { a.location }.compareTo(
            b.town.ifBlank { b.location },
            ignoreCase = true
        )
        if (townCompare != 0) return townCompare
        val dayCompare = daySortKey(a.schedule).compareTo(daySortKey(b.schedule))
        if (dayCompare != 0) return dayCompare
        return a.groupName.compareTo(b.groupName, ignoreCase = true)
    }

    fun compareByDistanceThenTown(
        a: Service,
        b: Service,
        userLocation: Location?
    ): Int {
        if (userLocation != null) {
            val distanceCompare = TownCoordinates.distanceKm(userLocation, a.town)
                .compareTo(TownCoordinates.distanceKm(userLocation, b.town))
            if (distanceCompare != 0) return distanceCompare
        }
        return compareByTownThenDay(a, b)
    }

    fun daySortKey(schedule: String?): Int {
        val text = schedule?.lowercase() ?: return days.size
        days.forEachIndexed { index, day ->
            if (Regex("\\b$day\\b").containsMatchIn(text)) return index
        }
        return days.size
    }
}
