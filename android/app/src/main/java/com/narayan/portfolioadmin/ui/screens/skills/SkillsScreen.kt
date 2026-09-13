package com.narayan.portfolioadmin.ui.screens.skills

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
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

    val avgProficiency = remember(skills) {
        if (skills.isEmpty()) 0 else skills.sumOf { it.proficiency } / skills.size
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingSkill = null
                    showDialog = true
                },
                containerColor = Color(0xFF0F1E36),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Add Skill", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier.padding(bottom = 80.dp, end = 8.dp)
            )
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. Header: "PORTFOLIO" / "Skills & Stack" / "N indexed"
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
                            text = "Skills & Stack",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1E36)
                        )
                        Text(
                            text = "${filteredSkills.size} indexed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8A99AD)
                        )
                    }
                }
            }

            // 2. Summary Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0F1E36)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL SKILLS", color = Color(0xFF8EA3BF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${skills.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF243652)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AVG PROFICIENCY", color = Color(0xFF8EA3BF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$avgProficiency%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF243652)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CATEGORIES", color = Color(0xFF8EA3BF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val catCount = skills.map { it.category }.distinct().size
                            Text("$catCount", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Segmented Category Rail
            item {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CATEGORIES.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF0F1E36) else Color.Transparent)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Skills List
            items(filteredSkills, key = { it.id }) { skill ->
                SkillCardItem(
                    skill = skill,
                    onEdit = {
                        editingSkill = skill
                        showDialog = true
                    },
                    onDelete = { skillToDelete = skill }
                )
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
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
                    color = Color(0xFF0F1E36)
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
                    Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold)
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
fun SkillCardItem(
    skill: Skill,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, BorderSubtle)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF4F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = skill.name.take(2).uppercase(),
                            color = Color(0xFF0F1E36),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = skill.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1E36)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = skill.category,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "${skill.proficiency}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1E36)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0F1E36), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dual tone progress bar
            LinearProgressIndicator(
                progress = { skill.proficiency / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF0F1E36),
                trackColor = Color(0xFFF1F5F9)
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
                text = if (skill == null) "Add Skill" else "Edit Skill",
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
                                selectedContainerColor = Color(0xFF0F1E36),
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
                        Text("${proficiency.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1E36))
                    }
                    Slider(
                        value = proficiency,
                        onValueChange = { proficiency = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF0F1E36),
                            activeTrackColor = Color(0xFF0F1E36),
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
