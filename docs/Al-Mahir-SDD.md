# Al-Māhir (الماهر) — System & Product Specification
**Version:** 0.1 (July 2026)  
**Objective:** AI-Powered Qur'an Recitation, Memorization & Assessment Platform  
**Target Architectures:**  Android (Kotlin/Jetpack Compose), Backend (gRPC/REST), and AI/ML pipeline  

---

## 1. System Priorities & MOSCOW Mapping
To guide MVP development and subsequent release phases, all platform specifications are strictly prioritized using the MoSCoW framework:

* **MUST (M):** Non-negotiable core capabilities required for a viable, secure, and compliant production release.
* **SHOULD (S):** Vital capabilities that enrich the UX, support core user-retention mechanisms, or provide essential offline coverage.
* **COULD (C):** Non-critical, high-value visual enhancements, widgets, or peripheral features.
* **LATER (L):** Post-MVP Phase 2 epics involving human-led interaction, social frameworks, or highly complex predictive models.

---

## 2. Functional Specification (Epics, Features & User Stories)

### Epic 1: Authentication & User Management (AUTH)
* **Priority:** MUST
* **Description:** Secure identity creation, persistent session state handling, and localized credential recovery.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **AUTH-01** | Email/Password Registration | **MUST** | As a new user, I want to register using my email and password, so that my progress is securely saved. | Password must be >= 8 characters, containing an uppercase letter, lowercase letter, number, and special character. |
| **AUTH-02** | Google Sign-In | **MUST** | As a user, I want to register and authenticate using my Google Account, so that I can access the app quickly. | Integration via Google Identity Services SDK. Fallback graceful UI alerts on network timeouts. |
| **AUTH-03** | Apple Sign-In | **MUST (iOS)** | As an iOS user, I want to authenticate via Sign in with Apple, so that I can securely access my account. | Required for App Store approval. Implements nonce verification and secure keychain storage. |
| **AUTH-04** | Guest Mode | **SHOULD** | As a first-time visitor, I want to use the application without creating an account, so that I can explore the app. | Local-only access. **Strict restrictions apply:** No cloud sync, no bookmarks, no session history, no reminders, and no exam history. |
| **AUTH-05** | Login with Saved Data Access | **MUST** | As a registered user, I want to log in, so that I can sync and access all my cloud-saved historical progress. | Sync local SQLite database with backend server state upon successful JWT token issuance. |
| **AUTH-06** | Persistent Sessions | **SHOULD** | As a user, I want my login session remembered, so I do not have to sign in manually every time I launch the app. | Refresh token rotated automatically in native Secure Storage (iOS Keychain / Android EncryptedSharedPreferences). |
| **AUTH-07** | Secure Logout | **MUST** | As a user, I want to log out of my profile, so that my private account details and cached files stay secure. | Clears local session caches, active database bindings, and invalidates JWT tokens on the backend. |
| **AUTH-08** | Password Reset | **MUST** | As a user, I want to reset my password via email link, so that I can regain access to my account if forgotten. | Generates a secure, 15-minute expiring link. Prevent accounts from being enumeration-tested in the public API. |
| **AUTH-09** | Email Verification | **SHOULD** | As a new user, I want to verify my email address, so that my account is validated and secure. | Dispatches an verification OTP code immediately after signing up. block cloud-sync until verified. |

---

### Epic 2: Profile & Account Management (PROF)
* **Priority:** SHOULD
* **Description:** Personalization settings, user profile customization, and self-service privacy controls.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **PROF-01** | Edit Profile Details | **SHOULD** | As a registered user, I want to modify my profile options (name, photo, app language, preferred reciter, daily goal, number of modod), so that the application adapts to my preferences. | "Number of modod" refers to tajwid elongation preferences (e.g., 2, 4, or 6 harakat for madd rules). |
| **PROF-02** | Secure Password Change | **MUST** | As a registered user, I want to change my password securely from within my profile, so that I keep my account safe. | Requires verification of the current password before accepting any new inputs. |
| **PROF-03** | Permanent Account Deletion | **MUST** | As a registered user, I want to permanently delete my account, so that all my personal data is removed from your systems. | Must trigger hard deletion of all recording files, historical statistics, and credentials from servers under GDPR. |
| **PROF-04** | Privacy Self-Service | **MUST** | As a privacy-conscious user, I want to download my personal data, delete voice recordings, and manage OS permissions. | Provide single-tap controls to delete all voice data while preserving progress stats. |

---

### Epic 3: Mushaf - Reading (MUS)
* **Priority:** MUST
* **Description:** High-fidelity, authentic Qur'an presentation engine featuring vector-perfect text scaling.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **MUS-01** | Launch to Mushaf | **MUST** | As a user, I want the app to launch directly to the Mushaf screen, so that I can immediately begin reading. | Default landing page if authenticated. Render pipeline must load under 400ms. |
| **MUS-02** | Continue Last Reading | **MUST** | As a returning user, I want the app to remember my exact reading position, so that I can resume exactly where I left off. | Store active page, surah index, and ayah offset in local db state; sync to cloud immediately. |
| **MUS-03** | Browse Surahs | **MUST** | As a user, I want to browse an index of all Surahs, so that I can quickly navigate to any specific chapter. | Provide structured indexes categorizing chapters by Surah name, Juz', or page. |
| **MUS-04** | Display Mushaf Authentically | **MUST** | As a user, I want the pages to render exactly like the official printed Mushaf, showing right/left distinctions, so I read comfortably. | Must utilize official QUL vector fonts. **Strictly forbid using brand fonts for Qur'an text** to prevent rendering errors. |
| **MUS-05** | Resume After Interruptions | **SHOULD** | As a user, I want my reading state to remain intact after a system interruption, so that I do not lose my place. | Automatically save application state when backgrounded by phone calls or incoming push alerts. |
| **MUS-06** | Tap-Ayah Bottom Sheet | **MUST** | As a reader, I want to tap any Ayah to trigger an action menu, so that I can quickly view translation, bookmarks, or tafsir. | Modal bottom sheet must open with micro-interactions, supporting bookmarking, copying, and sharing. |

---

