package com.phild.servicescanner.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.RemotePublishResult
import com.phild.servicescanner.domain.repository.PeerSupportRepository
import kotlinx.coroutines.tasks.await

class FirestorePeerSupportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val townsRepository: FirestoreTownsRepository = FirestoreTownsRepository(firestore)
) : PeerSupportRepository {

    private val servicesCollection = firestore.collection(COLLECTION)

    override suspend fun publishServices(
        services: List<ExtractedService>
    ): Result<RemotePublishResult> {
        return try {
            val snapshot = servicesCollection.get().await()
            val existingIds = snapshot.documents.map { it.id }.toHashSet()
            val existingKeys = snapshot.documents.map { doc ->
                val data = doc.data.orEmpty()
                PeerSupportFirestoreMapper.duplicateKey(
                    organisation = data[PeerSupportFirestoreMapper.FIELD_ORGANISATION] as? String ?: "",
                    groupName = data[PeerSupportFirestoreMapper.FIELD_GROUP_NAME] as? String ?: "",
                    location = data[PeerSupportFirestoreMapper.FIELD_LOCATION] as? String ?: ""
                )
            }.toHashSet()

            var uploaded = 0
            var skippedDuplicates = 0
            var skippedIncomplete = 0
            val batch = firestore.batch()
            val usedIds = existingIds.toMutableSet()
            val usedKeys = existingKeys.toMutableSet()
            val townsToUpsert = linkedSetOf<String>()

            for (service in services) {
                val fields = PeerSupportFirestoreMapper.toFirestoreMap(service)
                if (fields == null) {
                    skippedIncomplete++
                    continue
                }
                val organisation = fields[PeerSupportFirestoreMapper.FIELD_ORGANISATION] as String
                val groupName = fields[PeerSupportFirestoreMapper.FIELD_GROUP_NAME] as String
                val location = fields[PeerSupportFirestoreMapper.FIELD_LOCATION] as String
                val town = fields[PeerSupportFirestoreMapper.FIELD_TOWN] as String
                val id = PeerSupportFirestoreMapper.documentId(organisation, groupName, town)
                val key = PeerSupportFirestoreMapper.duplicateKey(organisation, groupName, location)
                if (id in usedIds || key in usedKeys) {
                    skippedDuplicates++
                    continue
                }
                batch.set(servicesCollection.document(id), fields)
                usedIds.add(id)
                usedKeys.add(key)
                if (town.isNotBlank()) {
                    townsToUpsert.add(town)
                }
                uploaded++
            }

            if (uploaded > 0) {
                townsRepository.enqueueUpserts(batch, townsToUpsert)
                batch.commit().await()
            }

            Result.success(
                RemotePublishResult(
                    uploadedCount = uploaded,
                    skippedDuplicateCount = skippedDuplicates,
                    skippedIncompleteCount = skippedIncomplete
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    companion object {
        const val COLLECTION = "services"
    }
}
