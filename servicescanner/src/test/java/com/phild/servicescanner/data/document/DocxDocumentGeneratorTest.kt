package com.phild.servicescanner.data.document

import com.phild.servicescanner.domain.model.ExtractedService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory

class DocxDocumentGeneratorTest {

    @Test
    fun generatesWordCompatibleDocx() = runBlocking {
        val outputDir = createTempDirectory("service-scanner-docx").toFile()
        val generator = DocxDocumentGenerator(outputDir)
        val service = ExtractedService(
            serviceName = "Burnley Walking Group",
            description = "Weekly walk",
            postcode = null
        )

        val file = generator.generateDocument(service)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.endsWith(".docx"))
        ZipFile(file).use { zip ->
            val document = zip.getEntry("word/document.xml")
            assertTrue(document != null)
            val xml = zip.getInputStream(document).bufferedReader().readText()
            assertTrue(xml.contains("Burnley Walking Group"))
            assertTrue(xml.contains("Weekly walk"))
            assertTrue(xml.contains("Group/Program Name:"))
            assertTrue(xml.contains("Organization Name:"))
            assertTrue(xml.contains("Service Description:"))
            assertTrue(xml.contains("Location Details:"))
            assertTrue(xml.contains("Town:"))
            assertTrue(xml.contains("Service Type:"))
            assertTrue(xml.contains("Contact Name:"))
            assertTrue(xml.contains("Session Times:"))
            assertTrue(xml.contains("Days Available:"))
            assertTrue(xml.contains("Frequency:"))
            assertTrue(zip.getEntry("[Content_Types].xml") != null)
            assertTrue(zip.getEntry("_rels/.rels") != null)
        }
    }

    @Test
    fun generatesPageBreakBetweenActivities() = runBlocking {
        val outputDir = createTempDirectory("service-scanner-multi-docx").toFile()
        val generator = DocxDocumentGenerator(outputDir)
        val services = listOf(
            ExtractedService(
                organisation = "Red Rose Recovery",
                serviceName = "Get crafty – arts and crafts"
            ),
            ExtractedService(
                organisation = "Red Rose Recovery",
                serviceName = "Community café"
            )
        )

        val file = generator.generateDocument(services)

        assertEquals("Red_Rose_Recovery.docx", file.name)
        ZipFile(file).use { zip ->
            val xml = zip.getInputStream(zip.getEntry("word/document.xml")).bufferedReader().readText()
            assertTrue(xml.contains("Get crafty"))
            assertTrue(xml.contains("Community café"))
            assertTrue(xml.contains("""w:type="page""""))
        }
    }
}