### Epic 4: Search & Index (SRCH)
* **Priority:** SHOULD
* **Description:** Multi-modal lookup engine supporting exact textual searches, metadata filtration, and semantic query matching.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **SRCH-01** | Search by Surah | **MUST** | As a user, I want to search for a Surah by entering its name, so that I can navigate to it quickly. | Supports exact and fuzzy matching in both Arabic and English transliterations. |
| **SRCH-02** | Search by Surah & Ayah | **MUST** | As a user, I want to search using both Surah and Ayah numbers, so that I can jump directly to a precise verse. | Formats accepted: `Surah:Ayah` (e.g., `2:255`). Validate inputs against actual structural bounds. |
| **SRCH-03** | Keyword Text Search | **MUST** | As a user, I want to run a text search for specific words within the Qur'an, so that I can find related verses. | Locally indexed SQLite search (FTS5) for instantaneous queries, utilizing diacritic-insensitive matching. |
| **SRCH-04** | Semantic Search | **SHOULD** | As a user, I want to search by semantic meaning, so that I can find verses related to abstract concepts. | Translates abstract queries (e.g., "Patience", "Charity") via backend vector-embeddings. |
| **SRCH-05** | Recent Searches | **COULD** | As a user, I want to view my recent search history, so that I can run previous queries faster. | Save the last 10 successful search queries locally. Clear on request or during guest mode session close. |
| **SRCH-06** | Typeahead Suggestions | **COULD** | As a user, I want typing suggestions in real time, so that searching becomes easier and more fluid. | Debounce user inputs by 150ms before querying the local index to prevent rendering stutter. |
| **SRCH-07** | Search Filtering | **SHOULD** | As a user, I want to filter my search results, so that I find relevant verses faster. | Filter options: Surah, Juz', Meccan/Medinan origins, Translation, and Tafsir sources. |
| **SRCH-08** | Open Search Result | **MUST** | As a user, I want to tap a search result, so that the app opens directly to that exact verse inside the Mushaf. | Jumps to target page, triggers a flash highlighting effect on the selected Ayah, and closes search sheet. |
| **SRCH-09** | Empty-State / Zero Results | **SHOULD** | As a user, I want informative feedback when search returns nothing, so that I know what to try next. | Display a "No results found" warning, and show suggested keywords, recent searches, or a browse button. |
| **SRCH-10** | Voice Search | **LATER** | As a user, I want to search for verses using my voice, so that I can search hands-free. | Post-MVP speech-to-text semantic parsing pipeline. |

---

### Epic 5: Listen Mode (LSN)
* **Priority:** MUST
* **Description:** High-fidelity audio player utilizing word-level highlight synchronization.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **LSN-01** | Start Listen Session | **MUST** | As a user, I want to start a listening session, so that I can listen to the Qur'an while following along on screen. | Highlighting updates automatically based on timed offsets provided by the audio feed. |
| **LSN-02** | Select Reciter | **MUST** | As a user, I want to select my preferred reciter, so that I can enjoy the recitation in a voice I prefer. | Query remote CDN database of multiple qaris; present selected audio stream. |
| **LSN-03** | Playback Controls | **MUST** | As a user, I want playback controls (play, pause, resume, and scrub), so that I can listen at my own pace. | Seamless audio buffering. Scrubbing must snap dynamically to the nearest Ayah boundary. |
| **LSN-04** | Ayah Navigation | **MUST** | As a user, I want to skip between verses during playback, so that I can navigate to the exact part of the Surah. | Next/Previous skip buttons jump to the start of neighboring verses; updates screen layout immediately. |
| **LSN-05** | Word-by-Word Highlight | **MUST** | As a learner, I want each word highlighted as it is recited, so that I can track and study proper pronunciation. | Requires precise sub-second word-timestamp schemas synchronized directly with audio file playback. |
| **LSN-06** | Playback Speed Control | **SHOULD** | As a learner, I want to adjust the recitation speed (0.75x to 1.5x), so that I can closely study complex rules. | Linear pitch correction must be maintained across all speed variations to prevent audio distortion. |
| **LSN-07** | Background Playback | **SHOULD** | As a user, I want audio playback to continue in the background, so that I can keep listening while using other apps. | Integrates with native lockscreen and media systems (iOS Remote Command Center, Android MediaSession). |
| **LSN-08** | Download Audio | **SHOULD** | As a user, I want to download recitations, so that I can enjoy listening when I do not have active internet. | Cache downloaded audio locally in sandbox directories (`.m4a` or `.mp3` format). |
| **LSN-09** | Offline Listening | **SHOULD** | As a user, I want to play my downloaded recitations while offline, so that I do not use cellular data. | Playback engine must switch automatically to local storage paths if remote streams are unreachable. |
| **LSN-10** | Audio Error Handling | **SHOULD** | As a user, I want playback errors to be handled gracefully, so that I understand connection issues and can recover. | Provide explanatory retry prompts rather than hard crashes; automatically resume playback upon reconnect. |

---

