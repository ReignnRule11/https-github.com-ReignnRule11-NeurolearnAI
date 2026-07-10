package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ConceptMastery
import com.example.data.StudyTask
import com.example.ui.MainViewModel
import com.example.ui.Screen
import java.text.SimpleDateFormat
import java.util.*

sealed interface RoadmapStep {
    data class StudyTaskStep(
        val task: StudyTask,
        val startTime: String,
        val durationMinutes: Int
    ) : RoadmapStep

    data class BreakStep(
        val startTime: String,
        val durationMinutes: Int,
        val breakType: String
    ) : RoadmapStep
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("study_planner_prefs", Context.MODE_PRIVATE) }

    // State from ViewModel
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val tasks by viewModel.studyTasks.collectAsStateWithLifecycle()
    val concepts by viewModel.allConcepts.collectAsStateWithLifecycle()
    val aiAdvice by viewModel.aiPlannerAdvice.collectAsStateWithLifecycle()
    val isAILoading by viewModel.isAILoading.collectAsStateWithLifecycle()

    // Local Exam Target States
    val currentLocalTime = System.currentTimeMillis()
    var targetExamName by remember { 
        mutableStateOf(sharedPrefs.getString("target_exam_name", "AP Calculus & CS Boards") ?: "AP Calculus & CS Boards") 
    }
    var targetExamDateMillis by remember { 
        mutableStateOf(sharedPrefs.getLong("target_exam_date_millis", currentLocalTime + 28L * 24 * 60 * 60 * 1000)) 
    }

    var showEditExamDialog by remember { mutableStateOf(false) }
    var editedExamName by remember { mutableStateOf("") }
    var editedExamDays by remember { mutableStateOf("28") }

    // Dynamic Roadmap calculation
    val roadmapSteps = remember(tasks, profile?.availableStudyTime) {
        val studyTime = profile?.availableStudyTime ?: 45
        val stepsList = mutableListOf<RoadmapStep>()
        if (tasks.isEmpty()) return@remember emptyList()

        val numTasks = tasks.size
        val breakDuration = 5
        val totalBreakTime = (numTasks - 1).coerceAtLeast(0) * breakDuration
        val availableTaskTime = (studyTime - totalBreakTime).coerceAtLeast(10)
        val durationPerTask = availableTaskTime / numTasks

        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        tasks.forEachIndexed { index, task ->
            stepsList.add(
                RoadmapStep.StudyTaskStep(
                    task = task,
                    startTime = timeFormat.format(calendar.time),
                    durationMinutes = durationPerTask
                )
            )
            calendar.add(Calendar.MINUTE, durationPerTask)

            if (index < numTasks - 1) {
                val breakType = when (index % 3) {
                    0 -> "Cognitive Rest (Pomodoro Break)"
                    1 -> "Hydration Break"
                    else -> "Mindful Stretch & Breathe"
                }
                stepsList.add(
                    RoadmapStep.BreakStep(
                        startTime = timeFormat.format(calendar.time),
                        durationMinutes = breakDuration,
                        breakType = breakType
                    )
                )
                calendar.add(Calendar.MINUTE, breakDuration)
            }
        }
        stepsList
    }

    // Days Countdown
    val daysRemaining = remember(targetExamDateMillis) {
        val diffMillis = targetExamDateMillis - System.currentTimeMillis()
        if (diffMillis > 0) {
            (diffMillis / (1000L * 60 * 60 * 24)).toInt() + 1
        } else {
            0
        }
    }

    val weakConcepts = remember(concepts) {
        concepts.filter { (it.understandingScore + it.confidenceScore) / 2f < 0.6f }
    }

    Scaffold(
        modifier = Modifier.testTag("study_planner_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Intelligent Study Planner",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("study_planner_back_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            
            // --- SECTION 1: EXAM TARGETS & GOALS EDITOR ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exam_goals_editor_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TARGET EXAM GOAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = targetExamName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    editedExamName = targetExamName
                                    editedExamDays = daysRemaining.toString()
                                    showEditExamDialog = true
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), CircleShape)
                                    .size(36.dp)
                                    .testTag("edit_exam_goal_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Goal",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Days Left Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$daysRemaining Days",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onTertiary
                                    )
                                    Text(
                                        text = "Exam Countdown",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Daily Time Budget Card
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${profile?.availableStudyTime ?: 45} mins",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Daily Time Budget",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: THE ROADMAP ADVISOR & SCHEDULER BAR ---
            item {
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    )
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradientBrush, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Socratic Roadmap Advisor",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.generateIntelligentStudySchedulerPlan() },
                                enabled = !isAILoading,
                                modifier = Modifier.testTag("advisor_regenerate_btn")
                            ) {
                                if (isAILoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate Plan",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isAILoading && aiAdvice == null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Analyzing concept mastery, recall curves, and exam goals...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = aiAdvice ?: "No roadmap generated yet. Tap 'Generate Daily Roadmap' to construct an optimized timeline guided by your Digital Twin.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }

                        if (tasks.isEmpty() && !isAILoading) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.generateIntelligentStudySchedulerPlan() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roadmap_generate_action_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Interactive Daily Roadmap")
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: INTERACTIVE DAILY TIMELINE ROADMAP ---
            if (roadmapSteps.isNotEmpty()) {
                item {
                    Text(
                        text = "Today's Chronological Roadmap",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(roadmapSteps) { index, step ->
                    val isFirst = index == 0
                    val isLast = index == roadmapSteps.lastIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        // Left Timeline node representation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(40.dp)
                                .fillMaxHeight()
                        ) {
                            // Top half line
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(
                                        if (isFirst) Color.Transparent 
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                            )

                            // Inner circle indicator
                            when (step) {
                                is RoadmapStep.StudyTaskStep -> {
                                    val statusColor = if (step.task.isCompleted) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .border(2.dp, statusColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (step.task.isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                is RoadmapStep.BreakStep -> {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom half line
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(
                                        if (isLast) Color.Transparent 
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                            )
                        }

                        // Right Step contents
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 12.dp, start = 8.dp)
                        ) {
                            when (step) {
                                is RoadmapStep.StudyTaskStep -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("roadmap_task_card_${step.task.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (step.task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (step.task.isCompleted) Color.Transparent
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = step.task.isCompleted,
                                                    onCheckedChange = { viewModel.toggleTaskCompletion(step.task) },
                                                    modifier = Modifier.testTag("roadmap_checkbox_${step.task.id}")
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = step.task.conceptName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            textDecoration = if (step.task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                        ),
                                                        color = if (step.task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                                        else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = step.startTime,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "•",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "${step.durationMinutes} mins",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "•",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "+${step.task.xpAwarded} XP",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF43A047)
                                                        )
                                                    }
                                                }
                                            }

                                            if (!step.task.isCompleted) {
                                                Button(
                                                    onClick = {
                                                        if (step.task.taskType == "deck" && step.task.deckId != null) {
                                                            viewModel.navigateTo(Screen.Review)
                                                        } else {
                                                            viewModel.startTutorSession(step.task.conceptId)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp).testTag("start_roadmap_task_${step.task.id}")
                                                ) {
                                                    Text("Start", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }
                                }
                                is RoadmapStep.BreakStep -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = step.breakType,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Text(
                                                text = "${step.startTime} (${step.durationMinutes}m)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 4: PROGRESS ANALYZER (WEAK CONCEPT GAPS) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Revision Gaps Detected (${weakConcepts.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                              )
                        }

                        Text(
                            text = "Concepts with cognitive baseline understanding below 60%. Schedule reviews to close these gaps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (weakConcepts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Excellent job! No active knowledge gaps found. 🏆",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF43A047)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                weakConcepts.take(3).forEach { concept ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = concept.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${concept.subject} • Mastery: ${(concept.understandingScore * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.startTutorSession(concept.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp).testTag("gap_tutor_btn_${concept.id}")
                                        ) {
                                            Text("Tutor", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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

    // Edit Exam Target Dialog
    if (showEditExamDialog) {
        AlertDialog(
            onDismissRequest = { showEditExamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Exam Goal & Date",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editedExamName,
                        onValueChange = { editedExamName = it },
                        label = { Text("Exam Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_exam_name_input")
                    )

                    OutlinedTextField(
                        value = editedExamDays,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                editedExamDays = it
                            }
                        },
                        label = { Text("Days until Exam") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_exam_days_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = editedExamDays.toLongOrNull() ?: 28L
                        val targetMillis = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L)
                        sharedPrefs.edit()
                            .putString("target_exam_name", editedExamName)
                            .putLong("target_exam_date_millis", targetMillis)
                            .apply()
                        targetExamName = editedExamName
                        targetExamDateMillis = targetMillis
                        showEditExamDialog = false
                    },
                    modifier = Modifier.testTag("save_exam_goal_btn")
                ) {
                    Text("Save Goals")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditExamDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("edit_exam_goal_dialog")
        )
    }
}
