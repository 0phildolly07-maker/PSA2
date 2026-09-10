package com.philapp.psa2.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.ui.components.CategoryIconWell
import com.philapp.psa2.ui.components.OfflineIndicatorBanner
import com.philapp.psa2.ui.components.icon
import com.philapp.psa2.ui.components.psaInnerTopAppBarColors
import com.philapp.psa2.ui.components.rememberNetworkConnectivity
import com.philapp.psa2.utils.LocationFilter
import com.philapp.psa2.utils.ServiceSearch
import com.philapp.psa2.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            customSearch != null -> {
                                when (location) {
                                    LocationFilter.ANYWHERE -> "Results for '$customSearch'"
                                    LocationFilter.NEARBY -> "Results for '$customSearch'"
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
                },
                colors = psaInnerTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineIndicatorBanner(connectionStatus = connectionStatus)

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "All Locations",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
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
                        services.isEmpty() && !isLoading -> {
                            val suggestion = customSearch?.let { ServiceSearch.suggestedAlternative(it) }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Outlined.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No services found for this selection.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!suggestion.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Try searching for '$suggestion' instead.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    val context = LocalContext.current
    val typeIcon = service.types.firstOrNull()?.icon() ?: Icons.Default.LocationOn

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${service.groupName} by ${service.organizationName}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { navController.navigate("details/${service.id}") }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryIconWell(
                    icon = typeIcon,
                    containerColor = MaterialTheme.colorScheme.primary,
                    iconTint = MaterialTheme.colorScheme.onPrimary,
                    size = 48.dp,
                    iconSize = 24.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.groupName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!service.schedule.isNullOrBlank()) {
                MetaRow(icon = Icons.Default.Schedule, text = service.schedule)
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (service.location.isNotBlank()) {
                MetaRow(icon = Icons.Default.LocationOn, text = service.location)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = buildList {
                    addAll(service.types.take(2).map { it.getDisplayName() })
                    if (size < 2) {
                        addAll(service.features.take(2 - size))
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chips.forEach { label ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                Button(
                    onClick = { navController.navigate("details/${service.id}") },
                    modifier = Modifier.semantics {
                        contentDescription = "More information about ${service.groupName}"
                    }
                ) {
                    Text("More info")
                }
            }

            service.websiteUrl?.takeIf { it.isNotBlank() }?.let { url ->
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                ) {
                    Text("Website")
                }
            }
        }
    }
}

@Composable
private fun MetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
