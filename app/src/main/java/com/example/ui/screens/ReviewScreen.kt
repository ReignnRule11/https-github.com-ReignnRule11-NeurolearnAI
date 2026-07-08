package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.MainViewModel

@Composable
fun ReviewScreen(viewModel: MainViewModel) {
    val allDecks by viewModel.allDecks.collectAsState()
    val allCards by viewModel.allFlashcards.collectAsState()
    
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var showCreateDeckDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showMindMapDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
        
        // Mind Map Dialog Overlay
        if (showMindMapDialog) {
            MindMapDialog(
                viewModel = viewModel,
                onDismiss = { showMindMapDialog = false }
            )
        }
        
        // Create Deck Dialog
        if (showCreateDeckDialog) {
            CreateDeckDialog(
                onDismiss = { showCreateDeckDialog = false },
                onCreate = { name, desc, subject ->
                    viewModel.createDeck(name, desc, subject)
                    showCreateDeckDialog = false
                }
            )
        }
        
        // Add Flashcard Dialog
        if (showAddCardDialog && selectedDeckId != null) {
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
    REVIEW
}

@Composable
fun ActiveStudySession(
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
                onQuit()
            }
        )
        return
    }

    var currentCardIndex by remember { mutableStateOf(0) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var studyMode by remember { mutableStateOf(StudyMode.ACTIVE_RECALL) }
    
    val currentCard = cards.getOrNull(currentCardIndex)
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
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
            IconButton(onClick = {}, enabled = false) {
                // Spacer item to center
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = Color.Transparent)
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Recall",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (studyMode == StudyMode.ACTIVE_RECALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (studyMode == StudyMode.REVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
                            AnimatedVisibility(
                                visible = isAnswerRevealed,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                                modifier = Modifier.weight(1f)
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
                            }
                            
                            if (!isAnswerRevealed) {
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Calculus", "Computer Science", "Chemistry", "Biology", "General").forEach { sub ->
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


