package com.phild.servicescanner.ui.screens.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewFieldEditsTest {

    @Test
    fun emptyToNullKeepsSpacesWhileTyping() {
        assertEquals("Community ", "Community ".emptyToNull())
        assertEquals(" ", " ".emptyToNull())
        assertNull("".emptyToNull())
    }

    @Test
    fun parseDaysFieldIgnoresIncompleteTrailingSeparator() {
        assertEquals(listOf("Monday"), parseDaysField("Monday, "))
        assertEquals(listOf("Monday", "Tuesday"), parseDaysField("Monday, Tuesday"))
        assertEquals(emptyList<String>(), parseDaysField("  "))
    }
}
