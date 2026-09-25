package com.philapp.psa2.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ContactInfo
import android.util.Log
import com.philapp.psa2.utils.FirestoreServiceMapper

object ServiceManager {
    private const val PREFS_NAME = "service_cache"
    private const val PREFS_KEY = "services_list"
    private const val PREFS_VERSION_KEY = "services_cache_version"
    private const val CACHE_VERSION = 4
    private val gson = Gson()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        // Enable Firestore offline persistence once
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Stores data locally for offline use
            .build()
        Firebase.firestore.firestoreSettings = settings
    }

    fun loadServicesLive(context: Context, onResult: (List<Service>) -> Unit) {
        val prefs = getPrefs(context)

        // Show the last Firebase snapshot immediately, if one has been saved.
        val cached = loadFromCache(prefs)
        onResult(cached)

        // Step 2: Listen for Firestore changes (works offline too)
        listenerRegistration?.remove()
        listenerRegistration = Firebase.firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("ServiceManager", "Firestore listener error: ${error?.message}")
                    return@addSnapshotListener
                }

                Log.d("ServiceManager", "Received ${snapshot.documents.size} documents from Firestore")
                
                val firestoreServices = snapshot.documents.mapNotNull { doc ->
                    try {
                        val serviceData = doc.data
                        if (serviceData != null) {
                            Log.d("ServiceManager", "Processing document ${doc.id}")
                            Log.d("ServiceManager", "  Raw fields: ${serviceData.keys}")
                            
                            // Create a case-insensitive map for field lookup, removing spaces and slashes
                            val caseInsensitiveData = serviceData.mapKeys { 
                                it.key.toString().lowercase().replace(Regex("[\\s/]"), "")
                            }
                            
                            Log.d("ServiceManager", "  Normalized fields: ${caseInsensitiveData.keys}")
                            
                            val service = Service(
                                id = doc.id,
                                organizationName = getFieldValue(caseInsensitiveData, "organisationname", "organizationname", "organization_name", "organization") ?: "",
                                groupName = getFieldValue(caseInsensitiveData, "groupprogramname", "groupname", "group_name", "group") ?: "",
                                location = getFieldValue(caseInsensitiveData, "locationdetails", "location", "address", "place") ?: "",
                                town = getFieldValue(caseInsensitiveData, "town", "city", "locality", "area") ?: "",
                                description = getFieldValue(caseInsensitiveData, "groupdescription", "description", "desc", "details") ?: "",
                                types = parseServiceTypes(caseInsensitiveData, serviceData),
                                features = parseFeatures(caseInsensitiveData, serviceData),
                                contact = parseContactInfo(caseInsensitiveData, serviceData),
                                schedule = getFieldValue(caseInsensitiveData, "sessiontimes", "schedule", "sessions", "times", "hours") ?: "",
                                status = parseServiceStatus(caseInsensitiveData),
                                isDuplicate = caseInsensitiveData["isduplicate"] as? Boolean ?: false,
                                websiteUrl = getFieldValue(caseInsensitiveData, "websiteurl", "website", "url", "link")
                            )
                            
                            Log.d("ServiceManager", "  Parsed: ${service.organizationName} - ${service.groupName}, status=${service.status}, types=${service.types}")
                            service
                        } else null
                    } catch (e: Exception) {
                        Log.e("ServiceManager", "Error parsing service document ${doc.id}", e)
                        null
                    }
                }
                
                Log.d("ServiceManager", "Successfully parsed ${firestoreServices.size} services from Firestore")
                saveToCache(prefs, firestoreServices)
                onResult(firestoreServices)
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun saveToCache(prefs: SharedPreferences, services: List<Service>) {
        prefs.edit()
            .putInt(PREFS_VERSION_KEY, CACHE_VERSION)
            .putString(PREFS_KEY, gson.toJson(services))
            .apply()
    }

    private fun loadFromCache(prefs: SharedPreferences): List<Service> {
        if (prefs.getInt(PREFS_VERSION_KEY, 0) != CACHE_VERSION) return emptyList()
        val json = prefs.getString(PREFS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Service>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Last Firestore list persisted under [PREFS_NAME] (JSON), without querying Firestore again. */
    fun loadCachedServices(context: Context): List<Service> {
        return loadFromCache(getPrefs(context))
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Function to check if Firebase has data
    fun checkFirebaseData(onResult: (Boolean, String) -> Unit) {
        Firebase.firestore.collection("services")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val hasData = !snapshot.isEmpty
                val message = if (hasData) {
                    "Firebase has ${snapshot.size()} documents"
                } else {
                    "Firebase is empty"
                }
                onResult(hasData, message)
            }
            .addOnFailureListener { e ->
                onResult(false, "Error checking Firebase: ${e.message}")
            }
    }

    // Helper function to get field value with multiple possible field names (case-insensitive)
    private fun getFieldValue(data: Map<String, Any?>, vararg fieldNames: String): String? {
        for (fieldName in fieldNames) {
            data[fieldName.lowercase()]?.let { value ->
                if (value is String && value.isNotBlank()) {
                    return value
                }
            }
        }
        return null
    }

    private fun parseServiceTypes(data: Map<String, Any?>, originalData: Map<String, Any?>): List<ServiceType> {
        val typesString = data["servicetype"] as? String
        if (!typesString.isNullOrBlank()) {
            return FirestoreServiceMapper.parseTypes(typesString)
        }

        val rawTypes = data["types"]
        val typesList = when (rawTypes) {
            is List<*> -> rawTypes.mapNotNull { it as? String }
            else -> emptyList()
        }
        if (typesList.isNotEmpty()) {
            return typesList.mapNotNull { FirestoreServiceMapper.parseTypeToken(it) }
        }

        return emptyList()
    }

    // Helper function to parse features
    private fun parseFeatures(data: Map<String, Any?>, originalData: Map<String, Any?>): List<String> {
        // First try to get as a list (new format)
        val featuresList = data["features"] as? List<String>
        if (featuresList != null && featuresList.isNotEmpty()) {
            return featuresList
        }
        
        // Try to get as comma-separated string (ServiceRepository format)
        val featuresString = data["features"] as? String
        if (!featuresString.isNullOrBlank()) {
            return FirestoreServiceMapper.parseFeatures(featuresString)
        }
        
        return emptyList()
    }

    // Helper function to parse contact info with case-insensitive field names
    private fun parseContactInfo(data: Map<String, Any?>, originalData: Map<String, Any?>): ContactInfo? {
        // First try to get as a nested map (new format)
        val contactData = data["contact"] as? Map<*, *>
        if (contactData != null) {
            val phone = getFieldValue(contactData.mapKeys { it.key.toString().lowercase().replace(Regex("[\\s/]"), "") }, "phone", "telephone", "tel")
            val email = getFieldValue(contactData.mapKeys { it.key.toString().lowercase().replace(Regex("[\\s/]"), "") }, "email", "mail")
            if (!phone.isNullOrBlank() || !email.isNullOrBlank()) {
                return ContactInfo(
                    phone = phone ?: "",
                    email = email ?: ""
                )
            }
        }
        
        // Try to get as a simple string (ServiceRepository format - "Contact Information")
        val contactString = data["contactinformation"] as? String
        if (!contactString.isNullOrBlank()) {
            return ContactInfo(
                phone = contactString,
                email = ""
            )
        }
        
        return null
    }

    // Helper function to parse service status with case-insensitive handling
    private fun parseServiceStatus(data: Map<String, Any?>): ServiceStatus {
        return FirestoreServiceMapper.parseStatus(getFieldValue(data, "status", "state"))
    }
}