### Epic 6: Ta'ahud - Live AI Correction (TAH)
* **Priority:** MUST
* **Description:** Real-time, interactive audio analysis engine evaluating spoken recitation against selected target ranges.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **TAH-01** | Start Session | **MUST** | As a user, I want to start a Ta'ahud session for a range of verses, so that I can practice recitation. | Allows selection of ranges from single verses up to full Surahs. |
| **TAH-02** | Request Microphone | **MUST** | As a user, I want the app to request microphone access with a clear pre-prompt, so I understand how it evaluates me. | Must explain privacy policies clearly prior to triggering native system OS permissions dialogues. |
| **TAH-03** | Voice-Activity Detection | **MUST** | As a user, I want the app to detect when I start/stop speaking, so that only actual recitation is captured. | Integrates voice activity detection (VAD) locally to save bandwidth and skip room noise. |
| **TAH-04** | Live Mistake Detection | **MUST** | As a user, I want the AI to analyze my recitation live, so that mistakes are identified as I speak. | **Mistake Taxonomy to Detect:** <br>• *Memorization:* Missing word, Extra word, Incorrect word, Incorrect sequence.<br>• *Tashkil:* Incorrect vowel, Missing vowel.<br>• *Tajwid:* Madd, Ghunnah, Idgham, Ikhfa, Iqlab, Qalqalah, Makhraj. |
| **TAH-05** | Real-Time Inline Highlight | **MUST** | As a user, I want my mistakes highlighted inline on the page, so that I immediately see where errors occurred. | Must render color codes coupled with assistive indicators: **(color + icon + text)**. |
| **TAH-06** | View Mistake Details | **MUST** | As a user, I want to tap on a mistake to read details, so that I understand what I did incorrectly. | Opens an overlay explaining the specific linguistic rule or missing word. |
| **TAH-07** | Live Session Feedback | **MUST** | As a user, I want my session stats to update continuously, so that I always know my current performance. | Updates overall accuracy indicators and error counts dynamically upon each completed verse. |
| **TAH-08** | Pause & Resume | **SHOULD** | As a user, I want to pause and resume my session, so that I can continue practice without losing progress. | Freezes system timer and audio analysis pipeline, keeping active session state in memory. |
| **TAH-09** | Finish Session | **MUST** | As a user, I want to end the session to process my full results, so that I can review final metrics. | Stops mic stream, concludes AI parsing, and generates a session overview. |
| **TAH-10** | View Session Summary | **MUST** | As a user, I want to view a comprehensive summary of my practice session, so that I identify my weak spots. | Displays overall scores alongside categorized charts tracking exact memorization, tajwid, and pronunciation mistakes. |
| **TAH-11** | Suggested Revision | **SHOULD** | As a user, I want the app to suggest specific revision material, so that I can focus on improving. | Evaluates errors to suggest target verses, specific tajwid rules, and recurring mistakes. |
| **TAH-12** | Save Session to History | **MUST** | As a user, I want completed sessions saved to my history, so that I can monitor my improvement over time. | Appends data to local history log; schedules remote server synchronization once connected. |
| **TAH-13** | Recover Recognition Failures | **SHOULD** | As a user, I want the app to handle recognition drops gracefully, so that I do not lose ongoing progress. | Restores pipeline automatically without breaking the session interface if connection drops briefly. |

---

### Epic 7: Mu'allem - Repeat After the Sheikh (MLM)
* **Priority:** SHOULD
* **Description:** Call-and-response educational flow structured around master imitation and structured repetition.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **MLM-01** | Start Guided Session | **SHOULD** | As a user, I want to start a guided session for a specific range, so that I practice my memorization systematically. | Defines the target range of verses; initializes the specialized call-and-response loop. |
| **MLM-02** | Select Sheikh | **SHOULD** | As a user, I want to select a reciter to listen to first, so that I hear the correct pronunciation before attempting it. | Streams high-quality audio files of the target verse from the selected reciter. |
| **MLM-03** | Set Repetition Count | **SHOULD** | As a user, I want to configure the repetition count for each verse, so that I can memorize it thoroughly. | Allows loop ranges from 1 to 10 repetitions per verse before moving forward. |
| **MLM-04** | Repeat After Sheikh | **SHOULD** | As a user, I want to record my recitation after the Sheikh finishes playing, so that I practice memorization. | Triggers mic recording state and UI prompts immediately after the primary audio finishes playing. |
| **MLM-05** | Evaluate Every Repetition | **SHOULD** | As a user, I want each of my repeated recordings evaluated by the AI, so that I know if I got it right. | Runs local/cloud evaluation on each try, feeding individual attempt logs to the session history. |
| **MLM-06** | Highlight Practice Errors | **SHOULD** | As a user, I want mistakes highlighted on my screen, so that I can see exactly where I went wrong. | Highlights mistakes dynamically after each user recording concludes. |
| **MLM-07** | Auto-Advance Verses | **SHOULD** | As a user, I want the app to move to the next verse automatically once I pass, so that my practice flows smoothly. | Advances to the next verse once the user meets the target repetition count with passing scores. |
| **MLM-08** | Recitation Style Selection | **COULD** | As an advanced user, I want to choose the speed/style of recitation (Tahqiq, Tadwir, or Hadr), so that I practice appropriately. | Configures reciter options and scales ASR temporal alignment parameters accordingly. |
| **MLM-09** | Session Restoration | **SHOULD** | As a user, I want to pause, resume, and restore interrupted sessions, so that I can practice later. | Restores active parameters and progress indicators following incoming interruptions. |
| **MLM-10** | Practice Session Complete | **SHOULD** | As a user, I want to end my session and review a summary, so that I can track my overall progress. | Saves final stats and displays progress dashboards for all completed recitation runs. |

---

### Epic 8: Ikhtibar - Exam (IKH)
* **Priority:** SHOULD
* **Description:** formal assessment modules generating customized test prompts to evaluate memorization retention.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **IKH-01** | Create Exam | **SHOULD** | As a user, I want to create a new exam, so that I can formally test my overall memorization accuracy. | Starts a clean test state; records starting timestamps and disables active inline hints. |
| **IKH-02** | Select Verse Range | **SHOULD** | As a user, I want to choose the exact range of verses to test, so that the exam covers only what I have studied. | Restricts exam generation to user-defined lists or selected Surah boundaries. |
| **IKH-03** | Select Exam Strategy | **SHOULD** | As a user, I want to choose my examination strategy, so that I can test my memory in different ways. | Strategies: Spot-check, complete-from-memory, or start-to-finish recitation. |
| **IKH-04** | Auto-Generate Questions | **SHOULD** | As a user, I want the exam questions generated automatically, so that every test attempt is unique. | Backend engine creates randomized exam prompts based on the chosen strategy. |
| **IKH-05** | Play Exam Prompt | **SHOULD** | As a user, I want the app to play the start of a verse as a prompt, so that I know where to continue reciting. | Plays audio for the first few words, then stops and prompts the user to recite the rest from memory. |
| **IKH-06** | Recite Answers | **SHOULD** | As a user, I want to recite my answers to receive live AI scoring, so that I know if I made mistakes. | Mic recording captures voice responses, feeding speech directly to the verification system. |
| **IKH-07** | Auto-Complete Exam | **SHOULD** | As a user, I want the exam to conclude automatically when finished, so that I receive my final grade. | Stops the mic, stops the timer, calculates final scores, and generates the exam report. |
| **IKH-08** | View Exam Results | **SHOULD** | As a user, I want to view my final exam score and detailed metrics, so that I understand my overall performance. | **Display Requirements:** Score, Accuracy percentage, total mistakes (broken down by tajwid, memorization, and tashkeel), duration, and grade. |
| **IKH-09** | Review Exam Mistakes | **SHOULD** | As a user, I want to review each mistake I made during the exam, so that I can focus on my weak points. | Highlights specific errors on the script, allowing users to tap on each for detail panels. |
| **IKH-10** | Export Exam Results | **SHOULD** | As a user, I want to export my exam results as a PDF or image, so that I can share them with my teacher. | **Export contents:** Student name, Date, Score, and list of Mistakes in PDF/Image formats. |
| **IKH-11** | Share Exam Results | **SHOULD** | As a user, I want to share my results via system share sheets, so that my teacher can easily review my progress. | Integrates with native iOS and Android sharing frameworks. |
| **IKH-12** | View Previous Exams | **COULD** | As a user, I want to view a log of my past exams, so that I can track my overall progress over time. | Displays a history scroll containing dates, scores, and tested verse ranges. |
| **IKH-13** | Recover Interruptions | **SHOULD** | As a user, I want my exam to recover gracefully from unexpected interruptions, so that I do not lose progress. | Auto-saves exam state upon incoming call or minimized state, allowing resume from active question. |

