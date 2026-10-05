package com.dettle.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dettle.app.domain.model.AIProviderType
import kotlinx.coroutines.launch

/**
 * Universal 4-Quest Gamified Onboarding Experience ("Awaken the Machine").
 * Styled with Apple Liquid Glass and Obsidian Dark aesthetics.
 */
@Composable
fun GamifiedOnboardingScreen(
    onComplete: () -> Unit,
    onSaveApiKey: (AIProviderType, String) -> Unit,
    onSaveGitHubToken: (String) -> Unit,
    onSaveCloudflareToken: (String, String) -> Unit,
    onAnchorProject: suspend (String, String) -> Unit,
    onPlaySuccessSound: () -> Unit,
    onPlayLevelUpSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentQuest by remember { mutableIntStateOf(1) } // 1..4

    // Quest 1 inputs
    var geminiKey by remember { mutableStateOf("") }
    var groqKey by remember { mutableStateOf("") }
    var quest1Done by remember { mutableStateOf(false) }

    // Quest 2 inputs
    var githubToken by remember { mutableStateOf("") }
    var cfAccountId by remember { mutableStateOf("") }
    var cfToken by remember { mutableStateOf("") }
    var quest2Done by remember { mutableStateOf(false) }

    // Quest 3 inputs
    var projectName by remember { mutableStateOf("My First Agent Project") }
    var linkedRepo by remember { mutableStateOf("sandilyapoorv/Dettle") }
    var quest3Done by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = when (currentQuest) {
            1 -> 0.25f
            2 -> 0.50f
            3 -> 0.75f
            else -> 1.0f
        },
        animationSpec = tween(300),
        label = "questProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)) // Obsidian background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Companion Header
            Surface(
                shape = CircleShape,
                color = Color(0xFF1C1C1E),
                border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.5f)),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⚡", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "AWAKEN DETTLE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Color.White
            )

            Text(
                text = "Quest $currentQuest of 4: ${
                    when (currentQuest) {
                        1 -> "Neural Core Uplink"
                        2 -> "Bare-Metal Bridge"
                        3 -> "Anchor Your Kingdom"
                        else -> "First Light Awakening"
                    }
                }",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF007AFF)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF007AFF),
                trackColor = Color(0xFF27272A),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sally Companion Roast / Guidance Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141417),
                border = BorderStroke(1.dp, Color(0xFF27272A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🤖", fontSize = 24.sp)
                    Text(
                        text = when (currentQuest) {
                            1 -> "I'm Sally. Before we can write a single line of code, plug in at least one free AI key. Don't be lazy."
                            2 -> "Good. Now hook up GitHub & Cloudflare so I have actual hands to deploy and commit."
                            3 -> "Every sovereign engineer needs a kingdom. Pick or name your target repository."
                            else -> "All systems live. Tap Awaken and prepare to ship like a 10x architect."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quest Step Bodies
            when (currentQuest) {
                1 -> {
                    // QUEST 1: NEURAL CORE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Connect Free AI Provider",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = geminiKey,
                            onValueChange = { geminiKey = it },
                            label = { Text("Google AI Studio (Gemini) API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFF27272A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get Free Gemini Key", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get Free Groq Key", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (geminiKey.isNotBlank()) {
                                    onSaveApiKey(AIProviderType.GEMINI, geminiKey.trim())
                                }
                                if (groqKey.isNotBlank()) {
                                    onSaveApiKey(AIProviderType.GROQ, groqKey.trim())
                                }
                                quest1Done = true
                                onPlaySuccessSound()
                                currentQuest = 2
                            },
                            enabled = geminiKey.isNotBlank() || groqKey.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Test Neural Pulse (+50 XP) ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // QUEST 2: BARE METAL BRIDGE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "GitHub & Cloudflare Access",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = githubToken,
                            onValueChange = { githubToken = it },
                            label = { Text("GitHub Personal Access Token (PAT)") },
                            placeholder = { Text("ghp_...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFF27272A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = cfToken,
                            onValueChange = { cfToken = it },
                            label = { Text("Cloudflare API Token (Optional)") },
                            placeholder = { Text("Pages & Workers deploy token") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFF27272A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (githubToken.isNotBlank()) onSaveGitHubToken(githubToken.trim())
                                if (cfToken.isNotBlank()) onSaveCloudflareToken(cfAccountId.trim(), cfToken.trim())
                                quest2Done = true
                                onPlaySuccessSound()
                                currentQuest = 3
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Establish Uplink (+100 XP) ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                3 -> {
                    // QUEST 3: ANCHOR YOUR KINGDOM
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Anchor Primary Project",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            label = { Text("Project Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFF27272A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = linkedRepo,
                            onValueChange = { linkedRepo = it },
                            label = { Text("Linked GitHub Repo (owner/repo)") },
                            placeholder = { Text("username/repo-name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFF27272A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    onAnchorProject(projectName.trim(), linkedRepo.trim())
                                    quest3Done = true
                                    onPlaySuccessSound()
                                    currentQuest = 4
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Claim Kingdom (+50 XP) ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                4 -> {
                    // QUEST 4: FIRST LIGHT
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "🎉 Quest Completed!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "You've earned 250 XP and unlocked Level 1: Script Apprentice.\nSally is ready in the cockpit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA1A1AA),
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF18181B),
                            border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔥 1-Day Streak Activated", fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                                Text("⚡ Level 1: Script Apprentice", fontSize = 12.sp, color = Color(0xFF007AFF))
                            }
                        }

                        Button(
                            onClick = {
                                onPlayLevelUpSound()
                                onComplete()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)), // Apple Success Green
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Awaken Dettle (Enter Cockpit) 🚀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
