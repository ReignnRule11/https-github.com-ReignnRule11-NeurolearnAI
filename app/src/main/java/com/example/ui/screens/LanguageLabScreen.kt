package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import com.example.data.SavedPhrase
import com.example.ui.MainViewModel
import com.example.ui.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageLabScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val pronunciationFeedback by viewModel.pronunciationFeedback.collectAsState()
    val phrases by viewModel.savedPhrases.collectAsState()

    var englishInputText by remember { mutableStateOf("") }
    var showAddPhraseDialog by remember { mutableStateOf(false) }
    var activePracticePhrase by remember { mutableStateOf<SavedPhrase?>(null) }

    // Dialog form state
    var formOriginal by remember { mutableStateOf("") }
    var formTranslation by remember { mutableStateOf("") }
    var formPronunciation by remember { mutableStateOf("") }

    // Available target languages
    val availableLanguages = listOf(
        Triple("Spanish", "🇪🇸", "es-ES"),
        Triple("French", "🇫🇷", "fr-FR"),
        Triple("German", "🇩🇪", "de-DE"),
        Triple("Japanese", "🇯🇵", "ja-JP"),
        Triple("Swahili", "🇰🇪", "sw-KE")
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LingoLab Multilingual",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "Learn, translate, and speak languages using Gemini AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("language_lab_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("language_lab_top_bar")
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPhraseDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Manual Phrase") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .testTag("add_phrase_fab")
                    .padding(bottom = 16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Panel
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Multilingual Hub",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Unlock master badges. Earn +15 XP for vocabulary card translations, or up to +20 XP for pronunciation accuracy scores!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Language Switcher (horizontal filter chips with custom emojis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableLanguages.forEach { (lang, flag, _) ->
                    val isSelected = selectedLang == lang
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectLanguage(lang) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(flag, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(lang)
                            }
                        },
                        modifier = Modifier.testTag("lang_chip_${lang.lowercase()}")
                    )
                }
            }

            // Interactive translation & learning helper
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✨ Translate and Save with Gemini AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter any English phrase to translate to $selectedLang, auto-generate pronunciation, and append it to your list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = englishInputText,
                        onValueChange = { englishInputText = it },
                        placeholder = { Text("e.g. Where is the nearest train station?") },
                        singleLine = true,
                        trailingIcon = {
                            if (isTranslating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                IconButton(
                                    onClick = {
                                        if (englishInputText.isNotBlank()) {
                                            viewModel.translateAndLearn(englishInputText)
                                            englishInputText = ""
                                        }
                                    },
                                    enabled = englishInputText.isNotBlank(),
                                    modifier = Modifier.testTag("translate_submit_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Translate"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("english_translation_input")
                    )

                    AnimatedVisibility(visible = translationResult != null) {
                        translationResult?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⚡ AI Learning Insight:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sub-header for vocabulary phrases
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedLang Vocabulary Study Desk",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${phrases.size} cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vocabulary List
            if (phrases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Study Desk Empty",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Use the AI translator or tap '+' to start building your custom $selectedLang list!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(phrases) { phrase ->
                        PhraseItemCard(
                            phrase = phrase,
                            onPracticeClick = { activePracticePhrase = phrase },
                            onToggleMastery = { viewModel.togglePhraseMastery(phrase.id, phrase.isMastered) },
                            onDelete = { viewModel.deletePhrase(phrase.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Manual Phrase Dialog
    if (showAddPhraseDialog) {
        Dialog(onDismissRequest = { showAddPhraseDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("manual_phrase_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "✍️ Custom $selectedLang Phrase",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )

                    OutlinedTextField(
                        value = formOriginal,
                        onValueChange = { formOriginal = it },
                        label = { Text("English Phrase") },
                        placeholder = { Text("e.g. Good morning, friend") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_original_input")
                    )

                    OutlinedTextField(
                        value = formTranslation,
                        onValueChange = { formTranslation = it },
                        label = { Text("$selectedLang Translation") },
                        placeholder = { Text("e.g. Buenos días, amigo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_translation_input")
                    )

                    OutlinedTextField(
                        value = formPronunciation,
                        onValueChange = { formPronunciation = it },
                        label = { Text("Phonetic Pronunciation (Optional)") },
                        placeholder = { Text("e.g. bweh-nohs dee-ahss ah-mee-goh") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_pronunciation_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                formOriginal = ""
                                formTranslation = ""
                                formPronunciation = ""
                                showAddPhraseDialog = false
                            },
                            modifier = Modifier.testTag("form_cancel_btn")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (formOriginal.isNotBlank() && formTranslation.isNotBlank()) {
                                    viewModel.addCustomPhrase(formOriginal, formTranslation, formPronunciation)
                                    formOriginal = ""
                                    formTranslation = ""
                                    formPronunciation = ""
                                    showAddPhraseDialog = false
                                }
                            },
                            enabled = formOriginal.isNotBlank() && formTranslation.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("form_submit_btn")
                        ) {
                            Text("Save Phrase")
                        }
                    }
                }
            }
        }
    }

    // Voice Transcription / Speech Practice Dialog
    if (activePracticePhrase != null) {
        val phrase = activePracticePhrase!!
        val speechLocaleCode = availableLanguages.find { it.first == selectedLang }?.third ?: "es-ES"

        Dialog(onDismissRequest = {
            activePracticePhrase = null
            // Reset feedback
            viewModel.evaluateSpeechPronunciation("", "")
        }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("speech_practice_dialog")
            ) {
                var isListening by remember { mutableStateOf(false) }
                var spokenTextResult by remember { mutableStateOf("") }
                var manualPracticeInput by remember { mutableStateOf("") }
                var isManualMode by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        spokenTextResult = ""
                        isListening = true
                    } else {
                        viewModel.showToast("Microphone permission is required for speech practice.")
                    }
                }

                // Native Speech Recognizer Instance
                val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
                val speechRecognizerIntent = remember {
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocaleCode)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechLocaleCode)
                        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, speechLocaleCode)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        speechRecognizer.destroy()
                    }
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Pronunciation Coach",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { activePracticePhrase = null },
                            modifier = Modifier.testTag("close_practice_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Divider()

                    // Target Phrase Showcase
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "SAY THIS PHRASE:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phrase.translatedText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Phonetic: ${phrase.pronunciation}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Translation: \"${phrase.originalText}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Native Speech Recognizer Button or Text Input Toggle
                    if (!isManualMode) {
                        // Native Speech engine listening UI
                        val recognitionListener = remember {
                            object : RecognitionListener {
                                override fun onReadyForSpeech(params: Bundle?) {
                                    Log.d("Speech", "Ready for speech")
                                }
                                override fun onBeginningOfSpeech() {
                                    Log.d("Speech", "Speech beginning")
                                }
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() {
                                    isListening = false
                                }
                                override fun onError(error: Int) {
                                    isListening = false
                                    Log.e("Speech", "Recognizer error code: $error")
                                }
                                override fun onResults(results: Bundle?) {
                                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    val topResult = matches?.firstOrNull() ?: ""
                                    if (topResult.isNotBlank()) {
                                        spokenTextResult = topResult
                                        viewModel.evaluateSpeechPronunciation(phrase.translatedText, topResult)
                                    }
                                }
                                override fun onPartialResults(partialResults: Bundle?) {}
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            }
                        }

                        LaunchedEffect(isListening) {
                            if (isListening) {
                                speechRecognizer.setRecognitionListener(recognitionListener)
                                speechRecognizer.startListening(speechRecognizerIntent)
                            } else {
                                speechRecognizer.stopListening()
                            }
                        }

                        Button(
                            onClick = {
                                if (isListening) {
                                    isListening = false
                                } else {
                                    spokenTextResult = ""
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasPermission) {
                                        isListening = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(90.dp)
                                .testTag("record_mic_btn")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Tap to speak",
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (isListening) "Listening" else "Tap",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                )
                            }
                        }

                        Text(
                            text = if (isListening) "Speak clearly in $selectedLang now..." else "Tap the microphone to speak and transcribe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { isManualMode = true },
                            modifier = Modifier.testTag("toggle_manual_btn")
                        ) {
                            Text("Keyboard practice mode ⌨️")
                        }
                    } else {
                        // Manual text entry simulation
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualPracticeInput,
                                onValueChange = { manualPracticeInput = it },
                                label = { Text("Type exactly what you say:") },
                                placeholder = { Text("e.g. ${phrase.translatedText}") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_practice_input")
                            )

                            Button(
                                onClick = {
                                    if (manualPracticeInput.isNotBlank()) {
                                        spokenTextResult = manualPracticeInput
                                        viewModel.evaluateSpeechPronunciation(phrase.translatedText, manualPracticeInput)
                                        manualPracticeInput = ""
                                    }
                                },
                                enabled = manualPracticeInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_practice_submit_btn")
                            ) {
                                Text("Check Spelling & Pronunciation")
                            }

                            TextButton(
                                onClick = { isManualMode = false },
                                modifier = Modifier.testTag("toggle_voice_btn")
                            ) {
                                Text("Switch back to Microphone 🎙️")
                            }
                        }
                    }

                    // Transcription Results & Feedback
                    AnimatedVisibility(visible = spokenTextResult.isNotBlank() || isTranslating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🗣️ TRANSCRIPTION RESULT:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )

                            if (spokenTextResult.isNotBlank()) {
                                Text(
                                    text = "\"$spokenTextResult\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }

                            if (isTranslating) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "AI Coach is evaluating phonetics...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            pronunciationFeedback?.let { (score, feedback) ->
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🎯 AI Pronunciation Accuracy:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "$score% Perfect",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                color = when {
                                                    score >= 85 -> Color(0xFF2E7D32)
                                                    score >= 60 -> Color(0xFFEF6C00)
                                                    else -> Color(0xFFC62828)
                                                }
                                            )
                                        )
                                    }

                                    // Circular score badge
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    score >= 85 -> Color(0xFFE8F5E9)
                                                    score >= 60 -> Color(0xFFFFF3E0)
                                                    else -> Color(0xFFFFEBEE)
                                                }
                                            )
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                score >= 85 -> Icons.Default.CheckCircle
                                                score >= 60 -> Icons.Default.Info
                                                else -> Icons.Default.Cancel
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                score >= 85 -> Color(0xFF2E7D32)
                                                score >= 60 -> Color(0xFFEF6C00)
                                                else -> Color(0xFFC62828)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = feedback,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun PhraseItemCard(
    phrase: SavedPhrase,
    onPracticeClick: () -> Unit,
    onToggleMastery: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = if (phrase.isMastered) BorderStroke(1.5.dp, Color(0xFF4CAF50).copy(alpha = 0.6f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("phrase_card_${phrase.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Status badge & delete action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mastery status chip
                Box(
                    modifier = Modifier
                        .background(
                            if (phrase.isMastered) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onToggleMastery() }
                        .testTag("toggle_mastery_${phrase.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (phrase.isMastered) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (phrase.isMastered) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (phrase.isMastered) "Mastered" else "Practice",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (phrase.isMastered) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_phrase_${phrase.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete phrase",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phrase Translation Details
            Text(
                text = phrase.translatedText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pronunciation: \"${phrase.pronunciation}\"",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "English: \"${phrase.originalText}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Call to action button to launch interactive speech
            Button(
                onClick = onPracticeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phrase_practice_btn_${phrase.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Speak and Transcribe", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
