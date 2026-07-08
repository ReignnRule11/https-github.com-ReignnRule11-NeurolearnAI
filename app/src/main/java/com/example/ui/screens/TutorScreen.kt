package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.Context
import android.os.Bundle
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.data.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(viewModel: MainViewModel, conceptId: String? = null, deckId: String? = null) {
    val profile by viewModel.profile.collectAsState()
    val messages by viewModel.activeChatMessages.collectAsState()
    val isLoading by viewModel.isAILoading.collectAsState()
    val concepts by viewModel.allConcepts.collectAsState()
    val decks by viewModel.allDecks.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showAvatarCustomizer by remember { mutableStateOf(false) }
    var showVoiceSession by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val inlineSpeechState = remember(context) {
        SpeechRecognizerState(
            context = context,
            onTranscriptionUpdated = { text ->
                if (text.isNotBlank()) {
                    inputText = text
                }
            },
            onErrorOccurred = { err ->
                viewModel.showToast(err)
            }
        )
    }

    DisposableEffect(inlineSpeechState) {
        onDispose {
            inlineSpeechState.destroy()
        }
    }

    // Dynamic Live Transcription binding
    LaunchedEffect(inlineSpeechState.transcription) {
        if (inlineSpeechState.isListening && inlineSpeechState.transcription.isNotBlank()) {
            inputText = inlineSpeechState.transcription
        }
    }

    // Inline voice recording permission launcher
    val inlinePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                inlineSpeechState.transcription = ""
                inlineSpeechState.startListening()
            } else {
                viewModel.showToast("Microphone permission is required for voice dictation.")
            }
        }
    )

    val concept = concepts.find { it.id == conceptId }
    val deck = decks.find { it.id == deckId }
    val entityName = deck?.name ?: concept?.name ?: "Topic"
    val tutorSubtitle = if (deckId != null) "Digital Twin Deck Tutor" else "Socratic AI Tutor"

    // Auto-scroll chat to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = entityName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = tutorSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            if (deckId != null) {
                                viewModel.navigateTo(Screen.Review)
                            } else {
                                viewModel.navigateTo(Screen.Learn)
                            }
                        },
                        modifier = Modifier.testTag("tutor_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAvatarCustomizer = true },
                        modifier = Modifier.testTag("tutor_customize_twin_button")
                    ) {
                        Icon(imageVector = Icons.Default.Face, contentDescription = "Customize AI Twin", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { viewModel.clearTutorChat(conceptId, deckId) },
                        modifier = Modifier.testTag("tutor_clear_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Chat History", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("tutor_screen_container")
        ) {
            // Chat Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }

                items(messages) { message ->
                    ChatBubble(message = message, twinAvatar = profile?.selectedTwinAvatar ?: "socratic")
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            val activeTwin = profile?.selectedTwinAvatar ?: "socratic"
                            val avatarRes = when (activeTwin) {
                                "tech" -> R.drawable.img_twin_tech
                                "scholar" -> R.drawable.img_twin_scholar
                                "creative" -> R.drawable.img_twin_creative
                                else -> R.drawable.img_twin_socratic
                            }
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = "Active Twin Avatar",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tutor is thinking...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Socratic Quick Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = if (deckId != null) {
                    listOf(
                        "Quiz me on this deck",
                        "Explain a card with an analogy",
                        "Give me a random question"
                    )
                } else {
                    listOf(
                        "Explain this with an analogy",
                        "Solve a step-by-step problem",
                        "Quiz me on this concept"
                    )
                }
                suggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .clickable {
                                inputText = suggestion
                                viewModel.sendMessageToTutor(conceptId, deckId, suggestion)
                                inputText = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Chat Input Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val inlineMicColor = if (inlineSpeechState.isListening) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }

                val infiniteTransition = rememberInfiniteTransition(label = "inline_mic_pulse")
                val micScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (inlineSpeechState.isListening) 1.25f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(750, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "micScale"
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { 
                        Text(
                            text = if (inlineSpeechState.isListening) "Listening... Speak now!" else "Ask your Socratic Tutor anything..."
                        ) 
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            // Inline Dictation / Speech-to-text Button
                            IconButton(
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (inlineSpeechState.isListening) {
                                        inlineSpeechState.stopListening()
                                    } else {
                                        inlineSpeechState.transcription = ""
                                        if (hasPermission) {
                                            inlineSpeechState.startListening()
                                        } else {
                                            inlinePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .testTag("tutor_inline_mic_button")
                                    .scale(micScale)
                            ) {
                                Icon(
                                    imageVector = if (inlineSpeechState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (inlineSpeechState.isListening) "Stop dictation" else "Speak question (voice-to-text)",
                                    tint = inlineMicColor
                                )
                            }

                            // Full Immersive Hands-Free Voice Dialogue Button
                            IconButton(
                                onClick = { showVoiceSession = true },
                                modifier = Modifier.testTag("tutor_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Start Immersive Socratic Voice Session",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tutor_text_input"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (inlineSpeechState.isListening) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = if (inlineSpeechState.isListening) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        focusedBorderColor = if (inlineSpeechState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (inlineSpeechState.isListening) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                    )
                )

                IconButton(
                    onClick = {
                        val txt = inputText
                        if (txt.isNotBlank()) {
                            viewModel.sendMessageToTutor(conceptId, deckId, txt)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("tutor_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            if (showAvatarCustomizer) {
                TwinCustomizerDialog(
                    currentTwin = profile?.selectedTwinAvatar ?: "socratic",
                    onDismiss = { showAvatarCustomizer = false },
                    onSelect = { selectedAvatar ->
                        viewModel.updateSelectedTwinAvatar(selectedAvatar)
                        showAvatarCustomizer = false
                    }
                )
            }

            if (showVoiceSession) {
                VoiceSessionDialog(
                    conceptName = entityName,
                    twinAvatar = profile?.selectedTwinAvatar ?: "socratic",
                    onDismiss = { showVoiceSession = false },
                    onSendSpeech = { spokenText ->
                        viewModel.sendMessageToTutor(conceptId, deckId, spokenText)
                        showVoiceSession = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, twinAvatar: String) {
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Custom Speech Recognition state holder
class SpeechRecognizerState(
    val context: Context,
    val onTranscriptionUpdated: (String) -> Unit,
    val onErrorOccurred: (String) -> Unit
) {
    var isListening by mutableStateOf(false)
    var rmsDb by mutableStateOf(0f)
    var transcription by mutableStateOf("")
    
    private var recognizer: SpeechRecognizer? = null
    
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorOccurred("Speech recognition is not supported on this device.")
            return
        }
        
        try {
            recognizer?.destroy()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        rmsDb = rmsdB
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client speech engine error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission denied"
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue occurred"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No matching voice heard"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech engine busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server-side speech error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
                            else -> "Speech recognition issue ($error)"
                        }
                        onErrorOccurred(errorMsg)
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            transcription = matches[0]
                            onTranscriptionUpdated(matches[0])
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            transcription = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            onErrorOccurred("Failed to initialize mic: ${e.localizedMessage}")
        }
    }
    
    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {}
        isListening = false
    }
    
    fun destroy() {
        try {
            recognizer?.destroy()
        } catch (e: Exception) {}
        recognizer = null
    }
}

// Animated waveform visualizer using standard jetpack compose transitions
@Composable
fun AudioWaveformVisualizer(
    isListening: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    // Create animated wave phases for the lines to simulate sound movement
    val baseHeightAnim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    val baseHeightAnim2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )
    val baseHeightAnim3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val numBars = 11
        for (i in 0 until numBars) {
            val multiplier = when (i % 3) {
                0 -> baseHeightAnim1
                1 -> baseHeightAnim2
                else -> baseHeightAnim3
            }
            
            // If listening, let RMS DB influence the height, else keep a tiny idle pulse
            val heightPercent = if (isListening) {
                ((rmsDb.coerceIn(0f, 15f) / 15f) * 0.7f + multiplier * 0.3f).coerceIn(0.1f, 1.0f)
            } else {
                0.15f + multiplier * 0.05f
            }
            
            val barColor = when (i) {
                in 0..2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                in 3..7 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(heightPercent)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

// Gorgeous Voice Q&A Overlay dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSessionDialog(
    conceptName: String,
    twinAvatar: String,
    onDismiss: () -> Unit,
    onSendSpeech: (String) -> Unit
) {
    val context = LocalContext.current
    var transcriptionText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val speechState = remember(context) {
        SpeechRecognizerState(
            context = context,
            onTranscriptionUpdated = { text -> transcriptionText = text },
            onErrorOccurred = { err -> errorMessage = err }
        )
    }

    // Handle lifetime of speech recognizer
    DisposableEffect(speechState) {
        onDispose {
            speechState.destroy()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                errorMessage = null
                speechState.startListening()
            } else {
                errorMessage = "Microphone permission is required for verbal sessions."
            }
        }
    )

    // Automatically check permission and start listening on open
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            speechState.startListening()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

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

    AlertDialog(
        onDismissRequest = {
            speechState.stopListening()
            onDismiss()
        },
        title = null, // Custom beautiful layout
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Verbal Q&A",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Socratic Verbal Q&A",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Digital Twin Avatar bubble
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = avatarRes),
                            contentDescription = "AI Twin Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = twinName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Speak naturally, ask your question, or respond to the digital tutor's concepts verbally!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Live animated Waveform Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AudioWaveformVisualizer(
                        isListening = speechState.isListening,
                        rmsDb = speechState.rmsDb,
                        modifier = Modifier.height(48.dp)
                    )
                }

                // Transcription Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 150.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (transcriptionText.isBlank()) {
                            Text(
                                text = if (speechState.isListening) "Listening... Speak now." else "Mic standby mode.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Text(
                                text = transcriptionText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Error State or Helpful Suggestion
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Mic Notice: Speech Engine Offline?",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Use Socratic Vocal Quick-Picks below to simulate perfect verbal interaction!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Socratic Speak Prompts (Quick Picks)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Socratic Voice Quick-Picks:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val speakPrompts = listOf(
                        "Explain this concept to me using an easy real-world analogy.",
                        "What are some common pitfalls or gotchas when working with $conceptName?",
                        "Give me a step-by-step example problem to solve right now.",
                        "Can you summarize the most critical takeaways for my upcoming exam?"
                    )
                    
                    speakPrompts.forEach { prompt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    transcriptionText = prompt
                                    errorMessage = null
                                }
                                .testTag("vocal_prompt_${prompt.take(15).replace(" ", "_")}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Simulate voice prompt",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Restart / Mic Button
                OutlinedButton(
                    onClick = {
                        transcriptionText = ""
                        errorMessage = null
                        speechState.startListening()
                    },
                    modifier = Modifier.weight(1f).testTag("vocal_retry_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Mic")
                    }
                }

                // Send speech transcription
                Button(
                    onClick = {
                        if (transcriptionText.isNotBlank()) {
                            speechState.stopListening()
                            onSendSpeech(transcriptionText)
                        }
                    },
                    enabled = transcriptionText.isNotBlank(),
                    modifier = Modifier.weight(1f).testTag("vocal_send_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send spoken text", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    speechState.stopListening()
                    onDismiss()
                },
                modifier = Modifier.testTag("vocal_dismiss_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
