# Dettle — Build Tasks

## Phase 1: Foundation ✅ COMPLETE
- [x] Android project scaffold (Kotlin + Compose + Hilt + Retrofit)
- [x] AI Provider layer (OpenAI-compat + Gemini + KeyPoolManager round-robin)
- [x] Orchestrator Core (ReActLoop + ToolExecutor + SkillInjector + RepoMapper)
- [x] GitHub integration (GraphQL tree, file read, branch+commit+PR, trigger workflow)
- [x] Encrypted settings (ApiKeyStore — Android Keystore + EncryptedSharedPreferences)
- [x] DI module (AppModule — all singletons)
- [x] Chat UI (full Compose: streaming, tool cards, approval flow, welcome card)
- [x] SettingsScreen + SettingsViewModel
- [x] DettleNavGraph (5-screen bottom nav)
- [x] AgentForegroundService (persistent notification, START_STICKY)
- [x] Build config (ProGuard, gradle-wrapper.properties, libs.versions.toml)
- [x] CI/CD (.github/workflows/android-ci.yml — debug APK on push, signed release on tag)
- [x] .gitignore (blocks keystores, APKs, secrets)
- [x] AGENTS.md (18-rule permanent delivery protocol)

## Phase 2: WebView Bridge ✅ COMPLETE
- [x] SelectorRegistry — remote Gist JSON, 8 provider defaults, 6hr cache
- [x] DOMAutomationBridge — JavascriptInterface (chunk/complete/error/login/toolCall/ready)
- [x] WebViewAutomationScript — JS: React synthetic events, ProseMirror, MutationObserver, XML tool call detection
- [x] WebViewSession — per-provider hidden WebView, cookie persistence, login detection, rate limit handling
- [x] WebViewPool — singleton pool for all 8 providers, needsReauth StateFlow, priority waterfall
- [x] ApiKeyStore — isWebViewProviderEnabled() / setWebViewProviderEnabled() per provider
- [x] AgentForegroundService — WebViewPool starts on onCreate, destroys on onDestroy
- [x] AuthVaultScreen — overview of all 8 providers + fullscreen WebView login mode
- [x] AuthVaultViewModel — tracks session states, re-auth alerts
- [x] AIProviderType expanded — 8 WebView provider types (CHATGPT_WEB … QWEN_WEB)
- [x] Google Drive connector (OAuth2, drive.file scope, uploadTextFile, saveOvernightSummary, isConnected)

## Phase 3: Agent Stack ✅ COMPLETE
- [x] CloudflareClient — Pages Direct Upload, Workers deploy, token verify
- [x] DettleDatabase — Room DB (3 tables: memories, task_logs, deployments)
- [x] MemoryEntity + TaskLogEntity + DeploymentEntity
- [x] MemoryDao + TaskLogDao + DeploymentDao
- [x] MemoryInjector — retrieves relevant memories → prepends to every system prompt
- [x] AgentBus — 6 roles (ORCHESTRATOR/READER/CODER/REVIEWER/DEPLOYER/RESEARCHER) + live event bus
- [x] CoderReviewerDebate — CODER writes → REVIEWER critiques, max 3 rounds, early LGTM exit
- [x] McpClient — JSON-RPC 2.0 client for Serena (local) + Context7 (cloud docs)
- [x] AgentsScreen + AgentsViewModel — live agent fleet view, pulsing dots, event log
- [x] DeploymentsScreen + DeploymentsViewModel — deploy history, token status, tap-to-open URLs
- [x] ReposScreen + ReposViewModel — file tree browser, language icons, inline code viewer
- [x] TaskContext — expanded with isOvernightRun, language fields
- [x] GitHubClient — getRepoFileTree + readFile convenience wrappers added
- [x] Nav graph — wired all 3 Phase 3 screens (Repos, Deployments, Agents)
- [x] Room deps — added to build.gradle.kts
- [x] DI module — Phase 3 providers added (DB, DAOs, MemoryInjector, AgentBus, McpClient)

