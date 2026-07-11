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
   - Continuous, universal learning globally is a fundamental right. NeuroLearn connects learners directly to the world's most trusted open-access academic and protocol libraries:
     * **arXiv Open Science Archive**: 16+ Million STEM preprints, advanced mathematical proofs, and neural systems research papers.
     * **MIT OpenCourseWare**: Complete curriculum blueprints, engineering schemas, and algorithms lectures notes.
     * **Project Gutenberg**: Historic scientific monographs, philosophy of science classics, and logical systems.
     * **PubMed Central (PMC)**: 100% free computational neuroscience, bioinformatics, and health informatics.
     * **W3C & IETF Protocol Specs**: Decentralized internet protocol RFCs, routing standards, and cryptography specifications.
     * **Internet Archive**: Universal literature, language manuscripts, and historical technology reviews.
3. **AI Research Librarian Synthesis**:
   - Select any library and set your specific focus topic.
   - The AI Librarian processes the collection to generate a tailored **Scholarly Study Brief** containing key textbook definitions, advanced proofs analysis, and Socratic study questions.
4. **Public Open-Access Publishing**:
   - If the synthesis is valuable, click **"Publish to Public Open Access Repository"** to save it straight to the local SQLite database.
   - Once saved, the study brief immediately appears on the decentralized marketplace with a **0-Coin Free Access** tier, allowing other learners globally to study, review, and mine certificates on it!
5. **Knowledge Marketplace**:
   - Free or open abstracts can be downloaded immediately.
   - Advanced technical papers can be unlocked using **NeuroCoins** (e.g., 30 coins), directly supporting scholarly authors.
   - Premium Max subscribers bypass all coin requirements with unlimited instant access.
6. **On-Chain Certification (Mining Simulation)**:
   - Upon studying a paper in full, the learner can opt to complete the study.
   - This launches a **Simulated Proof-of-Work (PoW) Mining Miner**.
   - The miner runs a live cryptographic loop on the device's JVM, searching for a SHA-256 hash that begins with `00` (representing network difficulty).
   - Once mined, the system issues a **Blockchain Certificate** containing a verifiable transaction hash (`0x...`), Block Number, Nonce, and previous block's SHA-256 hash.
   - Completing studies and mining certificates rewards the user with **+40 XP** to level up.
4. **Verifiable Ledger Verification**:
   - Inside the **Certificates** tab, the learner's entire block history is rendered.
   - A **Live Cryptographic Verification Check** button re-hashes all block headers mathematically:
     `SHA-256(recipientName | title | sourceName | type | previousHash | nonce)`
     to verify block authenticity and guarantee mathematical integrity live on-screen.

---

## 🎓 Governing Exam Boards & Strategic Partnerships Ecosystem

NeuroLearn AI introduces an innovative regionalized module aligning **Accredited National Exam Boards** with a **Global Venture, NGO & Institutional Alliance Network**:

### 🧠 1. Accredited Exam Boards (Region-Sensitive Question Banks)
- **Local Governing Bodies**: Students can dynamically filter and browse certified standard exams based on their country's regulatory authorities:
  - **Nigeria**: West African Examinations Council (WAEC), JAMB, NECO.
  - **Kenya**: Kenya National Examinations Council (KNEC - KCSE).
  - **United States**: College Board Advanced Placement (AP) Standards.
  - **United Kingdom**: Ofqual Standards (Edexcel & AQA syllabus).
  - **India**: CBSE Board national curricula benchmarks.
  - **South Africa**: UMALUSI National Senior Certificate benchmarks.
- **Accredited Step-by-Step Solutions**: Each mathematical or scientific question is served with a fully verified, board-compliant **3-Step procedural layout** detailing the conceptual formula, variables, calculation, and final solution with visual indicators.

### 🌐 2. Strategic Partnerships & Funding Hub (Ecosystem Support)
- **Alliance Pathways**: We map institutional connections across several major categories to provide real platform and funding support for active learner projects:
  - **Universities & Tech Colleges**: e.g., Stanford University, Nairobi Technical College.
  - **NGOs**: e.g., UNICEF STEM Fund, Mastercard Foundation.
  - **Government Parastatals**: e.g., National Information Technology Development Agency (NITDA).
  - **Accelerators & VCs**: e.g., Y Combinator (YC Academy Support), Sequoia Capital (Launchpad Syndicate), Techstars.
