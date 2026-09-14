package com.example.data

import android.content.Context
import android.util.Log
import com.example.production.FirebaseBootstrap
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class FirestoreDeck(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val userId: String = ""
)

data class FirestoreFlashcard(
    val id: String = "",
    val deckId: String = "",
    val question: String = "",
    val answer: String = "",
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val intervalDays: Int = 1,
    val nextReviewDate: Long = 0L,
    val lastReviewed: Long = 0L
)

class FirestoreManager(context: Context) {
    private val TAG = "FirestoreManager"

    private var db: FirebaseFirestore? = null
    private var isConfigured = false

    // Local in-memory fallback lists in case Firestore is not initialized or fails
    private val localDecks = mutableListOf<FirestoreDeck>()
    private val localCards = mutableMapOf<String, MutableList<FirestoreFlashcard>>()

    private val _decks = MutableStateFlow<List<FirestoreDeck>>(emptyList())
    val decks: StateFlow<List<FirestoreDeck>> = _decks

    private val _cardsMap = MutableStateFlow<Map<String, List<FirestoreFlashcard>>>(emptyMap())
    val cardsMap: StateFlow<Map<String, List<FirestoreFlashcard>>> = _cardsMap

    private val _isFirestoreActive = MutableStateFlow(false)
    val isFirestoreActive: StateFlow<Boolean> = _isFirestoreActive

    init {
        try {
            val firebaseReady = FirebaseBootstrap.initialize(context)
            val apps = FirebaseApp.getApps(context)
            if (!firebaseReady || apps.isEmpty()) {
                Log.w(TAG, "FirebaseApp is not configured (missing google-services.json). Using local persistence fallback.")
                throw IllegalStateException("Firebase is not configured")
            } else {
                Log.d(TAG, "FirebaseApp already initialized automatically.")
            }

            db = FirebaseFirestore.getInstance()
            isConfigured = true
            _isFirestoreActive.value = true
            Log.d(TAG, "Firestore successfully initialized.")
        } catch (e: Exception) {
            isConfigured = false
            _isFirestoreActive.value = false
            Log.e(TAG, "Firestore initialization skipped: ${e.message}. operating in local-persistence fallback mode.")
            
            // Pre-seed a sample local custom deck so the user has something beautiful immediately
            preseedLocalData()
        }
    }

    private fun preseedLocalData() {
        val sampleDeckId = "local_deck_neuroscience"
        val sampleDeck = FirestoreDeck(
            id = sampleDeckId,
            name = "Brain Anatomy & Memory",
            description = "Custom deck covering cognitive science, synaptic plasticity, and hippocampal regions.",
            createdAt = System.currentTimeMillis() - 86400000L,
            userId = "user_default"
        )
        localDecks.add(sampleDeck)
        _decks.value = localDecks.toList()

        val cards = mutableListOf(
            FirestoreFlashcard(
                id = "card_1",
                deckId = sampleDeckId,
                question = "What is Long-Term Potentiation (LTP)?",
                answer = "A persistent strengthening of synapses based on recent patterns of activity, widely considered one of the major cellular mechanisms underlying learning and memory.",
                easeFactor = 2.5f,
                repetitions = 0,
                intervalDays = 1,
                nextReviewDate = System.currentTimeMillis() - 1000L
            ),
            FirestoreFlashcard(
                id = "card_2",
                deckId = sampleDeckId,
                question = "Which brain structure is primarily responsible for consolidating short-term memory into long-term memory?",
                answer = "The Hippocampus, located in the temporal lobe.",
                easeFactor = 2.6f,
                repetitions = 1,
                intervalDays = 1,
                nextReviewDate = System.currentTimeMillis() - 1000L
            ),
            FirestoreFlashcard(
                id = "card_3",
                deckId = sampleDeckId,
                question = "What is the difference between retroactive and proactive interference?",
                answer = "Retroactive interference is when new information blocks retrieval of old info. Proactive interference is when old information blocks learning or retrieval of new info.",
                easeFactor = 2.4f,
                repetitions = 2,
                intervalDays = 3,
                nextReviewDate = System.currentTimeMillis() + 86400000L
            )
        )
        localCards[sampleDeckId] = cards
        _cardsMap.value = localCards.toMap()
    }

