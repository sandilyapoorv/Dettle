package com.dettle.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dettle.app.ui.authvault.AuthVaultScreen
import com.dettle.app.ui.chat.ChatScreen
import com.dettle.app.ui.deployments.DeploymentsScreen
import com.dettle.app.ui.overnight.OvernightScreen
import com.dettle.app.ui.repos.ReposScreen
import com.dettle.app.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Outlined.ChatBubbleOutline)
    object Repos : Screen("repos", "Repos", Icons.Outlined.FolderCopy)
    object Deployments : Screen("deployments", "Deploy", Icons.Outlined.CloudUpload)
    object Agents : Screen("agents", "Vault", Icons.Outlined.VpnKey)
    object Overnight : Screen("overnight", "Overnight", Icons.Outlined.Bedtime)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Tune)
}

@Composable
fun DettleNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToVault = {
                    navController.navigate(Screen.Agents.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Agents.route) {
            AuthVaultScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Repos.route) { ReposScreen() }
        composable(Screen.Deployments.route) { DeploymentsScreen() }
        composable(Screen.Overnight.route) { OvernightScreen() }
    }
}

@Composable
fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
