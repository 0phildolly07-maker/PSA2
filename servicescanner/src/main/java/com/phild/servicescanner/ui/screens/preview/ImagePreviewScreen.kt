package com.phild.servicescanner.ui.screens.preview

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phild.servicescanner.R
import com.phild.servicescanner.data.local.ImageStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    imageUri: Uri,
    onUseImage: (Uri) -> Unit,
    onChooseAnother: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageStore = remember { ImageStore(context.applicationContext) }
    var currentUri by remember(imageUri) { mutableStateOf(imageUri) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(currentUri) {
        val loaded = imageStore.loadPreviewBitmap(currentUri)
        bitmap = loaded
        loadFailed = loaded == null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_image_preview)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val rotated = imageStore.rotateClockwise(currentUri)
                            if (rotated != null) {
                                currentUri = rotated
                            }
                        },
                        enabled = bitmap != null
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                            contentDescription = stringResource(R.string.action_rotate)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            when {
                loadFailed -> {
                    Text(
                        text = stringResource(R.string.image_load_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                bitmap != null -> {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.selected_flyer_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.image_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onUseImage(currentUri) },
                enabled = bitmap != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_use_this_image))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onChooseAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_choose_another))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        val rotated = imageStore.rotateClockwise(currentUri)
                        if (rotated != null) {
                            currentUri = rotated
                        }
                    },
                    enabled = bitmap != null
                ) {
                    Text(stringResource(R.string.action_rotate))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
