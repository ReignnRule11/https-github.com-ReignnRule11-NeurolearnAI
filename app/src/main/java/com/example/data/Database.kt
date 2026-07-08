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
    val subjects: String = "Calculus, Computer Science, Chemistry",
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
    val role: String = "Learner" // "Learner", "Instructor", "Admin"
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
    val deckId: String? = "default"
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
    val xpAwarded: Int = 15
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
        FlashcardDeck::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learnerProfileDao(): LearnerProfileDao
    abstract fun conceptMasteryDao(): ConceptMasteryDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun flashcardDeckDao(): FlashcardDeckDao

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
            // 0. Preseed default deck
            db.flashcardDeckDao().insertDeck(
                FlashcardDeck(
                    id = "default",
                    name = "General Knowledge",
                    description = "Default deck for auto-synthesized and diagnostic flashcards."
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
                ConceptMastery("stoichiometry", "Stoichiometry & Mole Concept", "Chemistry", "reactions", 0.0f, 0.0f, 0.0f, 0.0f, "Hard")
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
                )
            )
            db.flashcardDao().insertAllCards(flashcards)

            // 4. Preseed starter study tasks
            val tasks = listOf(
                StudyTask(conceptId = "derivatives", conceptName = "Derivatives Review", subject = "Calculus"),
                StudyTask(conceptId = "functions_cs", conceptName = "Master Functions in CS", subject = "Computer Science"),
                StudyTask(conceptId = "chemical_bonds", conceptName = "Bonding Quiz Preparation", subject = "Chemistry")
            )
            db.studyTaskDao().insertAllTasks(tasks)
        }
    }
}
