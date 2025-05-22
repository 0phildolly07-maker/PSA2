package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.viewmodel.SearchViewModel
import com.philapp.psa2.model.ServiceType
import kotlinx.coroutines.launch
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.model.ServiceStatus
import androidx.compose.ui.layout.Layout

data class LocationDetails(
    val addressLine1: String = "",
    val addressLine2: String = "",
    val town: String = "",
    val postcode: String = ""
)

enum class ServiceDay(val shortName: String) {
    MONDAY("Mon"),
    TUESDAY("Tues"),
    WEDNESDAY("Wed"),
    THURSDAY("Thurs"),
    FRIDAY("Fri"),
    SATURDAY("Sat"),
    SUNDAY("Sun")
}

enum class Frequency(val displayName: String) {
    WEEKLY("Weekly"),
    BIWEEKLY("Every Two Weeks"),
    MONTHLY("Monthly"),
    DAILY("Daily"),
    VARIABLE("Variable/Flexible")
}

data class ScheduleDetails(
    val startTime: String = "",
    val endTime: String = "",
    val days: List<ServiceDay> = emptyList(),
    val frequency: Frequency = Frequency.WEEKLY,
    val additionalInfo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceScreen(
    navController: NavController,
    searchViewModel: SearchViewModel
) {
    var organizationName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var locationDetails by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var sessionTimes by remember { mutableStateOf("") }
    var daysAvailable by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var selectedTypes by remember { mutableStateOf(setOf<ServiceType>()) }
    var features by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Text("←")
            }
            Text(
                "Add New Service",
                style = MaterialTheme.typography.headlineMedium
            )
            Box(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Organization Information
        SectionHeader("Organization Information", true)
        OutlinedTextField(
            value = organizationName,
            onValueChange = { organizationName = it },
            label = { Text("Organization Name") },
            placeholder = { Text("e.g., Red Rose Recovery") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Group Information
        SectionHeader("Group Information", true)
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group/Program Name") },
            placeholder = { Text("e.g., Get Crafty, No Excuse Boxing") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Service Description") },
            placeholder = { Text("Describe the service and its benefits") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Location Section
        SectionHeader("Location Details", true)
        OutlinedTextField(
            value = locationDetails,
            onValueChange = { locationDetails = it },
            label = { Text("Location Details") },
            placeholder = { Text("e.g., St.James Old School Building") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = town,
            onValueChange = { town = it },
            label = { Text("Town") },
            placeholder = { Text("e.g., Accrington") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Service Type Section
        SectionHeader("Service Type", true)
        FlowRow {
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
                    label = { Text(type.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contact Information
        SectionHeader("Contact Information", true)
        OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("Contact Name") },
            placeholder = { Text("e.g., Bridget") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone") },
            placeholder = { Text("e.g., 07483356858") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule Section
        SectionHeader("Schedule", true)
        OutlinedTextField(
            value = sessionTimes,
            onValueChange = { sessionTimes = it },
            label = { Text("Session Times") },
            placeholder = { Text("e.g., 10:00 - 11:45") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = daysAvailable,
            onValueChange = { daysAvailable = it },
            label = { Text("Days Available") },
            placeholder = { Text("e.g., Monday") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = { Text("Frequency") },
            placeholder = { Text("e.g., Weekly") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                scope.launch {
                    // Only validate required fields
                    if (organizationName.isBlank() || groupName.isBlank() || selectedTypes.isEmpty()) {
                        errorMessage = "Please fill in all required fields (Organization Name, Group Name, and at least one Service Type)"
                        return@launch
                    }

                    isSubmitting = true
                    errorMessage = null

                    val fullLocation = if (locationDetails.isNotBlank() && town.isNotBlank()) {
                        "$locationDetails, $town"
                    } else if (locationDetails.isNotBlank()) {
                        locationDetails
                    } else if (town.isNotBlank()) {
                        town
                    } else {
                        ""
                    }

                    val contactInfo = if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                        "$contactName - $contactPhone"
                    } else if (contactPhone.isNotBlank()) {
                        contactPhone
                    } else if (contactName.isNotBlank()) {
                        contactName
                    } else {
                        ""
                    }

                    val schedule = if (daysAvailable.isNotBlank() && sessionTimes.isNotBlank() && frequency.isNotBlank()) {
                        "$daysAvailable $sessionTimes ($frequency)"
                    } else if (sessionTimes.isNotBlank()) {
                        sessionTimes
                    } else if (daysAvailable.isNotBlank()) {
                        daysAvailable
                    } else {
                        ""
                    }

                    val newService = Service(
                        id = "", // Will be set by Firestore
                        organizationName = organizationName,
                        groupName = groupName,
                        location = fullLocation,
                        description = description,
                        types = selectedTypes.toList(),
                        features = features,
                        contact = if (contactInfo.isNotBlank()) {
                            ContactInfo(
                                phone = contactInfo,
                                email = ""
                            )
                        } else null,
                        schedule = if (schedule.isNotBlank()) schedule else null,
                        status = ServiceStatus.PENDING
                    )

                    try {
                        searchViewModel.addService(newService)
                        navController.popBackStack()
                    } catch (e: Exception) {
                        errorMessage = "Failed to add service: ${e.message}"
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            Text(if (isSubmitting) "Adding Service..." else "Add Service")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String, isRequired: Boolean = false) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
        if (isRequired) {
            Text(
                text = " *",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<List<Int>>()
        val itemConstraints = constraints.copy(minWidth = 0)
        
        val placeables = measurables.map { measurable ->
            measurable.measure(itemConstraints)
        }

        var currentRow = mutableListOf<Int>()
        var currentRowWidth = 0

        placeables.forEachIndexed { index, placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(index)
            currentRowWidth += placeable.width
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row ->
            row.maxOf { placeables[it].height }
        }

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOf { placeables[it].height }
                row.forEach { index ->
                    placeables[index].place(x, y)
                    x += placeables[index].width
                }
                y += rowHeight
            }
        }
    }
} 