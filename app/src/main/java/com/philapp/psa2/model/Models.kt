package com.philapp.psa2.model

data class Service(
    val id: String,
    val organizationName: String,
    val groupName: String,
    val location: String,
    val description: String,
    val types: List<ServiceType>,
    val features: List<String>,
    val contact: ContactInfo? = null,
    val schedule: String? = null,
    val status: ServiceStatus = ServiceStatus.PENDING,
    val isDuplicate: Boolean = false
)

data class ContactInfo(
    val phone: String,
    val email: String
)

data class ExternalActivity(
    val id: String,
    val name: String,
    val organization: String,
    val location: String,
    val description: String,
    val types: List<ServiceType>,
    val schedule: String?,
    val contact: ContactInfo?,
    val features: List<String>,
    val source: String
)

data class ActivitiesResponse(
    val results: List<ExternalActivity>
)

sealed class SearchState {
    object Initial : SearchState()
    object Loading : SearchState()
    data class Success(val services: List<Service>) : SearchState()
    data class Error(val message: String) : SearchState()
} 