## Phase 4: Overnight Mode ✅ COMPLETE
- [x] OvernightLoop — gnhf-style autonomous execution loop, max runtime guard, graceful stop
  - [x] Dequeues tasks in priority order (CRITICAL → HIGH → NORMAL → LOW)
  - [x] Runs each task via full ReActLoop (all tools available)
  - [x] Stores outcomes in long-term memory (failures → corrections, successes → skills)
  - [x] Persists every task to Room task log
  - [x] Emits live OvernightState (phase, current task, log entries)
- [x] TaskQueue — persistent FIFO priority queue (serialized to EncryptedSharedPreferences)
  - [x] Supports 8 task types: CODE_FEATURE, CODE_REFACTOR, CODE_FIX, WRITE_TESTS, DEPLOY, DOCS, RESEARCH, CUSTOM
  - [x] 4 priority levels: CRITICAL, HIGH, NORMAL, LOW
  - [x] Retry failed tasks, clear completed, reorder
- [x] OvernightSummaryWriter — generates rich Markdown summary → pushes to GitHub + Drive
  - [x] GitHub: PR to owner/dettle-workspace repo with per-task results table
  - [x] Drive: uploads overnight-summary-YYYY-MM-DD.md to Dettle folder
- [x] SelfImprovingSkillUpdater — records failures as corrections, learns success patterns
  - [x] Per-task-type avoidance rules after repeated failures
  - [x] Efficient pattern detection (fast + low-token tasks noted for reuse)
- [x] OvernightScreen — full Compose UI
  - [x] Status banner animating through IDLE/STARTING/RUNNING/WRAPPING_UP/DONE/INTERRUPTED
  - [x] Start/Stop button with pending task count
  - [x] Priority-ordered task queue cards with type/priority badges
  - [x] Live terminal-style log scroll during run (tail -f style)
  - [x] FAB → Add Task bottom sheet (type/priority dropdowns, repo field)
- [x] OvernightViewModel — combines loop state + queue into single UI state
- [x] AgentForegroundService — OvernightLoop injected, ACTION_START_OVERNIGHT/STOP_OVERNIGHT intents
- [x] ApiKeyStore — overnight settings (maxHours, autoStart, scheduledTime, taskQueueJson, notifyOnComplete)
- [x] Nav graph — 6th tab "Overnight" (Bedtime icon) wired to OvernightScreen
- [x] DI module — SelfImprovingSkillUpdater + TaskQueue + OvernightSummaryWriter providers added

## Phase 5: The Cognitive Engine (Brain) 🔄 DONE
- [x] The Hippocampus (ExperienceConsolidator & "Dream Phase")
- [x] The Prefrontal Cortex (Local Vector RAG & Dynamic Context)
- [x] The Neocortex (Semantic Knowledge Graph nodes/edges)
- [x] The Amygdala (SalienceEvaluator & UI interrupts)
- [x] Motor Cortex (Procedural Tool Reflexes)

## Project totals
- 68 Kotlin source files
- 4 phases complete
- Docs: docs/dom_selectors.json (Gist template), AGENTS.md, .github/workflows/android-ci.yml

## Remaining (not blocking)
- [ ] gradlew wrapper — open in Android Studio to auto-generate OR run `gradle wrapper`
- [ ] google-services.json — required for Credential Manager OAuth to work (from Google Cloud Console)
- [ ] MCP settings in SettingsScreen (serenaBaseUrl, context7ApiKey fields)
- [ ] WebViewPool integration into KeyPoolManager waterfall (Track B fallback)
- [ ] PromptCacheManager (Gemini cachedContent API, Groq prefix caching) — Phase 5
- [ ] Overnight auto-schedule via WorkManager (alarm at keyStore.overnightScheduledTime)
- [ ] Polish: custom app icon, onboarding screen