---

### Epic 9: In-Session Feedback Bar (FBK)
* **Priority:** MUST
* **Description:** Context-aware feedback layer displaying vital statistics and hints during practice and assessment.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **FBK-01** | Live Session Accuracy | **MUST** | As a user, I want to see my live accuracy score in the feedback bar, so that I know how well I am performing. | Displays a real-time percentage score updated dynamically by the evaluation stream. |
| **FBK-02** | Running Mistake Count | **MUST** | As a user, I want to see my running mistake count, so that I can monitor my performance during recitation. | Tracks total mistakes on the current page; increments immediately as errors are detected. |
| **FBK-03** | Toggle Ayah Visibility | **MUST** | As a memorization student, I want to hide verses, so that I can practice reciting them from memory. | Blurs or hides the Mushaf text, displaying only the target verse bounds to encourage memorization. |
| **FBK-04** | Toggle Mistake Highlight | **SHOULD** | As a user, I want to toggle mistake highlighting on and off, so that I can customize my learning style. | Instantly shows or hides inline highlights across the current page. |
| **FBK-05** | Show Next Word (Hint) | **SHOULD** | As a memorization student, I want to reveal only the next word as a hint, so that I get help without exposing the whole verse. | Reveals only the immediate next word; adds a slight penalty to active session score. |
| **FBK-06** | Reveal Remaining Ayah | **SHOULD** | As a user, I want to reveal the rest of the current verse, so that I can continue if I get completely stuck. | Uncovers the full text of the active verse; marks the verse as containing a prompt error. |
| **FBK-07** | Open Past Mistakes | **SHOULD** | As a user, I want to access past mistakes made on this verse or page, so that I can review recurring errors. | Taps indicator to slide up a list of historically flagged errors on the active passage. |
| **FBK-08** | Minimize Visual Distraction | **SHOULD** | As a user, I want my feedback to be clean and non-intrusive, so that I can focus entirely on my recitation. | Utilizes clean typography and subtle transitions; satisfies WCAG accessibility criteria. |

---

### Epic 10: Bookmarks & Collections (BMK)
* **Priority:** SHOULD
* **Description:** Saved passages management, featuring categorization and offline accessibility.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **BMK-01** | Bookmark Ayah | **MUST** | As a user, I want to bookmark specific verses, so that I can easily return to them later. | Simple toggle control saves the target verse identifier to the local database. |
| **BMK-02** | Remove Bookmark | **MUST** | As a user, I want to remove bookmarks, so that I can keep my saved list clean and organized. | Tapping the active bookmark icon prompts deletion, removing it from active lists. |
| **BMK-03** | View All Bookmarks | **MUST** | As a user, I want to view all my bookmarks in one place, so that I can quickly access saved verses. | Displays a centralized index organized chronologically or grouped by Surah. |
| **BMK-04** | Organize Collections | **SHOULD** | As a user, I want to organize my bookmarks into custom collections, so that I can separate study topics. | Provides options to create, rename, and delete named folders containing selected bookmarks. |
| **BMK-05** | Search Bookmarks | **COULD** | As a user, I want to search within my bookmarks, so that I can quickly find specific saved verses. | Runs text search over the bookmarked verses index and user-added notes. |
| **BMK-06** | Add Notes to Bookmarks | **COULD** | As a user, I want to add personal notes to my bookmarks, so that I can remember why I saved a verse. | Provides text fields on bookmarks allowing custom notes of up to 500 characters. |
| **BMK-07** | Offline Bookmark Access | **SHOULD** | As a user, I want my bookmarks available offline, so that I can continue studying anywhere. | Bookmarked text, notes, and collections metadata are cached locally to remain accessible offline. |

---

### Epic 11: Tafsir & Translation (TFS)
* **Priority:** SHOULD
* **Description:** Multi-source translation datasets and scholarly interpretations accessible offline.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **TFS-01** | View Translation | **MUST** | As a user, I want to read translations alongside verses, so that I understand the meaning of the Qur'an. | Renders translation text inline under the selected verse within bottom sheets or split views. |
| **TFS-02** | Switch Translation Language | **SHOULD** | As a user, I want to switch translation languages, so that I can study in my preferred language. | Supports multiple verified translation sets (e.g., Arabic, English, Urdu). |
| **TFS-03** | Read Short Tafsir | **SHOULD** | As a learner, I want to view a brief explanation of the verse, so that I can quickly grasp its context. | Displays summary interpretations (e.g., Tafsir al-Jalalayn) within the detail sheets. |
| **TFS-04** | Read Detailed Tafsir | **SHOULD** | As a learner, I want to read detailed scholarly Tafsir, so that I can study the verses deeply. | Loads detailed exegesis text (e.g., Tafsir Ibn Kathir) on demand with scrolling capabilities. |
| **TFS-05** | Select Tafsir Source | **COULD** | As a user, I want to choose my preferred Tafsir source, so that I can study from trusted references. | Provides selection menu within settings to swap between available scholarly sources. |
| **TFS-06** | Offline Tafsir & Translation | **SHOULD** | As a user, I want translation and Tafsir datasets available offline, so that I can study without internet. | Prompts users to download localized translation/tafsir packages to local storage. |

