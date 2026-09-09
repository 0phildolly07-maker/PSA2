package com.phild.servicescanner.data.document

import com.phild.servicescanner.domain.model.ExtractedService

object FilenameGenerator {
    private val illegalChars = Regex("""[\\/:*?"<>|]""")
    private val whitespace = Regex("""\s+""")

    fun fileNameFor(services: List<ExtractedService>): String {
        val preferred = services.firstOrNull()?.organisation?.takeIf { it.isNotBlank() }
            ?: services.firstOrNull()?.serviceName
        return fileNameFor(preferred)
    }

    fun fileNameFor(serviceName: String?): String {
        val base = serviceName
            ?.trim()
            ?.replace(illegalChars, "")
            ?.replace(whitespace, "_")
            ?.trim('_')
            ?.take(80)
            .orEmpty()
        val safe = base.ifBlank { "Service_Information" }
        return "$safe.docx"
    }
}
