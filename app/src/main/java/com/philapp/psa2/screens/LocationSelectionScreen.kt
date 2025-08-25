package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.AreaList
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    navController: NavController
) {
    var customSearch by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var locationExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Search") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Custom Search Input
            OutlinedTextField(
                value = customSearch,
                onValueChange = { customSearch = it },
                label = { Text("What are you looking for?") },
                placeholder = { Text("e.g., mindfulness, art therapy, support groups") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Location Selection
            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = !locationExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedLocation,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select location") },
                    placeholder = { Text("Choose a location or search everywhere") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false }
                ) {
                    // Add "Anywhere" option first
                    DropdownMenuItem(
                        text = { Text("Anywhere") },
                        onClick = {
                            selectedLocation = "all"
                            locationExpanded = false
                        }
                    )
                    
                    // Add all Lancashire areas
                    AreaList.Lancashire_Areas.forEach { area ->
                        DropdownMenuItem(
                            text = { Text(area) },
                            onClick = {
                                selectedLocation = area
                                locationExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Search Button
            Button(
                onClick = {
                    if (customSearch.isNotBlank() && selectedLocation.isNotBlank()) {
                        navController.navigate("results/custom/${customSearch.trim()}/${selectedLocation}")
                    }
                },
                enabled = customSearch.isNotBlank() && selectedLocation.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search")
            }
        }
    }
}
