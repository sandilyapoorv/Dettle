# Dettle Bare-Metal Agent Runtime & Gamified Developer Experience

**Date:** 2026-10-05  
**Status:** Draft / Proposed  
**Authors:** Dettle Core Architecture Team  

---

## 1. Executive Summary & Problem Diagnosis

### 1.1 The Current State: "A Chatbot Wrapped in Prompts"
Dettle was initially architected as a mobile AI interface with ambitious concepts: GNHF, Headroom, System Design Architectures, Build Mode, and Overnight Mode. However, under close inspection of the codebase:
- **Tool Disconnect:** `ToolExecutor` contains mock/stub responses for core capabilities (e.g. `cloudflare_deploy_preview` and `cloudflare_publish_worker` return `"Cloudflare deployment will be available in Phase 3."`, and `memory_recall` returns `"Memory recall not yet implemented."`).
- **Broken SSE Streaming for Function Calling:** In `OpenAICompatProvider`, streaming `delta.tool_calls` chunks are converted to strings per-delta without an accumulator assembling index, function name, and partial JSON arguments across SSE packets. Models often hallucinate tool calls into markdown text because native function call completion isn't cleanly resolved.
- **Unanchored Project Context:** Tools like `github_read_file` require `owner`, `repo`, and `branch` as prompt arguments because the agent has no active project anchor binding it to a user repository or workspace.
- **Missing Onboarding & Engagement:** Setting up API keys, GitHub tokens, and Cloudflare credentials requires diving into scattered settings screens. There is no guided on-ramp, and the app lacks user motivation or momentum loops.

### 1.2 The Target State: "Autonomous Developer Studio + Gamified Engagement"
1. **Bare-Metal Agent Runtime Engine (ARE):** A true ReAct loop that gives AI models bare-metal agency via real, wired APIs (GitHub, Cloudflare, local filesystem sandboxing, and background overnight task runners), while strictly maintaining zero compile load on the host mobile device by delegating heavy builds to GitHub Actions CI.
2. **Universal Gamified Onboarding ("Awaken the Machine"):** A 4-stage Duolingo-style quest that every first-time user plays through to configure their keys, verify bare-metal connectivity, anchor their first project, and unlock the app.
3. **Savage "Tough-Love" Developer Companion ("Sally the Enforcer"):** A Duolingo-like streak and XP system paired with an unhinged, sarcastic AI personality that roasts slacking users and celebrates green CI builds and successful deployments.

---

## 2. System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                    DETTLE CLIENT UI                                    │
│   ┌─────────────────────┐   ┌──────────────────────────────┐   ┌───────────────────┐   │
│   │ Gamified Top Bar    │   │  Environment Cockpit         │   │ Sally Companion   │   │
│   │ [🔥 Streak] [XP/Lvl]│   │  [Chat] [Build] [Overnight]  │   │ [Mood & Roasts]   │   │
│   └──────────┬──────────┘   └──────────────┬───────────────┘   └─────────┬─────────┘   │
└──────────────┼─────────────────────────────┼─────────────────────────────┼─────────────┘
               │                             │                             │
               ▼                             ▼                             ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              AGENT RUNTIME ENGINE (ARE)                                │
│                                                                                        │
│ ┌───────────────────────────┐ ┌───────────────────────────┐ ┌────────────────────────┐ │
│ │  Project Context Anchor   │ │ Streaming Tool Calling    │ │ Gamification & Reflex  │ │
│ │  • Active GitHub Repo     │ │ • SSE Delta Accumulator   │ │ • Daily Streak Tracker │ │
│ │  • Cloudflare Site/Worker │ │ • OpenAI / Gemini Native  │ │ • XP & Leveling Engine │ │
│ │  • Local Sandbox FS       │ │ • Multi-turn ReAct Loop   │ │ • Audio Chimes & Haptics│ │
│ └─────────────┬─────────────┘ └─────────────┬─────────────┘ └────────────┬───────────┘ │
│               │                             │                            │             │
│               └──────────────────────┬──────┴────────────────────────────┘             │
│                                      ▼                                                 │
│                        BARE-METAL TOOL EXECUTOR BUS                                    │
│       ┌──────────────────────┬──────────────────────┬──────────────────────┐           │
│       │    GitHub Engine     │  Cloudflare Engine   │  Local File Sandbox  │           │
│       │ • Trees, Commits     │ • Pages DirectUpload │ • /workspaces/<id>/  │           │
│       │ • Branch/PR, Actions │ • Worker Deployment  │ • Diff, Patch, Clean │           │
│       └──────────┬───────────┴──────────┬───────────┴──────────┬───────────┘           │
└──────────────────┼──────────────────────┼──────────────────────┼───────────────────────┘
                   ▼                      ▼                      ▼
           GitHub REST / CI       Cloudflare API v4       Android Internal FS
