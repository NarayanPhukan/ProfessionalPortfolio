package com.narayan.portfolioadmin.ui.screens.skills

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
                title = { Text("Skills (${skills.size})", color = TextPrimary) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSkill = null
                    showDialog = true
                },
                containerColor = AccentCyan,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Skill")
            }
        },
        containerColor = BackgroundDark
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
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
            title = { Text("Delete Skill") },
            text = { Text("Are you sure you want to delete '${skillToDelete?.name}'?") },
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
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { skillToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceDark
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
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
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
                        color = TextPrimary
                    )
                    Text(
                        text = skill.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                }

                Text(
                    text = "${skill.proficiency}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Proficiency Bar
            LinearProgressIndicator(
                progress = { skill.proficiency / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryIndigo,
                trackColor = CardDark
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (skill == null) "Add Skill" else "Edit Skill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", fontSize = 13.sp, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CATEGORIES.filter { it != "All" }) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Proficiency", fontSize = 13.sp, color = TextSecondary)
                        Text("${proficiency.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                    }
                    Slider(
                        value = proficiency,
                        onValueChange = { proficiency = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryIndigo,
                            activeTrackColor = PrimaryIndigo
                        )
                    )
                }

                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = { displayOrder = it },
                    label = { Text("Display Order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = SurfaceDark
    )
}
