package com.phild.servicescanner.data.local

import android.content.Context
import android.net.Uri
import com.phild.servicescanner.domain.model.HistoryEntry
import com.phild.servicescanner.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LocalHistoryRepository(
    context: Context
) : HistoryRepository {

    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "history").apply { mkdirs() }
    private val imagesDir = File(root, "images").apply { mkdirs() }
    private val documentsDir = File(root, "documents").apply { mkdirs() }

    override suspend fun list(): List<HistoryEntry> = withContext(Dispatchers.IO) {
        root.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file -> runCatching { HistoryJson.fromJson(file.readText()) }.getOrNull() }
            ?.sortedByDescending { it.createdAtEpochMs }
            .orEmpty()
    }

    override suspend fun get(id: String): HistoryEntry? = withContext(Dispatchers.IO) {
        val file = File(root, "$id.json")
        if (!file.exists()) return@withContext null
        runCatching { HistoryJson.fromJson(file.readText()) }.getOrNull()
    }

    override suspend fun save(entry: HistoryEntry): HistoryEntry = withContext(Dispatchers.IO) {
        val stored = if (entry.id.isBlank()) entry.copy(id = UUID.randomUUID().toString()) else entry
        File(root, "${stored.id}.json").writeText(HistoryJson.toJson(stored))
        stored
    }

    override suspend fun update(entry: HistoryEntry) {
        save(entry)
    }

    override suspend fun attachImage(id: String, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val dest = File(imagesDir, "$id.jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
                dest.absolutePath
            }.getOrNull()
        }
    }

    override suspend fun attachDocument(id: String, source: File): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val dest = File(documentsDir, "$id.docx")
                source.copyTo(dest, overwrite = true)
                dest.absolutePath
            }.getOrNull()
        }
    }
}
