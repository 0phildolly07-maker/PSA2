package com.phild.servicescanner.domain.model

data class TownRecord(
    val id: String,
    val name: String,
    val normalizedName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val source: String = SOURCE_SCANNER
) {
    companion object {
        const val SOURCE_SCANNER = "scanner"
        const val SOURCE_ADMIN = "admin"
        const val SOURCE_SEED = "seed"
    }
}
