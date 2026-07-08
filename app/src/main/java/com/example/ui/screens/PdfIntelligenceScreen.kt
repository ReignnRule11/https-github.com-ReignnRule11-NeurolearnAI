package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfIntelligenceScreen(viewModel: MainViewModel, conceptId: String) {
    val concepts by viewModel.allConcepts.collectAsState()
    val isLoading by viewModel.isAILoading.collectAsState()
    val generatedNotes by viewModel.generatedNotes.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val concept = concepts.find { it.id == conceptId }
    val conceptName = concept?.name ?: "Topic"

    var pastedNotesText by remember { mutableStateOf("") }
    var activeSubTab by remember { mutableStateOf("Summary") }

    // Curated samples for quick testing
    val samples = mapOf(
        "Calculus Limits" to """
            Limits and Continuity form the bedrocks of Calculus. The limit of f(x) as x approaches a is the value L that the function outputs get arbitrarily close to.
            Continuity requires: 1) f(a) is defined, 2) lim x->a f(x) exists, and 3) lim x->a f(x) = f(a).
            Common limit theorems: Squeeze theorem, L'Hopital's rule for indeterminate forms like 0/0 and inf/inf.
        """.trimIndent(),
        "CS Control Flow" to """
            Control Flow determines the order of executing statements in a program. If-else conditional blocks allow programs to branch based on Boolean conditions.
            Loops execute code repeatedly. 'While' loops run while a condition is true. 'For' loops iterate over ranges or collections.
            Break terminates the loop immediately, whereas continue skips the current iteration and goes to the next condition evaluation.
        """.trimIndent(),
        "Chemistry Bonds" to """
            Chemical Bonding describes how atoms bond together. Ionic bonding is formed by the complete transfer of valence electrons from a metal to a non-metal, creating charged ions.
            Covalent bonding is formed by sharing of electron pairs between non-metal atoms.
            Metallic bonding is the pooling of electrons as a shared sea of free-moving electrons around metallic nuclei.
        """.trimIndent()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "PDF & Notes Synthesizer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Topic: $conceptName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Learn) }, modifier = Modifier.testTag("pdf_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("pdf_screen_container")
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI is extracting formulas, summaries, and flashcards...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (generatedNotes == null) {
                // Notes Input Form
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Extract Core Study Assets",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Paste your lecture slides, notes, textbook pages, or try out a sample document below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Predefined Samples
                    item {
                        Text(
                            text = "Select Sample Document",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            samples.keys.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { pastedNotesText = samples[key] ?: "" }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("sample_$key")
                                ) {
                                    Text(text = key, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    // Notes Text Area Input
                    item {
                        OutlinedTextField(
                            value = pastedNotesText,
                            onValueChange = { pastedNotesText = it },
                            placeholder = { Text("Paste textbook pages, lecture slides, notes, or math formulas here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("notes_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Process Button
                    item {
                        Button(
                            onClick = { viewModel.processLectureNotes(conceptId, pastedNotesText) },
                            shape = RoundedCornerShape(12.dp),
                            enabled = pastedNotesText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("synthesize_notes_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesize Study Assets with AI")
                            }
                        }
                    }
                }
            } else {
                // Display Generated Results
                val notes = generatedNotes!!
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sub Tabs selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Summary", "Glossary", "Flashcards", "Quiz").forEach { tabName ->
                            val active = activeSubTab == tabName
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { activeSubTab = tabName }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("sub_tab_$tabName")
                            ) {
                                Text(
                                    text = tabName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeSubTab) {
                            "Summary" -> {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Text(
                                                text = "Material Synthesis",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = notes.summary,
                                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (notes.formulas.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Extracted Formulas & Core Rules",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    items(notes.formulas) { formula ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = formula,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            "Glossary" -> {
                                items(notes.glossary) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = item.term,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.definition,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            "Flashcards" -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "✨ Spaced repetition cards loaded! They will appear dynamically on your Review screen according to the SM-2 algorithm schedule.",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                items(notes.flashcards) { card ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Q: ${card.question}",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "A: ${card.answer}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            "Quiz" -> {
                                items(notes.quizzes) { quiz ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = quiz.question,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            quiz.options.forEachIndexed { i, opt ->
                                                val isCorrect = opt == quiz.correctAnswer
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isCorrect) Color(0xFF4CAF50)
                                                            else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = "${('A' + i)}. $opt",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reset button
                    Button(
                        onClick = { coroutineScope.launch { viewModel.resetData() } },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset & Upload New Material")
                    }
                }
            }
        }
    }
}
