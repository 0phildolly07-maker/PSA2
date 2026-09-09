package com.phild.servicescanner.domain.repository

import android.net.Uri
import com.phild.servicescanner.domain.model.HistoryEntry
import java.io.File

interface HistoryRepository {
    suspend fun list(): List<HistoryEntry>
    suspend fun get(id: String): HistoryEntry?
    suspend fun save(entry: HistoryEntry): HistoryEntry
    suspend fun update(entry: HistoryEntry)
    suspend fun attachImage(id: String, uri: Uri): String?
    suspend fun attachDocument(id: String, file: File): String?
}
