package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.data.ConceptMastery
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun ProgressScreen(viewModel: MainViewModel) {
    val profile by viewModel.profile.collectAsState()
    val concepts by viewModel.allConcepts.collectAsState()

    // Calculations
    val averageUnderstanding = if (concepts.isNotEmpty()) concepts.map { it.understandingScore }.average().toFloat() else 0.5f
    val averageConfidence = if (concepts.isNotEmpty()) concepts.map { it.confidenceScore }.average().toFloat() else 0.4f
    val averageRetention = if (concepts.isNotEmpty()) concepts.map { it.retentionScore }.average().toFloat() else 0.45f
    val examReadiness = if (concepts.isNotEmpty()) concepts.map { it.predictedExamPerformance }.average().toFloat() else 0.45f

    val strongConcepts = concepts.filter { it.understandingScore >= 0.6f }.sortedByDescending { it.understandingScore }
    val weakConcepts = concepts.filter { it.understandingScore < 0.6f }.sortedBy { it.understandingScore }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .testTag("progress_screen_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Digital Learning Twin",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Cognitive Learner Identity Model",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Cognitive Style Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Learning Style",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Twin Characteristics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TwinTraitRow(label = "Cognitive Learning Style", value = profile?.learningStyle ?: "Conceptual & Step-by-Step")
                    TwinTraitRow(label = "Curriculum Level", value = profile?.curriculum ?: "AP / College Prep")
                    TwinTraitRow(label = "Diagnostic Score", value = "${((profile?.diagnosticScore ?: 0.5f) * 100).toInt()}%")
                    TwinTraitRow(label = "Memory Retention Decay", value = "Standard Ebbinghaus Model")
                }
            }
        }

        // Central AI Digital Twin Status Dashboard
        item {
            var selectedRadarTab by remember { mutableStateOf(0) } // 0 = Subject Mastery, 1 = Cognitive Sync
            
            val subjectMasteries = remember(concepts) {
                if (concepts.isEmpty()) {
                    mapOf("Calculus" to 0.5f, "Computer Science" to 0.5f, "Chemistry" to 0.5f)
                } else {
                    concepts.groupBy { it.subject }.mapValues { (_, subjectConcepts) ->
                        if (subjectConcepts.isEmpty()) 0.0f else {
                            subjectConcepts.map { (it.understandingScore + it.retentionScore + it.confidenceScore + it.predictedExamPerformance) / 4f }.average().toFloat()
                        }
                    }
                }
            }
            
            val cognitiveDimensions = remember(averageUnderstanding, examReadiness, averageConfidence, averageRetention, profile) {
                mapOf(
                    "Understanding" to averageUnderstanding,
                    "Exam Readiness" to examReadiness,
                    "Confidence" to averageConfidence,
                    "Retention" to averageRetention,
                    "Diagnostic" to (profile?.diagnosticScore ?: 0.5f)
                )
            }
            
            val activeData = if (selectedRadarTab == 0) subjectMasteries else cognitiveDimensions
            val chartColor = if (selectedRadarTab == 0) MaterialTheme.colorScheme.primary else Color(0xFF00BCD4)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_twin_dashboard_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "AI Sync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Twin Cognitive Sync",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time neuro-diagnostic mirror",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val averageMastery = activeData.values.average().toFloat()
                            val statusText = when {
                                averageMastery >= 0.8f -> "Synchronized (High)"
                                averageMastery >= 0.5f -> "Active Sync (Med)"
                                else -> "Calibrating (Low)"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Radar Tab selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Subject Mastery", "Cognitive Sync").forEachIndexed { index, title ->
                            val selected = (selectedRadarTab == index)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .clickable { selectedRadarTab = index }
                                    .testTag("radar_tab_$index"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Radar Chart Component
                    RadarChart(
                        data = activeData,
                        color = chartColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("radar_chart_canvas")
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Small legend / advice
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Insight",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val currentInsight = if (selectedRadarTab == 0) {
                            val weakestSubject = activeData.minByOrNull { it.value }?.key ?: "N/A"
                            "Twin status suggests prioritizing study tasks under **$weakestSubject**."
                        } else {
                            val lowestDim = activeData.minByOrNull { it.value }?.key ?: "N/A"
                            "Work on Socratic reviews and flashcards to improve your **$lowestDim** metric."
                        }
                        Text(
                            text = currentInsight,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Summary Performance Gauges
        item {
            Text(
                text = "Aesthetic Performance metrics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProgressMetricGauges(label = "Average Concept Understanding", value = averageUnderstanding, color = MaterialTheme.colorScheme.primary)
                    ProgressMetricGauges(label = "Predicted Exam Readiness", value = examReadiness, color = Color(0xFF4CAF50))
                    ProgressMetricGauges(label = "Twin Confidence Score", value = averageConfidence, color = Color(0xFF00BCD4))
                    ProgressMetricGauges(label = "Active Material Retention", value = averageRetention, color = Color(0xFFFF9800))
                }
            }
        }

        // Weak Concepts (Focus Required)
        item {
            Text(
                text = "Knowledge Gaps (Focus Required)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (weakConcepts.isEmpty()) {
            item {
                Text(
                    text = "No weak concepts detected! Your Learning Twin is fully optimized.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(weakConcepts) { concept ->
                ConceptCompactRow(concept = concept, isWeak = true)
            }
        }

        // Strong Concepts (Mastered)
        item {
            Text(
                text = "Strengths & Masteries",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (strongConcepts.isEmpty()) {
            item {
                Text(
                    text = "No concepts fully mastered yet. Review cards or take quizzes to progress!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(strongConcepts) { concept ->
                ConceptCompactRow(concept = concept, isWeak = false)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TwinTraitRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProgressMetricGauges(label: String, value: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = value,
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    }
}

@Composable
fun ConceptCompactRow(concept: ConceptMastery, isWeak: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Subject: ${concept.subject}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isWeak) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = if (isWeak) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${(concept.understandingScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isWeak) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun RadarChart(
    data: Map<String, Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val labelTextSize = 10f * density
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).toArgb()
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f).toArgb()
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2 * 0.70f
        
        val keys = data.keys.toList()
        val values = data.values.toList()
        val numAxes = keys.size
        
        if (numAxes < 3) return@Canvas
        
        // 1. Draw grid circles/polygons
        val levels = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
        val gridPaint = Paint().apply {
            this.color = gridColor
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            isAntiAlias = true
        }
        
        levels.forEach { level ->
            val path = Path()
            for (i in 0 until numAxes) {
                val angle = -PI / 2 + i * (2 * PI / numAxes)
                val x = centerX + cos(angle) * maxRadius * level
                val y = centerY + sin(angle) * maxRadius * level
                if (i == 0) {
                    path.moveTo(x.toFloat(), y.toFloat())
                } else {
                    path.lineTo(x.toFloat(), y.toFloat())
                }
            }
            path.close()
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(path, gridPaint)
            }
        }
        
        // 2. Draw spoke lines and labels
        val textPaint = Paint().apply {
            this.color = labelColor
            textSize = labelTextSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val axisPaint = Paint().apply {
            this.color = axisColor
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            isAntiAlias = true
        }
        
        for (i in 0 until numAxes) {
            val angle = -PI / 2 + i * (2 * PI / numAxes)
            val outerX = centerX + cos(angle) * maxRadius
            val outerY = centerY + sin(angle) * maxRadius
            
            // Spoke line
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawLine(centerX, centerY, outerX.toFloat(), outerY.toFloat(), axisPaint)
            }
            
            // Text offset
            val textDistance = maxRadius + 15f * density
            val tx = centerX + cos(angle) * textDistance
            // Adjust vertical alignment slightly based on y coordinate to avoid overlap
            val ty = centerY + sin(angle) * textDistance + (if (sin(angle) > 0.1) 8f * density else if (sin(angle) < -0.1) -4f * density else 4f * density)
            
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(keys[i], tx.toFloat(), ty.toFloat(), textPaint)
            }
        }
        
        // 3. Draw data polygon
        val dataPath = Path()
        val vertexPoints = mutableListOf<Offset>()
        for (i in 0 until numAxes) {
            val angle = -PI / 2 + i * (2 * PI / numAxes)
            val score = values[i].coerceIn(0.0f, 1.0f)
            val x = centerX + cos(angle) * maxRadius * score
            val y = centerY + sin(angle) * maxRadius * score
            val point = Offset(x.toFloat(), y.toFloat())
            vertexPoints.add(point)
            
            if (i == 0) {
                dataPath.moveTo(point.x, point.y)
            } else {
                dataPath.lineTo(point.x, point.y)
            }
        }
        dataPath.close()
        
        val fillPaint = Paint().apply {
            this.color = color.copy(alpha = 0.25f).toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val strokePaint = Paint().apply {
            this.color = color.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            isAntiAlias = true
        }
        
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(dataPath, fillPaint)
            canvas.nativeCanvas.drawPath(dataPath, strokePaint)
        }
        
        // 4. Draw glowing dots at vertices
        val dotPaint = Paint().apply {
            this.color = color.toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val dotOuterPaint = Paint().apply {
            this.color = color.copy(alpha = 0.4f).toArgb()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        vertexPoints.forEach { point ->
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawCircle(point.x, point.y, 6f * density, dotOuterPaint)
                canvas.nativeCanvas.drawCircle(point.x, point.y, 3.5f * density, dotPaint)
            }
        }
    }
}
