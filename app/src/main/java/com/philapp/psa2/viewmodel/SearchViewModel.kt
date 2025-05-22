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
                Log.d("SearchViewModel", "Loading approved services")
                serviceRepository.getApprovedServices().collect { approvedServices ->
                    Log.d("SearchViewModel", "Received ${approvedServices.size} approved services")
                    _services.value = approvedServices
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading services", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
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
                serviceRepository.getApprovedServices().collect { allServices ->
                    val filteredServices = allServices.filter { service ->
                        val matchesQuery = query.isEmpty() || 
                            service.organizationName.contains(query, ignoreCase = true) ||
                            service.groupName.contains(query, ignoreCase = true) ||
                            service.description.contains(query, ignoreCase = true)
                        
                        val matchesType = type == null || service.types.contains(type)
                        
                        val matchesLocation = location.isNullOrEmpty() ||
                            service.location.contains(location, ignoreCase = true)
                        
                        matchesQuery && matchesType && matchesLocation
                    }
                    
                    Log.d("SearchViewModel", "Found ${filteredServices.size} matching services")
                    _services.value = filteredServices
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching services", e)
                _error.value = e
            } finally {
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
                repository.addService(newService)
                loadServices() // Reload services after adding
            } catch (e: Exception) {
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
        schedule: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Find the existing service
                val existingService = _services.value.find { it.id == serviceId } ?: return@launch
                
                // Create updated service with new values or existing values if not provided
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
                    schedule = schedule ?: existingService.schedule
                )

                // Update in repository
                repository.updateService(updatedService)
                
                // Update in local list
                _services.value = _services.value.map { 
                    if (it.id == serviceId) updatedService else it 
                }
            } catch (e: Exception) {
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
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Mindful Movement",
                location = "St.James Old School Building, Accrington",
                description = "Calm your mind with relaxing movements and light exercise, recovery based.",
                types = listOf(ServiceType.SPORT, ServiceType.RECOVERY),
                features = listOf("Mindfulness", "Exercise", "Recovery Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Monday 14:30-15:30 (Weekly)",
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Lunch, Brunch, Walk and Talk",
                location = "ABD Centre, Burnley Road, Bacup",
                description = "Come along for breakfast and a lovely walk through the countryside. Pick-ups can be arranged from fixed locations.",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT),
                features = listOf("Breakfast", "Walking", "Transport available", "Social"),
                contact = ContactInfo(
                    phone = "Shaun - 07351614902",
                    email = ""
                ),
                schedule = "Tuesday 11:00-14:00 (Weekly)",
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Feel Good Fitness",
                location = "St.James Old School Building, Accrington",
                description = "Light exercise suitable for all abilities",
                types = listOf(ServiceType.SPORT, ServiceType.RECOVERY, ServiceType.SOCIAL),
                features = listOf("Exercise", "All abilities", "Recovery Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Tuesday 14:00-15:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Fishing for Mental Health",
                location = "Burnley Inspire East Lancashire, Westgate, BB111RY",
                description = "Fishing Group, Recovery Based, Pick-ups from fixed locations. Bookings must be made with Shaun or Gareth.",
                types = listOf(ServiceType.SPORT, ServiceType.RECOVERY, ServiceType.MENTAL_HEALTH, ServiceType.SKILL_BUILDING),
                features = listOf("Fishing", "Transport available", "Booking required", "Recovery Support"),
                contact = ContactInfo(
                    phone = "Shaun - 07351614902, Gareth - 07351614926",
                    email = ""
                ),
                schedule = "Wednesday 09:00-14:00 (Weekly)",
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "No Excuse Boxing Workout",
                location = "87 Blackburn Road, Accrington",
                description = "Improve your fitness in a friendly environment with other people in recovery from drugs/alcohol/mental health issues.",
                types = listOf(ServiceType.SPORT, ServiceType.RECOVERY),
                features = listOf("Boxing", "Fitness", "Recovery Support", "Group Workout"),
                contact = ContactInfo(
                    phone = "Gemma - 07483915707",
                    email = ""
                ),
                schedule = "Monday 11:45-13:00 (Weekly)",
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Red Rose Recovery",
                groupName = "Get Crafty",
                location = "St.James Old School Building, Accrington",
                description = "Arts And Crafts",
                types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
                features = listOf("Arts and Crafts", "Social", "Peer Support"),
                contact = ContactInfo(
                    phone = "Bridget - 07483356858",
                    email = ""
                ),
                schedule = "Monday 10:00-11:45 (Weekly)",
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
            ),
            Service(
                id = UUID.randomUUID().toString(),
                organizationName = "Inspire",
                groupName = "Walking group Bacup",
                location = "A B & D Centre, Burnley Road, Bacup, OL13 8AB",
                description = "Walking group with friendly folk. All abilities welcome!",
                types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL),
                features = listOf("Walking", "Exercise", "Social", "Inclusive"),
                contact = ContactInfo(phone = "", email = ""),
                schedule = "Tuesday 12:00-14:00",
                status = ServiceStatus.PENDING
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
                status = ServiceStatus.PENDING
            )
            // Add more sample services as needed
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
                    types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.SPORT, ServiceType.PEER_SUPPORT),
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
                    types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.RECOVERY),
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
                    types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT, ServiceType.MENTAL_HEALTH),
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
                    types = listOf(ServiceType.SOCIAL, ServiceType.SKILL_BUILDING),
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
                    types = listOf(ServiceType.SOCIAL, ServiceType.SKILL_BUILDING, ServiceType.SPORT, ServiceType.PEER_SUPPORT),
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
                    types = listOf(ServiceType.SOCIAL, ServiceType.SKILL_BUILDING),
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
} 