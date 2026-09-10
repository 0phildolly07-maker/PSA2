package com.phild.servicescanner.ui.screens.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phild.servicescanner.R
import com.phild.servicescanner.domain.model.ExtractedService
import com.phild.servicescanner.domain.model.ServiceCategories
import com.phild.servicescanner.ui.components.FormSectionTitle
import com.phild.servicescanner.ui.components.ReviewTextField
import com.phild.servicescanner.ui.components.appErrorMessage
import com.phild.servicescanner.ui.state.FlyerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    uiState: FlyerUiState.Reviewing,
    onServiceChange: (ExtractedService) -> Unit,
    onGenerateDocument: () -> Unit,
    onGenerationErrorShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showGenerateButton: Boolean = true
) {
    val service = uiState.service
    val uncertain = uiState.uncertainFields
    val snackbarHostState = remember { SnackbarHostState() }
    val generationError = uiState.generationError
    val errorMessage = generationError?.let { appErrorMessage(it) }

    LaunchedEffect(generationError) {
        if (generationError != null && errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onGenerationErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_review)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
            if (uiState.multipleActivitiesDetected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.multiple_activities_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            FormSectionTitle(stringResource(R.string.section_basic_information))
            ReviewTextField(
                label = stringResource(R.string.field_organisation),
                value = service.organisation.orEmpty(),
                onValueChange = { onServiceChange(service.copy(organisation = it.emptyToNull())) },
                needsReview = "organisation" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_service_name),
                value = service.serviceName.orEmpty(),
                onValueChange = { onServiceChange(service.copy(serviceName = it.emptyToNull())) },
                needsReview = "serviceName" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_description),
                value = service.description.orEmpty(),
                onValueChange = { onServiceChange(service.copy(description = it.emptyToNull())) },
                singleLine = false,
                minLines = 3,
                needsReview = "description" in uncertain
            )
            CategoryDropdown(
                selected = ServiceCategories.parseSelected(service.category),
                needsReview = "category" in uncertain,
                onSelected = { onServiceChange(service.copy(category = ServiceCategories.formatSelected(it))) }
            )

            FormSectionTitle(stringResource(R.string.section_location))
            ReviewTextField(
                label = stringResource(R.string.field_venue),
                value = service.venue.orEmpty(),
                onValueChange = { onServiceChange(service.copy(venue = it.emptyToNull())) },
                needsReview = "venue" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_address),
                value = service.address.orEmpty(),
                onValueChange = { onServiceChange(service.copy(address = it.emptyToNull())) },
                singleLine = false,
                minLines = 2,
                needsReview = "address" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_postcode),
                value = service.postcode.orEmpty(),
                onValueChange = { onServiceChange(service.copy(postcode = it.emptyToNull())) },
                needsReview = "postcode" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_area_covered),
                value = service.areaCovered.orEmpty(),
                onValueChange = { onServiceChange(service.copy(areaCovered = it.emptyToNull())) },
                needsReview = "areaCovered" in uncertain
            )

            FormSectionTitle(stringResource(R.string.section_contact))
            ReviewTextField(
                label = stringResource(R.string.field_contact_name),
                value = service.contactName.orEmpty(),
                onValueChange = { onServiceChange(service.copy(contactName = it.emptyToNull())) },
                needsReview = "contactName" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_email),
                value = service.email.orEmpty(),
                onValueChange = { onServiceChange(service.copy(email = it.emptyToNull())) },
                needsReview = "email" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_website),
                value = service.website.orEmpty(),
                onValueChange = { onServiceChange(service.copy(website = it.emptyToNull())) },
                needsReview = "website" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_telephone),
                value = service.telephone.orEmpty(),
                onValueChange = { onServiceChange(service.copy(telephone = it.emptyToNull())) },
                needsReview = "telephone" in uncertain
            )

            FormSectionTitle(stringResource(R.string.section_event_details))
            ReviewTextField(
                label = stringResource(R.string.field_times),
                value = service.times.orEmpty(),
                onValueChange = { onServiceChange(service.copy(times = it.emptyToNull())) },
                needsReview = "times" in uncertain
            )
            DaysReviewField(
                days = service.days,
                selectedIndex = uiState.selectedIndex,
                needsReview = "days" in uncertain,
                onDaysChange = { onServiceChange(service.copy(days = it)) }
            )
            ReviewTextField(
                label = stringResource(R.string.field_frequency),
                value = service.frequency.orEmpty(),
                onValueChange = { onServiceChange(service.copy(frequency = it.emptyToNull())) },
                needsReview = "frequency" in uncertain
            )

            FormSectionTitle(stringResource(R.string.section_additional))
            ReviewTextField(
                label = stringResource(R.string.field_cost),
                value = service.cost.orEmpty(),
                onValueChange = { onServiceChange(service.copy(cost = it.emptyToNull())) },
                needsReview = "cost" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_eligibility),
                value = service.eligibility.orEmpty(),
                onValueChange = { onServiceChange(service.copy(eligibility = it.emptyToNull())) },
                singleLine = false,
                minLines = 2,
                needsReview = "eligibility" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_referral_process),
                value = service.referralProcess.orEmpty(),
                onValueChange = { onServiceChange(service.copy(referralProcess = it.emptyToNull())) },
                singleLine = false,
                minLines = 2,
                needsReview = "referralProcess" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_accessibility),
                value = service.accessibility.orEmpty(),
                onValueChange = { onServiceChange(service.copy(accessibility = it.emptyToNull())) },
                singleLine = false,
                minLines = 2,
                needsReview = "accessibility" in uncertain
            )
            ReviewTextField(
                label = stringResource(R.string.field_additional_notes),
                value = service.additionalNotes.orEmpty(),
                onValueChange = { onServiceChange(service.copy(additionalNotes = it.emptyToNull())) },
                singleLine = false,
                minLines = 3,
                needsReview = "additionalNotes" in uncertain
            )

            if (showGenerateButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onGenerateDocument,
                    enabled = !uiState.isGenerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isGenerating) {
                            stringResource(R.string.generating_document)
                        } else {
                            stringResource(R.string.action_generate_document)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DaysReviewField(
    days: List<String>,
    selectedIndex: Int?,
    needsReview: Boolean,
    onDaysChange: (List<String>) -> Unit
) {
    var text by remember(selectedIndex) { mutableStateOf(days.joinToString(", ")) }
    ReviewTextField(
        label = stringResource(R.string.field_days),
        value = text,
        onValueChange = { value ->
            text = value
            onDaysChange(parseDaysField(value))
        },
        needsReview = needsReview
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: List<String>,
    needsReview: Boolean,
    onSelected: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(selected) {
        val extras = selected.filter { it !in ServiceCategories.all }
        extras + ServiceCategories.all
    }
    val displayValue = ServiceCategories.formatSelected(selected).orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_category)) },
            placeholder = { Text(stringResource(R.string.category_unspecified)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            minLines = 1,
            maxLines = 3,
            isError = needsReview,
            supportingText = {
                Text(
                    if (needsReview) {
                        stringResource(R.string.please_check)
                    } else {
                        stringResource(R.string.category_select_hint)
                    }
                )
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val checked = option in selected
                val description = ServiceCategories.descriptionFor(option)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option)
                            if (!description.isNullOrBlank()) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        val next = if (checked) selected - option else selected + option
                        onSelected(next)
                    }
                )
            }
        }
    }
}

internal fun String.emptyToNull(): String? = ifEmpty { null }

internal fun parseDaysField(text: String): List<String> =
    text.split(',', ';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
