package com.example.production

object ProductionSecrets {
    private val placeholderKeys = setOf(
        "",
        "MY_GEMINI_API_KEY",
        "your-api-key-here",
        "CHANGEME",
        "REPLACE_ME"
    )

    fun isUsableGeminiKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val trimmed = key.trim()
        if (trimmed in placeholderKeys) return false
        if (trimmed.contains("fake", ignoreCase = true)) return false
        if (trimmed.contains("placeholder", ignoreCase = true)) return false
        return true
    }
}
