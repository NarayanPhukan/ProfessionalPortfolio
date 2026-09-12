package com.narayan.portfolioadmin.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.narayan.portfolioadmin.data.model.Profile
import com.narayan.portfolioadmin.data.repository.ProfileRepository
import com.narayan.portfolioadmin.data.repository.StorageRepository
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileRepository: ProfileRepository,
    storageRepository: StorageRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val initialProfile by profileRepository.getProfileFlow().collectAsState(initial = null)

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
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                    Toast.makeText(context, "Avatar image prepared!", Toast.LENGTH_SHORT).show()
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
                    available_for_hire = availableForHire
                )
                val res = profileRepository.updateProfile(updated)
                isSaving = false
                if (res.isSuccess) {
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    onBack()
                } else {
                    snackbarHostState.showSnackbar("Failed to save: ${res.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = saveProfile,
                        enabled = !isSaving && name.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = PrimaryIndigo,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Profile",
                                tint = if (name.isNotBlank()) PrimaryIndigo else TextMuted
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with photo picker badge
            Box(
                modifier = Modifier
                    .size(108.dp)
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
                            .size(108.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(2).uppercase().ifBlank { "NP" },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentCyan),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = if (isUploadingAvatar) "Processing photo..." else "Tap avatar above to pick photo, or enter URL below",
                fontSize = 12.sp,
                color = TextSecondary
            )

            OutlinedTextField(
                value = avatarUrl,
                onValueChange = { avatarUrl = it },
                label = { Text("Avatar Photo URL") },
                placeholder = { Text("https://... or photo data URI") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Personal Info
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Subtitle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Bio (Hero Section)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = aboutText,
                onValueChange = { aboutText = it },
                label = { Text("About Details (About Section)") },
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            // Contact Information
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Contact Details",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Socials & Resume
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Social Links & Resume",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = githubUrl,
                onValueChange = { githubUrl = it },
                label = { Text("GitHub URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = linkedinUrl,
                onValueChange = { linkedinUrl = it },
                label = { Text("LinkedIn URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = instagramUrl,
                onValueChange = { instagramUrl = it },
                label = { Text("Instagram URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = resumeUrl,
                onValueChange = { resumeUrl = it },
                label = { Text("Resume URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Available for Hire", color = TextPrimary, fontWeight = FontWeight.Medium)
                Switch(
                    checked = availableForHire,
                    onCheckedChange = { availableForHire = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = saveProfile,
                enabled = !isSaving && name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
