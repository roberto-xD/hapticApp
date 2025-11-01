package com.smartbe.hapticcontrol.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartbe.hapticcontrol.presentation.control.ControlScreen
import com.smartbe.hapticcontrol.presentation.scan.ScanScreen
import com.smartbe.hapticcontrol.presentation.settings.SettingsScreen


/**
 * Rutas de navegación de la aplicación
 */
sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object Control : Screen("control")
    object Settings : Screen("settings")
}

/**
 * Grafo de navegación principal
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Scan.route,
        modifier = modifier
    ) {
        composable(Screen.Scan.route) {
            ScanScreen(
                onDeviceSelected = {

                }
            )
        }

        composable(Screen.Control.route) {
            ControlScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}