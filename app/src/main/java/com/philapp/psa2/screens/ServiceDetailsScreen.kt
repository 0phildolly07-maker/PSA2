package com.philapp.psa2.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ContactInfo
import com.philapp.psa2.viewmodel.SearchViewModel

// Map of organization names to website URLs
val organizationWebsites = mapOf(
    "Red Rose Recovery" to "https://redroserecovery.org.uk",
    "Inspire" to "https://inspirelancs.org.uk"
    // Add more organizations and their websites here
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    navController: NavController,
    serviceId: String,
    searchViewModel: SearchViewModel
) {
    val service = searchViewModel.services.value.find { it.id == serviceId }
    val context = LocalContext.current

    // Use the service's websiteUrl, or fallback to the organization map
    val websiteUrl = service?.websiteUrl?.takeIf { it.isNotBlank() } ?: organizationWebsites[service?.organizationName]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        IconButton(
            onClick = { navController.navigateUp() }
        ) {
            Text("←")
        }

        service?.let { 
            // Service details
            Text(
                text = service.groupName,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = service.organizationName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = service.schedule ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = service.location,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(service.features) { feature ->
                    AssistChip(
                        onClick = { },
                        label = { Text(feature) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyLarge
            )

            service.contact?.let { contact ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium
                )
                if (contact.phone.isNotBlank()) {
                    // Parse phone numbers - handles both single and multiple numbers
                    val phoneEntries = contact.phone.split(",").map { it.trim() }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        phoneEntries.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phone: $entry",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        // Extract only digits and + sign from the phone number
                                        val cleanedPhone = entry.replace(Regex("[^0-9+]"), "")
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanedPhone"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = "Call"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call")
                                }
                            }
                        }
                    }
                }
                if (contact.email.isNotBlank()) {
                    Text(
                        text = "Email: ${contact.email}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            // Website button or fallback message
            if (!service.websiteUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(service.websiteUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Website")
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Website information not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Add Update Service button
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { navController.navigate("edit_service/${service.id}") }) {
                Text("Update Service")
            }
        } ?: run {
            Text("Service not found")
        }
    }
} 