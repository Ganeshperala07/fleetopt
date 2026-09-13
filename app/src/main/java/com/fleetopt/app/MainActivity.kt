package com.fleetopt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.fleetopt.app.ui.navigation.FleetOptNavGraph
import com.fleetopt.app.ui.navigation.Screen
import com.fleetopt.app.ui.screens.splash.SplashScreen
import com.fleetopt.app.ui.theme.FleetOptTheme
import com.fleetopt.app.ui.viewmodel.FleetOptViewModel
import com.fleetopt.app.ui.viewmodel.FleetOptViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as FleetOptApp
        val viewModel: FleetOptViewModel by viewModels {
            FleetOptViewModelFactory(
                app.repository,
                app.simulationEngine,
                app.adminAuthManager,
                app.themePreferenceManager
            )
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var isSplashFinished by remember { mutableStateOf(false) }

            FleetOptTheme(themeMode = uiState.themeMode) {
                Crossfade(
                    targetState = isSplashFinished,
                    animationSpec = tween(durationMillis = 400),
                    label = "splash_crossfade"
                ) { finished ->
                    if (!finished) {
                        SplashScreen(
                            onSplashComplete = { isSplashFinished = true }
                        )
                    } else {
                        val navController = rememberNavController()

                        // Check for notification deep-link
                        LaunchedEffect(intent) {
                            val navigateTo = intent.getStringExtra("navigate_to")
                            if (navigateTo == "recommendation" || navigateTo == "dispatch") {
                                navController.navigate(Screen.Recommendation.route)
                            }
                        }

                        FleetOptNavGraph(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
