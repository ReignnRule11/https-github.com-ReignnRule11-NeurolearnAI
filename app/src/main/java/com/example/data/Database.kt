package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// --- Entities ---

@Entity(tableName = "learner_profile")
data class LearnerProfile(
    @PrimaryKey val id: String = "user_default",
    val name: String = "Learner",
    val email: String = "user@neurolearn.ai",
    val learningGoals: String = "Master core STEM concepts and ace exams",
    val curriculum: String = "Advanced / College Prep",
    val subjects: String = "Calculus, Computer Science, Chemistry, Product Management, Software Development, Web3 & Blockchain, E-commerce, Business Analysis, Product Design, Project Management, Digital Marketing, Data Analysis",
    val targetExam: String = "AP / Board Exams",
    val availableStudyTime: Int = 45, // minutes per day
    val diagnosticScore: Float = 0.0f, // 0.0 to 1.0
    val learningStyle: String = "Conceptual & Step-by-Step",
    val streak: Int = 1,
    val lastActiveDate: Long = System.currentTimeMillis(),
    val xp: Int = 50,
    val level: Int = 1,
    val isLoggedIn: Boolean = false,
    val selectedTwinAvatar: String = "socratic",
    val role: String = "Learner", // "Learner", "Instructor", "Admin"
    val cardsReviewedCount: Int = 0,
    val quizzesCompletedCount: Int = 0,
    val isPremium: Boolean = false,
    val coins: Int = 150,
    val dailyGoalType: String = "cards", // "cards", "quizzes", "xp"
    val dailyGoalTarget: Int = 10,
    val dailyGoalProgress: Int = 0,
    val weeklyGoalType: String = "cards", // "cards", "quizzes", "xp"
    val weeklyGoalTarget: Int = 50,
    val weeklyGoalProgress: Int = 0
)

@Entity(tableName = "concept_mastery")
data class ConceptMastery(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val prerequisites: String, // Comma-separated concept IDs
    val understandingScore: Float = 0.0f, // 0.0 to 1.0
    val retentionScore: Float = 0.0f, // 0.0 to 1.0
    val confidenceScore: Float = 0.0f, // 0.0 to 1.0
    val predictedExamPerformance: Float = 0.0f, // 0.0 to 1.0
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val lastReviewed: Long = 0L,
    val nextReviewDate: Long = 0L
)

@Entity(tableName = "flashcard_decks")
data class FlashcardDeck(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val subject: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conceptId: String,
    val question: String,
    val answer: String,
    val difficulty: String = "Medium",
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewed: Long = 0L,
    val deckId: String? = "default",
    val tags: String = ""
)

data class FlashcardRatingResult(
    val question: String,
    val answer: String,
    val rating: Int // 1 = Hard, 2 = Good, 3 = Easy
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conceptId: String,
    val conceptName: String,
    val subject: String,
    val dueDate: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val xpAwarded: Int = 15,
    val deckId: String? = null,
    val taskType: String = "concept" // "concept", "deck", "custom"
)

@Entity(tableName = "active_recall_sessions")
data class ActiveRecallSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: String?,
    val deckName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val easyCount: Int,
    val goodCount: Int,
    val hardCount: Int,
    val averageScore: Float, // 0.0f to 100.0f
    val summaryText: String
)

@Entity(tableName = "verbal_recall_evaluations")
data class VerbalRecallEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val deckId: String?,
    val question: String,
    val expectedAnswer: String,
    val spokenAnswer: String,
    val score: Int, // 0 to 100
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_study_progress")
data class DailyStudyProgress(
    @PrimaryKey val dateKey: String, // e.g. "2026-07-13"
    val timestamp: Long = System.currentTimeMillis(),
    val cardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val xpGained: Int = 0,
    val studyMinutes: Int = 0
)

// --- DAOs ---

@Dao
interface ActiveRecallSessionDao {
    @Query("SELECT * FROM active_recall_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ActiveRecallSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ActiveRecallSession)

    @Query("DELETE FROM active_recall_sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)
}

@Dao
interface VerbalRecallEvaluationDao {
    @Query("SELECT * FROM verbal_recall_evaluations ORDER BY timestamp DESC")
    fun getAllEvaluations(): Flow<List<VerbalRecallEvaluation>>

    @Query("SELECT * FROM verbal_recall_evaluations WHERE deckId = :deckId ORDER BY timestamp DESC")
    fun getEvaluationsForDeck(deckId: String): Flow<List<VerbalRecallEvaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: VerbalRecallEvaluation)
}

@Dao
interface DailyStudyProgressDao {
    @Query("SELECT * FROM daily_study_progress WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getProgressForDate(dateKey: String): DailyStudyProgress?

    @Query("SELECT * FROM daily_study_progress ORDER BY timestamp DESC")
    fun getAllProgressLogs(): Flow<List<DailyStudyProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: DailyStudyProgress)
}

@Dao
interface LearnerProfileDao {
    @Query("SELECT * FROM learner_profile WHERE id = :id LIMIT 1")
    fun getProfile(id: String = "user_default"): Flow<LearnerProfile?>

    @Query("SELECT * FROM learner_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileSync(id: String = "user_default"): LearnerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: LearnerProfile)
}

@Dao
interface ConceptMasteryDao {
    @Query("SELECT * FROM concept_mastery")
    fun getAllConcepts(): Flow<List<ConceptMastery>>

    @Query("SELECT * FROM concept_mastery WHERE id = :id LIMIT 1")
    suspend fun getConceptById(id: String): ConceptMastery?

    @Query("SELECT * FROM concept_mastery WHERE subject = :subject")
    fun getConceptsBySubject(subject: String): Flow<List<ConceptMastery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcept(concept: ConceptMastery)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllConcepts(concepts: List<ConceptMastery>)

    @Query("UPDATE concept_mastery SET understandingScore = :understanding, retentionScore = :retention, confidenceScore = :confidence, predictedExamPerformance = :predicted, lastReviewed = :timestamp, nextReviewDate = :nextReview WHERE id = :id")
    suspend fun updateScores(id: String, understanding: Float, retention: Float, confidence: Float, predicted: Float, timestamp: Long, nextReview: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY nextReviewDate ASC")
    fun getAllCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    fun getDueCards(currentTime: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE conceptId = :conceptId")
    fun getCardsByConcept(conceptId: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    fun getCardsByDeck(deckId: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    fun getDueCardsByDeck(deckId: String, currentTime: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Flashcard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCards(cards: List<Flashcard>)

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Int): Flashcard?

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteCard(id: Int)
}

@Dao
interface FlashcardDeckDao {
    @Query("SELECT * FROM flashcard_decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<FlashcardDeck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeck)

    @Query("DELETE FROM flashcard_decks WHERE id = :id")
    suspend fun deleteDeck(id: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)
}

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<StudyTask>)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, completed: Boolean)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Query("DELETE FROM study_tasks")
    suspend fun clearAllTasks()
}

// --- Database Configuration & Pre-seeding ---

@Database(
    entities = [
        LearnerProfile::class,
        ConceptMastery::class,
        Flashcard::class,
        ChatMessage::class,
        StudyTask::class,
        FlashcardDeck::class,
        TechProject::class,
        ProjectComment::class,
        SavedPhrase::class,
        RecentlyStudiedDeck::class,
        PendingSyncAction::class,
        TechStudyRoom::class,
        TechRoomMessage::class,
        ScratchpadItem::class,
        MentorMatch::class,
        ResearchPaper::class,
        BlockchainCertificate::class,
        AccreditedExamQuestion::class,
        PlatformPartner::class,
        PartnershipApplication::class,
        ProjectTask::class,
        TalentProfile::class,
        TalentEngagement::class,
        GlobalInternship::class,
        InternshipPlacement::class,
        ActiveRecallSession::class,
        VerbalRecallEvaluation::class,
        DailyStudyProgress::class
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learnerProfileDao(): LearnerProfileDao
    abstract fun conceptMasteryDao(): ConceptMasteryDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun flashcardDeckDao(): FlashcardDeckDao
    abstract fun techProjectDao(): TechProjectDao
    abstract fun projectCommentDao(): ProjectCommentDao
    abstract fun savedPhraseDao(): SavedPhraseDao
    abstract fun recentlyStudiedDeckDao(): RecentlyStudiedDeckDao
    abstract fun pendingSyncActionDao(): PendingSyncActionDao
    abstract fun techStudyRoomDao(): TechStudyRoomDao
    abstract fun techRoomMessageDao(): TechRoomMessageDao
    abstract fun scratchpadItemDao(): ScratchpadItemDao
    abstract fun mentorMatchDao(): MentorMatchDao
    abstract fun researchPaperDao(): ResearchPaperDao
    abstract fun blockchainCertificateDao(): BlockchainCertificateDao
    abstract fun accreditedExamQuestionDao(): AccreditedExamQuestionDao
    abstract fun platformPartnerDao(): PlatformPartnerDao
    abstract fun partnershipApplicationDao(): PartnershipApplicationDao
    abstract fun projectTaskDao(): ProjectTaskDao
    abstract fun talentProfileDao(): TalentProfileDao
    abstract fun talentEngagementDao(): TalentEngagementDao
    abstract fun globalInternshipDao(): GlobalInternshipDao
    abstract fun internshipPlacementDao(): InternshipPlacementDao
    abstract fun activeRecallSessionDao(): ActiveRecallSessionDao
    abstract fun verbalRecallEvaluationDao(): VerbalRecallEvaluationDao
    abstract fun dailyStudyProgressDao(): DailyStudyProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neurolearn_db"
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    preseedDatabase(database)
                }
            }
        }

