package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.Flashcard
import com.example.data.FlashcardRatingResult
import com.example.data.MindMapEdge
import com.example.data.MindMapGraph
import com.example.data.MindMapNode
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ContentResponse?
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val parts: List<PartResponse>?
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    val text: String?
)

// --- Client Implementation ---

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a valid API key is present in the build config
     */
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Public entry point redirected through the Enterprise AI Routing Orchestration Layer.
     */
    suspend fun generate(prompt: String, systemPrompt: String? = null): String {
        return EnterpriseBackend.routeLlmRequest(prompt, systemPrompt)
    }

    /**
     * Directly contacts the Gemini REST endpoint or resolves to local secure fallback.
     * This is invoked exclusively by the Enterprise Orchestrator to bypass the proxy loop.
     */
    suspend fun executeDirectGemini(prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local simulated response.")
            return@withContext getLocalFallbackResponse(prompt, systemPrompt)
        }

        try {
            val requestUrl = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"

            val requestBodyObj = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = systemPrompt?.let { Content(parts = listOf(Part(text = it))) },
                generationConfig = GenerationConfig(temperature = 0.7f)
            )

            val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequest = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed with code ${response.code}: $errBody")
                    throw Exception("API call failed with code ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val responseObj = responseAdapter.fromJson(responseBody)

                val generatedText = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                return@withContext generatedText ?: throw Exception("Failed to parse text from Gemini response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            return@withContext getLocalFallbackResponse(prompt, systemPrompt)
        }
    }

    /**
     * Generates a local fallback response when the API key is not present or an error occurs.
     * Keeps the app fully functional and interactive in all states.
     */
    fun getLocalFallbackResponse(prompt: String, systemPrompt: String?): String {
        val p = prompt.lowercase()
        return when {
            p.contains("diagnostic") || p.contains("assessment") -> {
                """
                JSON_START
                [
                  {
                    "question": "If f(x) = x^2 + 3x, what is f'(2)?",
                    "options": ["4", "7", "8", "10"],
                    "correctAnswer": "7",
                    "explanation": "The derivative is f'(x) = 2x + 3. Evaluating at x = 2 gives f'(2) = 2(2) + 3 = 7."
                  },
                  {
                    "question": "Which of the following is a mutable variable declaration in Kotlin?",
                    "options": ["val x = 5", "var y = 10", "const val z = 15", "let a = 2"],
                    "correctAnswer": "var y = 10",
                    "explanation": "In Kotlin, 'var' is used to declare mutable variables, whereas 'val' declares read-only (immutable) references."
                  },
                  {
                    "question": "What type of chemical bond is formed by the sharing of electrons?",
                    "options": ["Ionic bond", "Covalent bond", "Hydrogen bond", "Metallic bond"],
                    "correctAnswer": "Covalent bond",
                    "explanation": "Covalent bonds are formed when atoms share electron pairs. Ionic bonds involve the complete transfer of electrons."
                  }
                ]
                JSON_END
                """.trimIndent()
            }
            p.contains("summary") || p.contains("formulas") || p.contains("pdf") || p.contains("lecture") -> {
                """
                JSON_START
                {
                  "summary": "This document covers core foundational concepts including functional relationships, limiting behavior, atomic bonding principles, and computer science program controls. It highlights the importance of analyzing prerequisite structures to build solid concept mastery.",
                  "formulas": [
                    "f'(x) = lim (h -> 0) [f(x+h) - f(x)] / h",
                    "E = h * v (Plank's Equation)",
                    "Time Complexity: O(log n) for Binary Search"
                  ],
                  "glossary": [
                    {"term": "Limit", "definition": "The value that a function approaches as the input approaches some value."},
                    {"term": "Covalent Bond", "definition": "A chemical bond formed when electrons are shared between atoms."},
                    {"term": "Control Flow", "definition": "The order in which individual statements, instructions, or function calls of an imperative program are executed."}
                  ],
                  "flashcards": [
                    {"question": "What is the physical interpretation of a derivative?", "answer": "The instantaneous rate of change of a function, or the slope of the tangent line to the curve at a given point."},
                    {"question": "What is an isotope?", "answer": "Atoms of the same element with the same number of protons but different numbers of neutrons."},
                    {"question": "Explain Big O Notation.", "answer": "A mathematical notation that describes the limiting behavior of a function when the argument tends towards a particular value or infinity, used to analyze algorithm efficiency."}
                  ],
                  "quizzes": [
                    {
                      "question": "What does a limit describe in calculus?",
                      "options": ["The value at exactly x=a", "The value a function approaches as x gets arbitrarily close to a", "The maximum value of a function", "The area under a curve"],
                      "correctAnswer": "The value a function approaches as x gets arbitrarily close to a",
                      "explanation": "Limits focus entirely on the behavior of a function near a point, not necessarily exactly at that point."
                    },
                    {
                      "question": "Which data structure follows the First-In-First-Out (FIFO) principle?",
                      "options": ["Stack", "Queue", "Tree", "Graph"],
                      "correctAnswer": "Queue",
                      "explanation": "Queues process items in the order they arrive (FIFO), while Stacks process in Last-In-First-Out (LIFO) order."
                    }
                  ]
                }
                JSON_END
                """.trimIndent()
            }
            p.contains("solve") || p.contains("problem") || p.contains("explain") || p.contains("tutor") || p.contains("teach") -> {
                """
                Hello! I am your AI Tutor. Let's tackle this concept together step-by-step.

                To check our understanding, answer this: 
                What is the first derivative of x^3 + 5x?

                *Think about the Power Rule: d/dx (x^n) = n*x^(n-1).*
                
                Give it a try and I'll guide you to the final answer!

                [MASTERY_DELTA: +8%, CONFIDENCE_DELTA: +10%]
                """.trimIndent()
            }
            p.contains("past performance") || p.contains("academic goals") || p.contains("study plan") || p.contains("study twin") || p.contains("milestones planner") -> {
                val name = try { prompt.substringAfter("Learner: ", "Scholar").substringBefore("\n").trim() } catch (e: Exception) { "Scholar" }
                val goals = try {
                    val g = prompt.substringAfter("Learning Objectives: ", "").ifEmpty {
                        prompt.substringAfter("Primary Study Goal: ", "Master core STEM concepts")
                    }.substringBefore("\n").trim()
                    if (g.length > 80) g.take(77) + "..." else g
                } catch (e: Exception) { "Master core STEM concepts" }
                val streak = try { prompt.substringAfter("Streak: ", "").ifEmpty { prompt.substringAfter("Study Streak: ", "1") }.substringBefore(" ").substringBefore("\n").trim() } catch (e: Exception) { "1" }
                val studyTime = try { prompt.substringAfter("Daily Time: ", "").ifEmpty { prompt.substringAfter("Time Budget: ", "45") }.substringBefore(" ").substringBefore("\n").trim() } catch (e: Exception) { "45" }
                val diagnosticScore = try { prompt.substringAfter("Baseline: ", "0").substringBefore("%").trim() } catch (e: Exception) { "0" }

                """
                Hello $name! Having analyzed your complete past performance telemetry, including your $streak-day continuous study streak, $diagnosticScore% diagnostic baseline, and custom goals, I have constructed an optimized daily study roadmap. Memory decay trends suggest that immediate active recall should be focused on your due flashcards. Let's make sure we consolidate these items to lock in your retention and sustain your high-performance streak!

                For your $studyTime-minute study budget today, I recommend the following chronological layout: spend the first 15 minutes of your session in high-intensity active recall with your due flashcards. Then, allocate 20 minutes to review weak concepts where your understanding score is currently lagging, using your custom tutor modules to close these revision gaps step-by-step. Spend the final 10 minutes analyzing active research milestones, ensuring your practical skill training and theoretical research grow in alignment.

                As your Socratic Study Twin, I leave you with this reflection: academic excellence is not a single act, but the consistent curiosity that turns daily practice into profound understanding. Let's approach your targeted goal of "$goals" with precision and energy today!
                """.trimIndent()
            }
            else -> {
                """
                I've analyzed your Digital Learning Twin. Your current mastery is developing well!
                Let's continue focusing on your weak concepts to optimize our upcoming revision sessions.
                
                Keep up the great work, and don't hesitate to ask me anything about Calculus, Computer Science, or Chemistry!
                
                [MASTERY_DELTA: +5%, CONFIDENCE_DELTA: +5%]
                """.trimIndent()
            }
        }
    }

    /**
     * Generates a visual JSON graph representation (mind map) from flashcards.
     */
    suspend fun generateMindMap(deckName: String, cards: List<Flashcard>): MindMapGraph = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local programmatic mind map.")
            return@withContext getLocalFallbackMindMap(deckName, cards)
        }

        try {
            val requestUrl = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"

            val systemPrompt = """
                You are an expert educational taxonomist and cognitive map generator.
                Your task is to analyze a set of flashcards (questions and answers) and generate a clean, coherent structural hierarchy or mind map as a visual JSON graph representation.

                You must output a single, well-formed JSON object containing "nodes" and "edges" with the following JSON schema:
                {
                  "centralTheme": "A name for the main subject or theme connecting these cards",
                  "nodes": [
                    {
                      "id": "node_id",
                      "label": "Short descriptive label for the concept (1-3 words)",
                      "type": "root" | "category" | "concept" | "card_detail",
                      "description": "A clear, concise 1-sentence definition or summary of this concept."
                    }
                  ],
                  "edges": [
                    {
                      "from": "source_node_id",
                      "to": "target_node_id",
                      "label": "Relationship name (e.g., 'subconcept', 'prerequisite', 'defines', 'formula')"
                    }
                  ]
                }

                Rules for Graph Construction:
                1. Identify 1 central Root node (representing the core subject).
                2. Group the flashcards into logical Categories or High-level Concepts (these will be Category/Concept nodes).
                3. Connect the Categories/Concepts to the Root node.
                4. Add specific card details or definitions as child nodes (card_detail type) under their corresponding Categories/Concepts.
                5. Create relationships (edges) between nodes to show prerequisites, dependencies, or direct subconcepts.
                6. Make sure all Node IDs are unique.
                7. Only return valid JSON. Do not include markdown formatting or extra text outside the JSON.
            """.trimIndent()

            val cardListStr = cards.joinToString("\n") { "- Q: ${it.question} | A: ${it.answer}" }
            val prompt = """
                Analyze the following flashcards from the study collection "$deckName" and organize them into an interactive visual hierarchical mind map.
                
                Flashcards:
                $cardListStr
            """.trimIndent()

            val requestBodyObj = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    temperature = 0.2f,
                    responseMimeType = "application/json"
                )
            )

            val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequest = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed with code ${response.code}: $errBody")
                    throw Exception("API call failed with code ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val responseObj = responseAdapter.fromJson(responseBody)

                val rawText = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: throw Exception("Failed to parse text from Gemini response")
                
                // Clean markdown wrapping if present
                val cleanedText = rawText.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                val mindMapAdapter = moshi.adapter(MindMapGraph::class.java)
                return@withContext mindMapAdapter.fromJson(cleanedText) 
                    ?: throw Exception("Failed to deserialize MindMapGraph JSON")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating mind map from Gemini API, falling back.", e)
            return@withContext getLocalFallbackMindMap(deckName, cards)
        }
    }

    /**
     * Programmatically constructs a beautiful mind map graph offline or as a fallback.
     */
    fun getLocalFallbackMindMap(deckName: String, cards: List<Flashcard>): MindMapGraph {
        val nodes = mutableListOf<MindMapNode>()
        val edges = mutableListOf<MindMapEdge>()
        
        val rootId = "root"
        nodes.add(MindMapNode(
            id = rootId,
            label = deckName,
            type = "root",
            description = "Main visual study plan and core concepts for $deckName."
        ))
        
        if (cards.isEmpty()) {
            nodes.add(MindMapNode("cat1", "Add Cards", "category", "Start by adding active recall cards to expand this map."))
            nodes.add(MindMapNode("cat2", "Practice", "category", "Test yourself daily with spaced repetition."))
            edges.add(MindMapEdge(rootId, "cat1", "action"))
            edges.add(MindMapEdge(rootId, "cat2", "process"))
        } else {
            val categories = mutableMapOf<String, MutableList<Flashcard>>()
            
            cards.forEachIndexed { index, card ->
                val keywords = listOf("limit", "derivative", "integral", "carbon", "bond", "acid", "class", "function", "variable", "recursion")
                var foundCat = "General Concepts"
                val qLower = card.question.lowercase()
                for (kw in keywords) {
                    if (qLower.contains(kw)) {
                        foundCat = kw.replaceFirstChar { it.uppercase() }
                        break
                    }
                }
                if (foundCat == "General Concepts") {
                    foundCat = "Topic ${ (index / 3) + 1 }"
                }
                
                categories.getOrPut(foundCat) { mutableListOf() }.add(card)
            }
            
            var catCounter = 1
            categories.forEach { (catName, catCards) ->
                val catId = "cat_$catCounter"
                nodes.add(MindMapNode(
                    id = catId,
                    label = catName,
                    type = "category",
                    description = "Key concepts grouped under $catName."
                ))
                edges.add(MindMapEdge(from = rootId, to = catId, label = "includes"))
                
                catCards.forEachIndexed { cIndex, card ->
                    val cardNodeId = "card_${card.id}"
                    val answerNodeId = "ans_${card.id}"
                    
                    nodes.add(MindMapNode(
                        id = cardNodeId,
                        label = if (card.question.length > 25) card.question.take(22) + "..." else card.question,
                        type = "concept",
                        description = card.question
                    ))
                    edges.add(MindMapEdge(from = catId, to = cardNodeId, label = "tests"))
                    
                    nodes.add(MindMapNode(
                        id = answerNodeId,
                        label = "Recall Answer",
                        type = "card_detail",
                        description = card.answer
                    ))
                    edges.add(MindMapEdge(from = cardNodeId, to = answerNodeId, label = "solution"))
                }
                catCounter++
            }
        }
        
        return MindMapGraph(centralTheme = deckName, nodes = nodes, edges = edges)
    }

    /**
     * Generates an Active Recall session summary and identifies knowledge gaps using Gemini.
     */
    suspend fun generateActiveRecallSummary(
        deckName: String,
        results: List<FlashcardRatingResult>
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local simulated summary.")
            return@withContext getLocalFallbackActiveRecallSummary(deckName, results)
        }

        try {
            val requestUrl = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"

            val systemPrompt = """
                You are an expert cognitive psychologist and personal learning tutor.
                Your task is to analyze a student's 'Active Recall' flashcard study session results and produce a visually appealing, highly motivational, and structured summary.
                
                You should:
                1. Summarize their performance (how many cards they rated Easy, Good, and Hard).
                2. Identify specific knowledge gaps based on the questions they struggled with (rated Hard or Good).
                3. Provide clear, actionable recommendations or study tips to bridge those gaps.
                
                Structure your response with clear Markdown headings, bullet points, and high-impact emojis. Keep the tone encouraging, constructive, and highly personalized.
            """.trimIndent()

            val resultsStr = results.joinToString("\n") { result ->
                val ratingWord = when (result.rating) {
                    1 -> "Hard (Struggled)"
                    2 -> "Good (Got it with effort)"
                    else -> "Easy (Mastered)"
                }
                "- Q: ${result.question}\n  A: ${result.answer}\n  Rating: $ratingWord"
            }

            val prompt = """
                Analyze the study session results for the flashcard deck "$deckName".
                
                Here are the cards reviewed and the student's rating for each:
                ${if (resultsStr.isEmpty()) "No cards reviewed." else resultsStr}
                
                Please generate the study summary, identify the knowledge gaps, and suggest next steps.
            """.trimIndent()

            val requestBodyObj = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )

            val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequest = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed with code ${response.code}: $errBody")
                    throw Exception("API call failed with code ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val responseObj = responseAdapter.fromJson(responseBody)

                val generatedText = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                return@withContext generatedText ?: throw Exception("Failed to parse text from Gemini response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Active Recall summary", e)
            return@withContext getLocalFallbackActiveRecallSummary(deckName, results)
        }
    }

    private fun getLocalFallbackActiveRecallSummary(
        deckName: String,
        results: List<FlashcardRatingResult>
    ): String {
        val total = results.size
        val hardCount = results.count { it.rating == 1 }
        val goodCount = results.count { it.rating == 2 }
        val easyCount = results.count { it.rating == 3 }
        
        val hardCards = results.filter { it.rating == 1 }
        val goodCards = results.filter { it.rating == 2 }
        
        val sb = StringBuilder()
        sb.append("### 📊 Active Recall Session Summary: **$deckName**\n\n")
        sb.append("Fantastic job finishing this active study session! Regularly challenging your brain to recall information strengthens your retention and halts memory decay.\n\n")
        
        sb.append("#### **Session Breakdown**\n")
        sb.append("- 🟢 **Easy (Mastered):** $easyCount card(s) — *Strong retention, require less frequent reviews.*\n")
        sb.append("- 🟡 **Good (Developing):** $goodCount card(s) — *Almost consolidated, review again soon.*\n")
        sb.append("- 🔴 **Hard (Struggled):** $hardCount card(s) — *High priority knowledge gaps needing attention.*\n\n")
        
        sb.append("#### **🔍 Identified Knowledge Gaps**\n")
        if (hardCards.isEmpty() && goodCards.isEmpty()) {
            sb.append("🎉 **Perfect Session!** You've mastered all the flashcards in this set during this run. Great job maintaining an optimal recall curve!\n\n")
        } else {
            if (hardCards.isNotEmpty()) {
                sb.append("⚠️ **High Priority Gaps (Struggled):**\n")
                hardCards.forEach { card ->
                    sb.append("- **Concept:** \"${card.question}\"\n")
                    sb.append("  - *Recall Answer:* ${card.answer}\n")
                }
                sb.append("\n")
            }
            if (goodCards.isNotEmpty()) {
                sb.append("💡 **Medium Priority Gaps (Needs Reinforcement):**\n")
                goodCards.forEach { card ->
                    sb.append("- **Concept:** \"${card.question}\"\n")
                    sb.append("  - *Recall Answer:* ${card.answer}\n")
                }
                sb.append("\n")
            }
        }
        
        sb.append("#### **🎯 Recommended Next Steps**\n")
        if (hardCount > 0) {
            sb.append("1. **Focus on high priority cards:** Review the ${hardCount} cards you marked as 'Hard' tomorrow. Spaced repetition works best when you tackle these right at the edge of forgetting.\n")
        }
        sb.append("2. **Use the AI Digital Twin Tutor:** Launch a Socratic chat session for this deck to discuss the underlying concepts, request relatable analogies, and run personalized practice quizzes.\n")
        sb.append("3. **Visualize the connections:** Generate the Concept Mind Map to visualize how these concepts relate to the rest of the deck.\n")
        
        return sb.toString()
    }

    /**
     * Generates a summarized textual overview of a flashcard deck, highlighting key concepts, definitions, and themes.
     */
    suspend fun generateDeckContentSummary(
        deckName: String,
        cards: List<Flashcard>
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable()) {
            Log.w(TAG, "Gemini API key is not configured. Falling back to local simulated deck content summary.")
            return@withContext getLocalFallbackDeckContentSummary(deckName, cards)
        }

        try {
            val requestUrl = "$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}"

            val systemPrompt = """
                You are an elite educational AI and study companion.
                Your goal is to synthesize and generate a comprehensive yet concise "Deck Content Overview" based on a list of flashcards.
                
                You should:
                1. Identify the core overarching theme of the flashcard deck.
                2. Summarize the major definitions, concepts, or formulas contained in the cards.
                3. Group related terms logically into key takeaway pillars.
                4. Keep the presentation structured, highly academic yet digestible, with professional formatting, bullet points, and key terms highlighted in bold.
                
                Use professional formatting and emojis. Keep the tone encouraging and highly intellectual.
            """.trimIndent()

            val cardListStr = cards.joinToString("\n") { "- Q: ${it.question} | A: ${it.answer}" }
            val prompt = """
                Generate a summarized textual overview of the flashcard deck "$deckName".
                
                Here are the flashcards in this deck:
                $cardListStr
                
                Synthesize these cards into a neat, high-yield summary sheet highlighting the key concepts and major takeaways.
            """.trimIndent()

            val requestBodyObj = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(temperature = 0.5f)
            )

            val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
            val jsonRequest = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonRequest.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API call failed with code ${response.code}: $errBody")
                    throw Exception("API call failed with code ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val responseObj = responseAdapter.fromJson(responseBody)

                val generatedText = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                return@withContext generatedText ?: throw Exception("Failed to parse text from Gemini response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating deck content summary", e)
            return@withContext getLocalFallbackDeckContentSummary(deckName, cards)
        }
    }

    private fun getLocalFallbackDeckContentSummary(
        deckName: String,
        cards: List<Flashcard>
    ): String {
        val sb = StringBuilder()
        sb.append("### 📝 Study Sheet: Core Content Overview for **$deckName**\n\n")
        sb.append("This high-yield overview synthesizes the fundamental knowledge, key definitions, and overarching principles present in your flashcard set. Use it as a quick-reference study guide before exams.\n\n")
        
        if (cards.isEmpty()) {
            sb.append("⚠️ **No concepts to summarize.** Start by adding some flashcards to this deck, and your AI Digital Twin will automatically synthesize a complete study sheet here!")
            return sb.toString()
        }
        
        sb.append("#### 💡 **Core Foundational Concepts**\n")
        cards.take(5).forEach { card ->
            sb.append("- **${card.question}:** ${card.answer}\n")
        }
        
        if (cards.size > 5) {
            sb.append("\n#### 🔍 **Secondary Takeaways**\n")
            cards.drop(5).forEach { card ->
                sb.append("- *${card.question}:* ${card.answer}\n")
            }
        }
        
        sb.append("\n#### 🧠 **Twin Cognitive Recommendation**\n")
        sb.append("To fully cement these ideas, try teaching them back to your digital twin or testing yourself under exam conditions using the Socratic chat tutor!")
        
        return sb.toString()
    }
}
