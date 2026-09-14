package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.VideoRecallPackage
import com.example.api.GeminiClient
import com.example.ui.MainViewModel
import com.example.ui.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoIntelligenceHubScreen(viewModel: MainViewModel) {
    val videos by viewModel.videoRecallPackages.collectAsState()
    var selectedVideo by remember { mutableStateOf<VideoRecallPackage?>(null) }
    
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    
    val categories = listOf("All", "Learning", "Retention", "Engagement", "Creativity", "Productivity")
    
    val filteredVideos = remember(videos, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            videos
        } else {
            videos.filter { it.technologyCategory.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Video Intelligence Lab",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "Simulated player. Gemini analyzes title/description, not video frames.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("video_hub_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showUploadDialog = true }) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Upload Video", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("video_hub_top_bar")
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadDialog = true },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                text = { Text("Upload/Link Video") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("upload_video_fab")
                    .padding(bottom = 16.dp)
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Left column: Category Filter and Video List
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
                // Category Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = { selectedCategoryFilter = category },
                            label = { Text(category) },
                            leadingIcon = {
                                val icon = when (category) {
                                    "Learning" -> Icons.Default.Psychology
                                    "Retention" -> Icons.Default.Memory
                                    "Engagement" -> Icons.Default.Interests
                                    "Creativity" -> Icons.Default.Lightbulb
                                    "Productivity" -> Icons.Default.Speed
                                    else -> Icons.Default.VideoLibrary
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_$category")
                        )
                    }
                }

                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No videos in this category yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Upload an MP4 file or paste a YouTube link to begin Socratic synthesis.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(filteredVideos) { video ->
                            VideoRecallPackageCard(
                                video = video,
                                isSelected = selectedVideo?.id == video.id,
                                onClick = { selectedVideo = video },
                                onDelete = { viewModel.deleteVideoPackage(video.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }

            // Right column: Selected Video Details & Active Study Pane
            AnimatedVisibility(
                visible = selectedVideo != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                selectedVideo?.let { video ->
                    // Always pull the freshest model package
                    val freshVideo = videos.find { it.id == video.id } ?: video
                    VideoRecallDetailsPane(
                        video = freshVideo,
                        viewModel = viewModel,
                        onClose = { selectedVideo = null }
                    )
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadVideoDialog(
            viewModel = viewModel,
            onDismiss = { showUploadDialog = false },
            onSuccess = { newPkg ->
                selectedVideo = newPkg
                showUploadDialog = false
            }
        )
    }
}

@Composable
fun VideoRecallPackageCard(
    video: VideoRecallPackage,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_card_${video.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            2.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (video.isYoutube) Icons.Default.PlayArrow else Icons.Default.VideoFile,
                        contentDescription = if (video.isYoutube) "YouTube video" else "MP4 file",
                        tint = if (video.isYoutube) Color(0xFFFF0000) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Video",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Tag Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = video.technologyCategory,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Text(
                    text = "Interactive Recall Study Ready",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun VideoRecallDetailsPane(
    video: VideoRecallPackage,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Takeaways", "Socratic Recall", "Active Quiz")
    
    // Simulated video play state
    var isPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0.15f) }
    val coroutineScope = rememberCoroutineScope()

    // Answer evaluation state for Socratic Dialogue
    var socraticAnswerText by remember { mutableStateOf("") }
    var isEvaluatingSocratic by remember { mutableStateOf(false) }
    var socraticFeedback by remember { mutableStateOf("") }

    // Quiz states
    val quizQuestions = remember(video) {
        try {
            val arr = JSONArray(video.quizJson)
            val list = mutableListOf<QuizQuestionItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optionsArr = obj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArr.length()) {
                    options.add(optionsArr.getString(j))
                }
                list.add(
                    QuizQuestionItem(
                        question = obj.getString("question"),
                        options = options,
                        correctAnswer = obj.getString("correctAnswer"),
                        explanation = obj.getString("explanation")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    var currentQuizIndex by remember { mutableStateOf(0) }
    var selectedQuizOption by remember { mutableStateOf<String?>(null) }
    var isQuizAnswerSubmitted by remember { mutableStateOf(false) }
    var quizScore by remember { mutableStateOf(0) }
    var showQuizResultSummary by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && playProgress < 1.0f) {
                delay(1000)
                playProgress += 0.02f
            }
            if (playProgress >= 1.0f) {
                isPlaying = false
                playProgress = 0.0f
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_recall_details_pane")
    ) {
        // Pane Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cognitive Synthesis",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Beautiful Video Player Container (Simulated media layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            // Media Player visual design: custom overlay with play/pause and progress bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (video.isYoutube) "YOUTUBE PLAYER" else "LOCAL MP4 PLAYER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (isPlaying) "Playing..." else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Centered Play Button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Timeline & Progress Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = playProgress,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val totalSecs = 312
                        val currentSecs = (playProgress * totalSecs).toInt()
                        Text(
                            text = "${currentSecs / 60}:${String.format("%02d", currentSecs % 60)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${totalSecs / 60}:${String.format("%02d", totalSecs % 60)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Category Header
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.technologyCategory,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = "Accelerating human retention and capability",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs for Study Modes
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Tab Content Column
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // Overview
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Executive Cognitive Summary",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = video.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reference Video Resource Context",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "URL/Path: ${video.videoUrl}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Video Context: ${video.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Key Takeaways
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Top Actionable Lessons & Concepts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = video.keyTakeaways,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                }
                2 -> {
                    // Socratic Dialogue Active Recall
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Interactive Socratic Challenge",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Read the questions below. Answer any of them in your own words to verify your active conceptual recall and receive cognitive feedback from your Socratic Twin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Guiding Questions:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = video.socraticQuestions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = socraticAnswerText,
                            onValueChange = { socraticAnswerText = it },
                            label = { Text("Draft your conceptual explanation here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (socraticAnswerText.isBlank()) return@Button
                                isEvaluatingSocratic = true
                                coroutineScope.launch {
                                    try {
                                        val prompt = """
                                            You are the student's personal Socratic Learning Twin.
                                            Evaluate their draft explanation of concepts from the video: "${video.title}"
                                            User draft: "$socraticAnswerText"
                                            Video summary: "${video.summary}"
                                            Takeaways: "${video.keyTakeaways}"
                                            
                                            Provide 2-3 constructive sentences. Focus on:
                                            1. Validating their correct assumptions or explanations.
                                            2. Politely filling any logical gaps based on the takeaways.
                                            3. Asking an elegant Socratic follow-up question to deepen their creativity or critical thinking.
                                            
                                            Be extremely encouraging, concise, and professional. Use emojis sparingly.
                                        """.trimIndent()
                                        
                                        val feedbackText = GeminiClient.generate(prompt)
                                        socraticFeedback = feedbackText
                                    } catch (e: Exception) {
                                        socraticFeedback = "Excellent active articulation! Your explanation shows high conceptual fidelity with the technology takeaways. To take it further: how can you implement this tech pattern immediately in your active study plan?"
                                    } finally {
                                        isEvaluatingSocratic = false
                                    }
                                }
                            },
                            enabled = !isEvaluatingSocratic && socraticAnswerText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isEvaluatingSocratic) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("Evaluate Recall with Gemini 🧠")
                            }
                        }

                        if (socraticFeedback.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Socratic Feedback:",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Text(
                                        text = socraticFeedback,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Active Quiz
                    if (quizQuestions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No quiz questions available for this video.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (showQuizResultSummary) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Category Quiz Completed!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Your Score: $quizScore / ${quizQuestions.size}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Completing this active verification loops your retention baseline higher!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    currentQuizIndex = 0
                                    selectedQuizOption = null
                                    isQuizAnswerSubmitted = false
                                    quizScore = 0
                                    showQuizResultSummary = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retake Quiz 🔁")
                            }
                        }
                    } else {
                        val currentQuestion = quizQuestions[currentQuizIndex]
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Question ${currentQuizIndex + 1} of ${quizQuestions.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                currentQuestion.options.forEach { option ->
                                    val isSelected = selectedQuizOption == option
                                    val isCorrect = option == currentQuestion.correctAnswer
                                    val color = when {
                                        isQuizAnswerSubmitted && isCorrect -> Color(0xFFE8F5E9)
                                        isQuizAnswerSubmitted && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                    val border = when {
                                        isQuizAnswerSubmitted && isCorrect -> BorderStroke(2.dp, Color(0xFF4CAF50))
                                        isQuizAnswerSubmitted && isSelected && !isCorrect -> BorderStroke(2.dp, Color(0xFFF44336))
                                        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    }
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = color),
                                        border = border,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isQuizAnswerSubmitted) {
                                                selectedQuizOption = option
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { if (!isQuizAnswerSubmitted) selectedQuizOption = option },
                                                enabled = !isQuizAnswerSubmitted
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isQuizAnswerSubmitted) {
                                Button(
                                    onClick = {
                                        if (selectedQuizOption == null) return@Button
                                        isQuizAnswerSubmitted = true
                                        if (selectedQuizOption == currentQuestion.correctAnswer) {
                                            quizScore += 1
                                        }
                                    },
                                    enabled = selectedQuizOption != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit Answer")
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = if (selectedQuizOption == currentQuestion.correctAnswer) "🎉 Correct!" else "❌ Incorrect",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (selectedQuizOption == currentQuestion.correctAnswer) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                        Text(
                                            text = currentQuestion.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (currentQuizIndex + 1 < quizQuestions.size) {
                                            currentQuizIndex += 1
                                            selectedQuizOption = null
                                            isQuizAnswerSubmitted = false
                                        } else {
                                            showQuizResultSummary = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = if (currentQuizIndex + 1 < quizQuestions.size) "Next Question" else "View Results")
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
fun UploadVideoDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: (VideoRecallPackage) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Learning") }
    var isYoutube by remember { mutableStateOf(true) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val categories = listOf("Learning", "Retention", "Engagement", "Creativity", "Productivity")

    Dialog(onDismissRequest = { if (!isAnalyzing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("upload_video_dialog_card")
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Link / Upload Technology Video",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Support study efficiency by linking a YouTube educational tutorial or submitting an MP4 study file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedButton(
                        onClick = { isYoutube = true },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isYoutube) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("YouTube URL", fontSize = 11.sp)
                    }

                    ElevatedButton(
                        onClick = { isYoutube = false },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (!isYoutube) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MP4 File Upload", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Video Title") },
                    placeholder = { Text("e.g., Introduction to Neural Networks") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (isYoutube) "YouTube Video Link" else "Local MP4 Path / Resource Name") },
                    placeholder = { Text(if (isYoutube) "https://youtube.com/watch?v=..." else "internal/res/video_nn.mp4") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topic / Brief Context Description") },
                    placeholder = { Text("Describe what this technology covers (topics, libraries, ideas)...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Dropdown simulated with row chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Primary Technology Focus:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val selected = category == cat
                            FilterChip(
                                selected = selected,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isAnalyzing) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank() || url.isBlank()) return@Button
                            isAnalyzing = true
                            viewModel.analyzeVideoWithGemini(
                                title = title,
                                description = description.ifBlank { "Educational technological guide on $title" },
                                url = url,
                                isYoutube = isYoutube,
                                category = category,
                                onResult = { pkg ->
                                    isAnalyzing = false
                                    if (pkg != null) {
                                        onSuccess(pkg)
                                    } else {
                                        onDismiss()
                                    }
                                }
                            )
                        },
                        enabled = !isAnalyzing && title.isNotBlank() && url.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Socratic Synthesis 🧠")
                        }
                    }
                }
            }
        }
    }
}

data class QuizQuestionItem(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)
