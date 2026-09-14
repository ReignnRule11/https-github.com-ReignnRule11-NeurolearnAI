package com.example.production

import com.example.api.GeminiClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiFailSafeTest {
    @Test
    fun placeholderBuildConfigKeyIsNotTreatedAsLive() {
        assertFalse(ProductionSecrets.isUsableGeminiKey("MY_GEMINI_API_KEY"))
        val fallback = GeminiClient.getLocalFallbackResponse("diagnostic assessment", null)
        assertTrue(fallback.contains("JSON_START"))
        assertTrue(fallback.contains("correctAnswer"))
    }

    @Test
    fun unavailableGeminiStillReturnsStudyContent() {
        val fallback = GeminiClient.getLocalFallbackResponse("summary of this lecture pdf", null)
        assertTrue(fallback.contains("summary") || fallback.contains("JSON_START"))
    }
}
