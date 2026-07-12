# 🧠 NeuroLearn AI: Your Personal AI Learning Operating System

NeuroLearn AI is a next-generation Android application designed to elevate continuous learning, structured skill acquisition, and technical mentorship. Powered by Jetpack Compose, Room Database, and the Gemini-3.5-Flash model, NeuroLearn AI turns standard studying into a gamified, structured, and collaborative journey.

---

## 💼 Standalone Employers & Learners Hub (Talent Hub) - *NEW!*

The **Employers & Learners Hub** (previously embedded inside Tech Hub) has been fully decoupled to stand alone as a first-class feature! It is accessible directly from the **Home Screen** via the high-visibility **Employers & Learners Hub** card, providing streamlined engagement and borderless recruiter interactions.

### 🧠 How It Works
1. **Decoupled Architecture**: Designed as an independent module (`TalentHubScreen.kt`) to ensure fast performance, focused layouts, and immediate accessibility.
2. **Interactive Talent Directory**:
   - **Browse Talents Tab**: Recruiters can browse global portfolios, filtering candidates by specific technical skills (e.g., Kotlin, Solidity) or work preferences (Remote, Hybrid, Onsite).
   - **My Profile Tab**: Students publish their certified, blockchain-verifiable credentials to the open market, indicating their professional title, target rates, technical skills, and locations.
3. **Direct Recruiter Engagements**:
   - Recruiters click **Engage** to launch a direct engagement proposal form, specifying role title, salary/rate offer, work model, and customizable invitations.
   - Offers flow straight into the student's personal **Recruiter Engagement Inbox** in real-time, allowing students to **Accept** or **Decline** contract proposals interactively.
4. **Verifiable Proof-of-Work**: Student profiles are marked with green checkmarks indicating verified credentials committed via the platform's simulated Proof-of-Work certificate ledger.

---

## 🎭 Socratic Digital Twin & Conversational Avatar - *NEW!*

Located on the **Home Screen** under the **Digital Twin Dashboard**, this feature uses the Gemini API to construct and interact with a personalized, real-time replica of your academic style, progress, and focus.

### 🧠 How It Works
1. **Dynamic Synced Personality**: The Socratic Digital Twin analyzes your active Room database (including daily time budget, learning goals, flashcard review history, and weak concept gaps) to calibrate its advice.
2. **Interactive Personas**: Choose from distinct conversational avatar personalities:
   - **Tech Visionary**: Logic-driven, focuses on systems design, optimization pipelines, and debugging.
   - **Scholar Academic**: Focused on deep theoretical science, classical research methodology, and foundational literature.
   - **Creative Innovator**: Highly playful, suggests visual metaphors and brainstorming analogies.
   - **Socratic Mentor**: Interactive query-driven guide helping you unpack concepts step-by-step.
3. **Real-Time Conversational Chat**: 
   - Engage with your Digital Twin in a highly polished chat interface (`digital_twin_chat` session).
   - Includes graceful, offline Socratic fallbacks tailored precisely to your current knowledge gaps and study priorities in case of API latency.

---

## 📅 Socratic Daily Study Planner & Research Milestones - *NEW!*

Accessed from the **Home Screen**, the **Intelligent Study Planner** organizes learning milestones based on your active research papers, progress, and spaced repetition curves.

### 🧠 How It Works
1. **Active Research Integration**: If you have purchased academic research papers (e.g., blockchain consensus, neural synths), the planner automatically detects them as active research topics.
2. **AI Milestone Generation**:
   - Clicking **Generate Daily Roadmap** sends your profile goals, spaced recall deck counts, weak concept gaps, and active research topics to the Gemini API.
   - The AI processes this telemetry to generate a customized 3-paragraph chronological daily roadmap and structured milestones.
   - Research papers are dynamically added to the timeline as **Research Milestones** (custom tasks awarding **+25 XP** upon deep analysis).
3. **Chronological Interactive Timeline**: Displays Pomodoro intervals, cognitive breaks, concept tutoring exercises, and spaced review sessions with status tracking checkboxes.

---

## 🎯 Expert Mentor-Mentee Matching System

Located within the **Tech Hub**, the **Mentor Match** tab provides an intelligent bridge between learners collaborating on high-level tech projects and virtual industry mentors.

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

## 🤖 AI Project Tutor, Mentor & Bug Fixer

Located inside the **AI Tutor** tab, this panel provides continuous Socratic teaching, architectural feedback, and instant source-level bug fixing directly integrated with the learner's workspace.

### 🧠 How It Works
1. **Context-Aware Assistance**: The system automatically pulls details of the user's selected tech proposal (e.g., project title, technologies used) to tailor all suggestions specifically to their technical environment.
2. **Interactive Persona Switching**:
   - **Ex-Principal Architect (Mentor)**: Synthesizes high-level system design feedback, database structure improvements, directory tree organization, security best-practices, and deployment workflows.
   - **Elite QA & Bug Fixer**: Inspects syntax fragments, handles memory leak investigation, analyzes stack traces, and suggests code translation across languages.
   - **Socratic Tech Educator (Tutor)**: Walks users through algorithms, explains computer science principles step-by-step, and provides bite-sized exercises instead of giving away direct answers.
