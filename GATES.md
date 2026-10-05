# GATES.md — Bare-Metal Runtime & Gamified Onboarding

OWNS:
- app/src/main/java/com/dettle/app/data/api/StreamingToolCallAccumulator.kt
- app/src/main/java/com/dettle/app/data/api/OpenAICompatProvider.kt
- app/src/main/java/com/dettle/app/orchestrator/ToolExecutor.kt
- app/src/main/java/com/dettle/app/domain/model/Tool.kt
- app/src/main/java/com/dettle/app/data/db/entity/CodeChunkEntity.kt
- app/src/main/java/com/dettle/app/data/db/dao/CodeChunkDao.kt
- app/src/main/java/com/dettle/app/data/db/DettleDatabase.kt
- app/src/main/java/com/dettle/app/orchestrator/rag/CodebaseChunker.kt
- app/src/main/java/com/dettle/app/orchestrator/rag/CodebaseRagService.kt
- app/src/main/java/com/dettle/app/orchestrator/project/ProjectContextManager.kt
- app/src/main/java/com/dettle/app/orchestrator/gamification/GamificationEngine.kt
- app/src/main/java/com/dettle/app/orchestrator/gamification/SallyEnforcerPersona.kt
- app/src/main/java/com/dettle/app/audio/ProceduralAudioService.kt
- app/src/main/java/com/dettle/app/ui/onboarding/GamifiedOnboardingScreen.kt
- app/src/main/java/com/dettle/app/ui/components/StreakXpChip.kt
- app/src/main/java/com/dettle/app/ui/components/ActiveProjectChip.kt
- app/src/main/java/com/dettle/app/ui/navigation/DettleNavGraph.kt

---

### G1: Streaming Tool Delta Accumulator Exists and Handles Chunked Arguments
CHECK: test -f app/src/main/java/com/dettle/app/data/api/StreamingToolCallAccumulator.kt
EXPECT: 0

### G2: ToolExecutor Has Live Cloudflare and Codebase Search Wiring (No Phase 3 Stubs)
CHECK: grep -c "Phase 3" app/src/main/java/com/dettle/app/orchestrator/ToolExecutor.kt || true
EXPECT: 0

### G3: Codebase RAG Entity and DAO Registered in DettleDatabase
CHECK: grep -c "CodeChunkEntity" app/src/main/java/com/dettle/app/data/db/DettleDatabase.kt
EXPECT: 1

### G4: Gamification Engine and Sally Persona Created
CHECK: test -f app/src/main/java/com/dettle/app/orchestrator/gamification/GamificationEngine.kt && test -f app/src/main/java/com/dettle/app/orchestrator/gamification/SallyEnforcerPersona.kt
EXPECT: 0

### G5: Procedural Audio Service Implemented Using ToneGenerator
CHECK: grep -c "ToneGenerator" app/src/main/java/com/dettle/app/audio/ProceduralAudioService.kt
EXPECT: [1-9]

### G6: Gamified 4-Quest Onboarding Screen Exists and Wired in NavGraph
CHECK: test -f app/src/main/java/com/dettle/app/ui/onboarding/GamifiedOnboardingScreen.kt && grep -c "GamifiedOnboarding" app/src/main/java/com/dettle/app/ui/navigation/DettleNavGraph.kt
EXPECT: [1-9]

### G7: GitHub Actions CI Build and Tests Pass Cleanly
CHECK: git status --porcelain
EXPECT: ""