        private suspend fun preseedDatabase(db: AppDatabase) {
            // 0. Preseed decks
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "default",
                    name = "General Knowledge",
                    description = "Default deck for auto-synthesized and diagnostic flashcards."
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_product_management",
                    name = "Product Management Essentials 🎯",
                    description = "Learn the core concepts of product management, lifecycles, and user-centric planning.",
                    subject = "Product Management"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_software_development",
                    name = "Software Development Masterclass 💻",
                    description = "Master software development workflows, version control, and API architectures.",
                    subject = "Software Development"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_web3_blockchain",
                    name = "Web3 & Blockchain Blueprint ⛓️",
                    description = "Understand decentralized technologies, cryptography, and smart contract development.",
                    subject = "Web3 & Blockchain"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_ecommerce",
                    name = "E-commerce Strategies 🛍️",
                    description = "Discover the business metrics, models, and optimization tools that power online stores.",
                    subject = "E-commerce"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_business_analysis",
                    name = "Business Analysis Toolkit 📊",
                    description = "Learn tools and techniques to gather system requirements and analyze business gaps.",
                    subject = "Business Analysis"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_product_design",
                    name = "Product Design Studio 🎨",
                    description = "Study visual foundations, design systems, and user experience paradigms.",
                    subject = "Product Design"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_project_management",
                    name = "Agile Project Management ⚙️",
                    description = "Explore agile methodologies, task scheduling, Gantt charting, and risk mitigation.",
                    subject = "Project Management"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_digital_marketing",
                    name = "Digital Marketing Accelerator 📣",
                    description = "Master SEO, paid ads campaigns, user acquisitions, and content strategies.",
                    subject = "Digital Marketing"
                )
            )
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "deck_data_analysis",
                    name = "Data Analysis & SQL 📈",
                    description = "Deep-dive into database querying, data manipulation, and visual storytelling.",
                    subject = "Data Analysis"
                )
            )

            // 1. Preseed default learner profile
            db.learnerProfileDao().insertOrUpdateProfile(LearnerProfile())

            // 2. Preseed concept mastery (Knowledge Graph)
            val concepts = listOf(
                // Calculus Track
                ConceptMastery("functions", "Functions & Graphs", "Calculus", "", 0.8f, 0.75f, 0.7f, 0.78f, "Easy"),
                ConceptMastery("limits", "Limits & Continuity", "Calculus", "functions", 0.6f, 0.5f, 0.55f, 0.58f, "Medium"),
                ConceptMastery("derivatives", "Derivatives & Rates of Change", "Calculus", "limits", 0.3f, 0.2f, 0.25f, 0.28f, "Medium"),
                ConceptMastery("integration", "Integrals & Area", "Calculus", "derivatives", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),
                ConceptMastery("differential_equations", "Differential Equations", "Calculus", "integration", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),

                // Computer Science Track
                ConceptMastery("variables", "Variables & Data Types", "Computer Science", "", 0.9f, 0.85f, 0.9f, 0.88f, "Easy"),
                ConceptMastery("control_flow", "Control Flow & Loops", "Computer Science", "variables", 0.75f, 0.7f, 0.8f, 0.76f, "Easy"),
                ConceptMastery("functions_cs", "Functions & Scope", "Computer Science", "control_flow", 0.5f, 0.45f, 0.5f, 0.48f, "Medium"),
                ConceptMastery("data_structures", "Linear Data Structures", "Computer Science", "functions_cs", 0.2f, 0.15f, 0.2f, 0.18f, "Medium"),
                ConceptMastery("algorithms", "Sorting & Searching Algorithms", "Computer Science", "data_structures", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),

                // Chemistry Track
                ConceptMastery("atoms", "Atomic Structure", "Chemistry", "", 0.85f, 0.8f, 0.85f, 0.82f, "Easy"),
                ConceptMastery("periodic_table", "The Periodic Table", "Chemistry", "atoms", 0.7f, 0.65f, 0.7f, 0.68f, "Easy"),
                ConceptMastery("chemical_bonds", "Chemical Bonding", "Chemistry", "periodic_table", 0.4f, 0.35f, 0.3f, 0.36f, "Medium"),
                ConceptMastery("reactions", "Chemical Reactions", "Chemistry", "chemical_bonds", 0.1f, 0.05f, 0.1f, 0.08f, "Medium"),
                ConceptMastery("stoichiometry", "Stoichiometry & Mole Concept", "Chemistry", "reactions", 0.0f, 0.0f, 0.0f, 0.0f, "Hard"),

                // Product Management
                ConceptMastery("product_lifecycle", "Product Lifecycle Management", "Product Management", "", 0.8f, 0.7f, 0.75f, 0.76f, "Medium"),
                ConceptMastery("user_stories", "User Stories & Backlog", "Product Management", "product_lifecycle", 0.5f, 0.4f, 0.45f, 0.48f, "Easy"),

                // Software Development
                ConceptMastery("git_vcs", "Git Version Control", "Software Development", "", 0.9f, 0.85f, 0.9f, 0.88f, "Easy"),
                ConceptMastery("rest_apis", "RESTful API Design", "Software Development", "git_vcs", 0.6f, 0.55f, 0.5f, 0.58f, "Medium"),

                // Web3 & Blockchain
                ConceptMastery("blockchain_basics", "Blockchain Foundations", "Web3 & Blockchain", "", 0.7f, 0.6f, 0.65f, 0.64f, "Medium"),
                ConceptMastery("smart_contracts", "Smart Contracts & Solidity", "Web3 & Blockchain", "blockchain_basics", 0.4f, 0.3f, 0.35f, 0.32f, "Hard"),

                // E-commerce
                ConceptMastery("ecom_metrics", "E-commerce Metrics & KPIs", "E-commerce", "", 0.8f, 0.75f, 0.8f, 0.78f, "Easy"),
                ConceptMastery("payment_gateways", "Payment Processing Flows", "E-commerce", "ecom_metrics", 0.5f, 0.45f, 0.5f, 0.48f, "Medium"),

                // Business Analysis
                ConceptMastery("swot_analysis", "SWOT & Competitor Analysis", "Business Analysis", "", 0.85f, 0.8f, 0.85f, 0.83f, "Easy"),
                ConceptMastery("req_gathering", "Requirements Elicitation", "Business Analysis", "swot_analysis", 0.6f, 0.5f, 0.55f, 0.54f, "Medium"),

                // Product Design
                ConceptMastery("ui_principles", "UI/UX Foundations", "Product Design", "", 0.85f, 0.8f, 0.85f, 0.82f, "Easy"),
                ConceptMastery("design_systems", "Design Systems & Components", "Product Design", "ui_principles", 0.6f, 0.5f, 0.55f, 0.58f, "Medium"),

                // Project Management
                ConceptMastery("agile_scrum", "Agile & Scrum Methodologies", "Project Management", "", 0.9f, 0.8f, 0.85f, 0.86f, "Easy"),
                ConceptMastery("gantt_charts", "Project Scheduling & Gantt", "Project Management", "agile_scrum", 0.7f, 0.6f, 0.65f, 0.66f, "Medium"),

                // Digital Marketing
                ConceptMastery("seo_foundations", "SEO Foundations", "Digital Marketing", "", 0.8f, 0.75f, 0.8f, 0.78f, "Easy"),
                ConceptMastery("social_ads", "Paid Social Advertising", "Digital Marketing", "seo_foundations", 0.6f, 0.5f, 0.55f, 0.54f, "Medium"),

                // Data Analysis
                ConceptMastery("sql_queries", "SQL Querying & Databases", "Data Analysis", "", 0.85f, 0.8f, 0.85f, 0.83f, "Medium"),
                ConceptMastery("pandas_viz", "Pandas DataFrames & Visualization", "Data Analysis", "sql_queries", 0.6f, 0.5f, 0.55f, 0.54f, "Medium")
            )
            db.conceptMasteryDao().insertAllConcepts(concepts)

            // 3. Preseed starter flashcards
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
                ),
                // Product Management
                Flashcard(
                    conceptId = "product_lifecycle",
                    question = "What are the four main stages of the Product Lifecycle (PLC)?",
                    answer = "Introduction, Growth, Maturity, and Decline.",
                    difficulty = "Medium",
                    deckId = "deck_product_management"
                ),
                Flashcard(
                    conceptId = "user_stories",
                    question = "What is an MVP (Minimum Viable Product)?",
                    answer = "A version of a product with just enough features to be usable by early customers who can then provide feedback for future product development.",
                    difficulty = "Easy",
                    deckId = "deck_product_management"
                ),
                // Software Development
                Flashcard(
                    conceptId = "git_vcs",
                    question = "What is the difference between Git and GitHub?",
                    answer = "Git is a local version control tool, while GitHub is a cloud-based hosting service for Git repositories.",
                    difficulty = "Easy",
                    deckId = "deck_software_development"
                ),
                Flashcard(
                    conceptId = "rest_apis",
                    question = "What are the main HTTP methods used in RESTful APIs?",
                    answer = "GET (retrieve), POST (create), PUT (update), DELETE (remove).",
                    difficulty = "Medium",
                    deckId = "deck_software_development"
                ),
                // Web3 & Blockchain
                Flashcard(
                    conceptId = "blockchain_basics",
                    question = "What is the consensus mechanism used by Ethereum 2.0?",
                    answer = "Proof of Stake (PoS).",
                    difficulty = "Medium",
                    deckId = "deck_web3_blockchain"
                ),
                Flashcard(
                    conceptId = "smart_contracts",
                    question = "What is a Smart Contract?",
                    answer = "A self-executing contract with the terms of the agreement between buyer and seller being directly written into lines of code, running on a decentralized blockchain.",
                    difficulty = "Hard",
                    deckId = "deck_web3_blockchain"
                ),
                // E-commerce
                Flashcard(
                    conceptId = "ecom_metrics",
                    question = "What is Conversion Rate (CR) in e-commerce?",
                    answer = "The percentage of website visitors who make a purchase, calculated as (Orders / Total Visitors) * 100.",
                    difficulty = "Easy",
                    deckId = "deck_ecommerce"
                ),
                Flashcard(
                    conceptId = "payment_gateways",
                    question = "What does cart abandonment rate measure?",
                    answer = "The percentage of online shoppers who add items to a virtual shopping cart but leave the site without completing the purchase.",
                    difficulty = "Medium",
                    deckId = "deck_ecommerce"
                ),
                // Business Analysis
                Flashcard(
                    conceptId = "swot_analysis",
                    question = "What does SWOT stand for?",
                    answer = "Strengths, Weaknesses, Opportunities, and Threats.",
                    difficulty = "Easy",
                    deckId = "deck_business_analysis"
                ),
                Flashcard(
                    conceptId = "req_gathering",
                    question = "What is the difference between functional and non-functional requirements?",
                    answer = "Functional requirements define what the system should do (features), while non-functional requirements specify how the system should perform (security, speed, scalability).",
                    difficulty = "Medium",
                    deckId = "deck_business_analysis"
                ),
                // Product Design
                Flashcard(
                    conceptId = "ui_principles",
                    question = "What is the primary difference between UI and UX?",
                    answer = "UI (User Interface) focuses on the visual and interactive elements of a product, while UX (User Experience) focuses on the overall feel, usability, and journey of the user.",
                    difficulty = "Easy",
                    deckId = "deck_product_design"
                ),
                Flashcard(
                    conceptId = "design_systems",
                    question = "What is a wireframe in product design?",
                    answer = "A basic, low-fidelity outline or layout schematic of a screen, focusing on structure, hierarchy, and functionality rather than visual style.",
                    difficulty = "Medium",
                    deckId = "deck_product_design"
                ),
                // Project Management
                Flashcard(
                    conceptId = "agile_scrum",
                    question = "What are the core roles defined in Scrum?",
                    answer = "Product Owner, Scrum Master, and the Development Team.",
                    difficulty = "Easy",
                    deckId = "deck_project_management"
                ),
                Flashcard(
                    conceptId = "gantt_charts",
                    question = "What is a Gantt chart used for?",
                    answer = "To visually represent project tasks, schedules, dependencies, and timelines.",
                    difficulty = "Medium",
                    deckId = "deck_project_management"
                ),
                // Digital Marketing
                Flashcard(
                    conceptId = "seo_foundations",
                    question = "What does SEO stand for and why is it important?",
                    answer = "Search Engine Optimization; it helps websites rank higher in organic search results to gain more traffic.",
                    difficulty = "Easy",
                    deckId = "deck_digital_marketing"
                ),
                Flashcard(
                    conceptId = "social_ads",
                    question = "What is CTR (Click-Through Rate)?",
                    answer = "The ratio of users who click on a specific link to the number of total users who view a page, email, or advertisement, calculated as (Clicks / Impressions) * 100.",
                    difficulty = "Medium",
                    deckId = "deck_digital_marketing"
                ),
                // Data Analysis
                Flashcard(
                    conceptId = "sql_queries",
                    question = "What is the difference between INNER JOIN and LEFT JOIN in SQL?",
                    answer = "INNER JOIN returns records with matching values in both tables, whereas LEFT JOIN returns all records from the left table and matching records from the right table.",
                    difficulty = "Medium",
                    deckId = "deck_data_analysis"
                ),
                Flashcard(
                    conceptId = "pandas_viz",
                    question = "What library is commonly used for data manipulation in Python?",
                    answer = "Pandas, which provides highly efficient DataFrame structures.",
                    difficulty = "Medium",
                    deckId = "deck_data_analysis"
                )
            )
            db.flashcardDao().insertAllCards(flashcards)

            // 4. Preseed starter study tasks
            val tasks = listOf(
                StudyTask(conceptId = "derivatives", conceptName = "Derivatives Review", subject = "Calculus"),
                StudyTask(conceptId = "functions_cs", conceptName = "Master Functions in CS", subject = "Computer Science"),
                StudyTask(conceptId = "chemical_bonds", conceptName = "Bonding Quiz Preparation", subject = "Chemistry"),
                StudyTask(conceptId = "product_lifecycle", conceptName = "Synthesize PLC Concepts", subject = "Product Management"),
                StudyTask(conceptId = "git_vcs", conceptName = "Git Workflows Practical", subject = "Software Development"),
                StudyTask(conceptId = "blockchain_basics", conceptName = "Explore Smart Contracts", subject = "Web3 & Blockchain"),
                StudyTask(conceptId = "ecom_metrics", conceptName = "Ecom KPI Calculation", subject = "E-commerce"),
                StudyTask(conceptId = "swot_analysis", conceptName = "Conduct Competitor Analysis", subject = "Business Analysis"),
                StudyTask(conceptId = "ui_principles", conceptName = "UX Design Challenge", subject = "Product Design"),
                StudyTask(conceptId = "agile_scrum", conceptName = "Scrum Sprint Planning", subject = "Project Management"),
                StudyTask(conceptId = "seo_foundations", conceptName = "Optimize Website SEO", subject = "Digital Marketing"),
                StudyTask(conceptId = "sql_queries", conceptName = "SQL Joins Practice", subject = "Data Analysis")
            )
            db.studyTaskDao().insertAllTasks(tasks)

            // 5. Preseed starter Tech Hub Projects for LIFELONG LEARNERS
            val starterProjects = listOf(
                TechProject(
                    id = "proj_ai_tutor",
                    title = "Smart Study Partner AI",
                    description = "An offline-first, local Gemini-powered study assistant tailored for senior learners to master digital literacy and science.",
                    creatorName = "Professor Socrates",
                    creatorRole = "Instructor",
                    techStack = "Kotlin, Jetpack Compose, Gemini API",
                    teamMembers = "Professor Socrates (Instructor), Dr. Marie (Mentor), Alex (Learner)",
                    status = "In Progress",
                    likesCount = 24
                ),
                TechProject(
                    id = "proj_decentralized_ledger",
                    title = "Decentralized Learner Credential Ledger",
                    description = "A lightweight peer-to-peer verification system to store and share micro-credentials and skill badges securely.",
                    creatorName = "Nate Carter",
                    creatorRole = "Learner",
                    techStack = "Kotlin Multiplatform, SQLite, Cryptography",
                    teamMembers = "Nate Carter (Learner), Dr. Marie (Mentor)",
                    status = "Ideation",
                    likesCount = 12
                ),
                TechProject(
                    id = "proj_ecoquest",
                    title = "EcoQuest Gamified Learning App",
                    description = "An interactive, custom Canvas simulation game designed to teach elementary children ecological footprints and waste recycling.",
                    creatorName = "Dr. Marie",
                    creatorRole = "Mentor",
                    techStack = "Jetpack Compose Canvas, Room, Coroutines",
                    teamMembers = "Dr. Marie (Mentor), Nate Carter (Learner), Socrates (Instructor)",
                    status = "Completed",
                    likesCount = 38
                )
            )
            for (proj in starterProjects) {
                db.techProjectDao().insertProject(proj)
            }

            // Preseed some starter comments
            val starterComments = listOf(
                ProjectComment(
                    projectId = "proj_ai_tutor",
                    authorName = "Alex",
                    authorRole = "Learner",
                    text = "I've started building the Jetpack Compose conversation screens. Professor Socrates, could you help me integrate the Socratic response logic next week?",
                    timestamp = System.currentTimeMillis() - 86400000L
                ),
                ProjectComment(
                    projectId = "proj_ai_tutor",
                    authorName = "Professor Socrates",
                    authorRole = "Instructor",
                    text = "Of course, Alex! I've drafted the prompt templates. We should use standard temperature settings for structured socratic reasoning.",
                    timestamp = System.currentTimeMillis() - 43200000L
                ),
                ProjectComment(
                    projectId = "proj_decentralized_ledger",
                    authorName = "Dr. Marie",
                    authorRole = "Mentor",
                    text = "This is a brilliant initiative! I can assist in mapping the credential structures to the Open Badges standard.",
                    timestamp = System.currentTimeMillis() - 72000000L
                )
            )
            for (comm in starterComments) {
                db.projectCommentDao().insertComment(comm)
            }

            // Preseed Tech Study Rooms
            val starterRooms = listOf(
                TechStudyRoom(
                    id = "room_ai_tutor_lab",
                    projectId = "proj_ai_tutor",
                    projectName = "Smart Study Partner AI",
                    name = "Socrates' AI Sandbox 🤖",
                    creatorName = "Professor Socrates",
                    activeParticipants = "Professor Socrates, Jusreal, Dr. Marie",
                    collaborativeNotes = "### Smart Study Partner AI Architecture\n\n1. Use Kotlin Coroutines for asynchronous Gemini client invocation.\n2. Leverage Jetpack Compose for building intuitive chats for learners.\n3. Cache conversations in Room for offline access."
                ),
                TechStudyRoom(
                    id = "room_ledger_audit",
                    projectId = "proj_decentralized_ledger",
                    projectName = "Decentralized Learner Credential Ledger",
                    name = "P2P Cryptography Audit 🔐",
                    creatorName = "Nate Carter",
                    activeParticipants = "Nate Carter, Jusreal",
                    collaborativeNotes = "### Decrypting bad keys:\n- Implement Curve25519 using Kotlin Multiplatform.\n- Define SQLite schemas for offline credential verification."
                )
            )
            for (room in starterRooms) {
                db.techStudyRoomDao().insertRoom(room)
            }

            // Preseed some messages for these rooms
            val starterRoomMessages = listOf(
                TechRoomMessage(
                    id = "starter_msg_1",
                    roomId = "room_ai_tutor_lab",
                    senderName = "Professor Socrates",
                    senderRole = "Instructor",
                    content = "Welcome everyone to our virtual sandbox! Let's draft the core architecture of our Smart Study Partner here.",
                    timestamp = System.currentTimeMillis() - 120000L
                ),
                TechRoomMessage(
                    id = "starter_msg_2",
                    roomId = "room_ai_tutor_lab",
                    senderName = "Dr. Marie",
                    senderRole = "Mentor",
                    content = "I added some notes to the collaborative pad above about local caching. Feel free to refine it!",
                    timestamp = System.currentTimeMillis() - 60000L
                )
            )
            for (msg in starterRoomMessages) {
                db.techRoomMessageDao().insertMessage(msg)
            }

            // 6. Preseed Multilingual Starter Phrases
            val starterPhrases = listOf(
                SavedPhrase(
                    id = "phr_es_hello",
                    language = "Spanish",
                    originalText = "Hello, how are you?",
                    translatedText = "Hola, ¿cómo estás?",
                    pronunciation = "OH-lah, KOH-moh ess-TAHSS",
                    isMastered = false
                ),
                SavedPhrase(
                    id = "phr_fr_please",
                    language = "French",
                    originalText = "Please, help me.",
                    translatedText = "S'il vous plaît, aidez-moi.",
                    pronunciation = "seel voo pleh, eh-deh mwah",
                    isMastered = false
                ),
                SavedPhrase(
                    id = "phr_de_nice",
                    language = "German",
                    originalText = "Nice to meet you.",
                    translatedText = "Freut mich, Sie kennenzulernen.",
                    pronunciation = "froyt mikh, zee kenn-en-tsoo-lair-nen",
                    isMastered = false
                ),
                SavedPhrase(
                    id = "phr_ja_water",
                    language = "Japanese",
                    originalText = "Could I have some water, please?",
                    translatedText = "お水をいただけますか？",
                    pronunciation = "o-mee-zoo o ee-tah-dah-keh-mass kah?",
                    isMastered = false
                ),
                SavedPhrase(
                    id = "phr_sw_welcome",
                    language = "Swahili",
                    originalText = "Welcome to our learning hub!",
                    translatedText = "Karibu kwenye kitovu chetu cha kujifunza!",
                    pronunciation = "kah-ree-boo kweh-nyeh kee-toh-voo cheh-too chah koo-jee-foon-zah",
                    isMastered = false
                )
            )
            for (phr in starterPhrases) {
                db.savedPhraseDao().insertPhrase(phr)
            }

            // Seed Research Papers & Marketplace Listings
            val starterPapers = listOf(
                ResearchPaper(
                    id = "paper_zkp_identity",
                    title = "Zero-Knowledge Proofs in Decentralized Identity Verification",
                    authors = "Dr. Elena Rostova, Prof. Alan Turing",
                    abstractText = "This paper evaluates the performance characteristics of zk-SNARKs and zk-STARKs in constrained mobile operating systems, demonstrating a lightweight, secure client-side proof generation implementation.",
                    category = "Web3 & Blockchain",
                    content = "Full Research & Knowledge Transfer Blueprint:\n\n1. Introduction\nZero-Knowledge Proofs (ZKPs) allow a prover to demonstrate to a verifier that a statement is true without revealing any information beyond the statement itself. In mobile environments, ZKPs offer powerful capabilities for private credentials, localized verification, and gas-efficient scalability.\n\n2. Practical Implementation Steps\nTo build a verified login using ZKPs in Jetpack Compose, the client generates a cryptographic proof locally, which is then verified against an on-chain smart contract or decentralized registry. Our Android implementation leverages the Halo2 or Groth16 proving system with optimized WASM bindings.\n\n3. Code Blueprint (Solidity Verification)\n```solidity\ncontract ZKPVerifier {\n    function verifyProof(bytes calldata proof, uint256[] calldata inputs) external view returns (bool) {\n        // Cryptographic check here\n        return true;\n    }\n}\n```\n\n4. Conclusion\nBy offloading proving computation to client devices, local cryptographic integrity is maintained with minimal energy expenditure and high resistance to eavesdropping attacks.",
                    coinCost = 30,
                    isPurchased = false,
                    publisherName = "MIT Cryptography Labs",
                    publishYear = 2025,
                    fileSizeKb = 1450,
                    reviewsCount = 42,
                    rating = 4.8f
                ),
                ResearchPaper(
                    id = "paper_consensus_mechanisms",
                    title = "Decentralized Consensus Mechanisms: PBFT vs Raft in Private Ledgers",
                    authors = "Satoshi Nakamoto Jr., Leslie Lamport",
                    abstractText = "An academic review of Practical Byzantine Fault Tolerance versus Leader-based consensus in private educational enterprise blockchain systems.",
                    category = "Web3 & Blockchain",
                    content = "Comprehensive Analysis:\n\n1. Overview\nConsensus algorithms ensure a unified state across distributed ledger networks. PBFT guarantees safety and liveness under up to 1/3 Byzantine actors, while Raft focuses on crash-fault tolerance (CFT).\n\n2. Key Differences Table\n- PBFT: 3f+1 nodes required, 3-phase commit overhead (O(n^2) message complexity).\n- Raft: 2f+1 nodes required, simple leader replication (O(n) complexity).\n\n3. Verifiable Certification Path\nOnce complete, click 'Mint Verifiable Blockchain Certificate' to commit your research completion hash to the local sandbox chain block.",
                    coinCost = 0,
                    isPurchased = true, // Free paper, initially purchased
                    publisherName = "IEEE Distributed Systems",
                    publishYear = 2026,
                    fileSizeKb = 880,
                    reviewsCount = 18,
                    rating = 4.4f
                ),
                ResearchPaper(
                    id = "paper_audio_synthesizers",
                    title = "Deep Neural Representation for Dynamic Real-time Audio Synthesizers",
                    authors = "Prof. Clara Oswald, Dr. John Smith",
                    abstractText = "This research paper presents a modern approach to training lightweight neural regression models that approximate complex multi-oscillator breathing synth modulators inside client applications.",
                    category = "Artificial Intelligence",
                    content = "1. Abstract & Introduction\nWe introduce a dynamic neural synthesis method that optimizes physical acoustic waveforms on lightweight mobile CPUs. By predicting the Fourier wave components in real-time, we drastically reduce memory footprint.\n\n2. Wave Form Synthesizer Code\n```kotlin\nval sampleRate = 44100\nval amplitude = 0.5f\nval frequency = 440.0 // A4 note\n```\n\n3. Deployment Guidelines\nQuantizing weights to INT8 ensures flawless, stutter-free performance during background breathing focus sessions.",
                    coinCost = 40,
                    isPurchased = false,
                    publisherName = "Stanford AI Research",
                    publishYear = 2026,
                    fileSizeKb = 2100,
                    reviewsCount = 31,
                    rating = 4.7f
                ),
                ResearchPaper(
                    id = "paper_edge_intelligence",
                    title = "Advanced Micro-Architectures for Edge Intelligence",
                    authors = "Dr. Lisa Su, René Descartes",
                    abstractText = "Analyzing how dynamic sub-networks can be executed synchronously on edge processors without cloud-based REST scheduling overhead.",
                    category = "Artificial Intelligence",
                    content = "Comprehensive Edge Execution Guide.\n\n1. Abstract\nCompressing neural networks via INT8 quantization enables 10x throughput increases with less than 1% degradation in cognitive accuracy. This paper details compiler optimizations and hardware register mappings.\n\n2. Benchmarks\nExecution latency drops from 450ms to 42ms for a standard MobileNet-v3 backbone model running locally on smartphone neural processing units (NPUs).",
                    coinCost = 0,
                    isPurchased = true, // Free
                    publisherName = "ACM Computing Frontiers",
                    publishYear = 2025,
                    fileSizeKb = 1250,
                    reviewsCount = 65,
                    rating = 4.9f
                )
            )
            for (paper in starterPapers) {
                db.researchPaperDao().insertPaper(paper)
            }

            // Preseed Accredited Examination Questions
            val starterQuestions = listOf(
                AccreditedExamQuestion(
                    country = "Nigeria",
                    governingBody = "WAEC (West African Examinations Council)",
                    subject = "Mathematics",
                    acreditationStatus = "Regionally Approved & Governed",
                    questionText = "In a class of 40 students, 25 offer Physics and 18 offer Chemistry. If 5 students offer neither subject, how many students offer both Physics and Chemistry?",
                    difficulty = "Medium",
                    step1Title = "Identify the Universal Set and Subsets",
                    step1Explain = "Total students in the class (Universal set) U = 40. Students offering neither subject = 5. Therefore, students offering at least one subject (Physics or Chemistry) P ∪ C = 40 - 5 = 35 students.",
                    step2Title = "Formulate the Set Intersection Equation",
                    step2Explain = "Using the formula for union of sets: n(P ∪ C) = n(P) + n(C) - n(P ∩ C). Let x be the number of students who offer both subjects, which is n(P ∩ C). Substitute the values: 35 = 25 + 18 - x.",
                    step3Title = "Solve for the Intersection Variable",
                    step3Explain = "Simplify the equation: 35 = 43 - x. Isolating x: x = 43 - 35 = 8. Therefore, 8 students in the class offer both Physics and Chemistry.",
                    correctAnswer = "8 students"
                ),
                AccreditedExamQuestion(
                    country = "Kenya",
                    governingBody = "KNEC (Kenya National Examinations Council - KCSE)",
                    subject = "Physics",
                    acreditationStatus = "Nationally Accredited & Approved",
                    questionText = "A body starts from rest and accelerates uniformly at 4 m/s² for 6 seconds. Calculate the total distance traveled during this time interval.",
                    difficulty = "Easy",
                    step1Title = "Extract Kinematic Given Values",
                    step1Explain = "Initial velocity (starts from rest) u = 0 m/s. Constant acceleration a = 4 m/s². Time duration t = 6 seconds.",
                    step2Title = "Select the Correct Equation of Motion",
                    step2Explain = "To find the displacement/distance (s), select the kinematic equation: s = u*t + 0.5 * a * t².",
                    step3Title = "Calculate the Final Distance Value",
                    step3Explain = "Substitute the variables: s = (0 * 6) + 0.5 * 4 * (6)². s = 0 + 2 * 36 = 72 meters. Thus, the body travels a distance of 72 meters.",
                    correctAnswer = "72 meters"
                ),
                AccreditedExamQuestion(
                    country = "United States",
                    governingBody = "College Board (AP Computer Science A)",
                    subject = "Computer Science",
                    acreditationStatus = "Globally Accredited / College Board Approved",
                    questionText = "What is the return value of the recursive method call mystery(4)?\n\npublic int mystery(int n) {\n    if (n <= 1) return 1;\n    return n * mystery(n - 1);\n}",
                    difficulty = "Medium",
                    step1Title = "Trace Recursion Base Cases and Transitions",
                    step1Explain = "Method call starts with n = 4. Since 4 is greater than 1, it triggers the recursive step: 4 * mystery(3). This continues the recursion stack.",
                    step2Title = "Build the Recursion Call Stack",
                    step2Explain = "Trace subsequent levels:\nmystery(3) returns 3 * mystery(2)\nmystery(2) returns 2 * mystery(1)\nmystery(1) matches base case (1 <= 1) and returns 1 immediately.",
                    step3Title = "Unwind the Stack with Evaluations",
                    step3Explain = "Unwind the return chain starting from the base case:\nmystery(2) = 2 * 1 = 2\nmystery(3) = 3 * 2 = 6\nmystery(4) = 4 * 6 = 24. Thus, the method returns 24.",
                    correctAnswer = "24"
                ),
                AccreditedExamQuestion(
                    country = "United Kingdom",
                    governingBody = "Ofqual (Pearson Edexcel A-Level)",
                    subject = "Mathematics",
                    acreditationStatus = "Regionally Accredited & Standardized",
                    questionText = "Find the derivative of the function f(x) = 3x⁴ - 5x² + 7 with respect to x.",
                    difficulty = "Medium",
                    step1Title = "Apply the Calculus Power Rule",
                    step1Explain = "The power rule states that d/dx (x^n) = n * x^(n-1). For constant terms, d/dx (c) = 0. We will differentiate each term of the polynomial separately.",
                    step2Title = "Differentiate the Individual Algebraic Terms",
                    step2Explain = "Differentiate Term 1: d/dx (3x⁴) = 3 * 4x³ = 12x³.\nDifferentiate Term 2: d/dx (-5x²) = -5 * 2x = -10x.\nDifferentiate Term 3: d/dx (7) = 0.",
                    step3Title = "Combine Differentiated Terms",
                    step3Explain = "Combine the differentiated outputs: f'(x) = 12x³ - 10x. This is the first derivative representation of the original function.",
                    correctAnswer = "12x³ - 10x"
                ),
                AccreditedExamQuestion(
                    country = "India",
                    governingBody = "CBSE (Central Board of Secondary Education)",
                    subject = "Chemistry",
                    acreditationStatus = "Nationally Approved Board Syllabus",
                    questionText = "Determine the pH value of a 1.0 * 10⁻³ M aqueous solution of hydrochloric acid (HCl) at 25°C.",
                    difficulty = "Easy",
                    step1Title = "Establish Species Ionization Status",
                    step1Explain = "HCl is a strong monobasic acid that dissociates completely in water: HCl -> H⁺ + Cl⁻. Therefore, the hydronium ion concentration [H⁺] is equal to the acid concentration: [H⁺] = 1.0 * 10⁻³ M.",
                    step2Title = "Introduce the Standard pH Formula",
                    step2Explain = "The pH is mathematically defined as the negative logarithm (base 10) of the hydrogen ion concentration: pH = -log₁₀[H⁺].",
                    step3Title = "Perform Logarithmic Substitution",
                    step3Explain = "Substitute the concentration: pH = -log₁₀(1.0 * 10⁻³). pH = -(-3) = 3. Thus, the pH of the HCl solution is 3.",
                    correctAnswer = "3"
                ),
                AccreditedExamQuestion(
                    country = "South Africa",
                    governingBody = "UMALUSI (National Senior Certificate)",
                    subject = "Physics",
                    acreditationStatus = "Nationally Governed & Standardized",
                    questionText = "A 2 kg friction-free block is pulled horizontally along a table by a constant force of 10 N. Calculate the resulting acceleration of the block.",
                    difficulty = "Easy",
                    step1Title = "Identify Governed Physics Laws",
                    step1Explain = "According to Newton's Second Law of Motion, the net external force acting on an object is proportional to its mass and acceleration: F_net = m * a.",
                    step2Title = "Isolate the Target Acceleration Variable",
                    step2Explain = "Rearrange the equation to express acceleration as the subject: a = F_net / m.",
                    step3Title = "Substitute Known Dynamics Values",
                    step3Explain = "Substitute force (10 N) and mass (2 kg): a = 10 / 2 = 5 m/s². The resulting acceleration of the block is 5 m/s².",
                    correctAnswer = "5 m/s²"
                )
            )
            db.accreditedExamQuestionDao().insertAllQuestions(starterQuestions)

            // Preseed Strategic Platform Partners
            val starterPartners = listOf(
                PlatformPartner(
                    id = "partner_stanford",
                    name = "Stanford University (Department of CS)",
                    type = "University",
                    description = "Provides academic alignment, early research sandbox resources, and technical support for student-led software development.",
                    fundingRange = "$15,000 - $120,000",
                    focusAreas = "Generative AI, Quantum Computing, Cryptography",
                    supportProvided = "Grants, Lab Access & Professor Mentorship"
                ),
                PlatformPartner(
                    id = "partner_nitda",
                    name = "National Information Technology Development Agency (NITDA)",
                    type = "Government Parastatal",
                    description = "The principal governing IT development agency in Nigeria, backing young indigenous talent, software projects, and digital literacy.",
                    fundingRange = "₦5,000,000 - ₦35,000,000 ($5,000 - $35,000)",
                    focusAreas = "STEM Outreach, Local Talent Training, Open-source Solutions",
                    supportProvided = "Sponsorship Grants, Legal Advisory & Sandboxes"
                ),
                PlatformPartner(
                    id = "partner_yc",
                    name = "Y Combinator (YC Academy Support)",
                    type = "Accelerator",
                    description = "The world's premier tech startup incubator, providing seed resources, venture networks, and mentoring to student-led teams.",
                    fundingRange = "$500,000 (Standard Seed SAFE)",
                    focusAreas = "SaaS, AI Engineering, Web3 Infrastructures",
                    supportProvided = "Venture Seed Capital, Partner Advising & Pitch Prep"
                ),
                PlatformPartner(
                    id = "partner_sequoia",
                    name = "Sequoia Capital (Launchpad Syndicate)",
                    type = "Venture Capital",
                    description = "A legendary venture capital firm investing in game-changing software, Web3 consensus, and deep-tech hardware concepts.",
                    fundingRange = "$100,000 - $1,500,000",
                    focusAreas = "AI Agents, Blockchain Consensus, Robotics, Energy",
                    supportProvided = "Strategic Seed Capital, Talent Acquisition, Global Partnerships"
                ),
                PlatformPartner(
                    id = "partner_unicef",
                    name = "UNICEF (STEM Education Fund)",
                    type = "NGO",
                    description = "Leading non-governmental body accelerating digital inclusion, localized STEM labs, and basic computer science education.",
                    fundingRange = "$10,000 - $75,000",
                    focusAreas = "Inclusive Digital Education, Girls in STEM, Rural Tech Hubs",
                    supportProvided = "Non-dilutive Impact Grants, Field Trials, Hardware Kits"
                ),
                PlatformPartner(
                    id = "partner_techstars",
                    name = "Techstars (Impact Venture Syndicate)",
                    type = "Accelerator",
                    description = "A global startup network providing immersive mentoring, community hubs, and fundraising paths for student tech developers.",
                    fundingRange = "$120,000",
                    focusAreas = "EdTech, AgriTech, Clean Energy Solutions",
                    supportProvided = "Seed Capital, Executive Coaching & Partner Network"
                ),
                PlatformPartner(
                    id = "partner_mastercard",
                    name = "Mastercard Foundation",
                    type = "NGO",
                    description = "Promotes youth technical employment, micro-enterprise tools, and inclusive financial infrastructures in emerging nations.",
                    fundingRange = "$20,000 - $150,000",
                    focusAreas = "Digital Finance, Applied ICT Skills, Agri-processing Tech",
                    supportProvided = "Platform Funding Grants, Local Ecosystem Networking"
                ),
                PlatformPartner(
                    id = "partner_nairobi_tech",
                    name = "Nairobi Technical College",
                    type = "Technical College",
                    description = "A prestigious technical institute focusing on applied hardware prototypes, IoT systems, and vocational robotics.",
                    fundingRange = "N/A (Platform Support & Tooling)",
                    focusAreas = "Applied Robotics, IoT Micro-grids, FabLab Machinery",
                    supportProvided = "Makerspace Machinery, Hardware Labs & Component Supply"
                )
            )
            db.platformPartnerDao().insertAllPartners(starterPartners)

            // Preseed Global Talent Pool
            val starterTalents = listOf(
                TalentProfile(
                    id = "talent_alex",
                    name = "Alex Rivera",
                    email = "alex.rivera@globaldev.net",
                    title = "Senior Android Engineer",
                    skills = "Kotlin, Jetpack Compose, Coroutines, Room Database, Flow, MVVM, CI/CD",
                    certificationTitle = "Blockchain-Certified Software Engineer",
                    bio = "Experienced mobile developer specialized in creating high-performance, fluid, and modern Android applications with Jetpack Compose. Passionate about offline-first architectures and user experience.",
                    location = "Austin, USA",
                    workPreference = "Remote",
                    hourlyRate = "$85/hr",
                    avatar = "avatar_1"
                ),
                TalentProfile(
                    id = "talent_chioma",
                    name = "Chioma Okafor",
                    email = "chioma.o@blockchainlabs.io",
                    title = "Web3 & Smart Contract Architect",
                    skills = "Solidity, Rust, Ethereum, Web3.js, Cryptography, Node.js, Go",
                    certificationTitle = "Certified Web3 Specialist",
                    bio = "Web3 engineer designing secure decentralized applications and robust smart contracts. Expert in DeFi protocol audits, cryptography paradigms, and zero-knowledge proofs.",
                    location = "Lagos, Nigeria",
                    workPreference = "Hybrid",
                    hourlyRate = "$95/hr",
                    avatar = "avatar_2"
                ),
                TalentProfile(
                    id = "talent_meiling",
                    name = "Mei-Ling Chen",
                    email = "meiling.c@aistudios.sg",
                    title = "AI Research Engineer",
                    skills = "Python, PyTorch, TensorFlow, LLMs, NLP, Prompt Engineering, LangChain",
                    certificationTitle = "Blockchain-Accredited AI Architect",
                    bio = "ML research scientist building natural language processing models, fine-tuning large language models, and developing generative AI capabilities for educational tech ecosystems.",
                    location = "Singapore",
                    workPreference = "Remote",
                    hourlyRate = "$110/hr",
                    avatar = "avatar_3"
                ),
                TalentProfile(
                    id = "talent_carlos",
                    name = "Carlos Santana",
                    email = "carlos.s@uxcreative.br",
                    title = "Senior UI/UX Product Designer",
                    skills = "Figma, Material Design 3, Design Systems, Prototyping, Wireframing, User Research",
                    certificationTitle = "Certified Product Experience Specialist",
                    bio = "User-centered designer crafting intuitive and highly accessible digital experiences. Specializes in Material 3 design systems, high-fidelity prototypes, and running agile UX design sprints.",
                    location = "São Paulo, Brazil",
                    workPreference = "Onsite",
                    hourlyRate = "$65/hr",
                    avatar = "avatar_4"
                ),
                TalentProfile(
                    id = "talent_sarah",
                    name = "Sarah Jenkins",
                    email = "sarah.j@agilesprints.co.uk",
                    title = "Agile Product Owner / Project Manager",
                    skills = "Agile, Scrum, Jira, Product Roadmap Planning, Stakeholder Management, SQL, Gantt Charts",
                    certificationTitle = "Certified Project Master",
                    bio = "Results-driven project leader directing software engineering squads through Agile scrum sprints, backlog refinement, and product lifecycle releases with high operational velocity.",
                    location = "London, UK",
                    workPreference = "Remote",
                    hourlyRate = "$75/hr",
                    avatar = "avatar_5"
                )
            )
            starterTalents.forEach { db.talentProfileDao().insertTalent(it) }

            // Pre-seed Global Internships
            val starterInternships = listOf(
                GlobalInternship(
                    id = "intern_google",
                    companyName = "Google AI Research",
                    logoText = "G",
                    title = "AI Research Fellow (Multimodal Evaluation)",
                    description = "Participate in evaluation, benchmarking, and structured Socratic testing of state-of-the-art multimodal Gemini LLM configurations. Work alongside principal research scientists to evaluate model reasoning limits, formulate structured prompt evaluation patterns, and draft critical academic reports.",
                    stipend = "$4,500/month",
                    location = "Remote (Silicon Valley)",
                    requiredSkills = "Kotlin, Python, LLMs, Prompt Engineering, Research Methodology",
                    tasksText = "Synthesize benchmark accuracy metrics for multimodal reasoning;Implement automated Socratic prompt test suites;Formulate system instructions and edge-case validation scripts",
                    difficulty = "Expert"
                ),
                GlobalInternship(
                    id = "intern_stripe",
                    companyName = "Stripe",
                    logoText = "S",
                    title = "FinTech Systems Software Engineer",
                    description = "Bridge classroom learning with global finTech ecosystem deployments. Construct highly resilient network cache systems, secure callback mechanisms, and encrypted token synchronization engines for borderless distributed payment rails using modern Kotlin architectures.",
                    stipend = "$3,800/month",
                    location = "Hybrid (New York, NY)",
                    requiredSkills = "Kotlin, REST APIs, SQLite, Cryptography, Clean Architecture",
                    tasksText = "Deploy callback validation endpoints using standard SHA-256 integrity tags;Optimize local Room databases with incremental caching rules;Construct offline-first transaction queue managers with conflict handlers",
                    difficulty = "Intermediate"
                ),
                GlobalInternship(
                    id = "intern_ethereum",
                    companyName = "Ethereum Foundation",
                    logoText = "Ξ",
                    title = "Decentralized Systems Protocol Developer",
                    description = "Contribute directly to decentralized peer-to-peer state machines, PBFT consensus logs, and layer-2 Rollup client-side integrations. Write robust, mathematically formal protocols and verification modules designed to scale global secure computing environments.",
                    stipend = "$4,200/month",
                    location = "Remote (Zug, Switzerland)",
                    requiredSkills = "Web3 & Blockchain, Cryptography, Kotlin, Consensus Engines",
                    tasksText = "Deconstruct PBFT logging blocks and verify slot selection criteria;Design secure cryptographic signature verification algorithms;Simulate decentralized verifiable credential exchange structures",
                    difficulty = "Advanced"
                )
            )
            starterInternships.forEach { db.globalInternshipDao().insertInternship(it) }
        }
    }
}

