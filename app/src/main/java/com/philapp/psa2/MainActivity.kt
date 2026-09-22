package com.philapp.psa2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.philapp.psa2.screens.HomeScreen
import com.philapp.psa2.screens.ResultsScreen
import com.philapp.psa2.screens.ServiceDetailsScreen
import com.philapp.psa2.screens.AddServiceScreen
import com.philapp.psa2.screens.AdminScreen
import com.philapp.psa2.screens.AdminCachedServicesScreen
import com.philapp.psa2.screens.EditServiceScreen
import com.philapp.psa2.screens.TitleScreen
import com.philapp.psa2.viewmodel.SearchViewModel
import com.philapp.psa2.viewmodel.AdminViewModel
import com.philapp.psa2.repository.ServiceRepository
import com.philapp.psa2.ui.theme.PSATheme
import com.philapp.psa2.model.ServiceType
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.philapp.psa2.screens.LocationSelectionScreen
import com.phild.servicescanner.data.firebase.FirestoreTownsRepository
import com.phild.servicescanner.data.geo.TownGeocoder
import com.phild.servicescanner.ui.navigation.AppNavHost
import com.phild.servicescanner.ui.theme.ServiceScannerTheme

class MainActivity : ComponentActivity() {
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var adminViewModel: AdminViewModel
    private lateinit var serviceRepository: ServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository
        serviceRepository = ServiceRepository(
            townsRepository = FirestoreTownsRepository(
                geocoder = TownGeocoder(application)
            )
        )

        // Create ViewModelFactory
        val factory = viewModelFactory {
            initializer {
                SearchViewModel(
                    application = application,
                    serviceRepository = serviceRepository
                )
            }
            initializer {
                AdminViewModel(serviceRepository)
            }
        }

        // Initialize ViewModels
        val viewModelProvider = ViewModelProvider(this, factory)
        searchViewModel = viewModelProvider[SearchViewModel::class.java]
        adminViewModel = viewModelProvider[AdminViewModel::class.java]

        setContent {
            PSATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "title") {
                        composable("title") {
                            TitleScreen(navController = navController)
                        }
                        composable("scanner") {
                            ServiceScannerTheme {
                                AppNavHost(
                                    modifier = Modifier.fillMaxSize(),
                                    onExit = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("home") {
                            HomeScreen(
                                navController = navController,
                                searchViewModel = searchViewModel
                            )
                        }

                        // Add the missing custom_search route
                        composable("custom_search") {
                            LocationSelectionScreen(
                                navController = navController,
                                searchViewModel = searchViewModel
                            )
                        }

                        composable(
                            "results/{type}/{location}",
                            arguments = listOf(
                                navArgument("type") { type = NavType.StringType },
                                navArgument("location") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            ResultsScreen(
                                navController = navController,
                                searchViewModel = searchViewModel,
                                type = backStackEntry.arguments?.getString("type")?.let { ServiceType.valueOf(it) },
                                location = backStackEntry.arguments?.getString("location")
                            )
                        }

                        composable(
                            "results/custom/{search}/{location}",
                            arguments = listOf(
                                navArgument("search") { type = NavType.StringType },
                                navArgument("location") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            ResultsScreen(
                                navController = navController,
                                searchViewModel = searchViewModel,
                                customSearch = backStackEntry.arguments?.getString("search"),
                                location = backStackEntry.arguments?.getString("location")
                            )
                        }

                        composable("details/{serviceId}") { backStackEntry ->
                            ServiceDetailsScreen(
                                navController = navController,
                                serviceId = backStackEntry.arguments?.getString("serviceId") ?: "",
                                searchViewModel = searchViewModel
                            )
                        }

                        composable("add_service") {
                            AddServiceScreen(
                                navController = navController,
                                searchViewModel = searchViewModel
                            )
                        }

                        composable("admin") {
                            AdminScreen(
                                navController = navController,
                                searchViewModel = searchViewModel,
                                viewModel = adminViewModel
                            )
                        }

                        composable("admin_cached_services") {
                            AdminCachedServicesScreen(navController = navController)
                        }

                        composable("edit_service/{serviceId}") { backStackEntry ->
                            EditServiceScreen(
                                navController = navController,
                                serviceId = backStackEntry.arguments?.getString("serviceId") ?: "",
                                viewModel = searchViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}



