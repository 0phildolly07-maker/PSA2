package com.phild.servicescanner.data.ai

import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.ExtractionResult
import com.phild.servicescanner.domain.model.TimetableInput
import com.phild.servicescanner.domain.repository.AiException
import com.phild.servicescanner.domain.repository.AiRepository
import kotlinx.coroutines.delay

class MockAiRepository(
    private val parser: ExtractionJsonParser = ExtractionJsonParser(),
    private val sampleJson: String = SampleExtractionJson.DEFAULT,
    private val timetableJson: String = SampleExtractionJson.TIMETABLE,
    private val delayMs: Long = 900
) : AiRepository {

    override suspend fun analyseFlyer(image: ByteArray): Result<ExtractionResult> {
        delay(delayMs)
        if (image.isEmpty()) {
            return Result.failure(AiException(AppError.NoReadableInformation))
        }
        return parser.parse(sampleJson)
    }

    override suspend fun analyseTimetable(input: TimetableInput): Result<ExtractionResult> {
        delay(delayMs)
        if (input.text.isNullOrBlank() && input.pageImages.isEmpty()) {
            return Result.failure(AiException(AppError.NoReadableInformation))
        }
        return parser.parse(timetableJson)
    }
}
