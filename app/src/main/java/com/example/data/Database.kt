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
    val coins: Int = 150
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

// --- DAOs ---

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
        MentorMatch::class
    ],
    version = 13,
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


