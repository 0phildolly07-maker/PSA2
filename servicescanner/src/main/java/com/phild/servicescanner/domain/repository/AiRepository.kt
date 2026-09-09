package com.phild.servicescanner.domain.repository

import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.ExtractionResult
import com.phild.servicescanner.domain.model.TimetableInput

interface AiRepository {
    suspend fun analyseFlyer(image: ByteArray): Result<ExtractionResult>
    suspend fun analyseTimetable(input: TimetableInput): Result<ExtractionResult>
}

class AiException(val error: AppError) : Exception()
