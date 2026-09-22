package com.phild.servicescanner.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.FieldValue
import com.phild.servicescanner.data.geo.TownGeocoder
import com.phild.servicescanner.domain.model.TownRecord
import com.phild.servicescanner.domain.town.TownNormalizer
import com.phild.servicescanner.domain.town.TownUpsert
import kotlinx.coroutines.tasks.await

class FirestoreTownsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val geocoder: TownGeocoder? = null
) {
    private val townsCollection = firestore.collection(COLLECTION)

    suspend fun getTown(slug: String): TownRecord? {
        return try {
            parse(townsCollection.document(slug).get().await())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getAllTowns(): List<TownRecord> {
        return try {
            townsCollection.get().await().documents.mapNotNull(::parse)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun upsertTown(name: String, source: String = TownRecord.SOURCE_SCANNER): TownRecord? {
        val normalized = TownNormalizer.normalize(name) ?: return null
        val existing = getTown(normalized.slug)
        val coords = resolveCoordinates(normalized.displayName, existing)
        val incoming = TownRecord(
            id = normalized.slug,
            name = normalized.displayName,
            normalizedName = normalized.normalizedName,
            latitude = coords?.first,
            longitude = coords?.second,
            source = source
        )
        val fields = TownUpsert.mergeFields(incoming, existing).toMutableMap()
        fields[TownUpsert.FIELD_UPDATED_AT] = FieldValue.serverTimestamp()
        townsCollection.document(normalized.slug).set(fields, SetOptions.merge()).await()
        return incoming.copy(
            name = existing?.name ?: incoming.name,
            latitude = existing?.latitude ?: incoming.latitude,
            longitude = existing?.longitude ?: incoming.longitude
        )
    }

    suspend fun enqueueUpserts(
        batch: WriteBatch,
        townNames: Collection<String>,
        source: String = TownRecord.SOURCE_SCANNER
    ) {
        val unique = townNames.mapNotNull { TownNormalizer.normalize(it) }.distinctBy { it.slug }
        for (normalized in unique) {
            val existing = getTown(normalized.slug)
            val coords = resolveCoordinates(normalized.displayName, existing)
            val incoming = TownRecord(
                id = normalized.slug,
                name = normalized.displayName,
                normalizedName = normalized.normalizedName,
                latitude = coords?.first,
                longitude = coords?.second,
                source = source
            )
            val fields = TownUpsert.mergeFields(incoming, existing).toMutableMap()
            fields[TownUpsert.FIELD_UPDATED_AT] = FieldValue.serverTimestamp()
            batch.set(townsCollection.document(normalized.slug), fields, SetOptions.merge())
        }
    }

    fun listen(onResult: (List<TownRecord>) -> Unit): ListenerRegistration {
        return townsCollection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                onResult(emptyList())
                return@addSnapshotListener
            }
            onResult(snapshot.documents.mapNotNull(::parse))
        }
    }

    fun parse(doc: DocumentSnapshot): TownRecord? {
        val data = doc.data ?: return null
        val name = data[TownUpsert.FIELD_NAME] as? String ?: return null
        val normalized = TownNormalizer.normalize(name) ?: return null
        return TownRecord(
            id = doc.id,
            name = name,
            normalizedName = data[TownUpsert.FIELD_NORMALIZED_NAME] as? String
                ?: normalized.normalizedName,
            latitude = asDouble(data[TownUpsert.FIELD_LATITUDE]),
            longitude = asDouble(data[TownUpsert.FIELD_LONGITUDE]),
            source = data[TownUpsert.FIELD_SOURCE] as? String ?: TownRecord.SOURCE_SCANNER
        )
    }

    private suspend fun resolveCoordinates(
        displayName: String,
        existing: TownRecord?
    ): Pair<Double, Double>? {
        if (existing?.latitude != null && existing.longitude != null) {
            return Pair(existing.latitude, existing.longitude)
        }
        return geocoder?.geocode(displayName)
    }

    private fun asDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        else -> null
    }

    companion object {
        const val COLLECTION = "towns"
    }
}
