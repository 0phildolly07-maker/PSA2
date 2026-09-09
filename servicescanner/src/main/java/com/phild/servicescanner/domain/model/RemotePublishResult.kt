package com.phild.servicescanner.domain.model

data class RemotePublishResult(
    val uploadedCount: Int,
    val skippedDuplicateCount: Int = 0,
    val skippedIncompleteCount: Int = 0
)