// --- Tech Hub Entities and DAOs ---

@Entity(tableName = "tech_projects")
data class TechProject(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val creatorName: String,
    val creatorRole: String, // "Learner", "Mentor", "Instructor"
    val techStack: String,
    val teamMembers: String, // Comma-separated list of names/roles
    val status: String = "Ideation", // "Ideation", "In Progress", "Completed"
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_comments")
data class ProjectComment(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val authorName: String,
    val authorRole: String, // "Learner", "Mentor", "Instructor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TechProjectDao {
    @Query("SELECT * FROM tech_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<TechProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: TechProject)

    @Query("UPDATE tech_projects SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun likeProject(id: String)

    @Query("UPDATE tech_projects SET teamMembers = :members WHERE id = :id")
    suspend fun updateTeamMembers(id: String, members: String)

    @Query("DELETE FROM tech_projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}

@Dao
interface ProjectCommentDao {
    @Query("SELECT * FROM project_comments WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getCommentsForProject(projectId: String): Flow<List<ProjectComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ProjectComment)

    @Query("DELETE FROM project_comments WHERE projectId = :projectId")
    suspend fun deleteCommentsForProject(projectId: String)
}

// --- Multilingual Learning Entities and DAOs ---

@Entity(tableName = "saved_phrases")
data class SavedPhrase(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val language: String,
    val originalText: String,
    val translatedText: String,
    val pronunciation: String,
    val isMastered: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface SavedPhraseDao {
    @Query("SELECT * FROM saved_phrases ORDER BY savedAt DESC")
    fun getAllPhrases(): Flow<List<SavedPhrase>>

    @Query("SELECT * FROM saved_phrases WHERE language = :language ORDER BY savedAt DESC")
    fun getPhrasesByLanguage(language: String): Flow<List<SavedPhrase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: SavedPhrase)

    @Query("UPDATE saved_phrases SET isMastered = :isMastered WHERE id = :id")
    suspend fun updatePhraseMastery(id: String, isMastered: Boolean)

    @Query("DELETE FROM saved_phrases WHERE id = :id")
    suspend fun deletePhrase(id: String)
}

// --- Caching and Offline Synchronization ---

@Entity(tableName = "recently_studied_decks")
data class RecentlyStudiedDeck(
    @PrimaryKey val deckId: String,
    val lastStudiedAt: Long = System.currentTimeMillis()
)

@Dao
interface RecentlyStudiedDeckDao {
    @Query("SELECT * FROM recently_studied_decks ORDER BY lastStudiedAt DESC")
    fun getRecentlyStudied(): Flow<List<RecentlyStudiedDeck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(deck: RecentlyStudiedDeck)

    @Query("DELETE FROM recently_studied_decks WHERE deckId = :deckId")
    suspend fun deleteRecent(deckId: String)
}

@Entity(tableName = "pending_sync_actions")
data class PendingSyncAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // e.g. "RATE_CARD", "ADD_DECK", "ADD_CARD", "ADD_COMMENT"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PendingSyncActionDao {
    @Query("SELECT * FROM pending_sync_actions ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<PendingSyncAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingSyncAction)

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    suspend fun deleteAction(id: Int)
}

// --- Virtual Collaborative Study Rooms ---

@Entity(tableName = "tech_study_rooms")
data class TechStudyRoom(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val name: String,
    val creatorName: String,
    val activeParticipants: String, // Comma-separated names
    val collaborativeNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tech_room_messages")
data class TechRoomMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val roomId: String,
    val senderName: String,
    val senderRole: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TechStudyRoomDao {
    @Query("SELECT * FROM tech_study_rooms ORDER BY createdAt DESC")
    fun getAllRooms(): Flow<List<TechStudyRoom>>

    @Query("SELECT * FROM tech_study_rooms WHERE id = :id")
    fun getRoomById(id: String): Flow<TechStudyRoom?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: TechStudyRoom)

    @Query("UPDATE tech_study_rooms SET collaborativeNotes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)

    @Query("UPDATE tech_study_rooms SET activeParticipants = :participants WHERE id = :id")
    suspend fun updateParticipants(id: String, participants: String)

    @Query("DELETE FROM tech_study_rooms WHERE id = :id")
    suspend fun deleteRoom(id: String)
}

@Dao
interface TechRoomMessageDao {
    @Query("SELECT * FROM tech_room_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<TechRoomMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: TechRoomMessage)

    @Query("DELETE FROM tech_room_messages WHERE roomId = :roomId")
    suspend fun deleteMessagesForRoom(roomId: String)
}

@Entity(tableName = "scratchpad_items")
data class ScratchpadItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val roomId: String,
    val authorName: String,
    val authorRole: String,
    val content: String,
    val upvotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ScratchpadItemDao {
    @Query("SELECT * FROM scratchpad_items WHERE roomId = :roomId ORDER BY timestamp DESC")
    fun getItemsForRoom(roomId: String): Flow<List<ScratchpadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScratchpadItem)

    @Query("UPDATE scratchpad_items SET upvotes = upvotes + 1 WHERE id = :id")
    suspend fun upvoteItem(id: String)

    @Query("DELETE FROM scratchpad_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM scratchpad_items WHERE roomId = :roomId")
    suspend fun deleteItemsForRoom(roomId: String)
}

@Entity(tableName = "mentor_matches")
data class MentorMatch(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val mentorName: String,
    val alignmentScore: Int, // 0-100
    val analysisText: String,
    val milestonesText: String,
    val matchedAt: Long = System.currentTimeMillis()
)

@Dao
interface MentorMatchDao {
    @Query("SELECT * FROM mentor_matches WHERE projectId = :projectId ORDER BY matchedAt DESC")
    fun getMatchesForProject(projectId: String): Flow<List<MentorMatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MentorMatch)

    @Query("DELETE FROM mentor_matches WHERE id = :id")
    suspend fun deleteMatch(id: String)

    @Query("DELETE FROM mentor_matches WHERE projectId = :projectId")
    suspend fun deleteMatchesForProject(projectId: String)
}

@Entity(tableName = "research_papers")
data class ResearchPaper(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val authors: String,
    val abstractText: String,
    val category: String, // "Web3 & Blockchain", "Artificial Intelligence", etc.
    val content: String,
    val coinCost: Int,
    val isPurchased: Boolean = false,
    val publisherName: String,
    val publishYear: Int,
    val fileSizeKb: Int,
    val reviewsCount: Int = 0,
    val rating: Float = 4.5f,
    val associatedProjectId: String? = null
)

@Dao
interface ResearchPaperDao {
    @Query("SELECT * FROM research_papers ORDER BY publishYear DESC, title ASC")
    fun getAllPapers(): Flow<List<ResearchPaper>>

    @Query("SELECT * FROM research_papers WHERE id = :id LIMIT 1")
    suspend fun getPaperById(id: String): ResearchPaper?

    @Query("SELECT * FROM research_papers WHERE associatedProjectId = :projectId")
    fun getPapersByProject(projectId: String): Flow<List<ResearchPaper>>

    @Query("UPDATE research_papers SET associatedProjectId = :projectId WHERE id = :id")
    suspend fun associatePaperWithProject(id: String, projectId: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: ResearchPaper)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPapers(papers: List<ResearchPaper>)

    @Query("UPDATE research_papers SET isPurchased = :purchased WHERE id = :id")
    suspend fun updatePurchaseStatus(id: String, purchased: Boolean)

    @Query("UPDATE research_papers SET reviewsCount = reviewsCount + 1 WHERE id = :id")
    suspend fun incrementReviews(id: String)
}

@Entity(tableName = "blockchain_certificates")
data class BlockchainCertificate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val recipientName: String,
    val title: String,
    val sourceName: String,
    val type: String, // "RESEARCH", "PROJECT", "ROOM"
    val dateIssued: Long = System.currentTimeMillis(),
    val blockNumber: Int,
    val nonce: Int,
    val previousHash: String,
    val hash: String,
    val transactionHash: String
)

@Dao
interface BlockchainCertificateDao {
    @Query("SELECT * FROM blockchain_certificates ORDER BY dateIssued DESC")
    fun getAllCertificates(): Flow<List<BlockchainCertificate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: BlockchainCertificate)

    @Query("DELETE FROM blockchain_certificates WHERE id = :id")
    suspend fun deleteCertificate(id: String)

    @Query("DELETE FROM blockchain_certificates")
    suspend fun clearAllCertificates()
}

@Entity(tableName = "accredited_exam_questions")
data class AccreditedExamQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val country: String,
    val governingBody: String,
    val subject: String,
    val acreditationStatus: String, // "Globally Accredited", "Regionally Approved", "Nationally Governed"
    val questionText: String,
    val difficulty: String = "Medium",
    val step1Title: String,
    val step1Explain: String,
    val step2Title: String,
    val step2Explain: String,
    val step3Title: String,
    val step3Explain: String,
    val correctAnswer: String
)

@Dao
interface AccreditedExamQuestionDao {
    @Query("SELECT * FROM accredited_exam_questions ORDER BY governingBody ASC, subject ASC")
    fun getAllQuestions(): Flow<List<AccreditedExamQuestion>>

    @Query("SELECT * FROM accredited_exam_questions WHERE country = :country")
    fun getQuestionsByCountry(country: String): Flow<List<AccreditedExamQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: AccreditedExamQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<AccreditedExamQuestion>)
}

@Entity(tableName = "platform_partners")
data class PlatformPartner(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String, // "University", "Technical College", "NGO", "Government Parastatal", "STEM Community", "Accelerator", "Venture Capital"
    val description: String,
    val fundingRange: String,
    val focusAreas: String,
    val supportProvided: String
)

@Dao
interface PlatformPartnerDao {
    @Query("SELECT * FROM platform_partners ORDER BY type ASC, name ASC")
    fun getAllPartners(): Flow<List<PlatformPartner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PlatformPartner)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPartners(partners: List<PlatformPartner>)
}

@Entity(tableName = "partnership_applications")
data class PartnershipApplication(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val partnerId: String,
    val partnerName: String,
    val projectName: String,
    val applicantName: String,
    val pitchText: String,
    val fundingRequested: String,
    val status: String = "Pending Review", // "Pending Review", "Approved & Funded", "Matched"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PartnershipApplicationDao {
    @Query("SELECT * FROM partnership_applications ORDER BY timestamp DESC")
    fun getAllApplications(): Flow<List<PartnershipApplication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: PartnershipApplication)

    @Query("DELETE FROM partnership_applications")
    suspend fun clearAllApplications()
}

