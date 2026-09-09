package com.phild.servicescanner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.phild.servicescanner.R
import com.phild.servicescanner.domain.model.AppError

@Composable
fun appErrorMessage(error: AppError): String {
    return when (error) {
        AppError.NoInternet -> stringResource(R.string.error_no_internet)
        AppError.ApiUnavailable -> stringResource(R.string.error_api_unavailable)
        AppError.InvalidApiKey -> stringResource(R.string.error_invalid_api_key)
        AppError.RateLimited -> stringResource(R.string.error_rate_limited)
        AppError.Timeout -> stringResource(R.string.error_timeout)
        AppError.ImageUploadFailed -> stringResource(R.string.error_image_upload)
        AppError.UnsupportedImage -> stringResource(R.string.error_unsupported_image)
        AppError.UnsupportedFile -> stringResource(R.string.error_unsupported_file)
        AppError.TimetableReadFailed -> stringResource(R.string.error_timetable_read)
        AppError.PoorQualityImage -> stringResource(R.string.error_poor_quality)
        AppError.NoReadableInformation -> stringResource(R.string.error_no_readable_information)
        AppError.InvalidAiResponse -> stringResource(R.string.error_invalid_ai_response)
        AppError.JsonParsingFailed -> stringResource(R.string.error_json_parsing)
        AppError.DocumentGenerationFailed -> stringResource(R.string.error_document_generation)
        AppError.FirebaseUploadFailed -> stringResource(R.string.error_firebase_upload)
        is AppError.Unknown -> stringResource(R.string.error_unknown)
        is AppError.ProviderFailure -> providerFailureMessage(error)
    }
}

@Composable
private fun providerFailureMessage(error: AppError.ProviderFailure): String {
    val base = when (error.statusCode) {
        400 -> stringResource(R.string.error_invalid_ai_response)
        404 -> stringResource(R.string.error_api_model_unavailable)
        else -> stringResource(R.string.error_api_unavailable)
    }
    val detail = error.providerMessage?.takeIf { it.isNotBlank() }
    return if (detail != null) {
        "$base\n\n$detail"
    } else if (error.statusCode != null) {
        "$base (${error.statusCode})"
    } else {
        base
    }
}
