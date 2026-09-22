package com.phild.servicescanner.domain.town

import com.phild.servicescanner.domain.model.TownRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TownUpsertTest {

    @Test
    fun newTownIncludesNameSourceAndCoordinates() {
        val incoming = TownRecord(
            id = "preston",
            name = "Preston",
            normalizedName = "preston",
            latitude = 53.7632,
            longitude = -2.7031,
            source = TownRecord.SOURCE_SCANNER
        )

        val fields = TownUpsert.mergeFields(incoming, existing = null)

        assertEquals("Preston", fields[TownUpsert.FIELD_NAME])
        assertEquals("preston", fields[TownUpsert.FIELD_NORMALIZED_NAME])
        assertEquals(TownRecord.SOURCE_SCANNER, fields[TownUpsert.FIELD_SOURCE])
        assertEquals(53.7632, fields[TownUpsert.FIELD_LATITUDE])
        assertEquals(-2.7031, fields[TownUpsert.FIELD_LONGITUDE])
    }

    @Test
    fun existingDisplayNameIsNotOverwritten() {
        val existing = TownRecord(
            id = "preston",
            name = "Preston",
            normalizedName = "preston",
            source = TownRecord.SOURCE_SCANNER
        )
        val incoming = TownRecord(
            id = "preston",
            name = "PRESTON",
            normalizedName = "preston",
            source = TownRecord.SOURCE_ADMIN
        )

        val fields = TownUpsert.mergeFields(incoming, existing)

        assertFalse(fields.containsKey(TownUpsert.FIELD_NAME))
        assertFalse(fields.containsKey(TownUpsert.FIELD_SOURCE))
        assertEquals("preston", fields[TownUpsert.FIELD_NORMALIZED_NAME])
    }

    @Test
    fun existingCoordinatesAreNotOverwritten() {
        val existing = TownRecord(
            id = "preston",
            name = "Preston",
            normalizedName = "preston",
            latitude = 53.76,
            longitude = -2.70
        )
        val incoming = TownRecord(
            id = "preston",
            name = "Preston",
            normalizedName = "preston",
            latitude = 1.0,
            longitude = 2.0
        )

        val fields = TownUpsert.mergeFields(incoming, existing)

        assertFalse(fields.containsKey(TownUpsert.FIELD_LATITUDE))
        assertFalse(fields.containsKey(TownUpsert.FIELD_LONGITUDE))
    }

    @Test
    fun missingCoordinatesAreFilledFromIncoming() {
        val existing = TownRecord(
            id = "preston",
            name = "Preston",
            normalizedName = "preston"
        )
        val incoming = TownRecord(
            id = "preston",
            name = "Preston",
            normalizedName = "preston",
            latitude = 53.7632,
            longitude = -2.7031
        )

        val fields = TownUpsert.mergeFields(incoming, existing)

        assertEquals(53.7632, fields[TownUpsert.FIELD_LATITUDE])
        assertEquals(-2.7031, fields[TownUpsert.FIELD_LONGITUDE])
        assertTrue(fields.containsKey(TownUpsert.FIELD_LATITUDE))
    }
}
