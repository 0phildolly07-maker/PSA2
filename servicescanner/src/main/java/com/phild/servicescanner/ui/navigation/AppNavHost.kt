package com.phild.servicescanner.ui.navigation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phild.servicescanner.ServiceScannerDependencies
import com.phild.servicescanner.ui.FlyerViewModel
import com.phild.servicescanner.ui.HistoryViewModel
import com.phild.servicescanner.ui.SettingsViewModel
import com.phild.servicescanner.ui.screens.analysing.AnalysingScreen
import com.phild.servicescanner.ui.screens.history.HistoryScreen
import com.phild.servicescanner.ui.screens.home.HomeRoute
import com.phild.servicescanner.ui.screens.output.DocumentCreatedScreen
import com.phild.servicescanner.ui.screens.preview.ImagePreviewScreen
import com.phild.servicescanner.ui.screens.review.ActivityListScreen
import com.phild.servicescanner.ui.screens.review.ReviewScreen
import com.phild.servicescanner.ui.screens.settings.SettingsScreen
import com.phild.servicescanner.ui.state.FlyerUiState

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onExit: (() -> Unit)? = null
) {
    val activity = LocalContext.current as ComponentActivity
    val container = (activity.application as ServiceScannerDependencies).container
    val viewModel: FlyerViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = FlyerViewModel.factory(container)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val atScannerHome = backStackEntry?.destination?.route == AppDestinations.HOME
    val exitScanner = {
        viewModel.chooseAnother()
        onExit?.invoke()
    }

    BackHandler(enabled = onExit != null && atScannerHome) {
        exitScanner()
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is FlyerUiState.Reviewing -> {
                val reviewing = uiState as FlyerUiState.Reviewing
                val current = navController.currentDestination?.route
                val showList = reviewing.isTimetable && reviewing.selectedIndex == null
                val target = if (showList) AppDestinations.ACTIVITY_LIST else AppDestinations.REVIEW
                val fromAnalysisOrHistory = current == AppDestinations.ANALYSING ||
                    current == AppDestinations.HISTORY
                val fromListToReview = current == AppDestinations.ACTIVITY_LIST && !showList
                val fromReviewToList = current == AppDestinations.REVIEW && showList
                if (fromAnalysisOrHistory || fromListToReview || fromReviewToList) {
                    navController.navigate(target) {
                        if (current == AppDestinations.ANALYSING) {
                            popUpTo(AppDestinations.ANALYSING) { inclusive = true }
                        }
                        if (fromReviewToList) {
                            popUpTo(AppDestinations.REVIEW) { inclusive = true }
                        }
                        launchSingleTop = true
                    }
                }
            }
            is FlyerUiState.DocumentGenerated -> {
                val current = navController.currentDestination?.route
                if (current == AppDestinations.REVIEW || current == AppDestinations.ACTIVITY_LIST) {
                    navController.navigate(AppDestinations.DOCUMENT_CREATED) {
                        launchSingleTop = true
                    }
                }
            }
            is FlyerUiState.Idle -> {
                val current = navController.currentDestination?.route
                if (current != AppDestinations.HOME &&
                    current != AppDestinations.HISTORY &&
                    current != AppDestinations.SETTINGS
                ) {
                    navController.popBackStack(AppDestinations.HOME, inclusive = false)
                }
            }
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME,
        modifier = modifier
    ) {
        composable(AppDestinations.HOME) {
            HomeRoute(
                onImageSelected = { uri ->
                    viewModel.onImageSelected(uri)
                    navController.navigate(AppDestinations.imagePreview(uri))
                },
                onTimetableSelected = { uri ->
                    viewModel.analyseTimetable(uri)
                    navController.navigate(AppDestinations.ANALYSING)
                },
                onHistory = { navController.navigate(AppDestinations.HISTORY) },
                onSettings = { navController.navigate(AppDestinations.SETTINGS) },
                onExit = onExit?.let { { exitScanner() } }
            )
        }
        composable(
            route = AppDestinations.IMAGE_PREVIEW_ROUTE,
            arguments = AppDestinations.imagePreviewArguments
        ) { entry ->
            val encoded = entry.arguments?.getString(AppDestinations.ARG_IMAGE_URI).orEmpty()
            val imageUri = Uri.parse(Uri.decode(encoded))
            ImagePreviewScreen(
                imageUri = imageUri,
                onUseImage = { selected ->
                    viewModel.onImageSelected(selected)
                    viewModel.analyseSelectedImage()
                    navController.navigate(AppDestinations.ANALYSING)
                },
                onChooseAnother = {
                    viewModel.chooseAnother()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.ANALYSING) {
            AnalysingScreen(
                uiState = uiState,
                onRetry = { viewModel.retryAnalysis() },
                onChooseAnother = {
                    viewModel.chooseAnother()
                    navController.popBackStack(AppDestinations.HOME, inclusive = false)
                }
            )
        }
        composable(AppDestinations.ACTIVITY_LIST) {
            val reviewing = uiState as? FlyerUiState.Reviewing
            if (reviewing != null) {
                ActivityListScreen(
                    uiState = reviewing,
                    onOpenActivity = viewModel::openActivity,
                    onRemoveActivity = viewModel::removeActivity,
                    onGenerateDocument = viewModel::generateDocument,
                    onGenerationErrorShown = viewModel::clearGenerationError,
                    onBack = {
                        viewModel.chooseAnother()
                        navController.popBackStack(AppDestinations.HOME, inclusive = false)
                    }
                )
            }
        }
        composable(AppDestinations.REVIEW) {
            val reviewing = uiState as? FlyerUiState.Reviewing
            if (reviewing != null) {
                ReviewScreen(
                    uiState = reviewing,
                    onServiceChange = viewModel::updateService,
                    onGenerateDocument = viewModel::generateDocument,
                    onGenerationErrorShown = viewModel::clearGenerationError,
                    onBack = {
                        if (reviewing.isTimetable) {
                            viewModel.closeActivity()
                        } else {
                            navController.popBackStack()
                        }
                    },
                    showGenerateButton = !reviewing.isTimetable
                )
            }
        }
        composable(AppDestinations.DOCUMENT_CREATED) {
            val generated = uiState as? FlyerUiState.DocumentGenerated
            if (generated != null) {
                DocumentCreatedScreen(
                    uiState = generated,
                    onCreateAnother = viewModel::createAnother
                )
            }
        }
        composable(AppDestinations.HISTORY) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(container)
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onOpenEntry = { id -> viewModel.openHistory(id) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
