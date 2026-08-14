package com.philapp.psa2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.viewmodel.SearchViewModel
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.*
import com.philapp.psa2.viewmodel.AdminViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavController,
    searchViewModel: SearchViewModel,
    viewModel: AdminViewModel
) {
    val pendingServices by viewModel.pendingServices.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusUpdateState by viewModel.statusUpdateState.collectAsState()
    val migrationState by viewModel.migrationState.collectAsState()
    val firebaseCheckState by viewModel.firebaseCheckState.collectAsState()
    val firebaseServiceCount by viewModel.firebaseServiceCount.collectAsState()
    val duplicateAnalysis by viewModel.duplicateAnalysis.collectAsState()
    val detailedDuplicates by viewModel.detailedDuplicates.collectAsState()
    val duplicateDeletionState by viewModel.duplicateDeletionState.collectAsState()
    
    var showDuplicateSection by remember { mutableStateOf(false) }
    var showDuplicateAnalysis by remember { mutableStateOf(false) }
    var showDetailedDuplicates by remember { mutableStateOf(false) }
    var showActionButtons by remember { mutableStateOf(false) }
    val allServices by searchViewModel.services.collectAsState()
    val duplicateServices by remember(allServices) {
        derivedStateOf {
            val services = allServices
            val grouped = services.groupBy { Triple(it.organizationName, it.groupName, it.location) }
            val duplicates = grouped.values.filter { it.size > 1 }
            duplicates
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPendingServices()
        viewModel.loadFirebaseServiceCount()
        searchViewModel.loadServices()
    }

    // Auto-analyze duplicates when services change
    LaunchedEffect(allServices) {
        if (allServices.isNotEmpty()) {
            viewModel.analyzeDuplicates(allServices)
            viewModel.getDetailedDuplicates(allServices)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showDuplicateConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSingleDuplicateDialog by remember { mutableStateOf<Service?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf<AdminViewModel.DetailedDuplicateGroup?>(null) }
    
    val scope = rememberCoroutineScope()

    // Show migration feedback
    LaunchedEffect(migrationState) {
        migrationState?.onSuccess { count ->
            snackbarHostState.showSnackbar("Successfully migrated $count documents to descriptive IDs")
            viewModel.clearMigrationState()
        }?.onFailure { e ->
            snackbarHostState.showSnackbar("Migration failed: ${e.message}")
            viewModel.clearMigrationState()
        }
    }

    // Show Firebase check feedback
    LaunchedEffect(firebaseCheckState) {
        firebaseCheckState?.onSuccess { message ->
            snackbarHostState.showSnackbar("Firebase Check: $message")
            viewModel.clearFirebaseCheckState()
        }?.onFailure { e ->
            snackbarHostState.showSnackbar("Firebase check failed: ${e.message}")
            viewModel.clearFirebaseCheckState()
        }
    }

    // Show duplicate deletion feedback
    LaunchedEffect(duplicateDeletionState) {
        duplicateDeletionState?.onSuccess { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearDuplicateDeletionState()
        }?.onFailure { e ->
            snackbarHostState.showSnackbar("Failed to delete duplicate: ${e.message}")
            viewModel.clearDuplicateDeletionState()
        }
    }

    if (showDuplicateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateConfirmDialog = false },
            title = { Text("Remove Duplicates") },
            text = { Text("This will remove all duplicate services, keeping only the first occurrence of each. Are you sure you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDuplicateConfirmDialog = false
                        searchViewModel.removeDuplicateServices()
                        scope.launch {
                            snackbarHostState.showSnackbar("Removing duplicate services...")
                        }
                    }
                ) {
                    Text("Yes, Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteSingleDuplicateDialog != null) {
        val service = showDeleteSingleDuplicateDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteSingleDuplicateDialog = null },
            title = { Text("Delete Duplicate Service") },
            text = { 
                Text("Are you sure you want to delete this duplicate service?\n\n" +
                     "Organization: ${service.organizationName}\n" +
                     "Group: ${service.groupName}\n" +
                     "Location: ${service.location}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDuplicateService(service.id)
                        showDeleteSingleDuplicateDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSingleDuplicateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteGroupDialog != null) {
        val group = showDeleteGroupDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            title = { Text("Delete All Duplicates in Group") },
            text = { 
                Text("Are you sure you want to delete all duplicates in this group, keeping only the first service?\n\n" +
                     "Organization: ${group.organizationName}\n" +
                     "Group: ${group.groupName}\n" +
                     "Location: ${group.location}\n" +
                     "This will delete ${group.count - 1} duplicate services.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllDuplicatesInGroup(group.key)
                        showDeleteGroupDialog = null
                    }
                ) {
                    Text("Delete All Duplicates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Menu button to show/hide action buttons
                    IconButton(
                        onClick = { showActionButtons = !showActionButtons }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading services...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "An error occurred",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadPendingServices() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                pendingServices.isEmpty() && duplicateServices.isEmpty() && !showDuplicateAnalysis && !showActionButtons -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending services or duplicates to review",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Action buttons section (shown when menu is toggled)
                        if (showActionButtons) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Admin Actions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    // First row of buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Analyze Duplicates button
                                        Button(
                                            onClick = { 
                                                showDuplicateAnalysis = !showDuplicateAnalysis
                                                showActionButtons = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (duplicateAnalysis?.duplicateGroups?.isNotEmpty() == true) 
                                                    MaterialTheme.colorScheme.error 
                                                else 
                                                    MaterialTheme.colorScheme.tertiary
                                            )
                                        ) {
                                            Icon(Icons.Default.Analytics, contentDescription = "Analyze")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Analyze", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        
                                        // Duplicates button
                                        Button(
                                            onClick = { 
                                                showDuplicateSection = !showDuplicateSection
                                                showActionButtons = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (duplicateServices.isNotEmpty()) 
                                                    MaterialTheme.colorScheme.error 
                                                else 
                                                    MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicates")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Duplicates", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Second row of buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Sync Services button
                                        Button(
                                            onClick = { 
                                                searchViewModel.syncAllServicesWithFirebase()
                                                showActionButtons = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = "Sync")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sync", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        
                                        // Check Firebase button
                                        Button(
                                            onClick = { 
                                                viewModel.checkAndPopulateFirebase()
                                                showActionButtons = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "Check")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Check", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Third row - view local JSON cache (read-only)
                                    OutlinedButton(
                                        onClick = {
                                            navController.navigate("admin_cached_services")
                                            showActionButtons = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("View cached services")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Fourth row - Replace Firebase button (full width due to destructive nature)
                                    Button(
                                        onClick = { 
                                            searchViewModel.replaceFirebaseWithHardcodedServices()
                                            showActionButtons = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Replace Firebase")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Replace Firebase")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Add service count information at the top
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Service Counts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "App Services",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "${allServices.size}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "Firebase Services",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = firebaseServiceCount?.toString() ?: "Loading...",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                
                                // Add duplicate analysis summary if available
                                duplicateAnalysis?.let { analysis ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Unique Services",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = "${analysis.uniqueServices}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "Duplicates",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = "${analysis.totalServices - analysis.uniqueServices}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (analysis.totalServices > analysis.uniqueServices) 
                                                    MaterialTheme.colorScheme.error 
                                                else 
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Show duplicate analysis if requested
                        if (showDuplicateAnalysis && duplicateAnalysis != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Duplicate Analysis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = duplicateAnalysis!!.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    
                                    if (duplicateAnalysis!!.duplicateGroups.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Duplicate Groups:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        LazyColumn(
                                            modifier = Modifier.heightIn(max = 200.dp)
                                        ) {
                                            items(duplicateAnalysis!!.duplicateGroups) { group ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp)
                                                    ) {
                                                        Text(
                                                            text = group.key,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = "${group.count} copies found",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (showDuplicateSection && duplicateServices.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Duplicate Services (${duplicateServices.size} groups)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = { showDuplicateConfirmDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove All Duplicates")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove All")
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    duplicateServices.forEach { duplicateGroup ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Group: ${duplicateGroup.first().groupName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Organization: ${duplicateGroup.first().organizationName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "Location: ${duplicateGroup.first().location}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "Count: ${duplicateGroup.size}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (pendingServices.isNotEmpty()) {
                            Text(
                                text = "Pending Services (${pendingServices.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = pendingServices,
                                    key = { it.id }
                                ) { service ->
                                    PendingServiceCard(
                                        service = service,
                                        onApprove = {
                                            viewModel.approveService(service.id)
                                        },
                                        onReject = {
                                            viewModel.rejectService(service.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(statusUpdateState) {
                statusUpdateState?.let { result ->
                    result.onSuccess {
                        snackbarHostState.showSnackbar("Service status updated successfully")
                        viewModel.loadPendingServices()
                    }.onFailure { e ->
                        snackbarHostState.showSnackbar("Failed to update service status: ${e.message}")
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PendingServiceCard(
    service: Service,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = service.groupName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = service.organizationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (!service.schedule.isNullOrBlank()) {
                Text(
                    text = service.schedule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Text(
                text = service.location,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approve")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
                
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
} 