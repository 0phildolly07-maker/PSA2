package com.philapp.psa2.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ContactInfo
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Log
import java.util.NoSuchElementException

class ServiceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val servicesCollection = db.collection("services")

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
            // Map Service fields to Firestore field names (title case, spaces)
            val serviceMap = mutableMapOf<String, Any>()
            serviceMap["Organisation Name"] = service.organizationName
            serviceMap["Group/Program Name"] = service.groupName
            serviceMap["Location Details"] = service.location
            serviceMap["Group Description"] = service.description
            serviceMap["Service Type"] = service.types.joinToString(", ") { it.name }
            serviceMap["Features"] = service.features.joinToString(", ")
            serviceMap["Contact Information"] = service.contact?.phone ?: ""
            serviceMap["Session Times"] = service.schedule ?: ""
            serviceMap["Status"] = service.status.name
            // Optionally add these if present in your model
            // serviceMap["Town"] = ...
            // serviceMap["Frequency"] = ...
            // serviceMap["Days Available"] = ...

            Log.d("ServiceRepository", "Adding new service: ${service.organizationName} - ${service.groupName}")
            val docRef = servicesCollection.add(serviceMap).await()
            Log.d("ServiceRepository", "Service added successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error adding service", e)
            Result.failure(e)
        }
    }

    suspend fun updateService(service: Service) {
        db.collection("services").document(service.id).set(service).await()
    }

    suspend fun deleteService(serviceId: String) {
        db.collection("services").document(serviceId).delete().await()
    }

    suspend fun getServiceById(serviceId: String): Result<Service> {
        return try {
            val doc = servicesCollection.document(serviceId).get().await()
            val service = doc.toObject(Service::class.java)?.copy(id = doc.id)
            
            if (service != null) {
                Log.d("ServiceRepository", "Retrieved service: ${service.organizationName} - ${service.groupName}")
                Result.success(service)
            } else {
                Log.d("ServiceRepository", "Service not found: $serviceId")
                Result.failure(NoSuchElementException("Service not found"))
            }
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error getting service by ID", e)
            Result.failure(e)
        }
    }

    fun getAllServices(): Flow<List<Service>> = flow {
        try {
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
                            }
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e("ServiceRepository", "Error converting document ${doc.id}", e)
                    null
                }
            }
            emit(services)
        } catch (e: Exception) {
            Log.e("ServiceRepository", "Error in getAllServices", e)
            emit(emptyList())
        }
    }

    fun getApprovedServices(): Flow<List<Service>> = flow {
        try {
            val snapshot = servicesCollection
                .whereEqualTo("Status", "APPROVED")
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
            val snapshot = servicesCollection
                .whereEqualTo("Status", ServiceStatus.PENDING.name)
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
} 