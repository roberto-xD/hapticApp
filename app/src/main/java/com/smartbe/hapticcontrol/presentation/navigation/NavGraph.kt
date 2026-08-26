package com.smartbe.hapticcontrol.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartbe.hapticcontrol.presentation.control.ControlScreen

/**
 * Un solo destino.
 *
 * Las pantallas de escaneo y ajustes desaparecieron con la ruta GATT: estos
 * dispositivos no se escanean ni se emparejan, solo escuchan.
 */
sealed class Screen(val route: String) {
    data object Control : Screen("control")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Control.route,
        modifier = modifier
    ) {
        composable(Screen.Control.route) {
            ControlScreen()
        }
    }
}
