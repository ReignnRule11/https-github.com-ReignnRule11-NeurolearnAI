package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val profile by viewModel.profile.collectAsState()
    var studyReminders by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .testTag("profile_screen_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Account Profile",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage your goals and AI configurations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Account Metadata Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Avatar",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = profile?.name ?: "Alex",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = profile?.email ?: "alex@neurolearn.ai",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Role based access switcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_switcher_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Role Access Control",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "RBAC System Access Role",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Customize your permission level. Changing roles dynamically adjusts visible dashboards and diagnostic actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentRole = profile?.role ?: "Learner"
                        val rolesList = listOf("Learner", "Instructor", "Admin")

                        rolesList.forEach { roleName ->
                            val isSelected = currentRole == roleName
                            val colorScheme = MaterialTheme.colorScheme

                            val icon = when (roleName) {
                                "Learner" -> Icons.Default.School
                                "Instructor" -> Icons.Default.SupervisedUserCircle
                                else -> Icons.Default.Settings
                            }

                            val bg = when {
                                isSelected && roleName == "Learner" -> colorScheme.primaryContainer
                                isSelected && roleName == "Instructor" -> colorScheme.secondaryContainer
                                isSelected && roleName == "Admin" -> colorScheme.tertiaryContainer
                                else -> colorScheme.surface
                            }

                            val borderCol = when {
                                isSelected && roleName == "Learner" -> colorScheme.primary
                                isSelected && roleName == "Instructor" -> colorScheme.secondary
                                isSelected && roleName == "Admin" -> colorScheme.tertiary
                                else -> colorScheme.outline.copy(alpha = 0.15f)
                            }

                            val textCol = when {
                                isSelected && roleName == "Learner" -> colorScheme.onPrimaryContainer
                                isSelected && roleName == "Instructor" -> colorScheme.onSecondaryContainer
                                isSelected && roleName == "Admin" -> colorScheme.onTertiaryContainer
                                else -> colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { viewModel.updateUserRole(roleName) }
                                    .testTag("role_btn_$roleName"),
                                colors = CardDefaults.cardColors(containerColor = bg),
                                border = BorderStroke(1.5.dp, borderCol),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = roleName,
                                        tint = textCol,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = roleName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = textCol
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Premium Subscription Tier Card
        item {
            val isPremium = profile?.isPremium == true
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) MaterialTheme.colorScheme.tertiaryContainer 
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.5.dp, 
                    if (isPremium) MaterialTheme.colorScheme.tertiary 
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("profile_premium_card")
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPremium) "👑 NeuroLearn Premium Max" else "🌟 Free Study Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPremium) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isPremium) "Unlimited AI Matcher, Code Reviews & Smart PDF parsing" 
                                   else "Unlock Unlimited matching with Premium Max for $9.99/mo",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPremium) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        if (!isPremium) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.upgradeToPremium() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("profile_upgrade_btn")
                            ) {
                                Text("Upgrade to Premium Max", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    if (isPremium) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "PRO MAX",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                }
            }
        }

        // NeuroCoins Balances item
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${profile?.coins ?: 0} NeuroCoins",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Earn +15 coins by completing tasks!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.addCoinsReward(100) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("free_refill_btn")
                    ) {
                        Text("+100 Free Refill", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Achievements Section State
        item {
            val allFlashcards by viewModel.allFlashcards.collectAsState(initial = emptyList())
            val masteredCount = remember(allFlashcards) { allFlashcards.filter { it.repetitions >= 1 }.size }
            val currentStreak = profile?.streak ?: 0
            val currentLevel = profile?.level ?: 1
            val cardsReviewedCount = profile?.cardsReviewedCount ?: 0
            val quizzesCompletedCount = profile?.quizzesCompletedCount ?: 0
            
            val profileBadges = listOf(
                BadgeData(
                    title = "Novice Spark",
                    description = "Start a 1-day study streak",
                    icon = Icons.Default.FlashOn,
                    color = Color(0xFFE040FB),
                    isUnlocked = currentStreak >= 1
                ),
                BadgeData(
                    title = "7-Day Neuro Titan",
                    description = "Maintain a 7-day study streak",
                    icon = Icons.Default.Whatshot,
                    color = Color(0xFFFF5722),
                    isUnlocked = currentStreak >= 7
                ),
                BadgeData(
                    title = "Retention Disciple",
                    description = "Master 10+ flashcards",
                    icon = Icons.Default.Star,
                    color = Color(0xFF4CAF50),
                    isUnlocked = masteredCount >= 10
                ),
                BadgeData(
                    title = "Concept Overlord",
                    description = "Master 50+ flashcards",
                    icon = Icons.Default.WorkspacePremium,
                    color = Color(0xFF2196F3),
                    isUnlocked = masteredCount >= 50
                ),
                BadgeData(
                    title = "Memory Grandmaster",
                    description = "Master 100+ flashcards",
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFFFFB300),
                    isUnlocked = masteredCount >= 100
                ),
                BadgeData(
                    title = "Level Prodigy",
                    description = "Achieve Level 5 or higher",
                    icon = Icons.Default.MilitaryTech,
                    color = Color(0xFF9C27B0),
                    isUnlocked = currentLevel >= 5
                ),
                BadgeData(
                    title = "Spaced Repetition Disciple",
                    description = "Review 10+ flashcards",
                    icon = Icons.Default.MenuBook,
                    color = Color(0xFF00E676),
                    isUnlocked = cardsReviewedCount >= 10
                ),
                BadgeData(
                    title = "Twin Quiz Pioneer",
                    description = "Complete 1 Twin Quiz",
                    icon = Icons.Default.Quiz,
                    color = Color(0xFF29B6F6),
                    isUnlocked = quizzesCompletedCount >= 1
                ),
                BadgeData(
                    title = "Quiz Champion",
                    description = "Complete 5+ Twin Quizzes",
                    icon = Icons.Default.WorkspacePremium,
                    color = Color(0xFFFFCA28),
                    isUnlocked = quizzesCompletedCount >= 5
                )
            )

            var selectedBadgeForDetail by remember { mutableStateOf<BadgeData?>(null) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_achievements_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Achievements",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cognitive Achievements",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Unlock visual milestones to boost neuro-retention",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Badges Grid
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (i in profileBadges.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                profileBadges.getOrNull(i)?.let { badge ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProfileBadgeItem(badge = badge, onClick = { selectedBadgeForDetail = badge })
                                    }
                                }
                                profileBadges.getOrNull(i + 1)?.let { badge ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProfileBadgeItem(badge = badge, onClick = { selectedBadgeForDetail = badge })
                                    }
                                }
                            }
                        }
                    }
                    
                    val unlockedCount = profileBadges.count { it.isUnlocked }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Milestones Unlocked",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$unlockedCount of ${profileBadges.size} Badges",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Badge Detail Dialog
            selectedBadgeForDetail?.let { badge ->
                val currentProgress = when (badge.title) {
                    "Novice Spark" -> if (currentStreak >= 1) 1f else 0f
                    "7-Day Neuro Titan" -> (currentStreak / 7f).coerceIn(0f, 1f)
                    "Retention Disciple" -> (masteredCount / 10f).coerceIn(0f, 1f)
                    "Concept Overlord" -> (masteredCount / 50f).coerceIn(0f, 1f)
                    "Memory Grandmaster" -> (masteredCount / 100f).coerceIn(0f, 1f)
                    "Level Prodigy" -> (currentLevel / 5f).coerceIn(0f, 1f)
                    "Spaced Repetition Disciple" -> (cardsReviewedCount / 10f).coerceIn(0f, 1f)
                    "Twin Quiz Pioneer" -> (quizzesCompletedCount / 1f).coerceIn(0f, 1f)
                    "Quiz Champion" -> (quizzesCompletedCount / 5f).coerceIn(0f, 1f)
                    else -> if (badge.isUnlocked) 1f else 0f
                }
                
                val progressText = when (badge.title) {
                    "Novice Spark" -> if (currentStreak >= 1) "Completed (1/1 Days)" else "Not Started (0/1 Days)"
                    "7-Day Neuro Titan" -> "${currentStreak.coerceAtMost(7)} / 7 Days Streak"
                    "Retention Disciple" -> "${masteredCount.coerceAtMost(10)} / 10 Cards Mastered"
                    "Concept Overlord" -> "${masteredCount.coerceAtMost(50)} / 50 Cards Mastered"
                    "Memory Grandmaster" -> "${masteredCount.coerceAtMost(100)} / 100 Cards Mastered"
                    "Level Prodigy" -> "Level $currentLevel / 5"
                    "Spaced Repetition Disciple" -> "${cardsReviewedCount.coerceAtMost(10)} / 10 Cards Reviewed"
                    "Twin Quiz Pioneer" -> "${quizzesCompletedCount.coerceAtMost(1)} / 1 Twin Quizzes Completed"
                    "Quiz Champion" -> "${quizzesCompletedCount.coerceAtMost(5)} / 5 Twin Quizzes Completed"
                    else -> if (badge.isUnlocked) "Completed" else "Locked"
                }

                val motivationalQuote = when (badge.title) {
                    "Novice Spark" -> "The longest journey begins with a single step. You have initiated your active recall cycle!"
                    "7-Day Neuro Titan" -> "Consistency builds powerful synapses. A 7-day streak transforms studying into an effortless habit!"
                    "Retention Disciple" -> "Excellent progress. Reviewing cards regularly keeps your long-term memory retrieval pathways active."
                    "Concept Overlord" -> "Fabulous dedication! Mastering 50 concepts demonstrates superb academic discipline and recall strength."
                    "Memory Grandmaster" -> "Phenomenal achievement! You have mastered 100 concepts, indicating an elite level of dynamic retention."
                    "Level Prodigy" -> "Your intelligence index is scaling up rapidly. Level 5 proves you are a top-tier cognitive scholar!"
                    "Spaced Repetition Disciple" -> "Excellent work reviewing cards. Each review strengthens your neurological recall precision!"
                    "Twin Quiz Pioneer" -> "Splendid! You have successfully completed your first digital twin personalized evaluation!"
                    "Quiz Champion" -> "A master of twin-designed assessments! Complete 5 quizzes to secure your ultimate retention credentials!"
                    else -> "Every milestone unlocked strengthens your learning index. Keep studying and mastering concepts!"
                }

                AlertDialog(
                    onDismissRequest = { selectedBadgeForDetail = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(badge.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (badge.isUnlocked) badge.icon else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = badge.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Milestone Progress",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = progressText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (badge.isUnlocked) badge.color else MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                LinearProgressIndicator(
                                    progress = currentProgress,
                                    color = badge.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Neuro-Scientific Insight",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = motivationalQuote,
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { selectedBadgeForDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = if (badge.isUnlocked) badge.color else MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("close_badge_detail_btn")
                        ) {
                            Text("Got it")
                        }
                    }
                )
            }
        }

        // Study Twin Simulation Panel (Visible only to Admin/Instructor role)
        if (profile?.role == "Admin" || profile?.role == "Instructor") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_simulation_panel")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Simulation Sandbox",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Neuro-Study Simulation Sandbox",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Instructor & Admin control suite for diagnostic review",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "To verify dynamic badge transitions, trigger quick simulation triggers below to instantly satisfy achievement requirements:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.updateStreak(7) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sim_streak_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("7-Day Streak", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = { viewModel.simulateMasteredCards(100) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sim_mastery_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("100 Cards", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.simulateGamification(10, 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sim_game_lvl1_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lvl 1 Milestones", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = { viewModel.simulateGamification(50, 5) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sim_game_lvl2_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lvl 2 Milestones", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings Section
        item {
            Text(
                text = "Preferences & System Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Study reminders toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Reminders", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Daily Study Notifications", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = studyReminders,
                            onCheckedChange = { studyReminders = it },
                            modifier = Modifier.testTag("notification_toggle_switch")
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Diagnostic Assessment Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.OnboardingSetup) }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Redo Diagnostic", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Reconfigure Study Twin", style = MaterialTheme.typography.bodyLarge)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Sign Out Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.logout() }
                            .padding(vertical = 16.dp)
                            .testTag("logout_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Database Reset Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showResetDialog = true }
                            .padding(vertical = 16.dp)
                            .testTag("reset_db_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Reset Database", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Factory Reset Database",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Reset Database Confirm Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Study Twin?") },
            text = { Text("This operation is irreversible. Your streak, XP, concept progress, flashcards, and chat history will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileBadgeItem(badge: BadgeData, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) badge.color.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (badge.isUnlocked) badge.color.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("profile_badge_${badge.title.replace(" ", "_").lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (badge.isUnlocked) badge.color.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (badge.isUnlocked) badge.icon else Icons.Default.Lock,
                    contentDescription = badge.title,
                    tint = if (badge.isUnlocked) badge.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = badge.description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
