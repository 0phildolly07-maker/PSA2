package com.philapp.psa2.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.philapp.psa2.data.ServiceManager
import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.repository.ServiceRepository
import com.philapp.psa2.utils.LocationFilter
import com.philapp.psa2.utils.ServiceSearch
import com.philapp.psa2.utils.ServiceSort
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    application: Application,
    private val serviceRepository: ServiceRepository
) : AndroidViewModel(application) {
    private val repository = serviceRepository

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null

    private val _statusUpdateState = MutableStateFlow<Result<Unit>?>(null)
    val statusUpdateState: StateFlow<Result<Unit>?> = _statusUpdateState.asStateFlow()

    init {
        loadServices()
        getUserLocation()
    }

    fun loadServices() {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val migrationResult = serviceRepository.approveExistingPendingServicesOnFirstRun(getApplication())
                migrationResult.onSuccess { count ->
                    if (count > 0) {
                        Log.d("SearchViewModel", "Auto-approved $count existing pending services on first run")
                    }
                }.onFailure { e ->
                    Log.e("SearchViewModel", "Auto-approval migration failed: ${e.message}", e)
                }

                ServiceManager.loadServicesLive(getApplication()) { services ->
                    _services.value = services
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading services", e)
                _error.value = e
                _isLoading.value = false
            }
        }
    }

    fun stopListening() {
        ServiceManager.stopListening()
    }

    @SuppressLint("MissingPermission")
    fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    _userLocation.value = location
                }
                .addOnFailureListener { e ->
                    Log.w("SearchViewModel", "Unable to get location: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.w("SearchViewModel", "Location permission not granted")
        }
    }

    fun searchServices(query: String = "", type: ServiceType? = null, location: String? = null) {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (LocationFilter.isNearby(location)) {
                    getUserLocation()
                }
                ServiceManager.loadServicesLive(getApplication()) { allServices ->
                    val filtered = allServices.filter { service ->
                        val matchesQuery = ServiceSearch.matches(service, query)
                        val matchesType = type == null || service.types.contains(type)
                        val matchesLocation = when {
                            LocationFilter.isAnywhere(location) || LocationFilter.isNearby(location) -> true
                            else -> service.location.contains(location.orEmpty(), ignoreCase = true) ||
                                service.town.contains(location.orEmpty(), ignoreCase = true)
                        }
                        matchesQuery && matchesType && matchesLocation && service.status == ServiceStatus.APPROVED
                    }

                    val sorted = if (LocationFilter.isNearby(location)) {
                        filtered.sortedWith { a, b ->
                            ServiceSort.compareByDistanceThenTown(a, b, _userLocation.value)
                        }
                    } else {
                        filtered.sortedWith(ServiceSort::compareByTownThenDay)
                    }

                    _services.value = sorted
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching services", e)
                _error.value = e
                _isLoading.value = false
            }
        }
    }

    data class DuplicateInfo(
        val isDuplicate: Boolean,
        val matchingService: Service? = null
    )

    fun checkForDuplicateService(service: Service): DuplicateInfo {
        val duplicate = _services.value.find { existingService ->
            existingService.id != service.id &&
                existingService.groupName.equals(service.groupName, ignoreCase = true) &&
                existingService.organizationName.equals(service.organizationName, ignoreCase = true) &&
                existingService.location.equals(service.location, ignoreCase = true)
        }
        return DuplicateInfo(isDuplicate = duplicate != null, matchingService = duplicate)
    }

    fun getPendingServices(): List<Pair<Service, DuplicateInfo>> {
        return _services.value
            .filter { it.status == ServiceStatus.PENDING }
            .map { service -> service to checkForDuplicateService(service) }
    }

    fun getDuplicateServices(): List<List<Service>> {
        val grouped = _services.value.groupBy { Triple(it.organizationName, it.groupName, it.location) }
        return grouped.values.filter { it.size > 1 }
    }

    fun removeDuplicateServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                serviceRepository.removeDuplicateServices()
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateServiceStatus(serviceId: String, newStatus: ServiceStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = repository.updateServiceStatus(serviceId, newStatus)
                _statusUpdateState.value = result
                result.onSuccess {
                    _services.value = _services.value.map { service ->
                        if (service.id == serviceId) service.copy(status = newStatus) else service
                    }
                }.onFailure { e ->
                    _error.value = e
                }
            } catch (e: Exception) {
                _error.value = e
                _statusUpdateState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateService(
        serviceId: String,
        organizationName: String,
        groupName: String,
        location: String,
        town: String,
        description: String,
        types: List<ServiceType>,
        features: List<String>,
        contactPhone: String,
        contactEmail: String,
        schedule: String,
        websiteUrl: String,
        onComplete: ((Throwable?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                var existingService = _services.value.find { it.id == serviceId }
                if (existingService == null) {
                    existingService = repository.getServiceById(serviceId).getOrNull()
                }
                if (existingService == null) {
                    val err = IllegalStateException("Service not found: $serviceId")
                    _error.value = err
                    onComplete?.invoke(err)
                    return@launch
                }
                val updatedService = existingService.copy(
                    organizationName = organizationName,
                    groupName = groupName,
                    location = location,
                    town = town,
                    description = description,
                    types = types,
                    features = features,
                    contact = ContactInfo(phone = contactPhone, email = contactEmail),
                    schedule = schedule.takeIf { it.isNotBlank() },
                    websiteUrl = websiteUrl.takeIf { it.isNotBlank() }
                )
                repository.updateService(updatedService)
                _services.value = _services.value.map { if (it.id == serviceId) updatedService else it }
                loadServices()
                onComplete?.invoke(null)
            } catch (e: Exception) {
                _error.value = e
                onComplete?.invoke(e)
            }
        }
    }

    fun syncAllServicesWithFirebase() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val existingServices = mutableListOf<Service>()
                serviceRepository.getAllServices().collect { services ->
                    existingServices.clear()
                    existingServices.addAll(services)
                }
                val missingServices = ServiceManager.getHardcodedServices().filter { hardcodedService ->
                    existingServices.none { existingService ->
                        existingService.organizationName.equals(hardcodedService.organizationName, ignoreCase = true) &&
                            existingService.groupName.equals(hardcodedService.groupName, ignoreCase = true) &&
                            existingService.location.equals(hardcodedService.location, ignoreCase = true)
                    }
                }
                missingServices.forEach { service ->
                    repository.addService(service)
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun replaceFirebaseWithHardcodedServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val clearResult = serviceRepository.clearAllServices()
                clearResult.onSuccess {
                    ServiceManager.getHardcodedServices().forEach { service ->
                        repository.addService(service)
                    }
                }.onFailure { e ->
                    _error.value = e
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun addService(service: Service) {
        val result = serviceRepository.addService(service)
        result.onFailure { throw it }
    }

    suspend fun deleteService(serviceId: String) {
        repository.deleteService(serviceId)
    }
}
