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

    init {
        loadPendingServices()
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