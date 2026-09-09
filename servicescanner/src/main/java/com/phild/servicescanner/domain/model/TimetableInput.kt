package com.phild.servicescanner.domain.model

data class TimetableInput(
    val text: String? = null,
    val pageImages: List<ByteArray> = emptyList()
)
