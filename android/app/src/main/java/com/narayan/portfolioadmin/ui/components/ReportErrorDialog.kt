package com.narayan.portfolioadmin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narayan.portfolioadmin.data.tracker.ErrorTracker
import com.narayan.portfolioadmin.ui.theme.*

private val SCREEN_OPTIONS = listOf("General", "Dashboard", "Projects", "Skills", "Profile", "Messages", "Network")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportErrorDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, screen: String, includeDiagnostics: Boolean) -> Unit,
    isSubmitting: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedScreen by remember { mutableStateOf("General") }
    var includeDiagnostics by remember { mutableStateOf(true) }

    val deviceInfo = remember { ErrorTracker.getDeviceInfo() }

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
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Report Issue / Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Describe the bug or unexpected behavior. Your report will be logged to Firestore & Firebase Crashlytics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What went wrong? *") },
                    placeholder = { Text("e.g. Failed saving project thumbnail") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Affected Screen / Area
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Affected Area:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SCREEN_OPTIONS) { screen ->
                            FilterChip(
                                selected = selectedScreen == screen,
                                onClick = { selectedScreen = screen },
                                label = { Text(screen, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SurfaceSubtle,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Details Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Additional Details (Optional)") },
                    placeholder = { Text("Steps to reproduce, error message seen, etc.") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Diagnostics Toggle
                Surface(
                    color = SurfaceSubtle,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Diagnostics",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Device specs, OS version & recent events",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = includeDiagnostics,
                                onCheckedChange = { includeDiagnostics = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NavyPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceSubtle
                                )
                            )
                        }

                        if (includeDiagnostics) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Device: $deviceInfo",
                                fontSize = 10.sp,
                                color = NavyPrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(title, description, selectedScreen, includeDiagnostics)
                    }
                },
                enabled = title.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Report", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite
    )
}
