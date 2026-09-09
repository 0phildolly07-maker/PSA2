package com.phild.servicescanner.data.document

import com.phild.servicescanner.domain.model.ExtractedService
import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameGeneratorTest {

    @Test
    fun ordinaryNameUsesUnderscores() {
        assertEquals(
            "Burnley_Walking_Group.docx",
            FilenameGenerator.fileNameFor("Burnley Walking Group")
        )
    }

    @Test
    fun invalidCharactersAreRemoved() {
        assertEquals(
            "BurnleyWalkingGroup.docx",
            FilenameGenerator.fileNameFor("Burnley/Walking:Group*?\"<>|")
        )
    }

    @Test
    fun blankOrNullNameUsesFallback() {
        assertEquals("Service_Information.docx", FilenameGenerator.fileNameFor(null))
        assertEquals("Service_Information.docx", FilenameGenerator.fileNameFor("   "))
        assertEquals("Service_Information.docx", FilenameGenerator.fileNameFor("***"))
    }

    @Test
    fun timetableUsesOrganisationName() {
        val services = listOf(
            ExtractedService(
                organisation = "Red Rose Recovery",
                serviceName = "Get crafty – arts and crafts"
            )
        )
        assertEquals("Red_Rose_Recovery.docx", FilenameGenerator.fileNameFor(services))
    }
}
