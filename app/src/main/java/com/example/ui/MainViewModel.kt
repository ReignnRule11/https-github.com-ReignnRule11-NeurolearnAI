package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// --- UI Navigation & Screen States ---

sealed interface MindMapState {
    object Idle : MindMapState
    object Loading : MindMapState
    data class Success(val graph: MindMapGraph) : MindMapState
    data class Error(val message: String) : MindMapState
}

sealed interface Screen {
    object Login : Screen
    object OnboardingWelcome : Screen
    object OnboardingSetup : Screen
    object DiagnosticQuiz : Screen
    object Home : Screen
    object Learn : Screen
    object Review : Screen
    object Progress : Screen
    object Profile : Screen
    data class TutorChat(val conceptId: String? = null, val deckId: String? = null) : Screen
    data class PdfIntelligence(val conceptId: String? = null) : Screen
    data class QuizGame(val conceptId: String, val difficulty: String) : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    private val database = AppDatabase.getDatabase(application)
    
    // DAOs
    private val profileDao = database.learnerProfileDao()
    private val conceptDao = database.conceptMasteryDao()
    private val flashcardDao = database.flashcardDao()
    private val chatDao = database.chatMessageDao()
    private val taskDao = database.studyTaskDao()
    private val deckDao = database.flashcardDeckDao()

