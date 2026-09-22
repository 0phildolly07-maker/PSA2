package com.philapp.psa2.utils

import android.location.Location
import com.phild.servicescanner.domain.model.TownRecord

object TownCoordinates {
    @Volatile
    private var extraCoordinates: Map<String, Pair<Double, Double>> = emptyMap()

    private val coordinates = mapOf(
        "accrington" to Pair(53.7534, -2.3638),
        "bacup" to Pair(53.7034, -2.2010),
        "barrowford" to Pair(53.8472, -2.2184),
        "blackburn" to Pair(53.7480, -2.4820),
        "brierfield" to Pair(53.8248, -2.2346),
        "burnley" to Pair(53.7890, -2.2480),
        "colne" to Pair(53.8570, -2.1750),
        "earby" to Pair(53.9160, -2.1440),
        "haslingden" to Pair(53.7080, -2.3260),
        "nelson" to Pair(53.8350, -2.2140),
        "rawtenstall" to Pair(53.7010, -2.2860),
        "rossendale" to Pair(53.7030, -2.2870),
        "lancashire" to Pair(53.7890, -2.2480),
        "lancashire wide" to Pair(53.7890, -2.2480)
    )

    fun updateExtras(towns: List<TownRecord>) {
        extraCoordinates = towns.mapNotNull { town ->
            val lat = town.latitude
            val lng = town.longitude
            if (lat == null || lng == null) null
            else town.normalizedName to Pair(lat, lng)
        }.toMap()
    }

    fun distanceKm(userLocation: Location, town: String): Float {
        val key = town.trim().lowercase()
        val coords = extraCoordinates[key] ?: coordinates[key] ?: return Float.MAX_VALUE
        val townLocation = Location("town").apply {
            latitude = coords.first
            longitude = coords.second
        }
        return userLocation.distanceTo(townLocation) / 1000f
    }
}
