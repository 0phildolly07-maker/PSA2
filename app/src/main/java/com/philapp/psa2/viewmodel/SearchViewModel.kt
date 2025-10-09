package com.philapp.psa2.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.philapp.psa2.api.PlacesApiService
import com.philapp.psa2.api.ActivitiesApiService
import com.philapp.psa2.model.*
import com.philapp.psa2.repository.ServiceRepository
import com.philapp.psa2.data.ServiceManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class SearchViewModel(
    application: Application,
    private val serviceRepository: ServiceRepository
) : AndroidViewModel(application) {
    private val repository = serviceRepository
    
    private val _searchState = mutableStateOf<SearchState>(SearchState.Initial)
    val searchState = _searchState

    private val _userLocation = mutableStateOf<Location?>(null)
    val userLocation = _userLocation

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _externalActivities = mutableStateOf<List<ExternalActivity>>(emptyList())
    val externalActivities = _externalActivities

    private val _isSearching = mutableStateOf(false)
    val isSearching = _isSearching

    private val _locationSuggestions = mutableStateOf<List<String>>(emptyList())
    val locationSuggestions: State<List<String>> = _locationSuggestions

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val placesApi = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/place/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlacesApiService::class.java)

    private val activitiesApi = Retrofit.Builder()
        .baseUrl("https://api.activities.com/") // Replace with actual API endpoint
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ActivitiesApiService::class.java)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null

    private val _statusUpdateState = MutableStateFlow<Result<Unit>?>(null)
    val statusUpdateState: StateFlow<Result<Unit>?> = _statusUpdateState.asStateFlow()

    init {
        loadServices()
    }

    fun loadServices() {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("SearchViewModel", "Loading all services using ServiceManager")
                ServiceManager.loadServicesLive(getApplication()) { services ->
                    Log.d("SearchViewModel", "Received ${services.size} services from ServiceManager")
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

    fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    _userLocation.value = location
                }
                .addOnFailureListener { e ->
                    _searchState.value = SearchState.Error("Unable to get location: ${e.message}")
                }
        } catch (e: SecurityException) {
            _searchState.value = SearchState.Error("Location permission not granted")
        }
    }

    fun searchServices(query: String = "", type: ServiceType? = null, location: String? = null) {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("SearchViewModel", "Searching services - Query: $query, Type: $type, Location: $location")
                ServiceManager.loadServicesLive(getApplication()) { allServices ->
                    val filteredServices = allServices.filter { service ->
                        val matchesQuery = query.isEmpty() || 
                            service.organizationName.contains(query, ignoreCase = true) ||
                            service.groupName.contains(query, ignoreCase = true) ||
                            service.description.contains(query, ignoreCase = true)
                        
                        val matchesType = type == null || service.types.contains(type)
                        
                        val matchesLocation = location.isNullOrEmpty() ||
                            service.location.contains(location, ignoreCase = true)
                        
                        // Only show approved services in public search results
                        val isApproved = service.status == ServiceStatus.APPROVED
                        
                        matchesQuery && matchesType && matchesLocation && isApproved
                    }
                    
                    Log.d("SearchViewModel", "Found ${filteredServices.size} approved services matching criteria (from ${allServices.size} total)")
                    _services.value = filteredServices
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching services", e)
                _error.value = e
                _isLoading.value = false
            }
        }
    }

    fun retryLastSearch() {
        loadServices()
    }

    fun filterServicesByType(type: ServiceType?) {
        viewModelScope.launch {
            try {
                println("Filtering services by type: $type") // Debug log
                val filteredServices = if (type == null) {
                    println("No type filter, showing all services") // Debug log
                    _services.value
                } else {
                    _services.value.filter { service ->
                        val matches = service.types.contains(type)
                        println("Service ${service.organizationName} types ${service.types}: matches=$matches") // Debug log
                        matches
                    }
                }
                println("Found ${filteredServices.size} services matching type $type") // Debug log
                _searchState.value = SearchState.Success(filteredServices)
            } catch (e: Exception) {
                println("Error filtering services by type: ${e.message}") // Debug log
                e.printStackTrace()
                _searchState.value = SearchState.Error("Failed to filter services: ${e.message}")
            }
        }
    }

    fun getServicesByCategory(category: ServiceType): List<Service> {
        return _services.value.filter { service -> category in service.types }
    }

    data class DuplicateInfo(
        val isDuplicate: Boolean,
        val matchingService: Service? = null
    )

    fun checkForDuplicateService(service: Service): DuplicateInfo {
        val duplicate = _services.value.find { existingService ->
            existingService.id != service.id && // Not the same service
            existingService.groupName.equals(service.groupName, ignoreCase = true) && // Same group name
            existingService.organizationName.equals(service.organizationName, ignoreCase = true) && // Same organization
            existingService.location.equals(service.location, ignoreCase = true) // Same location
        }
        return DuplicateInfo(isDuplicate = duplicate != null, matchingService = duplicate)
    }

    fun getPendingServices(): List<Pair<Service, DuplicateInfo>> {
        return _services.value
            .filter { it.status == ServiceStatus.PENDING }
            .map { service -> 
                service to checkForDuplicateService(service)
            }
    }

    fun getApprovedServices(): List<Service> {
        return _services.value.filter { it.status == ServiceStatus.APPROVED }
    }

    fun getRejectedServices(): List<Service> {
        return _services.value.filter { it.status == ServiceStatus.REJECTED }
    }

    fun getDuplicateServices(): List<List<Service>> {
        val services = _services.value
        val grouped = services.groupBy { Triple(it.organizationName, it.groupName, it.location) }
        return grouped.values.filter { it.size > 1 }
    }

    fun removeDuplicateServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                serviceRepository.removeDuplicateServices()
                // ServiceManager will automatically update via Firestore listener
                Log.d("SearchViewModel", "Successfully removed duplicate services")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error removing duplicate services", e)
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
                Log.d("SearchViewModel", "Updating service $serviceId status to $newStatus")
                
                // Update in repository
                val result = repository.updateServiceStatus(serviceId, newStatus)
                _statusUpdateState.value = result
                
                result.onSuccess {
                    // Update the service status in the current list
                    val updatedServices = _services.value.map { service ->
                        if (service.id == serviceId) {
                            service.copy(status = newStatus)
                        } else {
                            service
                        }
                    }
                    _services.value = updatedServices
                    Log.d("SearchViewModel", "Successfully updated service status")
                }.onFailure { e ->
                    Log.e("SearchViewModel", "Failed to update service status", e)
                    _error.value = e
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error updating service status", e)
                _error.value = e
                _statusUpdateState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addNewService(
        organizationName: String,
        groupName: String,
        location: String,
        description: String,
        types: List<ServiceType>,
        features: List<String>,
        contactPhone: String,
        contactEmail: String,
        schedule: String
    ) {
        viewModelScope.launch {
            try {
                val newService = Service(
                    id = java.util.UUID.randomUUID().toString(),
                    organizationName = organizationName,
                    groupName = groupName,
                    location = location,
                    description = description,
                    types = types,
                    features = features,
                    contact = if (contactPhone.isNotBlank() || contactEmail.isNotBlank()) {
                        ContactInfo(phone = contactPhone, email = contactEmail)
                    } else null,
                    schedule = if (schedule.isNotBlank()) schedule else null
                )
                
                Log.d("SearchViewModel", "Adding new service: ${newService.organizationName} - ${newService.groupName}")
                val result = repository.addService(newService)
                
                result.onSuccess { serviceId ->
                    Log.d("SearchViewModel", "Successfully added service with ID: $serviceId")
                }.onFailure { e ->
                    Log.e("SearchViewModel", "Failed to add service", e)
                    _searchState.value = SearchState.Error("Failed to add service: ${e.message}")
                }
                
                // ServiceManager will automatically update via Firestore listener
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error in addNewService", e)
                _searchState.value = SearchState.Error("Failed to add service: ${e.message}")
            }
        }
    }

    fun getFilteredServices(type: ServiceType?, location: String?): List<Service> {
        return _services.value.filter { service ->
            (type == null || type in service.types) &&
            (location == null || location.trim().isEmpty() || 
                service.location.trim().lowercase() == location.trim().lowercase())
        }
    }

    fun searchExternalActivities(query: String, type: String? = null) {
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchState.value = SearchState.Loading

                val location = _userLocation.value
                if (location == null) {
                    _searchState.value = SearchState.Error("Location not available")
                    return@launch
                }

                val locationString = "${location.latitude},${location.longitude}"
                val response = activitiesApi.searchActivities(
                    query = query,
                    location = locationString,
                    type = type
                )

                _externalActivities.value = response.results
                _searchState.value = SearchState.Success(_services.value)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Failed to search activities: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addExternalActivityToServices(activity: ExternalActivity) {
        val newService = Service(
            id = "external_${activity.id}",
            organizationName = activity.organization,
            groupName = activity.name,
            location = activity.location,
            description = activity.description,
            types = activity.types,
            features = activity.features,
            contact = activity.contact,
            schedule = activity.schedule
        )

        viewModelScope.launch {
            try {
                repository.addService(newService)
                loadServices() // Reload services after adding
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Failed to add external activity: ${e.message}")
            }
        }
    }

    fun clearExternalActivities() {
        _externalActivities.value = emptyList()
    }

    fun getLocationSuggestions(query: String) {
        if (query.isBlank()) {
            _locationSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            // Get unique locations from services
            val uniqueLocations = _services.value
                .map { it.location }
                .distinct()
                .filter { it.contains(query, ignoreCase = true) }
                .take(5) // Limit to 5 suggestions
            
            _locationSuggestions.value = uniqueLocations
        }
    }

    fun updateService(
        serviceId: String,
        organizationName: String? = null,
        groupName: String? = null,
        location: String? = null,
        description: String? = null,
        types: List<ServiceType>? = null,
        features: List<String>? = null,
        contactPhone: String? = null,
        contactEmail: String? = null,
        schedule: String? = null,
        websiteUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val existingService = _services.value.find { it.id == serviceId } ?: return@launch
                val updatedService = existingService.copy(
                    organizationName = organizationName ?: existingService.organizationName,
                    groupName = groupName ?: existingService.groupName,
                    location = location ?: existingService.location,
                    description = description ?: existingService.description,
                    types = types ?: existingService.types,
                    features = features ?: existingService.features,
                    contact = if (contactPhone != null || contactEmail != null) {
                        ContactInfo(
                            phone = contactPhone ?: existingService.contact?.phone ?: "",
                            email = contactEmail ?: existingService.contact?.email ?: ""
                        )
                    } else existingService.contact,
                    schedule = schedule ?: existingService.schedule,
                    websiteUrl = websiteUrl ?: existingService.websiteUrl
                )
                repository.updateService(updatedService)
                
                // Update the local list immediately
                _services.value = _services.value.map { if (it.id == serviceId) updatedService else it }
                
                // Reload services from Firebase to ensure consistency
                loadServices()
                
                Log.d("SearchViewModel", "Successfully updated service: ${updatedService.organizationName} - ${updatedService.groupName}")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Failed to update service", e)
                _searchState.value = SearchState.Error("Failed to update service: ${e.message}")
            }
        }
    }

    private fun getSampleServices(): List<Service> {
        return listOf(
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Funday Monday",
                location = "St.James Old School Building, Accrington",
                description = "Light-hearted bingo with peers",
                types = listOf(ServiceType.SOCIAL),
                features = listOf("Bingo", "Social", "Peer Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Monday 13:30-14:30 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Mindful Movement",
                location = "St.James Old School Building, Accrington",
                description = "Calm your mind with relaxing movements and light exercise, recovery based.",
                types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.RECOVERY),
                features = listOf("Mindfulness", "Exercise", "Recovery Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Monday 14:30-15:30 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Community Cafe",
                location = "St.James Old School Building, Accrington",
                description = "Coffee and Chat with others in recovery",
                types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                features = listOf("Coffee", "Social", "Peer Support"),
                contact = ContactInfo(
                    phone = "Gemma",
                    email = ""
                ),
                schedule = "Tuesday 10:00-11:30 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Lunch, Brunch, Walk and Talk",
                location = "ABD Centre, Burnley Road, Bacup",
                description = "Come along for breakfast and a lovely walk through the countryside. Pick-ups can be arranged from fixed locations.",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
                features = listOf("Breakfast", "Walking", "Transport available", "Social"),
                contact = ContactInfo(
                    phone = "Shaun - 07351614902",
                    email = ""
                ),
                schedule = "Tuesday 11:00-14:00 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Here & Now",
                location = "St.James Old School Building, Accrington",
                description = "Peer support with lived experience facilitators, Recovery Based.",
                types = listOf(ServiceType.PEER_SUPPORT),
                features = listOf("Peer Support", "Recovery Based", "Lived Experience"),
                contact = ContactInfo(
                    phone = "Gemma - 07483915707",
                    email = ""
                ),
                schedule = "Tuesday 12:30-13:30 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Feel Good Fitness",
                location = "St.James Old School Building, Accrington",
                description = "Light exercise suitable for all abilities",
                types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.RECOVERY, ServiceType.SOCIAL),
                features = listOf("Exercise", "All abilities", "Recovery Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Tuesday 14:00-15:00 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Fishing for Mental Health",
                location = "Burnley Inspire East Lancashire, Westgate, BB111RY",
                description = "Fishing Group, Recovery Based, Pick-ups from fixed locations. Bookings must be made with Shaun or Gareth.",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.RECOVERY, ServiceType.MENTAL_HEALTH, ServiceType.SKILL_BUILDING),
                features = listOf("Fishing", "Transport available", "Booking required", "Recovery Support"),
                contact = ContactInfo(
                    phone = "Shaun - 07351614902, Gareth - 07351614926",
                    email = ""
                ),
                schedule = "Wednesday 09:00-14:00 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "5 Ways to Wellbeing",
                location = "St.James Old School Building, Accrington",
                description = "Peer support Group based on 5 ways of wellbeing, Recovery Based",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.RECOVERY, ServiceType.SKILL_BUILDING),
                features = listOf("Wellbeing", "Peer Support", "Recovery Based"),
                contact = ContactInfo(
                    phone = "Emma - 07852221505",
                    email = ""
                ),
                schedule = "Wednesday 10:00-11:00 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "No Excuse Boxing Workout",
                location = "87 Blackburn Road, Accrington",
                description = "Improve your fitness in a friendly environment with other people in recovery from drugs/alcohol/mental health issues.",
                types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.RECOVERY),
                features = listOf("Boxing", "Fitness", "Recovery Support", "Group Workout"),
                contact = ContactInfo(
                    phone = "Gemma - 07483915707",
                    email = ""
                ),
                schedule = "Monday 11:45-13:00 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Get Crafty",
                location = "St.James Old School Building, Accrington",
                description = "Arts And Crafts",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.PEER_SUPPORT),
                features = listOf("Arts and Crafts", "Social", "Peer Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Monday 10:00-11:45 (Weekly)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Cheeky Monkey",
                groupName = "Bumhole Defenders",
                location = "Accrington",
                description = "Mental health and wellbeing group for monkeys, peer support, breakfast club, guitar group, music",
                types = listOf(ServiceType.MENTAL_HEALTH),
                features = listOf("Peer support", "Breakfast club", "Guitar group", "Music"),
                contact = ContactInfo(
                    phone = "ring em up",
                    email = ""
                ),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday, 11am - 12pm",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://redroserecovery.org.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Woodnook Breakfast Club",
                location = "Woodnook Community Centre, Royd Street, Accrington, BB5 2JH",
                description = "",
                types = listOf(ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                features = listOf("Breakfast", "Social", "Support"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Tuesday 09:30-10:30",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Drop-in",
                location = "The Zone, New Era, Accrington, BB5 1PB",
                description = "Come along for a friendly chat, grab a brew and a biscuit, and talk things over in a supportive environment",
                types = listOf(ServiceType.PEER_SUPPORT),
                features = listOf("Drop-in", "Social", "Support", "Refreshments"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Tuesday 11:00-13:00",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Brunch Club",
                location = "A B & D Centre, Burnley Road, Bacup, OL13 8AB",
                description = "Free food, free advice, and friendly, supportive company",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL),
                features = listOf("Food", "Advice", "Support", "Social"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Tuesday 11:00-12:00",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Walking group Bacup",
                location = "A B & D Centre, Burnley Road, Bacup, OL13 8AB",
                description = "Walking group with friendly folk. All abilities welcome!",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
                features = listOf("Walking", "Exercise", "Social", "Inclusive"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Tuesday 12:00-14:00",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Acupuncture",
                location = "Inspire, 33 Eagle Street, Accrington, BB5 1LN",
                description = "",
                types = listOf(ServiceType.MENTAL_HEALTH),
                features = listOf("Acupuncture", "Wellbeing"),
                contact = ContactInfo(phone = "Please ask your key-worker for details", email = ""),
                schedule = "Tuesday 12:50",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
            ),
            
            // Add Colne services
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Colne Community Centre",
                groupName = "Colne Walking Group",
                location = "Colne Community Centre, Colne",
                description = "Weekly walking group exploring the beautiful countryside around Colne. All abilities welcome!",
                types = listOf(ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS, ServiceType.PEER_SUPPORT),
                features = listOf("Walking", "Outdoor", "Social", "All abilities"),
                contact = ContactInfo(
                    phone = "01282 123456",
                    email = "info@colnecommunity.org"
                ),
                schedule = "Tuesday 10:00-12:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Colne Mental Health Support",
                groupName = "Colne Wellbeing Group",
                location = "Colne Library, Market Street, Colne",
                description = "A supportive group for people experiencing mental health challenges. Share experiences and learn coping strategies.",
                types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                features = listOf("Mental health support", "Peer support", "Coping strategies", "Safe space"),
                contact = ContactInfo(
                    phone = "01282 654321",
                    email = "wellbeing@colne.org"
                ),
                schedule = "Thursday 14:00-16:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Colne Sports Centre",
                groupName = "Colne Fitness Club",
                location = "Colne Sports Centre, Colne",
                description = "Fitness sessions suitable for all levels. Improve your health and meet new people in a friendly environment.",
                types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.SOCIAL),
                features = listOf("Fitness", "Exercise", "Social", "All levels"),
                contact = ContactInfo(
                    phone = "01282 789012",
                    email = "fitness@colnesports.org"
                ),
                schedule = "Monday and Wednesday 18:00-19:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            
            // Practical Support Services
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Lancashire Food Bank",
                groupName = "Accrington Food Bank",
                location = "Accrington Community Centre, Accrington",
                description = "Emergency food support for individuals and families in need. No referral required, confidential service.",
                types = listOf(ServiceType.PRACTICAL, ServiceType.FOOD_BANKS),
                features = listOf("Emergency food", "No referral needed", "Confidential", "Family support"),
                contact = ContactInfo(
                    phone = "01282 456789",
                    email = "accrington@lancashirefoodbank.org"
                ),
                schedule = "Monday, Wednesday, Friday 10:00-14:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Lancashire Food Bank",
                groupName = "Burnley Food Bank",
                location = "Burnley Community Hub, Burnley",
                description = "Food bank providing essential supplies to those experiencing food poverty. Referral system available.",
                types = listOf(ServiceType.PRACTICAL, ServiceType.FOOD_BANKS),
                features = listOf("Essential supplies", "Referral system", "Emergency support", "Community hub"),
                contact = ContactInfo(
                    phone = "01282 789456",
                    email = "burnley@lancashirefoodbank.org"
                ),
                schedule = "Tuesday, Thursday 09:00-15:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Lancashire Care Services",
                groupName = "Home Care Support",
                location = "Various locations across Lancashire",
                description = "Professional home care services including personal care, domestic support, and companionship for elderly and vulnerable individuals.",
                types = listOf(ServiceType.PRACTICAL),
                features = listOf("Personal care", "Domestic support", "Companionship", "Elderly care", "Vulnerable support"),
                contact = ContactInfo(
                    phone = "0800 123 4567",
                    email = "info@lancashirecare.org"
                ),
                schedule = "Monday to Sunday, 24/7 availability",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Community Transport Service",
                groupName = "Lancashire Community Transport",
                location = "Various pick-up points across Lancashire",
                description = "Accessible transport service for medical appointments, shopping, and social activities. Wheelchair accessible vehicles available.",
                types = listOf(ServiceType.PRACTICAL),
                features = listOf("Medical transport", "Shopping trips", "Wheelchair accessible", "Social outings", "Door-to-door service"),
                contact = ContactInfo(
                    phone = "01282 321654",
                    email = "transport@lancashirecommunity.org"
                ),
                schedule = "Monday to Friday 08:00-18:00",
                status = ServiceStatus.PENDING
            ),
            
            // New Community Interest Groups Services
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Lancashire Creative Writing Group",
                groupName = "Creative Writing Workshop",
                location = "Accrington Library, St James Street, Accrington",
                description = "Join our creative writing group to explore storytelling, poetry, and creative expression. All levels welcome from beginners to experienced writers.",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SKILL_BUILDING),
                features = listOf("Creative writing", "Poetry", "Storytelling", "All levels", "Workshop format"),
                contact = ContactInfo(
                    phone = "01254 123456",
                    email = "writing@lancashirecreative.org"
                ),
                schedule = "Tuesday 14:00-16:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Burnley Music Society",
                groupName = "Community Choir",
                location = "Burnley Community Centre, Burnley",
                description = "Join our friendly community choir. No experience necessary - just bring your voice and enthusiasm! We sing a variety of music from folk to contemporary.",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
                features = listOf("Choir", "Music", "Singing", "No experience needed", "Social"),
                contact = ContactInfo(
                    phone = "01282 654321",
                    email = "choir@burnleymusic.org"
                ),
                schedule = "Thursday 19:00-21:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Colne Photography Club",
                groupName = "Photography for Beginners",
                location = "Colne Community Centre, Colne",
                description = "Learn photography basics and improve your skills. Bring your camera or smartphone. We cover composition, lighting, and editing techniques.",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SKILL_BUILDING),
                features = listOf("Photography", "Beginners welcome", "Camera skills", "Editing", "Outdoor sessions"),
                contact = ContactInfo(
                    phone = "01282 789123",
                    email = "photo@colnecommunity.org"
                ),
                schedule = "Saturday 10:00-12:00 (Weekly)",
                status = ServiceStatus.PENDING
            )
        )
    }

    fun addWednesdayServices() {
        viewModelScope.launch {
            try {
                // Women's Coffee Group
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Women's Coffee Group with Jodie",
                    location = "Inspire Location",  // Location was not specified in the list
                    description = "Women's coffee group session",
                    types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                    features = listOf("Women only", "Coffee group"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Wednesday 10:00 to 13:00"
                )

                // Fishing for mental health
                addNewService(
                    organizationName = "Red Rose Recovery/Inspire",
                    groupName = "Fishing for mental health",
                    location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                    description = "Inclusive fishing group & lessons. Booking must be made as spaces are limited.",
                    types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.RECOVERY, ServiceType.PEER_SUPPORT),
                    features = listOf("Fishing", "Lessons", "Booking required"),
                    contactPhone = "07727611550",
                    contactEmail = "",
                    schedule = "Wednesday 09:15 to 14:30"
                )

                // Art Group
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Art Group Accrington Inspire",
                    location = "Inspire, 33 Eagle Street, Accrington, BB5 1LN",
                    description = "Art group session",
                    types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.RECOVERY),
                    features = listOf("Art", "Creative"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Wednesday 13:00 to 15:00"
                )

                // Recovery Drop-in Clitheroe
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Recovery Drop-in Clitheroe",
                    location = "Young Peoples Centre, Wesleyan Row, Parson Ln, Clitheroe BB7 2JY",
                    description = "Including: Declutter your mind course, brews and biscuits, sound bowl therapy, badminton & basketball",
                    types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS, ServiceType.MENTAL_HEALTH),
                    features = listOf("Declutter your mind", "Sound bowl therapy", "Sports", "Refreshments"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Wednesday 12:30 to 14:30"
                )

                // Recovery Social Evening
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Recovery Social Evening",
                    location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                    description = "A friendly get together for a bit of food, a bit of fun and a bit of banter.",
                    types = listOf(ServiceType.PEER_SUPPORT, ServiceType.RECOVERY),
                    features = listOf("Social", "Food", "Entertainment"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Wednesday 17:00 to 19:30 (Last Wednesday of month)"
                )
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Failed to add Wednesday services: ${e.message}")
            }
        }
    }

    fun addThursdayServices() {
        viewModelScope.launch {
            try {
                // Cooking on a budget
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Cooking on a budget",
                    location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                    description = "Cooking class followed by lunch",
                    types = listOf(ServiceType.SOCIAL, ServiceType.SKILL_BUILDING),
                    features = listOf("Cooking", "Lunch provided", "Budget friendly"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Thursday 10:30 to 12:30"
                )

                // Breakfast Social Club
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Breakfast Social Club",
                    location = "Grassroots Nelson",
                    description = "Morning breakfast social club",
                    types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                    features = listOf("Breakfast", "Social"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Thursday 09:30 to 10:30"
                )

                // Declutter your Mind & Here and Now Group
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Declutter your Mind & Here and Now Group",
                    location = "Grassroots Nelson",
                    description = "Mental wellness and mindfulness group",
                    types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                    features = listOf("Mindfulness", "Mental wellness"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Thursday 10:30 to 11:30"
                )

                // Guitar Lessons
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Guitar Lessons for Beginners",
                    location = "Grassroots Nelson",
                    description = "Beginner guitar lessons",
                    types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SKILL_BUILDING),
                    features = listOf("Music", "Guitar", "Beginner friendly"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Thursday 10:30 to 11:30"
                )

                // Multiple Activities at Old Grammar School
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Multiple Activities Group",
                    location = "The Old Grammar School, Earby",
                    description = "Choice of groups; Walking and wellbeing, art group, cooking and retail volunteering in partnership with Robert Windle Foundation. Breakfast and lunch included (Transport from Burnley at 09:30/ Nelson at 10:30 and back to locations.)",
                    types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SKILL_BUILDING, ServiceType.SPORT_AND_FITNESS, ServiceType.PEER_SUPPORT),
                    features = listOf("Walking", "Art", "Cooking", "Volunteering", "Transport provided", "Meals included"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Thursday 9:30 to 14:30"
                )
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Failed to add Thursday services: ${e.message}")
            }
        }
    }

    fun addFridayServices() {
        viewModelScope.launch {
            try {
                // Breakfast Club
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Breakfast Club",
                    location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                    description = "Breakfast Club for those participating in groups",
                    types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                    features = listOf("Breakfast", "Social"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Friday 09:00 to 10:00"
                )

                // Declutter Your Mind Course
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Declutter Your Mind Course",
                    location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                    description = "Mental wellness and mindfulness course",
                    types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                    features = listOf("Mindfulness", "Mental wellness", "Course"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Friday 10:30 to 12:30"
                )

                // Fun Time Friday
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Fun Time Friday",
                    location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                    description = "Social Group - Burnley inspire",
                    types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                    features = listOf("Social", "Fun activities"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Friday 13:00 to 14:30"
                )

                // Arts & Crafts
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Arts & Crafts with Bekki",
                    location = "Grassroots Nelson",
                    description = "Arts and crafts session",
                    types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
                    features = listOf("Art", "Crafts", "Creative"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Friday 13:30 to 15:00"
                )

                // Here and Now Group
                addNewService(
                    organizationName = "Inspire",
                    groupName = "Here and Now Group with Bryan",
                    location = "Accrington Inspire",
                    description = "Mindfulness and present-moment awareness group",
                    types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                    features = listOf("Mindfulness", "Mental wellness"),
                    contactPhone = "",
                    contactEmail = "",
                    schedule = "Friday 14:00 to 15:00"
                )
            } catch (e: Exception) {
                _searchState.value = SearchState.Error("Failed to add Friday services: ${e.message}")
            }
        }
    }

    // Add a function to add all services at once
    fun addAllServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("SearchViewModel", "Starting to add all services to Firebase")
                
                // Add sample services first
                val sampleServices = getSampleServices()
                sampleServices.forEach { service ->
                    try {
                        Log.d("SearchViewModel", "Adding sample service: ${service.organizationName} - ${service.groupName}")
                        val result = repository.addService(service)
                        result.onSuccess { id ->
                            Log.d("SearchViewModel", "Successfully added sample service with ID: $id")
                        }.onFailure { e ->
                            Log.e("SearchViewModel", "Failed to add sample service", e)
                        }
                    } catch (e: Exception) {
                        Log.e("SearchViewModel", "Error adding sample service", e)
                    }
                }
                
                // Add Wednesday services
                Log.d("SearchViewModel", "Adding Wednesday services")
                try {
                    addWednesdayServices()
                    Log.d("SearchViewModel", "Successfully added Wednesday services")
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Error adding Wednesday services", e)
                }
                
                // Add Thursday services
                Log.d("SearchViewModel", "Adding Thursday services")
                try {
                    addThursdayServices()
                    Log.d("SearchViewModel", "Successfully added Thursday services")
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Error adding Thursday services", e)
                }
                
                // Add Friday services
                Log.d("SearchViewModel", "Adding Friday services")
                try {
                    addFridayServices()
                    Log.d("SearchViewModel", "Successfully added Friday services")
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Error adding Friday services", e)
                }
                
                // Reload services after adding all
                loadServices()
                Log.d("SearchViewModel", "Finished adding all services")
                
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error in addAllServices", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add a function to get all hardcoded services including Wednesday, Thursday, Friday
    private fun getAllHardcodedServices(): List<Service> {
        val allServices = mutableListOf<Service>()
        
        // Add sample services
        allServices.addAll(getSampleServices())
        
        // Add Wednesday services
        allServices.addAll(listOf(
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Women's Coffee Group with Jodie",
                location = "Inspire Location",
                description = "Women's coffee group session",
                types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                features = listOf("Women only", "Coffee group"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Wednesday 10:00 to 13:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery/Inspire",
                groupName = "Fishing for mental health",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Inclusive fishing group & lessons. Booking must be made as spaces are limited.",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.RECOVERY, ServiceType.PEER_SUPPORT),
                features = listOf("Fishing", "Lessons", "Booking required"),
                contact = ContactInfo(phone = "07727611550", email = ""),
                schedule = "Wednesday 09:15 to 14:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Recovery Social Evening",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "A friendly get together for a bit of food, a bit of fun and a bit of banter.",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.RECOVERY),
                features = listOf("Social", "Food", "Entertainment"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Wednesday 17:00 to 19:30 (Last Wednesday of month)",
                status = ServiceStatus.PENDING
            )
        ))
        
        // Add Thursday services
        allServices.addAll(listOf(
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Cooking on a budget",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Cooking class followed by lunch",
                types = listOf(ServiceType.SOCIAL, ServiceType.SKILL_BUILDING),
                features = listOf("Cooking", "Lunch provided", "Budget friendly"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Thursday 10:30 to 12:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Men's Group",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Men's group session",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL),
                features = listOf("Men only", "Peer support"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Thursday 13:00 to 14:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Walking Group",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Walking group for physical and mental wellbeing",
                types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.SOCIAL),
                features = listOf("Walking", "Exercise", "Social"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Thursday 14:00 to 15:30",
                status = ServiceStatus.PENDING
            )
        ))
        
        // Add Friday services
        allServices.addAll(listOf(
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Breakfast Club",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Breakfast Club for those participating in groups",
                types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                features = listOf("Breakfast", "Social"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Friday 09:00 to 10:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Declutter Your Mind Course",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Mental wellness and mindfulness course",
                types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                features = listOf("Mindfulness", "Mental wellness", "Course"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Friday 10:30 to 12:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Fun Time Friday",
                location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
                description = "Social Group - Burnley inspire",
                types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                features = listOf("Social", "Fun activities"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Friday 13:00 to 14:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Arts & Crafts with Bekki",
                location = "Grassroots Nelson",
                description = "Arts and crafts session",
                types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
                features = listOf("Art", "Crafts", "Creative"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Friday 13:30 to 15:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Here and Now Group with Bryan",
                location = "Accrington Inspire",
                description = "Mindfulness and present-moment awareness group",
                types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
                features = listOf("Mindfulness", "Mental wellness"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Friday 14:00 to 15:00",
                status = ServiceStatus.PENDING
            )
        ))
        
        // --- Food Bank Services (added July 2025) ---
        allServices.addAll(listOf(
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Burnley & Pendle Food Bank Group",
                groupName = "Ghausia Food Bank",
                location = "Burnley",
                description = "Deliver food packages in Burnley",
                types = listOf(ServiceType.PRACTICAL),
                features = listOf("Food Bank", "Delivery available"),
                contact = ContactInfo(phone = "07449559459", email = ""),
                schedule = "Monday to Friday (Daily)",
                status = ServiceStatus.PENDING
            )
        ))
        // --- End Food Bank Services ---

        // --- Additional Food Bank Services (added January 2025) ---
        allServices.addAll(getAdditionalFoodBankServices())
        // --- End Additional Food Bank Services ---

        return allServices
    }

    // Function to create additional food bank services
    private fun getAdditionalFoodBankServices(): List<Service> {
        return listOf(
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Burnley Together",
                groupName = "BFCitC Foodbank / Community Grocery",
                location = "Valley Street Community Centre, BB11 5LZ and Charter Walk (above New Look), BB11 1QJ",
                description = "Emergency food parcels and low-cost food membership scheme. Community Grocery offers affordable weekly food shopping for members.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food parcels", "Low-cost membership", "Community grocery", "Weekly shopping"),
                contact = ContactInfo(phone = "01282 686402", email = "contact@burnleytogether.org.uk"),
                schedule = "Community Grocery: 09:00 – 16:00 (times vary by location), Monday to Friday",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Church on the Street Ministries",
                groupName = "Inspiring Grace Foodbank",
                location = "B C Church, Adamson Street, Burnley, BB12 6RB",
                description = "Provides food parcels and support, including deliveries to Burnley & Pendle residents.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Food parcels", "Delivery service", "Support available"),
                contact = ContactInfo(phone = "07582 776574", email = "Michaelflemmingaim@gmail.com"),
                schedule = "By arrangement, as needed (call to confirm)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "The Salvation Army",
                groupName = "Burnley Salvation Army Food Support",
                location = "Richard Street, Burnley, BB11 3AJ",
                description = "Community assistance, including potential food parcel provision.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Community assistance", "Food parcels", "Support services"),
                contact = ContactInfo(phone = "01282 415840 / 01282 425588", email = "lorraine.oneill@salvationarmy.org.uk"),
                schedule = "By arrangement, contact for details",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "RAFT Foundation",
                groupName = "RAFT Food Bank",
                location = "Hardmans Business Centre, New Hall Hey, Rawtenstall, BB4 6HH",
                description = "Free food parcels for individuals and families in need.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Free food parcels", "Individuals and families", "Emergency support"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Tuesday, Thursday, Friday 10:00 – 12:00",
                status = ServiceStatus.PENDING,
                websiteUrl = "raftfoundation.org"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Positive Start",
                groupName = "Positive Start Food Group",
                location = "4 Bury Road, Rawtenstall, BB4 6AA",
                description = "Community food distribution and social meet-up.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.SOCIAL),
                features = listOf("Food distribution", "Social meet-up", "Community support"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Friday 09:30 – 10:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Colne Open Door Centre",
                groupName = "Crisis Drop-in & Food Support",
                location = "1 Great George Street, Colne, BB8 0SY",
                description = "Offers food parcels, free counselling, and community café.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL, ServiceType.MENTAL_HEALTH),
                features = listOf("Food parcels", "Free counselling", "Community café", "Crisis support"),
                contact = ContactInfo(phone = "01282 860342", email = "manager@opendoorcentre.org.uk"),
                schedule = "Monday to Friday 09:00 – 16:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "St Bartholomew's Church",
                groupName = "Community Grocery",
                location = "Church Street, Colne",
                description = "Affordable food club with refreshments and social interaction.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.SOCIAL),
                features = listOf("Affordable food", "Refreshments", "Social interaction", "Food club"),
                contact = ContactInfo(phone = "07547 373970", email = ""),
                schedule = "Friday 09:30 – 11:30",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "West Craven Foodbank",
                groupName = "Emergency Food Support",
                location = "Based in West Craven area (covers parts of Colne)",
                description = "Provides emergency food parcels and deliveries to those in need.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food parcels", "Delivery service", "West Craven area"),
                contact = ContactInfo(phone = "07415 186651", email = "wcravenfood@gmail.com"),
                schedule = "By arrangement, as needed",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Pendle Food for All Club",
                groupName = "Food Points System Club",
                location = "Pendle Family HUB, Leeds Road, Nelson, BB9 8EL",
                description = "£5 weekly membership for points to spend on food items.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Points system", "Weekly membership", "Affordable food"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Thursday 10:30 – 12:30, 13:00 – 14:15",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "St John's with St Philip's Church",
                groupName = "St John's & St Philip's Foodbank",
                location = "Leeds Road, Nelson, BB9 9XB",
                description = "Provides emergency food parcels with prior referral.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food parcels", "Referral required", "Church-based"),
                contact = ContactInfo(phone = "01282 877640", email = "stphilipsnelson@gmail.com"),
                schedule = "Tuesday 11:00 – 13:00",
                status = ServiceStatus.PENDING
            ),
            
            // Additional Food Bank Services - 7 new services
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Church on the Street (COTS)",
                groupName = "Food Bank",
                location = "Bethesda Street, Burnley",
                description = "Food Bank service providing emergency food support to the community.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food", "Community support", "Food Bank"),
                contact = ContactInfo(phone = "01282 222203", email = ""),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday, Sunday (Daily)",
                status = ServiceStatus.PENDING,
                websiteUrl = "https://www.cots-ministries.co.uk"
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Clayton Baptist Church",
                groupName = "Food Bank",
                location = "54 Sparth Rd, Clayton-le-Moors, Accrington BB5 5PZ",
                description = "Food Bank service providing emergency food support to the community.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food", "Community support", "Food Bank"),
                contact = ContactInfo(phone = "07834724530", email = ""),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - After 2pm",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Gannow Community Centre",
                groupName = "Food Bank",
                location = "Adamson St, Burnley BB12 6RB",
                description = "Food Bank service. You can call in to the centre on Adamson Street to collect your parcel but it would be helpful if you phone us first. Pet food is sometimes available.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food", "Pet food available", "Collection service", "Community support"),
                contact = ContactInfo(phone = "01282 436396", email = "alan.barnes@bprvcs.co.uk"),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - 11:00-14:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Burnley Community Kitchen",
                groupName = "Burnley Community Kitchen",
                location = "Unit 83, Upper Market Square of Charter Walk Shopping Centre",
                description = "Food Bank service providing emergency food support to the community.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food", "Community support", "Food Bank"),
                contact = ContactInfo(phone = "01282 686402", email = "contact@burnleytogether.org.uk"),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - 09:00-16:00",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Spacious Places Delivery",
                groupName = "Spacious Places Delivery",
                location = "Briercliffe Shopping Centre, Briercliffe Road, Burnley BB10 1WB",
                description = "Food Bank service that can do deliveries to the community.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food", "Delivery service", "Community support"),
                contact = ContactInfo(phone = "01282 222030", email = "food@spaciousplace.co.uk"),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Maundy Relief",
                groupName = "Maundy Relief",
                location = "29-31 Abbey Street, Accrington, BB5 1EN",
                description = "Food Bank that supplies food parcels for residents of Accrington. Also provide a community lunch which is a hot meal served 12pm – 1pm Monday to Saturday.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL, ServiceType.SOCIAL),
                features = listOf("Emergency food", "Community lunch", "Hot meals", "Accrington residents", "Food parcels"),
                contact = ContactInfo(phone = "01254 232328", email = ""),
                schedule = "Monday-Friday (Daily) - 08:00-16:00, Community lunch 12pm-1pm Monday to Saturday",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Burnley & Pendle Food Bank Group",
                groupName = "Nelson Community Mosque Food Bank",
                location = "Burnley, Pendle, Nelson",
                description = "Food Bank service providing emergency food support to the community.",
                types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
                features = listOf("Emergency food", "Community support", "Food Bank"),
                contact = ContactInfo(phone = "07873282580", email = ""),
                schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - 09:00-17:00",
                status = ServiceStatus.PENDING
            )
        )
    }

    // Add a function to sync all hardcoded services with Firebase
    fun syncAllServicesWithFirebase() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("SearchViewModel", "Starting to sync all hardcoded services with Firebase")
                
                // First, get all existing services from Firebase
                val existingServices = mutableListOf<Service>()
                serviceRepository.getAllServices().collect { services ->
                    existingServices.clear()
                    existingServices.addAll(services)
                }
                
                Log.d("SearchViewModel", "Found ${existingServices.size} existing services in Firebase")
                
                // Get all hardcoded services
                val hardcodedServices = getAllHardcodedServices()
                Log.d("SearchViewModel", "Found ${hardcodedServices.size} hardcoded services")
                
                // Check which hardcoded services are missing from Firebase
                val missingServices = hardcodedServices.filter { hardcodedService ->
                    !existingServices.any { existingService ->
                        existingService.organizationName.equals(hardcodedService.organizationName, ignoreCase = true) &&
                        existingService.groupName.equals(hardcodedService.groupName, ignoreCase = true) &&
                        existingService.location.equals(hardcodedService.location, ignoreCase = true)
                    }
                }
                
                Log.d("SearchViewModel", "Found ${missingServices.size} services missing from Firebase")
                
                // Add missing services to Firebase
                missingServices.forEach { service ->
                    try {
                        Log.d("SearchViewModel", "Adding missing service: ${service.organizationName} - ${service.groupName}")
                        val result = repository.addService(service)
                        result.onSuccess { id ->
                            Log.d("SearchViewModel", "Successfully added missing service with ID: $id")
                        }.onFailure { e ->
                            Log.e("SearchViewModel", "Failed to add missing service", e)
                        }
                    } catch (e: Exception) {
                        Log.e("SearchViewModel", "Error adding missing service", e)
                    }
                }
                
                // Add Wednesday, Thursday, and Friday services (these use addNewService which handles duplicates)
                Log.d("SearchViewModel", "Adding Wednesday services")
                addWednesdayServices()
                
                Log.d("SearchViewModel", "Adding Thursday services")
                addThursdayServices()
                
                Log.d("SearchViewModel", "Adding Friday services")
                addFridayServices()
                
                // ServiceManager will automatically update via Firestore listener
                Log.d("SearchViewModel", "Finished syncing all services with Firebase")
                
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error syncing services with Firebase", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add a function to completely replace Firebase with hardcoded services
    fun replaceFirebaseWithHardcodedServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("SearchViewModel", "Starting complete Firebase replacement with hardcoded services")
                
                // Step 1: Clear existing Firebase documents
                Log.d("SearchViewModel", "Step 1: Clearing existing Firebase documents")
                val clearResult = serviceRepository.clearAllServices()
                
                clearResult.onSuccess { deletedCount ->
                    Log.d("SearchViewModel", "Successfully cleared $deletedCount existing documents")
                    
                    // Step 2: Add all hardcoded services with descriptive IDs
                    Log.d("SearchViewModel", "Step 2: Adding all hardcoded services")
                    
                    val allHardcodedServices = getAllHardcodedServices()
                    Log.d("SearchViewModel", "Found ${allHardcodedServices.size} hardcoded services to add")
                    
                    // Debug: List all services that will be added
                    allHardcodedServices.forEachIndexed { index, service ->
                        Log.d("SearchViewModel", "Service ${index + 1}: ${service.organizationName} - ${service.groupName}")
                    }
                    
                    var successCount = 0
                    var failureCount = 0
                    
                    allHardcodedServices.forEach { service ->
                        try {
                            Log.d("SearchViewModel", "Adding service: ${service.organizationName} - ${service.groupName}")
                            val result = repository.addService(service)
                            result.onSuccess { id ->
                                successCount++
                                Log.d("SearchViewModel", "Successfully added service with ID: $id (Success count: $successCount)")
                            }.onFailure { e ->
                                failureCount++
                                Log.e("SearchViewModel", "Failed to add service: ${e.message} (Failure count: $failureCount)")
                            }
                        } catch (e: Exception) {
                            failureCount++
                            Log.e("SearchViewModel", "Error adding service: ${e.message} (Failure count: $failureCount)")
                        }
                    }
                    
                    Log.d("SearchViewModel", "Final results: $successCount successful, $failureCount failed")
                    
                    // ServiceManager will automatically update via Firestore listener
                    Log.d("SearchViewModel", "Complete Firebase replacement finished successfully")
                    
                }.onFailure { e ->
                    Log.e("SearchViewModel", "Failed to clear Firebase", e)
                    _error.value = e
                }
                
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error in complete Firebase replacement", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun addService(service: Service) {
        try {
            Log.d("SearchViewModel", "Adding new service: ${service.organizationName} - ${service.groupName}")
            val result = serviceRepository.addService(service)
            
            result.onSuccess { serviceId ->
                Log.d("SearchViewModel", "Successfully added service with ID: $serviceId")
            }.onFailure { e ->
                Log.e("SearchViewModel", "Failed to add service", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error adding service", e)
            throw e
        }
    }

    suspend fun deleteService(serviceId: String) {
        repository.deleteService(serviceId)
        // ServiceManager will automatically update via Firestore listener
    }

    // Add all Pendle YES Hub services
    fun addPendleYesHubServices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("SearchViewModel", "Adding Pendle YES Hub services")
                
                // 1. Don't Fret: Guitar Lessons (Advanced)
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "Don't Fret: Guitar Lessons (Advanced)",
                    location = "Pendle YES Hub, Nelson",
                    description = "Advanced guitar lessons with Aaron",
                    types = listOf(ServiceType.SOCIAL),
                    features = listOf("Guitar lessons", "Advanced level", "Music"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Monday 13:00-14:00 (Weekly)"
                )

                // 2. National Careers Service Employment Support
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "National Careers Service Employment Support",
                    location = "Pendle YES Hub, Nelson",
                    description = "Employment support with the National Careers Service",
                    types = listOf(ServiceType.EMPLOYMENT),
                    features = listOf("Employment support", "Career guidance", "Job search"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Tuesday 09:00-16:00 (Weekly)"
                )

                // 3. Pickleball, Badminton and Football
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "Pickleball, Badminton and Football",
                    location = "Leisure Box, BB9 5NH, Nelson",
                    description = "Group sports sessions including pickleball, badminton and football",
                    types = listOf(ServiceType.SPORT_AND_FITNESS),
                    features = listOf("Pickleball", "Badminton", "Football", "Group sports"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Tuesday 16:00-17:00 (Weekly)"
                )

                // 4. Walking Wednesdays
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "Walking Wednesdays",
                    location = "Pendle YES Hub, Nelson",
                    description = "Local walks for health and socialising",
                    types = listOf(ServiceType.SPORT_AND_FITNESS),
                    features = listOf("Walking", "Health", "Socialising", "Outdoor activity"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Wednesday 13:00-14:00 (Weekly)"
                )

                // 5. 1-1 Mental Health Wellbeing Support (Kieran and Sarah)
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "1-1 Mental Health Wellbeing Support (Kieran and Sarah)",
                    location = "Pendle YES Hub, Nelson",
                    description = "One-to-one mental health wellbeing support",
                    types = listOf(ServiceType.MENTAL_HEALTH),
                    features = listOf("One-to-one support", "Mental health", "Wellbeing", "Individual sessions"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Wednesday 12:00-15:00 (Weekly)"
                )

                // 6. Snooker and Pool
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "Snooker and Pool",
                    location = "Alexandra Snooker Club, 5 Holme Street, Nelson",
                    description = "Snooker and pool session",
                    types = listOf(ServiceType.SOCIAL),
                    features = listOf("Snooker", "Pool", "Social activity", "Indoor games"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Friday 12:00-13:00 (Weekly)"
                )

                // 7. Burnley College Employment & Courses Support
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "Burnley College Employment & Courses Support",
                    location = "Pendle YES Hub, Nelson",
                    description = "Employment and course support from Burnley College",
                    types = listOf(ServiceType.EMPLOYMENT, ServiceType.EDUCATION),
                    features = listOf("Employment support", "Course support", "Education", "Training"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Thursday 13:30-16:00 (Weekly)"
                )

                // 8. Gym Session
                addNewService(
                    organizationName = "Pendle YES Hub",
                    groupName = "Gym Session",
                    location = "Pendle Wavelengths, BB9 9TD, Nelson",
                    description = "Group gym session",
                    types = listOf(ServiceType.SPORT_AND_FITNESS),
                    features = listOf("Gym", "Fitness", "Group workout", "Exercise"),
                    contactPhone = "07859739635",
                    contactEmail = "DMarshall@activelancashire.org.uk",
                    schedule = "Thursday 14:00-15:00 (Weekly)"
                )

                Log.d("SearchViewModel", "Successfully added all Pendle YES Hub services")
                
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error adding Pendle YES Hub services", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }
} 