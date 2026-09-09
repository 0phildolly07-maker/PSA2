package com.phild.servicescanner.domain.model

sealed interface AppError {
    data object NoInternet : AppError
    data object ApiUnavailable : AppError
    data object InvalidApiKey : AppError
    data object RateLimited : AppError
    data object Timeout : AppError
    data object ImageUploadFailed : AppError
    data object UnsupportedImage : AppError
    data object UnsupportedFile : AppError
    data object TimetableReadFailed : AppError
    data object PoorQualityImage : AppError
    data object NoReadableInformation : AppError
    data object InvalidAiResponse : AppError
    data object JsonParsingFailed : AppError
    data object DocumentGenerationFailed : AppError
    data object FirebaseUploadFailed : AppError
    data class Unknown(val debugMessage: String? = null) : AppError
    data class ProviderFailure(
        val statusCode: Int? = null,
        val providerMessage: String? = null
    ) : AppError
}
