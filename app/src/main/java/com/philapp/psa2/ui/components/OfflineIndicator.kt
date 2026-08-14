package com.philapp.psa2.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.philapp.psa2.utils.ConnectionStatus
import com.philapp.psa2.utils.NetworkConnectivityObserver
import com.philapp.psa2.utils.isConnected

@Composable
fun rememberNetworkConnectivity(): State<ConnectionStatus> {
    val context = LocalContext.current
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    
    return connectivityObserver.observe().collectAsState(
        initial = if (connectivityObserver.isConnected()) 
            ConnectionStatus.Available 
        else 
            ConnectionStatus.Unavailable
    )
}

@Composable
fun OfflineIndicatorBanner(
    modifier: Modifier = Modifier,
    connectionStatus: ConnectionStatus = rememberNetworkConnectivity().value
) {
    AnimatedVisibility(
        visible = !connectionStatus.isConnected(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline - Using cached data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun OfflineIndicatorSnackbar(
    snackbarHostState: SnackbarHostState,
    connectionStatus: ConnectionStatus = rememberNetworkConnectivity().value
) {
    val isConnected = connectionStatus.isConnected()
    val previousConnectionState = remember { mutableStateOf(isConnected) }

    LaunchedEffect(isConnected) {
        if (previousConnectionState.value != isConnected) {
            if (!isConnected) {
                snackbarHostState.showSnackbar(
                    message = "You're offline. Using cached data.",
                    duration = SnackbarDuration.Short
                )
            } else if (previousConnectionState.value == false) {
                snackbarHostState.showSnackbar(
                    message = "Back online!",
                    duration = SnackbarDuration.Short
                )
            }
            previousConnectionState.value = isConnected
        }
    }
}

