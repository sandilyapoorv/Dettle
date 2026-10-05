# Dettle Bare-Metal Agent Runtime & Gamified Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Dettle into an autonomous developer agent runtime with real Bare-Metal tool executions (GitHub, Cloudflare, Local FS sandbox), Codebase AST RAG, an anchored project context, a Duolingo-style gamified developer engine with savage "Sally the Enforcer" roasts, and a 4-quest onboarding flow.

**Architecture:** 
1. `StreamingToolCallAccumulator` in `com.dettle.app.data.api` handles multi-chunk SSE tool argument streaming for OpenAI/Groq/OpenRouter.
2. `ToolExecutor` is wired to real `CloudflareClient`, `GitHubClient`, `LocalWorkspaceManager`, and `CodebaseRagService`.
3. Room Database is augmented with `CodeChunkEntity`, `CodeChunkFtsEntity`, and `CodeChunkDao` for hybrid lexical/vector codebase RAG.
4. `GamificationEngine` manages daily streaks, XP, level promotions, trophy badges, and procedural audio chimes via Android `ToneGenerator`.
5. `GamifiedOnboardingScreen` acts as the mandatory 4-quest first-launch on-ramp.
6. Zero local build load: all compilation verification is offloaded to GitHub Actions CI.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room DB with FTS4, MediaPipe Text Embeddings, OkHttp, Kotlinx Serialization, Coroutines/Flow, Android ToneGenerator, GitHub Actions CI.

**Spec:** [docs/superpowers/specs/2026-10-05-bare-metal-runtime-and-gamified-onboarding-design.md](file:///home/shiv/Sandilyapoorv/Dettle/docs/superpowers/specs/2026-10-05-bare-metal-runtime-and-gamified-onboarding-design.md)

---

## Global Constraints
- Zero local Gradle builds (`./gradlew`) on the host machine to eliminate CPU/memory load.
- Android delivery standard: all builds verified on GitHub Actions CI.
- Version-named APK distribution: `dettle-release-<version>.apk` and `dettle-debug-<version>.apk`.
- Free-tier Cloudflare and GitHub constraints enforced.
- Dark theme: obsidian carbon (`#09090B`) with Apple Liquid Glass frosted acrylic surfaces.

---

## Tasks

### Task 1: Streaming Tool Delta Accumulator & Tool Calling Protocol
- [ ] Create `StreamingToolCallAccumulator.kt` in `data/api/` to aggregate streaming tool call chunks (`id`, `name`, `argumentsBuffer`).
- [ ] Update `OpenAICompatProvider.kt` to assemble tool calls using `StreamingToolCallAccumulator` and emit complete `StreamChunk.ToolCallDetected`.
- [ ] Update `GeminiProvider.kt` to ensure structured function call compatibility.
- [ ] Add unit tests in `app/src/test/java/com/dettle/app/api/StreamingToolCallAccumulatorTest.kt`.

### Task 2: Bare-Metal Tool Execution Bus Wiring
- [ ] Wire `cloudflare_deploy_preview` in `ToolExecutor.kt` to invoke `CloudflareClient.deployToPages()`.
- [ ] Wire `cloudflare_publish_worker` in `ToolExecutor.kt` to invoke `CloudflareClient.deployWorker()`.
- [ ] Wire `github_trigger_action` and add `github_poll_run` in `ToolExecutor.kt` and `GitHubClient.kt`.
- [ ] Wire `workspace_list_files`, `workspace_read_file`, `workspace_write_file`, `workspace_delete_file` with project workspace isolation.

### Task 3: Bare-Metal Codebase RAG & Hybrid Search Pipeline
- [ ] Create `CodeChunkEntity.kt`, `CodeChunkFtsEntity.kt`, and `CodeChunkDao.kt` in `data/db/`.
- [ ] Register new entities and DAOs in `DettleDatabase.kt`.
- [ ] Create `CodebaseChunker.kt` in `orchestrator/rag/` for AST/symbol-aware code chunking.
- [ ] Create `CodebaseRagService.kt` in `orchestrator/rag/` combining FTS lexical search and vector cosine similarity.
- [ ] Add tool `codebase_search` to `AgentTools` in `domain/model/Tool.kt` and implement in `ToolExecutor.kt`.
- [ ] Inject token-budgeted `<codebase_context>` into `ReActLoop.kt`.

### Task 4: Active Project Context Anchor
- [ ] Create `ProjectContextManager.kt` in `orchestrator/project/` to track and emit active project state (repo, branch, CF project, workspace path).
- [ ] Update `ToolExecutor` and `ReActLoop` to use active project context as default when tool args omit owner/repo.
- [ ] Create `ActiveProjectChip.kt` in `ui/components/` for the top app bar.

### Task 5: Gamification Engine & Savage Duo Persona
- [ ] Create `GamificationEngine.kt` in `orchestrator/gamification/` (daily streaks, XP milestones, levels, badges, streak freeze).
- [ ] Create `SallyEnforcerPersona.kt` with unhinged tough-love roasts for slacking (3+ days) and hype for shipping.
- [ ] Create `ProceduralAudioService.kt` using Android `ToneGenerator` for zero-asset offline sound chimes (success chime, level-up fanfare, alert buzz).
- [ ] Update `UserProfileEntity.kt` / `UserProfileDao.kt` to persist gamification state.
- [ ] Create `StreakXpChip.kt` in `ui/components/` for top bar display.

### Task 6: Universal Gamified Onboarding Quest ("Awaken the Machine")
- [ ] Create `GamifiedOnboardingScreen.kt` in `ui/onboarding/` with 4 interactive quests (Neural Core, Bare Metal, Anchor Kingdom, First Light).
- [ ] Wire live test pings for AI keys, GitHub PAT, and Cloudflare tokens with audio chimes and XP celebrations.
- [ ] Update `DettleNavGraph.kt` to make onboarding mandatory for new users before entering main studio.

### Task 7: Build Verification, CI Push & Release Packaging
- [ ] Stage, commit, and push all changes to GitHub `main`.
- [ ] Monitor GitHub Actions CI build until completion.
- [ ] Verify unit test passes and APK artifact outputs.
- [ ] Tag release and verify version-named APK attachments (`dettle-release-1.8.0.apk`, `dettle-debug-1.8.0.apk`).
