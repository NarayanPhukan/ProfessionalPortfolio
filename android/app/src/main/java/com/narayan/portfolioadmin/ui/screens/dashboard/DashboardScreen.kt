package com.narayan.portfolioadmin.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.narayan.portfolioadmin.data.model.*
import com.narayan.portfolioadmin.data.repository.*
import com.narayan.portfolioadmin.data.tracker.ErrorTracker
import com.narayan.portfolioadmin.data.updater.UpdateManager
import com.narayan.portfolioadmin.ui.components.ErrorLogViewerDialog
import com.narayan.portfolioadmin.ui.components.ReportErrorDialog
import com.narayan.portfolioadmin.ui.components.UpdateDialog
import com.narayan.portfolioadmin.ui.screens.projects.ProjectEditDialog
import com.narayan.portfolioadmin.ui.screens.skills.SkillEditDialog
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    projectsRepository: ProjectsRepository,
    skillsRepository: SkillsRepository,
    messagesRepository: MessagesRepository,
    errorReportRepository: ErrorReportRepository = remember { ErrorReportRepository() },
    analyticsRepository: AnalyticsRepository = remember { AnalyticsRepository() },
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
    val analyticsSummary by analyticsRepository.getAnalyticsFlow().collectAsState(initial = null)

    val storageRepository = remember { StorageRepository() }

    var showReportDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    // Auto-update states
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val unreadCount = messages.count { !it.is_read }

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date())
    }

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    // Auto-detect pending crashes and check for updates
    LaunchedEffect(Unit) {
        val pendingCrash = ErrorTracker.checkAndUploadPendingCrash(context)
        if (pendingCrash != null) {
            snackbarHostState.showSnackbar(
                message = "Auto-detected crash was logged and reported.",
                duration = SnackbarDuration.Long
            )
        }

        try {
            val updateRes = UpdateManager.checkForUpdates(context)
            if (updateRes is UpdateCheckResult.UpdateAvailable) {
                availableUpdate = updateRes.info
                showUpdateDialog = true
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundCanvas
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // 1. Top Bar: Live Status Badge + User Profile Avatar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Portfolio Live & Synced",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Avatar Squircle Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F1E36))
                            .clickable(onClick = onNavigateToProfile),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = profile?.avatar_url
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = com.narayan.portfolioadmin.data.util.ImageUtils.parseImageModel(avatarUrl),
                                contentDescription = "Profile Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = profile?.name?.take(1)?.uppercase() ?: "N",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // 2. Greeting Header
            item {
                Column {
                    Text(
                        text = currentDateStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$greetingText\n${profile?.name?.split(" ")?.firstOrNull()?.ifBlank { "Narayan" } ?: "Narayan"}.",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F1E36),
                        lineHeight = 36.sp
                    )
                }
            }

            // 3. Hero Analytics Card (Dark Navy Container)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F1E36),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "TOTAL PORTFOLIO VISITS & INQUIRIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8EA3BF),
                            letterSpacing = 0.8.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val totalVisitsCount = (analyticsSummary?.total_visits ?: 0L) + messages.size
                                Text(
                                    text = String.format(Locale.US, "%,d", totalVisitsCount),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val rawGrowth = analyticsSummary?.monthly_growth?.ifBlank { "Active" } ?: "Active"
                                    val formattedGrowth = when {
                                        rawGrowth.startsWith("+") -> "↑ ${rawGrowth.removePrefix("+").trim()}"
                                        rawGrowth.startsWith("↑") -> rawGrowth
                                        else -> rawGrowth
                                    }
                                    Text(
                                        text = formattedGrowth,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "this month",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8EA3BF)
                                    )
                                }
                            }

                            // Dynamic Sparkline Canvas Chart
                            val trendData: List<Float> = remember(analyticsSummary?.sparkline_trend, analyticsSummary?.total_visits) {
                                val list = analyticsSummary?.sparkline_trend
                                if (list != null && list.size >= 2) list else listOf(0f, 0f, 0f, 0f, 0f, 0f, (analyticsSummary?.total_visits ?: 0L).toFloat())
                            }

                            Canvas(modifier = Modifier.width(110.dp).height(48.dp)) {
                                val minVal: Float = trendData.minOrNull() ?: 0f
                                val maxVal: Float = trendData.maxOrNull() ?: 0f
                                val hasSpread = maxVal > minVal
                                val range: Float = if (hasSpread) (maxVal - minVal) else 1f
                                val topPadding = size.height * 0.15f
                                val bottomPadding = size.height * 0.15f
                                val drawableHeight = size.height - topPadding - bottomPadding
                                val stepX = size.width / (trendData.size - 1).coerceAtLeast(1)

                                val path = Path()
                                trendData.forEachIndexed { index: Int, value: Float ->
                                    val x = index.toFloat() * stepX
                                    val normalized = if (hasSpread) (value - minVal) / range else 0.5f
                                    val y = size.height - bottomPadding - (normalized * drawableHeight)
                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3 Metric Capsules
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Projects Capsule
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onNavigateToProjects),
                                color = Color(0xFF1B2B46),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "PROJECTS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8EA3BF),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = projects.count { !it.hidden }.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8EA3BF)
                                    )
                                }
                            }

                            // Skills Capsule
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onNavigateToSkills),
                                color = Color(0xFF1B2B46),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "SKILLS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8EA3BF),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = skills.size.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Indexed",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8EA3BF)
                                    )
                                }
                            }

                            // Inquiries Capsule
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onNavigateToMessages),
                                color = Color(0xFF1B2B46),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "INQUIRIES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8EA3BF),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = messages.size.toString(),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (unreadCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF59E0B))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (unreadCount > 0) "$unreadCount Unread" else "All Read",
                                        fontSize = 11.sp,
                                        color = if (unreadCount > 0) Color(0xFFF59E0B) else Color(0xFF8EA3BF),
                                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F1E36)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // + New Project
                        Surface(
                            onClick = { showAddProjectDialog = true },
                            color = Color(0xFFEFF4F9),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEB))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F1E36), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Project", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F1E36))
                            }
                        }

                        // Add Tech
                        Surface(
                            onClick = { showAddSkillDialog = true },
                            color = Color(0xFFEFF4F9),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEB))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF0F1E36), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Tech", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F1E36))
                            }
                        }

                        // Broadcast / Updates
                        Surface(
                            onClick = {
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
                                            snackbarHostState.showSnackbar("Update check: ${res.message}")
                                        }
                                    }
                                    isCheckingUpdate = false
                                }
                            },
                            color = Color(0xFFEFF4F9),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEB))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF0F1E36))
                                } else {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF0F1E36), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Updates", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F1E36))
                            }
                        }

                        // Diagnostics
                        Surface(
                            onClick = { showLogsDialog = true },
                            color = Color(0xFFEFF4F9),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEB))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF0F1E36), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (errorReports.isNotEmpty()) "Errors (${errorReports.size})" else "Diagnostics",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F1E36)
                                )
                            }
                        }

                        // Report Issue
                        Surface(
                            onClick = { showReportDialog = true },
                            color = Color(0xFFEFF4F9),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFFD0DDEB))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Report Bug", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F1E36))
                            }
                        }
                    }
                }
            }

            // 5. Recent Inquiries (White Grouped Inset Card)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Inquiries",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1E36)
                        )
                        Text(
                            text = "View all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.clickable(onClick = onNavigateToMessages)
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MailOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No messages received yet.", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            Column(modifier = Modifier.padding(16.dp)) {
                                messages.take(3).forEachIndexed { index, msg ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = BorderSubtle,
                                            modifier = Modifier.padding(vertical = 14.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Initials Avatar Box
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFEFF4F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = msg.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "SK" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF0F1E36)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = msg.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF0F1E36)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (msg.created_at.length >= 10) msg.created_at.take(10) else "Recent",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    if (!msg.is_read) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(7.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF1D4ED8))
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = msg.message.ifBlank { msg.subject },
                                                fontSize = 13.sp,
                                                color = Color(0xFF475569),
                                                maxLines = 2,
                                                lineHeight = 18.sp,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Action pill: Reply via Email
                                            Surface(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:${msg.email}")
                                                        putExtra(Intent.EXTRA_SUBJECT, "Re: ${msg.subject.ifBlank { "Portfolio Inquiry" }}")
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Reply via"))
                                                },
                                                color = Color(0xFFEBF3FB),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "↗ Reply via Email",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF0F1E36)
                                                    )
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

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Quick Add Project Dialog
    if (showAddProjectDialog) {
        ProjectEditDialog(
            project = null,
            storageRepository = storageRepository,
            onDismiss = { showAddProjectDialog = false },
            onSave = { newProject ->
                coroutineScope.launch {
                    val res = projectsRepository.saveProject(newProject)
                    showAddProjectDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Project created!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save project", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Quick Add Skill Dialog
    if (showAddSkillDialog) {
        SkillEditDialog(
            skill = null,
            onDismiss = { showAddSkillDialog = false },
            onSave = { newSkill ->
                coroutineScope.launch {
                    val res = skillsRepository.saveSkill(newSkill)
                    showAddSkillDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Skill added!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save skill", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
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
