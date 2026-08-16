package com.philapp.psa2.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    navController: NavController,
    serviceId: String,
    searchViewModel: SearchViewModel
) {
    val services by searchViewModel.services.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val service = services.find { it.id == serviceId }
    val context = LocalContext.current
    val websiteUrl = service?.websiteUrl?.takeIf { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(service?.groupName ?: "Service details") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && service == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            service?.let {
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
                if (service.types.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(service.types) { type ->
                            AssistChip(onClick = { }, label = { Text(type.getDisplayName()) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!service.schedule.isNullOrBlank()) {
                    Text(
                        text = service.schedule,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = service.location,
                    style = MaterialTheme.typography.titleMedium
                )
                if (service.town.isNotBlank()) {
                    Text(
                        text = service.town,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (service.features.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(service.features) { feature ->
                            AssistChip(onClick = { }, label = { Text(feature) })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
                        val phoneEntries = contact.phone.split(",").map { it.trim() }.filter { it.isNotBlank() }
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
                                            val cleanedPhone = entry.replace(Regex("[^0-9+]"), "")
                                            context.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanedPhone"))
                                            )
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .semantics { contentDescription = "Call $entry" }
                                    ) {
                                        Icon(Icons.Filled.Phone, contentDescription = "Call")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call")
                                    }
                                }
                            }
                        }
                    }
                    if (contact.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Email ${contact.email}" }
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = "Email")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(contact.email)
                        }
                    }
                }

                if (service.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(service.location)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Open map for ${service.location}" }
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Map")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in Maps")
                    }
                }

                if (!websiteUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)))
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Open website" }
                    ) {
                        Text("Website")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate("edit_service/${service.id}") },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Update Service")
                }
            } ?: run {
                Text("Service not found")
            }
        }
    }
}
