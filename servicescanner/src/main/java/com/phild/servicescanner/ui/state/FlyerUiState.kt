package com.phild.servicescanner.ui.state

import android.net.Uri
import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.ExtractedService

sealed interface FlyerUiState {
    data object Idle : FlyerUiState

    data class ImageSelected(
        val imageUri: Uri,
        val isTimetable: Boolean = false
    ) : FlyerUiState

    data class Analysing(
        val imageUri: Uri,
        val phase: AnalysingPhase = AnalysingPhase.AnalysingFlyer,
        val isTimetable: Boolean = false
    ) : FlyerUiState

    data class ExtractionFailed(
        val imageUri: Uri,
        val error: AppError,
        val isTimetable: Boolean = false
    ) : FlyerUiState

    data class Reviewing(
        val imageUri: Uri,
        val services: List<ExtractedService>,
        val selectedIndex: Int? = null,
        val isTimetable: Boolean = false,
        val uncertainFields: Set<String> = emptySet(),
        val multipleActivitiesDetected: Boolean = false,
        val isGenerating: Boolean = false,
        val generationError: AppError? = null
    ) : FlyerUiState {
        val service: ExtractedService
            get() = services.getOrNull(selectedIndex ?: 0) ?: ExtractedService()
    }

    data object GeneratingDocument : FlyerUiState

    data class DocumentGenerated(
        val filePath: String,
        val fileName: String,
        val publishedCount: Int = 0,
        val skippedDuplicateCount: Int = 0,
        val skippedIncompleteCount: Int = 0,
        val publishError: AppError? = null
    ) : FlyerUiState

    data class Error(
        val error: AppError
    ) : FlyerUiState
}

enum class AnalysingPhase {
    AnalysingFlyer,
    ReadingInformation,
    OrganisingDetails
}
