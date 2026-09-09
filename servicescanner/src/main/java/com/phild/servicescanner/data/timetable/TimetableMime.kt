package com.phild.servicescanner.data.timetable

object TimetableMime {
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val PDF = "application/pdf"

    val openDocumentTypes = arrayOf(
        DOCX,
        PDF,
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    fun isSupported(mime: String, fileName: String = ""): Boolean {
        return isDocx(mime, fileName) || isPdf(mime, fileName) || isImage(mime)
    }

    fun isDocx(mime: String, fileName: String = ""): Boolean {
        return mime.equals(DOCX, ignoreCase = true) || fileName.endsWith(".docx", ignoreCase = true)
    }

    fun isPdf(mime: String, fileName: String = ""): Boolean {
        return mime.equals(PDF, ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)
    }

    fun isImage(mime: String): Boolean {
        return mime.equals("image/jpeg", ignoreCase = true) ||
            mime.equals("image/jpg", ignoreCase = true) ||
            mime.equals("image/png", ignoreCase = true) ||
            mime.equals("image/webp", ignoreCase = true)
    }
}
