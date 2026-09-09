package com.phild.servicescanner.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.phild.servicescanner.R
import com.phild.servicescanner.data.local.ImageStore
import com.phild.servicescanner.data.timetable.TimetableMime
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onImageSelected: (Uri) -> Unit,
    onTimetableSelected: (Uri) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onExit: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val imageStore = remember { ImageStore(context.applicationContext) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (success && uri != null) {
            onImageSelected(uri)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (isSupportedImageMime(mime)) {
                onImageSelected(uri)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.unsupported_image_type)
                    )
                }
            }
        }
    }

    val pickTimetableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val name = displayName(context, uri)
            if (TimetableMime.isSupported(mime, name)) {
                onTimetableSelected(uri)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.unsupported_timetable_type)
                    )
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = imageStore.createCaptureUri()
            pendingCaptureUri = uri
            takePictureLauncher.launch(uri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.camera_permission_required)
                )
            }
        }
    }

    HomeScreen(
        onTakePhoto = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val uri = imageStore.createCaptureUri()
                pendingCaptureUri = uri
                takePictureLauncher.launch(uri)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onChooseImage = {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onUploadTimetable = {
            pickTimetableLauncher.launch(TimetableMime.openDocumentTypes)
        },
        onHistory = onHistory,
        onSettings = onSettings,
        onExit = onExit,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    )
}

private fun displayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
}
