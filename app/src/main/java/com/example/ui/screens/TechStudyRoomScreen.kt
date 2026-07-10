package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TechRoomMessage
import com.example.data.TechStudyRoom
import com.example.data.ScratchpadItem
import com.example.ui.MainViewModel
import com.example.ui.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechStudyRoomScreen(
    viewModel: MainViewModel,
    roomId: String
) {
    val roomState = viewModel.getTechRoom(roomId).collectAsState(initial = null)
    val messages by viewModel.getTechRoomMessages(roomId).collectAsState(initial = emptyList())
    val isAILoading by viewModel.isAILoading.collectAsState()

    val room = roomState.value
    val scratchpadItems by viewModel.getScratchpadItems(roomId).collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf(0) } // 0: Whiteboard, 1: Live Chat, 2: Milestones
    var subTab by remember { mutableStateOf(0) } // 0: Shared Whiteboard Document, 1: Real-time Brainstorming Scratchpad
    var noteText by remember { mutableStateOf("") }
    var chatInputText by remember { mutableStateOf("") }
    var scratchpadInputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val chatListState = rememberLazyListState()

    // Sync note draft with persistent state when loaded or updated collaboratively
    LaunchedEffect(room?.collaborativeNotes) {
        room?.collaborativeNotes?.let {
            if (it != noteText) {
                noteText = it
            }
        }
    }

    // Auto-save changes with debounce as the user types
    LaunchedEffect(noteText) {
        if (room != null && noteText != room.collaborativeNotes) {
            kotlinx.coroutines.delay(800) // Debounce delay
            viewModel.updateCollaborativeNotes(roomId, noteText)
        }
    }

    // Scroll chat list to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = room?.name ?: "Virtual Workspace",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Project: ${room?.projectName ?: "Loading..."}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.TechHub) },
                        modifier = Modifier.testTag("room_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Tech Hub"
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Synced 🟢",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("tech_room_top_bar")
            )
        }
    ) { innerPadding ->
        if (room == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Configuring secure workspace session...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Roster section: Horizontal list of participants
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COLLABORATORS PRESENT IN ROOM:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active list of peers
                            val participants = room.activeParticipants.split(",")
                            participants.forEach { part ->
                                val name = part.trim()
                                if (name.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(
                                                    color = if (name.contains("You", ignoreCase = true) || name.contains(room.creatorName)) MaterialTheme.colorScheme.primaryContainer 
                                                           else MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (name.contains("You", ignoreCase = true) || name.contains(room.creatorName)) MaterialTheme.colorScheme.onPrimaryContainer 
                                                       else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sub tabs: Whiteboard, Live Chat, Milestones
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Whiteboard Pad", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.BorderColor, contentDescription = null) },
                        modifier = Modifier.testTag("room_tab_whiteboard")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Peer Chat", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                        modifier = Modifier.testTag("room_tab_chat")
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Milestones", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.PlaylistAddCheck, contentDescription = null) },
                        modifier = Modifier.testTag("room_tab_milestones")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        0 -> {
                            // --- WHITEBOARD & REAL-TIME SCRATCHPAD TAB ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                // Sub navigation chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = subTab == 0,
                                        onClick = { subTab = 0 },
                                        label = { Text("📝 Collaborative Document") },
                                        modifier = Modifier.weight(1f).testTag("subtab_doc")
                                    )
                                    FilterChip(
                                        selected = subTab == 1,
                                        onClick = { subTab = 1 },
                                        label = { Text("💡 Brainstorm Scratchpad") },
                                        modifier = Modifier.weight(1f).testTag("subtab_scratchpad")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (subTab == 0) {
                                    // --- 1. COLLABORATIVE DOCUMENT ---
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Auto-Saving Shared Notes",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Changes sync in real-time across peers",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.updateCollaborativeNotes(roomId, noteText)
                                                        viewModel.showToast("Whiteboard notes saved and synced manually!")
                                                    },
                                                    modifier = Modifier.testTag("save_notes_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDone,
                                                        contentDescription = "Sync manually",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = noteText,
                                                onValueChange = { noteText = it },
                                                placeholder = { Text("Draft code architectures, key proposals, or workflow plans here...") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .weight(1f)
                                                    .testTag("whiteboard_note_input")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // AI Co-author button
                                    Button(
                                        onClick = { viewModel.coAuthorNotesWithPeer(roomId, noteText) },
                                        enabled = !isAILoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .testTag("ai_coauthor_notes_btn")
                                    ) {
                                        if (isAILoading) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI Peer co-authoring text...")
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Co-Author with AI Peer Twin (+10 XP)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // --- 2. REAL-TIME BRAINSTORMING SCRATCHPAD ---
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        // Header actions row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Shared Ideas (${scratchpadItems.size})",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            
                                            // On-demand Peer Brainstorming button
                                            Button(
                                                onClick = { viewModel.requestPeerBrainstormContribution(roomId) },
                                                enabled = !isAILoading,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("peer_brainstorm_trigger_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Brainstorm with Peers", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Scratchpad Items list
                                        if (scratchpadItems.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lightbulb,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = "Collaborative Scratchpad Empty",
                                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Jot down a quick development idea, flow draft, or feature query below to brainstorm live with your team!",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(scratchpadItems, key = { it.id }) { item ->
                                                    val isUser = item.authorName.contains("You") || item.authorName == viewModel.profile.value?.name
                                                    Card(
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                                           else MaterialTheme.colorScheme.surface
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(
                                                            width = 1.dp,
                                                            color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                                   else MaterialTheme.colorScheme.outlineVariant
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("scratchpad_item_${item.id}")
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            // Header: Author details
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(24.dp)
                                                                        .background(
                                                                            color = if (isUser) MaterialTheme.colorScheme.primary
                                                                                   else MaterialTheme.colorScheme.secondary,
                                                                            shape = CircleShape
                                                                        ),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = item.authorName.take(1).uppercase(),
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = Color.White
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = item.authorName,
                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Text(
                                                                    text = item.authorRole,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.height(6.dp))

                                                            // Brainstorming point content
                                                            Text(
                                                                text = item.content,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )

                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            // Interaction bar (Upvote & Delete)
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                // Upvote chip
                                                                AssistChip(
                                                                    onClick = { viewModel.upvoteScratchpadItem(item.id) },
                                                                    label = { Text("${item.upvotes} Upvotes") },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = Icons.Default.ThumbUp,
                                                                            contentDescription = "Upvote idea",
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    },
                                                                    colors = AssistChipDefaults.assistChipColors(
                                                                        containerColor = if (item.upvotes > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                                                       else Color.Transparent,
                                                                        labelColor = if (item.upvotes > 0) MaterialTheme.colorScheme.primary
                                                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    ),
                                                                    modifier = Modifier.testTag("upvote_scratchpad_${item.id}")
                                                                )

                                                                // Delete button (Only for user-authored points)
                                                                if (isUser) {
                                                                    IconButton(
                                                                        onClick = { viewModel.deleteScratchpadItem(item.id) },
                                                                        modifier = Modifier
                                                                            .size(24.dp)
                                                                            .testTag("delete_scratchpad_${item.id}")
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Delete,
                                                                            contentDescription = "Delete idea",
                                                                            tint = MaterialTheme.colorScheme.error,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Real-time Input Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(bottom = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = scratchpadInputText,
                                                onValueChange = { scratchpadInputText = it },
                                                placeholder = { Text("Post quick brainstorming point...") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                keyboardOptions = KeyboardOptions(
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        if (scratchpadInputText.isNotBlank()) {
                                                            viewModel.addScratchpadItem(roomId, scratchpadInputText)
                                                            scratchpadInputText = ""
                                                            focusManager.clearFocus()
                                                        }
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("scratchpad_input_field")
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    if (scratchpadInputText.isNotBlank()) {
                                                        viewModel.addScratchpadItem(roomId, scratchpadInputText)
                                                        scratchpadInputText = ""
                                                        focusManager.clearFocus()
                                                    }
                                                },
                                                enabled = scratchpadInputText.isNotBlank(),
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (scratchpadInputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .testTag("scratchpad_send_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Post idea",
                                                    tint = if (scratchpadInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // --- LIVE PEER CHAT TAB ---
                            Column(modifier = Modifier.fillMaxSize()) {
                                // List of messages
                                LazyColumn(
                                    state = chatListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(messages) { msg ->
                                        val isSystem = msg.senderRole == "System"
                                        val isUser = msg.senderName == viewModel.profile.value?.name

                                        if (isSystem) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = msg.content,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    modifier = Modifier
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                            ) {
                                                if (!isUser) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(
                                                                color = MaterialTheme.colorScheme.primary,
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = msg.senderName.take(1).uppercase(),
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isUser) MaterialTheme.colorScheme.primary 
                                                                       else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    shape = RoundedCornerShape(
                                                        topStart = 12.dp,
                                                        topEnd = 12.dp,
                                                        bottomStart = if (isUser) 12.dp else 0.dp,
                                                        bottomEnd = if (isUser) 0.dp else 12.dp
                                                    ),
                                                    modifier = Modifier.widthIn(max = 280.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        if (!isUser) {
                                                            Text(
                                                                text = "${msg.senderName} (${msg.senderRole})",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                        }
                                                        Text(
                                                            text = msg.content,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isUser) MaterialTheme.colorScheme.onPrimary 
                                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Typing indicator simulated
                                if (isAILoading) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Peer is drafting proposal response...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // Message input row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = chatInputText,
                                        onValueChange = { chatInputText = it },
                                        placeholder = { Text("Ask peers, suggest schemas, or coordinate code...") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = ImeAction.Send
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSend = {
                                                if (chatInputText.isNotBlank()) {
                                                    viewModel.sendTechRoomMessage(roomId, chatInputText)
                                                    chatInputText = ""
                                                    focusManager.clearFocus()
                                                }
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("room_chat_input_field")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (chatInputText.isNotBlank()) {
                                                viewModel.sendTechRoomMessage(roomId, chatInputText)
                                                chatInputText = ""
                                                focusManager.clearFocus()
                                            }
                                        },
                                        enabled = chatInputText.isNotBlank(),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .testTag("room_chat_send_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send message",
                                            tint = if (chatInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // --- PROJECT MILESTONES TAB ---
                            var milestonesState = remember {
                                mutableStateListOf(
                                    Pair("Define MVP System Architecture blueprint 📋", true),
                                    Pair("Establish SQLite database Room schemas 🗄️", false),
                                    Pair("Build Jetpack Compose presentation interface scaffold 🎨", false),
                                    Pair("Deploy offline-ready Gemini co-author API integrations ⚡", false),
                                    Pair("Perform robust integration testing on P2P roster 🔐", false)
                                )
                            }

                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stars,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Coordinate real-time tasks with your peer roster. Checking completed tasks awards +5 XP instantly!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                items(milestonesState.size) { index ->
                                    val (taskTitle, isDone) = milestonesState[index]
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("milestone_card_$index")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    milestonesState[index] = Pair(taskTitle, !isDone)
                                                    if (!isDone) {
                                                        viewModel.awardXp(5)
                                                        viewModel.showToast("Milestone completed! +5 XP 🎉")
                                                    }
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Checkbox(
                                                checked = isDone,
                                                onCheckedChange = { checked ->
                                                    milestonesState[index] = Pair(taskTitle, checked)
                                                    if (checked) {
                                                        viewModel.awardXp(5)
                                                        viewModel.showToast("Milestone completed! +5 XP 🎉")
                                                    }
                                                },
                                                modifier = Modifier.testTag("milestone_checkbox_$index")
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = taskTitle,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold
                                                ),
                                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                                       else MaterialTheme.colorScheme.onSurface
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
}
