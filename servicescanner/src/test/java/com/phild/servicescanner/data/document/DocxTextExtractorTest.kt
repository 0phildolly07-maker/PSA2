package com.phild.servicescanner.data.document

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxTextExtractorTest {

    @Test
    fun extractsTableDaysAndActivityNames() {
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:tbl>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>MONDAY</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>TUESDAY</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc>
                      <w:p><w:r><w:t>10:00 – 11:45am</w:t></w:r></w:p>
                      <w:p><w:r><w:t>Get crafty – arts and crafts</w:t></w:r></w:p>
                      <w:p><w:r><w:t>St James Old School</w:t></w:r></w:p>
                    </w:tc>
                    <w:tc>
                      <w:p><w:r><w:t>10:00 – 11:30am</w:t></w:r></w:p>
                      <w:p><w:r><w:t>Community café</w:t></w:r></w:p>
                    </w:tc>
                  </w:tr>
                </w:tbl>
              </w:body>
            </w:document>
        """.trimIndent()

        val text = DocxTextExtractor.extract(minimalDocx(documentXml))

        assertTrue(text.contains("MONDAY"))
        assertTrue(text.contains("TUESDAY"))
        assertTrue(text.contains("Get crafty – arts and crafts"))
        assertTrue(text.contains("Community café"))
        assertTrue(text.contains("10:00 – 11:45am"))
    }

    private fun minimalDocx(documentXml: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}
