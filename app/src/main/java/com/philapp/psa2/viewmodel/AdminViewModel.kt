package com.philapp.psa2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.repository.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import android.util.Log

class AdminViewModel(
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    private val _pendingServices = MutableStateFlow<List<Service>>(emptyList())
    val pendingServices: StateFlow<List<Service>> = _pendingServices

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _statusUpdateState = MutableStateFlow<Result<Unit>?>(null)
    val statusUpdateState: StateFlow<Result<Unit>?> = _statusUpdateState

    private val _migrationState = MutableStateFlow<Result<Int>?>(null)
    val migrationState: StateFlow<Result<Int>?> = _migrationState

    private val _firebaseCheckState = MutableStateFlow<Result<String>?>(null)
    val firebaseCheckState: StateFlow<Result<String>?> = _firebaseCheckState

    private val _replaceFirebaseState = MutableStateFlow<Result<String>?>(null)
    val replaceFirebaseState: StateFlow<Result<String>?> = _replaceFirebaseState

    // Add Firebase service count state
    private val _firebaseServiceCount = MutableStateFlow<Int?>(null)
    val firebaseServiceCount: StateFlow<Int?> = _firebaseServiceCount

    // Add duplicate analysis state
    private val _duplicateAnalysis = MutableStateFlow<DuplicateAnalysis?>(null)
    val duplicateAnalysis: StateFlow<DuplicateAnalysis?> = _duplicateAnalysis

    data class DuplicateAnalysis(
        val totalServices: Int,
        val uniqueServices: Int,
        val duplicateGroups: List<DuplicateGroup>,
        val summary: String
    )

    data class DuplicateGroup(
        val key: String,
        val services: List<Service>,
        val count: Int
    )

    init {
        loadPendingServices()
        loadFirebaseServiceCount()
    }

    fun loadPendingServices() {
        viewModelScope.launch {
            serviceRepository.getPendingServices()
                .catch { e ->
                    _error.value = "Failed to load pending services: ${e.message}"
                }
                .collect { services ->
                    _pendingServices.value = services
                }
        }
    }

    // Add function to load Firebase service count
    fun loadFirebaseServiceCount() {
        viewModelScope.launch {
            try {
                Log.d("AdminViewModel", "Loading Firebase service count")
                serviceRepository.getAllServices()
                    .catch { e ->
                        Log.e("AdminViewModel", "Error loading Firebase service count", e)
                        _firebaseServiceCount.value = null
                    }
                    .collect { services ->
                        Log.d("AdminViewModel", "Firebase service count: ${services.size}")
                        _firebaseServiceCount.value = services.size
                    }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading Firebase service count", e)
                _firebaseServiceCount.value = null
            }
        }
    }

    // Add function to analyze duplicates
    fun analyzeDuplicates(allServices: List<Service>) {
        viewModelScope.launch {
            try {
                Log.d("AdminViewModel", "Analyzing duplicates for ${allServices.size} services")
                
                // Group services by organization, group name, and location
                val grouped = allServices.groupBy { service ->
                    "${service.organizationName.lowercase().trim()}|${service.groupName.lowercase().trim()}|${service.location.lowercase().trim()}"
                }
                
                // Find groups with more than one service
                val duplicateGroups = grouped.values
                    .filter { it.size > 1 }
                    .map { services ->
                        val firstService = services.first()
                        val key = "${firstService.organizationName} - ${firstService.groupName} - ${firstService.location}"
                        DuplicateGroup(key, services, services.size)
                    }
                    .sortedByDescending { it.count }
                
                val totalServices = allServices.size
                val uniqueServices = grouped.size
                val duplicateCount = duplicateGroups.sumOf { it.count - 1 }
                
                val summary = buildString {
                    appendLine(" Duplicate Analysis Summary")
                    appendLine("Total Services: $totalServices")
                    appendLine("Unique Services: $uniqueServices")
                    appendLine("Duplicate Services: $duplicateCount")
                    appendLine("Duplicate Groups: ${duplicateGroups.size}")
                    
                    if (duplicateGroups.isNotEmpty()) {
                        appendLine("\n🔍 Duplicate Groups:")
                        duplicateGroups.forEach { group ->
                            appendLine("• ${group.key} (${group.count} copies)")
                        }
                    }
                }
                
                val analysis = DuplicateAnalysis(
                    totalServices = totalServices,
                    uniqueServices = uniqueServices,
                    duplicateGroups = duplicateGroups,
                    summary = summary
                )
                
                _duplicateAnalysis.value = analysis
                Log.d("AdminViewModel", "Duplicate analysis completed: $summary")
                
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error analyzing duplicates", e)
                _error.value = "Error analyzing duplicates: ${e.message}"
            }
        }
    }

    fun clearDuplicateAnalysis() {
        _duplicateAnalysis.value = null
    }

    fun updateServiceStatus(serviceId: String, newStatus: ServiceStatus) {
        viewModelScope.launch {
            _statusUpdateState.value = serviceRepository.updateServiceStatus(serviceId, newStatus)
                .onSuccess {
                    loadPendingServices()
                }
                .onFailure { e ->
                    _error.value = "Failed to update service status: ${e.message}"
                }
        }
    }

    fun approveService(serviceId: String) {
        updateServiceStatus(serviceId, ServiceStatus.APPROVED)
    }

    fun rejectService(serviceId: String) {
        updateServiceStatus(serviceId, ServiceStatus.REJECTED)
    }

    fun clearStatusUpdate() {
        _statusUpdateState.value = null
    }

    // Add migration function
    fun migrateToDescriptiveIds() {
        viewModelScope.launch {
            try {
                Log.d("AdminViewModel", "Starting migration to descriptive IDs")
                val result = serviceRepository.migrateToDescriptiveIds()
                _migrationState.value = result
                
                result.onSuccess { count ->
                    Log.d("AdminViewModel", "Successfully migrated $count documents to descriptive IDs")
                    // Reload services after migration
                    loadPendingServices()
                    loadFirebaseServiceCount()
                }.onFailure { e ->
                    Log.e("AdminViewModel", "Failed to migrate documents", e)
                    _error.value = "Failed to migrate documents: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error during migration", e)
                _error.value = "Error during migration: ${e.message}"
                _migrationState.value = Result.failure(e)
            }
        }
    }

    fun clearMigrationState() {
        _migrationState.value = null
    }

    // Add function to check and populate Firebase
    fun checkAndPopulateFirebase() {
        viewModelScope.launch {
            try {
                Log.d("AdminViewModel", "Checking Firebase status")
                val result = serviceRepository.checkAndPopulateFirebase()
                _firebaseCheckState.value = result
                
                result.onSuccess { message ->
                    Log.d("AdminViewModel", "Firebase check result: $message")
                    _error.value = null
                    // Reload Firebase count after check
                    loadFirebaseServiceCount()
                }.onFailure { e ->
                    Log.e("AdminViewModel", "Failed to check Firebase", e)
                    _error.value = "Failed to check Firebase: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error checking Firebase", e)
                _error.value = "Error checking Firebase: ${e.message}"
                _firebaseCheckState.value = Result.failure(e)
            }
        }
    }

    fun clearFirebaseCheckState() {
        _firebaseCheckState.value = null
    }

    // Add function to replace Firebase with hardcoded services
    fun replaceFirebaseWithHardcodedServices() {
        viewModelScope.launch {
            try {
                Log.d("AdminViewModel", "Replacing Firebase with hardcoded services")
                val result = serviceRepository.replaceFirebaseWithHardcodedServices()
                _replaceFirebaseState.value = result
                
                result.onSuccess { message ->
                    Log.d("AdminViewModel", "Firebase replacement result: $message")
                    _error.value = null
                    // Reload Firebase count after replacement
                    loadFirebaseServiceCount()
                }.onFailure { e ->
                    Log.e("AdminViewModel", "Failed to replace Firebase", e)
                    _error.value = "Failed to replace Firebase: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error replacing Firebase", e)
                _error.value = "Error replacing Firebase: ${e.message}"
                _replaceFirebaseState.value = Result.failure(e)
            }
        }
    }

    fun clearReplaceFirebaseState() {
        _replaceFirebaseState.value = null
    }
} 