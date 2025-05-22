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

class AdminViewModel(
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    private val _pendingServices = MutableStateFlow<List<Service>>(emptyList())
    val pendingServices: StateFlow<List<Service>> = _pendingServices

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _statusUpdateState = MutableStateFlow<Result<Unit>?>(null)
    val statusUpdateState: StateFlow<Result<Unit>?> = _statusUpdateState

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
} 