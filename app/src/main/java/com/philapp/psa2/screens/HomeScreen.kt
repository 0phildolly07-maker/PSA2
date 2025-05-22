package com.philapp.psa2.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.philapp.psa2.viewmodel.SearchViewModel
import com.philapp.psa2.model.SearchState
import com.philapp.psa2.model.Service
import androidx.core.content.ContextCompat

enum class SupportType(val title: String, val description: String) {
    PEER_SUPPORT("Peer Support/Groups", "Connect with others who share similar experiences"),
    SOCIAL("Social Activities", "Join community groups and recreational activities"),
    MENTAL_HEALTH("Mental Health Support", "Access counseling and support services"),
    EMPLOYMENT("Employment & Volunteering", "Find work opportunities and volunteer positions"),
    EDUCATION("Education & Training", "Discover courses and skill development programs"),
    PRACTICAL("Practical Support", "Get help with daily needs and resources")
}

private val Lancashire_Areas = listOf(
    "Accrington",
    "Bacup",
    "Blackburn",
    "Burnley",
    "Nelson",
    "Rawtenstall"
).sorted() // Keep the list alphabetically sorted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    searchViewModel: SearchViewModel
) {
    var selectedType by remember { mutableStateOf<SupportType?>(null) }
    var selectedLocation by remember { mutableStateOf("") }
    var showLocationInput by remember { mutableStateOf(false) }
    var customSearch by remember { mutableStateOf("") }
    var isOtherSelected by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val searchState = searchViewModel.searchState.value
    val context = LocalContext.current

    // Handle location permission
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            searchViewModel.getUserLocation()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_service") }
            ) {
                Text("+")
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Support Services") },
                actions = {
                    IconButton(onClick = { navController.navigate("admin") }) {
                        Text("Admin")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = if (showLocationInput) "Where are you looking?" else "What support are you looking for?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (!showLocationInput) {
                // Support Type Selection
                SupportType.values().forEach { type ->
                    SupportTypeCard(
                        type = type,
                        isSelected = selectedType == type && !isOtherSelected,
                        onClick = {
                            selectedType = type
                            isOtherSelected = false
                            showLocationInput = true
                            customSearch = ""
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Other Option Card
                Card(
                    onClick = {
                        isOtherSelected = true
                        selectedType = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOtherSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Other",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search for specific activities or services",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (isOtherSelected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customSearch,
                                onValueChange = { customSearch = it },
                                label = { Text("What are you looking for?") },
                                placeholder = { Text("e.g., Walking, Music, Arts and Crafts") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showLocationInput = true },
                                enabled = customSearch.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            } else {
                // Location Input Screen
                if (isOtherSelected) {
                    Text(
                        text = "Looking for: $customSearch",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    selectedType?.let { type ->
                        Text(
                            text = "Selected: ${type.title}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                LocationSelector(
                    selectedLocation = selectedLocation,
                    onLocationSelected = { location ->
                        selectedLocation = location
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            showLocationInput = false
                            selectedLocation = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            if (isOtherSelected) {
                                navController.navigate("results/custom/$customSearch/$selectedLocation")
                            } else {
                                selectedType?.let { type ->
                                    navController.navigate("results/${type.name}/$selectedLocation")
                                }
                            }
                        },
                        enabled = selectedLocation.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text("Search")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportTypeCard(
    type: SupportType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = type.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSelector(
    selectedLocation: String,
    onLocationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLocation,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select town or area") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Lancashire_Areas.forEach { area ->
                DropdownMenuItem(
                    text = { Text(area) },
                    onClick = {
                        onLocationSelected(area)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSuggestion(
    location: String,
    onClick: (String) -> Unit
) {
    SuggestionChip(
        onClick = { onClick(location) },
        label = { Text(location) },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceCard(
    service: Service,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = service.groupName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = service.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location: ${service.location}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (service.schedule != null) {
                Text(
                    text = "Schedule: ${service.schedule}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                service.features.forEach { feature ->
                    AssistChip(
                        onClick = { },
                        label = { Text(feature) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 