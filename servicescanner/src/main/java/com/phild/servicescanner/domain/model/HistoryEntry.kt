package com.phild.servicescanner.domain.model

data class HistoryEntry(
    val id: String,
    val createdAtEpochMs: Long,
    val serviceName: String?,
    val service: ExtractedService,
    val services: List<ExtractedService> = listOf(service),
    val uncertainFields: Set<String> = emptySet(),
    val multipleActivitiesDetected: Boolean = false,
    val isTimetable: Boolean = false,
    val imagePath: String? = null,
    val documentPath: String? = null
)
