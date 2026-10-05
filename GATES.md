# GATES.md — 24/7 Background Runtime, Chat GitHub Read Access & Build Mode Repos

OWNS:
- app/src/main/java/com/dettle/app/data/github/GitHubClient.kt
- app/src/main/java/com/dettle/app/domain/model/Tool.kt
- app/src/main/java/com/dettle/app/orchestrator/ToolExecutor.kt
- app/src/main/java/com/dettle/app/ui/repos/ReposViewModel.kt
- app/src/main/java/com/dettle/app/ui/repos/ReposScreen.kt
- app/src/main/java/com/dettle/app/orchestrator/execution/AgentExecutionManager.kt
- app/src/main/java/com/dettle/app/service/AgentForegroundService.kt
- app/src/main/java/com/dettle/app/service/BootReceiver.kt
- app/src/main/java/com/dettle/app/ui/chat/ChatViewModel.kt
- app/src/main/java/com/dettle/app/DettleApplication.kt
- app/src/main/AndroidManifest.xml

---

### G1: GitHub Client Exposes getUserRepositories and restArray
CHECK: grep -c "getUserRepositories" app/src/main/java/com/dettle/app/data/github/GitHubClient.kt
EXPECT: [1-9]

### G2: AgentTools READ_ONLY Has Comprehensive GitHub Read Tools
CHECK: grep -c "GITHUB_LIST_REPOS" app/src/main/java/com/dettle/app/domain/model/Tool.kt
EXPECT: [1-9]

### G3: ToolExecutor Dispatches All New GitHub Read Tools
CHECK: grep -c "github_list_repos" app/src/main/java/com/dettle/app/orchestrator/ToolExecutor.kt
EXPECT: [1-9]

### G4: ReposViewModel Supports Connected Account Repo Auto-Fetch
CHECK: grep -c "getUserRepositories" app/src/main/java/com/dettle/app/ui/repos/ReposViewModel.kt
EXPECT: [1-9]

### G5: AgentExecutionManager Exists and Dispatches Execution
CHECK: test -f app/src/main/java/com/dettle/app/orchestrator/execution/AgentExecutionManager.kt
EXPECT: 0

### G6: BootReceiver Registered in AndroidManifest
CHECK: grep -c "BootReceiver" app/src/main/AndroidManifest.xml
EXPECT: [1-9]

### G7: GitHub Actions CI Build and Tests Pass Cleanly
CHECK: git status --porcelain
EXPECT: ""
