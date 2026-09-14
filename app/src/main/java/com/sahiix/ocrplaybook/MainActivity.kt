package com.sahiix.ocrplaybook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sahiix.ocrplaybook.ui.navigation.Route
import com.sahiix.ocrplaybook.ui.screens.HistoryScreen
import com.sahiix.ocrplaybook.ui.screens.ReportScreen
import com.sahiix.ocrplaybook.ui.screens.ScannerScreen
import com.sahiix.ocrplaybook.ui.screens.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7DD3FC),
                    secondary = Color(0xFF38BDF8),
                    background = Color(0xFF0B1220),
                    surface = Color(0xFF111C33)
                )
            ) { AppNav() }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = current == Route.Scanner.path,
                    onClick = { nav.navigate(Route.Scanner.path) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.PhotoCamera, null) },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = current == Route.History.path,
                    onClick = { nav.navigate(Route.History.path) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = current?.startsWith("report/") == true,
                    onClick = { },
                    enabled = false,
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text("Report") }
                )
                NavigationBarItem(
                    selected = current == Route.Settings.path,
                    onClick = { nav.navigate(Route.Settings.path) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Route.Scanner.path,
            modifier = Modifier.padding(pad)
        ) {
            composable(Route.Scanner.path) {
                ScannerScreen(onScanDone = { id -> nav.navigate(Route.Report.forId(id)) })
            }
            composable(Route.History.path) {
                HistoryScreen(onOpen = { id -> nav.navigate(Route.Report.forId(id)) })
            }
            composable(
                Route.Report.path,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType })
            ) { entry ->
                ReportScreen(scanId = entry.arguments?.getLong("scanId") ?: 0L)
            }
            composable(Route.Settings.path) { SettingsScreen() }
        }
    }
}
