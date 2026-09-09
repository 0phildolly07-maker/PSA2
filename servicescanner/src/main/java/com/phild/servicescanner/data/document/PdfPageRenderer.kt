package com.phild.servicescanner.data.document

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File

class PdfPageRenderer(
    private val maxPages: Int = 8,
    private val maxEdge: Int = 1600
) {
    fun renderPages(bytes: ByteArray, tempDir: File): List<ByteArray> {
        val temp = File.createTempFile("timetable", ".pdf", tempDir)
        return try {
            temp.writeBytes(bytes)
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount.coerceAtMost(maxPages)
                    (0 until count).map { index ->
                        renderer.openPage(index).use { page ->
                            pageToJpeg(page)
                        }
                    }
                }
            }
        } finally {
            temp.delete()
        }
    }

    private fun pageToJpeg(page: PdfRenderer.Page): ByteArray {
        val largest = maxOf(page.width, page.height).coerceAtLeast(1)
        val scale = maxEdge.toFloat() / largest
        val width = (page.width * scale).toInt().coerceAtLeast(1)
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        bitmap.recycle()
        return out.toByteArray()
    }
}
