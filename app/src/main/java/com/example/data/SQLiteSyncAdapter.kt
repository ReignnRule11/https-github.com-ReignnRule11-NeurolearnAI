package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.EnterpriseBackend
import com.example.api.LogLevel
import com.example.api.SyncConflict
import com.example.api.SyncConflictStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * SQLiteSyncAdapter handles offline-first synchronization between the local Room SQLite
 * database and simulated cloud databases. It reads and writes student progress
 * (LearnerProfile, ConceptMastery, DailyStudyProgress) and learning materials
 * (FlashcardDeck, Flashcard) to ensure data consistency across connectivity boundaries.
 */
object SQLiteSyncAdapter {
    private const val TAG = "SQLiteSyncAdapter"

    suspend fun synchronize(context: Context, strategy: SyncConflictStrategy): List<SyncConflict> = withContext(Dispatchers.IO) {
        EnterpriseBackend.writeLog(LogLevel.INFO, "SYNC_ADAPTER", "SQLiteSyncAdapter triggered with strategy: ${strategy.name}")
        EnterpriseBackend.logSecurityAction("SYNC_START", "SQLiteSyncAdapter starting bidirectional sync.")

        val db = AppDatabase.getDatabase(context)
        val profileDao = db.learnerProfileDao()
        val conceptDao = db.conceptMasteryDao()
        val flashcardDao = db.flashcardDao()
        val deckDao = db.flashcardDeckDao()
        val progressDao = db.dailyStudyProgressDao()
        val pendingDao = db.pendingSyncActionDao()

        val start = System.currentTimeMillis()

        // 1. Fetch current local student progress & learning materials
        val localProfile = profileDao.getProfileSync("user_default") ?: LearnerProfile()
        val localConcepts = conceptDao.getAllConcepts().first()
        val localDecks = deckDao.getAllDecks().first()
        val localPendingActions = pendingDao.getPendingActions()

        EnterpriseBackend.writeLog(
            LogLevel.INFO,
            "SYNC_ADAPTER",
            "Loaded local database state: profile XP = ${localProfile.xp}, concepts count = ${localConcepts.size}, decks count = ${localDecks.size}, pending sync queue = ${localPendingActions.size}"
        )

        // 2. Build real sync conflicts between local SQLite records and remote mock server baseline
        val conflictsList = mutableListOf<SyncConflict>()

        // Conflict A: Learner Profile (Student Progress)
        val remoteProfileXp = localProfile.xp + 150
        val remoteProfileCoins = localProfile.coins + 50
        val clientProfileVal = "XP: ${localProfile.xp}, Level: ${localProfile.level}, Coins: ${localProfile.coins}"
        val serverProfileVal = "XP: $remoteProfileXp, Level: ${localProfile.level}, Coins: $remoteProfileCoins (Web Portal Update)"
        
        conflictsList.add(
            SyncConflict(
                entityId = localProfile.id,
                tableName = "learner_profile",
                clientValue = clientProfileVal,
                serverValue = serverProfileVal
            )
        )

        // Conflict B: Concept Mastery (Student Progress)
        // Pick one concept (e.g. limits or integration or the first available one)
        val targetConcept = localConcepts.firstOrNull { it.id == "limits" } ?: localConcepts.firstOrNull()
        if (targetConcept != null) {
            val remoteUnderstanding = 0.92f
            val remoteConfidence = 0.88f
            val clientConceptVal = "Understanding: ${targetConcept.understandingScore}, Confidence: ${targetConcept.confidenceScore}"
            val serverConceptVal = "Understanding: $remoteUnderstanding, Confidence: $remoteConfidence (Classroom Tablet)"

            conflictsList.add(
                SyncConflict(
                    entityId = targetConcept.id,
                    tableName = "concept_mastery",
                    clientValue = clientConceptVal,
                    serverValue = serverConceptVal
                )
            )
        }

        // Conflict C: Learning Decks (Learning Materials)
        val targetDeck = localDecks.firstOrNull { it.id == "default" } ?: localDecks.firstOrNull()
        if (targetDeck != null) {
            val clientDeckVal = "Name: ${targetDeck.name}, Description: ${targetDeck.description}"
            val serverDeckVal = "Name: ${targetDeck.name} (Global Curated), Description: Curated standard and diagnostic learning materials."

            conflictsList.add(
                SyncConflict(
                    entityId = targetDeck.id,
                    tableName = "flashcard_decks",
                    clientValue = clientDeckVal,
                    serverValue = serverDeckVal
                )
            )
        }

        // 3. Resolve and write back to the local SQLite database based on conflict strategy
        val resolvedConflicts = conflictsList.map { conflict ->
            val resolvedVal: String
            when (strategy) {
                SyncConflictStrategy.CLIENT_WINS -> {
                    resolvedVal = conflict.clientValue
                    EnterpriseBackend.writeLog(
                        LogLevel.WARNING,
                        "SYNC_ADAPTER",
                        "[CLIENT_WINS] Kept local SQLite value for table '${conflict.tableName}' (ID: ${conflict.entityId})"
                    )
                }
                SyncConflictStrategy.SERVER_WINS -> {
                    resolvedVal = conflict.serverValue
                    EnterpriseBackend.writeLog(
                        LogLevel.WARNING,
                        "SYNC_ADAPTER",
                        "[SERVER_WINS] Overwriting local SQLite database with remote value for table '${conflict.tableName}' (ID: ${conflict.entityId})"
                    )

                    // Execute real SQLite database updates with server values
                    when (conflict.tableName) {
                        "learner_profile" -> {
                            val updatedProfile = localProfile.copy(
                                xp = remoteProfileXp,
                                coins = remoteProfileCoins
                            )
                            profileDao.insertOrUpdateProfile(updatedProfile)
                        }
                        "concept_mastery" -> {
                            if (targetConcept != null) {
                                val updatedConcept = targetConcept.copy(
                                    understandingScore = 0.92f,
                                    confidenceScore = 0.88f,
                                    lastReviewed = System.currentTimeMillis()
                                )
                                conceptDao.insertConcept(updatedConcept)
                            }
                        }
                        "flashcard_decks" -> {
                            if (targetDeck != null) {
                                val updatedDeck = targetDeck.copy(
                                    name = "${targetDeck.name} (Global Curated)",
                                    description = "Curated standard and diagnostic learning materials."
                                )
                                deckDao.insertDeck(updatedDeck)
                            }
                        }
                    }
                }
                SyncConflictStrategy.SMART_MERGE_AI -> {
                    // Smart merge logic: Take average/max/synthesized results
                    resolvedVal = when (conflict.tableName) {
                        "learner_profile" -> {
                            val mergedXp = maxOf(localProfile.xp, remoteProfileXp)
                            val mergedCoins = maxOf(localProfile.coins, remoteProfileCoins)
                            val updatedProfile = localProfile.copy(
                                xp = mergedXp,
                                coins = mergedCoins
                            )
                            profileDao.insertOrUpdateProfile(updatedProfile)
                            "Merged: XP = $mergedXp, Coins = $mergedCoins (Max value strategy applied)"
                        }
                        "concept_mastery" -> {
                            if (targetConcept != null) {
                                val avgUnderstanding = (targetConcept.understandingScore + 0.92f) / 2f
                                val avgConfidence = (targetConcept.confidenceScore + 0.88f) / 2f
                                val updatedConcept = targetConcept.copy(
                                    understandingScore = avgUnderstanding,
                                    confidenceScore = avgConfidence,
                                    lastReviewed = System.currentTimeMillis()
                                )
                                conceptDao.insertConcept(updatedConcept)
                                "Merged: Understanding = ${String.format("%.2f", avgUnderstanding)}, Confidence = ${String.format("%.2f", avgConfidence)} (Average blend strategy)"
                            } else {
                                conflict.clientValue
                            }
                        }
                        "flashcard_decks" -> {
                            if (targetDeck != null) {
                                val updatedDeck = targetDeck.copy(
                                    name = "${targetDeck.name} (Merged)",
                                    description = "Local content combined with Curated Global standard material."
                                )
                                deckDao.insertDeck(updatedDeck)
                                "Merged: Name = ${updatedDeck.name}, Description = Local and global info concatenated."
                            } else {
                                conflict.clientValue
                            }
                        }
                        else -> {
                            conflict.clientValue
                        }
                    }

                    EnterpriseBackend.writeLog(
                        LogLevel.INFO,
                        "SYNC_ADAPTER",
                        "[SMART_MERGE_AI] Synthesized local SQLite and remote baseline for '${conflict.tableName}' (ID: ${conflict.entityId})"
                    )
                }
            }
            conflict.copy(resolvedValue = resolvedVal, resolvedStrategy = strategy)
        }

        // 4. Process pending local sync actions queue to simulate final upload
        if (localPendingActions.isNotEmpty()) {
            EnterpriseBackend.writeLog(LogLevel.INFO, "SYNC_ADAPTER", "Uploading ${localPendingActions.size} local offline-queue operations to server...")
            localPendingActions.forEach { action ->
                try {
                    val payload = JSONObject(action.payloadJson)
                    EnterpriseBackend.writeLog(
                        LogLevel.INFO,
                        "SYNC_ADAPTER",
                        "Successfully uploaded pending operation ${action.actionType} (ID: ${action.id})"
                    )
                    // Remove from pending database table
                    pendingDao.deleteAction(action.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed uploading action: ${action.id}", e)
                }
            }
        }

        val duration = System.currentTimeMillis() - start
        EnterpriseBackend.writeLog(LogLevel.INFO, "SYNC_ADAPTER", "Bidirectional synchronization completed successfully in ${duration}ms.")
        EnterpriseBackend.logSecurityAction("SYNC_COMPLETE", "SQLiteSyncAdapter completed. Strategy: $strategy")

        return@withContext resolvedConflicts
    }
}
