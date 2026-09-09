package com.phild.servicescanner.ui.navigation

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

object AppDestinations {
    const val HOME = "home"
    const val IMAGE_PREVIEW = "image_preview"
    const val IMAGE_PREVIEW_ROUTE = "image_preview/{imageUri}"
    const val ANALYSING = "analysing"
    const val ACTIVITY_LIST = "activity_list"
    const val REVIEW = "review"
    const val DOCUMENT_CREATED = "document_created"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val ARG_IMAGE_URI = "imageUri"

    val imagePreviewArguments = listOf(
        navArgument(ARG_IMAGE_URI) { type = NavType.StringType }
    )

    fun imagePreview(uri: Uri): String {
        return "$IMAGE_PREVIEW/${Uri.encode(uri.toString())}"
    }
}