- **Strategic Pitch Builder (Funding Sandboxes)**: Learners can select any strategic partner and submit a formal, secure digital sandbox proposal (specifying Project Name, Lead Investigator, Requested Support, and an Entrepreneurial Pitch). 
- **Application History Log**: Active proposals are logged in real-time within the local SQLite ledger to monitor evaluation, sandbox matches, and funding allocations.

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

@Entity(tableName = "accredited_exam_questions")
data class AccreditedExamQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val country: String,
    val governingBody: String,
    val subject: String,
    val acreditationStatus: String,
    val questionText: String,
    val difficulty: String = "Medium",
    val step1Title: String,
    val step1Explain: String,
    val step2Title: String,
    val step2Explain: String,
    val step3Title: String,
    val step3Explain: String,
    val correctAnswer: String
)

@Entity(tableName = "platform_partners")
data class PlatformPartner(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val description: String,
    val fundingRange: String,
    val focusAreas: String,
    val supportProvided: String
)

@Entity(tableName = "partnership_applications")
data class PartnershipApplication(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val partnerId: String,
    val partnerName: String,
    val projectName: String,
    val applicantName: String,
    val pitchText: String,
    val fundingRequested: String,
    val status: String = "Pending Review",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_tasks")
data class ProjectTask(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val description: String,
    val assignedTo: String,
    val isCompleted: Boolean = false,
    val dueDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 📋 Collaborative Project Board & Mentor Milestone Integration

NeuroLearn AI integrates a comprehensive, real-time **Collaborative Project Management Board** allowing team members and mentors to track project roadmaps seamlessly.

### 🧠 How It Works
1. **Interactive Milestones**: Users can select from their active technology proposals or joined team projects.
2. **Team & Status Tracking**: Shows the project's real-time **Roster** of collaborators alongside a visual Material 3 **Completion Progress Bar** derived dynamically from task checkboxes.
3. **Seamless Mentor Integration**: 
   - If an expert mentor has been matched to the selected project under the **Mentor Match** tab, the board automatically fetches the match details (Mentor Name, Alignment, Analysis).
   - A single-click **"Import Milestones"** action parses the mentor's customized learning path (generated via Gemini AI or local intelligence) and populates them instantly as interactive tasks assigned directly to the mentor or student!
4. **Task Customization**:
   - Create custom collaborative tasks specifying Task Title, Description, Assigned Team Member, and Target Due Date.
   - Live status toggles reward progress with **+15 XP** upon completion of collaborative objectives.

---

## 🏷️ TestTag Reference for UI Testing

Every critical component includes `Modifier.testTag` declarations for effortless Robolectric, Espresso, and UI-driven automation:

| UI Element / Dialog | TestTag ID | Description |
| :--- | :--- | :--- |
| **Tab: Mentor Match** | `tech_hub_tab_mentors` | Switches active tab to the Mentor-Mentee panel. |
| **Tab: Repository** | `tech_hub_tab_repository` | Switches active tab to the Academic & Knowledge Marketplace. |
| **Tab: Certificates** | `tech_hub_tab_certificates` | Switches active tab to the Verifiable Blockchain Ledger. |
| **Research Cards** | `research_paper_card_<id>` | Individual card container for an academic paper. |
| **Buy Research Paper** | `buy_paper_btn_<id>` | Triggers coin purchase and unlocks full content. |
| **Study Research Paper** | `read_paper_btn_<id>` | Opens detailed study modal for a purchased paper. |
| **Mint Certificate** | `mint_research_certificate_btn` | Starts live SHA-256 cryptographic PoW mining. |
| **Verification Check** | `verify_hash_btn_<id>` | Triggers live mathematical hash verification. |
| **Reset Blockchain Ledger** | `reset_blockchain_ledger_btn` | Clears local database blockchain ledger blocks. |
| **Monetization Dashboard** | `monetization_panel` | Displays profile tier, coin balance, and upgrade CTA. |
| **Storefront Dialog** | `storefront_dialog` | Pop-up container displaying token packages. |
| **Launch Shop Button** | `open_store_btn` | Button that opens the NeuroCoins Token Shop. |
| **Starter Scholar Pack Purchase** | `buy_pack_starter` | Action card to purchase 100 coins for $1.99. |
| **Growth Pack Purchase** | `buy_pack_growth` | Action card to purchase 350 coins for $4.99. |
| **Premium Pass Purchase** | `buy_pack_premium` | Action card to purchase unlimited tier for $9.99. |
| **Upgrade Premium Button** | `upgrade_premium_btn` | Main CTA to instantly activate Premium Max. |
| **Exam & Partners Home Card** | `home_exam_partnerships_card` | Navigation card on the HomeScreen. |
| **Accredited Boards Tab** | `tab_questions_bank` | Tab to load regional exam question banks. |
| **Strategic Partnerships Tab** | `tab_partnerships` | Tab to load university/VC funding networks. |
| **Country Tab Filter** | `country_tab_<country>` | Clickable tag to switch local exam governing bodies. |
| **Subject Filter Chip** | `filter_chip_<subject>` | Filter chip to narrow down exam subject banks. |
| **Partner Strategic Card** | `partner_card_<id>` | Individual card representing a university, VC, or NGO. |
| **Submit Proposal Pitch** | `apply_partner_btn_<id>` | Action button to open proposal strategic builder. |
| **Proposal Strategic Form** | `apply_partnership_form` | Interactive form to enter pitch details. |
| **Submit Sandbox Pitch** | `submit_partnership_btn` | Sends the pitch to local SQLite persistence and alerts board. |
| **Tab: Project Board** | `tech_hub_tab_project_board` | Switches active tab to the Project Board panel. |
| **Project Selector** | `project_board_selector` | Clickable card that triggers the project selection dropdown. |
| **Project Board Progress** | `board_progress_card` | Card displaying dynamic project progress metrics. |
| **Import Milestones** | `import_mentor_milestones_btn` | Action button to parse and import matched mentor milestones as board tasks. |
| **Create Task Button** | `add_board_task_btn` | Opens the Add Task dialog. |
| **Submit Board Task** | `submit_board_task_btn` | Creates a new task and registers it under the active project. |
| **Task Card** | `task_card_<id>` | Container displaying individual task details. |
| **Task Status Toggle** | `task_checkbox_<id>` | Checkbox that registers task completion and awards XP. |
| **Delete Task Button** | `delete_task_btn_<id>` | Deletes task from active workspace. |
| **Tab: AI Tutor** | `tech_hub_tab_ai_tutor` | Switches active tab to the AI Tutor/Mentor panel. |
| **Select Project Dropdown** | `tutor_project_dropdown` | Opens dropdown to select one of your tech proposals. |
| **Persona Selectors** | `persona_chip_<Mentor/BugFixer/Tutor>` | Selects the AI assistant behavior. |
| **Query Input Field** | `ai_tutor_query_input` | Text field to enter architectural or code questions. |
| **Code Snippet Input** | `ai_tutor_code_input` | Text area to paste syntax or stack traces. |
| **Trigger Consultation** | `ai_tutor_synthesize_btn` | Sends inputs to Gemini API for deep analysis. |
| **Post Analysis to Feed** | `ai_tutor_publish_feed_btn` | Publishes AI advice to the collaborative Project Board feed. |
| **Unpaid Libraries Tab Toggle**| `academic_tab_unpaid_libraries` | Switches Academic Repository view to Unpaid Libraries. |
| **Library Card Selector** | `lib_card_<name_prefix>` | Selects an open global collection (arXiv, MIT, etc.). |
| **Research Topic Input** | `research_topic_input` | Text field to set specific scientific concept/focus. |
| **Synthesize Brief Button** | `lib_synthesize_btn` | Triggers the AI literature synthesizer. |
| **Publish Brief Button** | `lib_publish_to_repo_btn` | Saves synthesized textbooks to the public SQLite repository. |

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
