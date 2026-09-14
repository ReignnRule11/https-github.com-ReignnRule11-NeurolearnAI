package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    // Validation states
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    // Loading & Google Picker States
    val isAILoading by viewModel.isAILoading.collectAsState()
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var showCustomGoogleInput by remember { mutableStateOf(false) }
    var customGoogleEmail by remember { mutableStateOf("") }
    var customGoogleName by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Base background colors with a subtle modern gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .testTag("login_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful branding / logo header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "NeuroLearn Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "NeuroLearn AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Your personalized digital learning twin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Text(
                text = "Local device session only. Email, password, and Google buttons do not contact a remote identity provider.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_form_card")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon")
                        },
                        isError = emailError != null,
                        supportingText = {
                            emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Password Icon")
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = {
                            passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    // Sign In/Up Action Button
                    Button(
                        onClick = {
                            // Quick offline Validation
                            var hasError = false
                            if (email.isBlank() || !email.contains("@")) {
                                emailError = "Please enter a valid email address."
                                hasError = true
                            }
                            if (password.length < 6) {
                                passwordError = "Password must be at least 6 characters."
                                hasError = true
                            }

                            if (!hasError) {
                                viewModel.loginWithEmail(email, password, isSignUp) { success ->
                                    if (!success) {
                                        // Authentication failure handled by viewmodel toast
                                    }
                                }
                            }
                        },
                        enabled = !isAILoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isAILoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Register & Start Studying" else "Sign In",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Toggle between Sign In / Sign Up
                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            emailError = null
                            passwordError = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "New to NeuroLearn? Register now",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Or Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "OR CONTINUE WITH",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Perfectly Branded Google Sign-In Button (compliant with Google design standards)
            Card(
                onClick = { showGoogleAccountPicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_login_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google multi-colored 'G' logo vector emulation
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing G with colored canvas circles/arcs
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                            // Draw an elegant colorful G indicator
                            drawCircle(color = Color(0xFF4285F4), radius = size.minDimension / 2)
                        }
                        Text(
                            text = "G",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "Simulated Google Sign-in",
                        color = Color(0xFF1F1F1F),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // --- IMMERSIVE SYSTEM GOOGLE SIGN IN BOTTOM SHEET EMULATION ---
        if (showGoogleAccountPicker) {
            // Clickable background shadow overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showGoogleAccountPicker = false }
            )

            // Account selection sheet slides up from the bottom
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(enabled = false) {} // block clicks passing through
                        .testTag("google_account_picker_sheet")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Small handle indicator at top
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Google Logo branding header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color(0xFF4285F4),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "o",
                                color = Color(0xFFEA4335),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "o",
                                color = Color(0xFFFBBC05),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "g",
                                color = Color(0xFF4285F4),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "l",
                                color = Color(0xFF34A853),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "e",
                                color = Color(0xFFEA4335),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Choose an account",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F1F1F)
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "to continue to NeuroLearn AI",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5F6368)),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        // Accounts List
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Account 1: User's real email
                            GoogleAccountRow(
                                name = "Alex",
                                email = "jusreal2@gmail.com",
                                letter = 'A',
                                circleColor = Color(0xFF1A73E8)
                            ) {
                                showGoogleAccountPicker = false
                                viewModel.loginWithGoogle("jusreal2@gmail.com", "Alex") { _ -> }
                            }

                            Divider(color = Color(0xFFF1F3F4), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                            // Account 2: Demo/alternative account
                            GoogleAccountRow(
                                name = "Learner Test",
                                email = "learner.test@gmail.com",
                                letter = 'L',
                                circleColor = Color(0xFFE91E63)
                            ) {
                                showGoogleAccountPicker = false
                                viewModel.loginWithGoogle("learner.test@gmail.com", "Learner Test") { _ -> }
                            }

                            Divider(color = Color(0xFFF1F3F4), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                            // Custom account picker option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showCustomGoogleInput = true
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F3F4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add account",
                                        tint = Color(0xFF5F6368),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Use another account",
                                    color = Color(0xFF1F1F1F),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Consent/disclosure footer matching Google standard
                        Text(
                            text = "To create your NeuroLearn profile, Google will share your name, email address, and profile picture with NeuroLearn AI. Review NeuroLearn's Privacy Policy and Terms of Service.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF70757A),
                                lineHeight = 16.sp
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // --- CUSTOM GOOGLE ACCOUNT INPUT DIALOG ---
        if (showCustomGoogleInput) {
            AlertDialog(
                onDismissRequest = { showCustomGoogleInput = false },
                title = { Text("Sign in with another Google Account") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customGoogleName,
                            onValueChange = { customGoogleName = it },
                            label = { Text("Your Full Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = customGoogleEmail,
                            onValueChange = { customGoogleEmail = it },
                            label = { Text("Google Email Address") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customGoogleEmail.isNotBlank() && customGoogleEmail.contains("@")) {
                                showCustomGoogleInput = false
                                showGoogleAccountPicker = false
                                val finalName = customGoogleName.ifBlank { customGoogleEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
                                viewModel.loginWithGoogle(customGoogleEmail, finalName) { _ -> }
                            } else {
                                viewModel.showToast("Please enter a valid Google email.")
                            }
                        }
                    ) {
                        Text("Sign In")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomGoogleInput = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GoogleAccountRow(
    name: String,
    email: String,
    letter: Char,
    circleColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter.toString(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color(0xFF1F1F1F),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = email,
                color = Color(0xFF5F6368),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Select Account",
            tint = Color(0xFF9AA0A6)
        )
    }
}
