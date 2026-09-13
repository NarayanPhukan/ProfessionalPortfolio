package com.narayan.portfolioadmin.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import coil.imageLoader
import com.narayan.portfolioadmin.data.model.AppUpdateInfo
import com.narayan.portfolioadmin.data.model.ErrorReport
import com.narayan.portfolioadmin.data.model.Profile
import com.narayan.portfolioadmin.data.model.UpdateCheckResult
import com.narayan.portfolioadmin.data.notification.NotificationHelper
import com.narayan.portfolioadmin.data.notification.NotificationPreferences
import com.narayan.portfolioadmin.data.repository.AuthRepository
import com.narayan.portfolioadmin.data.repository.ErrorReportRepository
import com.narayan.portfolioadmin.data.repository.ProfileRepository
import com.narayan.portfolioadmin.data.repository.StorageRepository
import com.narayan.portfolioadmin.data.tracker.ErrorTracker
import com.narayan.portfolioadmin.data.updater.UpdateManager
import com.narayan.portfolioadmin.ui.components.ErrorLogViewerDialog
import com.narayan.portfolioadmin.ui.components.ReportErrorDialog
import com.narayan.portfolioadmin.ui.components.UpdateDialog
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileRepository: ProfileRepository,
    storageRepository: StorageRepository,
    authRepository: AuthRepository = remember { AuthRepository() },
    errorReportRepository: ErrorReportRepository = remember { ErrorReportRepository() },
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val initialProfile by profileRepository.getProfileFlow().collectAsState(initial = null)
    val errorReports by errorReportRepository.getErrorReportsFlow().collectAsState(initial = emptyList())

    // Notification preferences
    val notificationPrefs = remember { NotificationPreferences(context) }
    var isPushEnabled by remember { mutableStateOf(notificationPrefs.isPushEnabled) }
    var isInquiryAlertsEnabled by remember { mutableStateOf(notificationPrefs.isInquiryAlertsEnabled) }
    var isTrafficMilestonesEnabled by remember { mutableStateOf(notificationPrefs.isTrafficMilestonesEnabled) }
    var isAppUpdateAlertsEnabled by remember { mutableStateOf(notificationPrefs.isAppUpdateAlertsEnabled) }
    var isSoundAndVibrateEnabled by remember { mutableStateOf(notificationPrefs.isSoundAndVibrateEnabled) }
    var hasSystemPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    // Dialog states
    var showUpdateDialog by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    var showLogsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Profile form states
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var aboutText by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var resumeUrl by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var instagramUrl by remember { mutableStateOf("") }
    var availableForHire by remember { mutableStateOf(true) }

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    val currentVersionCode = remember { UpdateManager.getCurrentVersionCode(context) }
    val currentVersionName = remember { UpdateManager.getCurrentVersionName(context) }

    LaunchedEffect(initialProfile) {
        if (!isInitialized && initialProfile != null) {
            initialProfile?.let { p ->
                name = p.name
                title = p.title
                bio = p.bio
                aboutText = p.about_text
                email = p.email
                phone = p.phone
                location = p.location
                avatarUrl = p.avatar_url
                resumeUrl = p.resume_url
                githubUrl = p.github_url
                linkedinUrl = p.linkedin_url
                instagramUrl = p.instagram_url
                availableForHire = p.available_for_hire
            }
            isInitialized = true
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasSystemPermission = isGranted
        if (isGranted) {
            isPushEnabled = true
            notificationPrefs.isPushEnabled = true
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            NotificationHelper.showTestPushNotification(context)
        } else {
            Toast.makeText(context, "Notification permission was not granted", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingAvatar = true
                val res = storageRepository.uploadImage(context, uri, "avatars")
                isUploadingAvatar = false
                if (res.isSuccess) {
                    avatarUrl = res.getOrThrow()
                    Toast.makeText(context, "Avatar updated!", Toast.LENGTH_SHORT).show()
                } else {
                    val err = res.exceptionOrNull()?.localizedMessage ?: "Failed to set avatar"
                    snackbarHostState.showSnackbar("Could not set avatar: $err")
                }
            }
        }
    }

    val saveProfile = {
        if (!isSaving && name.isNotBlank()) {
            coroutineScope.launch {
                isSaving = true
                val targetId = initialProfile?.id?.ifBlank { "main" } ?: "main"
                val updated = Profile(
                    id = targetId,
                    name = name.trim(),
                    title = title.trim(),
                    bio = bio.trim(),
                    about_text = aboutText.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    location = location.trim(),
                    avatar_url = avatarUrl.trim(),
                    resume_url = resumeUrl.trim(),
                    github_url = githubUrl.trim(),
                    linkedin_url = linkedinUrl.trim(),
                    instagram_url = instagramUrl.trim(),
                    available_for_hire = availableForHire,
                    updated_at = ""
                )
                val res = profileRepository.updateProfile(updated)
                isSaving = false
                if (res.isSuccess) {
                    Toast.makeText(context, "Profile and portfolio settings saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    snackbarHostState.showSnackbar("Failed to save: ${res.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }

    val checkUpdates = {
        coroutineScope.launch {
            isCheckingUpdate = true
            try {
                val updateRes = UpdateManager.checkForUpdates(context)
                if (updateRes is UpdateCheckResult.UpdateAvailable) {
                    availableUpdate = updateRes.info
                    showUpdateDialog = true
                } else {
                    Toast.makeText(context, "You are on the latest version ($currentVersionName)!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Update check failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isCheckingUpdate = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NavyPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { saveProfile() },
                        enabled = !isSaving && name.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = NavyPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Profile",
                                tint = if (name.isNotBlank()) NavyPrimary else TextMuted
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. EXECUTIVE PROFILE HEADER CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle with Camera Badge
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = com.narayan.portfolioadmin.data.util.ImageUtils.parseImageModel(avatarUrl),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, BorderSubtle, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(NavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(2).uppercase().ifBlank { "NP" },
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NavyPrimary)
                                    .border(2.dp, SurfaceWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // User Info Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { "Narayan Phukan" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = title.ifBlank { "Full Stack Developer" },
                                fontSize = 13.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Available for Hire Badge Pill
                            Surface(
                                color = if (availableForHire) Color(0xFFECFDF5) else SurfaceSubtle,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (availableForHire) Color(0xFFA7F3D0) else BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (availableForHire) SuccessGreen else TextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (availableForHire) "Available for Hire" else "Not Available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (availableForHire) SuccessGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Availability Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open to Work / Freelance", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Displays 'Available for Hire' badge on public web portfolio", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = availableForHire,
                            onCheckedChange = { availableForHire = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NavyPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
                        )
                    }
                }
            }

            // ==========================================
            // 2. PUSH NOTIFICATIONS & ALERTS CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Push Notifications", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text("Real-time alerts to this phone", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        // Master Toggle
                        Switch(
                            checked = isPushEnabled,
                            onCheckedChange = { enabled ->
                                isPushEnabled = enabled
                                notificationPrefs.isPushEnabled = enabled
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasSystemPermission) {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NavyPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
                        )
                    }

                    // System Permission Status Chip
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        color = if (hasSystemPermission) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (hasSystemPermission) Color(0xFFA7F3D0) else Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasSystemPermission) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasSystemPermission) SuccessGreen else WarningAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasSystemPermission) "System Notification Permission: Active" else "Permission Required: Tap to grant notification permission",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasSystemPermission) SuccessGreen else WarningAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Alert Category 1: Client Inquiries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("New Client Inquiries", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Alert when a visitor contacts you via the website", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = isInquiryAlertsEnabled && isPushEnabled,
                            enabled = isPushEnabled,
                            onCheckedChange = {
                                isInquiryAlertsEnabled = it
                                notificationPrefs.isInquiryAlertsEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NavyPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Alert Category 2: Visitor Milestones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Visitor & Traffic Milestones", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Alert when portfolio traffic reaches major visitor milestones", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = isTrafficMilestonesEnabled && isPushEnabled,
                            enabled = isPushEnabled,
                            onCheckedChange = {
                                isTrafficMilestonesEnabled = it
                                notificationPrefs.isTrafficMilestonesEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NavyPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Alert Category 3: App Updates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Update Releases", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Alert when a new version of the Admin App is ready", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = isAppUpdateAlertsEnabled && isPushEnabled,
                            enabled = isPushEnabled,
                            onCheckedChange = {
                                isAppUpdateAlertsEnabled = it
                                notificationPrefs.isAppUpdateAlertsEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NavyPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Alert Category 4: Sound & Vibration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sound & Vibration", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Play audible chime and vibrate on incoming notifications", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = isSoundAndVibrateEnabled && isPushEnabled,
                            enabled = isPushEnabled,
                            onCheckedChange = {
                                isSoundAndVibrateEnabled = it
                                notificationPrefs.isSoundAndVibrateEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NavyPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TEST NOTIFICATION BUTTON
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasSystemPermission) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.showTestPushNotification(context)
                                Toast.makeText(context, "Test notification dispatched! Check your status bar.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NavyPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Push Notification", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ==========================================
            // 3. APP SETTINGS & DIAGNOSTICS CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("App Settings & Diagnostics", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("Maintenance, updates and system telemetry", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // In-App Updates Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Admin App Version", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("v$currentVersionName (Build $currentVersionCode)", fontSize = 11.sp, color = TextMuted)
                        }
                        Button(
                            onClick = { checkUpdates() },
                            enabled = !isCheckingUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Updates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Clear Cache Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear App Cache", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Frees temporary local images and memory", fontSize = 11.sp, color = TextMuted)
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.imageLoader.memoryCache?.clear()
                                    context.cacheDir.deleteRecursively()
                                    Toast.makeText(context, "Local app cache cleared successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderStrong),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Diagnostics & Error Logs Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogsDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NavyBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Error Logs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NavyBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Report Issue", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Device Telemetry Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceSubtle,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("DEVICE TELEMETRY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Device Model", fontSize = 11.sp, color = TextSecondary)
                                Text("${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Android OS", fontSize = 11.sp, color = TextSecondary)
                                Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Database Backend", fontSize = 11.sp, color = TextSecondary)
                                Text("Cloud Firestore (Active)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SuccessGreen)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. PORTFOLIO & PROFILE DETAILS CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Portfolio Public Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("Information shown on your portfolio website", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Personal Information
                    Text("Personal Information", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Professional Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Short Bio (Hero Section)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = aboutText,
                        onValueChange = { aboutText = it },
                        label = { Text("About Details (About Section)") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Contact Details
                    Text("Contact Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Social & External Links
                    Text("Social & External Links", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = githubUrl,
                        onValueChange = { githubUrl = it },
                        label = { Text("GitHub Profile URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = linkedinUrl,
                        onValueChange = { linkedinUrl = it },
                        label = { Text("LinkedIn Profile URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = instagramUrl,
                        onValueChange = { instagramUrl = it },
                        label = { Text("Instagram Profile URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resumeUrl,
                        onValueChange = { resumeUrl = it },
                        label = { Text("Resume Download URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = { Text("Avatar Photo URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Profile Button
                    Button(
                        onClick = { saveProfile() },
                        enabled = !isSaving && name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile Changes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            // ==========================================
            // 5. ACCOUNT & SECURITY CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Account & Security", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text("Session authentication and credentials", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Logged In Administrator", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(authRepository.currentUser?.email ?: "admin@portfolio.com", fontSize = 12.sp, color = TextSecondary)
                        }
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = "Admin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign Out Button
                    OutlinedButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Admin", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold, color = NavyPrimary) },
            text = { Text("Are you sure you want to sign out of the Portfolio Admin application?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Error Log Viewer Dialog
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

    // Report Error Dialog
    if (showReportDialog) {
        ReportErrorDialog(
            onDismiss = { showReportDialog = false },
            isSubmitting = isSubmittingReport,
            onSubmit = { reportTitle, reportDesc, screen, includeDiag ->
                coroutineScope.launch {
                    isSubmittingReport = true
                    val res = ErrorTracker.submitUserReport(
                        title = reportTitle,
                        description = reportDesc,
                        screenName = screen,
                        includeDiagnostics = includeDiag
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

    // App Update Dialog
    if (showUpdateDialog && availableUpdate != null) {
        val update = availableUpdate!!
        UpdateDialog(
            updateInfo = update,
            currentVersion = currentVersionName,
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
