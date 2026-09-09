package com.phild.servicescanner.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object GeminiRequestFactory {
    fun createBody(imageJpegBase64: String): String {
        val properties = serviceProperties()
            .put("multipleActivitiesDetected", JSONObject().put("type", "BOOLEAN"))
            .put("noReadableInformation", JSONObject().put("type", "BOOLEAN"))

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put("properties", properties)

        val imagePart = inlineImage(imageJpegBase64)
        val textPart = JSONObject().put("text", GeminiPrompt.userInstruction)
        val parts = JSONArray().put(imagePart).put(textPart)
        return request(GeminiPrompt.systemInstruction, parts, schema)
    }

    fun createTimetableBody(text: String?, imageJpegBase64: List<String>): String {
        val activitySchema = JSONObject()
            .put("type", "OBJECT")
            .put("properties", serviceProperties())

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put(
                        "activities",
                        JSONObject()
                            .put("type", "ARRAY")
                            .put("items", activitySchema)
                    )
                    .put("noReadableInformation", JSONObject().put("type", "BOOLEAN"))
            )

        val parts = JSONArray()
        imageJpegBase64.forEach { encoded ->
            parts.put(inlineImage(encoded))
        }
        if (!text.isNullOrBlank()) {
            parts.put(JSONObject().put("text", text))
        }
        parts.put(JSONObject().put("text", GeminiPrompt.timetableUserInstruction))
        return request(GeminiPrompt.timetableSystemInstruction, parts, schema)
    }

    fun jpegBase64(imageBytes: ByteArray): String {
        val bitmap = decode(imageBytes) ?: error("unsupported")
        val scaled = scale(bitmap, 1600)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        bitmap.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun request(systemInstruction: String, parts: JSONArray, schema: JSONObject): String {
        val generationConfig = JSONObject()
            .put("temperature", 0.1)
            .put("responseMimeType", "application/json")
            .put("responseSchema", schema)
        val content = JSONObject().put("role", "user").put("parts", parts)
        return JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            )
            .put("contents", JSONArray().put(content))
            .put("generationConfig", generationConfig)
            .toString()
    }

    private fun inlineImage(imageJpegBase64: String): JSONObject {
        return JSONObject().put(
            "inline_data",
            JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", imageJpegBase64)
        )
    }

    private fun serviceProperties(): JSONObject {
        val nullableString = JSONObject().put("type", "STRING").put("nullable", true)
        val stringArray = JSONObject()
            .put("type", "ARRAY")
            .put("items", JSONObject().put("type", "STRING"))
        return JSONObject()
            .put("serviceName", nullableString)
            .put("description", JSONObject().put("type", "STRING").put("nullable", true))
            .put("category", JSONObject().put("type", "STRING").put("nullable", true))
            .put("organisation", JSONObject().put("type", "STRING").put("nullable", true))
            .put("venue", JSONObject().put("type", "STRING").put("nullable", true))
            .put("address", JSONObject().put("type", "STRING").put("nullable", true))
            .put("postcode", JSONObject().put("type", "STRING").put("nullable", true))
            .put("telephone", JSONObject().put("type", "STRING").put("nullable", true))
            .put("contactName", JSONObject().put("type", "STRING").put("nullable", true))
            .put("email", JSONObject().put("type", "STRING").put("nullable", true))
            .put("website", JSONObject().put("type", "STRING").put("nullable", true))
            .put("days", stringArray)
            .put("times", JSONObject().put("type", "STRING").put("nullable", true))
            .put("frequency", JSONObject().put("type", "STRING").put("nullable", true))
            .put("cost", JSONObject().put("type", "STRING").put("nullable", true))
            .put("eligibility", JSONObject().put("type", "STRING").put("nullable", true))
            .put("referralProcess", JSONObject().put("type", "STRING").put("nullable", true))
            .put("accessibility", JSONObject().put("type", "STRING").put("nullable", true))
            .put("areaCovered", JSONObject().put("type", "STRING").put("nullable", true))
            .put("additionalNotes", JSONObject().put("type", "STRING").put("nullable", true))
            .put("uncertainFields", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 2000)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var size = 1
        val largest = maxOf(width, height).coerceAtLeast(1)
        while (largest / size > maxEdge) {
            size *= 2
        }
        return size
    }

    private fun scale(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
