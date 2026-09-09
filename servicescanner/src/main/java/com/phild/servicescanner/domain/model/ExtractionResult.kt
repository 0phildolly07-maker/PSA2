package com.phild.servicescanner.domain.model

data class ExtractionResult(
    val services: List<ExtractedService>,
    val uncertainFields: Set<String> = emptySet(),
    val multipleActivitiesDetected: Boolean = false
) {
    val service: ExtractedService
        get() = services.firstOrNull() ?: ExtractedService()
}
