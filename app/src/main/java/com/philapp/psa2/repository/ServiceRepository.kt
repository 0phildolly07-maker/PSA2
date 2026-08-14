package com.philapp.psa2.repository

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ContactInfo
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import android.util.Log
import java.util.NoSuchElementException
import com.philapp.psa2.utils.generateServiceId

class ServiceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val servicesCollection = db.collection("services")

    // Ensures we only auto-approve existing pending services once per app install/update.
    private val approvalMigrationPrefsName = "service_approval_migration"
    private val approvalMigrationDoneKey = "approved_existing_pending_services_once"

    init {
        println("ServiceRepository initialized with collection: ${servicesCollection.path}")
        checkFirestore()
    }

    private fun checkFirestore() {
        servicesCollection
            .get()
            .addOnSuccessListener { snapshot ->
                println("=== FIRESTORE CHECK ===")
                println("Successfully connected to Firestore")
                println("Collection path: ${servicesCollection.path}")
                println("Number of documents: ${snapshot.size()}")
                println("Documents:")
                snapshot.documents.forEach { doc ->
                    println("\nDocument ID: ${doc.id}")
                    println("Fields:")
                    doc.data?.forEach { (key, value) ->
                        println("  $key: $value")
                    }
                }
                println("=== END FIRESTORE CHECK ===")
            }
            .addOnFailureListener { e ->
                println("=== FIRESTORE ERROR ===")
                println("Failed to connect to Firestore: ${e.message}")
                e.printStackTrace()
                println("=== END FIRESTORE ERROR ===")
            }
    }

    suspend fun addService(service: Service): Result<String> {
        return try {
            // Check for duplicates before adding - use direct query instead of Flow
            val snapshot = servicesCollection.get().await()
            val existingServices = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    if (data != null) {
                        Service(
                            id = doc.id,
                            organizationName = data["Organisation Name"] as? String ?: "",
                            groupName = data["Group/Program Name"] as? String ?: "",
                            location = data["Location Details"] as? String ?: "",
                            town = data["Town"] as? String ?: "",
                            description = data["Group Description"] as? String ?: "",
                            types = listOf(), // We don't need types for duplicate checking
                            features = listOf(),
                            contact = null,
                            schedule = null,
                            status = ServiceStatus.PENDING
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e("ServiceRepository", "Error converting document ${doc.id} for duplicate check", e)
                    null
                }
            }
            
            val isDuplicate = existingServices.any { existingService ->
                existingService.id != service.id && // Not the same service
                existingService.groupName.equals(service.groupName, ignoreCase = true) && // Same group name
                existingService.organizationName.equals(service.organizationName, ignoreCase = true) && // Same organization
                existingService.location.equals(service.location, ignoreCase = true) // Same location
            }
            
            if (isDuplicate) {
                Log.w("ServiceRepository", "Duplicate service detected: ${service.organizationName} - ${service.groupName}")
                return Result.failure(IllegalStateException("A service with the same name, organization, and location already exists"))
            }
            
            val serviceMap = mutableMapOf<String, Any>()
            serviceMap["Organisation Name"] = service.organizationName
            serviceMap["Group/Program Name"] = service.groupName
            serviceMap["Location Details"] = service.location
            serviceMap["Town"] = service.town
            serviceMap["Group Description"] = service.description
            serviceMap["Service Type"] = service.types.joinToString(", ") { it.name }
            serviceMap["types"] = service.types.map { it.name }
            serviceMap["Features"] = service.features.joinToString(", ")
            serviceMap["Contact Information"] = service.contact?.phone ?: ""
            serviceMap["Session Times"] = service.schedule ?: ""
            serviceMap["Status"] = service.status.name
            serviceMap["Website URL"] = service.websiteUrl ?: ""
            
            // Canonical ID: derived from organisation + group name (must match Firestore doc ID)
            val descriptiveId = generateServiceId(service.organizationName, service.groupName)
            
            Log.d("ServiceRepository", "Adding new service: ${service.organizationName} - ${service.groupName}")
            Log.d("ServiceRepository", "Using descriptive ID: $descriptiveId")
            
            // Use set() with the descriptive ID instead of add()
            servicesCollection.document(descriptiveId).set(serviceMap).await()
            
            Log.d("ServiceRepository", "Service added successfully with ID: $descriptiveId")
            Result.success(descriptiveId)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error adding service", e)
            Result.failure(e)
        }
    }

    suspend fun updateService(service: Service) {
        val serviceMap = mutableMapOf<String, Any>()
        serviceMap["Organisation Name"] = service.organizationName
        serviceMap["Group/Program Name"] = service.groupName
        serviceMap["Location Details"] = service.location
        serviceMap["Town"] = service.town
        serviceMap["Group Description"] = service.description
        serviceMap["Service Type"] = service.types.joinToString(", ") { it.name }
        serviceMap["types"] = service.types.map { it.name }
        serviceMap["Features"] = service.features.joinToString(", ")
        serviceMap["Contact Information"] = service.contact?.phone ?: ""
        serviceMap["Session Times"] = service.schedule ?: ""
        serviceMap["Status"] = service.status.name
        serviceMap["Website URL"] = service.websiteUrl ?: ""
        
        try {
            // Merge preserves other Firestore fields and matches documents created outside this app.
            servicesCollection.document(service.id).set(serviceMap, SetOptions.merge()).await()
            Log.d("ServiceRepository", "Successfully updated service: ${service.organizationName} - ${service.groupName}")
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error updating service", e)
            throw e
        }
    }

    suspend fun deleteService(serviceId: String): Result<Unit> {
        return try {
            Log.d("ServiceRepository", "Deleting service with ID: $serviceId")
            
            servicesCollection.document(serviceId).delete().await()
            
            Log.d("ServiceRepository", "Successfully deleted service: $serviceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error deleting service: $serviceId", e)
            Result.failure(e)
        }
    }

    suspend fun getServiceById(serviceId: String): Result<Service> {
        return try {
            val doc = servicesCollection.document(serviceId).get().await()
            if (!doc.exists()) {
                return Result.failure(NoSuchElementException("Service not found: $serviceId"))
            }
            val service = parseServiceFromFirestore(doc)
            if (service != null) {
                Log.d("ServiceRepository", "Retrieved service: ${service.organizationName} - ${service.groupName}")
                Result.success(service)
            } else {
                Result.failure(NoSuchElementException("Service not found: $serviceId"))
            }
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error getting service by ID", e)
            Result.failure(e)
        }
    }

    /** Firestore uses human-readable field names, not Kotlin [Service] property names. */
    private fun parseServiceFromFirestore(doc: DocumentSnapshot): Service? {
        val data = doc.data ?: return null
        return try {
            Service(
                id = doc.id,
                organizationName = data["Organisation Name"] as? String ?: "",
                groupName = data["Group/Program Name"] as? String ?: "",
                location = data["Location Details"] as? String ?: "",
                town = data["Town"] as? String ?: "",
                description = data["Group Description"] as? String ?: "",
                types = (data["Service Type"] as? String)?.split(",")?.mapNotNull { typeStr ->
                    try {
                        ServiceType.valueOf(typeStr.trim().uppercase().replace(" ", "_"))
                    } catch (_: Exception) {
                        Log.w("ServiceRepository", "Failed to parse service type: $typeStr")
                        null
                    }
                } ?: listOf(),
                features = (data["Features"] as? String)?.split(",")?.map { it.trim() } ?: listOf(),
                contact = ContactInfo(
                    phone = data["Contact Information"] as? String ?: "",
                    email = ""
                ),
                schedule = data["Session Times"] as? String,
                status = when ((data["Status"] as? String)?.uppercase()) {
                    "APPROVED" -> ServiceStatus.APPROVED
                    "REJECTED" -> ServiceStatus.REJECTED
                    else -> ServiceStatus.PENDING
                },
                websiteUrl = data["Website URL"] as? String
            )
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error parsing document ${doc.id}", e)
            null
        }
    }

    fun getAllServices(): Flow<List<Service>> = flow {
        try {
            val snapshot = servicesCollection.get().await()
            val services = snapshot.documents.mapNotNull { doc ->
                parseServiceFromFirestore(doc)
            }
            emit(services)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error in getAllServices", e)
            emit(emptyList())
        }
    }

    fun getApprovedServices(): Flow<List<Service>> = flow {
        try {
            // Handle historical Firestore casing: "APPROVED" (enum name) vs "Approved" / "approved".
            val approvedStatusValues = listOf(
                ServiceStatus.APPROVED.name, // "APPROVED"
                ServiceStatus.APPROVED.name.lowercase(), // "approved"
                ServiceStatus.APPROVED.name.lowercase().replaceFirstChar { it.titlecase() } // "Approved"
            )

            val snapshot = servicesCollection
                .whereIn("Status", approvedStatusValues)
                .get()
                .await()
            val services = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    if (data != null) {
                        Service(
                            id = doc.id,
                            organizationName = data["Organisation Name"] as? String ?: "",
                            groupName = data["Group/Program Name"] as? String ?: "",
                            location = data["Location Details"] as? String ?: "",
                            town = data["Town"] as? String ?: "",
                            description = data["Group Description"] as? String ?: "",
                            types = (data["Service Type"] as? String)?.split(",")?.mapNotNull { typeStr ->
                                try { 
                                    ServiceType.valueOf(typeStr.trim().uppercase().replace(" ", "_")) 
                                } catch (_: Exception) { 
                                    Log.w("ServiceRepository", "Failed to parse service type: $typeStr")
                                    null 
                                }
                            } ?: listOf(),
                            features = (data["Features"] as? String)?.split(",")?.map { it.trim() } ?: listOf(),
                            contact = ContactInfo(
                                phone = data["Contact Information"] as? String ?: "",
                                email = ""
                            ),
                            schedule = data["Session Times"] as? String,
                            status = ServiceStatus.APPROVED
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e("ServiceRepository", "Error converting document ${doc.id}", e)
                    null
                }
            }
            emit(services)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error getting approved services", e)
            emit(emptyList())
        }
    }

    fun getServicesByType(type: ServiceType): Flow<List<Service>> = flow {
        val snapshot = servicesCollection
            .whereArrayContains("types", type)
            .get()
            .await()
        val services = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Service::class.java)
        }
        emit(services)
    }

    fun getServicesByLocation(location: String): Flow<List<Service>> = flow {
        val snapshot = servicesCollection
            .whereGreaterThanOrEqualTo("location", location)
            .whereLessThanOrEqualTo("location", location + '\uf8ff')
            .get()
            .await()
        val services = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Service::class.java)
        }
        emit(services)
    }

    fun getServicesByStatus(status: ServiceStatus): Flow<List<Service>> = flow {
        val snapshot = servicesCollection
            .whereEqualTo("status", status)
            .get()
            .await()
        val services = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Service::class.java)
        }
        emit(services)
    }

    fun getPendingServices(): Flow<List<Service>> = flow {
        try {
            // Handle historical Firestore casing: "PENDING" (enum name) vs "Pending" / "pending".
            val pendingStatusValues = listOf(
                ServiceStatus.PENDING.name, // "PENDING"
                ServiceStatus.PENDING.name.lowercase(), // "pending"
                ServiceStatus.PENDING.name.lowercase().replaceFirstChar { it.titlecase() } // "Pending"
            )

            val snapshot = servicesCollection
                .whereIn("Status", pendingStatusValues)
                .get()
                .await()
            
            val services = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    if (data != null) {
                        Service(
                            id = doc.id,
                            organizationName = data["Organisation Name"] as? String ?: "",
                            groupName = data["Group/Program Name"] as? String ?: "",
                            location = data["Location Details"] as? String ?: "",
                            town = data["Town"] as? String ?: "",
                            description = data["Group Description"] as? String ?: "",
                            types = (data["Service Type"] as? String)?.split(",")?.mapNotNull { typeStr ->
                                try { 
                                    ServiceType.valueOf(typeStr.trim().uppercase().replace(" ", "_")) 
                                } catch (_: Exception) { 
                                    Log.w("ServiceRepository", "Failed to parse service type: $typeStr")
                                    null 
                                }
                            } ?: listOf(),
                            features = (data["Features"] as? String)?.split(",")?.map { it.trim() } ?: listOf(),
                            contact = ContactInfo(
                                phone = data["Contact Information"] as? String ?: "",
                                email = ""
                            ),
                            schedule = data["Session Times"] as? String,
                            status = ServiceStatus.PENDING
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e("ServiceRepository", "Error converting document ${doc.id}", e)
                    null
                }
            }
            
            Log.d("ServiceRepository", "Retrieved ${services.size} pending services")
            emit(services)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error getting pending services", e)
            emit(emptyList())
        }
    }

    suspend fun updateServiceStatus(serviceId: String, newStatus: ServiceStatus): Result<Unit> {
        return try {
            servicesCollection.document(serviceId)
                .update("Status", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error updating service status", e)
            Result.failure(e)
        }
    }

    /**
     * One-time migration: on the app's first start, flip all existing `Pending` services to `Approved`.
     * After this migration runs successfully, newly added services should remain `Pending` and require admin approval.
     */
    suspend fun approveExistingPendingServicesOnFirstRun(context: Context): Result<Int> {
        val prefs = context.getSharedPreferences(approvalMigrationPrefsName, Context.MODE_PRIVATE)
        val alreadyCompleted = prefs.getBoolean(approvalMigrationDoneKey, false)
        if (alreadyCompleted) return Result.success(0)

        return try {
            // Firestore may contain historical values like "Pending" (Title Case) instead of "PENDING" (enum name).
            val pendingStatusValues = listOf(
                ServiceStatus.PENDING.name, // "PENDING"
                ServiceStatus.PENDING.name.lowercase(), // "pending"
                ServiceStatus.PENDING.name.lowercase().replaceFirstChar { it.titlecase() } // "Pending"
            )

            val snapshot = servicesCollection
                .whereIn("Status", pendingStatusValues)
                .get()
                .await()

            val pendingServices = snapshot.documents
            var updatedCount = 0

            // Mark migration as done once we've captured the set of "existing pending" docs.
            // This prevents newly submitted services (added after migration start) from being auto-approved
            // on a later retry.
            prefs.edit().putBoolean(approvalMigrationDoneKey, true).apply()

            if (pendingServices.isNotEmpty()) {
                val batch = db.batch()
                pendingServices.forEach { doc ->
                    batch.update(doc.reference, "Status", ServiceStatus.APPROVED.name)
                    updatedCount++
                }
                batch.commit().await()
            }

            Result.success(updatedCount)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error approving existing pending services", e)
            Result.failure(e)
        }
    }

    suspend fun removeDuplicateServices() {
        val snapshot = servicesCollection.get().await()
        val services = snapshot.documents.mapNotNull { doc ->
            val data = doc.data
            if (data != null) {
                Service(
                    id = doc.id,
                    organizationName = data["Organisation Name"] as? String ?: "",
                    groupName = data["Group/Program Name"] as? String ?: "",
                    location = data["Location Details"] as? String ?: "",
                    town = data["Town"] as? String ?: "",
                    description = data["Group Description"] as? String ?: "",
                    types = listOf(), // Types can be parsed if needed
                    features = listOf(),
                    contact = null,
                    schedule = null,
                    status = ServiceStatus.PENDING
                )
            } else null
        }
        val grouped = services.groupBy { Triple(it.organizationName, it.groupName, it.location) }
        for ((_, group) in grouped) {
            if (group.size > 1) {
                group.drop(1).forEach { duplicate ->
                    servicesCollection.document(duplicate.id).delete().await()
                }
            }
        }
    }

    suspend fun getDuplicateServices(): List<List<Service>> {
        val snapshot = servicesCollection.get().await()
        val services = snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data
                if (data != null) {
                    Service(
                        id = doc.id,
                        organizationName = data["Organisation Name"] as? String ?: "",
                        groupName = data["Group/Program Name"] as? String ?: "",
                        location = data["Location Details"] as? String ?: "",
                        town = data["Town"] as? String ?: "",
                        description = data["Group Description"] as? String ?: "",
                        types = (data["Service Type"] as? String)?.split(",")?.mapNotNull { typeStr ->
                            try { 
                                ServiceType.valueOf(typeStr.trim().uppercase().replace(" ", "_")) 
                            } catch (_: Exception) { 
                                Log.w("ServiceRepository", "Failed to parse service type: $typeStr")
                                null 
                            }
                        } ?: listOf(),
                        features = (data["Features"] as? String)?.split(",")?.map { it.trim() } ?: listOf(),
                        contact = ContactInfo(
                            phone = data["Contact Information"] as? String ?: "",
                            email = ""
                        ),
                        schedule = data["Session Times"] as? String,
                        status = when ((data["Status"] as? String)?.uppercase()) {
                            "APPROVED" -> ServiceStatus.APPROVED
                            "REJECTED" -> ServiceStatus.REJECTED
                            else -> ServiceStatus.PENDING
                        },
                        websiteUrl = data["Website URL"] as? String
                    )
                } else null
            } catch (e: Exception) {
                Log.e("ServiceRepository", "Error converting document ${doc.id}", e)
                null
            }
        }
        
        val grouped = services.groupBy { Triple(it.organizationName, it.groupName, it.location) }
        return grouped.values.filter { it.size > 1 }
    }

    // Enhanced migration function with better debugging
    suspend fun migrateToDescriptiveIds(): Result<Int> {
        return try {
            Log.d("ServiceRepository", "Starting migration to descriptive IDs")
            
            val snapshot = servicesCollection.get().await()
            Log.d("ServiceRepository", "Found ${snapshot.documents.size} total documents in Firebase")
            
            var migratedCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            for (doc in snapshot.documents) {
                val data = doc.data
                Log.d("ServiceRepository", "Processing document: ${doc.id}")
                
                if (data != null) {
                    val organizationName = data["Organisation Name"] as? String ?: ""
                    val groupName = data["Group/Program Name"] as? String ?: ""
                    
                    Log.d("ServiceRepository", "Document ${doc.id}: org='$organizationName', group='$groupName'")
                    
                    // Check if we have the required fields
                    if (organizationName.isBlank() || groupName.isBlank()) {
                        Log.w("ServiceRepository", "Document ${doc.id} missing required fields, skipping")
                        skippedCount++
                        continue
                    }
                    
                    // Generate descriptive ID using just organization and group name
                    val descriptiveId = generateServiceId(organizationName, groupName)
                    Log.d("ServiceRepository", "Generated descriptive ID: $descriptiveId")
                    
                    // Only migrate if the ID is different
                    if (doc.id != descriptiveId) {
                        try {
                            Log.d("ServiceRepository", "Migrating: ${doc.id} -> $descriptiveId")
                            
                            // Check if the target ID already exists
                            val existingDoc = servicesCollection.document(descriptiveId).get().await()
                            if (existingDoc.exists()) {
                                Log.w("ServiceRepository", "Target ID $descriptiveId already exists, skipping migration")
                                skippedCount++
                                continue
                            }
                            
                            // Create new document with descriptive ID
                            servicesCollection.document(descriptiveId).set(data).await()
                            
                            // Delete old document
                            doc.reference.delete().await()
                            
                            migratedCount++
                            Log.d("ServiceRepository", "Successfully migrated: ${doc.id} -> $descriptiveId")
                        } catch (e: Exception) {
                            Log.e("ServiceRepository", "Error migrating document ${doc.id}", e)
                            errorCount++
                        }
                    } else {
                        Log.d("ServiceRepository", "Document ${doc.id} already has descriptive ID, skipping")
                        skippedCount++
                    }
                } else {
                    Log.w("ServiceRepository", "Document ${doc.id} has no data, skipping")
                    skippedCount++
                }
            }
            
            Log.d("ServiceRepository", "Migration completed. Migrated: $migratedCount, Skipped: $skippedCount, Errors: $errorCount")
            Log.d("ServiceRepository", "Total documents processed: ${snapshot.documents.size}")
            
            Result.success(migratedCount)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error during migration", e)
            Result.failure(e)
        }
    }

    // Add a function to check Firebase status and add sample services if needed
    suspend fun checkAndPopulateFirebase(): Result<String> {
        return try {
            Log.d("ServiceRepository", "Checking Firebase status...")
            
            val snapshot = servicesCollection.get().await()
            val documentCount = snapshot.documents.size
            
            Log.d("ServiceRepository", "Found $documentCount documents in Firebase")
            
            if (documentCount == 0) {
                Log.d("ServiceRepository", "Firebase is empty. Adding sample services...")
                
                // Add a few sample services to test migration
                val sampleServices = listOf(
                    mapOf(
                        "Organisation Name" to "Red Rose Recovery",
                        "Group/Program Name" to "Funday Monday",
                        "Location Details" to "St.James Old School Building, Accrington",
                        "Group Description" to "Light-hearted bingo with peers",
                        "Service Type" to "SOCIAL",
                        "Features" to "Bingo, Social, Peer Support",
                        "Contact Information" to "Bridget - 07483356858",
                        "Session Times" to "Monday 13:30-14:30 (Weekly)",
                        "Status" to "PENDING",
                        "Website URL" to "https://redroserecovery.org.uk"
                    ),
                    mapOf(
                        "Organisation Name" to "Inspire",
                        "Group/Program Name" to "Woodnook Breakfast Club",
                        "Location Details" to "Woodnook Community Centre, Royd Street, Accrington, BB5 2JH",
                        "Group Description" to "Breakfast club for community members",
                        "Service Type" to "SOCIAL, MENTAL_HEALTH, PEER_SUPPORT",
                        "Features" to "Breakfast, Social, Support",
                        "Contact Information" to "",
                        "Session Times" to "Tuesday 09:30-10:30",
                        "Status" to "PENDING",
                        "Website URL" to "https://inspire.org.uk"
                    )
                )
                
                sampleServices.forEach { serviceData ->
                    val descriptiveId = generateServiceId(
                        serviceData["Organisation Name"] as String,
                        serviceData["Group/Program Name"] as String
                    )
                    servicesCollection.document(descriptiveId).set(serviceData).await()
                    Log.d("ServiceRepository", "Added sample service with ID: $descriptiveId")
                }
                
                Log.d("ServiceRepository", "Added ${sampleServices.size} sample services to Firebase")
                return Result.success("Firebase was empty. Added ${sampleServices.size} sample services.")
            } else {
                Log.d("ServiceRepository", "Firebase already has $documentCount documents")
                
                // List the first few document IDs for debugging
                val documentIds = snapshot.documents.take(5).map { it.id }
                val documentInfo = if (documentCount <= 5) {
                    "Document IDs: ${documentIds.joinToString(", ")}"
                } else {
                    "First 5 Document IDs: ${documentIds.joinToString(", ")}... and ${documentCount - 5} more"
                }
                
                return Result.success("Firebase has $documentCount existing documents. $documentInfo")
            }
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error checking/populating Firebase", e)
            Result.failure(e)
        }
    }

    // Add a function to replace Firebase with hardcoded services
    suspend fun replaceFirebaseWithHardcodedServices(): Result<String> {
        return try {
            Log.d("ServiceRepository", "Starting to replace Firebase with hardcoded services...")
            
            // First, get all existing documents and delete them
            val snapshot = servicesCollection.get().await()
            val existingCount = snapshot.documents.size
            Log.d("ServiceRepository", "Found $existingCount existing documents to remove")
            
            // Delete all existing documents
            snapshot.documents.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    Log.d("ServiceRepository", "Deleted document: ${doc.id}")
                } catch (e: Exception) {
                    Log.e("ServiceRepository", "Error deleting document ${doc.id}", e)
                }
            }
            
            Log.d("ServiceRepository", "Cleared Firebase. Now adding hardcoded services...")
            
            // Now add all hardcoded services with descriptive IDs
            // This will be handled by the SearchViewModel's sync function
            // which will call addService() for each hardcoded service
            
            Result.success("Cleared $existingCount existing documents. Ready to add hardcoded services.")
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error replacing Firebase", e)
            Result.failure(e)
        }
    }

    // Add a function to clear all documents from Firebase
    suspend fun clearAllServices(): Result<Int> {
        return try {
            Log.d("ServiceRepository", "Clearing all services from Firebase...")
            
            val snapshot = servicesCollection.get().await()
            val documentCount = snapshot.documents.size
            Log.d("ServiceRepository", "Found $documentCount documents to delete")
            
            var deletedCount = 0
            snapshot.documents.forEach { doc ->
                try {
                    doc.reference.delete().await()
                    deletedCount++
                    Log.d("ServiceRepository", "Deleted document: ${doc.id}")
                } catch (e: Exception) {
                    Log.e("ServiceRepository", "Error deleting document ${doc.id}", e)
                }
            }
            
            Log.d("ServiceRepository", "Successfully deleted $deletedCount documents")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error clearing services", e)
            Result.failure(e)
        }
    }
} 