package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ConceptMastery
import com.example.data.StudyTask
import com.example.ui.MainViewModel
import com.example.ui.Screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalTwinDashboardScreen(viewModel: MainViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val allConcepts by viewModel.allConcepts.collectAsStateWithLifecycle()
    val studyTasks by viewModel.studyTasks.collectAsStateWithLifecycle()
    val isOnline by viewModel.isNetworkOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val recentlyStudied by viewModel.recentlyStudiedDecks.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val twinAdvice by viewModel.twinGuidanceText.collectAsStateWithLifecycle()
    val isAILoading by viewModel.isAILoading.collectAsStateWithLifecycle()
    val chatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val placements by viewModel.internshipPlacements.collectAsStateWithLifecycle()

    var showCustomizerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showVoiceDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceDialog = true
        } else {
            Toast.makeText(context, "Microphone permission is required for Socratic Verbal Clarification.", Toast.LENGTH_LONG).show()
        }
    }

    if (showVoiceDialog) {
        SocraticVoiceDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false }
        )
    }

    if (showCustomizerDialog) {
        val currentTwinAvatar = profile?.selectedTwinAvatar ?: "socratic"
        TwinCustomizerDialog(
            currentTwin = currentTwinAvatar,
            onDismiss = { showCustomizerDialog = false },
            onSelect = { newAvatar ->
                viewModel.updateSelectedTwinAvatar(newAvatar)
                showCustomizerDialog = false
                viewModel.generateDigitalTwinGuidance()
                viewModel.startDigitalTwinChat()
            }
        )
    }

    // Trigger advice generation once when the screen opens if it hasn't been generated yet
    LaunchedEffect(profile) {
        if (twinAdvice == null) {
            viewModel.generateDigitalTwinGuidance()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startDigitalTwinChat()
    }

    val activeAvatar = profile?.selectedTwinAvatar ?: "socratic"
    val avatarRes = when (activeAvatar) {
        "socratic" -> R.drawable.img_twin_socratic
        "scholar" -> R.drawable.img_twin_scholar
        "tech" -> R.drawable.img_twin_tech
        else -> R.drawable.img_twin_creative
    }
    
    val twinDisplayName = when (activeAvatar) {
        "socratic" -> "Socratic Mentor"
        "scholar" -> "Scholar Academic"
        "tech" -> "Tech Visionary"
        else -> "Creative Innovator"
    }

    val knowledgeGaps = remember(allConcepts) {
        allConcepts.filter { (it.understandingScore + it.confidenceScore) / 2f < 0.6f }
    }
    val pendingTasks = remember(studyTasks) {
        studyTasks.filter { !it.isCompleted }
    }

    Scaffold(
        modifier = Modifier.testTag("digital_twin_dashboard_root"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Digital Twin",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800))
                            )
                            Text(
                                text = if (isOnline) "Cognitive Sync Active" else "Offline (Secure Caching Mode)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) MaterialTheme.colorScheme.primary else Color(0xFFFF9800)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Progress) },
                        modifier = Modifier.testTag("twin_dashboard_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateDigitalTwinGuidance() },
                        enabled = !isAILoading,
                        modifier = Modifier.testTag("refresh_twin_advice_icon")
                    ) {
                        if (isAILoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Advice")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Digital Twin Avatar & AI Persona Advice Bubble
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("twin_avatar_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar image
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = twinDisplayName,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { showCustomizerDialog = true },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = twinDisplayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your Personalized Learning Mirror",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Compact style display tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (activeAvatar) {
                                            "tech" -> "ANALYSIS MODE"
                                            "scholar" -> "FOUNDATIONAL RIGOR"
                                            "creative" -> "ANALOGY MODE"
                                            else -> "SOCRATIC DIALECTIC"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showCustomizerDialog = true },
                                modifier = Modifier.testTag("customize_twin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Customize Persona",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Speech bubble containing advice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Advice & Synthesized Study Plan:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isAILoading && twinAdvice == null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(
                                            text = "Digital Twin is synthesizing data...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = twinAdvice ?: "Click refresh to trigger digital twin advisor analysis.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Neural Cognitive Mind Map / Digital Twin State Visualization
            item {
                DigitalTwinMindStateMap(
                    streak = profile?.streak ?: 1,
                    level = profile?.level ?: 1,
                    cardsReviewed = profile?.cardsReviewedCount ?: 0,
                    activePlacementsCount = placements.count { it.status == "In Progress" },
                    graduatedCount = placements.count { it.status == "Completed" }
                )
            }

            // Chat with Digital Twin Conversational Avatar
            item {
                var chatInputText by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("twin_chat_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = "Chat",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Talk with your Twin Avatar",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.clearDigitalTwinChat() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Chat",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Interact directly with your synchronized mind. Your Digital Twin understands your strengths, weaknesses, learning goals, and progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Box displaying chat messages
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            val twinMessages = chatMessages.filter { it.sessionId == "digital_twin_chat" }
                            if (twinMessages.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    items(twinMessages) { msg ->
                                        val isUser = msg.role == "user"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 12.dp,
                                                            topEnd = 12.dp,
                                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                                        )
                                                    )
                                                    .background(
                                                        if (isUser) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .widthIn(max = 240.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    )
                                    if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        showVoiceDialog = true
                                    } else {
                                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("twin_chat_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            OutlinedTextField(
                                value = chatInputText,
                                onValueChange = { chatInputText = it },
                                placeholder = { Text("Ask your Twin...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("twin_chat_input"),
                                shape = RoundedCornerShape(26.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (chatInputText.isNotBlank()) {
                                        viewModel.sendMessageToDigitalTwin(chatInputText)
                                        chatInputText = ""
                                    }
                                },
                                enabled = chatInputText.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .testTag("twin_chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Caching and Synchronizer Status Panel
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_sync_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Cache & Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            // Pending badge
                            if (pendingSyncCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$pendingSyncCount Pending",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Recently studied decks are fully saved locally. Decks and flashcard ratings automatically cache and queue when offline, syncing instantly upon connection to avoid learning disruption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Recently studied decks list (Cached Decks)
                        Text(
                            text = "Recently Cached Decks:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (recentlyStudied.isEmpty()) {
                            Text(
                                text = "No decks studied recently. Start reviewing cards to activate offline caching!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            recentlyStudied.take(3).forEach { recent ->
                                val matchedDeck = allDecks.find { it.id == recent.deckId }
                                val deckName = matchedDeck?.name ?: "General Knowledge"
                                val deckSubject = matchedDeck?.subject ?: "General"
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Style,
                                            contentDescription = "Deck",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = deckName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = deckSubject,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CACHED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }

                        // Sync trigger button
                        if (pendingSyncCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.processPendingSyncQueue() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("force_sync_queue_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync Now", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Sync Pending Actions Now", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 3. Current Learning Progress Segment
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("learning_progress_dashboard_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Learning Progress Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${profile?.streak ?: 1} 🔥",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFFFF5722)
                                )
                                Text(text = "Daily Streak", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${profile?.level ?: 1}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(text = "Learner Level", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${profile?.cardsReviewedCount ?: 0}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF00BCD4)
                                )
                                Text(text = "Cards Reviewed", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${profile?.xp ?: 50} 🧪",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF9C27B0)
                                )
                                Text(text = "Total XP", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Average cognitive scores from concept list
                        val avgUnderstanding = if (allConcepts.isEmpty()) 0.5f else allConcepts.map { it.understandingScore }.average().toFloat()
                        val avgConfidence = if (allConcepts.isEmpty()) 0.5f else allConcepts.map { it.confidenceScore }.average().toFloat()
                        val avgRetention = if (allConcepts.isEmpty()) 0.5f else allConcepts.map { it.retentionScore }.average().toFloat()

                        Text(
                            text = "Cognitive Mastery Indices:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ProgressRow(label = "Understanding Index", value = avgUnderstanding, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressRow(label = "Retention Index", value = avgRetention, color = Color(0xFF3F51B5))
                        Spacer(modifier = Modifier.height(8.dp))
                        ProgressRow(label = "Retrieval Confidence", value = avgConfidence, color = Color(0xFF00BCD4))
                    }
                }
            }

            // 4. Knowledge Gaps and Bridge Actions

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("knowledge_gaps_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Gaps",
                                tint = Color(0xFFFBC02D),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detected Knowledge Gaps",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "These core topics currently display lower confidence indices. Bridge them with our suggested active revision strategies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (knowledgeGaps.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Perfect", tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Excellent sync! No substantial knowledge gaps detected. Your Learning Twin is fully aligned.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        } else {
                            knowledgeGaps.take(4).forEach { concept ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = concept.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = concept.subject,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = (concept.understandingScore + concept.confidenceScore) / 2f,
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = Color(0xFFF44336),
                                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.navigateTo(Screen.TutorChat(conceptId = concept.id)) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(imageVector = Icons.Default.Forum, contentDescription = "Tutor", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Tutor", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Upcoming Study Goals Checklist

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upcoming_goals_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = "Goals",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Learning Goals",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (pendingTasks.isEmpty()) {
                            Text(
                                text = "No pending study tasks! Generate a new plan or celebrate your milestones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            pendingTasks.take(4).forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.toggleTaskCompletion(task) }
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Not Done",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = task.conceptName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Bridge goal • +${task.xpAwarded} XP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Action",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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

@Composable
fun ProgressRow(label: String, value: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    }
}

@Composable
fun DigitalTwinMindStateMap(
    streak: Int,
    level: Int,
    cardsReviewed: Int,
    activePlacementsCount: Int,
    graduatedCount: Int
) {
    var selectedHub by remember { mutableStateOf(0) } // Default to memory strength (0)
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowOffset"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cognitive_mind_map_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🧠 Neural Cognitive Mapping",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Interactive Mind Space of your Digital Twin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Sync: Real-Time",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Interactive Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            // Simple bounding box checker to switch selection
                            selectedHub = (selectedHub + 1) % 4
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val cx = width / 2
                    val cy = height / 2
                    val r = kotlin.math.min(width, height) * 0.32f

                    // 1. Draw Cognitive Radar Grid
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.05f),
                        radius = r,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.03f),
                        radius = r * 0.6f,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )

                    // 2. Define Satellite Coordinates
                    // Angles: Top (-90), Right (0), Bottom (90), Left (180)
                    val angles = listOf(-90.0, 0.0, 90.0, 180.0)
                    val points = angles.map { deg ->
                        val rad = Math.toRadians(deg)
                        val px = cx + r * kotlin.math.cos(rad).toFloat()
                        val py = cy + r * kotlin.math.sin(rad).toFloat()
                        androidx.compose.ui.geometry.Offset(px, py)
                    }

                    // 3. Draw Pathways and Traveling Thought Impulses
                    points.forEachIndexed { idx, p ->
                        val isSelected = selectedHub == idx
                        val pathwayColor = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.25f)
                        
                        // Path line
                        drawLine(
                            color = pathwayColor,
                            start = androidx.compose.ui.geometry.Offset(cx, cy),
                            end = p,
                            strokeWidth = if (isSelected) 3f else 1.5f
                        )

                        // Glowing signal traveling from core to satellite
                        val dotX = cx + (p.x - cx) * flowOffset
                        val dotY = cy + (p.y - cy) * flowOffset
                        drawCircle(
                            color = if (isSelected) secondaryColor else primaryColor.copy(alpha = 0.7f),
                            radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                        )
                    }

                    // 4. Draw satellites
                    val labels = listOf("Memory", "Foundations", "Practical", "Readiness")
                    points.forEachIndexed { idx, p ->
                        val isSelected = selectedHub == idx
                        val nodeRadius = if (isSelected) 18.dp.toPx() else 14.dp.toPx()
                        val colorScheme = when(idx) {
                            0 -> primaryColor
                            1 -> secondaryColor
                            2 -> tertiaryColor
                            else -> Color(0xFF4CAF50)
                        }

                        // Node Outer Aura
                        drawCircle(
                            color = colorScheme.copy(alpha = if (isSelected) 0.2f else 0.1f),
                            radius = nodeRadius * pulseScale * 1.4f,
                            center = p
                        )

                        // Node Fill
                        drawCircle(
                            color = if (isSelected) colorScheme else colorScheme.copy(alpha = 0.65f),
                            radius = nodeRadius,
                            center = p
                        )

                        // Node Core
                        drawCircle(
                            color = Color.White,
                            radius = nodeRadius * 0.35f,
                            center = p
                        )
                    }

                    // 5. Draw central core
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.15f),
                        radius = 28.dp.toPx() * pulseScale,
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 20.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                    )
                }

                // Small absolute helpers inside Box to display labels on satellites
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Memory Recall",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedHub == 0) primaryColor else onSurfaceColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )
                    Text(
                        text = "Applied Practice",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedHub == 2) tertiaryColor else onSurfaceColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    )
                    Text(
                        text = "Foundations",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedHub == 1) secondaryColor else onSurfaceColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "Industry Prep",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedHub == 3) Color(0xFF2E7D32) else onSurfaceColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp)
                    )

                    // Tap Instruction indicator
                    Text(
                        text = "👉 TAP MAP TO ROTATE FOCUS HUB",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = onSurfaceColor.copy(alpha = 0.4f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 55.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Selected Satellite Insights Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val title = when (selectedHub) {
                        0 -> "🧠 Active Memory Recall Index"
                        1 -> "📚 Foundations & Rigor"
                        2 -> "💻 Applied Skill & Code Volume"
                        else -> "🌍 Industry Internship Alignment"
                    }
                    val indexValue = when (selectedHub) {
                        0 -> "${(65 + (streak * 2).coerceAtMost(25))}%"
                        1 -> "${(70 + (level * 4).coerceAtMost(25))}%"
                        2 -> "${(50 + (cardsReviewed / 5).coerceAtMost(45))}%"
                        else -> if (graduatedCount > 0) "100% (Certified)" else if (activePlacementsCount > 0) "In Training" else "Ready to Match"
                    }
                    val explanation = when (selectedHub) {
                        0 -> "Based on your daily streak of $streak and spaced-repetition performance. Your digital twin has stabilized active retention gaps."
                        1 -> "Reflects theoretical topics mastered. Currently at Level $level with robust concept mastery models in your DB."
                        2 -> "Aggregates absolute practice metrics including $cardsReviewed card repetitions. Confirms high tactile cognitive agility."
                        else -> "Evaluates readiness for enterprise-grade contributions. Active Placements: $activePlacementsCount. Graduates: $graduatedCount."
                    }
                    val advice = when (selectedHub) {
                        0 -> "Socratic advice: Revisit reviews every 12 hours to compress synaptic decay gaps."
                        1 -> "Socratic advice: Tackle an accredited advanced module or technical paper to level up."
                        2 -> "Socratic advice: Participate in open-source tech room exchanges and prototype creations."
                        else -> "Socratic advice: Navigate to the Global Internships board to secure your next project!"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = indexValue,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = advice,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocraticVoiceDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var listeningStatus by remember { mutableStateOf("Initializing Microphone Stream...") }
    var spokenResultText by remember { mutableStateOf("") }
    var isListeningActive by remember { mutableStateOf(true) }

    val isSpeechAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    
    val speechRecognizer = remember {
        if (isSpeechAvailable) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listeningStatus = "🎤 Listening for study concepts..."
            }
            override fun onBeginningOfSpeech() {
                listeningStatus = "🎙️ Verbal signal detected..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                listeningStatus = "⏳ Synthesizing voice data..."
            }
            override fun onError(error: Int) {
                val description = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio record error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side limit"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network failure"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server disconnected"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence timeout"
                    else -> "Audio capture gap"
                }
                listeningStatus = "⚠️ $description. Select a concept below:"
                isListeningActive = false
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val speechText = matches[0]
                    spokenResultText = speechText
                    listeningStatus = "💡 Voice recognized!"
                    isListeningActive = false
                    
                    viewModel.sendMessageToDigitalTwin(speechText)
                    Toast.makeText(context, "Spoken query sent: \"$speechText\"", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    listeningStatus = "⚠️ No words detected. Try again or tap a concept below."
                    isListeningActive = false
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    spokenResultText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer.setRecognitionListener(recognitionListener)
                speechRecognizer.startListening(intent)
                listeningStatus = "🎙️ Listening... speak now"
                isListeningActive = true
            } catch (e: Exception) {
                listeningStatus = "⚠️ Speech recognizer failure. Tap below to select topic:"
                isListeningActive = false
            }
        } else {
            listeningStatus = "🎙️ Voice Stream Simulated. Select a study topic below:"
            isListeningActive = true
        }
    }

    LaunchedEffect(Unit) {
        startListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform_pulse")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = 75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s3"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("socratic_voice_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Socratic Oral Clarify",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Verbally ask your Digital Twin to clarify tricky flashcards, database policies, consensus limits, or complex theories in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListeningActive) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.08f),
                                radius = scale2.dp.toPx() + 20.dp.toPx()
                            )
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.12f),
                                radius = scale3.dp.toPx() + 10.dp.toPx()
                            )
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.18f),
                                radius = scale1.dp.toPx()
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (!isListeningActive) {
                                startListening()
                            } else {
                                speechRecognizer?.stopListening()
                                isListeningActive = false
                                listeningStatus = "🎙️ Listening paused. Select a shortcut below:"
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListeningActive) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.secondary
                            )
                    ) {
                        Icon(
                            imageVector = if (isListeningActive) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mic toggle",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = listeningStatus,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (spokenResultText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$spokenResultText\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "🎙️ Quick study concept shortcuts:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val shortcuts = listOf(
                    "Explain PBFT Consensus Protocols" to "Could you clarify how Practical Byzantine Fault Tolerance (PBFT) consensus operates under high network latency?",
                    "Explain Multimodal Alignment" to "How does a multimodal LLM align visual tokens with semantic text spaces during embedding fusion?",
                    "Explain Room Migration Policies" to "What is the best strategy to implement fallbackToDestructiveMigration inside a local Android Room DB?",
                    "Explain Asynchronous Coroutines" to "Explain how Kotlin's Asynchronous flow compares to standard RxJava schedulers for background thread handshakes."
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    shortcuts.forEach { (label, prompt) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            onClick = {
                                spokenResultText = prompt
                                isListeningActive = false
                                listeningStatus = "⚡ Simulating oral signal transmission..."
                                
                                viewModel.sendMessageToDigitalTwin(prompt)
                                Toast.makeText(context, "Verbal concept sent to Socratic Twin", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