---

### Epic 12: Session History (HIS)
* **Priority:** SHOULD
* **Description:** Detailed session reporting showing chronological progress, mistake breakdowns, and trends.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **HIS-01** | View Session History | **MUST** | As a user, I want to view all my previous practice and exam sessions, so that I can track my learning journey. | Lists sessions in chronological order, showing quick summaries of scores and types. |
| **HIS-02** | View Session Details | **MUST** | As a user, I want to tap on a past session to view its details, so that I can review my performance. | **Display requirements:** Verse range, total mistakes, categories, accuracy, and duration. |
| **HIS-03** | View Mistake History | **SHOULD** | As a user, I want to review all my past mistakes, so that I can practice avoiding them. | Displays a dedicated "mistake bank" compiling verses where the user has struggled historically. |
| **HIS-04** | Compare Progress | **SHOULD** | As a user, I want to compare my sessions side-by-side, so that I can measure my overall progress. | Compares current sessions with historical baselines; indicates improvement or regression. |
| **HIS-05** | View Learning Statistics | **SHOULD** | As a user, I want to view aggregated learning statistics, so that I understand my progress at a glance. | **Display metrics:** Total sessions, reading time, average score, average mistakes, best score, and streak. |
| **HIS-06** | Personalized Recommendations | **COULD** | As a user, I want recommendations based on my historical mistakes, so that I know what to practice. | Parses error frequency to highlight specific sections or rules that need revision. |
| **HIS-07** | Filter Session History | **COULD** | As a user, I want to filter my history, so that I can quickly find specific previous sessions. | Filters history lists by session type (Ta'ahud, Exam, Mu'allem) or date range. |
| **HIS-08** | Delete Session History | **SHOULD** | As a user, I want to delete specific sessions, so that I can manage my storage and progress data. | Prompts for deletion of selected records from local databases and remote servers. |
| **HIS-09** | Export Progress Report | **COULD** | As a user, I want to export my overall progress report, so that I can share it with my teacher. | Generates structured PDF/CSV summaries detailing total study hours, average accuracy, and completed goals. |
| **HIS-10** | Sync History Across Devices | **SHOULD** | As a user, I want my history synchronized across devices, so that I can continue learning on any screen. | Runs quiet background synchronization to push local edits to the primary user profile. |

---

### Epic 13: Progress, Statistics & Gamification (PRG)
* **Priority:** SHOULD
* **Description:** Engagement systems tracking milestones, goals, consistency, and structural achievements.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **PRG-01** | Learning Dashboard | **SHOULD** | As a user, I want a unified dashboard, so that I can quickly understand my overall progress. | Displays: Current streak, total time, completed sessions, average accuracy, total mistakes, last activity, and Khatmah progress. |
| **PRG-02** | Track Daily Reading Goal | **SHOULD** | As a user, I want to set and track a daily reading goal, so that I maintain consistency. | Goal types: Pages, Ayahs, Minutes, or Sessions. Progress updates automatically, and goal completion is displayed. |
| **PRG-03** | View Daily Progress | **SHOULD** | As a user, I want to view today's progress, so that I can see how much I have completed. | Displays: Today's goal, completed percentage, remaining target, and active time spent. |
| **PRG-04** | Weekly & Monthly Reports | **COULD** | As a user, I want weekly and monthly reports, so that I can monitor my long-term improvement. | Displays: Reading days, sessions completed, accuracy average, total mistakes, and reading duration. |
| **PRG-05** | Track Reading Streak | **SHOULD** | As a user, I want my consecutive reading days tracked, so that I stay motivated. | Displays: Current streak, longest streak, last missed day, and streak status. |
| **PRG-06** | Accuracy Trend Charts | **COULD** | As a user, I want to see my accuracy trend over time, so that I know if my recitation is improving. | Renders interactive graphs showing average accuracy scores, improvement percentages, and regression alerts. |
| **PRG-07** | Analyze Mistake Trends | **COULD** | As a user, I want to analyze my recurring mistakes, so that I know where to focus my practice. | Highlights top Tajwid and Tashkeel mistakes, difficult verses, and frequently mispronounced words. |
| **PRG-08** | Track Khatmah Progress | **SHOULD** | As a user, I want to track my Khatmah completion, so that I know how much of the Qur'an I have completed. | Displays: Completion percentage, current Juz', remaining pages, estimated completion date. Generates a gold-accented achievement on completion. |
| **PRG-09** | Unlock Achievements | **COULD** | As a user, I want to unlock milestone achievements, so that I feel motivated to continue practicing. | Achievements: First session, first Surah completed, 7/30-day streak, first Khatmah, perfect exam, 100 consecutive correct verses. |
| **PRG-10** | Compare Historical Periods | **COULD** | As a user, I want to compare my current performance with previous periods, so that I can track my growth. | Compares: This week vs last week, this month vs last month, current vs previous accuracy, mistake reduction percentage. |
| **PRG-11** | Export Progress Stats | **COULD** | As a user, I want to export my progress statistics, so that I can share my study updates with my teacher. | Exports PDF summaries including charts, reading metrics, average accuracy, streaks, and Khatmah completion. |

---