```

---

## 3. Pillar 1: Bare-Metal Tool Execution Bus & Streaming SSE Loop

### 3.1 Streaming Tool Argument Delta Accumulator
In OpenAI-compatible APIs (Groq, OpenRouter, DeepSeek), tool call arguments stream in partial chunks:
```json
{"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_123","function":{"name":"github_read_file","arguments":"{\"pa"}}]}}]}
{"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"th\":\"build.gradle\"}"}}]}}]}
```
**Architecture:**
- Create `StreamingToolCallAccumulator` in `com.dettle.app.data.api`:
  - Maintains a map of `index -> AccumulatedToolCall(id, name, argumentsBuffer)`.
  - Ingests streaming chunks until `finish_reason == "tool_calls"`.
  - Parses complete JSON arguments and emits a single, well-formed `ToolCall` event to the `ReActLoop`.
- Implement parallel support in `GeminiProvider` using Google Gemini's native `FunctionCall` protobuf structures.

### 3.2 Real-Wire Tool Implementations in `ToolExecutor`
Replace all stubbed string responses with actual service executions:
1. `cloudflare_deploy_preview`:
   - Reads files from the local workspace.
   - Invokes `CloudflareClient.deployToPages(projectName, files)`.
   - Returns live preview URL (`https://<deployment-id>.<subdomain>.pages.dev`).
2. `cloudflare_publish_worker`:
   - Invokes `CloudflareClient.deployWorker(scriptName, scriptCode, bindings)`.
   - Returns live worker URL (`https://<scriptName>.<subdomain>.workers.dev`).
3. `github_map_repo`:
   - Automatically uses the anchored active project repository if `owner`/`repo` are omitted by the model.
   - Invokes `RepoMapper.buildRepoMap(owner, repo, branch)`.
4. `github_create_branch_pr`:
   - Creates a feature branch, commits file array via GitHub Trees API, and opens PR.
5. `github_trigger_action` & `github_poll_run`:
   - Dispatches GitHub Actions workflows (`workflow_dispatch`).
   - Polls workflow run status (`in_progress` -> `completed`, `conclusion: success/failure`) so the agent can autonomously verify compilation and unit tests without touching local hardware.
6. `memory_recall` & `memory_store`:
   - Queries `MemoryDao` and `ConceptNodeDao` via Room FTS (full-text search) and semantic embeddings.

---

## 4. Pillar 2: Active Project & Workspace Anchor

### 4.1 Global Project Context (`ProjectContextManager`)
- **Entities:** Extends existing `ProjectEntity`:
  - `id`: UUID
  - `name`: Human-readable project name
  - `githubOwner`: GitHub username/org
  - `githubRepo`: Repository name
  - `defaultBranch`: `main` or custom
  - `cloudflareProjectName`: Linked Pages project
  - `localPath`: Internal directory `/data/user/0/com.dettle.app/files/workspaces/<id>`
  - `isAnchored`: Active flag
- **Cockpit UI Integration:**
  - A persistent Liquid Glass **Active Project Chip** in the top bar.
  - Tapping opens a quick-switcher sheet to change projects or spawn a new one.
  - When the agent executes tools, it inherits the active project context implicitly.

---

## 5. Pillar 3: Bare-Metal Codebase & API RAG Pipeline

Currently, Dettle only does basic episodic memory injection via `MemoryInjector` (retrieving notes/preferences from `MemoryDao`). It has **zero real Codebase RAG or Documentation RAG**, forcing the model to hallucinate repo architecture or flood the context window with raw files.

### 5.1 The 4-Stage Bare-Metal RAG Architecture
To give models true semantic intelligence over projects and APIs, we implement a dedicated **Codebase & Knowledge RAG Subsystem**:

```
 ┌──────────────────────┐   ┌───────────────────────┐   ┌──────────────────────┐
 │  Anchored Repo Files │   │ Cloudflare/Git Docs   │   │  Episodic Memories   │
 └──────────┬───────────┘   └───────────┬───────────┘   └──────────┬───────────┘
            │                           │                          │
            ▼                           ▼                          ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                         INGESTION & CHUNKING ENGINE                         │
 │  • AST / Symbol-aware chunker (functions, classes, interfaces, schemas)     │
 │  • Embedding generation (Local MediaPipe TFLite or Gemini text-embedding-4) │
 │  • Room DB storage: `CodeChunkEntity` + FTS4 Virtual Table                  │
 └──────────────────────────────────────┬──────────────────────────────────────┘
                                        │
                                        ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                     HYBRID RETRIEVAL & RERANKING BUS                        │
 │  • Lexical BM25 Search (FTS4 exact symbol matching: class, function, var)   │
 │  • Vector Cosine Similarity (Semantic intent matching)                      │
 │  • Reranking: Recency + Relevance + Active File Priority                    │
 └──────────────────────────────────────┬──────────────────────────────────────┘
                                        │
                                        ▼
 ┌─────────────────────────────────────────────────────────────────────────────┐
 │                    TOKEN-BUDGETED INJECTION & TOOL ACCESS                   │
 │  1. Pre-Prompt Injection: `<codebase_context>` (Strict 2k-4k token cap)     │
 │  2. Active ReAct Tool: `codebase_search(query, symbol_filter)`              │
 │  3. Prompt Cache Headers (Gemini Context Cache / Anthropic Cache Control)   │
 └─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Room Database Schema for Codebase RAG
- `CodeChunkEntity`:
  - `id`: Primary Key
  - `projectId`: Foreign Key to active anchored project
  - `filePath`: Relative repo file path (e.g. `src/auth/jwt.rs`)
  - `symbolName`: Function/Class name (e.g. `validate_session_token`)
  - `symbolType`: `FUNCTION`, `CLASS`, `INTERFACE`, `CONFIG`
  - `content`: Actual chunk code (lines of code with context)
  - `startLine`: Integer
  - `endLine`: Integer
  - `vector`: FloatArray (Embedding)
  - `updatedAt`: Timestamp
- `CodeChunkFtsEntity`: SQLite FTS4 virtual table for millisecond lexical matching.

### 5.3 Active ReAct Tool: `codebase_search`
In addition to automated pre-prompt injection, the agent is equipped with a native tool:
```json
{
  "name": "codebase_search",
  "description": "Semantically and lexically search the indexed codebase for functions, types, schemas, and implementations. Returns exact snippets with file paths and line ranges without wasting tokens on full files.",
  "parameters": {
    "query": "search query or symbol name",
    "filter_type": "all | function | class | interface"
  }
}
```

### 5.4 Token Budgeting & Caching Strategy
- **Token Allocator:** Context assembler enforces strict token ceilings (e.g., max 3,500 tokens for retrieved code chunks, 1,000 tokens for repo map, 500 tokens for user preferences).
- **Context Caching:** For providers supporting caching (Gemini 1.5/2.0, Anthropic), the static codebase index and system instructions are tagged with `cache_control: {"type": "ephemeral"}` to drastically reduce token latency and cost.

---

## 6. Pillar 4: Universal Gamified Onboarding ("Awaken the Machine")

### 6.1 The 4-Quest Onboarding Progression
Every user must complete the onboarding quest on first launch before entering the general studio:

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          QUEST 1: AWAKEN THE NEURAL CORE                         │
│  Connect at least 1 free AI Provider (Google AI Studio, Groq, OpenRouter)        │
│  Action: Enter API Key -> Tap "Test Neural Pulse"                                │
│  Feedback: Liquid neon animation + 50 XP awarded                                 │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          QUEST 2: BRIDGE TO BARE METAL                           │
│  Connect GitHub PAT and Cloudflare API Token                                     │
│  Action: Enter PAT / CF Token -> Tap "Establish Uplink"                          │
│  Feedback: Verified badge with repo count and Pages domain + 100 XP awarded      │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          QUEST 3: ANCHOR YOUR KINGDOM                            │
│  Select an existing GitHub repo or generate a fresh starter workspace           │
│  Action: Choose repo + Cloudflare site name -> Tap "Claim Project"               │
│  Feedback: Project initialized on local sandbox + 50 XP awarded                  │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          QUEST 4: FIRST LIGHT (INITIAL LAUNCH)                   │
│  Sally autonomously maps the repo, runs a 3-second live system check             │
│  Feedback: Confetti fanfare + Audio chime + Unlocks Level 1 "Sovereign Architect"│
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Pillar 5: Duolingo-Style Gamified Engine & Savage Duo Persona

### 7.1 Savage Developer Persona ("Sally the Enforcer")
Sally is an unhinged, development-focused companion with tough-love energy:
- **Active Streak (1+ days):** Hyped, proud, and aggressive.
  - *"Hell yeah. 4-day streak. Keep shipping and don't let these script kiddies catch up."*
- **Streak in Danger (< 4 hours to midnight):** Urgent, provocative.
  - *"Oi, wake up. Your streak dies in 3 hours. Write a commit or admit defeat."*
- **Streak Broken (3+ days inactive):** Brutal, hilarious roasts.
  - *"Look who crawled back. You are a fat fucking slow sloth like your mama—she took 9 months to make a joke. Get in the terminal and push some code."*
  - *"Did your keyboard break or did your ambition just dissolve into dust? 3 days offline is embarrassing."*
- **Green CI Build / PR Merged:** Celebratory hype.
  - *"CI is GREEN! 0 warnings, clean release. You might actually know what you're doing."*

### 6.2 XP, Levels, and Badges

#### Engineering Ranks:
- **Level 1 (0–200 XP):** *Script Apprentice*
- **Level 2 (201–500 XP):** *Syntax Operator*
- **Level 3 (501–1,000 XP):** *Full-Stack Artisan*
- **Level 5 (1,001–2,500 XP):** *Agent Orchestrator*
- **Level 10 (2,501–5,000 XP):** *Cloudflare Sovereign*
- **Level 20 (10,000+ XP):** *Bare-Metal Overlord*

#### XP Milestones:
- Daily Check-in / Coding Activity: `+25 XP`
- Tool Execution Success (Git, CF, Workspace): `+15 XP`
- Local File Edit & Diff Verified: `+30 XP`
- GitHub PR Created: `+75 XP`
- Cloudflare Pages Deployment Live: `+100 XP`
- Overnight Autonomous Run Completed: `+150 XP`
- GitHub Actions CI Green Build: `+100 XP`

#### Trophy Case (Badges):
- 🦉 **Night Owl:** Complete a 6-hour Overnight Mode run while sleeping.
- ⚡ **Edge Runner:** Deploy a Cloudflare Worker that passes live health check in < 15s.
- 🛡️ **Zero Regret:** Open an agent PR where GitHub Actions CI passes on run #1.
- 🔥 **Iron Streak (7-day / 30-day):** Keep shipping every single day.
- 🧊 **Streak Freeze:** Awarded for completing an Overnight task; auto-saves a missed day.

### 6.3 Procedural Audio & Micro-Haptics
To avoid heavy audio files and maintain 100% offline capability:
- Use Android's `ToneGenerator` and `AudioTrack` with synthesized dual-tone frequencies:
  - **Success Chime:** Ascending major triad (C5 -> E5 -> G5, 80ms each).
  - **Level-Up Fanfare:** Arpeggiated sequence (C5 -> G5 -> C6 -> E6).
  - **Roast / Streak Alert:** Low dissonant buzz (F#3 + G3, 150ms).
- Micro-haptic vibration patterns (`VibrationEffect.createWaveform`) on Android API 26+.

---

## 8. Pillar 6: Autonomous Background Loop (Overnight & GNHF)

### 8.1 Background Worker (`OvernightWorker`)
- Built on Android `WorkManager` with `ForegroundService` notification (`"Dettle Autonomous Agent Operating"`).
- Runs while device is charging/overnight.
- Work cycle:
  1. Pulls top pending task from `TaskQueueDao`.
  2. Loads anchored `ProjectContext`.
  3. Executes `ReActLoop` against workspace.
  4. Runs unit tests via `github_trigger_action` and polls GitHub CI.
  5. Commits to feature branch, opens PR.
  6. Awards `+150 XP` and saves summary log to Google Drive (`GoogleDriveConnector`) and local Room database.

---

## 9. Verification & Delivery Plan

1. **Unit Testing:**
   - Test `StreamingToolCallAccumulator` against partial SSE JSON chunks.
   - Test `GamificationEngine` streak calculation, XP awards, and level promotion.
   - Test `ToolExecutor` parameter mapping for GitHub and Cloudflare calls.
2. **Local Compilation Constraint:**
   - Zero `./gradlew` builds run on user host machine.
   - All compilation, linting, test suites, and APK builds executed via **GitHub Actions CI**.
3. **Artifact Generation & Release:**
   - GitHub Actions CI will build and attach versioned APKs:
     - `dettle-release-<version>.apk`
     - `dettle-debug-<version>.apk`
   - Strict release verification following Protocol A.

---

## 10. Conclusion
This architecture transitions Dettle from a passive prompt-based chat interface to a bare-metal autonomous developer agent runtime with an engaging, hilarious, Duolingo-inspired user experience.
