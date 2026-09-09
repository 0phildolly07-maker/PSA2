package com.phild.servicescanner.ui.screens.home

fun isSupportedImageMime(mime: String): Boolean {
    return mime.equals("image/jpeg", ignoreCase = true) ||
        mime.equals("image/jpg", ignoreCase = true) ||
        mime.equals("image/png", ignoreCase = true) ||
        mime.equals("image/webp", ignoreCase = true)
}
