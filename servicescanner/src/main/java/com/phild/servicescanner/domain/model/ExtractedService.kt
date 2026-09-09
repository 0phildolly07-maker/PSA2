package com.phild.servicescanner.domain.model

data class ExtractedService(
    val serviceName: String? = null,
    val description: String? = null,
    val category: String? = null,
    val organisation: String? = null,
    val venue: String? = null,
    val address: String? = null,
    val postcode: String? = null,
    val telephone: String? = null,
    val contactName: String? = null,
    val email: String? = null,
    val website: String? = null,
    val days: List<String> = emptyList(),
    val times: String? = null,
    val frequency: String? = null,
    val cost: String? = null,
    val eligibility: String? = null,
    val referralProcess: String? = null,
    val accessibility: String? = null,
    val areaCovered: String? = null,
    val additionalNotes: String? = null
) {
    fun displayName(): String = serviceName?.takeIf { it.isNotBlank() } ?: "Untitled service"
}
