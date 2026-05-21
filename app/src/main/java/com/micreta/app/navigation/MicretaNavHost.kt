package com.micreta.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.micreta.app.feature.about.AboutScreen
import com.micreta.app.feature.debug.DebugScreen
import com.micreta.app.feature.driving.DrivingScreen
import com.micreta.app.feature.health.SystemHealthScreen
import com.micreta.app.feature.home.HomeScreen
import com.micreta.app.feature.maintenance.MaintenanceScreen
import com.micreta.app.feature.obd.VehicleStatusScreen
import com.micreta.app.feature.parking.ParkingScreen
import com.micreta.app.feature.refuel.RefuelLogScreen
import com.micreta.app.feature.settings.BluetoothSetupScreen
import com.micreta.app.feature.settings.CustomCommandsScreen
import com.micreta.app.feature.settings.SettingsScreen
import com.micreta.app.feature.trips.TripHistoryScreen
import com.micreta.app.feature.voice.VoiceCommandScreen
import com.micreta.app.service.MicretaForegroundService

/**
 * Top-level navigation.
 *
 * The bottom bar surfaces the 6 most-used routes. The rest (Trips,
 * Maintenance, Refuel, Parking, Custom commands, System health, About,
 * Bluetooth setup) are reached from Home, Settings or by voice.
 *
 * Order chosen for in-car usability: Home → Conducción → Voz → Coche →
 * Ajustes → Más (drawer-like landing page with secondary destinations).
 */
@Composable
fun MicretaNavHost(
    startRoute: String? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Routes.HOME

    // Deep-link from a notification action (P2): "voice" opens voice with
    // auto-listen, "status" opens the vehicle status screen.
    LaunchedEffect(startRoute) {
        when (startRoute) {
            MicretaForegroundService.ROUTE_VOICE -> navController.navigate(Routes.VOICE_AUTO)
            MicretaForegroundService.ROUTE_STATUS -> navController.navigate(Routes.STATUS)
            MicretaForegroundService.ROUTE_REFUEL -> navController.navigate(Routes.REFUEL)
            else -> {}
        }
        if (startRoute != null) onRouteConsumed()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = current.startsWith(item.route.substringBefore("?")),
                        onClick = {
                            if (current != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartVoice = { navController.navigate(Routes.VOICE_AUTO) },
                    onOpenWaze = { navController.navigate(Routes.VOICE_AUTO) },
                    onOpenVehicleStatus = { navController.navigate(Routes.STATUS) },
                    onOpenTrips = { navController.navigate(Routes.TRIPS) },
                    onOpenMaintenance = { navController.navigate(Routes.MAINTENANCE) },
                    onOpenRefuel = { navController.navigate(Routes.REFUEL) },
                    onOpenParking = { navController.navigate(Routes.PARKING) }
                )
            }
            composable(Routes.DRIVING) {
                DrivingScreen(onStartVoice = { navController.navigate(Routes.VOICE_AUTO) })
            }
            composable(
                route = "voice?auto={auto}",
                arguments = listOf(navArgument("auto") { type = NavType.StringType; defaultValue = "false"; nullable = true })
            ) { entry ->
                val auto = entry.arguments?.getString("auto") == "true"
                VoiceCommandScreen(autoStart = auto)
            }
            composable(Routes.STATUS) { VehicleStatusScreen() }
            composable(Routes.TRIPS) { TripHistoryScreen() }
            composable(Routes.MAINTENANCE) { MaintenanceScreen() }
            composable(Routes.REFUEL) { RefuelLogScreen() }
            composable(Routes.PARKING) { ParkingScreen() }
            composable(Routes.SYSTEM_HEALTH) { SystemHealthScreen() }
            composable(Routes.CUSTOM_COMMANDS) { CustomCommandsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenBluetoothSetup = { navController.navigate(Routes.BLUETOOTH) },
                    onOpenCustomCommands = { navController.navigate(Routes.CUSTOM_COMMANDS) },
                    onOpenSystemHealth = { navController.navigate(Routes.SYSTEM_HEALTH) },
                    onOpenDebug = { navController.navigate(Routes.DEBUG) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) }
                )
            }
            composable(Routes.BLUETOOTH) { BluetoothSetupScreen() }
            composable(Routes.DEBUG) { DebugScreen() }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.DRIVING, "Conducción", Icons.Filled.DirectionsCar),
    BottomItem("voice?auto=false", "Voz", Icons.Filled.Mic),
    BottomItem(Routes.STATUS, "Coche", Icons.Filled.Speed),
    BottomItem(Routes.TRIPS, "Viajes", Icons.Filled.Route),
    BottomItem(Routes.SETTINGS, "Ajustes", Icons.Filled.Settings),
)
