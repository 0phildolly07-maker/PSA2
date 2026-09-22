package com.phild.servicescanner.data.geo

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class TownGeocoder(context: Context) {
    private val appContext = context.applicationContext

    suspend fun geocode(townName: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(appContext, Locale.UK)
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName("$townName, United Kingdom", 1)
            val location = results?.firstOrNull() ?: return@withContext null
            Pair(location.latitude, location.longitude)
        } catch (_: Exception) {
            null
        }
    }
}
