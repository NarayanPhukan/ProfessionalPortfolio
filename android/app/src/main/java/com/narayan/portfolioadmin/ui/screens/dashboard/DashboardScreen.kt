package com.narayan.portfolioadmin.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.narayan.portfolioadmin.data.model.AppUpdateInfo
import com.narayan.portfolioadmin.data.model.ContactMessage
import com.narayan.portfolioadmin.data.model.Profile
import com.narayan.portfolioadmin.data.model.Project
import com.narayan.portfolioadmin.data.model.Skill
import com.narayan.portfolioadmin.data.model.UpdateCheckResult
import com.narayan.portfolioadmin.data.repository.*
import com.narayan.portfolioadmin.data.tracker.ErrorTracker
import com.narayan.portfolioadmin.data.updater.UpdateManager
import com.narayan.portfolioadmin.ui.components.ErrorLogViewerDialog
import com.narayan.portfolioadmin.ui.components.ReportErrorDialog
import com.narayan.portfolioadmin.ui.components.UpdateDialog
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    projectsRepository: ProjectsRepository,
    skillsRepository: SkillsRepository,
    messagesRepository: MessagesRepository,
    errorReportRepository: ErrorReportRepository = remember { ErrorReportRepository() },
    onNavigateToProjects: () -> Unit,
    onNavigateToSkills: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profile by profileRepository.getProfileFlow().collectAsState(initial = null)
    val projects by projectsRepository.getProjectsFlow().collectAsState(initial = emptyList())
    val skills by skillsRepository.getSkillsFlow().collectAsState(initial = emptyList())
    val messages by messagesRepository.getMessagesFlow().collectAsState(initial = emptyList())
    val errorReports by errorReportRepository.getErrorReportsFlow().collectAsState(initial = emptyList())

    var showReportDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    // Auto-update states
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val unreadCount = messages.count { !it.is_read }

    // Auto-detect & report pending crashes from previous session on startup
    LaunchedEffect(Unit) {
        val pendingCrash = ErrorTracker.checkAndUploadPendingCrash(context)
        if (pendingCrash != null) {
            snackbarHostState.showSnackbar(
                message = "Auto-detected crash from last session was logged and reported.",
                duration = SnackbarDuration.Long
            )
        }

        // Auto-check for updates on app startup
        try {
            val updateRes = UpdateManager.checkForUpdates(context)
            if (updateRes is UpdateCheckResult.UpdateAvailable) {
                availableUpdate = updateRes.info
                showUpdateDialog = true
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Live Portfolio Control",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Update check button
                    IconButton(onClick = {
                        coroutineScope.launch {
                            isCheckingUpdate = true
                            when (val res = UpdateManager.checkForUpdates(context)) {
                                is UpdateCheckResult.UpdateAvailable -> {
                                    availableUpdate = res.info
                                    showUpdateDialog = true
                                }
                                is UpdateCheckResult.UpToDate -> {
                                    snackbarHostState.showSnackbar("App is up to date (v${res.currentVersion})")
                                }
                                is UpdateCheckResult.Error -> {
                                    snackbarHostState.showSnackbar("Update check failed: ${res.message}")
                                }
                            }
                            isCheckingUpdate = false
                        }
                    }) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AccentCyan
                            )
                        } else {
                            BadgedBox(
                                badge = {
                                    if (availableUpdate != null) {
                                        Badge(containerColor = AccentCyan)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Check for Updates",
                                    tint = if (availableUpdate != null) AccentCyan else TextMuted
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Report Issue",
                            tint = WarningAmber
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Update Available Alert Banner
            if (availableUpdate != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUpdateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Available: v${availableUpdate?.versionName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AccentCyan
                                )
                                Text(
                                    text = "Tap to view changelog & update",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = { showUpdateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Update",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            // Profile Summary Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToProfile),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarUrl = profile?.avatar_url
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile?.name?.take(2)?.uppercase() ?: "NP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.name ?: "Narayan Phukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = profile?.title ?: "Student & Developer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile?.available_for_hire == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Available for Hire",
                                        fontSize = 11.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Edit Profile",
                            tint = TextMuted
                        )
                    }
                }
            }

            // Stats Grid (2 rows of 2 cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Projects",
                            count = projects.size.toString(),
                            icon = Icons.Default.Folder,
                            iconTint = PrimaryIndigo,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToProjects
                        )
                        StatCard(
                            title = "Skills Listed",
                            count = skills.size.toString(),
                            icon = Icons.Default.Star,
                            iconTint = AccentCyan,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSkills
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Messages",
                            count = messages.size.toString(),
                            icon = Icons.Default.Mail,
                            iconTint = AccentPurple,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMessages
                        )
                        StatCard(
                            title = "Unread Messages",
                            count = unreadCount.toString(),
                            icon = Icons.Default.MarkEmailUnread,
                            iconTint = if (unreadCount > 0) WarningAmber else SuccessGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMessages
                        )
                    }
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToProjects,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CardDark,
                            contentColor = PrimaryIndigo
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Project")
                    }

                    FilledTonalButton(
                        onClick = onNavigateToSkills,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CardDark,
                            contentColor = AccentCyan
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Skill")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CardDark,
                            contentColor = WarningAmber
                        )
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Report Bug")
                    }

                    FilledTonalButton(
                        onClick = { showLogsDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CardDark,
                            contentColor = if (errorReports.isNotEmpty()) DangerRed else TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (errorReports.isNotEmpty()) "Errors (${errorReports.size})" else "Diagnostics")
                    }
                }
            }

            // Recent Messages Preview
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Messages",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    TextButton(onClick = onNavigateToMessages) {
                        Text("View All (${messages.size})", color = PrimaryIndigo, fontSize = 13.sp)
                    }
                }

                if (messages.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages received yet.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        messages.take(3).forEach { msg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onNavigateToMessages),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!msg.is_read) CardDark else SurfaceDark
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!msg.is_read) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(WarningAmber)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = msg.name,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (msg.is_read) "Read" else "New",
                                                color = if (msg.is_read) TextMuted else WarningAmber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = msg.subject.ifBlank { msg.message },
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showReportDialog) {
        ReportErrorDialog(
            onDismiss = { showReportDialog = false },
            isSubmitting = isSubmittingReport,
            onSubmit = { title, description, screen, includeDiagnostics ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val res = ErrorTracker.submitUserReport(
                        title = title,
                        description = description,
                        screenName = screen,
                        includeDiagnostics = includeDiagnostics
                    )
                    isSubmittingReport = false
                    showReportDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Issue report submitted. Thank you!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showLogsDialog) {
        ErrorLogViewerDialog(
            reports = errorReports,
            onDismiss = { showLogsDialog = false },
            onDelete = { id ->
                coroutineScope.launch {
                    errorReportRepository.deleteReport(id)
                }
            },
            onMarkResolved = { id ->
                coroutineScope.launch {
                    errorReportRepository.markResolved(id)
                }
            }
        )
    }

    if (showUpdateDialog && availableUpdate != null) {
        val update = availableUpdate!!
        UpdateDialog(
            updateInfo = update,
            currentVersion = UpdateManager.getCurrentVersionName(context),
            isDownloading = isDownloadingUpdate,
            downloadProgress = downloadProgress,
            downloadError = downloadError,
            onDismiss = {
                showUpdateDialog = false
                downloadError = null
            },
            onInstall = {
                coroutineScope.launch {
                    isDownloadingUpdate = true
                    downloadError = null
                    downloadProgress = 0f
                    val result = UpdateManager.downloadAndInstallApk(
                        context = context,
                        downloadUrl = update.apkUrl,
                        onProgress = { progress ->
                            downloadProgress = progress
                        }
                    )
                    isDownloadingUpdate = false
                    if (result.isFailure) {
                        downloadError = result.exceptionOrNull()?.localizedMessage ?: "Failed to download update"
                    }
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = count,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
