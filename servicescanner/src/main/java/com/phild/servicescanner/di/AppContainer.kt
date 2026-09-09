package com.phild.servicescanner.di

import android.content.Context
import com.phild.servicescanner.BuildConfig
import com.phild.servicescanner.data.ai.GeminiAiRepository
import com.phild.servicescanner.data.ai.MockAiRepository
import com.phild.servicescanner.data.document.DocxDocumentGenerator
import com.phild.servicescanner.data.firebase.FirestorePeerSupportRepository
import com.phild.servicescanner.data.local.ApiKeyMask
import com.phild.servicescanner.data.local.GeminiApiKeyStore
import com.phild.servicescanner.data.local.ImageStore
import com.phild.servicescanner.data.local.LocalHistoryRepository
import com.phild.servicescanner.data.timetable.TimetableSourceReader
import com.phild.servicescanner.domain.model.ExtractionResult
import com.phild.servicescanner.domain.model.TimetableInput
import com.phild.servicescanner.domain.repository.AiRepository
import com.phild.servicescanner.domain.repository.DocumentGenerator
import com.phild.servicescanner.domain.repository.HistoryRepository
import com.phild.servicescanner.domain.repository.PeerSupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val apiKeyStore = GeminiApiKeyStore(appContext)

    private val _geminiConfigured = MutableStateFlow(false)
    val geminiConfigured: StateFlow<Boolean> = _geminiConfigured.asStateFlow()

    private val _maskedRuntimeKey = MutableStateFlow("")
    val maskedRuntimeKey: StateFlow<String> = _maskedRuntimeKey.asStateFlow()

    @Volatile
    private var activeAiRepository: AiRepository = MockAiRepository()

    val aiRepository: AiRepository = object : AiRepository {
        override suspend fun analyseFlyer(image: ByteArray): Result<ExtractionResult> {
            return activeAiRepository.analyseFlyer(image)
        }

        override suspend fun analyseTimetable(input: TimetableInput): Result<ExtractionResult> {
            return activeAiRepository.analyseTimetable(input)
        }
    }

    val imageStore: ImageStore = ImageStore(appContext)

    val timetableSourceReader: TimetableSourceReader = TimetableSourceReader(appContext)

    private val documentsDir: File = File(appContext.cacheDir, "documents").apply { mkdirs() }

    val documentGenerator: DocumentGenerator = DocxDocumentGenerator(documentsDir)

    val historyRepository: HistoryRepository = LocalHistoryRepository(appContext)

    val peerSupportRepository: PeerSupportRepository = FirestorePeerSupportRepository()

    val templateAvailable: Boolean = true

    init {
        applyKeys(apiKeyStore.readBlocking())
    }

    suspend fun saveRuntimeApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        apiKeyStore.save(trimmed)
        applyKeys(trimmed)
    }

    suspend fun clearRuntimeApiKey() {
        apiKeyStore.save("")
        applyKeys("")
    }

    private fun applyKeys(runtimeKey: String) {
        val trimmedRuntime = runtimeKey.trim()
        _maskedRuntimeKey.value = ApiKeyMask.mask(trimmedRuntime)
        val resolved = resolveApiKey(trimmedRuntime)
        val configured = resolved.isNotBlank()
        _geminiConfigured.value = configured
        activeAiRepository = if (configured) {
            GeminiAiRepository(
                context = appContext,
                apiKey = resolved
            )
        } else {
            MockAiRepository()
        }
    }

    private fun resolveApiKey(runtimeKey: String): String {
        return runtimeKey.ifBlank { BuildConfig.GEMINI_API_KEY.trim() }
    }
}
