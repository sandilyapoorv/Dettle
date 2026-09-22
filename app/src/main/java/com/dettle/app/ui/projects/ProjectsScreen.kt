package com.dettle.app.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dettle.app.domain.project.Project
import com.dettle.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel = hiltViewModel(),
    onProjectSelected: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DettleDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = DettleCyan,
                contentColor = DettleDark
            ) {
                Icon(Icons.Filled.Add, "New Project")
            }
        },
        containerColor = DettleDark
    ) { padding ->
        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📁", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No projects yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Create a project to give the AI context.", color = DettleTextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showCreateSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DettleCyan)
                    ) {
                        Text("Create Project", color = DettleDark)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = {
                            viewModel.selectProject(project)
                            onProjectSelected()
                        }
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateProjectSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, desc, emoji ->
                viewModel.createProject(name, desc, emoji, 0xFF00D4FF, "", "")
                showCreateSheet = false
            }
        )
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    val accentColor = Color(project.colorHex)
    val df = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DettleSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(project.emoji, fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    project.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                project.description.ifEmpty { "No description" },
                color = DettleTextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Used ${df.format(Date(project.lastUsedAt))}",
                    color = DettleTextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectSheet(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📁") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DettleDark) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 32.dp)) {
            Text("New Project", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DettleCyan, unfocusedBorderColor = DettleSurface,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                )
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = desc, onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DettleCyan, unfocusedBorderColor = DettleSurface,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                )
            )
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = DettleTextMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (name.isNotBlank()) onCreate(name, desc, emoji) },
                    colors = ButtonDefaults.buttonColors(containerColor = DettleCyan)
                ) {
                    Text("Create", color = DettleDark)
                }
            }
        }
    }
}
