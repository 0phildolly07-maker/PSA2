package com.phild.servicescanner.data.document

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.phild.servicescanner.R
import java.io.File

class DocumentOpener(private val context: Context) {

    fun open(file: File): Boolean {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DOCX_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launch(intent)
    }

    fun share(file: File): Boolean {
        val uri = uriFor(file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = DOCX_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.action_share_document)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launch(chooser)
    }

    fun uriFor(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    private fun launch(intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    companion object {
        const val DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }
}
