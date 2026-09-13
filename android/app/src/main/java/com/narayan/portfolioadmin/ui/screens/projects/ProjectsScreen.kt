package com.narayan.portfolioadmin.ui.screens.projects

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.narayan.portfolioadmin.data.model.Project
import com.narayan.portfolioadmin.data.repository.ProjectsRepository
import com.narayan.portfolioadmin.data.repository.StorageRepository
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projectsRepository: ProjectsRepository,
    storageRepository: StorageRepository,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by projectsRepository.getProjectsFlow().collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Projects (${projects.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NavyPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = BorderSubtle,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProject = null
                    showDialog = true
                },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NavyPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onEdit = {
                            editingProject = project
                            showDialog = true
                        },
                        onDelete = { projectToDelete = project }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showDialog) {
        ProjectEditDialog(
            project = editingProject,
            storageRepository = storageRepository,
            onDismiss = { showDialog = false },
            onSave = { savedProject ->
                coroutineScope.launch {
                    val res = projectsRepository.saveProject(savedProject)
                    showDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Project saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save project", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = {
                Text(
                    text = "Delete Project",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${projectToDelete?.title}'?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = projectToDelete?.id ?: ""
                        coroutineScope.launch {
                            projectsRepository.deleteProject(id)
                            projectToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (project.image_url.isNotBlank()) {
                AsyncImage(
                    model = com.narayan.portfolioadmin.data.util.ImageUtils.parseImageModel(project.image_url),
                    contentDescription = project.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (project.featured) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = "★ Featured",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (project.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (project.tech_stack.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(project.tech_stack) { tech ->
                            Surface(
                                color = NavySoft,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, NavyBorder.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = tech,
                                    color = NavyPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditDialog(
    project: Project?,
    storageRepository: StorageRepository,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf(project?.title ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var longDescription by remember { mutableStateOf(project?.long_description ?: "") }
    var imageUrl by remember { mutableStateOf(project?.image_url ?: "") }
    var techStackInput by remember { mutableStateOf(project?.tech_stack?.joinToString(", ") ?: "") }
    var liveUrl by remember { mutableStateOf(project?.live_url ?: "") }
    var githubUrl by remember { mutableStateOf(project?.github_url ?: "") }
    var featured by remember { mutableStateOf(project?.featured ?: false) }
    var displayOrder by remember { mutableStateOf(project?.display_order?.toString() ?: "0") }

    var isUploading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val result = storageRepository.uploadImage(context, uri, "projects")
                isUploading = false
                if (result.isSuccess) {
                    imageUrl = result.getOrThrow()
                    Toast.makeText(context, "Image processed successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to upload image"
                    Toast.makeText(context, "Image error: $err", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NavyPrimary,
        unfocusedBorderColor = BorderSubtle,
        focusedLabelColor = NavyPrimary,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = SurfaceWhite,
        cursorColor = NavyPrimary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (project == null) "Add Project" else "Edit Project",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image preview & upload button
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = com.narayan.portfolioadmin.data.util.ImageUtils.parseImageModel(imageUrl),
                        contentDescription = "Project Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Image...")
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (imageUrl.isBlank()) "Upload Image" else "Change Image")
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    placeholder = { Text("https://... or photo data URI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = longDescription,
                    onValueChange = { longDescription = it },
                    label = { Text("Detailed Description / Features") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = techStackInput,
                    onValueChange = { techStackInput = it },
                    label = { Text("Tech Stack (comma-separated)") },
                    placeholder = { Text("React, Node.js, PostgreSQL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = liveUrl,
                    onValueChange = { liveUrl = it },
                    label = { Text("Live Website URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = githubUrl,
                    onValueChange = { githubUrl = it },
                    label = { Text("GitHub Repository URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = { displayOrder = it },
                    label = { Text("Display Order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Featured Project", color = TextPrimary, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = featured,
                        onCheckedChange = { featured = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NavyPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceSubtle
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val tags = techStackInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val toSave = Project(
                            id = project?.id ?: "",
                            title = title.trim(),
                            description = description.trim(),
                            long_description = longDescription.trim(),
                            image_url = imageUrl.trim(),
                            tech_stack = tags,
                            live_url = liveUrl.trim(),
                            github_url = githubUrl.trim(),
                            featured = featured,
                            display_order = displayOrder.toIntOrNull() ?: 0,
                            created_at = project?.created_at ?: ""
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank() && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite
    )
}
