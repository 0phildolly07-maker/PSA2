package com.phild.servicescanner.data.timetable

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.phild.servicescanner.data.document.DocxTextExtractor
import com.phild.servicescanner.data.document.PdfPageRenderer
import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.TimetableInput
import com.phild.servicescanner.domain.repository.AiException
import java.io.File

class TimetableSourceReader(
    context: Context,
    private val pdfRenderer: PdfPageRenderer = PdfPageRenderer()
) {
    private val appContext = context.applicationContext
    private val tempDir: File = File(appContext.cacheDir, "timetables").apply { mkdirs() }

    fun read(uri: Uri): Result<TimetableInput> {
        return runCatching {
            val mime = appContext.contentResolver.getType(uri).orEmpty()
            val fileName = displayName(uri)
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                throw AiException(AppError.TimetableReadFailed)
            }
            when {
                TimetableMime.isDocx(mime, fileName) -> {
                    val text = DocxTextExtractor.extract(bytes)
                    if (text.isBlank()) {
                        throw AiException(AppError.NoReadableInformation)
                    }
                    TimetableInput(text = text)
                }
                TimetableMime.isPdf(mime, fileName) -> {
                    val pages = runCatching { pdfRenderer.renderPages(bytes, tempDir) }
                        .getOrElse { throw AiException(AppError.TimetableReadFailed) }
                    if (pages.isEmpty()) {
                        throw AiException(AppError.NoReadableInformation)
                    }
                    TimetableInput(pageImages = pages)
                }
                TimetableMime.isImage(mime) -> TimetableInput(pageImages = listOf(bytes))
                else -> throw AiException(AppError.UnsupportedFile)
            }
        }
    }

    private fun displayName(uri: Uri): String {
        return runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else {
                        null
                    }
                }
        }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
    }
}
