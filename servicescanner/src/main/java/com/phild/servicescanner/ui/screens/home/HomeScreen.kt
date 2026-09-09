package com.phild.servicescanner.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phild.servicescanner.R
import com.phild.servicescanner.ui.theme.ServiceScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onChooseImage: () -> Unit,
    onUploadTimetable: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    onExit: (() -> Unit)? = null
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = snackbarHost,
        topBar = {
            if (onExit != null) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onExit) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back_to_peerpower)
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.scanner_app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_take_photo))
            }
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onChooseImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_choose_image))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onUploadTimetable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_upload_timetable))
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_history))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onSettings) {
                Text(stringResource(R.string.action_settings))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ServiceScannerTheme {
        HomeScreen(
            onTakePhoto = {},
            onChooseImage = {},
            onUploadTimetable = {},
            onHistory = {},
            onSettings = {}
        )
    }
}
