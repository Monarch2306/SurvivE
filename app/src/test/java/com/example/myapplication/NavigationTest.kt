package com.example.myapplication

import com.example.myapplication.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {
    @Test
    fun testRoutes() {
        assertEquals("home", Screen.Home.route)
        assertEquals("manual", Screen.Manual.route)
        assertEquals("ai", Screen.AI.route)
        assertEquals("locator", Screen.Locator.route)
        assertEquals("bluetooth", Screen.Bluetooth.route)
        assertEquals("emergency", Screen.Emergency.route)
    }
}
