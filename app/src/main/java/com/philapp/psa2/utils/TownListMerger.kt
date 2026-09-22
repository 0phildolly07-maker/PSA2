package com.philapp.psa2.utils

import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.phild.servicescanner.domain.town.TownNormalizer

object TownListMerger {
    fun visibleTowns(
        seedTowns: List<String>,
        services: List<Service>
    ): List<String> {
        val byNormalized = linkedMapOf<String, String>()
        seedTowns.forEach { name ->
            val normalized = TownNormalizer.normalize(name) ?: return@forEach
            byNormalized.putIfAbsent(normalized.normalizedName, name)
        }
        services
            .filter { it.status == ServiceStatus.APPROVED }
            .forEach { service ->
                val normalized = TownNormalizer.normalize(service.town) ?: return@forEach
                byNormalized.putIfAbsent(normalized.normalizedName, normalized.displayName)
            }
        return byNormalized.values.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
}
