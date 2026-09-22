package com.dettle.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dettle.app.ui.agents.AgentsScreen
import com.dettle.app.ui.authvault.AuthVaultScreen
import com.dettle.app.ui.chat.ChatScreen
import com.dettle.app.ui.deployments.DeploymentsScreen
import com.dettle.app.ui.overnight.OvernightScreen
import com.dettle.app.ui.repos.ReposScreen
import com.dettle.app.ui.settings.SettingsScreen
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    object Repos : Screen("repos", "Repos", Icons.Filled.Code)
    object Deployments : Screen("deployments", "Deploy", Icons.Filled.Rocket)
    object Agents : Screen("agents", "Vault", Icons.Filled.SmartToy)
    object Overnight : Screen("overnight", "Overnight", Icons.Filled.Bedtime)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Chat,
    Screen.Repos,
    Screen.Deployments,
    Screen.Agents,
    Screen.Overnight,
    Screen.Settings
)

@Composable
fun DettleNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = DettleDark,
        bottomBar = {
            NavigationBar(
                containerColor = DettleSurface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                tint = if (selected) DettleCyan else DettleTextMuted
                            )
                        },
                        label = {
                            Text(
                                screen.label,
                                color = if (selected) DettleCyan else DettleTextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = DettleCyan.copy(alpha = 0.15f),
                            selectedIconColor = DettleCyan,
                            unselectedIconColor = DettleTextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Repos.route) { ReposScreen() }
            composable(Screen.Deployments.route) { DeploymentsScreen() }
            composable(Screen.Agents.route) { AuthVaultScreen() }
            composable(Screen.Overnight.route) { OvernightScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

@Composable
fun PlaceholderScreen(label: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(label, color = DettleTextSecondary)
    }
}

private val androidx.compose.ui.Modifier.Companion.fillMaxSize: () -> androidx.compose.ui.Modifier
    get() = { androidx.compose.foundation.layout.fillMaxSize() }
