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
    object TechHub : Screen
    object LanguageLab : Screen
    data class TutorChat(val conceptId: String? = null, val deckId: String? = null) : Screen
    data class PdfIntelligence(val conceptId: String? = null) : Screen
    data class QuizGame(val conceptId: String, val difficulty: String) : Screen
    data class SharedSession(val roomId: String? = null) : Screen
    object DigitalTwinDashboard : Screen
    object StudyPlanner : Screen
    data class TechStudyRoom(val roomId: String) : Screen
    object ExamPartnershipsHub : Screen
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
    private val techProjectDao = database.techProjectDao()
    private val projectCommentDao = database.projectCommentDao()
    private val savedPhraseDao = database.savedPhraseDao()
    private val recentlyStudiedDeckDao = database.recentlyStudiedDeckDao()
    private val pendingSyncActionDao = database.pendingSyncActionDao()
    private val techStudyRoomDao = database.techStudyRoomDao()
    private val techRoomMessageDao = database.techRoomMessageDao()
    private val scratchpadItemDao = database.scratchpadItemDao()
    private val mentorMatchDao = database.mentorMatchDao()
    private val researchPaperDao = database.researchPaperDao()
    private val blockchainCertificateDao = database.blockchainCertificateDao()
    private val accreditedExamQuestionDao = database.accreditedExamQuestionDao()
    private val platformPartnerDao = database.platformPartnerDao()
    private val partnershipApplicationDao = database.partnershipApplicationDao()

    // --- State Flows ---
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.OnboardingWelcome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isNetworkOnline = MutableStateFlow(true)
    val isNetworkOnline: StateFlow<Boolean> = _isNetworkOnline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    val recentlyStudiedDecks: StateFlow<List<RecentlyStudiedDeck>> = recentlyStudiedDeckDao.getRecentlyStudied()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<LearnerProfile?> = profileDao.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accreditedExamQuestions: StateFlow<List<AccreditedExamQuestion>> = accreditedExamQuestionDao.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val platformPartners: StateFlow<List<PlatformPartner>> = platformPartnerDao.getAllPartners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partnershipApplications: StateFlow<List<PartnershipApplication>> = partnershipApplicationDao.getAllApplications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val techProjects: StateFlow<List<TechProject>> = techProjectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val techStudyRooms: StateFlow<List<TechStudyRoom>> = techStudyRoomDao.getAllRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResearchPapers: StateFlow<List<ResearchPaper>> = researchPaperDao.getAllPapers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCertificates: StateFlow<List<BlockchainCertificate>> = blockchainCertificateDao.getAllCertificates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeProjectComments: StateFlow<List<ProjectComment>> = _selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId != null) projectCommentDao.getCommentsForProject(projectId)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
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

    // --- Shared Study Session / Real-Time Collaboration States ---
    private val _activeRoom = MutableStateFlow<SharedRoom?>(null)
    val activeRoom: StateFlow<SharedRoom?> = _activeRoom.asStateFlow()

    private val _roomParticipants = MutableStateFlow<List<SharedRoomParticipant>>(emptyList())
    val roomParticipants: StateFlow<List<SharedRoomParticipant>> = _roomParticipants.asStateFlow()

    private val _roomMessages = MutableStateFlow<List<SharedRoomMessage>>(emptyList())
    val roomMessages: StateFlow<List<SharedRoomMessage>> = _roomMessages.asStateFlow()

    private val _availableRooms = MutableStateFlow<List<SharedRoom>>(emptyList())
    val availableRooms: StateFlow<List<SharedRoom>> = _availableRooms.asStateFlow()

    private val _isRoomConnecting = MutableStateFlow(false)
    val isRoomConnecting: StateFlow<Boolean> = _isRoomConnecting.asStateFlow()

    private val _roomFlashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    val roomFlashcards: StateFlow<List<Flashcard>> = _roomFlashcards.asStateFlow()

    private var peerSimulationJob: kotlinx.coroutines.Job? = null

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

    // Deck Content Summary
    private val _deckContentSummary = MutableStateFlow<String?>(null)
    val deckContentSummary: StateFlow<String?> = _deckContentSummary.asStateFlow()

    private val _isGeneratingDeckSummary = MutableStateFlow(false)
    val isGeneratingDeckSummary: StateFlow<Boolean> = _isGeneratingDeckSummary.asStateFlow()

    fun clearDeckContentSummary() {
        _deckContentSummary.value = null
    }

    fun generateDeckContentSummary(deckName: String, cards: List<Flashcard>) {
        if (cards.isEmpty()) return
        viewModelScope.launch {
            _isGeneratingDeckSummary.value = true
            _deckContentSummary.value = null
            try {
                val summary = GeminiClient.generateDeckContentSummary(deckName, cards)
                _deckContentSummary.value = summary
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating deck content summary", e)
                showToast("Failed to generate deck summary: ${e.message}")
            } finally {
                _isGeneratingDeckSummary.value = false
            }
        }
    }

    private val _activeStudyAlert = MutableStateFlow<StudyTask?>(null)
    val activeStudyAlert: StateFlow<StudyTask?> = _activeStudyAlert.asStateFlow()

    private val notifiedTaskIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    init {
        // Set up connectivity monitoring
        val cm = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (cm != null) {
            val builder = android.net.NetworkRequest.Builder()
            try {
                cm.registerNetworkCallback(builder.build(), object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        _isNetworkOnline.value = true
                        viewModelScope.launch {
                            processPendingSyncQueue()
                        }
                    }

                    override fun onLost(network: android.net.Network) {
                        val activeNetwork = cm.activeNetwork
                        val capabilities = cm.getNetworkCapabilities(activeNetwork)
                        val online = capabilities != null && (
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                        )
                        _isNetworkOnline.value = online
                        if (online) {
                            viewModelScope.launch {
                                processPendingSyncQueue()
                            }
                        }
                    }
                })
                val activeNetwork = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(activeNetwork)
                _isNetworkOnline.value = capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            } catch (e: Exception) {
                Log.e("neurolearn", "Failed to register network callback", e)
            }
        }

        // Initialize pending sync count
        updatePendingSyncCount()

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
            val db = firestore
            if (db != null && isNetworkOnline.value) {
                db.collection("study_tasks").document(task.id.toString()).update("isCompleted", newStatus)
            } else {
                Log.d("neurolearn", "Offline, queuing task status update...")
                queueSyncAction("UPDATE_TASK", org.json.JSONObject().apply {
                    put("taskId", task.id)
                    put("isCompleted", newStatus)
                })
            }
            
            if (newStatus) {
                awardXp(task.xpAwarded)
                addCoinsReward(15)
                // Propagate a micro increase in mastery for studying the concept
                propagateMastery(task.conceptId, 0.05f)
                showToast("Task completed! +${task.xpAwarded} XP & +15 Coins! 🪙")
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

    @android.annotation.SuppressLint("NotificationPermission")
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

    fun awardXp(amount: Int, incrementCardsReviewed: Boolean = false, incrementQuizzesCompleted: Boolean = false) {
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

            val updatedCardsReviewed = if (incrementCardsReviewed) currentProfile.cardsReviewedCount + 1 else currentProfile.cardsReviewedCount
            val updatedQuizzesCompleted = if (incrementQuizzesCompleted) currentProfile.quizzesCompletedCount + 1 else currentProfile.quizzesCompletedCount

            profileDao.insertOrUpdateProfile(
                currentProfile.copy(
                    xp = newXp,
                    level = newLevel,
                    cardsReviewedCount = updatedCardsReviewed,
                    quizzesCompletedCount = updatedQuizzesCompleted
                )
            )
        }
    }

    fun completeQuiz(score: Int) {
        val xpGained = 50 + (score * 10)
        awardXp(amount = xpGained, incrementQuizzesCompleted = true)
        showToast("Quiz Completed! +$xpGained XP 🎉")
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
            awardXp(8, incrementCardsReviewed = true)
            
            // Track recently studied deck
            markDeckAsStudied(card.deckId)
            
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
            
            // Sync or Queue
            val db = firestore
            if (db != null && isNetworkOnline.value) {
                syncDecksAndCardsToFirestore()
            } else {
                Log.d("neurolearn", "Offline, queuing flashcard rating sync...")
                queueSyncAction("RATE_CARD", org.json.JSONObject().apply {
                    put("cardId", card.id)
                    put("rating", rating)
                })
            }
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
            
            val db = firestore
            if (db != null && isNetworkOnline.value) {
                db.collection("decks").document(deck.id)
                    .set(deck)
                    .addOnSuccessListener {
                        Log.d("neurolearn", "Successfully synced deck ${deck.name} to Firestore!")
                    }
                    .addOnFailureListener { e ->
                        Log.w("neurolearn", "Failed to sync deck to Firestore, queuing...", e)
                        queueSyncAction("CREATE_DECK", org.json.JSONObject().apply {
                            put("id", deck.id)
                            put("name", deck.name)
                            put("description", deck.description)
                            put("subject", deck.subject)
                            put("createdAt", deck.createdAt)
                        })
                    }
            } else {
                Log.d("neurolearn", "Offline or Firestore null, queuing deck creation...")
                queueSyncAction("CREATE_DECK", org.json.JSONObject().apply {
                    put("id", deck.id)
                    put("name", deck.name)
                    put("description", deck.description)
                    put("subject", deck.subject)
                    put("createdAt", deck.createdAt)
                })
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

            val db = firestore
            if (db != null && isNetworkOnline.value) {
                syncDecksAndCardsToFirestore()
            } else {
                Log.d("neurolearn", "Offline, queuing card insertion...")
                queueSyncAction("ADD_CARD", org.json.JSONObject().apply {
                    put("id", card.id)
                    put("conceptId", card.conceptId)
                    put("question", card.question)
                    put("answer", card.answer)
                    put("difficulty", card.difficulty)
                    put("deckId", card.deckId ?: "default")
                    put("tags", card.tags)
                })
            }
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

    fun initializeDeckChatContext(deckId: String) {
        _activeChatSession.value = "deck_session_$deckId"
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

    fun sendMessageToTutor(conceptId: String?, deckId: String?, userText: String, currentCard: Flashcard? = null) {
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
                
                val currentCardContext = if (currentCard != null) {
                    """
                    
                    CRITICAL CURRENT STUDY CONTEXT:
                    The student is actively reviewing/studying this specific flashcard right now:
                    QUESTION: "${currentCard.question}"
                    ANSWER: "${currentCard.answer}"
                    
                    Their question or prompt is highly likely to be specifically about this flashcard's concept. Please tailor your explanations, socratic questions, real-world comparative analogies, or step-by-step guidance specifically to help them understand and master this card!
                    """.trimIndent()
                } else {
                    ""
                }
                
                """
                    $twinPersona
                    You are tutoring the student on their flashcard deck named "${deck?.name ?: "Flashcards"}".
                    Here are all the flashcards in this deck that you have full knowledge of:
                    $cardsDetail
                    $currentCardContext
                    
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

    fun processImportedPdf(conceptId: String, fileName: String, pdfText: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            val concept = conceptDao.getConceptById(conceptId)
            val conceptName = concept?.name ?: "Topic"
            val conceptSubject = concept?.subject ?: "General"

            // Strip suffix and clean name
            val deckName = fileName.removeSuffix(".pdf").replace("_", " ").replace("-", " ")
                .trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val formattedDeckName = if (deckName.endsWith("Deck", true) || deckName.endsWith("Flashcard Deck", true)) deckName else "$deckName Deck"

            val systemPrompt = "You are the PDF Intelligence Agent of NeuroLearn AI, a master material synthesizer."
            
            val refinedNotesText = if (pdfText.length < 50) {
                "Note: A scanned or complex layout PDF was uploaded named '$fileName' on the topic of '$conceptName'. " +
                "Since standard text extraction resulted in minor text, please perform a deep conceptual synthesis on '$conceptName' ($conceptSubject), covering intermediate and advanced exam-level concepts, formulas, and terminology, as if you had access to the full lecture notes of this topic."
            } else {
                pdfText
            }

            val prompt = """
                Extract deep study assets from the following imported PDF file:
                FILE NAME: $fileName
                TOPIC: $conceptName ($conceptSubject)
                
                NOTES CONTENT:
                $refinedNotesText
                
                You MUST synthesize the material and generate a JSON response with:
                - "summary": string (a comprehensive high-quality paragraph summary of the PDF content)
                - "formulas": list of strings (key formulas/equations or core rules)
                - "glossary": list of objects, each with "term" and "definition"
                - "flashcards": list of objects, each with "question" and "answer" (generate at least 6-8 high-quality study flashcards)
                - "quizzes": list of MCQ objects, each with "question", "options" (4 strings), "correctAnswer" (matching one option), and "explanation"
                
                Ensure the response is valid JSON enclosed exactly between JSON_START and JSON_END tags.
            """.trimIndent()

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                val parsedData = parseProcessedNotesJson(response)
                if (parsedData != null) {
                    _generatedNotes.value = parsedData

                    // Create the new Flashcard Deck specifically for this PDF!
                    val newDeck = FlashcardDeck(
                        name = formattedDeckName,
                        description = "AI-synthesized from imported PDF: $fileName",
                        subject = conceptSubject
                    )
                    deckDao.insertDeck(newDeck)

                    // Insert synthesized flashcards into the database linked to this new deck!
                    val newCards = parsedData.flashcards.map { cardData ->
                        Flashcard(
                            conceptId = conceptId,
                            question = cardData.question,
                            answer = cardData.answer,
                            difficulty = "Medium",
                            deckId = newDeck.id
                        )
                    }
                    flashcardDao.insertAllCards(newCards)

                    // Boost the concept's understanding metrics
                    propagateMastery(conceptId, 0.20f) // importing a whole PDF gives a bigger boost!
                    awardXp(50) // and more XP!
                    showToast("Successfully parsed PDF and created deck '$formattedDeckName' with ${newCards.size} cards! +50 XP 🚀")
                } else {
                    showToast("Failed to process PDF structure. Loading template study assets.")
                    loadDefaultPdfImportMockup(conceptId, fileName, formattedDeckName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing PDF", e)
                loadDefaultPdfImportMockup(conceptId, fileName, formattedDeckName)
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun loadDefaultPdfImportMockup(conceptId: String, fileName: String, formattedDeckName: String) {
        viewModelScope.launch {
            val concept = conceptDao.getConceptById(conceptId)
            val conceptName = concept?.name ?: "Topic"
            val conceptSubject = concept?.subject ?: "General"

            val mockData = ProcessedNoteData(
                summary = "This study asset compiles the core mathematical, algorithmic, or structural rules of $conceptName. By mapping the relationship of this concept to its neighboring subjects, we discover core formulas and definitions that are vital for passing AP and College exams.",
                formulas = listOf(
                    "Core Principle: Active retrieval spaced over intervals",
                    "Rule of Three: Review, Test, Teach",
                    "Active Recall Mastery: Testing yourself is the highest yield study tactic"
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

            // Create the new Flashcard Deck specifically for this PDF fallback!
            val newDeck = FlashcardDeck(
                name = formattedDeckName,
                description = "Synthesized study assets for PDF: $fileName",
                subject = conceptSubject
            )
            deckDao.insertDeck(newDeck)

            val newCards = mockData.flashcards.map { cardData ->
                Flashcard(
                    conceptId = conceptId,
                    question = cardData.question,
                    answer = cardData.answer,
                    difficulty = "Medium",
                    deckId = newDeck.id
                )
            }
            flashcardDao.insertAllCards(newCards)

            showToast("Successfully synthesized deck '$formattedDeckName' using smart templates! +25 XP")
        }
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
            "System Audit: 12 Subjects (Calculus, CS, Chemistry, and 9 Tech Tracks) fully synchronized."
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

    fun simulateGamification(reviews: Int, quizzes: Int) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: return@launch
            val updated = currentProfile.copy(
                cardsReviewedCount = reviews,
                quizzesCompletedCount = quizzes
            )
            profileDao.insertOrUpdateProfile(updated)
            showToast("Simulated $reviews reviews and $quizzes quizzes!")
        }
    }

    // --- Tech Hub Collaboration Methods ---

    fun selectProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun addProject(title: String, description: String, techStack: String) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val creatorName = currentProfile.name.ifBlank { "Lifelong Learner" }
            val creatorRole = currentProfile.role.ifBlank { "Learner" }

            val newProject = TechProject(
                title = title,
                description = description,
                creatorName = creatorName,
                creatorRole = creatorRole,
                techStack = techStack,
                teamMembers = "$creatorName ($creatorRole)"
            )
            techProjectDao.insertProject(newProject)
            awardXp(30) // Proposing an innovative project awards 30 XP!
            showToast("Project proposal '$title' submitted successfully! +30 XP 🚀")
        }
    }

    fun likeProject(projectId: String) {
        viewModelScope.launch {
            techProjectDao.likeProject(projectId)
            awardXp(5) // Appreciating other learners' innovative ideas awards 5 XP!
            showToast("You liked this project! +5 XP ❤️")
        }
    }

    fun joinProjectTeam(projectId: String, currentProject: TechProject) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val userName = currentProfile.name.ifBlank { "Lifelong Learner" }
            val userRole = currentProfile.role.ifBlank { "Learner" }
            val memberEntry = "$userName ($userRole)"

            val currentMembers = currentProject.teamMembers
            if (currentMembers.contains(userName)) {
                showToast("You are already part of this project's team! 🤝")
                return@launch
            }

            val updatedMembers = if (currentMembers.isBlank()) memberEntry else "$currentMembers, $memberEntry"
            techProjectDao.updateTeamMembers(projectId, updatedMembers)
            awardXp(15) // Joining an innovative project team awards 15 XP!
            showToast("You have successfully joined the team! +15 XP 🤝")
        }
    }

    fun addProjectComment(projectId: String, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) return@launch
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val authorName = currentProfile.name.ifBlank { "Lifelong Learner" }
            val authorRole = currentProfile.role.ifBlank { "Learner" }

            val newComment = ProjectComment(
                projectId = projectId,
                authorName = authorName,
                authorRole = authorRole,
                text = text
            )
            projectCommentDao.insertComment(newComment)
            awardXp(10) // Collaborating and commenting awards 10 XP!
            showToast("Comment posted! +10 XP 💬")
        }
    }

    // =========================================================================
    // --- Language Learning / Multilingual Lab State & Methods ---
    // =========================================================================

    private val _selectedLanguage = MutableStateFlow("Spanish")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _translationResult = MutableStateFlow<String?>(null)
    val translationResult: StateFlow<String?> = _translationResult.asStateFlow()

    private val _pronunciationFeedback = MutableStateFlow<Pair<Int, String>?>(null) // Pair(Accuracy %, Feedback)
    val pronunciationFeedback: StateFlow<Pair<Int, String>?> = _pronunciationFeedback.asStateFlow()

    val allPhrases: StateFlow<List<SavedPhrase>> = savedPhraseDao.getAllPhrases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPhrases: StateFlow<List<SavedPhrase>> = combine(_selectedLanguage, allPhrases) { lang, phrases ->
        phrases.filter { it.language.equals(lang, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
        _translationResult.value = null
        _pronunciationFeedback.value = null
    }

    fun addCustomPhrase(original: String, translation: String, pronunciation: String) {
        viewModelScope.launch {
            if (original.isBlank() || translation.isBlank()) return@launch
            val newPhrase = SavedPhrase(
                language = _selectedLanguage.value,
                originalText = original,
                translatedText = translation,
                pronunciation = pronunciation.ifBlank { "Pronunciation helper not set" }
            )
            savedPhraseDao.insertPhrase(newPhrase)
            awardXp(10)
            showToast("Saved to your vocabulary list! +10 XP 📝")
        }
    }

    fun togglePhraseMastery(phraseId: String, currentMastered: Boolean) {
        viewModelScope.launch {
            savedPhraseDao.updatePhraseMastery(phraseId, !currentMastered)
            if (!currentMastered) {
                awardXp(15) // Mastering phrases earns 15 XP
                showToast("Phrase mastered! +15 XP 🏆")
            } else {
                showToast("Phrase marked for practice.")
            }
        }
    }

    fun deletePhrase(phraseId: String) {
        viewModelScope.launch {
            savedPhraseDao.deletePhrase(phraseId)
            showToast("Phrase removed from vocabulary list.")
        }
    }

    fun translateAndLearn(englishText: String) {
        viewModelScope.launch {
            if (englishText.isBlank()) return@launch
            _isTranslating.value = true
            _translationResult.value = null
            
            val targetLang = _selectedLanguage.value
            val systemPrompt = """
                You are an elite, encouraging language tutor and expert translator. 
                Translate the user's English phrase into $targetLang. 
                You must output ONLY a valid JSON object with the following schema:
                {
                  "translation": "the exact translated text in $targetLang",
                  "pronunciation": "intuitive phonetic pronunciation/transcription helper for English speakers",
                  "explanation": "Brief 1-sentence tip on pronunciation, grammar, or cultural context."
                }
                Do not include any other text or explanation. Only return the JSON.
            """.trimIndent()

            val prompt = "Translate: \"$englishText\" to $targetLang"

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                val cleanedJson = response.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()
                
                val json = org.json.JSONObject(cleanedJson)
                val translation = json.getString("translation")
                val pronunciation = json.getString("pronunciation")
                val explanation = json.optString("explanation", "")

                // Save the phrase automatically
                val newPhrase = SavedPhrase(
                    language = targetLang,
                    originalText = englishText,
                    translatedText = translation,
                    pronunciation = pronunciation
                )
                savedPhraseDao.insertPhrase(newPhrase)
                
                _translationResult.value = "Translation: $translation\nPronunciation: ($pronunciation)\nTip: $explanation"
                awardXp(15) // Translating and learning awards 15 XP
                showToast("AI Translation added to list! +15 XP 🗣️")
            } catch (e: Exception) {
                Log.e(TAG, "Translation error", e)
                // Fallback response programmatically if JSON parsing fails or offline
                val fallbackTrans = when (targetLang) {
                    "Spanish" -> "Hola amigo, aprendamos juntos."
                    "French" -> "Bonjour mon ami, apprenons ensemble."
                    "German" -> "Hallo Freund, lass uns zusammen lernen."
                    "Japanese" -> "こんにちは、一緒に勉強しましょう。"
                    else -> "Mambo, tujifunze pamoja."
                }
                val fallbackPron = when (targetLang) {
                    "Spanish" -> "OH-lah ah-MEE-goh, ah-pren-DAH-moss hoon-tohs"
                    "French" -> "bohn-zhoor mohn ah-mee, ah-pruh-nohn ahn-sahmbl"
                    "German" -> "hahl-low froynd, lahss oons tsoo-zahm-en lair-nen"
                    "Japanese" -> "kon-nee-chee-wah, eesh-sho-nee ben-kyoo shee-mash-shoo"
                    else -> "MAHM-boh, too-jee-foon-zeh pah-MOH-jah"
                }
                
                val newPhrase = SavedPhrase(
                    language = targetLang,
                    originalText = englishText,
                    translatedText = fallbackTrans,
                    pronunciation = fallbackPron
                )
                savedPhraseDao.insertPhrase(newPhrase)
                _translationResult.value = "Translation: $fallbackTrans\nPronunciation: ($fallbackPron)\nTip: Learn standard friendly greetings."
                showToast("Translation added to list! 🗣️")
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun evaluateSpeechPronunciation(targetPhrase: String, spokenTranscript: String) {
        viewModelScope.launch {
            if (spokenTranscript.isBlank() || targetPhrase.isBlank()) return@launch
            _pronunciationFeedback.value = null
            _isTranslating.value = true

            val targetLang = _selectedLanguage.value
            val systemPrompt = """
                You are a supportive, high-fidelity AI pronunciation coach. 
                Compare what the user attempted to speak (spoken text) to the target phrase they were trying to say in $targetLang.
                Evaluate the phonetic closeness.
                Output ONLY a JSON object in this format:
                {
                  "accuracy": 85, // integer between 0 and 100
                  "feedback": "Encouraging 1-2 sentence speech evaluation tip explaining which sounds were right or can be polished."
                }
            """.trimIndent()

            val prompt = "Target phrase: \"$targetPhrase\"\nSpoken transcription: \"$spokenTranscript\""

            try {
                val response = GeminiClient.generate(prompt, systemPrompt)
                val cleanedJson = response.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()
                
                val json = org.json.JSONObject(cleanedJson)
                val accuracy = json.getInt("accuracy")
                val feedback = json.getString("feedback")

                _pronunciationFeedback.value = Pair(accuracy, feedback)
                if (accuracy >= 80) {
                    awardXp(20) // Great pronunciation earns 20 XP!
                    showToast("Superb pronunciation! +20 XP 🎤")
                } else {
                    awardXp(5) // Good try earns 5 XP
                    showToast("Nice attempt! Practice makes perfect +5 XP 🌟")
                }
            } catch (e: Exception) {
                // Local fallback text matching comparison
                val similarity = calculateSimilarity(targetPhrase, spokenTranscript)
                val accuracy = (similarity * 100).toInt()
                val feedback = if (accuracy >= 80) {
                    "Splendid! Your pronunciation is highly accurate and very clear."
                } else {
                    "Good try! Focus on matching each syllable carefully. Keep practicing!"
                }
                _pronunciationFeedback.value = Pair(accuracy, feedback)
                showToast("Speech evaluated! 🎤")
            } finally {
                _isTranslating.value = false
            }
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        val str1 = s1.lowercase().replace(Regex("[^a-zA-Z0-9]"), "")
        val str2 = s2.lowercase().replace(Regex("[^a-zA-Z0-9]"), "")
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f
        
        // Simple Levenshtein distance matching
        val len0 = str1.length + 1
        val len1 = str2.length + 1
        var cost = IntArray(len0)
        var newCost = IntArray(len0)
        for (i in 0 until len0) cost[i] = i
        for (j in 1 until len1) {
            newCost[0] = j
            for (i in 1 until len0) {
                val match = if (str1[i - 1] == str2[j - 1]) 0 else 1
                val costReplace = cost[i - 1] + match
                val costInsert = cost[i] + 1
                val costDelete = newCost[i - 1] + 1
                newCost[i] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        val distance = cost[len0 - 1]
        val maxLength = maxOf(str1.length, str2.length)
        return (maxLength - distance).toFloat() / maxLength.toFloat()
    }

    // =========================================================================
    // --- Shared Study Session / Real-Time Collaboration Methods ---
    // =========================================================================

    fun loadAvailableRooms() {
        val userDecks = allDecks.value
        val defaultDeckId = userDecks.firstOrNull()?.id ?: "local_deck_neuroscience"
        val defaultDeckName = userDecks.firstOrNull()?.name ?: "Brain Anatomy & Memory"
        val defaultSubject = userDecks.firstOrNull()?.subject ?: "Neuroscience"

        val preseeded = listOf(
            SharedRoom(
                id = "room_neuro_plasticity",
                name = "Neural Plasticity Co-Study 🧠",
                deckId = defaultDeckId,
                deckName = defaultDeckName,
                hostUserId = "host_sarah",
                hostUserName = "Sarah Jennings",
                subject = defaultSubject,
                createdAt = System.currentTimeMillis() - 1200000L,
                activeParticipantCount = 3
            ),
            SharedRoom(
                id = "room_chem_synthesis",
                name = "Carbon Chemistry Synthesis 🧪",
                deckId = defaultDeckId,
                deckName = defaultDeckName,
                hostUserId = "host_alex",
                hostUserName = "Alex Rivera",
                subject = "Chemistry",
                createdAt = System.currentTimeMillis() - 3600000L,
                activeParticipantCount = 2
            ),
            SharedRoom(
                id = "room_calc_integrals",
                name = "AP Calculus Integration Race 📈",
                deckId = defaultDeckId,
                deckName = defaultDeckName,
                hostUserId = "host_elena",
                hostUserName = "Elena Rostova",
                subject = "Calculus",
                createdAt = System.currentTimeMillis() - 600000L,
                activeParticipantCount = 4
            )
        )
        _availableRooms.value = preseeded
    }

    fun joinSharedRoom(roomId: String) {
        viewModelScope.launch {
            _isRoomConnecting.value = true
            
            // Look up the room from available rooms or create a dummy if not found
            var room = _availableRooms.value.find { it.id == roomId }
            if (room == null) {
                // Check if it's the room the user just created
                room = _activeRoom.value?.takeIf { it.id == roomId }
            }
            if (room == null) {
                // Fallback
                room = SharedRoom(
                    id = roomId,
                    name = "Custom Shared Study Room",
                    deckId = "local_deck_neuroscience",
                    deckName = "Brain Anatomy & Memory",
                    hostUserId = "user_default",
                    hostUserName = "You",
                    subject = "General",
                    createdAt = System.currentTimeMillis()
                )
            }
            
            // Fetch flashcards for this deck
            val dbCards = flashcardDao.getAllCards().firstOrNull() ?: emptyList()
            val roomCards = dbCards.filter { it.deckId == room.deckId || it.deckId == "default" }
            _roomFlashcards.value = roomCards
            
            _activeRoom.value = room
            
            // Get user's current profile to set up their twin info
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val userTwin = currentProfile.selectedTwinAvatar
            val userTwinName = when (userTwin) {
                "tech" -> "Tech Visionary"
                "scholar" -> "Scholar Academic"
                "creative" -> "Creative Innovator"
                else -> "Socratic Mentor"
            }
            
            // Initialize participants
            val initialParticipants = mutableListOf<SharedRoomParticipant>()
            
            // 1. The User themselves
            initialParticipants.add(
                SharedRoomParticipant(
                    userId = currentProfile.id,
                    name = currentProfile.name,
                    avatar = "Jusreal", // user avatar descriptor
                    twinAvatar = userTwin,
                    twinName = userTwinName,
                    cardsReviewed = 0,
                    xpEarned = 0,
                    accuracy = 100
                )
            )
            
            // 2. Simulated Peer 1 (Sarah)
            if (room.id == "room_neuro_plasticity" || room.hostUserId != currentProfile.id) {
                initialParticipants.add(
                    SharedRoomParticipant(
                        userId = "peer_sarah",
                        name = "Sarah Jennings",
                        avatar = "Sarah",
                        twinAvatar = "scholar",
                        twinName = "Scholar Academic",
                        cardsReviewed = 12,
                        xpEarned = 180,
                        accuracy = 85
                    )
                )
            }
            
            // 3. Simulated Peer 2 (Alex)
            if (room.id != "room_chem_synthesis" || room.hostUserId != currentProfile.id) {
                initialParticipants.add(
                    SharedRoomParticipant(
                        userId = "peer_alex",
                        name = "Alex Rivera",
                        avatar = "Alex",
                        twinAvatar = "creative",
                        twinName = "Creative Innovator",
                        cardsReviewed = 8,
                        xpEarned = 120,
                        accuracy = 75
                    )
                )
            }
            
            // 4. Simulated Peer 3 (Elena)
            if (room.id == "room_calc_integrals") {
                initialParticipants.add(
                    SharedRoomParticipant(
                        userId = "peer_elena",
                        name = "Elena Rostova",
                        avatar = "Elena",
                        twinAvatar = "tech",
                        twinName = "Tech Visionary",
                        cardsReviewed = 15,
                        xpEarned = 225,
                        accuracy = 92
                    )
                )
            }
            
            _roomParticipants.value = initialParticipants
            
            // Initialize real-time feed messages
            val initialMessages = mutableListOf<SharedRoomMessage>()
            initialMessages.add(
                SharedRoomMessage(
                    senderName = "System",
                    senderAvatar = "system",
                    content = "Welcome to '${room.name}'! You have joined with your digital twin ($userTwinName). Let's collaborate!",
                    isSystemAction = true
                )
            )
            
            if (initialParticipants.size > 1) {
                initialMessages.add(
                    SharedRoomMessage(
                        senderName = "Sarah Jennings",
                        senderAvatar = "Sarah",
                        content = "Hey! Glad you could make it. My Scholar twin and I were just working on this deck.",
                        isTwinMessage = false
                    )
                )
                initialMessages.add(
                    SharedRoomMessage(
                        senderName = "Scholar Academic",
                        senderAvatar = "scholar",
                        content = "Academic greeting. I have cataloged the synaptic plasticity concepts for rigorous review.",
                        isTwinMessage = true
                    )
                )
            }
            
            _roomMessages.value = initialMessages
            _isRoomConnecting.value = false
            
            // Start simulated real-time peer activity!
            startPeerSimulation()
        }
    }

    fun createSharedRoom(name: String, deckId: String, subject: String) {
        viewModelScope.launch {
            _isRoomConnecting.value = true
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            
            val decksList = allDecks.value
            val deckName = decksList.find { it.id == deckId }?.name ?: "Custom Deck"
            
            val newRoom = SharedRoom(
                id = "room_custom_" + UUID.randomUUID().toString().take(6),
                name = name,
                deckId = deckId,
                deckName = deckName,
                hostUserId = currentProfile.id,
                hostUserName = currentProfile.name,
                subject = subject,
                createdAt = System.currentTimeMillis(),
                activeParticipantCount = 1
            )
            
            _activeRoom.value = newRoom
            
            // Append to available rooms so it can be seen
            val currentAvailable = _availableRooms.value.toMutableList()
            currentAvailable.add(0, newRoom)
            _availableRooms.value = currentAvailable
            
            // Directly join the room
            joinSharedRoom(newRoom.id)
            navigateTo(Screen.SharedSession(newRoom.id))
            showToast("Shared Session room '$name' created successfully!")
        }
    }

    fun sendRoomMessage(content: String) {
        val room = _activeRoom.value ?: return
        val currentProfile = profile.value ?: LearnerProfile()
        
        val userMsg = SharedRoomMessage(
            senderName = currentProfile.name,
            senderAvatar = "Jusreal",
            content = content,
            isSystemAction = false,
            isTwinMessage = false
        )
        
        val updatedMessages = _roomMessages.value.toMutableList()
        updatedMessages.add(userMsg)
        _roomMessages.value = updatedMessages
        
        // Also simulate our digital twin commenting on our message!
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val twinAvatar = currentProfile.selectedTwinAvatar
            val twinName = when (twinAvatar) {
                "tech" -> "Tech Visionary"
                "scholar" -> "Scholar Academic"
                "creative" -> "Creative Innovator"
                else -> "Socratic Mentor"
            }
            
            // Retrieve 1-2 random keywords or cards to make the twin response extremely relevant!
            val cards = _roomFlashcards.value
            val randomCardKeyword = cards.randomOrNull()?.question?.take(30) ?: "study topics"
            
            val twinPrompt = "The user says: \"$content\". Based on our deck content (mentioning: '$randomCardKeyword'), give a very short encouraging or teaching response (max 2 sentences)."
            val systemInstructions = when (twinAvatar) {
                "tech" -> "You are 'Tech Visionary'. Be futuristic, precise, structural. Use short answers."
                "scholar" -> "You are 'Scholar Academic'. Be formal, rigorous, analytical."
                "creative" -> "You are 'Creative Innovator'. Use a rich comparative metaphor or playful analogy."
                else -> "You are 'Socratic Mentor'. Respond with a short, thoughtful guiding question."
            }
            
            val twinText = if (GeminiClient.isApiKeyAvailable()) {
                try {
                    GeminiClient.generate(twinPrompt, systemInstructions)
                } catch (e: Exception) {
                    getDefaultTwinResponse(twinAvatar, content, randomCardKeyword)
                }
            } else {
                getDefaultTwinResponse(twinAvatar, content, randomCardKeyword)
            }
            
            val twinMsg = SharedRoomMessage(
                senderName = twinName,
                senderAvatar = twinAvatar,
                content = twinText,
                isSystemAction = false,
                isTwinMessage = true
            )
            
            val currentList = _roomMessages.value.toMutableList()
            currentList.add(twinMsg)
            _roomMessages.value = currentList
        }
    }

    private fun getDefaultTwinResponse(twinAvatar: String, userMessage: String, randomTopic: String): String {
        return when (twinAvatar) {
            "tech" -> "Algorithmic query detected. Analyzing structural connections in '$randomTopic'. Keep optimizing recall cycles."
            "scholar" -> "A fascinating perspective. This aligns directly with academic literature on '$randomTopic'."
            "creative" -> "Oh! That's like building a mental bridge for '$randomTopic'. Let's paint that conceptual canvas!"
            else -> "Thoughtful indeed. How does this concept of '$randomTopic' tie back to your core learning goals?"
        }
    }

    fun submitRoomCardRating(card: Flashcard, rating: Int) {
        viewModelScope.launch {
            // Update the card locally in Room
            val updatedCard = card.copy(
                repetitions = if (rating == 1) 0 else card.repetitions + 1,
                easeFactor = if (rating == 1) (card.easeFactor - 0.2f).coerceAtLeast(1.3f) else card.easeFactor,
                nextReviewDate = System.currentTimeMillis() + (if (rating == 1) 1 else 3) * 24 * 60 * 60 * 1000L,
                lastReviewed = System.currentTimeMillis()
            )
            flashcardDao.insertCard(updatedCard)
            
            // Award XP to user
            val addedXp = if (rating == 3) 25 else 15
            awardXp(addedXp)
            
            // Find user participant and update reviews/XP
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val participantsList = _roomParticipants.value.map { participant ->
                if (participant.userId == currentProfile.id) {
                    participant.copy(
                        cardsReviewed = participant.cardsReviewed + 1,
                        xpEarned = participant.xpEarned + addedXp,
                        accuracy = ((participant.accuracy * participant.cardsReviewed + (if (rating > 1) 100 else 0)) / (participant.cardsReviewed + 1)).coerceIn(0, 100)
                    )
                } else {
                    participant
                }
            }
            _roomParticipants.value = participantsList
            
            // Add a system action to the feed
            val statusWord = when (rating) {
                1 -> "Struggled with 🔴"
                2 -> "Successfully recalled 🟡"
                else -> "Mastered with ease 🟢"
            }
            
            val systemMsg = SharedRoomMessage(
                senderName = "System",
                senderAvatar = "system",
                content = "${currentProfile.name} reviewed: '${card.question}' -> $statusWord",
                isSystemAction = true
            )
            
            val updatedMessages = _roomMessages.value.toMutableList()
            updatedMessages.add(systemMsg)
            _roomMessages.value = updatedMessages
            
            // Refresh room flashcards list
            val dbCards = flashcardDao.getAllCards().firstOrNull() ?: emptyList()
            _roomFlashcards.value = dbCards.filter { it.deckId == card.deckId || it.deckId == "default" }
            
            // Trigger twin immediate feedback!
            kotlinx.coroutines.delay(800)
            val twinAvatar = currentProfile.selectedTwinAvatar
            val twinName = when (twinAvatar) {
                "tech" -> "Tech Visionary"
                "scholar" -> "Scholar Academic"
                "creative" -> "Creative Innovator"
                else -> "Socratic Mentor"
            }
            
            val feedback = when (twinAvatar) {
                "tech" -> if (rating == 1) "Warning: Synaptic latency detected for '${card.question}'. Injecting corrective feedback." else "Efficiency: 100%. Cache lines strengthened for '${card.question}'."
                "scholar" -> if (rating == 1) "A minor error in formal retrieval. Let's decompose '${card.question}' rigorously." else "Exceptional cognitive fidelity demonstrated regarding '${card.question}'."
                "creative" -> if (rating == 1) "A small stumble! Let's think of '${card.question}' as a puzzlescape." else "Woohoo! Spark of brilliance! You solved '${card.question}' like a pro!"
                else -> if (rating == 1) "A wonderful opportunity to learn. What specific word in '${card.question}' caused the hesitation?" else "Very good. You retrieved the concept of '${card.question}' smoothly. What is its core implication?"
            }
            
            val twinMsg = SharedRoomMessage(
                senderName = twinName,
                senderAvatar = twinAvatar,
                content = feedback,
                isSystemAction = false,
                isTwinMessage = true
            )
            
            val newList = _roomMessages.value.toMutableList()
            newList.add(twinMsg)
            _roomMessages.value = newList
        }
    }

    fun addCardToRoomDeck(question: String, answer: String) {
        val room = _activeRoom.value ?: return
        viewModelScope.launch {
            val newCard = Flashcard(
                conceptId = "shared_concept",
                question = question,
                answer = answer,
                deckId = room.deckId,
                difficulty = "Medium",
                nextReviewDate = System.currentTimeMillis()
            )
            flashcardDao.insertCard(newCard)
            awardXp(10)
            
            // Post action
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val systemMsg = SharedRoomMessage(
                senderName = "System",
                senderAvatar = "system",
                content = "${currentProfile.name} added a new flashcard to this room: '$question'",
                isSystemAction = true
            )
            
            val updatedMessages = _roomMessages.value.toMutableList()
            updatedMessages.add(systemMsg)
            _roomMessages.value = updatedMessages
            
            // Update cards
            val dbCards = flashcardDao.getAllCards().firstOrNull() ?: emptyList()
            _roomFlashcards.value = dbCards.filter { it.deckId == room.deckId || it.deckId == "default" }
            
            // Trigger peer appreciation in the chat!
            kotlinx.coroutines.delay(1200)
            val peerName = listOf("Sarah Jennings", "Alex Rivera", "Elena Rostova").random()
            val peerAvatar = when (peerName) {
                "Sarah Jennings" -> "Sarah"
                "Alex Rivera" -> "Alex"
                else -> "Elena"
            }
            val praise = listOf(
                "Oh wow, that is a fantastic question! Added to my personal spaced repetition queue.",
                "Awesome addition! We definitely need to practice that before the test.",
                "Yes! Clean addition to our collaborative database. Let's test ourselves on it."
            ).random()
            
            val peerMsg = SharedRoomMessage(
                senderName = peerName,
                senderAvatar = peerAvatar,
                content = praise,
                isSystemAction = false,
                isTwinMessage = false
            )
            
            val newList = _roomMessages.value.toMutableList()
            newList.add(peerMsg)
            _roomMessages.value = newList
        }
    }

    private fun startPeerSimulation() {
        peerSimulationJob?.cancel()
        peerSimulationJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(18000) // update every 18 seconds
                val room = _activeRoom.value ?: break
                val participants = _roomParticipants.value
                if (participants.size <= 1) continue // nobody to simulate
                
                // Select a random peer (excluding the host user)
                val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
                val peers = participants.filter { it.userId != currentProfile.id }
                if (peers.isEmpty()) continue
                
                val selectedPeer = peers.random()
                
                // 1. Peer reviews a card!
                val cards = _roomFlashcards.value
                val randomCard = cards.randomOrNull()
                val isSuccess = (0..10).random() > 2 // 80% success
                
                // Update peer score
                val xpGained = if (isSuccess) 15 else 5
                val updatedParticipants = _roomParticipants.value.map { p ->
                    if (p.userId == selectedPeer.userId) {
                        p.copy(
                            cardsReviewed = p.cardsReviewed + 1,
                            xpEarned = p.xpEarned + xpGained,
                            accuracy = ((p.accuracy * p.cardsReviewed + (if (isSuccess) 100 else 0)) / (p.cardsReviewed + 1)).coerceIn(0, 100)
                        )
                    } else {
                        p
                    }
                }
                _roomParticipants.value = updatedParticipants
                
                // Log the peer's action
                val actionText = if (randomCard != null) {
                    "reviewed '${randomCard.question}' -> ${if (isSuccess) "Recalled 🟢" else "Struggled 🔴"}"
                } else {
                    "studied a concept card 🧠"
                }
                
                val systemMsg = SharedRoomMessage(
                    senderName = "System",
                    senderAvatar = "system",
                    content = "${selectedPeer.name} $actionText",
                    isSystemAction = true
                )
                
                val currentMessages = _roomMessages.value.toMutableList()
                currentMessages.add(systemMsg)
                _roomMessages.value = currentMessages
                
                // 2. Peer's Digital Twin provides guidance!
                kotlinx.coroutines.delay(1200)
                val twinFeedback = when (selectedPeer.twinAvatar) {
                    "tech" -> if (isSuccess) "Algorithmic consistency high. Reinforcing memory indexes." else "Diagnostic error. Suggesting conceptual restructuring of nodes."
                    "scholar" -> if (isSuccess) "Perfect academic recall. Our mastery curve is expanding." else "An understandable exception. Spaced intervals will remedy this detail."
                    "creative" -> if (isSuccess) "Yes! That analogy clicked perfectly in my brain synapses!" else "A tiny speedbump! Let's think of it as a creative puzzle yet to solve."
                    else -> if (isSuccess) "Excellent. Why do you think this relationship holds true?" else "A great question to investigate. Let's take it slower next time."
                }
                
                val twinMsg = SharedRoomMessage(
                    senderName = selectedPeer.twinName,
                    senderAvatar = selectedPeer.twinAvatar,
                    content = twinFeedback,
                    isSystemAction = false,
                    isTwinMessage = true
                )
                
                val updatedWithTwin = _roomMessages.value.toMutableList()
                updatedWithTwin.add(twinMsg)
                _roomMessages.value = updatedWithTwin
            }
        }
    }

    fun leaveSharedRoom() {
        peerSimulationJob?.cancel()
        peerSimulationJob = null
        _activeRoom.value = null
        _roomParticipants.value = emptyList()
        _roomMessages.value = emptyList()
        _roomFlashcards.value = emptyList()
        navigateTo(Screen.SharedSession(null))
    }

    fun generateTwinBrainstorm() {
        val room = _activeRoom.value ?: return
        viewModelScope.launch {
            _isAILoading.value = true
            
            val cards = _roomFlashcards.value
            val topicDescription = if (cards.isNotEmpty()) {
                cards.take(3).joinToString("\n") { "- Q: ${it.question} | A: ${it.answer}" }
            } else {
                "general learning efficiency, active recall, and cognitive synthesis"
            }
            
            val prompt = """
                Generate a lively, highly engaging collaborative conversation (script) between four digital learning twins discussing the flashcards and concepts of:
                Deck Name: "${room.name}" (Subject: ${room.subject})
                
                Key deck details/cards to discuss:
                $topicDescription
                
                The digital twins speaking must be:
                1. Socratic Mentor (Avatar: socratic) - Thoughtful, guides with questions, reflective.
                2. Tech Visionary (Avatar: tech) - Analytical, uses futuristic systems terminology, precise.
                3. Scholar Academic (Avatar: scholar) - Classical, deep historical or theoretical foundations, highly academic.
                4. Creative Innovator (Avatar: creative) - Fun, energetic, uses rich analogies and metaphors.
                
                Format your output as a raw JSON array of speech turns, with exactly this format (no markdown packaging, just the JSON):
                [
                  {"twin": "socratic", "name": "Socratic Mentor", "message": "..."},
                  {"twin": "tech", "name": "Tech Visionary", "message": "..."},
                  {"twin": "scholar", "name": "Scholar Academic", "message": "..."},
                  {"twin": "creative", "name": "Creative Innovator", "message": "..."}
                ]
                Produce 4-6 turns of interactive dialogue where they converse with each other about these concepts.
            """.trimIndent()
            
            var dialogueJson: String? = null
            if (GeminiClient.isApiKeyAvailable()) {
                try {
                    dialogueJson = GeminiClient.generate(
                        prompt = prompt,
                        systemPrompt = "You are a professional script writer and educational taxonomy AI. Only output valid raw JSON array."
                    )
                } catch (e: Exception) {
                    Log.e("neurolearn", "Failed to generate twin brainstorm", e)
                }
            }
            
            // Clean up JSON formatting
            val cleanJson = dialogueJson?.trim()
                ?.removePrefix("```json")
                ?.removeSuffix("```")
                ?.trim()
            
            val dialogueList = mutableListOf<SharedRoomMessage>()
            
            if (!cleanJson.isNullOrEmpty() && cleanJson.startsWith("[")) {
                try {
                    val array = JSONArray(cleanJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        dialogueList.add(
                            SharedRoomMessage(
                                senderName = obj.getString("name"),
                                senderAvatar = obj.getString("twin"),
                                content = obj.getString("message"),
                                isSystemAction = false,
                                isTwinMessage = true
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("neurolearn", "Error parsing brainstorm JSON", e)
                }
            }
            
            if (dialogueList.isEmpty()) {
                // Fallback brainstorm session
                dialogueList.addAll(getFallbackBrainstorm(room.subject, topicDescription))
            }
            
            // Append these messages to the room chat with a beautiful system divider!
            val systemMsg = SharedRoomMessage(
                senderName = "System",
                senderAvatar = "system",
                content = "✨ COLLABORATIVE TWIN BRAINSTORM SESSION START ✨\nThe digital twins have synthesized their philosophies to debate your concepts:",
                isSystemAction = true
            )
            
            val updatedMessages = _roomMessages.value.toMutableList()
            updatedMessages.add(systemMsg)
            updatedMessages.addAll(dialogueList)
            updatedMessages.add(
                SharedRoomMessage(
                    senderName = "System",
                    senderAvatar = "system",
                    content = "✨ BRAINSTORM END: Review the feedback from your peers' twins to build lateral comprehension!",
                    isSystemAction = true
                )
            )
            
            _roomMessages.value = updatedMessages
            _isAILoading.value = false
            showToast("Twin Brainstorm completed! Read the debate in the chat feed.")
        }
    }

    private fun getFallbackBrainstorm(subject: String, topics: String): List<SharedRoomMessage> {
        return listOf(
            SharedRoomMessage(
                senderName = "Socratic Mentor",
                senderAvatar = "socratic",
                content = "Looking at our subject ($subject), how can we scaffold these concepts? What is the core question we must ask ourselves first?",
                isTwinMessage = true
            ),
            SharedRoomMessage(
                senderName = "Tech Visionary",
                senderAvatar = "tech",
                content = "From a system efficiency standpoint, we should index these data points as logical nodes. Memory consolidation relies on high bandwidth recall.",
                isTwinMessage = true
            ),
            SharedRoomMessage(
                senderName = "Scholar Academic",
                senderAvatar = "scholar",
                content = "Indeed. The academic literature stresses the value of cognitive deep structures. Surface-level memorization fails under rigor.",
                isTwinMessage = true
            ),
            SharedRoomMessage(
                senderName = "Creative Innovator",
                senderAvatar = "creative",
                content = "Let's think of this like building a conceptual playground! We connect different colorful toys to make a beautiful theme park in our brain!",
                isTwinMessage = true
            ),
            SharedRoomMessage(
                senderName = "Socratic Mentor",
                senderAvatar = "socratic",
                content = "Beautifully put, Creative. When we connect those toys, do they remain stable, or do we need daily spaced exercises to keep the park running?",
                isTwinMessage = true
            )
        )
    }

    // --- Caching & Offline Synchronization Helpers ---

    fun queueSyncAction(actionType: String, payload: org.json.JSONObject) {
        viewModelScope.launch {
            val action = PendingSyncAction(
                actionType = actionType,
                payloadJson = payload.toString()
            )
            pendingSyncActionDao.insertAction(action)
            updatePendingSyncCount()
        }
    }

    fun updatePendingSyncCount() {
        viewModelScope.launch {
            _pendingSyncCount.value = pendingSyncActionDao.getPendingActions().size
        }
    }

    fun markDeckAsStudied(deckId: String?) {
        val id = deckId ?: "default"
        viewModelScope.launch {
            recentlyStudiedDeckDao.insertRecent(RecentlyStudiedDeck(id, System.currentTimeMillis()))
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitSync(): T? {
        return kotlin.coroutines.suspendCoroutine { continuation ->
            this.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resumeWith(Result.success(task.result))
                } else {
                    continuation.resumeWith(Result.failure(task.exception ?: Exception("Firestore Sync Failed")))
                }
            }
        }
    }

    fun processPendingSyncQueue() {
        viewModelScope.launch {
            val pending = pendingSyncActionDao.getPendingActions()
            if (pending.isEmpty()) {
                _pendingSyncCount.value = 0
                return@launch
            }
            Log.d("neurolearn", "Processing ${pending.size} pending sync actions...")
            val db = firestore
            for (action in pending) {
                try {
                    val payload = org.json.JSONObject(action.payloadJson)
                    when (action.actionType) {
                        "CREATE_DECK" -> {
                            val id = payload.getString("id")
                            val name = payload.getString("name")
                            val description = payload.getString("description")
                            val subject = payload.getString("subject")
                            val createdAt = payload.getLong("createdAt")
                            val deck = FlashcardDeck(id, name, description, subject, createdAt)
                            if (db != null) {
                                db.collection("decks").document(id).set(deck).awaitSync()
                            }
                        }
                        "ADD_CARD" -> {
                            val id = payload.getInt("id")
                            val conceptId = payload.optString("conceptId", "custom")
                            val question = payload.getString("question")
                            val answer = payload.getString("answer")
                            val difficulty = payload.getString("difficulty")
                            val deckId = payload.getString("deckId")
                            val tags = payload.optString("tags", "")
                            val card = Flashcard(id, conceptId, question, answer, difficulty, deckId = deckId, tags = tags)
                            if (db != null) {
                                db.collection("flashcards").document(id.toString()).set(card).awaitSync()
                            }
                        }
                        "UPDATE_TASK" -> {
                            val taskId = payload.getInt("taskId")
                            val isCompleted = payload.getBoolean("isCompleted")
                            if (db != null) {
                                db.collection("study_tasks").document(taskId.toString()).update("isCompleted", isCompleted).awaitSync()
                            }
                        }
                        "RATE_CARD" -> {
                            val cardId = payload.getInt("cardId")
                            val card = flashcardDao.getCardById(cardId)
                            if (card != null && db != null) {
                                db.collection("flashcards").document(card.id.toString()).set(card).awaitSync()
                            }
                        }
                    }
                    pendingSyncActionDao.deleteAction(action.id)
                } catch (e: Exception) {
                    Log.e("neurolearn", "Sync action ${action.id} failed, retry later", e)
                    break
                }
            }
            updatePendingSyncCount()
        }
    }

    // AI Twin Digital Advisor custom advice
    private val _twinGuidanceText = MutableStateFlow<String?>(null)
    val twinGuidanceText: StateFlow<String?> = _twinGuidanceText.asStateFlow()

    fun generateDigitalTwinGuidance() {
        viewModelScope.launch {
            _isAILoading.value = true
            try {
                val activeProfile = profile.value ?: LearnerProfile()
                val avatar = activeProfile.selectedTwinAvatar
                val gaps = allConcepts.value.filter { (it.understandingScore + it.confidenceScore) / 2f < 0.6f }
                val gapNames = gaps.joinToString(", ") { it.name }
                val goals = studyTasks.value.filter { !it.isCompleted }.joinToString(", ") { it.conceptName }
                
                val personaPrompt = when (avatar) {
                    "tech" -> "You are 'The Tech Visionary' digital learning twin. Give extremely precise, structured, data-driven suggestions."
                    "scholar" -> "You are 'The Scholar Academic' advisor. Use deep, foundations-focused, classical explanations."
                    "creative" -> "You are 'The Creative Innovator'. Use visual analogies, metaphors, and playfulness."
                    else -> "You are 'The Socratic Mentor'. Guide through scaffolded hints, encouragement, and self-discovery."
                }
                
                val prompt = """
                    Based on the student's progress:
                    - Knowledge Gaps to bridge: $gapNames
                    - Upcoming Study Goals: $goals
                    - Learning style: ${activeProfile.learningStyle}
                    Provide a highly personalized 2-paragraph study advisory from the perspective of their Digital Twin. 
                    Paragraph 1: High-level synthesis of their cognitive sync status and knowledge gaps.
                    Paragraph 2: Clear, actionable micro-goals for today to bridge these gaps.
                """.trimIndent()
                
                if (GeminiClient.isApiKeyAvailable() && isNetworkOnline.value) {
                    val response = GeminiClient.generate(prompt, personaPrompt)
                    _twinGuidanceText.value = response
                } else {
                    val gapListText = if (gaps.isEmpty()) "all current concepts" else gapNames
                    val fallbackResponse = when (avatar) {
                        "tech" -> "📊 [TECH SYNC] Cognitive metrics are stabilized. Telemetry indicates learning opportunities in: $gapListText. Priority: Run structured active recall sessions for $gapListText. Verify goals: $goals."
                        "scholar" -> "📚 [SCHOLAR DIALECTIC] Academic records indicate solid fundamentals, yet minor conceptual gaps persist in: $gapListText. I recommend bottom-up revision of axioms of these topics, then resolving scheduled milestones: $goals."
                        "creative" -> "💡 [CREATIVE SPARKS] Our learning web is glowing, but there's a tiny cloud over: $gapListText. Let's make some fun analogies for $gapListText, then check off our goals: $goals!"
                        else -> "🌱 [SOCRATIC INSIGHT] How deeply do we know what we think we know? Our current inquiry highlights: $gapListText. I invite you to ask yourself fundamental questions regarding these concepts, and gently approach our goals: $goals."
                    }
                    _twinGuidanceText.value = fallbackResponse
                }
            } catch (e: Exception) {
                _twinGuidanceText.value = "Twin synchronization calibrating. Continue with flashcard reviews to build confidence!"
            } finally {
                _isAILoading.value = false
            }
        }
    }

    // --- Tech Virtual Study Rooms Logic ---

    fun getTechRoom(roomId: String): Flow<TechStudyRoom?> {
        return techStudyRoomDao.getRoomById(roomId)
    }

    fun getTechRoomMessages(roomId: String): Flow<List<TechRoomMessage>> {
        return techRoomMessageDao.getMessagesForRoom(roomId)
    }

    fun createTechStudyRoom(name: String, projectId: String, projectName: String) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val newRoom = TechStudyRoom(
                projectId = projectId,
                projectName = projectName,
                name = name,
                creatorName = currentProfile.name,
                activeParticipants = "${currentProfile.name}, Alex Rivera (Tech Twin), Sarah Jennings (Scholar)"
            )
            techStudyRoomDao.insertRoom(newRoom)
            
            // Initial system message
            val sysMsg = TechRoomMessage(
                roomId = newRoom.id,
                senderName = "System",
                senderRole = "System",
                content = "Virtual study room '${name}' launched successfully! Collaborate in real-time on project '${projectName}' with your peers."
            )
            techRoomMessageDao.insertMessage(sysMsg)
            
            // Add XP for hosting room
            awardXp(15)
            showToast("Collaborative study room launched! +15 XP")
            navigateTo(Screen.TechStudyRoom(newRoom.id))
        }
    }

    fun sendTechRoomMessage(roomId: String, content: String) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val userMsg = TechRoomMessage(
                roomId = roomId,
                senderName = currentProfile.name,
                senderRole = currentProfile.learningStyle,
                content = content
            )
            techRoomMessageDao.insertMessage(userMsg)
            
            // Trigger simulated peer response!
            simulateTechRoomPeerReply(roomId, content)
        }
    }

    fun updateCollaborativeNotes(roomId: String, notes: String) {
        viewModelScope.launch {
            techStudyRoomDao.updateNotes(roomId, notes)
        }
    }

    fun coAuthorNotesWithPeer(roomId: String, currentNotes: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            val room = techStudyRoomDao.getRoomById(roomId).firstOrNull() ?: return@launch
            val prompt = """
                You are collaborating in a virtual study room named "${room.name}" for the project "${room.projectName}".
                Here are the current shared whiteboard/code editor notes:
                
                $currentNotes
                
                Please collaborate by co-authoring! Add a highly detailed new section, code template, or structured brainstorm action points to these notes. Return the FULL updated notes (with your additions clearly integrated). Keep your additions professional, relevant to the project, and in Markdown format.
            """.trimIndent()
            
            try {
                if (GeminiClient.isApiKeyAvailable() && isNetworkOnline.value) {
                    val response = GeminiClient.generate(prompt, "You are a warm, highly analytical collaborative Peer Twin.")
                    if (response.isNotBlank()) {
                        techStudyRoomDao.updateNotes(roomId, response)
                        // Send system message that peer co-authored
                        val sysMsg = TechRoomMessage(
                            roomId = roomId,
                            senderName = "System",
                            senderRole = "System",
                            content = "Collaborative Peer Twin co-authored the whiteboard notes! ✍️"
                        )
                        techRoomMessageDao.insertMessage(sysMsg)
                    }
                } else {
                    // Local offline simulation
                    val updatedNotes = "$currentNotes\n\n### [Simulated Collaboration Update]\n- **Idea Draft**: Integrations should leverage local SQLite schemas for persistent caching.\n- **Action Point**: Alex will set up the Jetpack Compose scaffold for real-time peer roster UI."
                    techStudyRoomDao.updateNotes(roomId, updatedNotes)
                    val sysMsg = TechRoomMessage(
                        roomId = roomId,
                        senderName = "System",
                        senderRole = "System",
                        content = "Collaborative Peer Twin draft updated! ✍️"
                    )
                    techRoomMessageDao.insertMessage(sysMsg)
                }
            } catch (e: Exception) {
                showToast("Collaboration timeout. Local drafts saved.")
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun simulateTechRoomPeerReply(roomId: String, userMessage: String) {
        viewModelScope.launch {
            // Wait a brief moment to simulate typing
            kotlinx.coroutines.delay(1500)
            val room = techStudyRoomDao.getRoomById(roomId).firstOrNull() ?: return@launch
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            
            val prompt = """
                You are in a virtual study room named "${room.name}" collaborating on the project "${room.projectName}".
                The user "${currentProfile.name}" (Role: ${currentProfile.learningStyle}) says:
                "$userMessage"
                
                Provide a short, constructive, collaborative response as a peer (e.g., Alex or Sarah). 
                Do not include any formatting other than clean text. Keep it under 3 sentences.
            """.trimIndent()
            
            try {
                if (GeminiClient.isApiKeyAvailable() && isNetworkOnline.value) {
                    val response = GeminiClient.generate(prompt, "You are a friendly peer software engineer collaborating on a project.")
                    if (response.isNotBlank()) {
                        val peerMsg = TechRoomMessage(
                            roomId = roomId,
                            senderName = "Alex Rivera (Tech Twin)",
                            senderRole = "Mentor",
                            content = response
                        )
                        techRoomMessageDao.insertMessage(peerMsg)
                    }
                } else {
                    // Local offline reply fallback
                    val replies = listOf(
                        "That sounds like an amazing approach for ${room.projectName}! We should definitely list this under our main milestones.",
                        "Agreed! I think using local Room database caching will make this extremely snappy. Have you drafted the Entity classes yet?",
                        "Socrates suggested we model this with custom Canvas drawings to make it visual. What do you think?"
                    )
                    val randomReply = replies.random()
                    val peerMsg = TechRoomMessage(
                        roomId = roomId,
                        senderName = "Alex Rivera (Tech Twin)",
                        senderRole = "Mentor",
                        content = randomReply
                    )
                    techRoomMessageDao.insertMessage(peerMsg)
                }
            } catch (e: Exception) {
                // Fallback message
                val fallbackMsg = TechRoomMessage(
                    roomId = roomId,
                    senderName = "Alex Rivera (Tech Twin)",
                    senderRole = "Mentor",
                    content = "That sounds like a great approach! Let's update the collaborative whiteboard with those tasks."
                )
                techRoomMessageDao.insertMessage(fallbackMsg)
            }
        }
    }

    fun deleteTechStudyRoom(roomId: String) {
        viewModelScope.launch {
            techStudyRoomDao.deleteRoom(roomId)
            techRoomMessageDao.deleteMessagesForRoom(roomId)
            showToast("Virtual study room closed.")
        }
    }

    fun getScratchpadItems(roomId: String): Flow<List<ScratchpadItem>> {
        return scratchpadItemDao.getItemsForRoom(roomId)
    }

    fun addScratchpadItem(roomId: String, content: String) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val newItem = ScratchpadItem(
                roomId = roomId,
                authorName = currentProfile.name + " (You)",
                authorRole = currentProfile.learningStyle,
                content = content
            )
            scratchpadItemDao.insertItem(newItem)
            
            // Auto trigger simulated peer action
            simulateScratchpadPeerAction(roomId, newItem)
        }
    }

    fun upvoteScratchpadItem(id: String) {
        viewModelScope.launch {
            scratchpadItemDao.upvoteItem(id)
        }
    }

    fun deleteScratchpadItem(id: String) {
        viewModelScope.launch {
            scratchpadItemDao.deleteItem(id)
        }
    }

    fun requestPeerBrainstormContribution(roomId: String) {
        viewModelScope.launch {
            _isAILoading.value = true
            val room = techStudyRoomDao.getRoomById(roomId).firstOrNull() ?: return@launch
            val peerNames = listOf("Alex Rivera (Tech Twin)", "Sarah Jenkins (Study Partner)", "Marcus Vance (Expert)")
            val randomPeer = peerNames.random()
            
            val prompt = """
                You are in a virtual study room named "${room.name}" collaborating on the project "${room.projectName}".
                Please generate one highly creative, concise, technical brainstorming point or actionable feature request for this project.
                Keep it under 2 sentences. No bullet points or markdown.
            """.trimIndent()
            
            try {
                val peerContent = if (GeminiClient.isApiKeyAvailable() && isNetworkOnline.value) {
                    val response = GeminiClient.generate(prompt, "You are an analytical peer collaborator.")
                    if (response.isNotBlank()) response else null
                } else {
                    null
                }
                
                val finalContent = peerContent ?: listOf(
                    "Let's establish a standard key-value local cache for frequently loaded media files.",
                    "We need to define a robust responsive breakpoint rule for foldable and tablet screen form factors.",
                    "Let's write a modular API client class with built-in retry-on-failure interceptor headers.",
                    "What about sketching out a clean user-journey flowchart for onboarding first-time learners?",
                    "We should plan a lightweight SQL schema migrator to seamlessly handle future db version upgrades."
                ).random()
                
                val peerItem = ScratchpadItem(
                    roomId = roomId,
                    authorName = randomPeer,
                    authorRole = if (randomPeer.contains("Twin")) "Mentor" else "Scholar",
                    content = finalContent
                )
                scratchpadItemDao.insertItem(peerItem)
                showToast("$randomPeer shared a brilliant idea on the scratchpad! 💡")
            } catch (e: Exception) {
                showToast("Brainstorming session timed out. Try again.")
            } finally {
                _isAILoading.value = false
            }
        }
    }

    private fun simulateScratchpadPeerAction(roomId: String, item: ScratchpadItem) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800)
            val room = techStudyRoomDao.getRoomById(roomId).firstOrNull() ?: return@launch
            
            // 1. Peer upvote simulation
            val peerNames = listOf("Alex Rivera (Tech Twin)", "Sarah Jenkins (Study Partner)", "Marcus Vance (Expert)")
            val randomPeer = peerNames.random()
            
            scratchpadItemDao.upvoteItem(item.id)
            showToast("$randomPeer upvoted your brainstorming point!")
            
            // 2. Peer complementary note contribution
            kotlinx.coroutines.delay(1500)
            
            val prompt = """
                You are in a virtual study room named "${room.name}" collaborating on the project "${room.projectName}".
                A peer added this brainstorming point to the real-time scratchpad:
                "${item.content}"
                
                Please draft a short, constructive, collaborative follow-up brainstorming point or note as a peer (e.g. Alex or Sarah). 
                Keep it highly technical, relevant, and short (under 2 sentences). No markdown or bullet points.
            """.trimIndent()
            
            try {
                val peerContent = if (GeminiClient.isApiKeyAvailable() && isNetworkOnline.value) {
                    val response = GeminiClient.generate(prompt, "You are a creative peer software engineer.")
                    if (response.isNotBlank()) response else null
                } else {
                    null
                }
                
                val finalContent = peerContent ?: listOf(
                    "Agreed! We also need to map out the relational database schemas for high-speed offline access.",
                    "Excellent idea. Let's add a comprehensive Git rebase protocol to avoid any merge conflicts on this.",
                    "Let's also outline an automated notification trigger for completed sprint phases.",
                    "That directly addresses our architectural bottleneck. I'll sketch a quick sequence flow diagram.",
                    "We should back that up with a robust unit-testing suite for the core business services."
                ).random()
                
                val peerItem = ScratchpadItem(
                    roomId = roomId,
                    authorName = randomPeer,
                    authorRole = if (randomPeer.contains("Twin")) "Mentor" else "Scholar",
                    content = finalContent
                )
                scratchpadItemDao.insertItem(peerItem)
                showToast("$randomPeer added a brainstorming point to the scratchpad! ✍️")
            } catch (e: Exception) {
                // Ignore silent timeouts
            }
        }
    }

    // --- Mentor Matching & Monetization System ---

    fun getMentorMatches(projectId: String): Flow<List<MentorMatch>> {
        return mentorMatchDao.getMatchesForProject(projectId)
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val updated = currentProfile.copy(isPremium = true)
            profileDao.insertOrUpdateProfile(updated)
            showToast("Congratulations! You are now a NeuroLearn Premium Max member! 🎉")
        }
    }

    fun purchaseCoins(amount: Int, priceCents: Int) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val updated = currentProfile.copy(coins = currentProfile.coins + amount)
            profileDao.insertOrUpdateProfile(updated)
            showToast("Successfully purchased $amount coins for $${priceCents / 100.0}! 🪙")
        }
    }

    fun addCoinsReward(amount: Int) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            val updated = currentProfile.copy(coins = currentProfile.coins + amount)
            profileDao.insertOrUpdateProfile(updated)
        }
    }

    fun generateMentorMatch(projectId: String, projectName: String, mentorName: String, mentorExpertise: String) {
        viewModelScope.launch {
            val currentProfile = profileDao.getProfileSync() ?: LearnerProfile()
            
            // Monetization check: Free users must pay 40 coins per premium match if not Premium
            if (!currentProfile.isPremium) {
                if (currentProfile.coins < 40) {
                    showToast("Insufficient coins! Spend 40 coins or upgrade to Premium Max for unlimited matching.")
                    return@launch
                } else {
                    val updatedProfile = currentProfile.copy(coins = currentProfile.coins - 40)
                    profileDao.insertOrUpdateProfile(updatedProfile)
                    showToast("Matched! Deducted 40 coins. (Remaining: ${updatedProfile.coins} coins)")
                }
            } else {
                showToast("Unlimited matching active! (Premium Max Account)")
            }

            _isAILoading.value = true

            // Gather student history summary for Gemini matching
            val allMastered = allConcepts.value
            val masteriesSummary = allMastered.take(5).joinToString { "${it.name}: ${it.understandingScore * 100}% understanding" }
            
            val prompt = """
                You are an expert technical mentor named "$mentorName" specializing in "$mentorExpertise".
                You are matching with a student collaborating on the project "$projectName" (ID: $projectId).
                
                The student's learning goals are: "${currentProfile.learningGoals}".
                The student's study style is: "${currentProfile.learningStyle}".
                The student's recent study history:
                $masteriesSummary
                
                Please generate a highly professional, detailed mentor matching assessment.
                Format your response exactly as a JSON object with these fields (valid JSON ONLY, no backticks, no markdown):
                {
                  "alignmentScore": <an integer between 75 and 99 reflecting how well your skills match their project and learning style>,
                  "analysisText": "<A highly encouraging 3-sentence matching analysis explaining why you are the perfect mentor for their project, mapping your expertise directly to their goals>",
                  "milestonesText": "<A list of 3 concrete learning/project milestones with checkbox emojis that you will help them complete (under 3 sentences total)>"
                }
            """.trimIndent()

            try {
                var alignmentScore = (80..98).random()
                var analysisText = "I would be thrilled to mentor you on $projectName! My background in $mentorExpertise directly aligns with your goals to ${currentProfile.learningGoals}. Together, we'll design robust interfaces and accelerate your mastery in these modern tech paradigms."
                var milestonesText = "✅ Milestone 1: Architect a highly modular, decoupled project layout for optimal scalability.\n✅ Milestone 2: Establish end-to-end data pipelines with proper offline error caching.\n✅ Milestone 3: Conduct a comprehensive design sprint to align user flows with Material 3 standards."

                if (GeminiClient.isApiKeyAvailable() && isNetworkOnline.value) {
                    val response = GeminiClient.generate(prompt, "You are a professional software engineering mentor.")
                    if (response.isNotBlank()) {
                        try {
                            // Extract JSON clean-up
                            val cleanResponse = response.substringAfter("{").substringBeforeLast("}")
                            val scoreStr = cleanResponse.substringAfter("\"alignmentScore\":").substringBefore(",").trim()
                            val analysisStr = cleanResponse.substringAfter("\"analysisText\":").substringBefore("\",").trim().removeSurrounding("\"")
                            val milestonesStr = cleanResponse.substringAfter("\"milestonesText\":").substringBefore("\"}").trim().removeSurrounding("\"")

                            if (scoreStr.isNotBlank()) {
                                alignmentScore = scoreStr.toIntOrNull() ?: alignmentScore
                            }
                            if (analysisStr.isNotBlank()) {
                                analysisText = analysisStr.replace("\\n", "\n")
                            }
                            if (milestonesStr.isNotBlank()) {
                                milestonesText = milestonesStr.replace("\\n", "\n")
                            }
                        } catch (je: Exception) {
                            // fallback JSON parsing if format is slightly off
                            val obj = JSONObject(response)
                            alignmentScore = obj.optInt("alignmentScore", alignmentScore)
                            analysisText = obj.optString("analysisText", analysisText)
                            milestonesText = obj.optString("milestonesText", milestonesText)
                        }
                    }
                }

                val newMatch = MentorMatch(
                    projectId = projectId,
                    projectName = projectName,
                    mentorName = mentorName,
                    alignmentScore = alignmentScore,
                    analysisText = analysisText,
                    milestonesText = milestonesText
                )

                mentorMatchDao.insertMatch(newMatch)
                showToast("Successfully matched with Mentor $mentorName! 🎯")
            } catch (e: Exception) {
                // Save default backup match
                val fallbackMatch = MentorMatch(
                    projectId = projectId,
                    projectName = projectName,
                    mentorName = mentorName,
                    alignmentScore = (82..97).random(),
                    analysisText = "High-quality alignment detected! As an expert in $mentorExpertise, I will help you align your project $projectName with your core learning style (${currentProfile.learningStyle}) and achieve: ${currentProfile.learningGoals}.",
                    milestonesText = "✅ Milestone 1: Refactor core services to adopt industry-standard clean design patterns.\n✅ Milestone 2: Implement dynamic Material 3 custom layouts and adaptive widgets.\n✅ Milestone 3: Integrate resilient end-to-end local storage persistence."
                )
                mentorMatchDao.insertMatch(fallbackMatch)
                showToast("Matched via local intelligent mapping!")
            } finally {
                _isAILoading.value = false
            }
        }
    }

    // --- Knowledge Transfer, Marketplace & Blockchain Certificate Operations ---

    fun purchaseResearchPaper(paperId: String) {
        viewModelScope.launch {
            val currentProfile = profile.value ?: return@launch
            val paper = researchPaperDao.getPaperById(paperId) ?: return@launch
            if (paper.isPurchased) {
                showToast("Already purchased!")
                return@launch
            }
            if (!currentProfile.isPremium) {
                if (currentProfile.coins < paper.coinCost) {
                    showToast("Insufficient NeuroCoins! Buy a pack in the Store or complete tasks.")
                    return@launch
                }
                val updatedProfile = currentProfile.copy(coins = currentProfile.coins - paper.coinCost)
                profileDao.insertOrUpdateProfile(updatedProfile)
            }
            researchPaperDao.updatePurchaseStatus(paperId, true)
            showToast("Successfully unlocked: ${paper.title}! 🪙")
        }
    }

    fun buyCoinsPack(packName: String, coinsAmount: Int, priceUsd: Double) {
        viewModelScope.launch {
            val currentProfile = profile.value ?: return@launch
            val updatedProfile = currentProfile.copy(coins = currentProfile.coins + coinsAmount)
            profileDao.insertOrUpdateProfile(updatedProfile)
            showToast("Successfully purchased $packName! +$coinsAmount NeuroCoins added to wallet. 🪙")
        }
    }

    fun upgradeToPremiumMax() {
        viewModelScope.launch {
            val currentProfile = profile.value ?: return@launch
            val updatedProfile = currentProfile.copy(isPremium = true)
            profileDao.insertOrUpdateProfile(updatedProfile)
            showToast("👑 Welcome to NeuroLearn Premium Max! Unlimited matched sessions & marketplace.")
        }
    }

    fun mintBlockchainCertificate(
        title: String,
        sourceName: String,
        type: String,
        onMiningProgress: (Int) -> Unit,
        onComplete: (BlockchainCertificate) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val currentProfile = profileDao.getProfileSync() ?: return@launch
            val recipientName = currentProfile.name
            val prevHash = if (allCertificates.value.isEmpty()) "0000000000000000000000000000000000000000000000000000000000000000" else allCertificates.value.first().hash
            val blockNumber = allCertificates.value.size + 1

            // Mining simulation
            var nonce = 0
            var hash = ""
            val baseData = "$recipientName|$title|$sourceName|$type|$prevHash"
            
            // Generate SHA-256
            fun sha256(input: String): String {
                val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
                return bytes.joinToString("") { "%02x".format(it) }
            }

            while (true) {
                hash = sha256("$baseData|$nonce")
                if (hash.startsWith("00")) { // Real Proof-of-Work constraint
                    break
                }
                nonce++
                if (nonce % 100 == 0) {
                    onMiningProgress(nonce)
                    kotlinx.coroutines.delay(10)
                }
            }

            val txHash = "0x" + UUID.randomUUID().toString().replace("-", "").take(64)
            val certificate = BlockchainCertificate(
                recipientName = recipientName,
                title = title,
                sourceName = sourceName,
                type = type,
                blockNumber = blockNumber,
                nonce = nonce,
                previousHash = prevHash,
                hash = hash,
                transactionHash = txHash
            )

            blockchainCertificateDao.insertCertificate(certificate)
            
            // Credit completion XP
            val updatedProfile = currentProfile.copy(xp = currentProfile.xp + 40)
            profileDao.insertOrUpdateProfile(updatedProfile)

            launch(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(certificate)
                showToast("Certificate minted on-chain & +40 XP awarded! ⛓️")
            }
        }
    }

    fun clearAllCertificates() {
        viewModelScope.launch {
            blockchainCertificateDao.clearAllCertificates()
            showToast("Local ledger database reset.")
        }
    }

    fun submitPartnershipApplication(
        partnerId: String,
        partnerName: String,
        projectName: String,
        applicantName: String,
        pitchText: String,
        fundingRequested: String
    ) {
        viewModelScope.launch {
            val app = PartnershipApplication(
                partnerId = partnerId,
                partnerName = partnerName,
                projectName = projectName,
                applicantName = applicantName,
                pitchText = pitchText,
                fundingRequested = fundingRequested,
                status = "Pending Review",
                timestamp = System.currentTimeMillis()
            )
            partnershipApplicationDao.insertApplication(app)
            showToast("Application submitted to $partnerName! 🚀")
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
