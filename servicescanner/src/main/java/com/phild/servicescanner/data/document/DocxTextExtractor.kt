package com.phild.servicescanner.data.document

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

object DocxTextExtractor {

    fun extract(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                    return documentXmlToText(xml)
                }
                entry = zip.nextEntry
            }
        }
        return ""
    }

    fun documentXmlToText(xml: String): String {
        val builder = StringBuilder()
        val token = Regex(
            """</w:tr>|</w:tc>|</w:p>|<w:tab\b[^/]*/>|<w:t(?:\s[^>]*)?>(.*?)</w:t>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        for (match in token.findAll(xml)) {
            val value = match.value
            when {
                value.startsWith("<w:t", ignoreCase = true) -> builder.append(decode(match.groupValues[1]))
                value.equals("<w:tab/>", ignoreCase = true) || value.startsWith("<w:tab ", ignoreCase = true) -> {
                    builder.append('\t')
                }
                value.equals("</w:tc>", ignoreCase = true) -> builder.append('\t')
                value.equals("</w:tr>", ignoreCase = true) -> builder.append('\n')
                value.equals("</w:p>", ignoreCase = true) -> builder.append('\n')
            }
        }
        return builder.toString()
            .replace(Regex("[ \t]+\n"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun decode(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
