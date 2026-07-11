# 🧠 NeuroLearn AI: Your Personal AI Learning Operating System

NeuroLearn AI is a next-generation Android application designed to elevate continuous learning, structured skill acquisition, and technical mentorship. Powered by Jetpack Compose, Room Database, and the Gemini-3.5-Flash model, NeuroLearn AI turns standard studying into a gamified, structured, and collaborative journey.

---

## 🎯 Expert Mentor-Mentee Matching System

Located within the **Tech Hub** of NeuroLearn AI, the **Mentor Match** tab provides an intelligent bridge between learners collaborating on high-level tech projects and virtual industry mentors.

### 🧠 How It Works
1. **Goal Alignment**: The matching algorithm parses the learner's explicit **Learning Goals** and preferred **Study Style** configured in their user profile.
2. **Granular History Evaluation**: The system analyzes the user's active **Concept Mastery History** (tracked inside the Room database tracking scores/understanding rates of previously completed tasks) to establish a background baseline.
3. **Generative Synthesis**: The model analyzes this alignment relative to the project's tech stack and the selected mentor's deep background to output:
   - A personalized **Alignment Score** (75% to 99%).
   - A descriptive 3-sentence **Alignment Analysis** explaining precisely why this mentor matches their project and learning goals.
   - Three **Interactive Learning Milestones** mapped directly to the project objectives.
4. **Persistent Record**: Matches are saved locally in the SQLite database (`mentor_matches` table) via Room. Learners can check/uncheck these milestones interactively.
5. **Live Office Hour Workspaces**: Launching a workspace establishes a virtual, persistent collaborative Room where the student can consult, chat, and refine their code.

---

## 🪙 Comprehensive Monetization Strategy

NeuroLearn AI adopts a balanced, dual-tier monetization loop designed to reward consistent learning while cleanly dividing free usage limits from professional-grade tools.

### 1. The NeuroCoins Token Economy (Utility Tokens)
- **Earn Rate**: Consistent study habits are rewarded. Completing a technical task awards the learner **+15 NeuroCoins** directly into their profile wallet.
- **Spend Rate**: Requesting an intelligent mentor match or generating a learning syllabus costs **40 NeuroCoins**.
- **Free Refills**: Free tier students can easily request an instant `+100 Free Refill` inside their Profile Screen to test features.

### 2. NeuroLearn Premium Max (Monthly Pass)
- **Pricing**: Offered as a flat monthly subscription at **$9.99/mo**.
- **Unlocks**:
  - **Infinite Matching**: Generates unlimited expert matches without ever deducting NeuroCoins.
  - **Infinite Live Workspaces**: Start an unlimited number of collaborative study rooms.
  - **Priority Processing**: Direct, low-latency API scheduling.

### 3. Token Shop (Microtransactions)
- **Starter Scholar Pack**: Get 100 NeuroCoins for **$1.99** (ideal for quick, casual projects).
- **Growth Accelerator Pack**: Get 350 NeuroCoins (includes +50 bonus) for **$4.99** (optimized for deep focus cycles).
- **Premium Monthly Pass**: Unlock everything instantly for **$9.99**.

---

## 🗄️ Architecture & Database Schema

NeuroLearn AI is architected using **MVVM** and modern **Room Database** local storage.

### Data Models
```kotlin
@Entity(tableName = "learner_profiles")
data class LearnerProfile(
    @PrimaryKey val id: String = "default_user",
    val name: String = "Scholar",
    val learningGoals: String = "Master Jetpack Compose & Clean Architecture",
    val learningStyle: String = "Hands-on Architect",
    val xp: Int = 100,
    val level: Int = 1,
    val isPremium: Boolean = false, // True for Premium Max subscribers
    val coins: Int = 150             // Utility currency balance
)

@Entity(tableName = "mentor_matches")
data class MentorMatch(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val mentorName: String,
    val alignmentScore: Int,
    val analysisText: String,
    val milestonesText: String,
    val matchedAt: Long = System.currentTimeMillis()
)
```

---

## 🏷️ TestTag Reference for UI Testing

Every critical component includes `Modifier.testTag` declarations for effortless Robolectric, Espresso, and UI-driven automation:

| UI Element / Dialog | TestTag ID | Description |
| :--- | :--- | :--- |
| **Tab: Mentor Match** | `tech_hub_tab_mentors` | Switches active tab to the Mentor-Mentee panel. |
| **Monetization Dashboard** | `monetization_panel` | Displays profile tier, coin balance, and upgrade CTA. |
| **Storefront Dialog** | `storefront_dialog` | Pop-up container displaying token packages. |
| **Launch Shop Button** | `open_store_btn` | Button that opens the NeuroCoins Token Shop. |
| **Starter Scholar Pack Purchase** | `buy_pack_starter` | Action card to purchase 100 coins for $1.99. |
| **Growth Pack Purchase** | `buy_pack_growth` | Action card to purchase 350 coins for $4.99. |
| **Premium Pass Purchase** | `buy_pack_premium` | Action card to purchase unlimited tier for $9.99. |
| **Upgrade Premium Button** | `upgrade_premium_btn` | Main CTA to instantly activate Premium Max. |
| **Select Project Dropdown** | `select_project_dropdown` | Filter to select an active tech project. |
| **Mentor Cards** | `mentor_card_0` ... `3` | Iterative cards to select custom mentors. |
| **Request Match Button** | `mentor_match_action_btn` | Action button to invoke Gemini match generation. |
| **Active Match Card** | `active_match_card` | Container displaying alignment analysis and milestones. |
| **Milestone Checkboxes** | `milestone_checkbox_0` ... `2` | Interactive checkbox items to log milestone completion. |
| **Launch Mentor Room Button** | `launch_mentor_room_btn` | Button to instantiate a live collaborative workspace. |

---

## 🛠️ Verification & Development

### 1. Secret Configuration
Do not hardcode keys. Set your API Key in your environment or via the **AI Studio Secrets panel**. The app automatically reads this at compile-time to populate the `BuildConfig.GEMINI_API_KEY`.

### 2. Compilation and Build Checks
Verify syntax, dependencies, and Kotlin builds using:
```bash
gradle compileDebugKotlin
```

Or execute testing tasks locally using the JVM:
```bash
gradle :app:testDebugUnitTest
```
