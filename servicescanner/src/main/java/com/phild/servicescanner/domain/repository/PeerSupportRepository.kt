package com.phild.servicescanner.domain.repository

import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.RemotePublishResult

interface PeerSupportRepository {
    suspend fun publishServices(services: List<ExtractedService>): Result<RemotePublishResult>
}
