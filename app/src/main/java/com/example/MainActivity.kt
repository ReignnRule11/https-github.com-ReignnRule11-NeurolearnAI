package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NeuroLearnAppShell()
            }
        }
    }
}

@Composable
fun NeuroLearnAppShell() {
    val viewModel: MainViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMessage by viewModel.uiToast.collectAsState()
    val context = LocalContext.current

    // Display native android toasts when requested by the ViewModel
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Determine if bottom navigation is shown on the current screen
    val showBottomBar = when (currentScreen) {
        Screen.Home, Screen.Learn, Screen.Review, Screen.Progress, Screen.Profile -> true
        else -> false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_tab_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Learn,
                        onClick = { viewModel.navigateTo(Screen.Learn) },
                        icon = { Icon(imageVector = Icons.Default.Hub, contentDescription = "Learn") },
                        label = { Text("Learn") },
                        modifier = Modifier.testTag("nav_tab_learn")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Review,
                        onClick = { viewModel.navigateTo(Screen.Review) },
                        icon = { Icon(imageVector = Icons.Default.Alarm, contentDescription = "Review") },
                        label = { Text("Review") },
                        modifier = Modifier.testTag("nav_tab_review")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Progress,
                        onClick = { viewModel.navigateTo(Screen.Progress) },
                        icon = { Icon(imageVector = Icons.Default.Face, contentDescription = "Progress") },
                        label = { Text("Progress") },
                        modifier = Modifier.testTag("nav_tab_progress")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Profile,
                        onClick = { viewModel.navigateTo(Screen.Profile) },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_navigation_animation"
            ) { screen ->
                when (screen) {
                    Screen.Login -> LoginScreen(viewModel = viewModel)
                    Screen.OnboardingWelcome -> OnboardingWelcomeScreen(viewModel = viewModel)
                    Screen.OnboardingSetup -> OnboardingSetupScreen(viewModel = viewModel)
                    Screen.DiagnosticQuiz -> DiagnosticQuizScreen(viewModel = viewModel)
                    Screen.Home -> HomeScreen(viewModel = viewModel)
                    Screen.Learn -> LearnScreen(viewModel = viewModel)
                    Screen.Review -> ReviewScreen(viewModel = viewModel)
                    Screen.Progress -> ProgressScreen(viewModel = viewModel)
                    Screen.Profile -> ProfileScreen(viewModel = viewModel)
                    is Screen.TutorChat -> TutorScreen(viewModel = viewModel, conceptId = screen.conceptId)
                    is Screen.PdfIntelligence -> PdfIntelligenceScreen(viewModel = viewModel, conceptId = screen.conceptId ?: "limits")
                    is Screen.QuizGame -> QuizScreen(viewModel = viewModel, conceptId = screen.conceptId, difficulty = screen.difficulty)
                }
            }
        }
    }
}
