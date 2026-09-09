package com.phild.servicescanner.data.document

import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.repository.DocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxDocumentGenerator(
    private val outputDir: File
) : DocumentGenerator {

    override suspend fun generateDocument(services: List<ExtractedService>): File = withContext(Dispatchers.IO) {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            error("Could not create document folder")
        }
        val fileName = FilenameGenerator.fileNameFor(services)
        val output = uniqueFile(outputDir, fileName)
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            write(zip, "[Content_Types].xml", DocxTemplate.CONTENT_TYPES)
            write(zip, "_rels/.rels", DocxTemplate.RELS)
            write(zip, "word/document.xml", DocxTemplate.documentXml(services))
            write(zip, "word/_rels/document.xml.rels", DocxTemplate.DOCUMENT_RELS)
            write(zip, "word/styles.xml", DocxTemplate.STYLES)
            write(zip, "docProps/core.xml", DocxTemplate.CORE)
            write(zip, "docProps/app.xml", DocxTemplate.APP)
        }
        if (!output.exists() || output.length() == 0L) {
            error("Document was not created")
        }
        output
    }

    private fun write(zip: ZipOutputStream, path: String, xml: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(xml.trimIndent().trim().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val stem = fileName.removeSuffix(".docx")
        var index = 2
        while (true) {
            val next = File(dir, "${stem}_$index.docx")
            if (!next.exists()) return next
            index++
        }
    }
}
