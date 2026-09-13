package com.narayan.portfolioadmin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.narayan.portfolioadmin.data.model.ErrorReport
import com.narayan.portfolioadmin.ui.theme.*

@Composable
fun ErrorLogViewerDialog(
    reports: List<ErrorReport>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onMarkResolved: (String) -> Unit
) {
    var selectedReport by remember { mutableStateOf<ErrorReport?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Error Logs & Telemetry (${reports.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (reports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No errors or crashes detected!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("App is running healthy and clean.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reports, key = { it.id }) { report ->
                            val isExpanded = selectedReport?.id == report.id
                            val (badgeBg, badgeColor) = when (report.error_type) {
                                "CRASH" -> Color(0xFFFEF2F2) to DangerRed
                                "NON_FATAL" -> Color(0xFFFFFBEB) to WarningAmber
                                else -> NavySoft to NavyPrimary
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReport = if (isExpanded) null else report },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = BorderStroke(1.dp, if (isExpanded) NavyBorder else BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = badgeBg,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = report.error_type,
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = report.created_at.take(19).replace("T", " "),
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = report.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NavyPrimary,
                                        maxLines = if (isExpanded) 10 else 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (report.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = report.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = if (isExpanded) 10 else 2
                                        )
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = BorderSubtle)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (report.device_info.isNotBlank()) {
                                            Text("Device:", color = NavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(report.device_info, color = TextSecondary, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        if (report.stack_trace.isNotBlank()) {
                                            Text("Diagnostics / Stack Trace:", color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SurfaceSubtle)
                                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = report.stack_trace,
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (report.status != "resolved") {
                                                TextButton(onClick = { onMarkResolved(report.id) }) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreen)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Resolve", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            TextButton(onClick = { onDelete(report.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = DangerRed)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete", color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
