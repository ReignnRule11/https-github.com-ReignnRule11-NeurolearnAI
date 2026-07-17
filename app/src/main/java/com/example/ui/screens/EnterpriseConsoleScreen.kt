package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.*
import com.example.ui.MainViewModel
import com.example.ui.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseConsoleScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Bind state from our Enterprise Backend gateway flow managers
    val activeTenant by EnterpriseBackend.activeTenant.collectAsState()
    val activeRole by EnterpriseBackend.activeRole.collectAsState()
    val activeLlmConfig by EnterpriseBackend.activeLlmConfig.collectAsState()
    val auditTrail by EnterpriseBackend.auditTrail.collectAsState()
    val logEvents by EnterpriseBackend.logEvents.collectAsState()
    val perfMetrics by EnterpriseBackend.perfMetrics.collectAsState()
    val conflicts by EnterpriseBackend.conflicts.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Tenancy & Roles", "AI Orchestration", "Offline Sync", "Security & Logs")
    
    // Simulate interactive loading or execution states
    var isSyncing by remember { mutableStateOf(false) }
    val selectedSyncStrategy by EnterpriseBackend.syncStrategy.collectAsState()
    var mockLlmPromptInput by remember { mutableStateOf("Design an enterprise course overview for organic chemistry.") }
    var mockLlmResponseOutput by remember { mutableStateOf("") }
    var isMockLlmRunning by remember { mutableStateOf(false) }

    // Dynamic brand color adaptation (Multi-tenant visual isolation)
    val tenantPrimary = activeTenant.primaryColor
    val tenantSecondary = activeTenant.secondaryColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Enterprise Control Center",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Multi-Tenant Gateway • Role-Based Policies",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("enterprise_console_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Learning Hub"
                        )
                    }
                },
                actions = {
                    // System Security Status Indicator
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Active",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AES-256 SECURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
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
        ) {
            // Active Tenant Banner (Visual Adaptation based on active branding colors)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = tenantPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, tenantPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(tenantPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Tenant Emblem",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeTenant.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = tenantPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Active Role: ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeRole.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = tenantSecondary
                            )
                        }
                    }

                    // Tenant Compliance Badge
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = activeTenant.complianceStandard,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = tenantPrimary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("enterprise_tab_$index")
                    )
                }
            }

            // Main Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> TenantAndRbacTab(
                        activeTenant = activeTenant,
                        activeRole = activeRole,
                        tenantPrimary = tenantPrimary,
                        tenantSecondary = tenantSecondary
                    )
                    1 -> LlmOrchestrationTab(
                        activeLlmConfig = activeLlmConfig,
                        tenantPrimary = tenantPrimary,
                        tenantSecondary = tenantSecondary,
                        mockLlmPromptInput = mockLlmPromptInput,
                        onPromptChange = { mockLlmPromptInput = it },
                        mockLlmResponseOutput = mockLlmResponseOutput,
                        isMockLlmRunning = isMockLlmRunning,
                        onExecutePrompt = {
                            isMockLlmRunning = true
                            coroutineScope.launch {
                                try {
                                    val reply = EnterpriseBackend.routeLlmRequest(mockLlmPromptInput)
                                    mockLlmResponseOutput = reply
                                } catch (e: Exception) {
                                    mockLlmResponseOutput = "Routing error: ${e.message}"
                                } finally {
                                    isMockLlmRunning = false
                                }
                            }
                        }
                    )
                    2 -> OfflineSyncTab(
                        conflicts = conflicts,
                        isSyncing = isSyncing,
                        perfMetrics = perfMetrics,
                        selectedStrategy = selectedSyncStrategy,
                        tenantPrimary = tenantPrimary,
                        tenantSecondary = tenantSecondary,
                        onStrategyChange = { EnterpriseBackend.setSyncStrategy(it) },
                        onTriggerSync = {
                            isSyncing = true
                            coroutineScope.launch {
                                EnterpriseBackend.performSynchronize(context, selectedSyncStrategy)
                                isSyncing = false
                            }
                        }
                    )
                    3 -> SecurityLogsTab(
                        auditTrail = auditTrail,
                        logEvents = logEvents,
                        perfMetrics = perfMetrics,
                        tenantPrimary = tenantPrimary,
                        tenantSecondary = tenantSecondary
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 1: TENANCY & ROLE MANAGEMENT
// ==========================================
@Composable
fun TenantAndRbacTab(
    activeTenant: Tenant,
    activeRole: UserRole,
    tenantPrimary: Color,
    tenantSecondary: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-Tenant Selector
        item {
            Text(
                text = "Multi-Tenant Institution Selector",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Each tenant operates within an isolated secure database boundary, dynamic API usage budgets, and localized branding presets.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EnterpriseBackend.tenants.forEach { tenant ->
                    val isSelected = tenant.id == activeTenant.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { EnterpriseBackend.setTenant(tenant.id) }
                            .testTag("tenant_card_${tenant.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tenant.primaryColor.copy(alpha = 0.08f) 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) tenant.primaryColor else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(tenant.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tenant.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Domain constraints: ${tenant.domains.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active Tenant",
                                    tint = tenant.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // RBAC Context Selector
        item {
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Role-Based Access Control (RBAC)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Simulate and verify policy-enforced user profiles. Different security roles restrict or permit database views and system commands.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.values().forEach { role ->
                    val isSelected = role == activeRole
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { EnterpriseBackend.setRole(role) }
                            .testTag("role_card_${role.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tenantSecondary.copy(alpha = 0.08f) 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) tenantSecondary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = when (role) {
                                        UserRole.STUDENT -> Icons.Default.School
                                        UserRole.TEACHER -> Icons.Default.SupervisorAccount
                                        UserRole.PARENT -> Icons.Default.FamilyRestroom
                                        UserRole.SCHOOL_ADMIN -> Icons.Default.CorporateFare
                                        UserRole.RECRUITER -> Icons.Default.Work
                                        UserRole.SUPER_ADMIN -> Icons.Default.Shield
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) tenantSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = role.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Surface(
                                        color = tenantSecondary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "AUTHORIZED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = role.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Active Policy Matrix
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Keys",
                            tint = tenantPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RBAC Policy Clearance Matrix",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Permission.values().forEach { permission ->
                        val isGranted = EnterpriseBackend.hasPermission(permission)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = permission.code,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = permission.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isGranted) "GRANTED" else "DENIED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: AI MULTI-PROVIDER ORCHESTRATION
// ==========================================
@Composable
fun LlmOrchestrationTab(
    activeLlmConfig: LlmRouteConfig,
    tenantPrimary: Color,
    tenantSecondary: Color,
    mockLlmPromptInput: String,
    onPromptChange: (String) -> Unit,
    mockLlmResponseOutput: String,
    isMockLlmRunning: Boolean,
    onExecutePrompt: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-LLM Routing Policies
        item {
            Text(
                text = "Model Prompt Router & Cost Balancing",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Establish automatic routing rules to direct user inquiries based on cost constraints, latency limits, or local compliance requirements.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LlmProvider.values().forEach { provider ->
                    val isSelected = provider == activeLlmConfig.activeProvider
                    val costEstimate = when (provider) {
                        LlmProvider.GEMINI -> "$0.00015 / 1k t"
                        LlmProvider.OPENAI -> "$0.00150 / 1k t"
                        LlmProvider.ANTHROPIC -> "$0.00300 / 1k t"
                        LlmProvider.COHERE -> "$0.00100 / 1k t"
                        LlmProvider.LOCAL_SECURE -> "$0.00000 (Offline)"
                    }
                    val latencyClass = when (provider) {
                        LlmProvider.GEMINI -> "Ultra-Low"
                        LlmProvider.OPENAI -> "Medium"
                        LlmProvider.ANTHROPIC -> "High Accuracy"
                        LlmProvider.COHERE -> "Low Latency"
                        LlmProvider.LOCAL_SECURE -> "Instant (Cached)"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { EnterpriseBackend.setLlmProvider(provider) }
                            .testTag("provider_card_${provider.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tenantPrimary.copy(alpha = 0.08f) 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) tenantPrimary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (provider) {
                                    LlmProvider.LOCAL_SECURE -> Icons.Default.Storage
                                    else -> Icons.Default.Psychology
                                },
                                contentDescription = null,
                                tint = if (isSelected) tenantPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = provider.endpointUrl,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = costEstimate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tenantSecondary
                                )
                                Text(
                                    text = latencyClass,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Testing Playground (Uses the Orchestration Router API)
        item {
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Simulate Federated API Endpoint Routing",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Enter a study concept and click send. The request will pass through the active tenant container, log the transactional cost against their budget, and execute utilizing the provider chosen above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = mockLlmPromptInput,
                onValueChange = onPromptChange,
                label = { Text("Mock LLM System Prompt Input") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("orchestrate_prompt_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onExecutePrompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("orchestrate_prompt_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = tenantPrimary),
                enabled = !isMockLlmRunning
            ) {
                if (isMockLlmRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Submit")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proxy Route Prompt to API Gateway")
                    }
                }
            }

            if (mockLlmResponseOutput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Reply",
                                tint = tenantSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Orchestrated Model Response",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mockLlmResponseOutput,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: OFFLINE-FIRST DB SYNCHRONIZATION
// ==========================================
@Composable
fun OfflineSyncTab(
    conflicts: List<SyncConflict>,
    isSyncing: Boolean,
    perfMetrics: PerformanceMetrics,
    selectedStrategy: SyncConflictStrategy,
    tenantPrimary: Color,
    tenantSecondary: Color,
    onStrategyChange: (SyncConflictStrategy) -> Unit,
    onTriggerSync: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Strategy Toggles
        item {
            Text(
                text = "Offline-First Synchronization Core",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Data updates occur locally first. During connectivity recovery, the client synchronizes local Room delta packages to the cloud database. Select the resolution policy to enact when overlapping conflicts arise.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncConflictStrategy.values().forEach { strategy ->
                    val isSelected = strategy == selectedStrategy
                    val label = when (strategy) {
                        SyncConflictStrategy.CLIENT_WINS -> "Client State Wins Override (Local Priority)"
                        SyncConflictStrategy.SERVER_WINS -> "Remote Cloud State Wins Override (Server Priority)"
                        SyncConflictStrategy.SMART_MERGE_AI -> "AI Smart Merge (Synthesize Overlapping Deltas)"
                    }
                    val description = when (strategy) {
                        SyncConflictStrategy.CLIENT_WINS -> "Forces the central cloud to accept our mobile SQLite database state even if timestamps are older."
                        SyncConflictStrategy.SERVER_WINS -> "Ignores local edits if a newer timestamp update is recorded on the server."
                        SyncConflictStrategy.SMART_MERGE_AI -> "Analyzes conflicting fields with LLM context parsing, merging local logs with remote logs cleanly."
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStrategyChange(strategy) }
                            .testTag("sync_strategy_card_${strategy.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tenantSecondary.copy(alpha = 0.08f) 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) tenantSecondary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) tenantSecondary else MaterialTheme.colorScheme.outlineVariant)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Trigger Sync Action
        item {
            Button(
                onClick = onTriggerSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trigger_sync_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = tenantPrimary),
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Bidirectional DB Synchronization")
                    }
                }
            }
        }

        // Conflict Resolver Logs
        if (conflicts.isNotEmpty()) {
            item {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Conflict", tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sync Conflict Resolutions Ledger",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    conflicts.forEach { conflict ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Table: ${conflict.tableName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tenantPrimary
                                    )
                                    Text(
                                        text = "Entity ID: ${conflict.entityId}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("LOCAL CLIENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tenantSecondary)
                                        Text(conflict.clientValue, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("REMOTE SERVER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tenantSecondary)
                                        Text(conflict.serverValue, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = tenantPrimary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "RESOLVED STATE (${conflict.resolvedStrategy?.name})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tenantPrimary
                                        )
                                        Text(
                                            text = conflict.resolvedValue ?: "Awaiting resolution",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
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
}

// ==========================================
// TAB 4: SECURITY & OBSERVABILITY LOGS
// ==========================================
@Composable
fun SecurityLogsTab(
    auditTrail: List<AuditLogEntry>,
    logEvents: List<LogEvent>,
    perfMetrics: PerformanceMetrics,
    tenantPrimary: Color,
    tenantSecondary: Color
) {
    var logsDisplayMode by remember { mutableStateOf(0) } // 0 = Audit Trail, 1 = Structured Event Logs
    val df = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Observability Metrics
        item {
            Text(
                text = "Performance & Telemetry Observability",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: API Latency
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("API Latency", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${perfMetrics.apiLatencyMs}ms", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tenantPrimary)
                    }
                }
                // Metric 2: Sync Speed
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sync SyncTime", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${perfMetrics.syncDurationMs}ms", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tenantSecondary)
                    }
                }
                // Metric 3: DB Speed
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DB Write", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${perfMetrics.databaseWriteMs}ms", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }
        }

        // Logs Segment Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { logsDisplayMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (logsDisplayMode == 0) tenantPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (logsDisplayMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("toggle_audit_ledger_btn")
                ) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = "Receipt", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Secure Audit Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { logsDisplayMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (logsDisplayMode == 1) tenantPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (logsDisplayMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("toggle_structured_logs_btn")
                ) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = "Console", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Structured Log Stream", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Dynamic Lists
        if (logsDisplayMode == 0) {
            // Cryptographic SHA-256 Audit Ledger
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "Ledger", tint = tenantPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tamper-Proof Compliance Ledger",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "A chain of cryptographic hashes logging all security critical operations (role switches, tenant modifications, sync boundaries). If any record is altered, the SHA-256 hash validation instantly invalidates downstream entries.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            }

            items(auditTrail) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = tenantPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = entry.action,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tenantPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.role.displayName,
                                    fontSize = 10.sp,
                                    color = tenantSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = df.format(Date(entry.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entry.details,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "Ledger Chain Hash (SHA-256):",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tenantPrimary
                                )
                                Text(
                                    text = entry.sha256ChainHash,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Structured Log Stream
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = "Logs", tint = tenantSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Structured Events Server Log",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(logEvents) { event ->
                val levelColor = when (event.level) {
                    LogLevel.INFO -> Color(0xFF3B82F6)
                    LogLevel.WARNING -> Color(0xFFF59E0B)
                    LogLevel.ERROR -> Color(0xFFEF4444)
                    LogLevel.AUDIT -> Color(0xFF10B981)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = df.format(Date(event.timestamp)),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(75.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = levelColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.width(60.dp)
                    ) {
                        Text(
                            text = event.level.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${event.component}] ${event.message}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
