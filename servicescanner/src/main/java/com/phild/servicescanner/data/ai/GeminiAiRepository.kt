package com.phild.servicescanner.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.ExtractionResult
import com.phild.servicescanner.domain.model.TimetableInput
import com.phild.servicescanner.domain.repository.AiException
import com.phild.servicescanner.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class GeminiAiRepository(
    context: Context,
    private val apiKey: String,
    private val parser: ExtractionJsonParser = ExtractionJsonParser()
) : AiRepository {

    private val appContext = context.applicationContext

    override suspend fun analyseFlyer(image: ByteArray): Result<ExtractionResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (apiKey.isBlank()) {
                    throw AiException(AppError.InvalidApiKey)
                }
                if (!hasInternet()) {
                    throw AiException(AppError.NoInternet)
                }
                val jpeg = try {
                    GeminiRequestFactory.jpegBase64(image)
                } catch (_: Exception) {
                    throw AiException(AppError.UnsupportedImage)
                }
                val body = GeminiRequestFactory.createBody(jpeg)
                val responseText = executeWithFallback(body)
                val parsed = parser.parse(responseText).getOrElse { throw it }
                if (isUnreadable(responseText) || parsed.services.isEmpty()) {
                    throw AiException(AppError.NoReadableInformation)
                }
                parsed
            }
        }
    }

    override suspend fun analyseTimetable(input: TimetableInput): Result<ExtractionResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (apiKey.isBlank()) {
                    throw AiException(AppError.InvalidApiKey)
                }
                if (!hasInternet()) {
                    throw AiException(AppError.NoInternet)
                }
                val images = input.pageImages.map { bytes ->
                    try {
                        GeminiRequestFactory.jpegBase64(bytes)
                    } catch (_: Exception) {
                        throw AiException(AppError.UnsupportedImage)
                    }
                }
                if (input.text.isNullOrBlank() && images.isEmpty()) {
                    throw AiException(AppError.NoReadableInformation)
                }
                val body = GeminiRequestFactory.createTimetableBody(input.text, images)
                val responseText = executeWithFallback(body, timeoutMs = TIMETABLE_TIMEOUT_MS)
                val parsed = parser.parse(responseText).getOrElse { throw it }
                if (isUnreadable(responseText) || parsed.services.isEmpty()) {
                    throw AiException(AppError.NoReadableInformation)
                }
                parsed
            }
        }
    }

    private fun isUnreadable(json: String): Boolean {
        return runCatching {
            val start = json.indexOf('{')
            val end = json.lastIndexOf('}')
            if (start < 0 || end <= start) return false
            JSONObject(json.substring(start, end + 1)).optBoolean("noReadableInformation", false)
        }.getOrDefault(false)
    }

    private fun executeWithFallback(body: String, timeoutMs: Int = TIMEOUT_MS): String {
        val listed = listAvailableModels()
        val models = GeminiModelSelector.candidates(listed)
        var lastFailure: AiException? = null
        for (model in models) {
            try {
                return executeRequest(body, model, timeoutMs)
            } catch (error: AiException) {
                val failure = error.error as? AppError.ProviderFailure
                val canFallback = failure != null &&
                    GeminiErrorParser.shouldRetryWithFallbackModel(
                        statusCode = failure.statusCode ?: 0,
                        message = failure.providerMessage
                    )
                if (!canFallback) {
                    throw error
                }
                lastFailure = error
            }
        }
        throw lastFailure ?: AiException(AppError.ApiUnavailable)
    }

    private fun listAvailableModels(): List<GeminiModelRef> {
        val versions = listOf("v1beta", "v1")
        for (version in versions) {
            val listed = runCatching { fetchModelList(version) }.getOrNull().orEmpty()
            if (listed.isNotEmpty()) return listed
        }
        return emptyList()
    }

    private fun fetchModelList(apiVersion: String): List<GeminiModelRef> {
        val collected = mutableListOf<GeminiModelRef>()
        var pageToken: String? = null
        repeat(5) {
            val suffix = if (pageToken.isNullOrBlank()) "" else "?pageToken=$pageToken"
            val raw = httpGet("$HOST/$apiVersion/models$suffix")
            collected += GeminiModelSelector.parseListedModels(raw, apiVersion)
            pageToken = runCatching {
                JSONObject(raw).optString("nextPageToken").takeIf { it.isNotBlank() }
            }.getOrNull()
            if (pageToken == null) return collected
        }
        return collected
    }

    private fun executeRequest(body: String, model: GeminiModelRef, timeoutMs: Int): String {
        val raw = httpPost("$HOST/${model.apiVersion}/models/${model.modelId}:generateContent", body, timeoutMs)
        return extractModelText(raw)
    }

    private fun httpGet(url: String): String {
        return open(url, "GET", null, TIMEOUT_MS)
    }

    private fun httpPost(url: String, body: String, timeoutMs: Int = TIMEOUT_MS): String {
        return open(url, "POST", body, timeoutMs)
    }

    private fun open(url: String, method: String, body: String?, timeoutMs: Int): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("x-goog-api-key", apiKey)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.buffered().use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw AiException(mapHttpError(code, raw))
            }
            return raw
        } catch (error: AiException) {
            throw error
        } catch (_: SocketTimeoutException) {
            throw AiException(AppError.Timeout)
        } catch (_: UnknownHostException) {
            throw AiException(AppError.NoInternet)
        } catch (_: ConnectException) {
            throw AiException(AppError.NoInternet)
        } catch (error: Exception) {
            throw AiException(
                AppError.ProviderFailure(
                    statusCode = null,
                    providerMessage = GeminiErrorParser.sanitize(error.javaClass.simpleName)
                )
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun extractModelText(raw: String): String {
        val root = runCatching { JSONObject(raw) }.getOrElse {
            throw AiException(AppError.InvalidAiResponse)
        }
        val candidates = root.optJSONArray("candidates")
        val text = candidates
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
        if (text.isBlank()) {
            val blockReason = root.optJSONObject("promptFeedback")
                ?.optString("blockReason")
                .orEmpty()
            if (blockReason.isNotBlank()) {
                throw AiException(
                    AppError.ProviderFailure(
                        statusCode = 200,
                        providerMessage = GeminiErrorParser.sanitize("Blocked: $blockReason")
                    )
                )
            }
            throw AiException(AppError.InvalidAiResponse)
        }
        return text
    }

    private fun mapHttpError(code: Int, raw: String): AppError {
        val detail = GeminiErrorParser.messageFromBody(raw)
        return when (code) {
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN -> AppError.InvalidApiKey
            429 -> AppError.RateLimited
            else -> AppError.ProviderFailure(statusCode = code, providerMessage = detail)
        }
    }

    private fun hasInternet(): Boolean {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val HOST = "https://generativelanguage.googleapis.com"
        private const val TIMEOUT_MS = 45_000
        private const val TIMETABLE_TIMEOUT_MS = 90_000
    }
}
