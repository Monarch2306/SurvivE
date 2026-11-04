package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.ManualScreen
import com.example.myapplication.ui.ai.AiScreen
import com.example.myapplication.ui.screens.LocatorScreen
import com.example.myapplication.ui.screens.BluetoothScreen
import com.example.myapplication.ui.screens.EmergencyScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Manual.route) {
            ManualScreen()
        }
        composable(Screen.AI.route) {
            AiScreen()
        }
        composable(Screen.Locator.route) {
            LocatorScreen()
        }
        composable(Screen.Bluetooth.route) {
            BluetoothScreen()
        }
        composable(Screen.Emergency.route) {
            EmergencyScreen()
        }
    }
}
