package com.philapp.psa2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.ui.components.ExternalActivitiesList
import com.philapp.psa2.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SearchViewModel,
    onServiceClick: (Service) -> Unit,
    onAddServiceClick: () -> Unit
) {
    var showExternalSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<ServiceType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Looking for?") },
                actions = {
                    IconButton(
                        onClick = { 
                            viewModel.addAllServices()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Add All Services")
                    }
                    IconButton(onClick = { onAddServiceClick() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Service")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Services loaded: ${viewModel.services.value.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue -> 
                        searchQuery = newValue.trim()
                        if (showExternalSearch) {
                            viewModel.searchExternalActivities(searchQuery, selectedType?.name)
                        } else {
                            viewModel.searchServices(searchQuery)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for activities...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showExternalSearch,
                        onClick = { showExternalSearch = false },
                        label = { Text("Local Services") }
                    )
                    FilterChip(
                        selected = showExternalSearch,
                        onClick = { showExternalSearch = true },
                        label = { Text("Find More") }
                    )
                }
            }

            if (showExternalSearch) {
                item {
                    ExternalActivitiesList(
                        activities = viewModel.externalActivities.value,
                        searchState = viewModel.searchState.value,
                        onAddActivity = { activity ->
                            viewModel.addExternalActivityToServices(activity)
                            showExternalSearch = false
                        }
                    )
                }
            } else {
                items(ServiceType.values()) { type ->
                    ServiceTypeCard(
                        type = type,
                        selected = selectedType == type,
                        onClick = { selectedType = type }
                    )
                }

                item {
                    ServiceTypeCard(
                        type = null,
                        selected = selectedType == null,
                        onClick = { selectedType = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceTypeCard(
    type: ServiceType?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (type) {
                    ServiceType.SOCIAL -> Icons.Default.People
                    ServiceType.MENTAL_HEALTH -> Icons.Default.Psychology
                    ServiceType.PEER_SUPPORT -> Icons.Default.Group
                    ServiceType.SPORT -> Icons.Default.SportsHandball
                    ServiceType.RECOVERY -> Icons.Default.Refresh
                    ServiceType.SKILL_BUILDING -> Icons.Default.School
                    ServiceType.EDUCATION -> Icons.Default.School
                    ServiceType.FOOD_BANKS -> Icons.Default.Restaurant
                    null -> Icons.Default.AllInclusive
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = type?.name?.replace("_", " ") ?: "All Services",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
} 