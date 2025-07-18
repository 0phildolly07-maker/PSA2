package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import com.philapp.psa2.model.AreaList

data class ScheduleEntry(var day: String, var startTime: String, var endTime: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitServiceScreen(navController: NavController) {
    var serviceName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var serviceDescription by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var locationDetails by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var organisationName by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var features by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("weekly") }
    var serviceType by remember { mutableStateOf("Mental health") }
    var status by remember { mutableStateOf("Pending") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val serviceTypeOptions = listOf("Mental Health", "Social", "Sport & Fitness", "Peer Support")
    val statusOptions = listOf("Pending", "Approved", "Rejected")
    val frequencyOptions = listOf("weekly", "fortnightly", "monthly")
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val timeOptions = listOf("9am", "10am", "11am", "12pm", "1pm", "2pm", "3pm", "4pm", "5pm")

    var scheduleEntries by remember { mutableStateOf(listOf<ScheduleEntry>()) }

    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Submit a New Service",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = serviceName,
            onValueChange = { serviceName = it },
            label = { Text("Service Name (Group/Program Name)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group/Program Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = groupDescription,
            onValueChange = { groupDescription = it },
            label = { Text("Group Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = serviceDescription,
            onValueChange = { serviceDescription = it },
            label = { Text("Service Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = locationDetails,
            onValueChange = { locationDetails = it },
            label = { Text("Location Details") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Replace the town text field with a dropdown
        var townExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = townExpanded,
            onExpandedChange = { townExpanded = !townExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = town,
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
                            town = area
                            townExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = organisationName,
            onValueChange = { organisationName = it },
            label = { Text("Organisation Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = contactInfo,
            onValueChange = { contactInfo = it },
            label = { Text("Contact Information") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // --- Schedule Section ---
        Text("Session Times (add one or more)", style = MaterialTheme.typography.titleMedium)
        scheduleEntries.forEachIndexed { idx, entry ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Day dropdown
                var dayExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = !dayExpanded }
                ) {
                    OutlinedTextField(
                        value = entry.day,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        daysOfWeek.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    scheduleEntries = scheduleEntries.toMutableList().also { it[idx] = it[idx].copy(day = day) }
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }
                // Start time dropdown
                var startExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = startExpanded,
                    onExpandedChange = { startExpanded = !startExpanded }
                ) {
                    OutlinedTextField(
                        value = entry.startTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = startExpanded,
                        onDismissRequest = { startExpanded = false }
                    ) {
                        timeOptions.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    scheduleEntries = scheduleEntries.toMutableList().also { it[idx] = it[idx].copy(startTime = t) }
                                    startExpanded = false
                                }
                            )
                        }
                    }
                }
                // End time dropdown
                var endExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = endExpanded,
                    onExpandedChange = { endExpanded = !endExpanded }
                ) {
                    OutlinedTextField(
                        value = entry.endTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End") },
                        modifier = Modifier.weight(1f),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = endExpanded,
                        onDismissRequest = { endExpanded = false }
                    ) {
                        timeOptions.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    scheduleEntries = scheduleEntries.toMutableList().also { it[idx] = it[idx].copy(endTime = t) }
                                    endExpanded = false
                                }
                            )
                        }
                    }
                }
                // Remove button
                IconButton(onClick = {
                    scheduleEntries = scheduleEntries.toMutableList().also { it.removeAt(idx) }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Button(onClick = {
            scheduleEntries = scheduleEntries + ScheduleEntry("", timeOptions[0], timeOptions[1])
        }) {
            Text("Add Day/Time")
        }
        Spacer(modifier = Modifier.height(8.dp))
        // --- End Schedule Section ---
        OutlinedTextField(
            value = features,
            onValueChange = { features = it },
            label = { Text("Features (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Frequency dropdown
        var freqExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = freqExpanded,
            onExpandedChange = { freqExpanded = !freqExpanded }
        ) {
            OutlinedTextField(
                value = frequency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Frequency") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) }
            )
            ExposedDropdownMenu(
                expanded = freqExpanded,
                onDismissRequest = { freqExpanded = false }
            ) {
                frequencyOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            frequency = option
                            freqExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Service Type dropdown
        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = !typeExpanded }
        ) {
            OutlinedTextField(
                value = serviceType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Service Type") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) }
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                serviceTypeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            serviceType = option
                            typeExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Status dropdown
        var statusExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = !statusExpanded }
        ) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                label = { Text("Status") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) }
            )
            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                statusOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            status = option
                            statusExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (successMessage.isNotEmpty()) {
            Text(successMessage, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (serviceName.isBlank() || groupName.isBlank() || groupDescription.isBlank() || serviceDescription.isBlank() ||
                    location.isBlank() || locationDetails.isBlank() || town.isBlank() || organisationName.isBlank() ||
                    contactInfo.isBlank() || scheduleEntries.isEmpty() || scheduleEntries.any { it.day.isBlank() || it.startTime.isBlank() || it.endTime.isBlank() } || features.isBlank() ||
                    frequency.isBlank() || serviceType.isBlank() || status.isBlank()
                ) {
                    errorMessage = "Error, please fill in all the fields"
                    successMessage = ""
                } else {
                    errorMessage = ""
                    isSubmitting = true
                    val serviceMap = mutableMapOf<String, Any>()
                    serviceMap["Service Name (Group/Program Name)"] = serviceName
                    serviceMap["Group/Program Name"] = groupName
                    serviceMap["Group Description"] = groupDescription
                    serviceMap["Service Description"] = serviceDescription
                    serviceMap["Location"] = location
                    serviceMap["Location Details"] = locationDetails
                    serviceMap["Town"] = town
                    serviceMap["Organisation Name"] = organisationName
                    serviceMap["Contact Information"] = contactInfo
                    serviceMap["Session Times"] = scheduleEntries.joinToString(", ") { "${it.day} ${it.startTime}-${it.endTime}" }
                    serviceMap["Features"] = features
                    serviceMap["Frequency"] = frequency
                    serviceMap["Service Type"] = serviceType
                    serviceMap["Status"] = status
                    // Submit to Firestore
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.collection("services").add(serviceMap)
                                .addOnSuccessListener {
                                    isSubmitting = false
                                    successMessage = "Service submitted successfully!"
                                    errorMessage = ""
                                    // Pop back after a short delay
                                    CoroutineScope(Dispatchers.Main).launch {
                                        kotlinx.coroutines.delay(1000)
                                        navController.popBackStack()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isSubmitting = false
                                    errorMessage = "Failed to submit service: ${e.message}"
                                    successMessage = ""
                                }
                        } catch (e: Exception) {
                            isSubmitting = false
                            errorMessage = "Failed to submit service: ${e.message}"
                            successMessage = ""
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Submit Service")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            Text("Cancel")
        }
    }
} 