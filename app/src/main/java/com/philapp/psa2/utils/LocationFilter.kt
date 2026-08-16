package com.philapp.psa2.utils

object LocationFilter {
    const val ANYWHERE = "all"
    const val NEARBY = "nearby"

    fun displayLabel(value: String): String = when (value) {
        ANYWHERE -> "Anywhere"
        NEARBY -> "Nearby"
        else -> value
    }

    fun isAnywhere(value: String?): Boolean {
        return value.isNullOrBlank() || value == ANYWHERE
    }

    fun isNearby(value: String?): Boolean {
        return value == NEARBY
    }
}
