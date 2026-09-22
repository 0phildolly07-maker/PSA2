package com.phild.servicescanner.domain.town

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TownNormalizerTest {

    @Test
    fun blankAndWhitespaceAreSkipped() {
        assertNull(TownNormalizer.normalize(null))
        assertNull(TownNormalizer.normalize(""))
        assertNull(TownNormalizer.normalize("   "))
        assertNull(TownNormalizer.normalize("\t"))
    }

    @Test
    fun specialFilterNamesAreSkipped() {
        assertNull(TownNormalizer.normalize("Anywhere"))
        assertNull(TownNormalizer.normalize("nearby"))
        assertNull(TownNormalizer.normalize("all"))
    }

    @Test
    fun slugAndDisplayNameAreNormalised() {
        val preston = TownNormalizer.normalize("  preston  ")!!
        assertEquals("preston", preston.slug)
        assertEquals("preston", preston.normalizedName)
        assertEquals("Preston", preston.displayName)

        val lancashireWide = TownNormalizer.normalize("Lancashire Wide")!!
        assertEquals("lancashire-wide", lancashireWide.slug)
        assertEquals("lancashire wide", lancashireWide.normalizedName)
        assertEquals("Lancashire Wide", lancashireWide.displayName)
    }

    @Test
    fun prestonAndPrestonCollapseToSameSlug() {
        val titled = TownNormalizer.normalize("Preston")!!
        val lower = TownNormalizer.normalize("preston")!!
        val mixed = TownNormalizer.normalize("PRESTON")!!
        assertEquals(titled.slug, lower.slug)
        assertEquals(titled.slug, mixed.slug)
        assertEquals("preston", titled.slug)
        assertEquals("Preston", titled.displayName)
        assertEquals("Preston", lower.displayName)
        assertEquals("Preston", mixed.displayName)
    }
}
