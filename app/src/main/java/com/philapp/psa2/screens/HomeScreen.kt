package com.philapp.psa2.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.philapp.psa2.model.AreaList
import com.philapp.psa2.ui.components.CategoryIconWell
import com.philapp.psa2.ui.components.OfflineIndicatorBanner
import com.philapp.psa2.ui.components.OtherSearchIcon
import com.philapp.psa2.ui.components.cardTitle
import com.philapp.psa2.ui.components.icon
import com.philapp.psa2.ui.components.psaHomeTopAppBarColors
import com.philapp.psa2.ui.components.rememberNetworkConnectivity
import com.philapp.psa2.utils.LocationFilter
import com.philapp.psa2.utils.isConnected
import com.philapp.psa2.viewmodel.SearchViewModel

enum class SupportType(val title: String, val description: String) {
    PEER_SUPPORT("Peer Support/Groups", "Connect with others who share similar experiences"),
    SOCIAL("Social Activities", "Join community groups and recreational activities"),
    MENTAL_HEALTH("Mental Health Support", "Access counseling and support services"),
    RECOVERY("Recovery", "Find recovery-focused groups and support"),
    SPORT_AND_FITNESS("Sport & Fitness", "Join sports activities and fitness programs"),
    SKILL_BUILDING("Skill Building", "Learn practical and creative skills"),
    EMPLOYMENT("Employment & Volunteering", "Find work opportunities and volunteer positions"),
    EDUCATION("Education & Training", "Discover courses and skill development programs"),
    PRACTICAL("Practical Support", "Get help with daily needs and resources"),
    HOUSING("Housing", "Get help with housing, homelessness, and eviction"),
    FOOD_BANKS("Food Banks", "Access emergency food support and essential supplies"),
    COMMUNITY_INTEREST_GROUPS("Community Interest Groups", "Join hobby and interest-based groups");
}

fun SupportType.toServiceType(): String {
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    searchViewModel: SearchViewModel
) {
    var selectedType by remember { mutableStateOf<SupportType?>(null) }
    var selectedLocation by remember { mutableStateOf("") }
    var showLocationInput by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val connectionStatus = rememberNetworkConnectivity().value

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

    val isConnected = connectionStatus.isConnected()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            searchViewModel.getUserLocation()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isConnected) {
                        navController.navigate("add_service")
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add a new service") },
                text = { Text(if (isConnected) "Add service" else "Offline") },
                containerColor = if (isConnected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isConnected) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Support Services",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = psaHomeTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = { navController.navigate("admin") },
                        modifier = Modifier.semantics { contentDescription = "Open admin" }
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Open admin")
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
            OfflineIndicatorBanner(connectionStatus = connectionStatus)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = if (showLocationInput) {
                        "Where are you looking?"
                    } else {
                        "What support are you looking for?"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                )

                if (!showLocationInput) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(SupportType.entries) { type ->
                            SupportTypeCard(
                                type = type,
                                isSelected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    showLocationInput = true
                                }
                            )
                        }

                        item {
                            OtherOptionCard(
                                onClick = { navController.navigate("custom_search") }
                            )
                        }
                    }
                } else {
                    selectedType?.let { type ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryIconWell(icon = type.icon())
                                Text(
                                    text = type.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LocationSelector(
                        selectedLocation = selectedLocation,
                        onLocationSelected = { location ->
                            selectedLocation = location
                            if (location == LocationFilter.NEARBY && !hasLocationPermission) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else if (location == LocationFilter.NEARBY) {
                                searchViewModel.getUserLocation()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showLocationInput = false
                                selectedLocation = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                selectedType?.let { type ->
                                    navController.navigate("results/${type.toServiceType()}/$selectedLocation")
                                }
                            },
                            enabled = selectedLocation.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Search")
                        }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .semantics { contentDescription = type.title },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CategoryIconWell(icon = type.icon())
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = type.cardTitle(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtherOptionCard(
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .semantics { contentDescription = "Other, search for specific activities or services" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CategoryIconWell(icon = OtherSearchIcon)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Other",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
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
            value = LocationFilter.displayLabel(selectedLocation),
            onValueChange = {},
            readOnly = true,
            label = { Text("Select town or area") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = "Select town or area" }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Anywhere") },
                onClick = {
                    onLocationSelected(LocationFilter.ANYWHERE)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Nearby") },
                onClick = {
                    onLocationSelected(LocationFilter.NEARBY)
                    expanded = false
                }
            )
            AreaList.Lancashire_Areas.forEach { area ->
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
