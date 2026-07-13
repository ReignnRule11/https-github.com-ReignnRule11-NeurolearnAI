package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import com.example.ui.MindMapState
import com.example.data.MindMapNode
import kotlinx.coroutines.launch
import com.example.data.MindMapEdge
import com.example.data.MindMapGraph
import com.example.data.Flashcard
import com.example.data.FlashcardRatingResult
import com.example.data.FlashcardDeck
import com.example.data.FirestoreDeck
import com.example.data.FirestoreFlashcard
import com.example.data.ChatMessage
import com.example.ui.MainViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import java.util.Locale
import org.json.JSONObject
import com.example.api.GeminiClient
import com.example.R

@Composable
fun ReviewScreen(viewModel: MainViewModel) {
    val allDecks by viewModel.allDecks.collectAsState()
    val allCards by viewModel.allFlashcards.collectAsState()
    
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showMindMapDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Local storage, 1 = Firestore live sync
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedDeckId == null) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Local Study Hub", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Local storage") },
                        modifier = Modifier.testTag("local_tab")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Firestore Cloud Hub", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = "Firestore sync") },
                        modifier = Modifier.testTag("firestore_tab")
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (activeTab == 0) {
                    if (selectedDeckId == null) {
                        DecksDashboard(
                            decks = allDecks,
                            allCards = allCards,
                            onCreateDeckClick = { showCreateDeckDialog = true },
                            onDeckSelect = { selectedDeckId = it },
                            onSyncClick = { viewModel.syncDecksAndCardsToFirestore() },
                            onPullClick = { viewModel.pullDecksAndCardsFromFirestore() },
                            onShowMindMap = {
                                viewModel.generateMindMapForDeck("All Concepts", null)
                                showMindMapDialog = true
                            },
                            onDeleteCard = { viewModel.deleteFlashcard(it) }
                        )
                    } else {
                        val deck = allDecks.find { it.id == selectedDeckId } ?: allDecks.firstOrNull { it.id == "default" }
                        if (deck == null) {
                            selectedDeckId = null
                        } else {
                            DeckDetailAndStudyView(
                                deck = deck,
                                allCards = allCards,
                                viewModel = viewModel,
                                onBack = { selectedDeckId = null },
                                onAddCardClick = { showAddCardDialog = true },
                                onDeleteDeck = {
                                    viewModel.deleteDeck(deck.id)
                                    selectedDeckId = null
                                },
                                onShowMindMap = {
                                    viewModel.generateMindMapForDeck(deck.name, deck.id)
                                    showMindMapDialog = true
                                }
                            )
                        }
                    }
                } else {
                    FirestoreLiveHub(viewModel = viewModel)
                }
            }
        }
        
        // Mind Map Dialog Overlay
        if (showMindMapDialog && activeTab == 0) {
            MindMapDialog(
                viewModel = viewModel,
                onDismiss = { showMindMapDialog = false }
            )
        }
        
        // Create Deck Dialog
        if (showCreateDeckDialog && activeTab == 0) {
            CreateDeckDialog(
                onDismiss = { showCreateDeckDialog = false },
                onCreate = { name, desc, subject ->
                    viewModel.createDeck(name, desc, subject)
                    showCreateDeckDialog = false
                }
            )
        }
        
        // Add Flashcard Dialog
        if (showAddCardDialog && selectedDeckId != null && activeTab == 0) {
            AddFlashcardDialog(
                onDismiss = { showAddCardDialog = false },
                onAdd = { question, answer, diff, tags ->
                    viewModel.addCustomFlashcard(selectedDeckId!!, question, answer, diff, tags)
                    showAddCardDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksDashboard(
    decks: List<FlashcardDeck>,
    allCards: List<Flashcard>,
    onCreateDeckClick: () -> Unit,
    onDeckSelect: (String) -> Unit,
    onSyncClick: () -> Unit,
    onPullClick: () -> Unit,
    onShowMindMap: () -> Unit,
    onDeleteCard: (Int) -> Unit
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Unique subjects from decks
    val subjects = remember(decks) {
        decks.map { it.subject }.filter { it.isNotBlank() }.distinct()
    }
    
    // Unique tags from flashcards
    val tags = remember(allCards) {
        allCards.flatMap { card ->
            card.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.distinct()
    }
    
    val filteredDecks = remember(decks, selectedSubject) {
        if (selectedSubject != null) {
            decks.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
        } else {
            decks
        }
    }
    
    val filteredCards = remember(allCards, selectedSubject, selectedTag, decks) {
        if (selectedTag != null) {
            allCards.filter { card ->
                card.tags.split(",").map { it.trim() }.any { it.equals(selectedTag, ignoreCase = true) }
            }
        } else if (selectedSubject != null) {
            val deckIds = decks.filter { it.subject.equals(selectedSubject, ignoreCase = true) }.map { it.id }
            allCards.filter { deckIds.contains(it.deckId) }
        } else {
            allCards
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Browse Filters",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    HorizontalDivider()
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option to clear filters
                        item {
                            NavigationDrawerItem(
                                label = { Text("All Decks & Cards", fontWeight = FontWeight.Bold) },
                                selected = selectedSubject == null && selectedTag == null,
                                onClick = {
                                    selectedSubject = null
                                    selectedTag = null
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Folder, contentDescription = null) }
                            )
                        }
                        
                        // Subjects Section
                        item {
                            Text(
                                text = "SUBJECTS (${subjects.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        if (subjects.isEmpty()) {
                            item {
                                Text(
                                    text = "No subjects categorized yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        } else {
                            items(subjects) { sub ->
                                val count = decks.count { it.subject.equals(sub, ignoreCase = true) }
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sub)
                                            Badge { Text("$count") }
                                        }
                                    },
                                    selected = selectedSubject == sub,
                                    onClick = {
                                        selectedSubject = sub
                                        selectedTag = null
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                        
                        // Tags Section
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "FLASHCARD TAGS (${tags.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        if (tags.isEmpty()) {
                            item {
                                Text(
                                    text = "No tags added yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        } else {
                            items(tags) { tag ->
                                val count = allCards.count { card ->
                                    card.tags.split(",").map { it.trim() }.any { it.equals(tag, ignoreCase = true) }
                                }
                                NavigationDrawerItem(
                                    label = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("#$tag")
                                            Badge { Text("$count") }
                                        }
                                    },
                                    selected = selectedTag == tag,
                                    onClick = {
                                        selectedTag = tag
                                        selectedSubject = null
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(20.dp)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onCreateDeckClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("create_deck_fab"),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Create Deck") },
                    text = { Text("Create Deck", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Screen Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Study Decks",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        scope.launch { drawerState.open() }
                                    },
                                    modifier = Modifier.testTag("filter_sidebar_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filter by tags/subjects",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "SM-2 active recall training",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Cloud Sync Badge / Trigger
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = onPullClick,
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Pull from Firestore", modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = onSyncClick,
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Push to Firestore", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Show active filter indicator if any
                    if (selectedSubject != null || selectedTag != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Column {
                                        Text(
                                            text = "Filtering by: " + (selectedSubject ?: "#$selectedTag"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Showing matching decks and individual cards",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        selectedSubject = null
                                        selectedTag = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                ) {
                                    Text("Clear", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        // Firestore hint card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Cloud Synced",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Firestore Integration Active",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Your decks are fully backed up to the Firestore database. Study offline or sync on multiple devices.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Visual Mind Map Card for all concepts
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowMindMap() }
                                .testTag("visualize_all_mind_map_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mastery Mind Map",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Visualize connections across all your decks and concepts in a single cohesive AI structural hierarchy graph.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Visualize Map",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Text(
                        text = if (selectedSubject != null) "Matching Collections" else "Your Collections",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                if (filteredDecks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "No decks",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No Matching Decks",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredDecks) { deck ->
                        val deckCards = allCards.filter { it.deckId == deck.id }
                        val dueCount = deckCards.filter { it.nextReviewDate <= System.currentTimeMillis() }.size
                        
                        DeckRowItem(
                            deck = deck,
                            totalCards = deckCards.size,
                            dueCards = dueCount,
                            onClick = { onDeckSelect(deck.id) }
                        )
                    }
                }
                
                // Show matching flashcards list if any filter is active
                if (selectedSubject != null || selectedTag != null) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Matching Flashcards (${filteredCards.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    if (filteredCards.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching individual cards.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        items(filteredCards) { card ->
                            val deckName = decks.find { it.id == card.deckId }?.name ?: "General Knowledge"
                            FlashcardBrowseItem(
                                card = card,
                                deckName = deckName,
                                onDelete = { onDeleteCard(card.id) }
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun FlashcardBrowseItem(
    card: Flashcard,
    deckName: String,
    onDelete: () -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isRevealed = !isRevealed }
            .testTag("browse_card_item_${card.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isRevealed) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deck Name badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Difficulty Badge
                    Surface(
                        color = when (card.difficulty) {
                            "Easy" -> Color(0xFFE8F5E9)
                            "Hard" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = card.difficulty,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (card.difficulty) {
                                "Easy" -> Color(0xFF2E7D32)
                                "Hard" -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Flashcard",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Question section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Q: " + card.question,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Answer (conditionally revealed or with a tap hint)
                if (isRevealed) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "A: " + card.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Tap to reveal answer",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            // Tags Row
            if (card.tags.isNotBlank()) {
                val tagsList = card.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (tagsList.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tagsList.forEach { t ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "#$t",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeckRowItem(
    deck: FlashcardDeck,
    totalCards: Int,
    dueCards: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("deck_item_${deck.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Deck Icon/Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (deck.description.isNotEmpty()) {
                    Text(
                        text = deck.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total cards badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$totalCards cards",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Due cards indicator
                    if (dueCards > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$dueCards due",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else if (totalCards > 0) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Perfect",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Subject Tag Badge
                    if (deck.subject.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = deck.subject,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DeckDetailAndStudyView(
    deck: FlashcardDeck,
    allCards: List<Flashcard>,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAddCardClick: () -> Unit,
    onDeleteDeck: () -> Unit,
    onShowMindMap: () -> Unit
) {
    var isStudying by remember { mutableStateOf(false) }
    var studyAllMode by remember { mutableStateOf(false) }
    
    val deckCards = allCards.filter { it.deckId == deck.id }
    val dueCards = deckCards.filter { it.nextReviewDate <= System.currentTimeMillis() }
    
    if (isStudying) {
        val activeSessionCards = if (studyAllMode) deckCards else dueCards
        ActiveStudySession(
            deckId = deck.id,
            deckName = deck.name,
            cards = activeSessionCards,
            viewModel = viewModel,
            onQuit = { isStudying = false }
        )
    } else {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            text = deck.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (deck.id != "default") {
                            IconButton(onClick = onDeleteDeck) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Deck", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddCardClick,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.AddCard, contentDescription = "Add Card") },
                    text = { Text("Add Flashcard", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Deck Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = deck.description.ifEmpty { "No custom description provided for this collection." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Due Cards Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = dueCards.size.toString(),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Due Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                
                                // Total Cards Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = deckCards.size.toString(),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Total Cards",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Session launcher buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                studyAllMode = false
                                isStudying = true
                            },
                            enabled = dueCards.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(54.dp)
                                .testTag("start_due_session_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null)
                                Text("Review Due (${dueCards.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = {
                                studyAllMode = true
                                isStudying = true
                            },
                            enabled = deckCards.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("start_all_session_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.School, contentDescription = null)
                                Text("Study All", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Mind Map Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowMindMap() }
                            .testTag("visualize_mind_map_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Generate Concept Mind Map",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Use Gemini to analyze your flashcard content and construct an interactive visual graph representation of the concept hierarchy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Visualize",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Digital Twin Tutor Callout Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startDeckTutorSession(deck.id) }
                            .testTag("ai_deck_tutor_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chat with AI Digital Twin",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Review this deck via interactive Socratic chat, analogies, and quick quizzes tailored to your twin's personality.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Start Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Deck Contents (${deckCards.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                if (deckCards.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "This Deck is Empty",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Click 'Add Flashcard' below to build custom spaced-repetition notes for this deck.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(deckCards) { card ->
                        var isExpanded by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Q: ${card.question}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.deleteFlashcard(card.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Flashcard",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                
                                AnimatedVisibility(visible = isExpanded) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "A: ${card.answer}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("Interval: ${card.intervalDays}d") }
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("Ease: ${"%.2f".format(card.easeFactor)}") }
                                            )
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("Difficulty: ${card.difficulty}") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

enum class StudyMode {
    ACTIVE_RECALL,
    REVIEW,
    QUIZ
}

@Composable
fun ActiveStudySession(
    deckId: String,
    deckName: String,
    cards: List<Flashcard>,
    viewModel: MainViewModel,
    onQuit: () -> Unit
) {
    val sessionResults = remember { mutableStateListOf<FlashcardRatingResult>() }
    var showResultsSummary by remember { mutableStateOf(false) }

    if (showResultsSummary) {
        ActiveRecallSummaryScreen(
            deckName = deckName,
            results = sessionResults,
            viewModel = viewModel,
            onClose = {
                viewModel.clearActiveRecallSummary()
                viewModel.clearDeckContentSummary()
                onQuit()
            }
        )
        return
    }

    var currentCardIndex by remember { mutableStateOf(0) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var studyMode by remember { mutableStateOf(StudyMode.ACTIVE_RECALL) }
    var showTwinChat by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        viewModel.initializeDeckChatContext(deckId)
    }

    var quizQuestions by remember { mutableStateOf<List<com.example.ui.QuizQuestion>>(emptyList()) }
    var isQuizLoading by remember { mutableStateOf(false) }
    var quizError by remember { mutableStateOf<String?>(null) }
    var currentQuizIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isQuizAnswerChecked by remember { mutableStateOf(false) }
    var quizScore by remember { mutableStateOf(0) }
    var isQuizFinished by remember { mutableStateOf(false) }

    val currentProfileState by viewModel.profile.collectAsState()
    val twinAvatarState = currentProfileState?.selectedTwinAvatar ?: "socratic"
    val twinNameState = when (twinAvatarState) {
        "tech" -> "Tech Visionary"
        "scholar" -> "Scholar Academic"
        "creative" -> "Creative Innovator"
        else -> "Socratic Mentor"
    }
    val avatarResState = when (twinAvatarState) {
        "tech" -> R.drawable.img_twin_tech
        "scholar" -> R.drawable.img_twin_scholar
        "creative" -> R.drawable.img_twin_creative
        else -> R.drawable.img_twin_socratic
    }

    LaunchedEffect(studyMode) {
        if (studyMode == StudyMode.QUIZ && quizQuestions.isEmpty()) {
            isQuizLoading = true
            quizError = null
            try {
                quizQuestions = generateTwinQuiz(deckName, cards, twinNameState)
            } catch (e: Exception) {
                quizError = "Failed to design quiz: ${e.message}"
            } finally {
                isQuizLoading = false
            }
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var spokenAnswer by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordError by remember { mutableStateOf<String?>(null) }
    var isEvaluating by remember { mutableStateOf(false) }
    var evaluationResult by remember { mutableStateOf<VerbalEvaluation?>(null) }

    val speechState = remember(context) {
        SpeechRecognizerState(
            context = context,
            onTranscriptionUpdated = { text -> spokenAnswer = text },
            onErrorOccurred = { err -> recordError = err }
        )
    }

    DisposableEffect(speechState) {
        onDispose {
            speechState.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                recordError = null
                isRecording = true
                spokenAnswer = ""
                speechState.startListening()
            } else {
                recordError = "Microphone permission is required to speak answers."
            }
        }
    )

    LaunchedEffect(currentCardIndex) {
        spokenAnswer = ""
        isRecording = false
        recordError = null
        isEvaluating = false
        evaluationResult = null
    }
    
    val currentCard = cards.getOrNull(currentCardIndex)
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && currentCard != null) {
                    when (event.key) {
                        Key.Spacebar -> {
                            if (studyMode == StudyMode.ACTIVE_RECALL && !isAnswerRevealed) {
                                isAnswerRevealed = true
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionLeft -> {
                            if (studyMode == StudyMode.ACTIVE_RECALL) {
                                if (isAnswerRevealed) {
                                    val result = FlashcardRatingResult(currentCard.question, currentCard.answer, 1)
                                    sessionResults.add(result)
                                    viewModel.rateFlashcard(currentCard, 1)
                                    if (currentCardIndex + 1 < cards.size) {
                                        currentCardIndex++
                                        isAnswerRevealed = false
                                    } else {
                                        showResultsSummary = true
                                    }
                                    true
                                } else {
                                    false
                                }
                            } else { // Review mode
                                if (currentCardIndex > 0) {
                                    currentCardIndex--
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                        Key.DirectionRight -> {
                            if (studyMode == StudyMode.ACTIVE_RECALL) {
                                if (isAnswerRevealed) {
                                    val result = FlashcardRatingResult(currentCard.question, currentCard.answer, 3)
                                    sessionResults.add(result)
                                    viewModel.rateFlashcard(currentCard, 3)
                                    if (currentCardIndex + 1 < cards.size) {
                                        currentCardIndex++
                                        isAnswerRevealed = false
                                    } else {
                                        showResultsSummary = true
                                    }
                                    true
                                } else {
                                    false
                                }
                            } else { // Review mode
                                val isLastCard = currentCardIndex == cards.size - 1
                                if (isLastCard) {
                                    onQuit()
                                } else {
                                    currentCardIndex++
                                }
                                true
                            }
                        }
                        Key.DirectionDown -> {
                            if (studyMode == StudyMode.ACTIVE_RECALL) {
                                if (isAnswerRevealed) {
                                    val result = FlashcardRatingResult(currentCard.question, currentCard.answer, 2)
                                    sessionResults.add(result)
                                    viewModel.rateFlashcard(currentCard, 2)
                                    if (currentCardIndex + 1 < cards.size) {
                                        currentCardIndex++
                                        isAnswerRevealed = false
                                    } else {
                                        showResultsSummary = true
                                    }
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                        Key.DirectionUp -> {
                            studyMode = if (studyMode == StudyMode.ACTIVE_RECALL) StudyMode.REVIEW else StudyMode.ACTIVE_RECALL
                            isAnswerRevealed = false
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .padding(24.dp)
    ) {
        // Session Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onQuit) {
                Icon(Icons.Default.Close, contentDescription = "Quit Session")
            }
            Text(
                text = deckName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { showTwinChat = !showTwinChat },
                modifier = Modifier.testTag("toggle_twin_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Chat with AI Twin",
                    tint = if (showTwinChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Mode Selector Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
                .testTag("study_mode_toggle_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (studyMode == StudyMode.ACTIVE_RECALL) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { 
                        studyMode = StudyMode.ACTIVE_RECALL 
                        isAnswerRevealed = false
                    }
                    .padding(vertical = 8.dp)
                    .testTag("active_recall_mode_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Active Recall Mode",
                        tint = if (studyMode == StudyMode.ACTIVE_RECALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Active Recall",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (studyMode == StudyMode.ACTIVE_RECALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (studyMode == StudyMode.REVIEW) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { studyMode = StudyMode.REVIEW }
                    .padding(vertical = 8.dp)
                    .testTag("review_mode_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Review Mode",
                        tint = if (studyMode == StudyMode.REVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (studyMode == StudyMode.REVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (studyMode == StudyMode.QUIZ) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { studyMode = StudyMode.QUIZ }
                    .padding(vertical = 8.dp)
                    .testTag("quiz_mode_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = "Twin Quiz Mode",
                        tint = if (studyMode == StudyMode.QUIZ) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Twin Quiz",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (studyMode == StudyMode.QUIZ) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (studyMode == StudyMode.QUIZ) {
            if (isQuizLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = avatarResState),
                        contentDescription = "Twin Avatar",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Formulating Twin Quiz...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your digital twin, $twinNameState, is analyzing your retention patterns to design a personalized 5-question multiple choice quiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (quizError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Quiz Generation Failed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quizError ?: "An unexpected error occurred.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isQuizLoading = true
                                quizError = null
                                try {
                                    quizQuestions = generateTwinQuiz(deckName, cards, twinNameState)
                                } catch (e: Exception) {
                                    quizError = "Failed to design quiz: ${e.message}"
                                } finally {
                                    isQuizLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Try Again")
                    }
                }
            } else if (isQuizFinished) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = avatarResState),
                        contentDescription = "Twin Avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Quiz Completed!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Retrospective conceptual analysis by $twinNameState",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$quizScore/5",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Correct",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "$twinNameState's Retrospective:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val feedbackText = when (quizScore) {
                                5 -> "An absolute masterclass! You scored 100%. Your cognitive model predicts exceptional conceptual stability for these items. Keep up this magnificent pace!"
                                4 -> "Outstanding retention! You scored 80%. A few very minor conceptual nuances are still locking in, but you've demonstrated incredibly reliable recall."
                                3 -> "Solid grasp! You got 60%. You're in a stable learning transition, but we can do even better. Take some time to review your Hard rated items."
                                else -> "Keep studying! Active recall is all about iterative retrieval strength. Let's do a short review session and retake this quiz to lock in the neural pathways."
                            }
                            Text(
                                text = feedbackText,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isQuizLoading = true
                                    quizError = null
                                    currentQuizIndex = 0
                                    selectedOption = null
                                    isQuizAnswerChecked = false
                                    quizScore = 0
                                    isQuizFinished = false
                                    try {
                                        quizQuestions = generateTwinQuiz(deckName, cards, twinNameState)
                                    } catch (e: Exception) {
                                        quizError = "Failed to design quiz: ${e.message}"
                                    } finally {
                                        isQuizLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retake")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake Quiz")
                        }
                        
                        Button(
                            onClick = {
                                studyMode = StudyMode.ACTIVE_RECALL
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back to Cards")
                        }
                    }
                }
            } else {
                val question = quizQuestions.getOrNull(currentQuizIndex)
                if (question != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentQuizIndex + 1} of 5",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Score: $quizScore/5",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { (currentQuizIndex + 1).toFloat() / 5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = question.question,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 26.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            question.options.forEach { option ->
                                val isSelected = selectedOption == option
                                val isCorrect = option == question.correctAnswer
                                
                                val borderStroke = when {
                                    isQuizAnswerChecked && isCorrect -> BorderStroke(2.dp, Color(0xFF43A047))
                                    isQuizAnswerChecked && isSelected && !isCorrect -> BorderStroke(2.dp, Color(0xFFE53935))
                                    isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                                
                                val containerColor = when {
                                    isQuizAnswerChecked && isCorrect -> Color(0xFFE8F5E9)
                                    isQuizAnswerChecked && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                
                                val textColor = when {
                                    isQuizAnswerChecked && isCorrect -> Color(0xFF2E7D32)
                                    isQuizAnswerChecked && isSelected && !isCorrect -> Color(0xFFC62828)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isQuizAnswerChecked) {
                                            selectedOption = option
                                        },
                                    border = borderStroke,
                                    colors = CardDefaults.cardColors(containerColor = containerColor),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isQuizAnswerChecked) {
                                            if (isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Correct",
                                                    tint = Color(0xFF43A047),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = "Incorrect",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        1.5.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                        CircleShape
                                                    )
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.onPrimary)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (isQuizAnswerChecked && question.explanation.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = avatarResState),
                                        contentDescription = "Twin Avatar",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column {
                                        Text(
                                            text = "$twinNameState's Explanation:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = question.explanation,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (!isQuizAnswerChecked) {
                            Button(
                                onClick = {
                                    if (selectedOption != null) {
                                        isQuizAnswerChecked = true
                                        if (selectedOption == question.correctAnswer) {
                                            quizScore++
                                        }
                                    }
                                },
                                enabled = selectedOption != null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Submit Answer")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentQuizIndex + 1 < 5) {
                                        currentQuizIndex++
                                        selectedOption = null
                                        isQuizAnswerChecked = false
                                    } else {
                                        isQuizFinished = true
                                        viewModel.completeQuiz(quizScore)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(if (currentQuizIndex == 4) "Finish Quiz" else "Next Question")
                            }
                        }
                    }
                }
            }
        } else {
            if (currentCard != null) {
            // Deck progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Card ${currentCardIndex + 1} of ${cards.size}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (studyMode == StudyMode.ACTIVE_RECALL) {
                    Text(
                        text = "Interval: ${currentCard.intervalDays}d",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "View Mode",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (currentCardIndex + 1).toFloat() / cards.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Flashcard main viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("flashcard_container"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "QUESTION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = currentCard.question,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 30.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight()
                                .testTag("card_question_text")
                        )
                        
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        if (studyMode == StudyMode.ACTIVE_RECALL) {
                            // Resolve Twin info
                            val currentProfile by viewModel.profile.collectAsState()
                            val twinAvatar = currentProfile?.selectedTwinAvatar ?: "socratic"
                            val twinName = when (twinAvatar) {
                                "tech" -> "Tech Visionary"
                                "scholar" -> "Scholar Academic"
                                "creative" -> "Creative Innovator"
                                else -> "Socratic Mentor"
                            }
                            val avatarRes = when (twinAvatar) {
                                "tech" -> R.drawable.img_twin_tech
                                "scholar" -> R.drawable.img_twin_scholar
                                "creative" -> R.drawable.img_twin_creative
                                else -> R.drawable.img_twin_socratic
                            }

                            AnimatedVisibility(
                                visible = isAnswerRevealed,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "ANSWER",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4CAF50)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = currentCard.answer,
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("card_answer_text")
                                        )
                                    }

                                    // Vocal Recall Evaluation Feedback Card
                                    if (evaluationResult != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .testTag("verbal_recall_feedback_card"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Twin Avatar Header
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = avatarRes),
                                                        contentDescription = "AI Twin Avatar",
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Column {
                                                        Text(
                                                            text = "$twinName's Vocal Evaluation",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Text(
                                                            text = "Concept Match: ${evaluationResult!!.score}%",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }

                                                // Conceptual Score Indicator Bar
                                                LinearProgressIndicator(
                                                    progress = { evaluationResult!!.score.toFloat() / 100f },
                                                    color = when {
                                                        evaluationResult!!.score >= 75 -> Color(0xFF43A047)
                                                        evaluationResult!!.score >= 40 -> Color(0xFFFFB300)
                                                        else -> Color(0xFFE53935)
                                                    },
                                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                )

                                                // Verbal feedback text
                                                Text(
                                                    text = evaluationResult!!.feedback,
                                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )

                                                // Quick Grade Selection accept recommendation
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            // Accept Twin's Recommended Grade and trigger corresponding action!
                                                            val rating = when (evaluationResult!!.ratingRecommend.lowercase(Locale.getDefault())) {
                                                                "easy" -> 3
                                                                "good" -> 2
                                                                else -> 1
                                                            }
                                                            val result = FlashcardRatingResult(currentCard.question, currentCard.answer, rating)
                                                            sessionResults.add(result)
                                                            viewModel.rateFlashcard(currentCard, rating)
                                                            if (currentCardIndex + 1 < cards.size) {
                                                                currentCardIndex++
                                                                isAnswerRevealed = false
                                                            } else {
                                                                showResultsSummary = true
                                                            }
                                                        }
                                                        .testTag("apply_twin_grade_button")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ThumbUp,
                                                                contentDescription = "Apply grade",
                                                                tint = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Column {
                                                                Text(
                                                                    text = "Apply Twin's Recommended Grade",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                    text = "Automatically rates this card as \"${evaluationResult!!.ratingRecommend}\"",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Default.ChevronRight,
                                                            contentDescription = "Apply",
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!isAnswerRevealed) {
                                // Socratic Vocal Recall Input Widget
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .testTag("verbal_recall_section"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RecordVoiceOver,
                                                contentDescription = "Speak Answer",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Vocal Recall Evaluation",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        Text(
                                            text = "Speak your answer. Your Socratic twin will evaluate your active recall depth conceptually!",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        
                                        if (isRecording) {
                                            // Live waveform visualizer
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AudioWaveformVisualizer(
                                                    isListening = true,
                                                    rmsDb = speechState.rmsDb,
                                                    modifier = Modifier.height(32.dp)
                                                )
                                            }
                                            
                                            Text(
                                                text = if (spokenAnswer.isBlank()) "Listening..." else spokenAnswer,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().testTag("live_speech_transcription")
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    isRecording = false
                                                    speechState.stopListening()
                                                    if (spokenAnswer.isNotBlank()) {
                                                        isEvaluating = true
                                                        scope.launch {
                                                            val evalResult1 = evaluateVerbalAnswerWithGemini(
                                                                spoken = spokenAnswer,
                                                                expected = currentCard.answer,
                                                                question = currentCard.question
                                                            )
                                                            evaluationResult = evalResult1
                                                            viewModel.insertVerbalEvaluation(
                                                                com.example.data.VerbalRecallEvaluation(
                                                                    cardId = currentCard.id,
                                                                    deckId = currentCard.deckId,
                                                                    question = currentCard.question,
                                                                    expectedAnswer = currentCard.answer,
                                                                    spokenAnswer = spokenAnswer,
                                                                    score = evalResult1.score,
                                                                    feedback = evalResult1.feedback
                                                                )
                                                            )
                                                            isEvaluating = false
                                                            isAnswerRevealed = true
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("stop_recording_button")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop & Evaluate")
                                                    Text("Stop & Evaluate")
                                                }
                                            }
                                        } else {
                                            if (spokenAnswer.isNotBlank()) {
                                                Text(
                                                    text = "Spoken answer: \"$spokenAnswer\"",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = 8.dp).testTag("spoken_answer_text")
                                                )
                                            }
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val hasPermission = ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.RECORD_AUDIO
                                                        ) == PackageManager.PERMISSION_GRANTED
                                                        
                                                        if (hasPermission) {
                                                            recordError = null
                                                            isRecording = true
                                                            spokenAnswer = ""
                                                            speechState.startListening()
                                                        } else {
                                                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("start_voice_recall_button"),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Speak Answer")
                                                        Text(if (spokenAnswer.isBlank()) "Start Speaking" else "Re-record")
                                                    }
                                                }
                                                
                                                if (spokenAnswer.isNotBlank()) {
                                                    Button(
                                                        onClick = {
                                                            isEvaluating = true
                                                            scope.launch {
                                                                val evalResult2 = evaluateVerbalAnswerWithGemini(
                                                                    spoken = spokenAnswer,
                                                                    expected = currentCard.answer,
                                                                    question = currentCard.question
                                                                )
                                                                evaluationResult = evalResult2
                                                                viewModel.insertVerbalEvaluation(
                                                                    com.example.data.VerbalRecallEvaluation(
                                                                        cardId = currentCard.id,
                                                                        deckId = currentCard.deckId,
                                                                        question = currentCard.question,
                                                                        expectedAnswer = currentCard.answer,
                                                                        spokenAnswer = spokenAnswer,
                                                                        score = evalResult2.score,
                                                                        feedback = evalResult2.feedback
                                                                    )
                                                                )
                                                                isEvaluating = false
                                                                isAnswerRevealed = true
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("evaluate_voice_recall_button"),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Psychology, contentDescription = "Evaluate")
                                                            Text("Evaluate")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (isEvaluating) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    text = "Evaluating active recall...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        
                                        if (recordError != null) {
                                            Text(
                                                text = recordError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().testTag("record_error_msg")
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { isAnswerRevealed = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("reveal_answer_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "Reveal Answer")
                                        Text("Reveal Answer")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { showTwinChat = true },
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("ask_twin_about_card_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = "Ask Twin",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Ask Twin About This Card",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ANSWER",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = currentCard.answer,
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("review_card_answer_text")
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (studyMode == StudyMode.ACTIVE_RECALL) {
                // Grading buttons
                AnimatedVisibility(
                    visible = isAnswerRevealed,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hard button
                        Button(
                            onClick = {
                                val result = FlashcardRatingResult(currentCard.question, currentCard.answer, 1)
                                sessionResults.add(result)
                                viewModel.rateFlashcard(currentCard, 1)
                                if (currentCardIndex + 1 < cards.size) {
                                    currentCardIndex++
                                    isAnswerRevealed = false
                                } else {
                                    showResultsSummary = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("rate_hard_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.SentimentVeryDissatisfied, contentDescription = "Hard")
                                Text("Hard", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        
                        // Good button
                        Button(
                            onClick = {
                                val result = FlashcardRatingResult(currentCard.question, currentCard.answer, 2)
                                sessionResults.add(result)
                                viewModel.rateFlashcard(currentCard, 2)
                                if (currentCardIndex + 1 < cards.size) {
                                    currentCardIndex++
                                    isAnswerRevealed = false
                                } else {
                                    showResultsSummary = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("rate_good_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.SentimentNeutral, contentDescription = "Good")
                                Text("Good", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        
                        // Easy button
                        Button(
                            onClick = {
                                val result = FlashcardRatingResult(currentCard.question, currentCard.answer, 3)
                                sessionResults.add(result)
                                viewModel.rateFlashcard(currentCard, 3)
                                if (currentCardIndex + 1 < cards.size) {
                                    currentCardIndex++
                                    isAnswerRevealed = false
                                } else {
                                    showResultsSummary = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("rate_easy_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.SentimentSatisfiedAlt, contentDescription = "Easy")
                                Text("Easy", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            } else {
                // REVIEW MODE Navigation controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    OutlinedButton(
                        onClick = {
                            if (currentCardIndex > 0) {
                                currentCardIndex--
                            }
                        },
                        enabled = currentCardIndex > 0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("review_prev_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Card")
                            Text("Previous", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    
                    // Next / Finish button
                    val isLastCard = currentCardIndex == cards.size - 1
                    Button(
                        onClick = {
                            if (isLastCard) {
                                onQuit()
                            } else {
                                currentCardIndex++
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastCard) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("review_next_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isLastCard) "Finish" else "Next",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = if (isLastCard) Icons.Default.Check else Icons.Default.ArrowForward,
                                contentDescription = if (isLastCard) "Finish Review" else "Next Card"
                            )
                        }
                    }
                }
            }
        } else {
            // Deck empty / completed view
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Everything Clean! 🎉",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You've successfully cleared all active reviews for this session! Spaced Repetition has updated your memory models.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onQuit,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Finish Session")
                    }
                }
            }
        }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Keyboard Navigation Shortcut Hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = "Keyboard shortcuts",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (studyMode == StudyMode.ACTIVE_RECALL) {
                    "Keyboard: [Space] Flip • [←] Hard • [↓] Good • [→] Easy • [↑] Mode"
                } else {
                    "Keyboard: [←] Previous • [→] Next • [↑] Mode"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }

    AnimatedVisibility(
        visible = showTwinChat,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        TwinStudyChatDrawer(
            deckId = deckId,
            currentCard = currentCard,
            viewModel = viewModel,
            onDismiss = { showTwinChat = false },
            twinName = twinNameState,
            twinAvatar = twinAvatarState,
            avatarRes = avatarResState
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    var isError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Flashcard Deck",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Deck Name") },
                    placeholder = { Text("e.g. Calculus Derivatives") },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (isError) {
                    Text(
                        text = "Deck name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("What will you study in this deck?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Column {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (Tag)") },
                        placeholder = { Text("e.g. Calculus, Physics, History") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Select:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            "Calculus", "Computer Science", "Chemistry", "Biology",
                            "Product Management", "Software Development", "Web3 & Blockchain",
                            "E-commerce", "Business Analysis", "Product Design",
                            "Project Management", "Digital Marketing", "Data Analysis", "General"
                        ).forEach { sub ->
                            val isSelected = subject.equals(sub, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { subject = sub },
                                label = { Text(sub, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                isError = true
                            } else {
                                onCreate(name.trim(), description.trim(), subject.trim())
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlashcardDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }
    var tags by remember { mutableStateOf("") }
    
    var questionError by remember { mutableStateOf(false) }
    var answerError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Flashcard",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        question = it
                        questionError = false
                    },
                    label = { Text("Question") },
                    isError = questionError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        answerError = false
                    },
                    label = { Text("Answer") },
                    isError = answerError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    placeholder = { Text("e.g. formula, derivative, graph") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Difficulty row Selector
                Column {
                    Text(
                        text = "Difficulty:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach { diff ->
                            val isSelected = difficulty == diff
                            FilterChip(
                                selected = isSelected,
                                onClick = { difficulty = diff },
                                label = { Text(diff) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var hasError = false
                            if (question.trim().isEmpty()) {
                                questionError = true
                                hasError = true
                            }
                            if (answer.trim().isEmpty()) {
                                answerError = true
                                hasError = true
                            }
                            
                            if (!hasError) {
                                onAdd(question.trim(), answer.trim(), difficulty, tags.trim())
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Card")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val mindMapState by viewModel.mindMapState.collectAsState()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "AI Concept Mind Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val sub = when (val state = mindMapState) {
                                is MindMapState.Success -> state.graph.centralTheme ?: "Structural Hierarchy"
                                else -> "Gemini API Cognitive Graph"
                            }
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.clearMindMap()
                            onDismiss()
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (mindMapState is MindMapState.Success) {
                            IconButton(onClick = {
                                val state = mindMapState as MindMapState.Success
                                viewModel.generateMindMapForDeck(state.graph.centralTheme ?: "My Study Map", null)
                            }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (val state = mindMapState) {
                    is MindMapState.Idle, is MindMapState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Analyzing memory paths...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gemini is examining flashcard content to extract prerequisite relationships and generate a visual JSON cognitive graph.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }
                    }
                    is MindMapState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unable to generate Mind Map",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    viewModel.generateMindMapForDeck("My Study Map", null)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Retry Generation")
                            }
                        }
                    }
                    is MindMapState.Success -> {
                        MindMapVisualizer(graph = state.graph)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapVisualizer(graph: MindMapGraph) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(20f, 50f)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedNode by remember(graph) { mutableStateOf<MindMapNode?>(graph.nodes.firstOrNull { it.type == "root" }) }
    
    val positions = remember(graph) { computeNodePositions(graph) }
    val density = LocalDensity.current
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offset += pan
                }
            }
    ) {
        // 1. Interactive Panning & Zooming Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            // Draw connecting lines (Association paths)
            Canvas(modifier = Modifier.fillMaxSize()) {
                graph.edges.forEach { edge ->
                    val fromPos = positions[edge.from]
                    val toPos = positions[edge.to]
                    if (fromPos != null && toPos != null) {
                        val matchesSearch = searchQuery.isEmpty() ||
                            graph.nodes.find { it.id == edge.from }?.label?.contains(searchQuery, ignoreCase = true) == true ||
                            graph.nodes.find { it.id == edge.to }?.label?.contains(searchQuery, ignoreCase = true) == true
                        
                        val path = Path().apply {
                            moveTo(fromPos.x, fromPos.y)
                            cubicTo(
                                fromPos.x, (fromPos.y + toPos.y) / 2,
                                toPos.x, (fromPos.y + toPos.y) / 2,
                                toPos.x, toPos.y
                            )
                        }
                        
                        drawPath(
                            path = path,
                            color = if (matchesSearch) lineColor else lineColor.copy(alpha = 0.15f),
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        )
                    }
                }
            }
            
            // Render the Nodes as interactive card components
            graph.nodes.forEach { node ->
                val pos = positions[node.id] ?: Offset.Zero
                val isSelected = selectedNode?.id == node.id
                val matchesSearch = searchQuery.isEmpty() ||
                    node.label.contains(searchQuery, ignoreCase = true) ||
                    node.description.contains(searchQuery, ignoreCase = true)
                
                val nodeColor = when (node.type) {
                    "root" -> MaterialTheme.colorScheme.primaryContainer
                    "category" -> MaterialTheme.colorScheme.secondaryContainer
                    "concept" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                
                val onNodeColor = when (node.type) {
                    "root" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "category" -> MaterialTheme.colorScheme.onSecondaryContainer
                    "concept" -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
                
                val borderStroke = if (isSelected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                } else if (!matchesSearch) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
                
                val nodeWidth = 150.dp
                val nodeHeight = 65.dp
                
                val posX = with(density) { pos.x.toDp() } - (nodeWidth / 2)
                val posY = with(density) { pos.y.toDp() } - (nodeHeight / 2)
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = nodeColor.copy(alpha = if (matchesSearch) 1f else 0.2f)
                    ),
                    border = borderStroke,
                    modifier = Modifier
                        .offset(x = posX, y = posY)
                        .size(width = nodeWidth, height = nodeHeight)
                        .clickable { selectedNode = node }
                        .testTag("node_${node.id}")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = node.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = onNodeColor.copy(alpha = if (matchesSearch) 1f else 0.2f),
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = node.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = onNodeColor.copy(alpha = if (matchesSearch) 0.6f else 0.1f)
                            )
                        }
                    }
                }
            }
        }
        
        // 2. Search Field Overlay at the top
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search nodes & concept detail...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("mind_map_search"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        // 3. Zoom Controls Overlay on the right
        Card(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(3f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { 
                    scale = 1f
                    offset = Offset(20f, 50f)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset View", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        
        // 4. Selected Node Details Panel at the bottom
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            selectedNode?.let { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("node_details_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeColor = when (node.type) {
                                "root" -> MaterialTheme.colorScheme.primaryContainer
                                "category" -> MaterialTheme.colorScheme.secondaryContainer
                                "concept" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val badgeOnColor = when (node.type) {
                                "root" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "category" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "concept" -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = node.type.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeOnColor
                                )
                            }
                            
                            IconButton(
                                onClick = { selectedNode = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = node.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun computeNodePositions(graph: MindMapGraph): Map<String, Offset> {
    val positions = mutableMapOf<String, Offset>()
    val nodes = graph.nodes
    val edges = graph.edges
    
    val rootNode = nodes.firstOrNull { it.type == "root" } ?: nodes.firstOrNull() ?: return emptyMap()
    val categories = nodes.filter { it.type == "category" }
    val concepts = nodes.filter { it.type == "concept" }
    val details = nodes.filter { it.type == "card_detail" }
    
    val rootX = 500f
    val rootY = 100f
    positions[rootNode.id] = Offset(rootX, rootY)
    
    // Position Categories (Level 1)
    val catY = 320f
    val catSpacing = 320f
    val totalCatWidth = (categories.size - 1) * catSpacing
    val startCatX = rootX - (totalCatWidth / 2f)
    
    categories.forEachIndexed { index, cat ->
        val catX = startCatX + (index * catSpacing)
        positions[cat.id] = Offset(catX, catY)
        
        // Find concepts belonging to this category
        val catConcepts = concepts.filter { concept ->
            edges.any { it.from == cat.id && it.to == concept.id }
        }
        
        // Position concepts under this category
        val conceptY = 540f
        val conceptSpacing = 240f
        val totalConceptWidth = (catConcepts.size - 1) * conceptSpacing
        val startConceptX = catX - (totalConceptWidth / 2f)
        
        catConcepts.forEachIndexed { cIndex, concept ->
            val conceptX = startConceptX + (cIndex * conceptSpacing)
            positions[concept.id] = Offset(conceptX, conceptY)
            
            // Find details under this concept
            val conceptDetails = details.filter { detail ->
                edges.any { it.from == concept.id && it.to == detail.id }
            }
            
            // Position details under this concept
            val detailY = 760f
            val detailSpacing = 180f
            val totalDetailWidth = (conceptDetails.size - 1) * detailSpacing
            val startDetailX = conceptX - (totalDetailWidth / 2f)
            
            conceptDetails.forEachIndexed { dIndex, detail ->
                val detailX = startDetailX + (dIndex * detailSpacing)
                positions[detail.id] = Offset(detailX, detailY)
            }
        }
    }
    
    // For any unpositioned nodes, assign fallback positions so they aren't lost
    nodes.forEachIndexed { idx, node ->
        if (!positions.containsKey(node.id)) {
            positions[node.id] = Offset(200f + (idx * 180f) % 600f, 400f + (idx * 120f) % 300f)
        }
    }
    
    return positions
}

@Composable
fun ActiveRecallSummaryScreen(
    deckName: String,
    results: List<FlashcardRatingResult>,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()
    val activeRecallSummary by viewModel.activeRecallSummary.collectAsState()

    val isGeneratingDeckSummary by viewModel.isGeneratingDeckSummary.collectAsState()
    val deckContentSummary by viewModel.deckContentSummary.collectAsState()

    val decks by viewModel.allDecks.collectAsState()
    val deck = remember(decks) { decks.find { it.name == deckName } }
    val allCards by viewModel.allFlashcards.collectAsState()
    val deckCards = remember(deck, allCards) {
        if (deck != null) {
            allCards.filter { it.deckId == deck.id }
        } else {
            emptyList()
        }
    }

    // Trigger the generation of summary if it's empty and not currently generating
    LaunchedEffect(results) {
        if (activeRecallSummary == null && !isGeneratingSummary) {
            viewModel.generateActiveRecallSessionSummary(deckName, results)
        }
    }

    val easyCount = results.count { it.rating == 3 }
    val goodCount = results.count { it.rating == 2 }
    val hardCount = results.count { it.rating == 1 }
    val totalCount = results.size

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Active Recall Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Session Performance Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Deck: $deckName",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Easy Stats
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF43A047).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = Color(0xFF43A047))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$easyCount / $totalCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                                Text("Easy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            // Good Stats
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SentimentNeutral, contentDescription = null, tint = Color(0xFFFFB300))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$goodCount / $totalCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                                Text("Good", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            // Hard Stats
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SentimentVeryDissatisfied, contentDescription = null, tint = Color(0xFFE53935))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$hardCount / $totalCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                                Text("Hard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (isGeneratingSummary) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Analyzing with Gemini API...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Synthesizing your active recall pattern and mapping memory retention gaps...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else if (activeRecallSummary != null) {
                item {
                    // Gemini Response Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "AI Twin Cognitive Insights",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Style the markdown summary slightly
                            Text(
                                text = activeRecallSummary ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Deck Content Synthesis section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deck_summary_container_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Digital Twin Deck Overview",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (deckContentSummary == null) {
                            Text(
                                text = "Generate a comprehensive, high-yield textual summary of the active concepts in this flashcard deck.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isGeneratingDeckSummary) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Synthesizing deck concepts...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.generateDeckContentSummary(deckName, deckCards) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("generate_deck_summary_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Summarize Entire Deck")
                                }
                            }
                        } else {
                            if (isGeneratingDeckSummary) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Regenerating deck summary...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            Text(
                                text = deckContentSummary ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (!isGeneratingDeckSummary) {
                                OutlinedButton(
                                    onClick = { viewModel.generateDeckContentSummary(deckName, deckCards) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("regenerate_deck_summary_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Regenerate Summary")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick Action: Socratic chat to close gaps
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onClose()
                                // Find deck or start Socratic chat
                                val decks = viewModel.allDecks.value
                                val deck = decks.find { it.name == deckName }
                                if (deck != null) {
                                    viewModel.startDeckTutorSession(deck.id)
                                }
                            }
                            .testTag("summary_discuss_gaps_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Discuss Gaps with AI Tutor",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Start a chat specifically focused on explanations and analogies for the concepts you struggled with.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Start Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("summary_done_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done & Return", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

data class VerbalEvaluation(
    val score: Int,
    val ratingRecommend: String,
    val feedback: String,
    val comparison: String
)

fun localEvaluateVerbalAnswer(spoken: String, expected: String): VerbalEvaluation {
    val spokenClean = spoken.lowercase(Locale.getDefault()).replace(Regex("[^a-zA-Z0-9 ]"), "")
    val expectedClean = expected.lowercase(Locale.getDefault()).replace(Regex("[^a-zA-Z0-9 ]"), "")
    
    val spokenWords = spokenClean.split(" ").filter { it.isNotBlank() }.toSet()
    val expectedWords = expectedClean.split(" ").filter { it.isNotBlank() }.toSet()
    
    if (expectedWords.isEmpty()) {
        return VerbalEvaluation(
            score = 100,
            ratingRecommend = "Easy",
            feedback = "Wonderful! Your spoken answer was registered, and since there is no reference answer required, you get a perfect score! Let's continue.",
            comparison = "Spoken: '$spoken' vs. Expected: '$expected'"
        )
    }
    
    val matchingWords = spokenWords.intersect(expectedWords)
    val score = if (expectedWords.isEmpty()) 100 else ((matchingWords.size.toFloat() / expectedWords.size.toFloat()) * 100).toInt()
    
    val (recommend, textFeedback) = when {
        score >= 70 -> {
            "Easy" to "Superb verbal recall! Your response had a strong conceptual overlap (${score}%) with the expected definition. Excellent job articulating this concept!"
        }
        score >= 35 -> {
            "Good" to "Good attempt! You recalled several key terms correctly (${score}% overlap), but some fine-grained details of the definition were missed. Review the card's answer to fill the gap!"
        }
        else -> {
            "Hard" to "A brave effort! However, your verbal answer had low keyword overlap (${score}%) with the card's answer. Take a moment to read the full explanation, then try recalling it again."
        }
    }
    
    return VerbalEvaluation(
        score = score.coerceIn(5, 100),
        ratingRecommend = recommend,
        feedback = textFeedback,
        comparison = "Spoken: '$spoken' vs. Expected: '$expected'"
    )
}

suspend fun evaluateVerbalAnswerWithGemini(spoken: String, expected: String, question: String): VerbalEvaluation {
    if (!GeminiClient.isApiKeyAvailable()) {
        return localEvaluateVerbalAnswer(spoken, expected)
    }
    
    val prompt = """
        You are the user's Socratic Digital Twin. Your task is to evaluate the user's verbal response to a flashcard.
        
        Flashcard Question:
        "$question"
        
        Expected Answer:
        "$expected"
        
        User's Spoken Answer:
        "$spoken"
        
        Compare the user's spoken response against the expected answer. Be very intelligent:
        1. Ignore speech-to-text spelling/grammar artifacts or typos.
        2. Evaluate the conceptual accuracy (0 to 100).
        3. Recommend a rating: "Easy" (mastered, correct), "Good" (partially correct, missed some minor details), or "Hard" (wrong or completely off).
        4. Write a 2-3 sentence personalized, encouraging Socratic feedback message as their Twin. Be warm and supportive.
        
        Output strictly raw JSON format (no backticks, no markdown prefix):
        {
          "score": 85,
          "ratingRecommend": "Good",
          "feedback": "Your verbal answer captures the core concept wonderfully! You correctly noted that..., but make sure to also mention that..."
        }
    """.trimIndent()
    
    return try {
        val resultJson = GeminiClient.generate(prompt, "You are a helpful Socratic Digital Twin who evaluates verbal flashcard answers.")
        val clean = resultJson.trim()
            .removePrefix("```json")
            .removeSuffix("```")
            .trim()
            
        val json = JSONObject(clean)
        VerbalEvaluation(
            score = json.optInt("score", 70),
            ratingRecommend = json.optString("ratingRecommend", "Good"),
            feedback = json.optString("feedback", "Excellent work reviewing this card! Keep practicing."),
            comparison = "Spoken: '$spoken' vs. Expected: '$expected'"
        )
    } catch (e: Exception) {
        Log.e("neurolearn", "Gemini verbal eval failed, falling back to local word overlap", e)
        localEvaluateVerbalAnswer(spoken, expected)
    }
}

suspend fun generateTwinQuiz(
    deckName: String,
    cards: List<com.example.data.Flashcard>,
    twinName: String
): List<com.example.ui.QuizQuestion> {
    if (!com.example.api.GeminiClient.isApiKeyAvailable()) {
        // Fallback: generate mock questions based on the actual cards
        return cards.take(5).mapIndexed { index, card ->
            val options = mutableListOf(card.answer)
            val distractors = cards.filter { it.answer != card.answer }.map { it.answer }.shuffled().take(3)
            options.addAll(distractors)
            while (options.size < 4) {
                options.add("Distractor ${options.size + 1} for concept check")
            }
            com.example.ui.QuizQuestion(
                question = "Conceptual check for: ${card.question}",
                options = options.shuffled(),
                correctAnswer = card.answer,
                explanation = "Your twin recommends reviewing this concept: ${card.answer}"
            )
        }
    }

    val cardsPromptText = cards.take(15).joinToString("\n") { "Q: ${it.question} | A: ${it.answer}" }
    val prompt = """
        You are the user's Socratic Digital Twin ($twinName). Based on the following flashcards from the deck "$deckName", generate a high-quality 5-question multiple-choice quiz to test concept retention.
        
        Flashcards:
        $cardsPromptText
        
        Design 5 conceptually challenging multiple-choice questions (MCQs) that target the core definitions, processes, and concepts in these flashcards. Ensure your questions test active comprehension, not just surface-level word matching.
        
        The output format must be a raw JSON array of exactly 5 objects. Each object in the array represents a question with:
        - "question": string
        - "options": list of exactly 4 distinct options
        - "correctAnswer": string (matching exactly one of the options)
        - "explanation": string (explaining the correct answer conceptually as their Socratic Twin, referencing the concept)
        
        Output strictly raw JSON format. Do not wrap in markdown or backticks. Start with [ and end with ]:
    """.trimIndent()

    val response = com.example.api.GeminiClient.generate(prompt, "You are a warm and helpful Socratic Digital Twin who creates conceptual quizzes.")
    
    val list = mutableListOf<com.example.ui.QuizQuestion>()
    try {
        var clean = response.trim()
        if (clean.contains("JSON_START") && clean.contains("JSON_END")) {
            val start = clean.indexOf("JSON_START") + "JSON_START".length
            val end = clean.indexOf("JSON_END")
            clean = clean.substring(start, end).trim()
        } else {
            val firstArray = clean.indexOf('[')
            if (firstArray != -1) {
                val lastArray = clean.lastIndexOf(']')
                if (lastArray != -1) {
                    clean = clean.substring(firstArray, lastArray + 1)
                }
            }
        }
        
        val jsonArray = org.json.JSONArray(clean)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val q = obj.getString("question")
            val optsArray = obj.getJSONArray("options")
            val opts = mutableListOf<String>()
            for (j in 0 until optsArray.length()) {
                opts.add(optsArray.getString(j))
            }
            val ans = obj.getString("correctAnswer")
            val exp = obj.optString("explanation", "")
            list.add(com.example.ui.QuizQuestion(q, opts, ans, exp))
        }
    } catch (e: Exception) {
        Log.e("neurolearn", "Error generating/parsing twin quiz", e)
        return cards.take(5).mapIndexed { index, card ->
            val options = mutableListOf(card.answer)
            val distractors = cards.filter { it.answer != card.answer }.map { it.answer }.shuffled().take(3)
            options.addAll(distractors)
            while (options.size < 4) {
                options.add("Distractor ${options.size + 1} for concept check")
            }
            com.example.ui.QuizQuestion(
                question = "Conceptual check for: ${card.question}",
                options = options.shuffled(),
                correctAnswer = card.answer,
                explanation = "Your twin recommends reviewing this concept: ${card.answer}"
            )
        }
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwinStudyChatDrawer(
    deckId: String,
    currentCard: Flashcard?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    twinName: String,
    twinAvatar: String,
    avatarRes: Int
) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val isLoading by viewModel.isAILoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll chat to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .testTag("twin_study_chat_drawer"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Drag handle and Title Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = avatarRes),
                        contentDescription = "AI Twin Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = twinName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Deck & Flashcard Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Chat")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            // Scrollable Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                items(messages) { msg ->
                    InlineChatBubble(message = msg, twinAvatar = twinAvatar)
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "AI Twin Avatar",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$twinName is analyzing...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // Quick contextual chips about the current card
            if (currentCard != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val promptChips = listOf(
                        "Explain this card 💡",
                        "Give me an analogy 🧩",
                        "Real-world example 🌍",
                        "What is a key concept? 🔑"
                    )
                    promptChips.forEach { promptText ->
                        SuggestionChip(
                            onClick = {
                                viewModel.sendMessageToTutor(
                                    conceptId = null,
                                    deckId = deckId,
                                    userText = promptText,
                                    currentCard = currentCard
                                )
                            },
                            label = { Text(promptText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        )
                    }
                }
            }

            // Input bar with text field and Send button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { 
                        Text(
                            text = "Ask twin about concepts...",
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("twin_study_chat_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val textToSend = inputText
                                    inputText = ""
                                    viewModel.sendMessageToTutor(
                                        conceptId = null,
                                        deckId = deckId,
                                        userText = textToSend,
                                        currentCard = currentCard
                                    )
                                },
                                modifier = Modifier.testTag("twin_study_chat_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Message",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InlineChatBubble(message: ChatMessage, twinAvatar: String) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            val avatarRes = when (twinAvatar) {
                "tech" -> R.drawable.img_twin_tech
                "scholar" -> R.drawable.img_twin_scholar
                "creative" -> R.drawable.img_twin_creative
                else -> R.drawable.img_twin_socratic
            }
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "AI Twin Avatar",
                modifier = Modifier
                    .padding(end = 8.dp, top = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 12.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 240.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FirestoreLiveHub(viewModel: MainViewModel) {
    val firestoreDecks by viewModel.firestoreManager.decks.collectAsState()
    val firestoreCardsMap by viewModel.firestoreManager.cardsMap.collectAsState()
    val isFirestoreActive by viewModel.firestoreManager.isFirestoreActive.collectAsState()
    val userProfile by viewModel.profile.collectAsState()
    val userId = userProfile?.id ?: "user_default"

    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var isStudying by remember { mutableStateOf(false) }
    var studyAllMode by remember { mutableStateOf(false) }

    val currentDeck = firestoreDecks.find { it.id == selectedDeckId }

    if (isStudying && selectedDeckId != null && currentDeck != null) {
        val allDeckCards = firestoreCardsMap[selectedDeckId] ?: emptyList()
        val cardsToStudy = if (studyAllMode) allDeckCards else allDeckCards.filter { it.nextReviewDate <= System.currentTimeMillis() }

        FirestoreStudySession(
            deckId = selectedDeckId!!,
            deckName = currentDeck.name,
            cards = cardsToStudy,
            viewModel = viewModel,
            onQuit = { isStudying = false }
        )
    } else if (selectedDeckId != null && currentDeck != null) {
        val deckCards = firestoreCardsMap[selectedDeckId] ?: emptyList()
        FirestoreDeckDetailView(
            deck = currentDeck,
            cards = deckCards,
            viewModel = viewModel,
            onBack = { selectedDeckId = null },
            onAddCardClick = { showAddCardDialog = true },
            onStartStudy = { studyAll ->
                studyAllMode = studyAll
                isStudying = true
            },
            onDeleteDeck = {
                viewModel.firestoreManager.deleteDeck(selectedDeckId!!, {
                    selectedDeckId = null
                }, {
                    // handle error
                })
            }
        )

        if (showAddCardDialog) {
            AddFirestoreCardDialog(
                onDismiss = { showAddCardDialog = false },
                onAdd = { q, a ->
                    viewModel.firestoreManager.addCardToDeck(selectedDeckId!!, q, a, {
                        showAddCardDialog = false
                    }, {
                        // handle error
                    })
                }
            )
        }
    } else {
        // Main live hub lists
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFirestoreActive) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp, 
                        if (isFirestoreActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) 
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isFirestoreActive) Icons.Default.CloudSync else Icons.Default.CloudOff,
                            contentDescription = "Firestore Status",
                            tint = if (isFirestoreActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (isFirestoreActive) "Direct Firestore Sync Active" else "Firestore Offline Fallback",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isFirestoreActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (isFirestoreActive) 
                                    "Your terms are updated instantly in Google Cloud Firestore with real-time replication." 
                                else 
                                    "Running locally in sandbox mode. Progress will sync once cloud connection is verified.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFirestoreActive) 
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cloud Collections",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Collection", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (firestoreDecks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Cloud Collections Yet",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Create a custom subject-specific term deck to start training with SM-2 Spaced Repetition.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(firestoreDecks) { deck ->
                    val deckCards = firestoreCardsMap[deck.id] ?: emptyList()
                    val dueCount = deckCards.count { it.nextReviewDate <= System.currentTimeMillis() }

                    FirestoreDeckItem(
                        deck = deck,
                        totalCards = deckCards.size,
                        dueCards = dueCount,
                        onClick = { selectedDeckId = deck.id }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateFirestoreDeckDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, desc ->
                    viewModel.firestoreManager.createDeck(userId, name, desc, {
                        showCreateDialog = false
                    }, {
                        // handle error
                    })
                }
            )
        }
    }
}

@Composable
fun FirestoreDeckItem(
    deck: FirestoreDeck,
    totalCards: Int,
    dueCards: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("firestore_deck_item_${deck.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (deck.description.isNotBlank()) {
                    Text(
                        text = deck.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("$totalCards terms", style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(0.dp, Color.Transparent)
                    )

                    if (dueCards > 0) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$dueCards due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(0.dp, Color.Transparent)
                        )
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("All reviewed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(0.dp, Color.Transparent)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Deck",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun FirestoreDeckDetailView(
    deck: FirestoreDeck,
    cards: List<FirestoreFlashcard>,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAddCardClick: () -> Unit,
    onStartStudy: (Boolean) -> Unit,
    onDeleteDeck: () -> Unit
) {
    val dueCards = cards.filter { it.nextReviewDate <= System.currentTimeMillis() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            IconButton(
                onClick = onDeleteDeck,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Collection")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title and Description
        Text(
            text = deck.name,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (deck.description.isNotBlank()) {
            Text(
                text = deck.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Study action triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onStartStudy(false) },
                enabled = dueCards.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Study Due (${dueCards.size})")
            }

            OutlinedButton(
                onClick = { onStartStudy(true) },
                enabled = cards.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Study All (${cards.size})")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Terms header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subject Terms (${cards.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            TextButton(onClick = onAddCardClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Term")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No subject terms defined.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards) { card ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = card.question,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (isExpanded) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = card.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Repetitions: ${card.repetitions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Interval: ${card.intervalDays} days",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    val dueText = if (card.nextReviewDate <= System.currentTimeMillis()) "Due Now" else "Next review in ${((card.nextReviewDate - System.currentTimeMillis()) / (24 * 3600 * 1000L)).coerceAtLeast(1)} days"
                                    Text(
                                        text = dueText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (card.nextReviewDate <= System.currentTimeMillis()) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
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
fun FirestoreStudySession(
    deckId: String,
    deckName: String,
    cards: List<FirestoreFlashcard>,
    viewModel: MainViewModel,
    onQuit: () -> Unit
) {
    var currentCardIndex by remember { mutableStateOf(0) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var showResultsSummary by remember { mutableStateOf(false) }
    
    val ratedCounts = remember { mutableMapOf<Int, Int>(1 to 0, 2 to 0, 3 to 0) } // rating to count

    if (cards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All caught up!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "There are no due cards in this collection. Select 'Study All' to study terms anyway.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onQuit, shape = RoundedCornerShape(12.dp)) {
                    Text("Return to Deck")
                }
            }
        }
        return
    }

    if (showResultsSummary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Success",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Text(
                        text = "Session Complete!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "You've successfully completed active recall training for $deckName.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hard", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE53935))
                            Text("${ratedCounts[1] ?: 0}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Good", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB300))
                            Text("${ratedCounts[2] ?: 0}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Easy", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                            Text("${ratedCounts[3] ?: 0}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onQuit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Finish Study")
                    }
                }
            }
        }
        return
    }

    val currentCard = cards[currentCardIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onQuit) {
                Icon(Icons.Default.Close, contentDescription = "Close Study Session")
            }
            Text(
                text = "${currentCardIndex + 1} / ${cards.size}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Empty space to balance close button
            Spacer(modifier = Modifier.width(48.dp))
        }

        LinearProgressIndicator(
            progress = { (currentCardIndex + 1).toFloat() / cards.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Flashcard container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable { isAnswerRevealed = !isAnswerRevealed },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAnswerRevealed) 
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isAnswerRevealed) "REVEALED ANSWER" else "QUESTION/CONCEPT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isAnswerRevealed) currentCard.answer else currentCard.question,
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isAnswerRevealed) {
                        Text(
                            text = "Tap Card to Reveal Answer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Active Recall Grading Panel
        Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            if (isAnswerRevealed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hard Button
                    Button(
                        onClick = {
                            ratedCounts[1] = (ratedCounts[1] ?: 0) + 1
                            viewModel.firestoreManager.updateCardReview(deckId, currentCard, 1, {}, {})
                            if (currentCardIndex + 1 < cards.size) {
                                currentCardIndex++
                                isAnswerRevealed = false
                            } else {
                                showResultsSummary = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SentimentVeryDissatisfied, contentDescription = "Hard")
                            Text("Again (Hard)", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Good Button
                    Button(
                        onClick = {
                            ratedCounts[2] = (ratedCounts[2] ?: 0) + 1
                            viewModel.firestoreManager.updateCardReview(deckId, currentCard, 2, {}, {})
                            if (currentCardIndex + 1 < cards.size) {
                                currentCardIndex++
                                isAnswerRevealed = false
                            } else {
                                showResultsSummary = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SentimentNeutral, contentDescription = "Good")
                            Text("Good", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Easy Button
                    Button(
                        onClick = {
                            ratedCounts[3] = (ratedCounts[3] ?: 0) + 1
                            viewModel.firestoreManager.updateCardReview(deckId, currentCard, 3, {}, {})
                            if (currentCardIndex + 1 < cards.size) {
                                currentCardIndex++
                                isAnswerRevealed = false
                            } else {
                                showResultsSummary = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Easy")
                            Text("Easy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { isAnswerRevealed = true },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reveal Answer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun CreateFirestoreDeckDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Firestore Collection",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Collection Name (e.g. Physiology)") },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                onCreate(name, description)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun AddFirestoreCardDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var qError by remember { mutableStateOf(false) }
    var aError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Subject Term",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        question = it
                        if (it.isNotBlank()) qError = false
                    },
                    label = { Text("Concept / Question") },
                    isError = qError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        if (it.isNotBlank()) aError = false
                    },
                    label = { Text("Definition / Answer") },
                    isError = aError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (question.isBlank()) qError = true
                            if (answer.isBlank()) aError = true
                            if (question.isNotBlank() && answer.isNotBlank()) {
                                onAdd(question, answer)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}



