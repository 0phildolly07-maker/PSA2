package com.phild.servicescanner.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phild.servicescanner.data.local.ImageStore
import com.phild.servicescanner.data.timetable.TimetableSourceReader
import com.phild.servicescanner.di.AppContainer
import com.phild.servicescanner.domain.model.AppError
import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.HistoryEntry
import com.phild.servicescanner.domain.repository.AiException
import com.phild.servicescanner.domain.repository.AiRepository
import com.phild.servicescanner.domain.repository.DocumentGenerator
import com.phild.servicescanner.domain.repository.HistoryRepository
import com.phild.servicescanner.domain.repository.PeerSupportRepository
import com.phild.servicescanner.ui.state.AnalysingPhase
import com.phild.servicescanner.ui.state.FlyerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FlyerViewModel(
    private val aiRepository: AiRepository,
    private val documentGenerator: DocumentGenerator,
    private val imageStore: ImageStore,
    private val historyRepository: HistoryRepository,
    private val timetableSourceReader: TimetableSourceReader,
    private val peerSupportRepository: PeerSupportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FlyerUiState>(FlyerUiState.Idle)
    val uiState: StateFlow<FlyerUiState> = _uiState.asStateFlow()

    private var analyseJob: Job? = null
    private var currentHistoryId: String? = null

    fun onImageSelected(uri: Uri) {
        analyseJob?.cancel()
        currentHistoryId = null
        _uiState.value = FlyerUiState.ImageSelected(uri)
    }

    fun chooseAnother() {
        analyseJob?.cancel()
        currentHistoryId = null
        _uiState.value = FlyerUiState.Idle
    }

    fun analyseSelectedImage() {
        val current = _uiState.value
        val imageUri = when (current) {
            is FlyerUiState.ImageSelected -> current.imageUri
            is FlyerUiState.ExtractionFailed -> current.imageUri
            is FlyerUiState.Reviewing -> current.imageUri
            else -> return
        }
        if (analyseJob?.isActive == true) return

        analyseJob = viewModelScope.launch {
            _uiState.value = FlyerUiState.Analysing(imageUri, AnalysingPhase.AnalysingFlyer)
            val bytes = imageStore.readBytes(imageUri)
            if (bytes == null || bytes.isEmpty()) {
                _uiState.value = FlyerUiState.ExtractionFailed(imageUri, AppError.ImageUploadFailed)
                return@launch
            }

            delay(700)
            _uiState.value = FlyerUiState.Analysing(imageUri, AnalysingPhase.ReadingInformation)

            val result = runCatching { aiRepository.analyseFlyer(bytes) }
                .getOrElse { throwable -> Result.failure(throwable) }

            delay(700)
            _uiState.value = FlyerUiState.Analysing(imageUri, AnalysingPhase.OrganisingDetails)
            delay(400)

            result.fold(
                onSuccess = { extraction ->
                    _uiState.value = FlyerUiState.Reviewing(
                        imageUri = imageUri,
                        services = extraction.services.ifEmpty { listOf(extraction.service) },
                        selectedIndex = 0,
                        uncertainFields = extraction.uncertainFields,
                        multipleActivitiesDetected = extraction.multipleActivitiesDetected
                    )
                    persistNewHistory(
                        imageUri = imageUri,
                        services = extraction.services.ifEmpty { listOf(extraction.service) },
                        uncertainFields = extraction.uncertainFields,
                        multipleActivities = extraction.multipleActivitiesDetected,
                        isTimetable = false
                    )
                },
                onFailure = { throwable ->
                    val error = (throwable as? AiException)?.error ?: AppError.Unknown()
                    _uiState.value = FlyerUiState.ExtractionFailed(imageUri, error)
                }
            )
        }
    }

    fun analyseTimetable(uri: Uri? = null) {
        val sourceUri = uri ?: when (val current = _uiState.value) {
            is FlyerUiState.ImageSelected -> current.imageUri
            is FlyerUiState.ExtractionFailed -> current.imageUri
            is FlyerUiState.Analysing -> current.imageUri
            is FlyerUiState.Reviewing -> current.imageUri
            else -> return
        }
        if (analyseJob?.isActive == true) return

        analyseJob = viewModelScope.launch {
            _uiState.value = FlyerUiState.Analysing(
                imageUri = sourceUri,
                phase = AnalysingPhase.AnalysingFlyer,
                isTimetable = true
            )
            val input = timetableSourceReader.read(sourceUri).getOrElse { throwable ->
                val error = (throwable as? AiException)?.error ?: AppError.TimetableReadFailed
                _uiState.value = FlyerUiState.ExtractionFailed(sourceUri, error, isTimetable = true)
                return@launch
            }

            delay(400)
            _uiState.value = FlyerUiState.Analysing(
                imageUri = sourceUri,
                phase = AnalysingPhase.ReadingInformation,
                isTimetable = true
            )

            val result = runCatching { aiRepository.analyseTimetable(input) }
                .getOrElse { throwable -> Result.failure(throwable) }

            delay(400)
            _uiState.value = FlyerUiState.Analysing(
                imageUri = sourceUri,
                phase = AnalysingPhase.OrganisingDetails,
                isTimetable = true
            )
            delay(300)

            result.fold(
                onSuccess = { extraction ->
                    val services = extraction.services
                    if (services.isEmpty()) {
                        _uiState.value = FlyerUiState.ExtractionFailed(
                            sourceUri,
                            AppError.NoReadableInformation,
                            isTimetable = true
                        )
                        return@fold
                    }
                    _uiState.value = FlyerUiState.Reviewing(
                        imageUri = sourceUri,
                        services = services,
                        selectedIndex = null,
                        isTimetable = true,
                        uncertainFields = extraction.uncertainFields,
                        multipleActivitiesDetected = true
                    )
                    persistNewHistory(
                        imageUri = sourceUri,
                        services = services,
                        uncertainFields = extraction.uncertainFields,
                        multipleActivities = true,
                        isTimetable = true
                    )
                },
                onFailure = { throwable ->
                    val error = (throwable as? AiException)?.error ?: AppError.Unknown()
                    _uiState.value = FlyerUiState.ExtractionFailed(sourceUri, error, isTimetable = true)
                }
            )
        }
    }

    fun retryAnalysis() {
        val current = _uiState.value
        val isTimetable = when (current) {
            is FlyerUiState.ExtractionFailed -> current.isTimetable
            is FlyerUiState.Analysing -> current.isTimetable
            is FlyerUiState.Reviewing -> current.isTimetable
            else -> false
        }
        if (isTimetable) {
            analyseTimetable()
        } else {
            analyseSelectedImage()
        }
    }

    fun updateService(service: ExtractedService) {
        val current = _uiState.value
        if (current is FlyerUiState.Reviewing) {
            val index = current.selectedIndex ?: 0
            if (index !in current.services.indices) return
            val updated = current.services.toMutableList()
            updated[index] = service
            _uiState.value = current.copy(services = updated)
        }
    }

    fun openActivity(index: Int) {
        val current = _uiState.value
        if (current is FlyerUiState.Reviewing && index in current.services.indices) {
            _uiState.value = current.copy(selectedIndex = index)
        }
    }

    fun closeActivity() {
        val current = _uiState.value
        if (current is FlyerUiState.Reviewing && current.isTimetable) {
            _uiState.value = current.copy(selectedIndex = null)
        }
    }

    fun removeActivity(index: Int) {
        val current = _uiState.value
        if (current !is FlyerUiState.Reviewing || !current.isTimetable) return
        if (index !in current.services.indices) return
        val updated = current.services.toMutableList()
        updated.removeAt(index)
        _uiState.value = current.copy(services = updated, selectedIndex = null)
    }

    fun generateDocument() {
        val current = _uiState.value
        if (current !is FlyerUiState.Reviewing || current.isGenerating) return
        val services = if (current.isTimetable) current.services else listOf(current.service)
        if (services.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = current.copy(isGenerating = true)
            runCatching {
                documentGenerator.generateDocument(services)
            }.fold(
                onSuccess = { file ->
                    persistGeneratedDocument(services, file)
                    val publish = peerSupportRepository.publishServices(services)
                    val result = publish.getOrNull()
                    _uiState.value = FlyerUiState.DocumentGenerated(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        publishedCount = result?.uploadedCount ?: 0,
                        skippedDuplicateCount = result?.skippedDuplicateCount ?: 0,
                        skippedIncompleteCount = result?.skippedIncompleteCount ?: 0,
                        publishError = if (publish.isFailure) AppError.FirebaseUploadFailed else null
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(
                        isGenerating = false,
                        generationError = AppError.DocumentGenerationFailed
                    )
                }
            )
        }
    }

    fun openHistory(id: String) {
        viewModelScope.launch {
            val entry = historyRepository.get(id) ?: return@launch
            currentHistoryId = entry.id
            val imageUri = entry.imagePath?.let { Uri.fromFile(File(it)) } ?: Uri.EMPTY
            val services = entry.services.ifEmpty { listOf(entry.service) }
            _uiState.value = FlyerUiState.Reviewing(
                imageUri = imageUri,
                services = services,
                selectedIndex = if (entry.isTimetable) null else 0,
                isTimetable = entry.isTimetable,
                uncertainFields = entry.uncertainFields,
                multipleActivitiesDetected = entry.multipleActivitiesDetected
            )
        }
    }

    fun clearGenerationError() {
        val current = _uiState.value
        if (current is FlyerUiState.Reviewing && current.generationError != null) {
            _uiState.value = current.copy(generationError = null)
        }
    }

    fun createAnother() {
        analyseJob?.cancel()
        currentHistoryId = null
        _uiState.value = FlyerUiState.Idle
    }

    fun dismissError() {
        val current = _uiState.value
        _uiState.value = when (current) {
            is FlyerUiState.ExtractionFailed -> FlyerUiState.ImageSelected(
                current.imageUri,
                isTimetable = current.isTimetable
            )
            is FlyerUiState.Error -> FlyerUiState.Idle
            else -> current
        }
    }

    private suspend fun persistNewHistory(
        imageUri: Uri,
        services: List<ExtractedService>,
        uncertainFields: Set<String>,
        multipleActivities: Boolean,
        isTimetable: Boolean
    ) {
        runCatching {
            val primary = services.firstOrNull() ?: ExtractedService()
            val saved = historyRepository.save(
                HistoryEntry(
                    id = "",
                    createdAtEpochMs = System.currentTimeMillis(),
                    serviceName = historyTitle(services, isTimetable),
                    service = primary,
                    services = services,
                    uncertainFields = uncertainFields,
                    multipleActivitiesDetected = multipleActivities,
                    isTimetable = isTimetable
                )
            )
            currentHistoryId = saved.id
            if (!isTimetable) {
                val imagePath = historyRepository.attachImage(saved.id, imageUri)
                historyRepository.update(saved.copy(imagePath = imagePath))
            }
        }
    }

    private suspend fun persistGeneratedDocument(services: List<ExtractedService>, file: File) {
        runCatching {
            val id = currentHistoryId ?: return
            val existing = historyRepository.get(id) ?: return
            val documentPath = historyRepository.attachDocument(id, file)
            val primary = services.firstOrNull() ?: existing.service
            historyRepository.update(
                existing.copy(
                    service = primary,
                    services = services,
                    serviceName = historyTitle(services, existing.isTimetable),
                    documentPath = documentPath
                )
            )
        }
    }

    private fun historyTitle(services: List<ExtractedService>, isTimetable: Boolean): String? {
        return if (isTimetable) {
            services.firstOrNull()?.organisation?.takeIf { it.isNotBlank() }
                ?: services.firstOrNull()?.serviceName
        } else {
            services.firstOrNull()?.serviceName
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FlyerViewModel(
                        aiRepository = container.aiRepository,
                        documentGenerator = container.documentGenerator,
                        imageStore = container.imageStore,
                        historyRepository = container.historyRepository,
                        timetableSourceReader = container.timetableSourceReader,
                        peerSupportRepository = container.peerSupportRepository
                    ) as T
                }
            }
        }
    }
}
