package com.phild.servicescanner.domain.repository

import com.phild.servicescanner.domain.model.ExtractedService
import java.io.File

interface DocumentGenerator {
    suspend fun generateDocument(service: ExtractedService): File = generateDocument(listOf(service))
    suspend fun generateDocument(services: List<ExtractedService>): File
}
