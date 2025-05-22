package com.philapp.psa2.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.model.ExternalActivity

interface ActivitiesApiService {
    @GET("search")
    suspend fun searchActivities(
        @Query("q") query: String,
        @Query("location") location: String,
        @Query("type") type: String? = null,
        @Query("radius") radius: Int = 5000 // 5km radius
    ): ActivitiesResponse
}

data class ActivitiesResponse(
    val results: List<ExternalActivity>
) 