@Entity(tableName = "project_tasks")
data class ProjectTask(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val description: String,
    val assignedTo: String, // e.g. Name of member or mentor
    val isCompleted: Boolean = false,
    val dueDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectTaskDao {
    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getTasksForProject(projectId: String): Flow<List<ProjectTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTask)

    @Query("UPDATE project_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: String, isCompleted: Boolean)

    @Query("DELETE FROM project_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM project_tasks WHERE projectId = :projectId")
    suspend fun deleteTasksForProject(projectId: String)
}

@Entity(tableName = "talent_profiles")
data class TalentProfile(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val title: String,
    val skills: String,
    val certificationTitle: String,
    val bio: String,
    val location: String,
    val workPreference: String, // "Remote", "Hybrid", "Onsite"
    val hourlyRate: String,
    val isCertified: Boolean = true,
    val avatar: String = "avatar_1",
    val isUserProfile: Boolean = false
)

@Dao
interface TalentProfileDao {
    @Query("SELECT * FROM talent_profiles ORDER BY isUserProfile DESC, name ASC")
    fun getAllTalents(): Flow<List<TalentProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTalent(talent: TalentProfile)

    @Query("DELETE FROM talent_profiles WHERE id = :id")
    suspend fun deleteTalent(id: String)

    @Query("DELETE FROM talent_profiles WHERE isUserProfile = 1")
    suspend fun deleteUserProfile()
}

@Entity(tableName = "talent_engagements")
data class TalentEngagement(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val talentId: String,
    val talentName: String,
    val employerName: String,
    val jobTitle: String,
    val workType: String, // "Remote", "Hybrid", "Onsite"
    val salaryOffer: String,
    val message: String,
    val status: String = "Pending", // "Pending", "Accepted", "Declined"
    val contactEmail: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TalentEngagementDao {
    @Query("SELECT * FROM talent_engagements ORDER BY timestamp DESC")
    fun getAllEngagements(): Flow<List<TalentEngagement>>

    @Query("SELECT * FROM talent_engagements WHERE talentId = :talentId ORDER BY timestamp DESC")
    fun getEngagementsForTalent(talentId: String): Flow<List<TalentEngagement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngagement(engagement: TalentEngagement)

    @Query("UPDATE talent_engagements SET status = :status WHERE id = :id")
    suspend fun updateEngagementStatus(id: String, status: String)

    @Query("DELETE FROM talent_engagements WHERE id = :id")
    suspend fun deleteEngagement(id: String)
}

// --- Global Internship and Placements for Real-World Experience ---

@Entity(tableName = "global_internships")
data class GlobalInternship(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val companyName: String,
    val logoText: String,
    val title: String,
    val description: String,
    val stipend: String,
    val location: String,
    val requiredSkills: String, // Comma-separated list
    val tasksText: String, // Semicolon-separated list of milestones
    val difficulty: String = "Intermediate"
)

@Dao
interface GlobalInternshipDao {
    @Query("SELECT * FROM global_internships ORDER BY title ASC")
    fun getAllInternships(): Flow<List<GlobalInternship>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInternship(internship: GlobalInternship)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllInternships(internships: List<GlobalInternship>)

    @Query("DELETE FROM global_internships")
    suspend fun clearAllInternships()
}

@Entity(tableName = "internship_placements")
data class InternshipPlacement(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val internshipId: String,
    val companyName: String,
    val title: String,
    val currentProgress: Int = 0, // Number of tasks completed
    val totalTasks: Int,
    val status: String = "Applied", // "Applied", "In Progress", "Completed"
    val completedAt: Long? = null
)

@Dao
interface InternshipPlacementDao {
    @Query("SELECT * FROM internship_placements ORDER BY completedAt DESC, id DESC")
    fun getAllPlacements(): Flow<List<InternshipPlacement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacement(placement: InternshipPlacement)

    @Query("UPDATE internship_placements SET currentProgress = :progress, status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updatePlacementProgress(id: String, progress: Int, status: String, completedAt: Long?)

    @Query("DELETE FROM internship_placements WHERE id = :id")
    suspend fun deletePlacement(id: String)

    @Query("DELETE FROM internship_placements")
    suspend fun clearAllPlacements()
}