    // --- State Flows ---
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.OnboardingWelcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val profile: StateFlow<LearnerProfile?> = profileDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allConcepts: StateFlow<List<ConceptMastery>> = conceptDao.getAllConcepts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueFlashcards: StateFlow<List<Flashcard>> = flashcardDao.getDueCards(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFlashcards: StateFlow<List<Flashcard>> = flashcardDao.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDecks: StateFlow<List<FlashcardDeck>> = deckDao.getAllDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyTasks: StateFlow<List<StudyTask>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state parameters
    private val _activeChatSession = MutableStateFlow("session_default")
    val activeChatSession: StateFlow<String> = _activeChatSession.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatSession
        .flatMapLatest { sessionId -> chatDao.getMessagesForSession(sessionId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _uiToast = MutableStateFlow<String?>(null)
    val uiToast: StateFlow<String?> = _uiToast.asStateFlow()

    // Diagnostic Assessment questions state
    private val _diagnosticQuestions = MutableStateFlow<List<DiagnosticQuestion>>(emptyList())
    val diagnosticQuestions: StateFlow<List<DiagnosticQuestion>> = _diagnosticQuestions.asStateFlow()

    private val _currentDiagnosticIndex = MutableStateFlow(0)
    val currentDiagnosticIndex: StateFlow<Int> = _currentDiagnosticIndex.asStateFlow()

    private val _diagnosticCorrectCount = MutableStateFlow(0)
    val diagnosticCorrectCount: StateFlow<Int> = _diagnosticCorrectCount.asStateFlow()

    // Generated Note Data
    private val _generatedNotes = MutableStateFlow<ProcessedNoteData?>(null)
    val generatedNotes: StateFlow<ProcessedNoteData?> = _generatedNotes.asStateFlow()

    // --- Mind Map State ---
    private val _mindMapState = MutableStateFlow<MindMapState>(MindMapState.Idle)
    val mindMapState: StateFlow<MindMapState> = _mindMapState.asStateFlow()

    // Active Quiz State
    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val activeQuizQuestions: StateFlow<List<QuizQuestion>> = _activeQuizQuestions.asStateFlow()

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _isQuizFinished = MutableStateFlow(false)
    val isQuizFinished: StateFlow<Boolean> = _isQuizFinished.asStateFlow()

    private val _aiPlannerAdvice = MutableStateFlow<String?>(null)
    val aiPlannerAdvice: StateFlow<String?> = _aiPlannerAdvice.asStateFlow()

    // Active Recall Session Summary
    private val _activeRecallSummary = MutableStateFlow<String?>(null)
    val activeRecallSummary: StateFlow<String?> = _activeRecallSummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    fun clearActiveRecallSummary() {
        _activeRecallSummary.value = null
    }

    fun generateActiveRecallSessionSummary(deckName: String, results: List<FlashcardRatingResult>) {
        if (results.isEmpty()) return
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            _activeRecallSummary.value = null
            try {
                val summary = GeminiClient.generateActiveRecallSummary(deckName, results)
                _activeRecallSummary.value = summary
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating session summary", e)
                showToast("Failed to generate summary: ${e.message}")
            } finally {
                _isGeneratingSummary.value = false
            }
        }
    }

    private val _activeStudyAlert = MutableStateFlow<StudyTask?>(null)
    val activeStudyAlert: StateFlow<StudyTask?> = _activeStudyAlert.asStateFlow()

    private val notifiedTaskIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    init {
        // Evaluate if user is logged in and has completed onboarding
        viewModelScope.launch {
            val userProfile = profileDao.getProfileSync()
            if (userProfile == null || !userProfile.isLoggedIn) {
                _currentScreen.value = Screen.Login
            } else if (userProfile.diagnosticScore > 0f) {
                _currentScreen.value = Screen.Home
            } else {
                _currentScreen.value = Screen.OnboardingWelcome
            }
        }
        startScheduledTaskChecker()
    }

    fun loginWithEmail(email: String, password: String, isSignUp: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                // Perform quick validation
                if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
                    showToast("Please enter a valid email address.")
                    onResult(false)
                    return@launch
                }
                if (password.length < 6) {
                    showToast("Password must be at least 6 characters.")
                    onResult(false)
                    return@launch
                }

                val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
                val updatedProfile = if (isSignUp) {
                    // Sign Up: Register new email
                    val defaultName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    currentProfile.copy(
                        name = defaultName,
                        email = email,
                        isLoggedIn = true
                    )
                } else {
                    // Sign In: Log in existing
                    currentProfile.copy(
                        email = email,
                        isLoggedIn = true
                    )
                }

                profileDao.insertOrUpdateProfile(updatedProfile)
                showToast(if (isSignUp) "Registered & Logged in successfully!" else "Signed in successfully!")
                
                // Navigate
                if (updatedProfile.diagnosticScore > 0f) {
                    navigateTo(Screen.Home)
                } else {
                    navigateTo(Screen.OnboardingWelcome)
                }
                onResult(true)
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                showToast("Authentication failed: ${e.message}")
                onResult(false)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun loginWithGoogle(email: String, name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
                val updatedProfile = currentProfile.copy(
                    name = name,
                    email = email,
                    isLoggedIn = true
                )
                profileDao.insertOrUpdateProfile(updatedProfile)
                showToast("Signed in with Google as $name!")
                
                // Navigate
                if (updatedProfile.diagnosticScore > 0f) {
                    navigateTo(Screen.Home)
                } else {
                    navigateTo(Screen.OnboardingWelcome)
                }
                onResult(true)
            } catch (e: Exception) {
                Log.e(TAG, "Google Login failed", e)
                showToast("Google authentication failed.")
                onResult(false)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val currentProfile = profileDao.getProfileSync()
                if (currentProfile != null) {
                    profileDao.insertOrUpdateProfile(currentProfile.copy(isLoggedIn = false))
                }
                showToast("Logged out successfully.")
                navigateTo(Screen.Login)
            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun showToast(message: String) {
        _uiToast.value = message
    }

    fun clearToast() {
        _uiToast.value = null
    }

    // --- Onboarding & Diagnostic Logic ---

    fun startDiagnostic(track: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            _currentDiagnosticIndex.value = 0
            _diagnosticCorrectCount.value = 0
            
            val systemPrompt = "You are the Assessment Agent of NeuroLearn AI, an expert exam designer."
            val prompt = """
                Generate a set of 3 diagnostic assessment multiple choice questions for a high school / college student starting a study track in "$track".
                The output format must be a JSON array. Each object in the array should represent a question with:
                - "question": string
                - "options": list of 4 strings
                - "correctAnswer": string matching one of the options
                - "explanation": string explaining the correct answer conceptually
                
                Keep response in this exact JSON structure enclosed between JSON_START and JSON_END tags. Do not output conversational text or markdown code blocks.
            """.trimIndent()

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                val parsedQuestions = parseDiagnosticJson(response)
                if (parsedQuestions.isNotEmpty()) {
                    _diagnosticQuestions.value = parsedQuestions
                    navigateTo(Screen.DiagnosticQuiz)
                } else {
                    showToast("Failed to generate custom diagnostic. Loading default assessment.")
                    loadDefaultDiagnostic()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating diagnostic", e)
                loadDefaultDiagnostic()
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun loadDefaultDiagnostic() {
        // Populate fallback local questions
        val defaultQuestions = listOf(
            DiagnosticQuestion(
                question = "What is the rate of change of the position of an object called?",
                options = listOf("Acceleration", "Velocity", "Momentum", "Inertia"),
                correctAnswer = "Velocity",
                explanation = "Velocity is defined as the first derivative of position with respect to time, representing the rate of change of position."
            ),
            DiagnosticQuestion(
                question = "In Computer Science, what does a 'loop' allow us to do?",
                options = listOf("Store data in files", "Execute a block of code repeatedly", "Define variable scopes", "Compile machine instructions"),
                correctAnswer = "Execute a block of code repeatedly",
                explanation = "Loops (like for, while) repeatedly execute a specific instruction block while a condition remains true."
            ),
            DiagnosticQuestion(
                question = "Which particle in an atom carries a positive charge?",
                options = listOf("Electron", "Neutron", "Proton", "Positron"),
                correctAnswer = "Proton",
                explanation = "Protons have a positive electric charge (+1e), electrons have negative, and neutrons have zero charge."
            )
        )
        _diagnosticQuestions.value = defaultQuestions
        navigateTo(Screen.DiagnosticQuiz)
    }

    fun submitDiagnosticAnswer(selectedOption: String) {
        val currentQuestion = _diagnosticQuestions.value.getOrNull(_currentDiagnosticIndex.value)
        if (currentQuestion != null) {
            if (selectedOption == currentQuestion.correctAnswer) {
                _diagnosticCorrectCount.value += 1
            }
        }

        val nextIndex = _currentDiagnosticIndex.value + 1
        if (nextIndex < _diagnosticQuestions.value.size) {
            _currentDiagnosticIndex.value = nextIndex
        } else {
            // Diagnostic finished! Create Digital Learning Twin and Personalized Study Plan
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            val correct = _diagnosticCorrectCount.value.toFloat()
            val total = _diagnosticQuestions.value.size.toFloat()
            val score = if (total > 0) correct / total else 0.5f

            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val updatedProfile = currentProfile.copy(
                diagnosticScore = score,
                xp = currentProfile.xp + 50, // Award onboarding bonus!
                level = 1,
                streak = 1,
                lastActiveDate = System.currentTimeMillis()
            )
            profileDao.insertOrUpdateProfile(updatedProfile)

            // Personalize the concept masteries based on diagnostic score
            val concepts = allConcepts.value
            if (concepts.isNotEmpty()) {
                val updatedConcepts = concepts.map { concept ->
                    val multiplier = if (concept.difficulty == "Easy") 0.8f else if (concept.difficulty == "Medium") 0.4f else 0.1f
                    val baseline = score * multiplier
                    concept.copy(
                        understandingScore = baseline.coerceIn(0f, 1f),
                        retentionScore = (baseline * 0.9f).coerceIn(0f, 1f),
                        confidenceScore = (baseline * 0.8f).coerceIn(0f, 1f),
                        predictedExamPerformance = baseline.coerceIn(0f, 1f),
                        lastReviewed = System.currentTimeMillis(),
                        nextReviewDate = System.currentTimeMillis() + (1L * 24 * 60 * 60 * 1000)
                    )
                }
                conceptDao.insertAllConcepts(updatedConcepts)
            }

            // Create customized study tasks
            generateStudyPlannerTasks(updatedProfile)

            showToast("Digital Learning Twin generated successfully! +50 XP")
            navigateTo(Screen.Home)
        }
    }

    fun saveProfileGoals(
        name: String,
        goals: String,
        curriculum: String,
        subjects: String,
        targetExam: String,
        studyTime: Int,
        learningStyle: String
    ) {
        viewModelScope.launch {
            val existing = profileDao.getProfileSync() ?: LearnerProfile()
            val updated = existing.copy(
                name = name.ifBlank { "Learner" },
                learningGoals = goals.ifBlank { "Master core concepts" },
                curriculum = curriculum,
                subjects = subjects,
                targetExam = targetExam,
                availableStudyTime = studyTime,
                learningStyle = learningStyle
            )
            profileDao.insertOrUpdateProfile(updated)
            startDiagnostic(subjects)
        }
    }

    // --- Knowledge Graph & Mastery Engine ---

    /**
     * Propagates mastery adjustments back down to prerequisites (if mastery decreases or increases)
     * and updates dependent concept states.
     */
    private suspend fun propagateMastery(conceptId: String, delta: Float) {
        val concept = conceptDao.getConceptById(conceptId) ?: return
        
        // 1. Update target concept scores
        val newUnderstanding = (concept.understandingScore + delta).coerceIn(0.0f, 1.0f)
        val newRetention = (concept.retentionScore + (delta * 0.9f)).coerceIn(0.0f, 1.0f)
        val newConfidence = (concept.confidenceScore + (delta * 1.1f)).coerceIn(0.0f, 1.0f)
        val newPredicted = ((newUnderstanding + newRetention + newConfidence) / 3f).coerceIn(0.0f, 1.0f)
        
        conceptDao.updateScores(
            id = conceptId,
            understanding = newUnderstanding,
            retention = newRetention,
            confidence = newConfidence,
            predicted = newPredicted,
            timestamp = System.currentTimeMillis(),
            nextReview = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000) // schedule next review in 3 days
        )

        // 2. Mastery propagation to prerequisites
        if (concept.prerequisites.isNotEmpty()) {
            val prereqIds = concept.prerequisites.split(",").map { it.trim() }
            for (prereqId in prereqIds) {
                if (prereqId.isNotEmpty()) {
                    val prereqConcept = conceptDao.getConceptById(prereqId)
                    if (prereqConcept != null) {
                        // Prerequisites get a smaller echo of the delta (mastering advanced topics reinforces foundations)
                        val echoDelta = delta * 0.35f
                        val pUnd = (prereqConcept.understandingScore + echoDelta).coerceIn(0f, 1f)
                        val pRet = (prereqConcept.retentionScore + (echoDelta * 0.9f)).coerceIn(0f, 1f)
                        val pConf = (prereqConcept.confidenceScore + (echoDelta * 1.1f)).coerceIn(0f, 1f)
                        val pPred = ((pUnd + pRet + pConf) / 3f).coerceIn(0f, 1f)
                        
                        conceptDao.updateScores(
                            id = prereqId,
                            understanding = pUnd,
                            retention = pRet,
                            confidence = pConf,
                            predicted = pPred,
                            timestamp = System.currentTimeMillis(),
                            nextReview = prereqConcept.nextReviewDate
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks if a concept's prerequisites are met.
     * Returns true if all prerequisites have an understanding score >= 0.5f, or if there are no prerequisites.
     */
    fun checkPrerequisitesMet(concept: ConceptMastery): PrerequisiteCheckResult {
        if (concept.prerequisites.isEmpty()) return PrerequisiteCheckResult(true, emptyList())
        
        val prereqIds = concept.prerequisites.split(",").map { it.trim() }
        val unmet = mutableListOf<String>()
        
        for (prereqId in prereqIds) {
            if (prereqId.isNotEmpty()) {
                val prereq = allConcepts.value.find { it.id == prereqId }
                if (prereq != null && prereq.understandingScore < 0.5f) {
                    unmet.add(prereq.name)
                }
            }
        }
        
        return PrerequisiteCheckResult(unmet.isEmpty(), unmet)
    }

    // --- Study Planner task generation ---

    suspend fun generateStudyPlannerTasks(profile: LearnerProfile) {
        val concepts = allConcepts.value
        if (concepts.isEmpty()) return

        taskDao.clearAllTasks()

        // Select weak concepts (understanding < 0.6f) or items that haven't been reviewed
        val sortedConcepts = concepts.sortedWith(compareBy<ConceptMastery> { it.understandingScore }.thenBy { it.lastReviewed })
        val taskCount = when (profile.availableStudyTime) {
            in 0..20 -> 1
            in 21..44 -> 2
            else -> 3
        }

        val selectedConcepts = sortedConcepts.take(taskCount)
        val studyTasks = selectedConcepts.mapIndexed { index, concept ->
            StudyTask(
                conceptId = concept.id,
                conceptName = concept.name,
                subject = concept.subject,
                dueDate = System.currentTimeMillis() + (index * 60 * 1000), // staggering a bit
                isCompleted = false,
                xpAwarded = if (concept.difficulty == "Hard") 25 else 15
            )
        }
        taskDao.insertAllTasks(studyTasks)
    }

    fun toggleTaskCompletion(task: StudyTask) {
        viewModelScope.launch {
            val newStatus = !task.isCompleted
            taskDao.updateTaskStatus(task.id, newStatus)
            
            // Sync status to Firestore
            firestore?.collection("study_tasks")?.document(task.id.toString())?.update("isCompleted", newStatus)
            
            if (newStatus) {
                awardXp(task.xpAwarded)
                // Propagate a micro increase in mastery for studying the concept
                propagateMastery(task.conceptId, 0.05f)
                showToast("Task completed! +${task.xpAwarded} XP")
            } else {
                deductXp(task.xpAwarded)
                propagateMastery(task.conceptId, -0.05f)
            }
        }
    }

    // --- Intelligent Study Scheduler & Firestore Integration ---

    fun generateIntelligentStudySchedulerPlan() {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val profileVal = profile.value ?: LearnerProfile()
                val decksList = allDecks.value
                val cardsList = allFlashcards.value
                val concepts = allConcepts.value

                // 1. Clear current study tasks from local DB
                taskDao.clearAllTasks()

                // 2. Query deck review statuses
                val decksWithDueCounts = decksList.map { deck ->
                    val dueCount = cardsList.count { it.deckId == deck.id && it.nextReviewDate <= System.currentTimeMillis() }
                    val totalCount = cardsList.count { it.deckId == deck.id }
                    Triple(deck, dueCount, totalCount)
                }

                val studyTasks = mutableListOf<StudyTask>()
                var taskIndex = 0

                // Add tasks for decks that have due cards
                decksWithDueCounts.forEach { (deck, dueCount, totalCount) ->
                    if (dueCount > 0) {
                        val taskName = "Review Deck '${deck.name}' ($dueCount cards due)"
                        val task = StudyTask(
                            conceptId = "deck_${deck.id}",
                            conceptName = taskName,
                            subject = "Spaced Recall",
                            dueDate = System.currentTimeMillis() + (taskIndex * 60 * 1000),
                            isCompleted = false,
                            xpAwarded = 10 + (dueCount * 2).coerceAtMost(30),
                            deckId = deck.id,
                            taskType = "deck"
                        )
                        studyTasks.add(task)
                        taskIndex++
                    }
                }

                // If no decks have due cards, suggest studying/learning, or add standard tasks
                if (studyTasks.isEmpty()) {
                    decksList.forEach { deck ->
                        val cardCount = cardsList.count { it.deckId == deck.id }
                        val taskName = if (cardCount == 0) {
                            "Add flashcards to deck '${deck.name}'"
                        } else {
                            "Study deck '${deck.name}' (All caught up! ✨)"
                        }
                        val task = StudyTask(
                            conceptId = "deck_${deck.id}",
                            conceptName = taskName,
                            subject = "Continuous Learning",
                            dueDate = System.currentTimeMillis() + (taskIndex * 60 * 1000),
                            isCompleted = false,
                            xpAwarded = 15,
                            deckId = deck.id,
                            taskType = "deck"
                        )
                        studyTasks.add(task)
                        taskIndex++
                    }
                }

                // Fill up with weak concepts if available
                val weakConcepts = concepts.filter { it.understandingScore < 0.6f }.take(2)
                weakConcepts.forEach { concept ->
                    val task = StudyTask(
                        conceptId = concept.id,
                        conceptName = "Study gap: ${concept.name}",
                        subject = concept.subject,
                        dueDate = System.currentTimeMillis() + (taskIndex * 60 * 1000),
                        isCompleted = false,
                        xpAwarded = if (concept.difficulty == "Hard") 25 else 15,
                        deckId = null,
                        taskType = "concept"
                    )
                    studyTasks.add(task)
                    taskIndex++
                }

                // 3. Save tasks to local Room Database
                taskDao.insertAllTasks(studyTasks)

                // 4. Generate Socratic Twin advisor explanation using Gemini!
                val dueDecksSummary = decksWithDueCounts.filter { it.second > 0 }
                val prompt = """
                    As the Socratic Study Twin Agent, analyze this study status:
                    - Learner: ${profileVal.name}
                    - Study Goal: ${profileVal.learningGoals}
                    - Available Daily Time: ${profileVal.availableStudyTime} mins
                    - Flashcard Decks status:
                    ${decksWithDueCounts.joinToString("\n") { "  * Deck '${it.first.name}': ${it.second} cards due out of ${it.third} total." }}
                    - Weak Concept Gaps:
                    ${weakConcepts.joinToString("\n") { "  * ${it.name}: ${(it.understandingScore * 100).toInt()}% understanding" }}

                    Provide an actionable 2-3 paragraph study schedule advisor response:
                    - Paragraph 1: Analyze memory decay in their flashcard decks and state which deck needs most immediate active recall attention.
                    - Paragraph 2: Map out how they should allocate their ${profileVal.availableStudyTime} minutes today (e.g. Pomodoro intervals between due decks and concepts) for peak retention.
                    - Paragraph 3: A brief, wise, and Socratic encouraging word from their study twin.

                    Keep the response highly strategic, warm, professional, and do not use markdown lists. Just write clean, cohesive paragraphs.
                """.trimIndent()

                try {
                    val response = GeminiClient.generate(prompt, "You are a warm, wise, and highly analytical Socratic Study Twin.")
                    _aiPlannerAdvice.value = response
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch intelligent study plan advice from Gemini", e)
                    val mainTarget = dueDecksSummary.maxByOrNull { it.second }?.first?.name ?: "your active decks"
                    _aiPlannerAdvice.value = "Memory decay analysis suggests starting with the '$mainTarget' deck which has the most pending card reviews. Set a 15-minute timer for intense active recall, then spend 10 minutes filling the conceptual gaps detected in your weak learning modules."
                }

                // 5. Save/Sync all study tasks to Firestore cloud collection "study_tasks"
                syncStudyTasksToFirestore()

                showToast("Intelligent Study Plan Generated & Synced to Firestore!")
            } catch (e: Exception) {
                Log.e(TAG, "Scheduler plan generation failed", e)
                showToast("Scheduler error: ${e.message}")
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun syncStudyTasksToFirestore() {
        val db = firestore
        if (db == null) {
            Log.w("neurolearn", "Firestore is not configured. Saved study plan locally.")
            return
        }
        viewModelScope.launch {
            try {
                val currentTasks = taskDao.getAllTasks().first()
                for (task in currentTasks) {
                    val taskMap = hashMapOf(
                        "id" to task.id,
                        "conceptId" to task.conceptId,
                        "conceptName" to task.conceptName,
                        "subject" to task.subject,
                        "dueDate" to task.dueDate,
                        "isCompleted" to task.isCompleted,
                        "xpAwarded" to task.xpAwarded,
                        "deckId" to task.deckId,
                        "taskType" to task.taskType
                    )
                    db.collection("study_tasks").document(task.id.toString()).set(taskMap)
                }
                Log.d("neurolearn", "Synced study tasks to Firestore successfully!")
            } catch (e: Exception) {
                Log.e("neurolearn", "Failed to sync study tasks to Firestore", e)
            }
        }
    }

    fun pullStudyTasksFromFirestore() {
        val db = firestore
        if (db == null) {
            Log.w("neurolearn", "Firestore not available to pull study tasks.")
            return
        }
        viewModelScope.launch {
            try {
                db.collection("study_tasks").get().addOnSuccessListener { snapshot ->
                    viewModelScope.launch {
                        if (snapshot != null && !snapshot.isEmpty) {
                            taskDao.clearAllTasks()
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val conceptId = doc.getString("conceptId") ?: ""
                                val conceptName = doc.getString("conceptName") ?: ""
                                val subject = doc.getString("subject") ?: ""
                                val dueDate = doc.getLong("dueDate") ?: System.currentTimeMillis()
                                val isCompleted = doc.getBoolean("isCompleted") ?: false
                                val xpAwarded = doc.getLong("xpAwarded")?.toInt() ?: 15
                                val deckId = doc.getString("deckId")
                                val taskType = doc.getString("taskType") ?: "concept"

                                taskDao.insertTask(
                                    StudyTask(
                                        id = id,
                                        conceptId = conceptId,
                                        conceptName = conceptName,
                                        subject = subject,
                                        dueDate = dueDate,
                                        isCompleted = isCompleted,
                                        xpAwarded = xpAwarded,
                                        deckId = deckId,
                                        taskType = taskType
                                    )
                                )
                            }
                            Log.d("neurolearn", "Pulled study tasks from Firestore successfully!")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("neurolearn", "Failed to pull study tasks from Firestore", e)
            }
        }
    }

    fun fetchAIPlannerAdvice() {
        viewModelScope.launch {
            _isAILoading.value = true
            val concepts = allConcepts.value
            val profileVal = profile.value ?: LearnerProfile()
            
            val weakConcepts = concepts.filter { it.understandingScore < 0.6f }.take(3).joinToString { "${it.name} (${(it.understandingScore*100).toInt()}% understanding)" }
            val strongConcepts = concepts.filter { it.understandingScore >= 0.8f }.take(3).joinToString { "${it.name} (${(it.understandingScore*100).toInt()}% understanding)" }
            
            val prompt = """
                As the NeuroLearn Intelligent Planner Agent, analyze this learner profile and mastery data:
                - Name: ${profileVal.name}
                - Subjects: ${profileVal.subjects}
                - Available Study Time: ${profileVal.availableStudyTime} mins/day
                - Target Exam: ${profileVal.targetExam} (in 28 days)
                - Weak Concepts: ${weakConcepts.ifEmpty { "None" }}
                - Strong Concepts: ${strongConcepts.ifEmpty { "None" }}
                
                Provide a concise, 2-paragraph actionable study plan and tips:
                - Paragraph 1: Strategic focus for the week based on their weakness.
                - Paragraph 2: Smart advice on how to split their ${profileVal.availableStudyTime} minutes today for peak retention.
                
                Keep it ultra-professional, direct, and encouraging. Do not use markdown bullet points, just clean paragraphs.
            """.trimIndent()
            
            try {
                val response = GeminiClient.generate(prompt, "You are a professional study planner assistant.")
                _aiPlannerAdvice.value = response
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get planner advice", e)
                _aiPlannerAdvice.value = "To study efficiently today, prioritize your weaker concepts like ${concepts.firstOrNull { it.understandingScore < 0.6f }?.name ?: "foundations"}. Focus on 20-minute active recall intervals followed by quick spaced repetition cards."
            } finally {
                _isAILoading.value = false
            }
        }
    }

    fun updateAvailableStudyTime(minutes: Int) {
        viewModelScope.launch {
            val existing = profileDao.getProfileSync() ?: return@launch
            val updated = existing.copy(availableStudyTime = minutes)
            profileDao.insertOrUpdateProfile(updated)
            // Auto-regenerate planner tasks to match the new study time
            generateStudyPlannerTasks(updated)
        }
    }

    fun updateSelectedTwinAvatar(avatar: String) {
        viewModelScope.launch {
            val existing = profileDao.getProfileSync() ?: return@launch
            val updated = existing.copy(selectedTwinAvatar = avatar)
            profileDao.insertOrUpdateProfile(updated)
            showToast("Digital Twin customized: ${avatar.replaceFirstChar { it.uppercase() }} active!")
        }
    }

    fun addConceptToStudyPlan(concept: ConceptMastery) {
        viewModelScope.launch {
            val alreadyExists = studyTasks.value.any { it.conceptId == concept.id && !it.isCompleted }
            if (alreadyExists) {
                showToast("\"${concept.name}\" is already in your study plan!")
                return@launch
            }
            val task = StudyTask(
                conceptId = concept.id,
                conceptName = concept.name,
                subject = concept.subject,
                dueDate = System.currentTimeMillis(),
                isCompleted = false,
                xpAwarded = if (concept.difficulty == "Hard") 25 else 15
            )
            taskDao.insertTask(task)
            showToast("Added \"${concept.name}\" to today's study plan!")
        }
    }

    fun removeTaskFromStudyPlan(taskId: Int) {
        viewModelScope.launch {
            taskDao.deleteTaskById(taskId)
            showToast("Removed task from today's study plan.")
        }
    }

    private fun startScheduledTaskChecker() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // check every 5 seconds
                val now = System.currentTimeMillis()
                val tasks = studyTasks.value
                val dueIncompleteTask = tasks.find { !it.isCompleted && it.dueDate <= now && it.id !in notifiedTaskIds }
                if (dueIncompleteTask != null) {
                    notifiedTaskIds.add(dueIncompleteTask.id)
                    _activeStudyAlert.value = dueIncompleteTask
                    triggerSystemNotification(dueIncompleteTask)
                }
            }
        }
    }

    private fun triggerSystemNotification(task: StudyTask) {
        try {
            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channelId = "scheduled_study_sessions"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Scheduled Study Sessions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies when it's time for a scheduled study session"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Open the app when clicked
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // simple standard built-in alarm icon
                .setContentTitle("⏰ Study Session Due!")
                .setContentText(task.conceptName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                
            notificationManager.notify(task.id, builder.build())
            
            // Also show a toast so it's super visible
            showToast("⏰ Study Alarm: It is time to study \"${task.conceptName}\"!")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to trigger system notification", e)
        }
    }

    fun dismissActiveStudyAlert() {
        _activeStudyAlert.value = null
    }

    fun scheduleStudySession(conceptName: String, subject: String, minutesFromNow: Int) {
        viewModelScope.launch {
            val task = StudyTask(
                conceptId = "custom_${UUID.randomUUID().toString().take(6)}",
                conceptName = conceptName,
                subject = subject,
                dueDate = System.currentTimeMillis() + (minutesFromNow * 60L * 1000L),
                isCompleted = false,
                xpAwarded = 20,
                taskType = "custom"
            )
            taskDao.insertTask(task)
            syncStudyTasksToFirestore()
            showToast("Scheduled '$conceptName' study session in $minutesFromNow minutes!")
        }
    }

    // --- Gamification System ---

    fun awardXp(amount: Int) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: return@launch
            var newXp = currentProfile.xp + amount
            var newLevel = currentProfile.level
            
            // Level up threshold increases: level 1: 100XP, level 2: 250XP, level 3: 500XP, etc.
            var nextLevelXp = getXpThresholdForLevel(newLevel)
            while (newXp >= nextLevelXp) {
                newXp -= nextLevelXp
                newLevel += 1
                nextLevelXp = getXpThresholdForLevel(newLevel)
                showToast("Level Up! You reached Level $newLevel! 🎉")
            }

            profileDao.insertOrUpdateProfile(currentProfile.copy(xp = newXp, level = newLevel))
        }
    }

    private fun deductXp(amount: Int) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: return@launch
            val newXp = (currentProfile.xp - amount).coerceAtLeast(0)
            profileDao.insertOrUpdateProfile(currentProfile.copy(xp = newXp))
        }
    }

    fun getXpThresholdForLevel(level: Int): Int {
        return level * 120
    }

    // --- Spaced Repetition (SM-2 Algorithm) ---

    fun rateFlashcard(card: Flashcard, rating: Int) {
        viewModelScope.launch {
            // rating: 1 = Hard, 2 = Good, 3 = Easy
            val repetitions = if (rating == 1) 0 else card.repetitions + 1
            val easeFactor = if (rating == 1) {
                (card.easeFactor - 0.25f).coerceAtLeast(1.3f)
            } else if (rating == 3) {
                card.easeFactor + 0.15f
            } else {
                card.easeFactor
            }

            val intervalDays = when (repetitions) {
                0, 1 -> 1
                2 -> 3
                else -> (card.intervalDays * easeFactor).toInt().coerceAtLeast(4)
            }

            val nextReview = System.currentTimeMillis() + (intervalDays * 24L * 60 * 60 * 1000)
            val updatedCard = card.copy(
                repetitions = repetitions,
                easeFactor = easeFactor,
                intervalDays = intervalDays,
                nextReviewDate = nextReview,
                lastReviewed = System.currentTimeMillis()
            )

            flashcardDao.insertCard(updatedCard)
            awardXp(8)
            
            // Enhance the concept confidence slightly because of flashcard recall
            val concept = conceptDao.getConceptById(card.conceptId)
            if (concept != null) {
                val scoreMultiplier = when (rating) {
                    1 -> -0.05f
                    2 -> 0.05f
                    else -> 0.12f
                }
                propagateMastery(concept.id, scoreMultiplier)
            }
            showToast("Flashcard rated! +8 XP")
            syncDecksAndCardsToFirestore()
        }
    }

    // --- Flashcard Decks & Firestore Sync ---

    val firestore: com.google.firebase.firestore.FirebaseFirestore? by lazy {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("neurolearn", "Firestore not available or google-services.json missing", e)
            null
        }
    }

    fun createDeck(name: String, description: String, subject: String = "General") {
        viewModelScope.launch {
            val deck = FlashcardDeck(name = name, description = description, subject = subject)
            deckDao.insertDeck(deck)
            awardXp(15)
            showToast("Deck '$name' created locally!")
            
            // Sync to Firestore
            firestore?.let { db ->
                db.collection("decks").document(deck.id)
                    .set(deck)
                    .addOnSuccessListener {
                        Log.d("neurolearn", "Successfully synced deck ${deck.name} to Firestore!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("neurolearn", "Failed to sync deck to Firestore", e)
                    }
            }
        }
    }

    fun generateMindMapForDeck(deckName: String, deckId: String?) {
        viewModelScope.launch {
            _mindMapState.value = MindMapState.Loading
            try {
                val cards = allFlashcards.value.let { list ->
                    if (deckId != null) list.filter { it.deckId == deckId } else list
                }
                val result = GeminiClient.generateMindMap(deckName, cards)
                _mindMapState.value = MindMapState.Success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate mind map", e)
                _mindMapState.value = MindMapState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearMindMap() {
        _mindMapState.value = MindMapState.Idle
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            deckDao.deleteDeck(deckId)
            // also delete or update cards of this deck
            val cards = flashcardDao.getAllCards().first()
            for (card in cards) {
                if (card.deckId == deckId) {
                    flashcardDao.deleteCard(card.id)
                    firestore?.collection("flashcards")?.document(card.id.toString())?.delete()
                }
            }
            firestore?.collection("decks")?.document(deckId)?.delete()
            showToast("Deck and its cards deleted!")
        }
    }

    fun deleteFlashcard(cardId: Int) {
        viewModelScope.launch {
            flashcardDao.deleteCard(cardId)
            firestore?.collection("flashcards")?.document(cardId.toString())?.delete()
            showToast("Flashcard deleted!")
        }
    }

    fun addCustomFlashcard(deckId: String, question: String, answer: String, difficulty: String = "Medium", tags: String = "") {
        viewModelScope.launch {
            val card = Flashcard(
                conceptId = "custom",
                question = question,
                answer = answer,
                difficulty = difficulty,
                deckId = deckId,
                tags = tags
            )
            flashcardDao.insertCard(card)
            awardXp(5)
            showToast("Flashcard added!")

            syncDecksAndCardsToFirestore()
        }
    }

    fun syncDecksAndCardsToFirestore() {
        val db = firestore
        if (db == null) {
            Log.w("neurolearn", "Firestore is not configured. Saved locally.")
            return
        }
        
        viewModelScope.launch {
            try {
                val decksList = deckDao.getAllDecks().first()
                val cardsList = flashcardDao.getAllCards().first()
                
                for (deck in decksList) {
                    db.collection("decks").document(deck.id).set(deck)
                }
                
                for (card in cardsList) {
                    db.collection("flashcards").document(card.id.toString()).set(card)
                }
                Log.d("neurolearn", "Synced all decks and cards to Firestore successfully!")
                
                // Automatically sync study tasks too
                syncStudyTasksToFirestore()
            } catch (e: Exception) {
                Log.e("neurolearn", "Sync to Firestore failed", e)
            }
        }
    }

    fun pullDecksAndCardsFromFirestore() {
        val db = firestore
        if (db == null) {
            showToast("Cloud sync failed: Firestore is not configured.")
            return
        }
        
        viewModelScope.launch {
            try {
                db.collection("decks").get().addOnSuccessListener { decksSnapshot ->
                    viewModelScope.launch {
                        for (doc in decksSnapshot.documents) {
                            val id = doc.getString("id") ?: continue
                            val name = doc.getString("name") ?: ""
                            val description = doc.getString("description") ?: ""
                            val subject = doc.getString("subject") ?: "General"
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            deckDao.insertDeck(FlashcardDeck(id, name, description, subject, createdAt))
                        }
                    }
                }.addOnFailureListener { e ->
                    showToast("Pull decks failed: ${e.message}")
                }
                
                db.collection("flashcards").get().addOnSuccessListener { cardsSnapshot ->
                    viewModelScope.launch {
                        for (doc in cardsSnapshot.documents) {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val conceptId = doc.getString("conceptId") ?: ""
                            val question = doc.getString("question") ?: ""
                            val answer = doc.getString("answer") ?: ""
                            val difficulty = doc.getString("difficulty") ?: "Medium"
                            val intervalDays = doc.getLong("intervalDays")?.toInt() ?: 1
                            val easeFactor = doc.getDouble("easeFactor")?.toFloat() ?: 2.5f
                            val repetitions = doc.getLong("repetitions")?.toInt() ?: 0
                            val nextReviewDate = doc.getLong("nextReviewDate") ?: System.currentTimeMillis()
                            val lastReviewed = doc.getLong("lastReviewed") ?: 0L
                            val deckId = doc.getString("deckId") ?: "default"
                            val tags = doc.getString("tags") ?: ""
                            
                            flashcardDao.insertCard(
                                Flashcard(
                                    id = id,
                                    conceptId = conceptId,
                                    question = question,
                                    answer = answer,
                                    difficulty = difficulty,
                                    intervalDays = intervalDays,
                                    easeFactor = easeFactor,
                                    repetitions = repetitions,
                                    nextReviewDate = nextReviewDate,
                                    lastReviewed = lastReviewed,
                                    deckId = deckId,
                                    tags = tags
                                )
                            )
                        }
                        
                        // Automatically pull study tasks too
                        pullStudyTasksFromFirestore()
                        
                        showToast("Successfully synced from Firestore Cloud!")
                    }
                }.addOnFailureListener { e ->
                    showToast("Pull cards failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("neurolearn", "Pull from Firestore failed", e)
                showToast("Pull failed: ${e.message}")
            }
        }
    }

    fun rateConceptSpacedRepetition(conceptId: String, quality: Int) {
        viewModelScope.launch {
            val concept = conceptDao.getConceptById(conceptId) ?: return@launch
            
            // quality values:
            // 1 = Forgot/Complete Blank, 2 = Incorrect but familiar, 3 = Got it with serious difficulty, 4 = Good with minor hesitation, 5 = Perfect / instant recall
            
            // 1. Calculate new mastery scores based on the recall quality
            val isSuccess = quality >= 3
            val delta = when (quality) {
                1 -> -0.15f
                2 -> -0.08f
                3 -> 0.04f
                4 -> 0.09f
                5 -> 0.15f
                else -> 0.0f
            }
            
            val newUnderstanding = (concept.understandingScore + delta).coerceIn(if (isSuccess) 0.5f else 0.0f, 1.0f)
            val newConfidence = (concept.confidenceScore + delta * 1.2f).coerceIn(if (isSuccess) 0.5f else 0.0f, 1.0f)
            val newRetention = (concept.retentionScore + delta * 0.9f).coerceIn(if (isSuccess) 0.5f else 0.0f, 1.0f)
            val newPredicted = ((newUnderstanding + newConfidence + newRetention) / 3f).coerceIn(0.0f, 1.0f)
            
            // 2. SM-2 calculation for next interval in days
            // We model an Ease Factor (EF) based on the calculated understanding
            val ef = (1.3f + 1.2f * newUnderstanding).coerceIn(1.3f, 2.5f)
            
            val baseIntervalDays = when (quality) {
                1 -> 1
                2 -> 1
                3 -> 3
                4 -> 7
                5 -> 14
                else -> 1
            }
            
            // Scale base interval based on the ease factor and overall mastery multiplier
            val masteryMultiplier = 1.0f + (newUnderstanding * 0.5f) + (newConfidence * 0.5f)
            val finalIntervalDays = (baseIntervalDays * ef * masteryMultiplier).toInt().coerceIn(1, 45)
            
            val nextReviewDate = System.currentTimeMillis() + (finalIntervalDays * 24L * 60 * 60 * 1000)
            
            conceptDao.updateScores(
                id = conceptId,
                understanding = newUnderstanding,
                retention = newRetention,
                confidence = newConfidence,
                predicted = newPredicted,
                timestamp = System.currentTimeMillis(),
                nextReview = nextReviewDate
            )
            
            // 3. Propagate mastery changes down to prerequisite concepts to keep stats synced
            propagateMastery(conceptId, delta * 0.5f)
            
            // Award XP
            val xpAward = 10 + (quality * 4)
            awardXp(xpAward)
            
            showToast("Review recorded! +$xpAward XP. Next review in $finalIntervalDays days.")
        }
    }

    // --- AI Tutor Screen Operations ---

    fun startTutorSession(conceptId: String) {
        _activeChatSession.value = "session_$conceptId"
        navigateTo(Screen.TutorChat(conceptId = conceptId))

        // Preseed a welcoming tutor prompt if chat history is empty
        viewModelScope.launch {
            val existing = chatDao.getMessagesForSession("session_$conceptId").firstOrNull()
            if (existing.isNullOrEmpty()) {
                val concept = conceptDao.getConceptById(conceptId) ?: return@launch
                val tutorPrompt = """
                    Welcome, Learner! I am your NeuroLearn AI Tutor, personalized for your Digital Learning Twin. 
                    Let's master the concept of **${concept.name}** in ${concept.subject}.
                    
                    I suggest we start with a quick question to see what you already know, or would you prefer a conceptual analogy first?
                """.trimIndent()
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = "session_$conceptId",
                        role = "model",
                        text = tutorPrompt
                    )
                )
            }
        }
    }

    fun startDeckTutorSession(deckId: String) {
        _activeChatSession.value = "deck_session_$deckId"
        navigateTo(Screen.TutorChat(deckId = deckId))

        // Preseed a welcoming tutor prompt if chat history is empty
        viewModelScope.launch {
            val existing = chatDao.getMessagesForSession("deck_session_$deckId").firstOrNull()
            if (existing.isNullOrEmpty()) {
                val decks = deckDao.getAllDecks().firstOrNull() ?: emptyList()
                val deck = decks.find { it.id == deckId } ?: return@launch
                val cards = flashcardDao.getAllCards().firstOrNull() ?: emptyList()
                val deckCards = cards.filter { it.deckId == deckId }
                val cardsSummary = if (deckCards.isNotEmpty()) {
                    "This deck contains ${deckCards.size} flashcards. Here are some key questions in this deck: " + 
                    deckCards.take(4).joinToString(", ") { "'${it.question}'" } + (if (deckCards.size > 4) " and others." else ".")
                } else {
                    "This deck is currently empty. You can add flashcards to study, or we can discuss and create some together right now!"
                }
                
                val tutorPrompt = """
                    Welcome, Learner! I am your Socratic AI Digital Twin tutor, here to guide you through your flashcard deck **${deck.name}**. 
                    
                    $cardsSummary
                    
                    I am here to quiz you, explain any of these flashcards with custom analogies, or help you understand the core material. What would you like to focus on first?
                """.trimIndent()
                
                chatDao.insertMessage(
                    ChatMessage(
                        sessionId = "deck_session_$deckId",
                        role = "model",
                        text = tutorPrompt
                    )
                )
            }
        }
    }

    fun sendMessageToTutor(conceptId: String?, deckId: String?, userText: String) {
        if (userText.isBlank()) return
        val sessionId = if (deckId != null) "deck_session_$deckId" else "session_$conceptId"

        viewModelScope.launch {
            // Save user message
            chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "user", text = userText))
            _isAILoading.value = true

            val userProfile = profileDao.getProfileSync() ?: LearnerProfile()

            // Fetch chat history for context
            val history = chatDao.getMessagesForSession(sessionId).first().takeLast(6)
            val historyContext = history.joinToString("\n") { "${it.role}: ${it.text}" }

            val twinPersona = when (userProfile.selectedTwinAvatar) {
                "tech" -> "You are 'The Tech Visionary' digital learning twin. Your tone is futuristic, precise, and highly analytical. Focus on structured definitions, algorithmic/logical reasoning, and technical formulas."
                "scholar" -> "You are 'The Scholar Academic' digital learning twin. Your tone is classical, deep, and academically rigorous. Focus on historical foundations, deep theoretical insights, and formal academic explanations."
                "creative" -> "You are 'The Creative Innovator' digital learning twin. Your tone is highly visual, playful, and energetic. Focus on rich analogies, real-world comparative metaphors, and engaging thought experiments."
                else -> "You are 'The Socratic Mentor' digital learning twin. Your tone is thoughtful, reflective, and guided by questioning. Lead the student to answers using scaffolded hints and interactive dialogue instead of giving solutions outright."
            }

            val systemPrompt = if (deckId != null) {
                val decks = deckDao.getAllDecks().firstOrNull() ?: emptyList()
                val deck = decks.find { it.id == deckId }
                val cards = flashcardDao.getAllCards().firstOrNull() ?: emptyList()
                val deckCards = cards.filter { it.deckId == deckId }
                val cardsDetail = if (deckCards.isNotEmpty()) {
                    deckCards.joinToString("\n") { "- Q: ${it.question} | A: ${it.answer}" }
                } else {
                    "No cards in this deck yet."
                }
                
                """
                    $twinPersona
                    You are tutoring the student on their flashcard deck named "${deck?.name ?: "Flashcards"}".
                    Here are all the flashcards in this deck that you have full knowledge of:
                    $cardsDetail
                    
                    Your target student has a learning style of "${userProfile.learningStyle}" and target goals of "${userProfile.learningGoals}".
                    Your pedagogical principle: Help them master the deck. You can quiz them on these flashcards, explain the answers, provide real-world analogies, or guide them step-by-step through any questions they have.
                    Be warm, supportive, extremely precise, and interactive. Encourage active recall!
                """.trimIndent()
            } else {
                val concept = conceptDao.getConceptById(conceptId ?: "") ?: return@launch
                """
                    $twinPersona
                    You are tutoring the student on the concept "${concept.name}" (Subject: ${concept.subject}).
                    Your target student has a learning style of "${userProfile.learningStyle}" and target goals of "${userProfile.learningGoals}".
                    Your pedagogical principle: Teach before giving answers. Use active recall, step-by-step guidance, and analogies.
                    Be warm, supportive, and extremely precise.
                    
                    CRITICAL INSTRUCTION: If you feel the student has demonstrated good understanding of the concept or completed an exercise correctly, append the following tag to the very end of your response to update their Digital Learning Twin:
                    [MASTERY_DELTA: +8%, CONFIDENCE_DELTA: +10%]
                    If they are struggling or getting wrong answers, append:
                    [MASTERY_DELTA: -4%, CONFIDENCE_DELTA: -5%]
                    If it's just regular conversation or explanation, you don't need to append any delta.
                """.trimIndent()
            }

            val contextHeader = if (deckId != null) {
                val decks = deckDao.getAllDecks().firstOrNull() ?: emptyList()
                val deck = decks.find { it.id == deckId }
                "Current deck: ${deck?.name ?: "Flashcards"}"
            } else {
                val concept = conceptDao.getConceptById(conceptId ?: "")
                "Current topic: ${concept?.name ?: "Topic"}"
            }

            val prompt = """
                $contextHeader
                Student Profile: ${userProfile.subjects} | Diagnostic Level: ${userProfile.diagnosticScore}
                
                Conversation History:
                $historyContext
                
                Please generate your tutoring response.
            """.trimIndent()

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                
                // Parse out mastery or confidence deltas if present
                val cleanedResponse = if (conceptId != null) {
                    parseDeltasAndUpdateMastery(conceptId, response)
                } else {
                    response.trim()
                }
                
                chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "model", text = cleanedResponse))
                awardXp(5)
            } catch (e: Exception) {
                Log.e(TAG, "Tutor call failed", e)
                chatDao.insertMessage(ChatMessage(sessionId = sessionId, role = "model", text = "I am having trouble connecting to the brain center. Let's try again in a moment!"))
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private suspend fun parseDeltasAndUpdateMastery(conceptId: String, response: String): String {
        var cleanResponse = response
        var masteryDelta = 0.0f
        
        val masteryRegex = Regex("\\[MASTERY_DELTA:\\s*([+-]?\\d+)%\\]")
        val confidenceRegex = Regex("\\[CONFIDENCE_DELTA:\\s*([+-]?\\d+)%\\]")
        val compositeRegex = Regex("\\[MASTERY_DELTA:\\s*([+-]?\\d+)%,\\s*CONFIDENCE_DELTA:\\s*([+-]?\\d+)%\\]")

        compositeRegex.find(response)?.let { match ->
            val mVal = match.groupValues[1].toIntOrNull() ?: 0
            masteryDelta = mVal / 100.0f
            cleanResponse = cleanResponse.replace(match.value, "")
        } ?: run {
            masteryRegex.find(response)?.let { match ->
                val mVal = match.groupValues[1].toIntOrNull() ?: 0
                masteryDelta = mVal / 100.0f
                cleanResponse = cleanResponse.replace(match.value, "")
            }
            confidenceRegex.find(response)?.let { match ->
                cleanResponse = cleanResponse.replace(match.value, "")
            }
        }

        if (masteryDelta != 0.0f) {
            propagateMastery(conceptId, masteryDelta)
            showToast("Digital Learning Twin updated! Mastery change: ${(masteryDelta * 100).toInt()}%")
        }

        return cleanResponse.trim()
    }

    fun clearTutorChat(conceptId: String?, deckId: String?) {
        val sessionId = if (deckId != null) "deck_session_$deckId" else "session_$conceptId"
        viewModelScope.launch {
            chatDao.clearSession(sessionId)
            if (deckId != null) {
                startDeckTutorSession(deckId)
            } else if (conceptId != null) {
                startTutorSession(conceptId)
            }
        }
    }

    // --- PDF / Lecture Note Intelligence ---

    fun processLectureNotes(conceptId: String, notesText: String) {
        if (notesText.isBlank()) return

        viewModelScope.launch {
            _isAILoading.value = true
            val concept = conceptDao.getConceptById(conceptId) ?: return@launch

            val systemPrompt = "You are the PDF Intelligence Agent of NeuroLearn AI, a master material synthesizer."
            val prompt = """
                Extract deep study assets from the following lecture notes/text related to "${concept.name}":
                
                NOTES TEXT:
                $notesText
                
                You MUST synthesize the material and generate a JSON response with:
                - "summary": string (a comprehensive high-quality paragraph summary)
                - "formulas": list of strings (key formulas/equations or core rules)
                - "glossary": list of objects, each with "term" and "definition"
                - "flashcards": list of objects, each with "question" and "answer"
                - "quizzes": list of MCQ objects, each with "question", "options" (4 strings), "correctAnswer" (matching one option), and "explanation"
                
                Ensure the response is valid JSON enclosed exactly between JSON_START and JSON_END tags.
            """.trimIndent()

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                val parsedData = parseProcessedNotesJson(response)
                if (parsedData != null) {
                    _generatedNotes.value = parsedData

                    // Insert synthesized flashcards into the database!
                    val newCards = parsedData.flashcards.map { cardData ->
                        Flashcard(
                            conceptId = conceptId,
                            question = cardData.question,
                            answer = cardData.answer,
                            difficulty = "Medium"
                        )
                    }
                    flashcardDao.insertAllCards(newCards)

                    // Boost the concept's understanding metrics
                    propagateMastery(conceptId, 0.15f)
                    awardXp(35)
                    showToast("Synthesized ${newCards.size} new flashcards for Spaced Repetition! +35 XP")
                } else {
                    showToast("Failed to process lecture notes structure. Loading default notes mockup.")
                    loadDefaultNotesMockup(conceptId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notes", e)
                loadDefaultNotesMockup(conceptId)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun loadDefaultNotesMockup(conceptId: String) {
        // High quality fallback
        val conceptName = allConcepts.value.find { it.id == conceptId }?.name ?: "Topic"
        val mockData = ProcessedNoteData(
            summary = "This material synthesizes the core mathematical, algorithmic, or structural rules of $conceptName. By mapping the relationship of this concept to its neighboring subjects, we discover core formulas and definitions that are vital for passing AP and College exams.",
            formulas = listOf(
                "Key Principle: Always break down the concept into parts",
                "Rule of Three: Review, Active Recall, Teach",
                "O(1) Memory Constant limit rule"
            ),
            glossary = listOf(
                NoteGlossaryItem("Prerequisite Core", "The prior foundational concepts required for active retrieval"),
                NoteGlossaryItem("Spaced Repetition", "An efficient visual memory training technique that schedules review right before forgetting")
            ),
            flashcards = listOf(
                NoteFlashcard("What is the core takeaway of $conceptName?", "That mastery is achieved through small daily testing sessions."),
                NoteFlashcard("How does the Digital Learning Twin model learning?", "By tracking understanding, confidence, and retention continuously over time.")
            ),
            quizzes = listOf(
                QuizQuestion(
                    question = "Which learning technique guarantees the highest long-term retention?",
                    options = listOf("Re-reading the book", "Highlighter marking", "Active recall with spaced repetition", "Passive video lectures"),
                    correctAnswer = "Active recall with spaced repetition",
                    explanation = "Active recall strengthens neural pathways, and spacing reviews prevents cognitive decay."
                )
            )
        )
        _generatedNotes.value = mockData
        showToast("Loaded high-quality template study resources! +20 XP")
    }

    // --- Quiz Engine ---

    fun startQuiz(conceptId: String, difficulty: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            _currentQuizIndex.value = 0
            _quizScore.value = 0
            _isQuizFinished.value = false

            val concept = conceptDao.getConceptById(conceptId) ?: return@launch
            val systemPrompt = "You are the Assessment Agent of NeuroLearn AI, an expert exam designer."
            val prompt = """
                Generate a 3-question MCQ quiz on the concept "${concept.name}" in "${concept.subject}".
                Difficulty: $difficulty
                
                The output format must be a JSON array of objects. Each object in the array should represent a question with:
                - "question": string
                - "options": list of 4 strings
                - "correctAnswer": string matching one of the options
                - "explanation": string explaining the correct answer conceptually
                
                Keep response in this exact JSON structure enclosed between JSON_START and JSON_END tags.
            """.trimIndent()

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                val parsed = parseQuizQuestions(response)
                if (parsed.isNotEmpty()) {
                    _activeQuizQuestions.value = parsed
                    navigateTo(Screen.QuizGame(conceptId, difficulty))
                } else {
                    showToast("Failed to generate custom quiz. Loading preseed quiz.")
                    loadPreseedQuiz(conceptId, difficulty)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed", e)
                loadPreseedQuiz(conceptId, difficulty)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun loadPreseedQuiz(conceptId: String, difficulty: String) {
        val concept = allConcepts.value.find { it.id == conceptId }
        val conceptName = concept?.name ?: "Topic"
        val subjectName = concept?.subject ?: "STEM"
        
        val list = listOf(
            QuizQuestion(
                question = "How is mastery of ${'$'}conceptName measured in modern cognitive science?",
                options = listOf("Total hours spent highlighting notes", "Your ability to actively recall and explain the concept", "The speed at which you read the text", "Watching video tutorials multiple times"),
                correctAnswer = "Your ability to actively recall and explain the concept",
                explanation = "Active recall and teaching are active learning strategies that maximize synapse retrieval, while reading is passive."
            ),
            QuizQuestion(
                question = "If you are struggling with ${'$'}conceptName, what is the best pedagogical recommendation?",
                options = listOf("Review the prerequisite concepts to bridge the knowledge gap", "Skip the concept and try to learn advanced topics", "Give up and study another track", "Double the study hours without changing learning style"),
                correctAnswer = "Review the prerequisite concepts to bridge the knowledge gap",
                explanation = "A struggle in an advanced concept is almost always caused by a weak understanding of its prerequisite concepts."
            ),
            QuizQuestion(
                question = "What is the primary benefit of the NeuroLearn Digital Learning Twin?",
                options = listOf("It displays fancy charts without utility", "It tracks and predicts your forgetting curves to schedule timely reviews", "It completely automates your exams so you do not have to study", "It replaces the textbook with static files"),
                correctAnswer = "It tracks and predicts your forgetting curves to schedule timely reviews",
                explanation = "By modeling confidence, retention, and understanding, the Twin optimizes study sessions to guarantee mastery with minimal effort."
            )
        )
        _activeQuizQuestions.value = list
        navigateTo(Screen.QuizGame(conceptId, difficulty))
    }

    fun submitQuizAnswer(conceptId: String, selectedOption: String) {
        val currentQuestion = _activeQuizQuestions.value.getOrNull(_currentQuizIndex.value)
        if (currentQuestion != null) {
            if (selectedOption == currentQuestion.correctAnswer) {
                _quizScore.value += 1
            }
        }

        val nextIndex = _currentQuizIndex.value + 1
        if (nextIndex < _activeQuizQuestions.value.size) {
            _currentQuizIndex.value = nextIndex
        } else {
            // Quiz completed! Compute final performance and update concept mastery
            _isQuizFinished.value = true
            val correct = _quizScore.value
            val total = _activeQuizQuestions.value.size
            val ratio = correct.toFloat() / total.toFloat()

            viewModelScope.launch {
                // Adjust concept mastery
                val delta = if (ratio >= 0.6f) 0.15f else -0.10f
                propagateMastery(conceptId, delta)

                // Update Profile streak & award XP
                val xpGained = correct * 10 + 10
                awardXp(xpGained)

                val profileObj = profileDao.getProfileSync()
                if (profileObj != null) {
                    val lastActive = profileObj.lastActiveDate
                    val oneDayMs = 24L * 60 * 60 * 1000
                    val currentMs = System.currentTimeMillis()
                    
                    var newStreak = profileObj.streak
                    if (currentMs - lastActive in oneDayMs until (2 * oneDayMs)) {
                        newStreak += 1
                    } else if (currentMs - lastActive >= 2 * oneDayMs) {
                        newStreak = 1
                    }
                    
                    profileDao.insertOrUpdateProfile(
                        profileObj.copy(
                            streak = newStreak,
                            lastActiveDate = currentMs
                        )
                    )
                }

                // Refresh study tasks dynamically based on new scores!
                profileDao.getProfileSync()?.let { generateStudyPlannerTasks(it) }
            }
        }
    }

    // --- JSON Parsing Helpers ---

    private fun parseDiagnosticJson(jsonString: String): List<DiagnosticQuestion> {
        val list = mutableListOf<DiagnosticQuestion>()
        try {
            val jsonText = extractJsonBlock(jsonString)
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val q = obj.getString("question")
                val opts = obj.getJSONArray("options")
                val optList = mutableListOf<String>()
                for (j in 0 until opts.length()) {
                    optList.add(opts.getString(j))
                }
                val ans = obj.getString("correctAnswer")
                val exp = obj.optString("explanation", "")
                list.add(DiagnosticQuestion(q, optList, ans, exp))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing diagnostic JSON", e)
        }
        return list
    }

    private fun parseQuizQuestions(jsonString: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        try {
            val jsonText = extractJsonBlock(jsonString)
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val q = obj.getString("question")
                val opts = obj.getJSONArray("options")
                val optList = mutableListOf<String>()
                for (j in 0 until opts.length()) {
                    optList.add(opts.getString(j))
                }
                val ans = obj.getString("correctAnswer")
                val exp = obj.optString("explanation", "")
                list.add(QuizQuestion(q, optList, ans, exp))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing quiz questions", e)
        }
        return list
    }

    private fun parseProcessedNotesJson(jsonString: String): ProcessedNoteData? {
        try {
            val jsonText = extractJsonBlock(jsonString)
            val obj = JSONObject(jsonText)
            val summary = obj.getString("summary")
            
            val formulasArr = obj.getJSONArray("formulas")
            val formulas = mutableListOf<String>()
            for (i in 0 until formulasArr.length()) {
                formulas.add(formulasArr.getString(i))
            }

            val glossaryArr = obj.getJSONArray("glossary")
            val glossary = mutableListOf<NoteGlossaryItem>()
            for (i in 0 until glossaryArr.length()) {
                val gObj = glossaryArr.getJSONObject(i)
                glossary.add(NoteGlossaryItem(gObj.getString("term"), gObj.getString("definition")))
            }

            val flashcardsArr = obj.getJSONArray("flashcards")
            val flashcards = mutableListOf<NoteFlashcard>()
            for (i in 0 until flashcardsArr.length()) {
                val fObj = flashcardsArr.getJSONObject(i)
                flashcards.add(NoteFlashcard(fObj.getString("question"), fObj.getString("answer")))
            }

            val quizzesArr = obj.getJSONArray("quizzes")
            val quizzes = mutableListOf<QuizQuestion>()
            for (i in 0 until quizzesArr.length()) {
                val qObj = quizzesArr.getJSONObject(i)
                val qOpts = qObj.getJSONArray("options")
                val qOptList = mutableListOf<String>()
                for (j in 0 until qOpts.length()) {
                    qOptList.add(qOpts.getString(j))
                }
                quizzes.add(
                    QuizQuestion(
                        question = qObj.getString("question"),
                        options = qOptList,
                        correctAnswer = qObj.getString("correctAnswer"),
                        explanation = qObj.optString("explanation", "")
                    )
                )
            }

            return ProcessedNoteData(summary, formulas, glossary, flashcards, quizzes)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing processed notes JSON", e)
            return null
        }
    }

    private fun extractJsonBlock(text: String): String {
        if (text.contains("JSON_START") && text.contains("JSON_END")) {
            val start = text.indexOf("JSON_START") + "JSON_START".length
            val end = text.indexOf("JSON_END")
            return text.substring(start, end).trim()
        }
        // Fallback to searching for outer array or object
        val firstArray = text.indexOf('[')
        val firstObj = text.indexOf('{')
        if (firstArray != -1 && (firstObj == -1 || firstArray < firstObj)) {
            val lastArray = text.lastIndexOf(']')
            if (lastArray != -1) return text.substring(firstArray, lastArray + 1)
        } else if (firstObj != -1) {
            val lastObj = text.lastIndexOf('}')
            if (lastObj != -1) return text.substring(firstObj, lastObj + 1)
        }
        return text
    }

    fun resetData() {
        viewModelScope.launch {
            // Re-seed database Callback manually for fresh onboarding
            database.clearAllTables()
            
            // Re-seed baseline data
            val defaultProfile = LearnerProfile()
            profileDao.insertOrUpdateProfile(defaultProfile)

            val concepts = listOf(
                ConceptMastery("functions", "Functions & Graphs", "Calculus", "", 0.0f, 0.0f, 0.0f, 0.0f, "Easy"),
                ConceptMastery("limits", "Limits & Continuity", "Calculus", "functions", 0.0f, 0.0f, 0.0f, 0.0f, "Medium"),
                ConceptMastery("derivatives", "Derivatives & Rates of Change", "Calculus", "limits", 0.0f, 0.0f, 0.0f, 0.0f, "Medium"),
                ConceptMastery("integration", "Integrals & Area", "Calculus", "derivatives", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),
                ConceptMastery("differential_equations", "Differential Equations", "Calculus", "integration", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),

                ConceptMastery("variables", "Variables & Data Types", "Computer Science", "", 0.0f, 0.0f, 0.0f, 0.0f, "Easy"),
                ConceptMastery("control_flow", "Control Flow & Loops", "Computer Science", "variables", 0.0f, 0.0f, 0.0f, 0.0f, "Easy"),
                ConceptMastery("functions_cs", "Functions & Scope", "Computer Science", "control_flow", 0.0f, 0.0f, 0.0f, 0.0f, "Medium"),
                ConceptMastery("data_structures", "Linear Data Structures", "Computer Science", "functions_cs", 0.0f, 0.0f, 0.0f, 0.0f, "Medium"),
                ConceptMastery("algorithms", "Sorting & Searching Algorithms", "Computer Science", "data_structures", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),

                ConceptMastery("atoms", "Atomic Structure", "Chemistry", "", 0.0f, 0.0f, 0.0f, 0.0f, "Easy"),
                ConceptMastery("periodic_table", "The Periodic Table", "Chemistry", "atoms", 0.0f, 0.0f, 0.0f, 0.0f, "Easy"),
                ConceptMastery("chemical_bonds", "Chemical Bonding", "Chemistry", "chemical_bonds", 0.0f, 0.0f, 0.0f, 0.0f, "Medium"),
                ConceptMastery("reactions", "Chemical Reactions", "Chemistry", "chemical_bonds", 0.0f, 0.0f, 0.0f, 0.0f, "Medium"),
                ConceptMastery("stoichiometry", "Stoichiometry & Mole Concept", "Chemistry", "reactions", 0.0f, 0.0f, 0.0f, 0.0f, "Hard")
            )
            conceptDao.insertAllConcepts(concepts)

            val flashcards = listOf(
                Flashcard(
                    conceptId = "functions",
                    question = "What is the domain of f(x) = 1/x?",
                    answer = "All real numbers except x = 0.",
                    difficulty = "Easy"
                ),
                Flashcard(
                    conceptId = "limits",
                    question = "Evaluate the limit: lim (x -> 3) of (x^2 - 9)/(x - 3)",
                    answer = "Factor the numerator as (x-3)(x+3). Cancel the (x-3) terms. The limit of (x+3) as x approaches 3 is 3 + 3 = 6.",
                    difficulty = "Medium"
                ),
                Flashcard(
                    conceptId = "variables",
                    question = "What is the difference between 'val' and 'var' in Kotlin?",
                    answer = "'val' declares a read-only (immutable) reference, whereas 'var' declares a mutable variable.",
                    difficulty = "Easy"
                ),
                Flashcard(
                    conceptId = "atoms",
                    question = "What are the three main subatomic particles in an atom?",
                    answer = "Protons (positive charge), Neutrons (neutral charge) in the nucleus, and Electrons (negative charge) orbiting the nucleus.",
                    difficulty = "Easy"
                )
            )
            flashcardDao.insertAllCards(flashcards)

            taskDao.clearAllTasks()
            _currentScreen.value = Screen.OnboardingWelcome
            showToast("Database reset successfully!")
        }
    }

    // --- Role-Based Access Control State & Actions ---
    private val _systemLogs = MutableStateFlow<List<String>>(
        listOf(
            "System Booted: NeuroLearn AI core initialized.",
            "Database Version: 3 (Room with Fallback Destructive Migration).",
            "Gemini Model: gemini-3.5-flash connection verified.",
            "System Audit: 3 Subjects (Calculus, CS, Chemistry) fully synchronized."
        )
    )
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    private val _adminSettings = MutableStateFlow(
        AdminSettings(
            aiTemperature = 0.7f,
            maxTokens = 2048,
            safetyLevel = "Standard"
        )
    )
    val adminSettings: StateFlow<AdminSettings> = _adminSettings.asStateFlow()

    fun logSystemAction(action: String) {
        val current = _systemLogs.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $action")
        _systemLogs.value = current.take(30)
    }

    fun updateUserRole(role: String) {
        viewModelScope.launch {
            val existing = profileDao.getProfileSync() ?: LearnerProfile()
            val updated = existing.copy(role = role)
            profileDao.insertOrUpdateProfile(updated)
            logSystemAction("User role transitioned to: $role")
            showToast("Switched identity to $role")
        }
    }

    fun updateConceptScores(id: String, understanding: Float, retention: Float, confidence: Float, predicted: Float) {
        viewModelScope.launch {
            conceptDao.updateScores(id, understanding, retention, confidence, predicted, System.currentTimeMillis(), System.currentTimeMillis() + 86400000L)
            logSystemAction("Instructor updated concept [$id]: understanding=${String.format("%.2f", understanding)}, retention=${String.format("%.2f", retention)}")
            showToast("Concept scores customized successfully!")
        }
    }

    fun addCustomStudyTask(conceptId: String, conceptName: String, subject: String) {
        viewModelScope.launch {
            taskDao.insertTask(
                StudyTask(
                    conceptId = conceptId,
                    conceptName = conceptName,
                    subject = subject,
                    dueDate = System.currentTimeMillis() + 172800000L
                )
            )
            logSystemAction("Instructor added customized Study Task: $conceptName ($subject)")
            showToast("Custom study assignment dispatched!")
        }
    }

    fun updateAdminSettings(temperature: Float, maxTokens: Int, safetyLevel: String) {
        _adminSettings.value = AdminSettings(temperature, maxTokens, safetyLevel)
        logSystemAction("Admin modified system AI parameters: temp=$temperature, maxTokens=$maxTokens, safety=$safetyLevel")
        showToast("AI system parameters modified!")
    }

    fun updateStreak(streak: Int) {
        viewModelScope.launch {
            val existing = profileDao.getProfileSync() ?: LearnerProfile()
            val updated = existing.copy(streak = streak)
            profileDao.insertOrUpdateProfile(updated)
            logSystemAction("Simulated study streak set to $streak days")
            showToast("Streak updated to $streak days!")
        }
    }

    fun simulateMasteredCards(count: Int) {
        viewModelScope.launch {
            val existingCards = flashcardDao.getAllCards().first()
            val masteredCount = existingCards.filter { it.repetitions >= 1 }.size
            val needed = count - masteredCount
            if (needed <= 0) {
                showToast("Already have $masteredCount mastered cards!")
                return@launch
            }
            
            // First, let's mark existing unmastered cards as mastered
            val updatedExisting = existingCards.filter { it.repetitions < 1 }.take(needed).map {
                it.copy(repetitions = 1, intervalDays = 3, easeFactor = 2.5f)
            }
            for (card in updatedExisting) {
                flashcardDao.insertCard(card)
            }
            
            val remainingNeeded = needed - updatedExisting.size
            if (remainingNeeded > 0) {
                // We need to insert new dummy cards to reach the target count
                val newCards = (1..remainingNeeded).map { i ->
                    Flashcard(
                        conceptId = "functions",
                        question = "Simulated Mastery Question #$i",
                        answer = "Simulated Mastery Answer #$i",
                        difficulty = "Easy",
                        repetitions = 1,
                        intervalDays = 3,
                        easeFactor = 2.5f,
                        deckId = "default"
                    )
                }
                flashcardDao.insertAllCards(newCards)
            }
            
            // Also let's award some XP for mastering cards to keep level synced
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val addedXp = remainingNeeded * 10 + updatedExisting.size * 10
            val newXp = currentProfile.xp + addedXp
            val newLevel = (newXp / 100) + 1
            profileDao.insertOrUpdateProfile(currentProfile.copy(xp = newXp, level = newLevel))
            
            logSystemAction("Simulated $count mastered cards. Added $addedXp XP.")
            showToast("Successfully simulated $count mastered cards!")
        }
    }
}

// --- Domain helper models ---

data class AdminSettings(
    val aiTemperature: Float,
    val maxTokens: Int,
    val safetyLevel: String
)

data class PrerequisiteCheckResult(
    val isMet: Boolean,
    val unmetPrerequisites: List<String>
)

data class DiagnosticQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

data class NoteGlossaryItem(
    val term: String,
    val definition: String
)

data class NoteFlashcard(
    val question: String,
    val answer: String
)

data class ProcessedNoteData(
    val summary: String,
    val formulas: List<String>,
    val glossary: List<NoteGlossaryItem>,
    val flashcards: List<NoteFlashcard>,
    val quizzes: List<QuizQuestion>
)
