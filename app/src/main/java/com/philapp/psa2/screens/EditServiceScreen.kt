package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.philapp.psa2.model.AreaList

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditServiceScreen(
    navController: NavController,
    serviceId: String,
    viewModel: SearchViewModel
) {
    val service = remember(serviceId) {
        viewModel.services.value.find { it.id == serviceId }
    } ?: run {
        LaunchedEffect(Unit) {
            navController.navigateUp()
        }
        return
    }

    // Parse location to separate location details and town
    val locationParts = service.location.split(", ").toMutableList()
    val town = if (locationParts.size > 1) locationParts.removeLast() else ""
    val locationDetails = locationParts.joinToString(", ")

    var organizationName by remember { mutableStateOf(service.organizationName) }
    var groupName by remember { mutableStateOf(service.groupName) }
    var location by remember { mutableStateOf(locationDetails) }
    var selectedTown by remember { mutableStateOf(town) }
    var townExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(service.description) }
    var schedule by remember { mutableStateOf(service.schedule ?: "") }
    var contactPhone by remember { mutableStateOf(service.contact?.phone ?: "") }
    var contactEmail by remember { mutableStateOf(service.contact?.email ?: "") }
    var websiteUrl by remember { mutableStateOf(service.websiteUrl ?: "") }
    
    var selectedTypes by remember { mutableStateOf(service.types.toSet()) }
    var features by remember { mutableStateOf(service.features) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Service") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Text("←")
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
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = organizationName,
                onValueChange = { organizationName = it },
                label = { Text("Organization Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location Details") },
                placeholder = { Text("e.g., St.James Old School Building") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Town dropdown
            ExposedDropdownMenuBox(
                expanded = townExpanded,
                onExpandedChange = { townExpanded = !townExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedTown,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Town") },
                    placeholder = { Text("Select a town") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = townExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = townExpanded,
                    onDismissRequest = { townExpanded = false }
                ) {
                    AreaList.Lancashire_Areas.forEach { area ->
                        DropdownMenuItem(
                            text = { Text(area) },
                            onClick = {
                                selectedTown = area
                                townExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Service Types", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServiceType.values().forEach { type ->
                    FilterChip(
                        selected = type in selectedTypes,
                        onClick = {
                            selectedTypes = if (type in selectedTypes) {
                                selectedTypes - type
                            } else {
                                selectedTypes + type
                            }
                        },
                        label = { Text(type.getDisplayName()) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = schedule,
                onValueChange = { schedule = it },
                label = { Text("Schedule") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Contact Information", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("Website URL") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val fullLocation = if (location.isNotBlank() && selectedTown.isNotBlank()) {
                        "$location, $selectedTown"
                    } else if (location.isNotBlank()) {
                        location
                    } else if (selectedTown.isNotBlank()) {
                        selectedTown
                    } else {
                        ""
                    }
                    
                    viewModel.updateService(
                        serviceId = service.id,
                        organizationName = organizationName,
                        groupName = groupName,
                        location = fullLocation,
                        description = description,
                        types = selectedTypes.toList(),
                        features = features,
                        contactPhone = contactPhone.takeIf { it.isNotBlank() },
                        contactEmail = contactEmail.takeIf { it.isNotBlank() },
                        schedule = schedule.takeIf { it.isNotBlank() },
                        websiteUrl = websiteUrl.takeIf { it.isNotBlank() }
                    )
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Service", color = MaterialTheme.colorScheme.onError)
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Service") },
                    text = { Text("Are you sure you want to delete this service? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                coroutineScope.launch {
                                    viewModel.deleteService(service.id)
                                    navController.navigateUp()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.onError)
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
} 