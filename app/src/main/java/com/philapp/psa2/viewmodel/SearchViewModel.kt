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
import com.philapp.psa2.model.AreaList
import com.philapp.psa2.repository.ServiceRepository
import com.philapp.psa2.utils.LocationFilter
import com.philapp.psa2.utils.ServiceSearch
import com.philapp.psa2.utils.ServiceSort
import com.philapp.psa2.utils.TownCoordinates
import com.philapp.psa2.utils.TownListMerger
import com.phild.servicescanner.domain.model.TownRecord
import com.phild.servicescanner.domain.town.TownNormalizer
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
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

    private val _allServices = MutableStateFlow<List<Service>>(emptyList())

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null

    private val _statusUpdateState = MutableStateFlow<Result<Unit>?>(null)
    val statusUpdateState: StateFlow<Result<Unit>?> = _statusUpdateState.asStateFlow()

    private val _availableTowns = MutableStateFlow(AreaList.Lancashire_Areas)
    val availableTowns: StateFlow<List<String>> = _availableTowns.asStateFlow()

    private val _townRecords = MutableStateFlow<List<TownRecord>>(emptyList())
    private var townsListener: ListenerRegistration? = null

    init {
        loadServices()
        getUserLocation()
        listenToTowns()
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
                    _allServices.value = services
                    _services.value = services
                    refreshAvailableTowns()
                    _isLoading.value = false
                    viewModelScope.launch {
                        backfillApprovedTowns()
                    }
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
        townsListener?.remove()
        townsListener = null
    }

    private fun listenToTowns() {
        townsListener?.remove()
        townsListener = serviceRepository.townsRepository.listen { towns ->
            _townRecords.value = towns
            TownCoordinates.updateExtras(towns)
            refreshAvailableTowns()
            viewModelScope.launch {
                backfillApprovedTowns()
            }
        }
    }

    private fun refreshAvailableTowns() {
        _availableTowns.value = TownListMerger.visibleTowns(
            seedTowns = AreaList.Lancashire_Areas,
            services = _allServices.value
        )
    }

    private suspend fun backfillApprovedTowns() {
        val existingSlugs = _townRecords.value.map { it.id }.toSet()
        val missing = _allServices.value
            .filter { it.status == ServiceStatus.APPROVED }
            .mapNotNull { TownNormalizer.normalize(it.town) }
            .distinctBy { it.slug }
            .filter { it.slug !in existingSlugs }
        missing.forEach { town ->
            try {
                serviceRepository.townsRepository.upsertTown(town.displayName, TownRecord.SOURCE_ADMIN)
            } catch (e: Exception) {
                Log.w("SearchViewModel", "Unable to backfill town '${town.displayName}': ${e.message}")
            }
        }
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

                    _allServices.value = allServices
                    _services.value = sorted
                    refreshAvailableTowns()
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
                    _allServices.value = _allServices.value.map { service ->
                        if (service.id == serviceId) service.copy(status = newStatus) else service
                    }
                    refreshAvailableTowns()
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
                    ?: _allServices.value.find { it.id == serviceId }
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

    suspend fun addService(service: Service) {
        val result = serviceRepository.addService(service)
        result.onFailure { throw it }
    }

    suspend fun deleteService(serviceId: String) {
        repository.deleteService(serviceId)
    }
}