    // Load Decks from Firestore
    fun loadDecks(userId: String) {
        val database = db
        if (database == null || !isConfigured) {
            // Local fallback logic
            _decks.value = localDecks.filter { it.userId == userId || it.userId == "user_default" }
            _cardsMap.value = localCards.toMap()
            return
        }

        try {
            database.collection("decks")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to decks", error)
                        // Fallback to local on listener error
                        _decks.value = localDecks.toList()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val deckList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(FirestoreDeck::class.java)?.copy(id = doc.id)
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        _decks.value = deckList
                        // For each deck, listen to its flashcards
                        deckList.forEach { deck ->
                            listenToCardsForDeck(deck.id)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load decks from Firestore: ${e.message}")
            _decks.value = localDecks.toList()
        }
    }

    private fun listenToCardsForDeck(deckId: String) {
        val database = db ?: return
        try {
            database.collection("decks").document(deckId).collection("cards")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to cards for deck $deckId", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val cardsList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(FirestoreFlashcard::class.java)?.copy(id = doc.id)
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        val currentMap = _cardsMap.value.toMutableMap()
                        currentMap[deckId] = cardsList
                        _cardsMap.value = currentMap
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to listen to cards: ${e.message}")
        }
    }

    // Create Deck in Firestore
    fun createDeck(userId: String, name: String, description: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val database = db
        if (database == null || !isConfigured) {
            // Local fallback implementation
            val localId = "local_deck_" + UUID.randomUUID().toString().take(6)
            val newDeck = FirestoreDeck(
                id = localId,
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                userId = userId
            )
            localDecks.add(0, newDeck)
            localCards[localId] = mutableListOf()
            _decks.value = localDecks.toList()
            _cardsMap.value = localCards.toMap()
            onSuccess()
            return
        }

        try {
            val newDeckRef = database.collection("decks").document()
            val deck = FirestoreDeck(
                id = newDeckRef.id,
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                userId = userId
            )

            newDeckRef.set(deck)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Add card to a Deck in Firestore
    fun addCardToDeck(deckId: String, question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val database = db
        if (database == null || !isConfigured) {
            // Local fallback implementation
            val localId = "local_card_" + UUID.randomUUID().toString().take(6)
            val newCard = FirestoreFlashcard(
                id = localId,
                deckId = deckId,
                question = question,
                answer = answer,
                easeFactor = 2.5f,
                repetitions = 0,
                intervalDays = 1,
                nextReviewDate = System.currentTimeMillis(),
                lastReviewed = 0L
            )
            val cardsList = localCards.getOrPut(deckId) { mutableListOf() }
            cardsList.add(newCard)
            _cardsMap.value = localCards.toMap()
            onSuccess()
            return
        }

        try {
            val cardRef = database.collection("decks").document(deckId).collection("cards").document()
            val card = FirestoreFlashcard(
                id = cardRef.id,
                deckId = deckId,
                question = question,
                answer = answer,
                easeFactor = 2.5f,
                repetitions = 0,
                intervalDays = 1,
                nextReviewDate = System.currentTimeMillis(),
                lastReviewed = 0L
            )

            cardRef.set(card)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Update flashcard after rating (Spaced Repetition Algorithm)
    fun updateCardReview(deckId: String, card: FirestoreFlashcard, rating: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // SM-2 Spaced Repetition Algorithm based on Accuracy Rating
        // rating: 1 = Hard/Again, 2 = Good, 3 = Easy
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

        val database = db
        if (database == null || !isConfigured) {
            // Local fallback implementation
            val cardsList = localCards[deckId]
            if (cardsList != null) {
                val index = cardsList.indexOfFirst { it.id == card.id }
                if (index != -1) {
                    cardsList[index] = updatedCard
                }
            }
            _cardsMap.value = localCards.toMap()
            onSuccess()
            return
        }

        try {
            database.collection("decks").document(deckId).collection("cards").document(card.id)
                .set(updatedCard)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Delete a Deck from Firestore (including its subcollection cards)
    fun deleteDeck(deckId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val database = db
        if (database == null || !isConfigured) {
            // Local fallback implementation
            localDecks.removeAll { it.id == deckId }
            localCards.remove(deckId)
            _decks.value = localDecks.toList()
            _cardsMap.value = localCards.toMap()
            onSuccess()
            return
        }

        try {
            // Delete all cards first
            database.collection("decks").document(deckId).collection("cards")
                .get()
                .addOnSuccessListener { snapshot ->
                    val batch = database.batch()
                    snapshot.documents.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                    batch.delete(database.collection("decks").document(deckId))
                    batch.commit()
                        .addOnSuccessListener {
                            // Remove from local cards map
                            val currentMap = _cardsMap.value.toMutableMap()
                            currentMap.remove(deckId)
                            _cardsMap.value = currentMap
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
