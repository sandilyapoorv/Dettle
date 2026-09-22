package com.dettle.app.ui.deployments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.data.db.entity.DeploymentEntity
import com.dettle.app.ui.theme.DettleCard
import com.dettle.app.ui.theme.DettleCardBorder
import com.dettle.app.ui.theme.DettleCyan
import com.dettle.app.ui.theme.DettleDark
import com.dettle.app.ui.theme.DettleGreen
import com.dettle.app.ui.theme.DettleOrange
import com.dettle.app.ui.theme.DettleRed
import com.dettle.app.ui.theme.DettleSurface
import com.dettle.app.ui.theme.DettleTextMuted
import com.dettle.app.ui.theme.DettleTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deployments screen — history of all Cloudflare Pages / Workers deployments.
 *
 * Shows:
 * - Cloudflare token status (verified/invalid)
 * - All deployments ordered by most recent
 * - Tap a deployment → opens its live URL in browser
 * - Active deploy progress indicator
 */
@Composable
fun DeploymentsScreen(
    viewModel: DeploymentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val deployments by viewModel.deployments.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DettleDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────
        item {
            Text(
                "Deployments",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Cloudflare Pages • Free • Unlimited",
                style = MaterialTheme.typography.bodySmall,
                color = DettleTextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Token status banner
            CloudflareStatusBanner(
                status = state.tokenStatus,
                isVerifying = state.isVerifying
            )
        }

        // ── Active deploy indicator ──────────────────────────────────────
        if (state.isDeploying) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DettleOrange.copy(alpha = 0.1f))
                        .border(1.dp, DettleOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DettleOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Deploying to Cloudflare Pages...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DettleOrange
                    )
                }
            }
        }

        // ── Last successful URL ──────────────────────────────────────────
        state.lastDeployUrl?.let { url ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DettleGreen.copy(alpha = 0.08f))
                        .border(1.dp, DettleGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌐", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Latest deployment live", style = MaterialTheme.typography.titleSmall, color = DettleGreen, fontWeight = FontWeight.Bold)
                        Text(url, style = MaterialTheme.typography.bodySmall, color = DettleCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // ── Error banner ─────────────────────────────────────────────────
        state.deployError?.let { error ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DettleRed.copy(alpha = 0.1f))
                        .border(1.dp, DettleRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text("Deploy failed: $error", style = MaterialTheme.typography.bodySmall, color = DettleRed)
                }
            }
        }

        // ── Deployment history ───────────────────────────────────────────
        if (deployments.isNotEmpty()) {
            item {
                Text(
                    "HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = DettleTextMuted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(deployments) { deployment ->
                DeploymentCard(deployment, onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(deployment.productionUrl))
                    )
                })
            }
        } else {
            item {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚀", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No deployments yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = DettleTextSecondary
                        )
                        Text(
                            "Ask the agent to build and deploy a website",
                            style = MaterialTheme.typography.bodySmall,
                            color = DettleTextMuted
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun CloudflareStatusBanner(status: String, isVerifying: Boolean) {
    val isValid = status.startsWith("✅")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isVerifying -> DettleCyan.copy(alpha = 0.08f)
                    isValid -> DettleGreen.copy(alpha = 0.08f)
                    else -> DettleRed.copy(alpha = 0.08f)
                }
            )
            .border(
                1.dp,
                when {
                    isVerifying -> DettleCyan.copy(alpha = 0.3f)
                    isValid -> DettleGreen.copy(alpha = 0.3f)
                    else -> DettleRed.copy(alpha = 0.3f)
                },
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVerifying) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DettleCyan, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Verifying Cloudflare token...", style = MaterialTheme.typography.bodySmall, color = DettleCyan)
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isValid) DettleGreen else DettleRed)
            )
            Spacer(Modifier.width(10.dp))
            Text(status, style = MaterialTheme.typography.bodySmall,
                color = if (isValid) DettleGreen else DettleRed)
        }
    }
}

@Composable
fun DeploymentCard(deployment: DeploymentEntity, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(deployment.createdAt))
    val typeBadgeColor = if (deployment.type == "PAGES") DettleCyan else DettlePurple

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DettleCard)
            .border(1.dp, DettleCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeBadgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(deployment.type, style = MaterialTheme.typography.labelSmall, color = typeBadgeColor)
                }
                Spacer(Modifier.width(6.dp))
                Text(deployment.projectName, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Text(deployment.productionUrl, style = MaterialTheme.typography.bodySmall, color = DettleCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (deployment.commitMessage.isNotBlank()) {
                Text(deployment.commitMessage, style = MaterialTheme.typography.bodySmall, color = DettleTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = DettleTextMuted)
        }
        Spacer(Modifier.width(8.dp))
        Text("↗", style = MaterialTheme.typography.titleMedium, color = DettleTextMuted)
    }
}

private val DettlePurple = androidx.compose.ui.graphics.Color(0xFF9B59F5)
