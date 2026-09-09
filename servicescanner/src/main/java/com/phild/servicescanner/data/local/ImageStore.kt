package com.phild.servicescanner.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStore(private val context: Context) {

    private val imagesDir: File
        get() = File(context.cacheDir, "images").apply { mkdirs() }

    fun createCaptureUri(): Uri {
        val file = File(imagesDir, "capture_${UUID.randomUUID()}.jpg")
        file.createNewFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun loadPreviewBitmap(uri: Uri, maxEdge: Int = 1600): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val scale = maxOf(info.size.width, info.size.height).toFloat() / maxEdge
                    if (scale > 1f) {
                        decoder.setTargetSize(
                            (info.size.width / scale).toInt().coerceAtLeast(1),
                            (info.size.height / scale).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                decodeSampledBitmap(uri, maxEdge)
            }
        }.getOrNull()
    }

    fun rotateClockwise(uri: Uri): Uri? {
        val original = loadPreviewBitmap(uri) ?: return null
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(
            original,
            0,
            0,
            original.width,
            original.height,
            matrix,
            true
        )
        if (rotated !== original) {
            original.recycle()
        }
        val output = File(imagesDir, "rotated_${UUID.randomUUID()}.jpg")
        FileOutputStream(output).use { stream ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        rotated.recycle()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )
    }

    fun readBytes(uri: Uri): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    private fun decodeSampledBitmap(uri: Uri, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var inSampleSize = 1
        val largest = maxOf(width, height)
        while (largest / inSampleSize > maxEdge) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