### Epic 14: AI Intelligence & Personalization (AI)
* **Priority:** SHOULD
* **Description:** Algorithmic systems analyzing vocal accuracy, predicted difficulty, and custom learning plans.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **AI-01** | Show AI Confidence | **SHOULD** | As a user, I want to see the AI confidence level for every detected mistake, so that I know how reliable the evaluation is. | Surfaced transparently to the user interface. Clearly marks "uncertain" feedback. |
| **AI-02** | Explain Detected Mistakes | **SHOULD** | As a user, I want detailed explanations for my mistakes, so that I understand how to correct them. | Connects error types with targeted phonetic and tajwid rule explanations. |
| **AI-03** | Recommend Revision Verses | **COULD** | As a user, I want the AI to recommend verses to review, so that I can focus on my weaker areas. | Recommends focus areas based on the frequency and intensity of mistakes in recent sessions. |
| **AI-04** | Recommend Tajwid Rules | **COULD** | As a user, I want personalized Tajwid recommendations, so that I can work on my specific pronunciation issues. | Suggests specific exercises when the user repeatedly fails the same rules (e.g., Ghunnah, Qalqalah). |
| **AI-05** | Generate Study Plan | **COULD** | As a user, I want the AI to generate a personalized study plan, so that I study more efficiently. | Creates dynamic weekly routines matching user-configured daily goals with their error history. |
| **AI-06** | Recommend Learning Mode | **COULD** | As a user, I want the AI to recommend the best learning mode for me, so that my practice is effective. | Suggests Mu'allem mode if user makes heavy memorization errors, or Ta'ahud for minor vocal slips. |
| **AI-07** | Adaptive Difficulty | **COULD** | As a user, I want exam and practice difficulty to adapt to my skill level, so that I stay challenged but not frustrated. | Automatically adjusts prompts and question density based on historical accuracy levels. |
| **AI-08** | Smart Mistake Prediction | **LATER** | As a user, I want the AI to predict verses I am likely to struggle with, so that I can review them ahead of time. | Post-MVP: Analyzes general student data to predict personal struggles before they occur. |
| **AI-09** | Personalized Insights | **COULD** | As a user, I want AI-generated insights about my overall progress, so that I understand my strengths. | Provides text-based summaries showing improvement trends across specific recitation modes. |
| **AI-10** | Intelligent Reminders | **COULD** | As a user, I want study reminders based on forgetting curves, so that I review verses before forgetting them. | Calculates forgetting patterns using spaced-repetition models to trigger smart notifications. |
| **AI-11** | Report Incorrect AI | **SHOULD** | As a user, I want to report incorrect AI evaluations, so that future updates are more accurate. | Allows flagging questionable evaluations to send logs and audio to human-in-the-loop reviewers. |
| **AI-12** | AI Offline Fallback | **MUST** | As a user, I want the app to handle AI service interruptions gracefully, so that I can continue reading without crashes. | Disables live modes and displays clear explanation messages if connection is lost. |

---

### Epic 15: Notifications (NTF)
* **Priority:** SHOULD
* **Description:** Engagement systems and reminders with strict quiet-hour schedules.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **NTF-01** | Configure Preferences | **SHOULD** | As a user, I want to manage my notification preferences, so that I receive only the updates I choose. | Provides explicit toggle switches for each category of push notifications. |
| **NTF-02** | Daily Recitation Reminder | **SHOULD** | As a user, I want to receive a daily reminder, so that I can maintain my recitation habits. | Fires local notifications matching user-configured daily targets and times. |
| **NTF-03** | Streak Reminders | **COULD** | As a user, I want alerts when my reading streak is at risk, so that I do not lose my consistency. | Triggers local checks 4 hours prior to daily cycle ends to alert at-risk streaks. |
| **NTF-04** | Session Summary Alert | **SHOULD** | As a user, I want to be notified when my session analysis is complete, so that I can review my results. | Dispatches a local alert once background AI processing finishes for a session. |
| **NTF-05** | Exam Scored Notification | **SHOULD** | As a user, I want to receive a notification when my exam is scored, so that I can review my performance. | Tapping the alert opens the target exam summary screen directly. |
| **NTF-06** | Khatmah Achievement Alert | **COULD** | As a user, I want to be congratulated upon completing a Khatmah, so that I stay motivated. | Triggers a gold-accented celebration card and native congratulations alert. |
| **NTF-07** | Quiet Hours | **SHOULD** | As a user, I want notifications to respect my quiet hours, so that I am not disturbed. | Blocks non-critical alerts during user-specified quiet windows. |
| **NTF-08** | Permissions Guidance | **SHOULD** | As a user, I want clear guidance if notifications are disabled, so that I know how to enable them. | Renders in-app banners directing users to system OS settings if permissions are revoked. |

---

### Epic 16: Settings (SET)
* **Priority:** SHOULD
* **Description:** App localization, appearance options, font adjustments, and account configuration options.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **SET-01** | Change App Language | **MUST** | As a user, I want to change the application language, so that I can use the app comfortably. | Supports Arabic (RTL) and English (LTR) at launch. Reloads core UI views instantly. |
| **SET-02** | Select Translation Language | **SHOULD** | As a user, I want to choose my preferred translation language, so that I can understand the verses. | Configures translation fields across Mushaf, Search, and Session views. |
| **SET-03** | Enable Dark Mode | **SHOULD** | As a user, I want to switch between light and dark modes, so that I can read comfortably in low light. | Implements custom "Emerald Calm" dark palette while keeping Tashkil markings highly legible. |
| **SET-04** | Accessibility Settings | **MUST** | As a user, I want customizable accessibility options, so that the application is easier to use. | Configures screen reader support, audio descriptions, and high-contrast styling. |
| **SET-05** | Audio Settings | **SHOULD** | As a user, I want to configure my audio preferences, so that listening fits my environment. | Controls background streaming permissions, cache settings, and default volumes. |
| **SET-06** | Notification Settings | **SHOULD** | As a user, I want to manage notification preferences, so that I control active alerts. | Links directly to Epic 15 configuration dashboards. |
| **SET-07** | Privacy Settings | **MUST** | As a user, I want to control my privacy, so that I know how my data is handled. | Links to data download, recording deletion, and audio training opt-in tools. |
| **SET-08** | Account Settings | **SHOULD** | As a user, I want to manage my account, so that my details remain accurate and updated. | Configures email addresses, sync preferences, and linked authentication providers. |
| **SET-09** | Reset Settings | **COULD** | As a user, I want to restore default settings, so that I can undo configuration changes. | Clears local config files without deleting user progress or stored search history. |
| **SET-10** | Font Size Scaling | **SHOULD** | As a user, I want to adjust the text size, so that reading matches my vision needs. | Scales text size dynamically **without breaking Mushaf page alignment**. |

