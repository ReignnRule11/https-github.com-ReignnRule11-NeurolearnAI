package com.example.production

import com.example.billing.BillingCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseHardeningTest {
    private val workspace = File("/workspace")

    @Test
    fun requiredPlayProductIdsAreStable() {
        assertEquals("premium_max_monthly", BillingCatalog.PREMIUM_MAX_MONTHLY)
        assertEquals("coins_starter_100", BillingCatalog.COINS_STARTER_100)
        assertEquals("coins_standard_350", BillingCatalog.COINS_STANDARD_350)
    }

    @Test
    fun productionCredentialFilesAreNotCheckedIn() {
        assertFalse(File(workspace, "keystore.properties").exists())
        assertFalse(File(workspace, "my-upload-key.jks").exists())
        assertFalse(File(workspace, "app/google-services.json").exists())
        assertFalse(File(workspace, ".env").exists())
        assertTrue(File(workspace, "keystore.properties.example").exists())
        assertTrue(File(workspace, "app/google-services.json.example").exists())
        assertTrue(File(workspace, ".env.example").exists())
    }

    @Test
    fun firestoreRulesKeepTenantIsolationAndNestedDeckPath() {
        val rules = File(workspace, "firestore.rules").readText()
        assertTrue(rules.contains("match /decks/{deckId}"))
        assertTrue(rules.contains("match /cards/{cardId}"))
        assertTrue(rules.contains("documents/decks/\$(deckId)"))
        assertFalse(rules.contains("documents/decks/{deckId}"))
        assertTrue(rules.contains("matchesTenant("))
        assertTrue(rules.contains("allow update, delete: if false;"))
        assertTrue(rules.contains("!request.resource.data.diff(resource.data).affectedKeys().hasAny(['tenantId'])"))
    }

    @Test
    fun roomSchemaV22IsExportedAndDestructiveFallbackIsDebugOnly() {
        val schema = File(workspace, "app/schemas/com.example.data.AppDatabase/22.json")
        assertTrue(schema.exists())
        val schemaText = schema.readText()
        assertTrue(schemaText.contains("\"version\": 22"))
        assertTrue(schemaText.contains("learner_profile"))
        val databaseKt = File(workspace, "app/src/main/java/com/example/data/Database.kt").readText()
        assertTrue(databaseKt.contains("if (BuildConfig.DEBUG)"))
        assertTrue(databaseKt.contains("fallbackToDestructiveMigration(dropAllTables = true)"))
        val destructiveIndex = databaseKt.indexOf("fallbackToDestructiveMigration(dropAllTables = true)")
        val debugGuardIndex = databaseKt.lastIndexOf("if (BuildConfig.DEBUG)", destructiveIndex)
        assertTrue(debugGuardIndex in 0 until destructiveIndex)
    }

    @Test
    fun releaseSigningRejectsMissingAndDebugKeystores() {
        val gradle = File(workspace, "app/build.gradle.kts").readText()
        assertTrue(gradle.contains("Release signing requires KEYSTORE_PATH"))
        assertTrue(gradle.contains("Release signing rejected debug.keystore"))
        assertTrue(gradle.contains("looksLikeDebugKeystore"))
        assertTrue(gradle.contains("MissingGoogleServicesStrategy.ERROR"))
    }

    @Test
    fun geminiClientRejectsPlaceholderKeys() {
        val client = File(workspace, "app/src/main/java/com/example/api/GeminiClient.kt").readText()
        assertTrue(client.contains("ProductionSecrets.isUsableGeminiKey(BuildConfig.GEMINI_API_KEY)"))
        assertTrue(client.contains("Falling back to local simulated response."))
    }

    @Test
    fun syncAdapterDoesNotFabricateRemoteProgress() {
        val sync = File(workspace, "app/src/main/java/com/example/data/SQLiteSyncAdapter.kt").readText()
        assertTrue(sync.contains("Do not invent remote XP/coin deltas."))
        assertTrue(sync.contains("Local Room records remain authoritative until a real cloud snapshot exists."))
        assertFalse(sync.contains("remoteXp"))
        assertFalse(sync.contains("invented remote"))
    }
}
