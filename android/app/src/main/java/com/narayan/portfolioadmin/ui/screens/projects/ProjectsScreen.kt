package com.narayan.portfolioadmin.ui.screens.projects

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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

private val FILTER_TABS = listOf("All", "Featured", "Web", "Mobile")

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

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    val filteredProjects = remember(projects, searchQuery, selectedFilter) {
        projects.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.title.contains(searchQuery, ignoreCase = true) ||
                    p.tech_stack.any { it.contains(searchQuery, ignoreCase = true) } ||
                    p.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Featured" -> p.featured
                "Web" -> p.tech_stack.any { it.contains("Web", ignoreCase = true) || it.contains("React", ignoreCase = true) || it.contains("Next", ignoreCase = true) || it.contains("HTML", ignoreCase = true) } || p.title.contains("Web", ignoreCase = true) || p.description.contains("Web", ignoreCase = true)
                "Mobile" -> p.tech_stack.any { it.contains("Mobile", ignoreCase = true) || it.contains("Android", ignoreCase = true) || it.contains("iOS", ignoreCase = true) || it.contains("Native", ignoreCase = true) || it.contains("Flutter", ignoreCase = true) } || p.title.contains("Mobile", ignoreCase = true) || p.description.contains("Mobile", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val listState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 60
        }
    }
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProject = null
                    showDialog = true
                },
                expanded = isFabExpanded,
                containerColor = Color(0xFF0F1E36),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Project",
                        tint = Color.White,
                        modifier = Modifier.rotate(fabRotation)
                    )
                },
                text = { Text("Add Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier.padding(bottom = 80.dp, end = 8.dp)
            )
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. Header: "PORTFOLIO" / "Projects" / "N shown"
            item {
                Column {
                    Text(
                        text = "PORTFOLIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A99AD),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Projects",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1E36)
                        )
                        Text(
                            text = "${filteredProjects.size} shown",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8A99AD)
                        )
                    }
                }
            }

            // 2. Search Bar
            item {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF8A99AD),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search projects or tech...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color(0xFF0F1E36),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF8A99AD),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Segmented Filter Control
            item {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FILTER_TABS.forEach { tab ->
                            val isSelected = selectedFilter == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF0F1E36) else Color.Transparent)
                                    .clickable { selectedFilter = tab }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Projects Cards
            if (filteredProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No projects matching '$searchQuery'" else "No projects in this category.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectCardItem(
                        project = project,
                        onEdit = {
                            editingProject = project
                            showDialog = true
                        },
                        onToggleVisibility = {
                            coroutineScope.launch {
                                val updated = project.copy(featured = !project.featured)
                                projectsRepository.saveProject(updated)
                            }
                        },
                        onDelete = { projectToDelete = project }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
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
                    color = Color(0xFF0F1E36)
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
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold)
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
fun ProjectCardItem(
    project: Project,
    onEdit: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Cover Image with Floating Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (project.image_url.isNotBlank()) {
                    AsyncImage(
                        model = com.narayan.portfolioadmin.data.util.ImageUtils.parseImageModel(project.image_url),
                        contentDescription = project.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    }
                }

                // Top-Right: Featured Badge
                if (project.featured) {
                    Surface(
                        color = Color(0xDD0F1E36),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "★ Featured",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    // Top-Left: Hidden / Draft Badge (matches Image 3)
                    Surface(
                        color = Color(0xDD1E293B),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hidden",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Card Body
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F1E36),
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (project.github_url.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(project.github_url)))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = "GitHub", tint = Color(0xFF0F1E36), modifier = Modifier.size(18.dp))
                            }
                        }
                        if (project.live_url.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(project.live_url)))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Live Demo", tint = Color(0xFF0F1E36), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (project.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.description,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tech Stack Tags
                if (project.tech_stack.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(project.tech_stack) { tech ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = tech,
                                    color = Color(0xFF0F1E36),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(4.dp))

                // Card Footer: 3 Actions (Edit Details, Visible/Hidden, Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Edit Details
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onEdit)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0F1E36), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Edit Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F1E36))
                    }

                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(BorderSubtle))

                    // 2. Visible / Hidden Toggle
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onToggleVisibility)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (project.featured) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF0F1E36), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Visible", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F1E36))
                        } else {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Hidden", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(BorderSubtle))

                    // 3. Delete Action
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDelete)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
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
                    Toast.makeText(context, "Image processed!", Toast.LENGTH_SHORT).show()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to upload image"
                    Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF0F1E36),
        unfocusedBorderColor = BorderSubtle,
        focusedLabelColor = Color(0xFF0F1E36),
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = SurfaceWhite,
        unfocusedContainerColor = SurfaceWhite,
        cursorColor = Color(0xFF0F1E36)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (project == null) "Add Project" else "Edit Project",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F1E36)
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F1E36))
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0F1E36))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Image...")
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF0F1E36))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (imageUrl.isBlank()) "Upload Cover Image" else "Change Cover Image")
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
                    placeholder = { Text("React, TypeScript, Node.js, PostgreSQL") },
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
                    Text("Featured / Visible", color = TextPrimary, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = featured,
                        onCheckedChange = { featured = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0F1E36),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1E36))
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