---

### Epic 17: Offline Support & Sync (OFF)
* **Priority:** MUST
* **Description:** Offline-first architecture allowing local queueing and automatic cloud synchronization.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **OFF-01** | Read Qur'an Offline | **MUST** | As a user, I want to access the Qur'an without internet, so that I can read anywhere. | Core Mushaf rendering engine and reading tracking must run fully offline. |
| **OFF-02** | Access Bookmarks Offline | **SHOULD** | As a user, I want my bookmarks available offline, so that I can continue studying anywhere. | Reads and updates local bookmark lists; queues changes if offline. |
| **OFF-03** | Offline History Access | **SHOULD** | As a user, I want to view previous sessions offline, so that I can review my progress. | Reads session statistics from the local cache when no network is available. |
| **OFF-04** | Download Audio Offline | **SHOULD** | As a user, I want to download recitations, so that I can listen to them offline. | Downloads audio segments directly to secure offline directories. |
| **OFF-05** | Local Queueing | **SHOULD** | As a user, I want my actions stored locally while offline, so that they synchronize automatically later. | Queues database modifications (bookmarks, streak updates) locally using transaction logs. |
| **OFF-06** | Automatic Synchronization | **SHOULD** | As a user, I want my changes synced automatically once connected, so my data stays consistent. | Detects internet, uploads queued changes, downloads cloud updates, and resolves conflicts. |
| **OFF-07** | Sync Status Alerts | **COULD** | As a user, I want to know my synchronization progress, so that I know my data is up to date. | Displays clean, non-intrusive indicator icons in profile or settings views. |
| **OFF-08** | Graceful Offline UI | **MUST** | As a user, I want unavailable online features handled gracefully, so that I understand what requires internet. | Gracefully disables AI modes and exam generation, keeping reading accessible with user guidance. |

---

### Epic 18: Platform Integrations (INT)
* **Priority:** COULD
* **Description:** System-level OS components to keep users engaged and simplify sharing.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **INT-01** | Home-Screen Widgets | **COULD** | As a user, I want widgets on my home screen, so that I can see my daily verse, streak, or quickly resume reading. | Implements iOS WidgetKit and Android AppWidgetProvider, updated daily via local schedules. |
| **INT-02** | Deep / Universal Linking | **SHOULD** | As a user, I want links to jump to specific verses (e.g., "read with me"), so that I can share passages. | Formats: `almahir://surah/{id}/ayah/{num}`. Gracefully falls back to browser page if app is missing. |
| **INT-03** | Voice Shortcuts | **COULD** | As a user, I want to trigger actions using system voice commands, so that I can start practicing hands-free. | Integrates Siri Shortcuts (iOS) and Android App Actions to trigger daily recitations. |
| **INT-04** | Native Share Sheet | **SHOULD** | As a user, I want to use native share sheets, so that I can easily share my summaries and exam results. | Formats clean share text, linking image exports directly to native OS sharing drawers. |
| **INT-05** | Smartwatch Companions | **LATER** | As a user, I want a watch app (Apple Watch / Wear OS), so that I can track my streak or glance at my daily progress. | Post-MVP watch extension showing quick stats, reading progress, and streak statuses. |

---

### Epic 19: Community & Sharing (P2)
* **Priority:** LATER
* **Description:** Post-MVP social features, study groups, and human-led teacher interactions.

| ID | Capability / Feature | Priority | User Story (As a... I want to... So that...) | Implementation & Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| **P2-HUF** | Huffaz (1-to-1 Teacher) | **LATER** | As a student, I want to browse verified teachers, view profiles, and book live evaluation sessions. | Includes live audio calls, scheduling tools, and rating systems. |
| **P2-HAL** | Halaqa (Group Sessions) | **LATER** | As a user, I want to join group Halaqa sessions, so that we can listen and recite together with a Sheikh. | Group voice channels supporting role-based permissions (moderator, reader, listener). |
| **P2-SHR** | Social Sharing | **LATER** | As a user, I want to share my study progress, exam grades, and annotated verses on social networks. | Integrates social graph features directly with native sharing APIs. |

---

## 3. Non-Functional Requirements (NFRs)
Cross-cutting constraints and quality attributes required across all platforms:

### A. Privacy & Data Protection (NFR-PRV)
* **Priority:** MUST
* **PRV-01 (Explicit Retention Policy):** Clear privacy terms must specify what audio is saved, how long, and if it is used for training.
* **PRV-02 (Data Erasure):** Provide simple profile tools to let users delete all voice recordings and download their personal data history.
* **PRV-03 (Security Standards):** All user data must be encrypted using AES-256 at rest and TLS 1.3 in transit.
* **PRV-04 (Privacy Nutrition Label):** Must compile detailed data usage declarations matching App Store and Google Play privacy criteria.

### B. AI Accuracy & Honesty (NFR-ACC)
* **Priority:** MUST
* **ACC-01 (Metrics Tracking):** Must set and track precise precision and recall targets for each mistake type prior to release.
* **ACC-02 (Confidence Handling):** The client app must surface AI confidence scores; "uncertain" evaluations must be clearly marked.
* **ACC-03 (Domain-Specific ASR):** Must utilize speech recognition and forced-alignment models trained specifically for Qur'anic recitation.
* **ACC-04 (Decoupled Scoring):** The evaluation system must score Tashkil (vowels) and Tajwid (recitation rules) independently.

### C. Performance & Reliability (NFR-PRF)
* **Priority:** MUST
* **PRF-01 (Feedback Latency):** Live feedback must show errors on screen in under 1.0 second from speech.
* **PRF-02 (Startup Budget):** App launch time must be under 2.0 seconds; Mushaf vector pages must load within 400ms.
* **PRF-03 (Thermal and Battery):** Optimize pipeline processing to prevent thermal throttling or excessive battery drain over 1-hour sessions.
* **PRF-04 (Stability Targets):** Maintain a crash-free session rate of >= 99.9%; implement structured error logging for the audio pipeline.

