package com.narayan.portfolioadmin.ui.screens.messages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narayan.portfolioadmin.data.model.ContactMessage
import com.narayan.portfolioadmin.data.repository.MessagesRepository
import com.narayan.portfolioadmin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    messagesRepository: MessagesRepository,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val messages by messagesRepository.getMessagesFlow().collectAsState(initial = emptyList())

    var selectedMessage by remember { mutableStateOf<ContactMessage?>(null) }
    var messageToDelete by remember { mutableStateOf<ContactMessage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Messages (${messages.size})", color = TextPrimary)
                        val unread = messages.count { !it.is_read }
                        if (unread > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = WarningAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$unread new",
                                    color = WarningAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
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
        containerColor = BackgroundDark
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No contact messages received yet.",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageCard(
                        message = msg,
                        onClick = {
                            selectedMessage = msg
                            if (!msg.is_read) {
                                coroutineScope.launch {
                                    messagesRepository.markAsRead(msg.id)
                                }
                            }
                        },
                        onDelete = { messageToDelete = msg }
                    )
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Detail Dialog
    if (selectedMessage != null) {
        val msg = selectedMessage!!
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = {
                Column {
                    Text(msg.subject.ifBlank { "Message from ${msg.name}" }, fontWeight = FontWeight.Bold)
                    Text("From: ${msg.name} <${msg.email}>", fontSize = 13.sp, color = TextSecondary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = msg.message,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${msg.email}")
                            putExtra(Intent.EXTRA_SUBJECT, "Re: ${msg.subject.ifBlank { "Portfolio Inquiry" }}")
                        }
                        context.startActivity(Intent.createChooser(intent, "Reply via"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reply via Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMessage = null }) {
                    Text("Close")
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Dialog
    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete message from '${messageToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = messageToDelete?.id ?: ""
                        coroutineScope.launch {
                            messagesRepository.deleteMessage(id)
                            messageToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun MessageCard(
    message: ContactMessage,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!message.is_read) CardDark else SurfaceDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!message.is_read) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!message.is_read) FontWeight.Bold else FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (!message.is_read) "New" else "Read",
                        fontSize = 11.sp,
                        color = if (!message.is_read) WarningAmber else TextMuted
                    )
                }

                Text(
                    text = message.subject.ifBlank { message.email },
                    fontSize = 13.sp,
                    color = AccentCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}
