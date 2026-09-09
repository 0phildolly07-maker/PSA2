package com.phild.servicescanner.data.document

import com.phild.servicescanner.domain.model.ExtractedService

object DocxTemplate {

    fun documentXml(service: ExtractedService): String = documentXml(listOf(service))

    fun documentXml(services: List<ExtractedService>): String {
        val forms = services.ifEmpty { listOf(ExtractedService()) }
        val body = forms.mapIndexed { index, service ->
            buildString {
                append(formXml(service))
                if (index < forms.lastIndex) {
                    append(PAGE_BREAK)
                }
            }
        }.joinToString("\n")
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                $body
                <w:sectPr>
                  <w:pgSz w:w="11906" w:h="16838"/>
                  <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
                </w:sectPr>
              </w:body>
            </w:document>
        """.trimIndent()
    }

    private fun formXml(service: ExtractedService): String {
        val days = service.days.joinToString(", ")
        return """
                ${labeled("Organization Name:", service.organisation)}
                ${labeled("Group/Program Name:", service.serviceName)}
                ${labeled("Service Description:", service.description)}
                ${labeled("Location Details:", locationDetails(service))}
                ${labeled("Town:", service.areaCovered)}
                ${labeled("Service Type:", service.category)}
                ${labeled("Contact Name:", service.contactName)}
                ${labeled("Email:", service.email)}
                ${labeled("Website:", service.website)}
                ${labeled("Session Times:", service.times)}
                ${labeled("Days Available:", days.ifBlank { null })}
                ${labeled("Frequency:", service.frequency)}
                ${optionalLabeled("Telephone:", service.telephone)}
                ${optionalLabeled("Cost:", service.cost)}
                ${optionalLabeled("Eligibility:", service.eligibility)}
                ${optionalLabeled("Referral Process:", service.referralProcess)}
                ${optionalLabeled("Accessibility:", service.accessibility)}
                ${optionalLabeled("Additional Notes:", service.additionalNotes)}
        """.trimIndent()
    }

    const val CONTENT_TYPES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
          <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
          <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
          <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
        </Types>
    """

    const val RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
        </Relationships>
    """

    const val DOCUMENT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """

    const val STYLES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:style w:type="paragraph" w:styleId="Normal" w:default="1">
            <w:name w:val="Normal"/>
            <w:rPr>
              <w:sz w:val="22"/>
              <w:szCs w:val="22"/>
            </w:rPr>
          </w:style>
        </w:styles>
    """

    const val CORE = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                           xmlns:dc="http://purl.org/dc/elements/1.1/"
                           xmlns:dcterms="http://purl.org/dc/terms/"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <dc:title>Service Information</dc:title>
          <dc:creator>Service Scanner</dc:creator>
        </cp:coreProperties>
    """

    const val APP = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">
          <Application>Service Scanner</Application>
        </Properties>
    """

    private const val PAGE_BREAK = """
            <w:p>
              <w:r>
                <w:br w:type="page"/>
              </w:r>
            </w:p>
        """

    private fun locationDetails(service: ExtractedService): String? {
        val parts = listOfNotNull(
            service.venue?.takeIf { it.isNotBlank() },
            service.address?.takeIf { it.isNotBlank() },
            service.postcode?.takeIf { it.isNotBlank() }
        )
        return parts.joinToString(", ").ifBlank { null }
    }

    private fun optionalLabeled(label: String, value: String?): String {
        return if (value.isNullOrBlank()) "" else labeled(label, value)
    }

    private fun labeled(label: String, value: String?): String {
        val display = value.orEmpty()
        return """
            <w:p>
              <w:r>
                <w:rPr><w:b/></w:rPr>
                <w:t xml:space="preserve">${escape(label)}</w:t>
              </w:r>
            </w:p>
            <w:p>
              <w:pPr><w:spacing w:after="200"/></w:pPr>
              <w:r>
                <w:t xml:space="preserve">${escapeWithBreaks(display)}</w:t>
              </w:r>
            </w:p>
        """.trimIndent()
    }

    fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeWithBreaks(value: String): String {
        return escape(value)
            .replace("\r\n", "\n")
            .replace("\n", "</w:t><w:br/><w:t xml:space=\"preserve\">")
    }
}