3. **Collaborative Feed Integration**: A single click allows users to publish the generated AI review straight to the collaborative **Project Board Feed** as an official workspace post, rewarding them with **+10 XP** for keeping teammates aligned.

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

## 📚 Global Repository & Academic Knowledge Transfer Marketplace

NeuroLearn AI integrates a state-of-the-art **World Repository for Academic Research and Technical Blueprints**. Learners can acquire advanced technical knowledge while validating their learning milestones directly on-chain.

### 🧠 How It Works
1. **Repository Access**: Learners can browse peer-reviewed research papers (e.g., Web3 Consensus, Neural Networks, Edge Computing) or consult the **Global Unpaid Libraries** switcher.
2. **Global Unpaid Libraries System**:
   - Connections to:
     * **arXiv Open Science Archive**: 16+ Million STEM preprints.
     * **MIT OpenCourseWare**: Complete curriculum blueprints and engineering schemas.
     * **Project Gutenberg**: Scientific monographs and classical philosophy.
     * **PubMed Central (PMC)**: Free computational neuroscience and biology papers.
     * **W3C & IETF Protocol Specs**: Decentralized internet protocol RFCs and routing standards.
     * **Internet Archive**: Literature and history texts.
3. **AI Research Librarian Synthesis**: Generates a tailored **Scholarly Study Brief** containing key textbook definitions, advanced proofs analysis, and Socratic study questions.
4. **On-Chain Certification (Mining Simulation)**: Runs a Proof-of-Work (PoW) miner searching for SHA-256 hashes starting with `00`. Once mined, commits a **Blockchain Certificate** containing nonce, previous hash, and block index to the local verifiable ledger.

---

## 🎓 Governing Exam Boards & Strategic Partnerships Ecosystem

Aligns regional educational standards with funding sandboxes:

### 🧠 1. Accredited Exam Boards (Region-Sensitive Question Banks)
- **Local Governing Bodies**: Filter exam questions from **WAEC/JAMB/NECO** (Nigeria), **KNEC** (Kenya), **College Board AP** (USA), **Ofqual** (UK), **CBSE** (India), and **UMALUSI** (South Africa).
- **Accredited Step-by-Step Solutions**: Formats science/math solutions into verified 3-step compliant paths.

### 🌐 2. Strategic Partnerships & Funding Hub (Ecosystem Support)
- **Alliance Pathways**: Connects Stanford, UNICEF STEM Fund, NITDA, Y Combinator, and Sequoia.
- **Strategic Pitch Builder**: Enter Project Name, Requested Support, and an Entrepreneurial Pitch to submit sandbox funding requests.

---

## 🗄️ Architecture & Database Schema

NeuroLearn AI is architected using **MVVM** and modern **Room Database** local storage.

### Core Data Models (Latest Version: 16)
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

@Entity(tableName = "research_papers")
data class ResearchPaper(
    @PrimaryKey val id: String,
    val title: String,
    val authors: String,
    val abstractText: String,
    val content: String,
    val category: String,
    val coinCost: Int,
    val isPurchased: Boolean,
    val rating: Double,
    val reviewsCount: Int,
    val fileSizeKb: Int,
    val publishYear: Int,
    val publisherName: String
)

@Entity(tableName = "blockchain_certificates")
data class BlockchainCertificate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val recipientName: String,
    val title: String,
    val sourceName: String,
    val type: String, // e.g. "RESEARCH" or "MILESTONE"
    val blockNumber: Int,
    val nonce: Int,
    val previousHash: String,
    val hash: String,
    val transactionHash: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 🏷️ TestTag Reference for UI Testing

| UI Element / Dialog | TestTag ID | Description |
| :--- | :--- | :--- |
| **Home: Talent Hub Card** | `home_talent_hub_card` | Navigation card on HomeScreen to open Talent Hub. |
| **Talent Hub Root** | `talent_hub_screen_root` | Screen container for Standalone Employers & Learners Hub. |
| **Talent Back Button** | `talent_hub_back_button` | Back arrow returning from Talent Hub to Home. |
| **Tab: Browse Pool** | `talent_hub_tab_browse_pool` | Tab to search/view directory of candidates. |
| **Tab: My Profile** | `talent_hub_tab_my_profile` | Tab to register candidate listing and view proposals. |
| **Talent Search Field** | `talent_pool_search` | Search box for talents directory. |
| **Candidate Card** | `talent_card_<id>` | Individual candidate card displaying credentials. |
| **Engage Candidate** | `engage_talent_btn_<id>` | Launches recruiter direct proposal form. |
| **Submit Proposal** | `submit_engagement_offer_btn` | Sends contract pitch to candidate's inbox. |
| **Proposal Item** | `inbox_offer_<id>` | Individual received contract offer card. |
| **Accept Offer** | `accept_offer_btn_<id>` | Changes status of proposal to ACCEPTED. |
| **Decline Offer** | `decline_offer_btn_<id>` | Changes status of proposal to DECLINED. |

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
