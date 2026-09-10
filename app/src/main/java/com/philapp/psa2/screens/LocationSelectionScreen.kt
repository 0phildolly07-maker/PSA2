package com.philapp.psa2.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philapp.psa2.ui.components.psaInnerTopAppBarColors
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.philapp.psa2.model.AreaList
import com.philapp.psa2.utils.LocationFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    navController: NavController
) {
    var customSearch by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var locationExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Custom Search") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = customSearch,
                onValueChange = { customSearch = it },
                label = { Text("What are you looking for?") },
                placeholder = { Text("e.g., walking, art therapy, support groups") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search query" },
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = !locationExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = LocationFilter.displayLabel(selectedLocation),
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
                        .semantics { contentDescription = "Select location" }
                )

                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Anywhere") },
                        onClick = {
                            selectedLocation = LocationFilter.ANYWHERE
                            locationExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nearby") },
                        onClick = {
                            selectedLocation = LocationFilter.NEARBY
                            locationExpanded = false
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    )
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

            Text(
                text = "Struggling to find what you're looking for? You might find help through these instead:",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = { openExternalUrl(context, "https://hubofhope.co.uk/") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Open Hub of Hope website" }
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hub of Hope")
            }

            OutlinedButton(
                onClick = { openExternalUrl(context, "https://servicefinder.lancashire.gov.uk/") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Open Lancashire Service Finder website" }
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lancashire Service Finder")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (customSearch.isNotBlank() && selectedLocation.isNotBlank()) {
                        navController.navigate("results/custom/${customSearch.trim()}/${selectedLocation}")
                    }
                },
                enabled = customSearch.isNotBlank() && selectedLocation.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search")
            }
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
