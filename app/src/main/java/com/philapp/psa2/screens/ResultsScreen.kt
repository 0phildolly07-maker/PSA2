package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.viewmodel.SearchViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.style.TextOverflow
import android.util.Log
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.philapp.psa2.ui.components.OfflineIndicatorBanner
import com.philapp.psa2.ui.components.rememberNetworkConnectivity
import com.philapp.psa2.utils.LocationFilter
import com.philapp.psa2.utils.ServiceSearch

@Composable
fun ResultsScreen(
    navController: NavController,
    type: ServiceType? = null,
    customSearch: String? = null,
    location: String?,
    searchViewModel: SearchViewModel
) {
    var locationText by remember { mutableStateOf(location ?: "") }
    val services by searchViewModel.services.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val error by searchViewModel.error.collectAsState()
    val connectionStatus = rememberNetworkConnectivity().value

    LaunchedEffect(type, customSearch, location) {
        // Handle "all" location parameter - pass null to search all locations
        val searchLocation = if (location == "all") null else location
        searchViewModel.searchServices(
            query = customSearch ?: "",
            type = type,
            location = searchLocation
        )
    }

    Log.d("ResultsScreen", "Current services: ${services.size}")
    Log.d("ResultsScreen", "Loading state: $isLoading")
    Log.d("ResultsScreen", "Error state: $error")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            customSearch != null -> {
                                when (location) {
                                    LocationFilter.ANYWHERE -> "Results for '$customSearch' (All Locations)"
                                    LocationFilter.NEARBY -> "Results for '$customSearch' (Nearby)"
                                    else -> "Results for '$customSearch'"
                                }
                            }
                            else -> type?.getDisplayName() ?: "All Services"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Offline indicator banner
            OfflineIndicatorBanner(connectionStatus = connectionStatus)
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Location search field - only show if not searching all locations
                if (!LocationFilter.isAnywhere(location) && !LocationFilter.isNearby(location)) {
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { newLocation -> 
                            locationText = newLocation
                            searchViewModel.searchServices(
                                query = customSearch ?: "",
                                type = type,
                                location = newLocation
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Show a message that we're searching all locations
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "All Locations",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    LocationFilter.isNearby(location) && !customSearch.isNullOrBlank() ->
                                        "Showing nearest matches for '$customSearch'"
                                    LocationFilter.isNearby(location) -> "Showing nearest services first"
                                    !customSearch.isNullOrBlank() -> "Searching all locations for '$customSearch'"
                                    else -> "Searching all locations"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when {
                    error != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error?.message ?: "An error occurred",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    searchViewModel.searchServices(
                                        query = customSearch ?: "",
                                        type = type,
                                        location = locationText
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                    services.isEmpty() -> {
                        val suggestion = customSearch?.let { ServiceSearch.suggestedAlternative(it) }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No services found for this selection.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (!suggestion.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Try searching for '$suggestion' instead.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = services,
                                key = { it.id }
                            ) { service ->
                                ServiceCard(service, navController)
                            }
                        }
                    }
                }
            }

                // Loading indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceCard(
    service: Service,
    navController: NavController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics {
                contentDescription = "${service.groupName} by ${service.organizationName}"
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = service.groupName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = service.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!service.schedule.isNullOrBlank()) {
                Text(
                    text = service.schedule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = service.location,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (service.types.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    service.types.take(3).forEach { serviceType ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = serviceType.getDisplayName(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                service.features.take(3).forEach { feature ->
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = feature,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                if (service.features.size > 3) {
                    AssistChip(
                        onClick = { },
                        label = { Text("+${service.features.size - 3}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { navController.navigate("details/${service.id}") },
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "More information about ${service.groupName}" }
            ) {
                Text("More Info")
            }

            service.websiteUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Website")
                }
            }
        }
    }
} 