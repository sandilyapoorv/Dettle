package com.dettle.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dettle.app.ui.chat.ChatScreen
import com.dettle.app.ui.chat.ChatViewModel
import com.dettle.app.ui.deployments.DeploymentsScreen
import com.dettle.app.ui.onboarding.GamifiedOnboardingScreen
import com.dettle.app.ui.overnight.OvernightScreen
import com.dettle.app.ui.repos.ReposScreen
import com.dettle.app.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Outlined.AutoAwesome)
    object Chat : Screen("chat", "Chat", Icons.Outlined.ChatBubbleOutline)
    object Repos : Screen("repos", "Repos", Icons.Outlined.FolderCopy)
    object Deployments : Screen("deployments", "Deploy", Icons.Outlined.CloudUpload)
    object Overnight : Screen("overnight", "Overnight", Icons.Outlined.Bedtime)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Tune)
}

@Composable
fun DettleNavGraph(
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.profile?.onboardingCompleted) {
        val prof = uiState.profile
        if (prof != null && !prof.onboardingCompleted) {
            if (navController.currentDestination?.route != Screen.Onboarding.route) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            android.util.Log.d("DettleNav", "🚀 [NAV] Current Screen: ${entry.destination.route}")
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Onboarding.route) {
            GamifiedOnboardingScreen(
                onComplete = {
                    chatViewModel.completeOnboarding()
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSaveApiKey = { provider, key ->
                    chatViewModel.addQuickApiKey(provider, key)
                },
                onSaveGitHubToken = { token ->
                    chatViewModel.saveGitHubToken(token)
                },
                onSaveCloudflareToken = { accId, token ->
                    chatViewModel.saveCloudflareToken(accId, token)
                },
                onAnchorProject = { name, repo ->
                    chatViewModel.anchorProject(name, repo)
                },
                onPlaySuccessSound = {
                    chatViewModel.playSuccessSound()
                },
                onPlayLevelUpSound = {
                    chatViewModel.playLevelUpSound()
                }
            )
        }
        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
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
