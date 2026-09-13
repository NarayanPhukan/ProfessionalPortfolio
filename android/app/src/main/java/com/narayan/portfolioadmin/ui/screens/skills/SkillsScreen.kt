package com.narayan.portfolioadmin.ui.screens.skills

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narayan.portfolioadmin.data.model.Skill
import com.narayan.portfolioadmin.data.repository.SkillsRepository
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("All", "Languages", "Frontend", "Backend", "Database", "Tools", "DevOps", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    skillsRepository: SkillsRepository,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val skills by skillsRepository.getSkillsFlow().collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf("All") }
    var showDialog by remember { mutableStateOf(false) }
    var editingSkill by remember { mutableStateOf<Skill?>(null) }
    var skillToDelete by remember { mutableStateOf<Skill?>(null) }

    val filteredSkills = remember(skills, selectedCategory) {
        if (selectedCategory == "All") skills else skills.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Skills (${skills.size})",
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
                    editingSkill = null
                    showDialog = true
                },
                containerColor = NavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Skill")
            }
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category Filter Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 13.sp, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceWhite,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedCategory == cat) NavyPrimary else BorderSubtle,
                            selectedBorderColor = NavyPrimary,
                            enabled = true,
                            selected = selectedCategory == cat
                        )
                    )
                }
            }

            // Skills List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }
                items(filteredSkills, key = { it.id }) { skill ->
                    SkillCard(
                        skill = skill,
                        onEdit = {
                            editingSkill = skill
                            showDialog = true
                        },
                        onDelete = { skillToDelete = skill }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showDialog) {
        SkillEditDialog(
            skill = editingSkill,
            onDismiss = { showDialog = false },
            onSave = { savedSkill ->
                coroutineScope.launch {
                    val res = skillsRepository.saveSkill(savedSkill)
                    showDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Skill saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save skill", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (skillToDelete != null) {
        AlertDialog(
            onDismissRequest = { skillToDelete = null },
            title = {
                Text(
                    text = "Delete Skill",
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${skillToDelete?.name}'?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = skillToDelete?.id ?: ""
                        coroutineScope.launch {
                            skillsRepository.deleteSkill(id)
                            skillToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { skillToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }
}

@Composable
fun SkillCard(
    skill: Skill,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = skill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = NavySoft,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = skill.category,
                            color = NavyPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${skill.proficiency}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proficiency Bar
            LinearProgressIndicator(
                progress = { skill.proficiency / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NavyPrimary,
                trackColor = SurfaceSubtle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillEditDialog(
    skill: Skill?,
    onDismiss: () -> Unit,
    onSave: (Skill) -> Unit
) {
    var name by remember { mutableStateOf(skill?.name ?: "") }
    var category by remember { mutableStateOf(skill?.category ?: "Frontend") }
    var proficiency by remember { mutableFloatStateOf(skill?.proficiency?.toFloat() ?: 80f) }
    var displayOrder by remember { mutableStateOf(skill?.display_order?.toString() ?: "0") }

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
                text = if (skill == null) "Add Skill" else "Edit Skill",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )

                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CATEGORIES.filter { it != "All" }) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceSubtle,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Proficiency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${proficiency.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                    Slider(
                        value = proficiency,
                        onValueChange = { proficiency = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NavyPrimary,
                            activeTrackColor = NavyPrimary,
                            inactiveTrackColor = SurfaceSubtle
                        )
                    )
                }

                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = { displayOrder = it },
                    label = { Text("Display Order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val toSave = Skill(
                            id = skill?.id ?: "",
                            name = name.trim(),
                            category = category,
                            proficiency = proficiency.toInt(),
                            display_order = displayOrder.toIntOrNull() ?: 0,
                            created_at = skill?.created_at ?: ""
                        )
                        onSave(toSave)
                    }
                },
                enabled = name.isNotBlank(),
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