### D. Audio & Microphone Pipeline (NFR-AUD)
* **Priority:** MUST
* **AUD-01 (Capture Pipeline):** Implements low-latency microphone capture with echo cancellation and a play-then-listen state machine.
* **AUD-02 (Background Audio):** Audio player must continue background playback during Listen mode unless interrupted.
* **AUD-03 (Interruption Handling):** Automatically pauses and gracefully recovers playback or recording when interrupted by calls or other audio.
* **AUD-04 (Hybrid ASR):** Implements a hybrid audio strategy: local VAD and light ASR on-device, with heavier scoring processed in the cloud.

### E. Localization & RTL (NFR-LOC)
* **Priority:** MUST
* **LOC-01 (Languages):** Full app interface localization in Arabic and English at a minimum.
* **LOC-02 (RTL Mirroring):** Full Right-to-Left (RTL) layout mirroring treated as a native mode, not an afterthought.
* **LOC-03 (Licensed Translations):** Incorporates legally licensed, verified translation datasets supporting multiple sources per language.
* **LOC-04 (Locale Formatting):** Displays numbers, times, and calendar dates using locale-aware formatting, including Hijri options.

### F. Accessibility (NFR-A11Y)
* **Priority:** MUST
* **A11Y-01 (Inclusive Indicators):** Recitation status (Correct, Almost, Mistake) must never be shown using color alone. Use combinations of **(icon + text + haptics/sound)**.
* **A11Y-02 (Screen Readers):** All interactive buttons and visual controls must feature descriptive VoiceOver (iOS) and TalkBack (Android) labels.
* **A11Y-03 (Font Scaling):** Interface text must scale with system font sizes without breaking Mushaf layout templates.
* **A11Y-04 (Contrast Legibility):** Emerald Calm dark mode must preserve sharp contrast ratios (>= 4.5:1) for Tashkil markings.
* **A11Y-05 (Multi-Pane Layouts):** Displays a two-page spread format when viewed on tablets and iPad devices.

### G. Availability & Offline Resilience (NFR-AVL)
* **Priority:** MUST
* **AVL-01 (Offline Core):** Reading, bookmarks, and settings must be fully available without an active internet connection.
* **AVL-02 (Graceful Degradation):** Cloud-dependent AI features must fail gracefully when offline; show clear help guides rather than freezing.
* **AVL-03 (Offline Queueing):** Saves and queues offline changes local database logs, syncing automatically when connection is restored.

### H. Security & Store Compliance (NFR-SEC)
* **Priority:** MUST
* **SEC-01 (Auth Security):** All user credentials and session tokens must use secure storage APIs (Keychain on iOS, Keystore on Android).
* **SEC-02 (Compliance Standards):** App matches all App Store and Play Store guidelines for microphone usage, background audio, and religious categorization.
* **SEC-03 (Permission Flow):** Implements a pre-prompt modal explaining microphone usage before displaying the native system request.

### I. Architecture & Cross-Team Contracts (NFR-ARC)
* **Priority:** SHOULD
* **ARC-01 (User-Data Service):** Implements a unified data microservice managing bookmarks, session logs, exams, and streaks.
* **ARC-02 (Shared Schemas):** Mobile, Backend, and AI teams must use shared ProtoBuf definitions to standardize scoring schemas.
* **ARC-03 (Unified Taxonomy):** Implements a shared Tajwid error taxonomy validated by Qur'anic scholars and language experts.
* **ARC-04 (Vector Embeddings):** Employs a dedicated embedding service to power semantic searches and concept matching.
* **ARC-05 (Typesetting Integrity):** Protects Mushaf page formatting by using official QUL vector assets. **Never use brand fonts for Quranic text**.

---

## 4. Technical Architecture: Shared Schemas (ProtoBuf Draft)
To maintain consistency across Mobile (iOS/Android), Backend, and AI engines, teams should refer to this standardized Protocol Buffer contract for session data and scoring:

```protobuf
syntax = "proto3";

package almahir.v1;

option go_package = "almahir/v1;almahirtypes";
option java_multiple_files = true;
option java_package = "com.almahir.v1";

enum MistakeCategory {
  MISTAKE_CATEGORY_UNSPECIFIED = 0;
  MISTAKE_CATEGORY_MEMORIZATION = 1;
  MISTAKE_CATEGORY_TASHKIL = 2;
  MISTAKE_CATEGORY_TAJWID = 3;
}

enum MistakeType {
  MISTAKE_TYPE_UNSPECIFIED = 0;
  
  // Memorization
  MISTAKE_TYPE_MISSING_WORD = 1;
  MISTAKE_TYPE_EXTRA_WORD = 2;
  MISTAKE_TYPE_INCORRECT_WORD = 3;
  MISTAKE_TYPE_INCORRECT_SEQUENCE = 4;
  
  // Tashkil
  MISTAKE_TYPE_INCORRECT_VOWEL = 5;
  MISTAKE_TYPE_MISSING_VOWEL = 6;
  
  // Tajwid
  MISTAKE_TYPE_MADD = 7;
  MISTAKE_TYPE_GHUNNAH = 8;
  MISTAKE_TYPE_IDGHAM = 9;
  MISTAKE_TYPE_IKHFA = 10;
  MISTAKE_TYPE_IQLAB = 11;
  MISTAKE_TYPE_QALQALAH = 12;
  MISTAKE_TYPE_MAKHRAJ = 13;
}

message VerseRange {
  int32 surah_number = 1;
  int32 start_ayah = 2;
  int32 end_ayah = 3;
}

message EvaluationMistake {
  MistakeCategory category = 1;
  MistakeType type = 2;
  int32 word_index = 3;         // Word position within the verse (0-indexed)
  string expected_text = 4;
  string spoken_text = 5;
  float confidence = 6;         // 0.0 to 1.0 confidence score
  bool is_uncertain = 7;        // Flagged if confidence is below threshold
}

message AyahFeedback {
  int32 ayah_number = 1;
  float accuracy_score = 2;
  repeated EvaluationMistake mistakes = 3;
}

message SessionLog {
  string session_id = 1;
  string user_id = 2;
  int64 timestamp = 3;
  VerseRange target_range = 4;
  float final_accuracy = 5;
  int32 duration_seconds = 6;
  repeated AyahFeedback feedback_records = 7;
}