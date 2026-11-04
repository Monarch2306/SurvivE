package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Manual : Screen("manual", "Manual", Icons.Default.Book)
    object AI : Screen("ai", "Emergence AI", Icons.Default.Chat)
    object Locator : Screen("locator", "Locator", Icons.Default.Map)
    object Bluetooth : Screen("bluetooth", "Bluetooth", Icons.Default.Bluetooth)
    object Emergency : Screen("emergency", "Emergency", Icons.Default.ContactPhone)
}
