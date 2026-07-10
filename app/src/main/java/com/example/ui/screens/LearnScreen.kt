package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.data.ConceptMastery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(viewModel: MainViewModel) {
    val concepts by viewModel.allConcepts.collectAsState()
    var selectedSubject by remember { mutableStateOf("Calculus") }
    var selectedConceptForDetail by remember { mutableStateOf<ConceptMastery?>(null) }
    var quizDifficulty by remember { mutableStateOf("Medium") }

    val filteredConcepts = concepts.filter { it.subject == selectedSubject }
    val subjects = listOf(
        "Calculus", "Computer Science", "Chemistry",
        "Product Management", "Software Development", "Web3 & Blockchain",
        "E-commerce", "Business Analysis", "Product Design",
        "Project Management", "Digital Marketing", "Data Analysis"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("learn_screen_container")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Header
        Text(
            text = "Knowledge Map",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = "Navigate your concept dependency tree",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Tabs
        ScrollableTabRow(
            selectedTabIndex = subjects.indexOf(selectedSubject),
            edgePadding = 24.dp,
            divider = {},
            containerColor = Color.Transparent,
            modifier = Modifier.testTag("subject_tabs")
        ) {
            subjects.forEachIndexed { index, subject ->
                Tab(
                    selected = selectedSubject == subject,
                    onClick = {
                        selectedSubject = subject
                        selectedConceptForDetail = null
                    },
                    modifier = Modifier.testTag("tab_$subject")
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedSubject == subject) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedSubject == subject) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main List or Details Panel
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Column: Concept Dependency Graph Nodes
            LazyColumn(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredConcepts) { concept ->
                    ConceptNodeCard(
                        concept = concept,
                        isSelected = selectedConceptForDetail?.id == concept.id,
                        onClick = { selectedConceptForDetail = concept }
                    )

                    // Draw connecting arrow if not the last item
                    if (filteredConcepts.indexOf(concept) < filteredConcepts.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardDoubleArrowDown,
                                contentDescription = "Prerequisite Connection",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }

            // Right Column or Sheet: Active Concept Study Details
            AnimatedVisibility(
                visible = selectedConceptForDetail != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                selectedConceptForDetail?.let { concept ->
                    // Re-fetch concept state dynamically to ensure we display updated data
                    val freshConcept = concepts.find { it.id == concept.id } ?: concept
                    ConceptDetailPane(
                        concept = freshConcept,
                        quizDifficulty = quizDifficulty,
                        onDifficultyChange = { quizDifficulty = it },
                        viewModel = viewModel,
                        onClose = { selectedConceptForDetail = null }
                    )
                }
            }
        }
    }
}

@Composable
fun ConceptNodeCard(
    concept: ConceptMastery,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("concept_node_${concept.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Difficulty badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (concept.difficulty) {
                                "Easy" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                "Medium" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                else -> Color(0xFFF44336).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = concept.difficulty,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = when (concept.difficulty) {
                            "Easy" -> Color(0xFF2E7D32)
                            "Medium" -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mastery Gauge Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = concept.understandingScore,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "${(concept.understandingScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ConceptDetailPane(
    concept: ConceptMastery,
    quizDifficulty: String,
    onDifficultyChange: (String) -> Unit,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val prereqCheck = viewModel.checkPrerequisitesMet(concept)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("concept_detail_pane"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pane Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Study Concept",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onClose, modifier = Modifier.testTag("pane_close_button")) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close pane")
            }
        }

        Text(
            text = concept.name,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        // Prerequisite Warning (Prerequisite Detection!)
        if (!prereqCheck.isMet) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Prerequisites Locked",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Prerequisite Locked",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Your digital twin suggests mastering: ${prereqCheck.unmetPrerequisites.joinToString()} first to avoid study fatigue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Mastery stats list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MasteryStatRow(label = "Understanding", value = concept.understandingScore)
            MasteryStatRow(label = "Confidence", value = concept.confidenceScore)
            MasteryStatRow(label = "Predicted Exam", value = concept.predictedExamPerformance)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Options
        Text(
            text = "Learning Engine Controls",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Launch AI Tutor Button
        Button(
            onClick = { viewModel.startTutorSession(concept.id) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pane_tutor_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat with AI Tutor")
            }
        }

        // 2. Quiz Generator with difficulty
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quiz Level: $quizDifficulty",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { level ->
                        val active = quizDifficulty == level
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onDifficultyChange(level) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("quiz_diff_$level")
                        ) {
                            Text(
                                text = level,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { viewModel.startQuiz(concept.id, quizDifficulty) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pane_quiz_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Custom Quiz")
                }
            }
        }

        // 3. Document Synthesis (PDF Intelligence)
        OutlinedButton(
            onClick = { viewModel.navigateTo(Screen.PdfIntelligence(concept.id)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pane_pdf_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthesize Notes")
            }
        }
    }
}

@Composable
fun MasteryStatRow(label: String, value: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = value,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}
