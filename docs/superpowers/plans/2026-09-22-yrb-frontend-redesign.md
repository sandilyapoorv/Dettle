# YRB Frontend Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the entire Dettle Android frontend to match the warm, organic-modern editorial UI of YRB (warm paper/charcoal palette, soft rounded radii, subtle tinted surfaces, proper Material 3 vector icons, zero emojis), build the APK with GitHub Actions, and provide the direct download link.

**Architecture:** Adopt the YRB theme system supporting both Light and Dark modes with transparent window insets. Replace all hardcoded raw neon colors and Unicode emojis across the UI with semantic Material 3 tokens, elevated cards, and vector icons. Enhance the GitHub Actions workflow to build and upload the debug APK.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Material Icons Extended, AndroidX Navigation, Hilt DI, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-22-yrb-frontend-design.md`

## Global Constraints
- Strictly zero emojis in the entire UI (replace all emojis with proper Material 3 vector icons).
- Match the exact YRB warm editorial color scheme in both Light and Dark modes.
- Use YRB rounded corner radii (10.dp, 14.dp, 20.dp, 28.dp, 36.dp).
- Retain all existing business logic, ViewModels, and reactive state flows.
- Ensure `./gradlew assembleDebug` compiles without errors.

---

### Task 1: Theme & Design System Tokens (`Color.kt`, `Theme.kt`, `Typography.kt`)
**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/theme/Typography.kt`

- [ ] **Step 1:** Update `Color.kt` to define YRB warm light & dark palette colors, and semantic container aliases.
- [ ] **Step 2:** Update `Theme.kt` to define `LightColors` and `DarkColors` matching YRB, configure `isAppearanceLightStatusBars`, and define shapes (10.dp, 14.dp, 20.dp, 28.dp, 36.dp).
- [ ] **Step 3:** Update `Typography.kt` to support clean headline, title, and body hierarchy.
- [ ] **Step 4:** Run `./gradlew compileDebugKotlin` to verify tokens compile cleanly.

---

### Task 2: Global Navigation & TopBar (`DettleNavGraph.kt`)
**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/navigation/DettleNavGraph.kt`

- [ ] **Step 1:** Replace navigation icons with clean outlined vector icons (`Icons.Outlined.ChatBubbleOutline`, `Icons.Outlined.FolderCopy`, `Icons.Outlined.CloudUpload`, `Icons.Outlined.VpnKey`, `Icons.Outlined.Bedtime`, `Icons.Outlined.Tune`).
- [ ] **Step 2:** Style `NavigationBar` to use `MaterialTheme.colorScheme.surface` with subtle tonal indicators.
- [ ] **Step 3:** Run `./gradlew compileDebugKotlin` to verify navigation builds cleanly.

---

### Task 3: Chat Screen & Mode UI Redesign (`ChatScreen.kt`, `ModeUI.kt`)
**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/mode/ModeUI.kt`

- [ ] **Step 1:** Redesign `ChatTopBar` with `CenterAlignedTopAppBar`, running status badge, and clear action.
- [ ] **Step 2:** Redesign `WelcomeCard` in YRB hero style with clean suggested action pills using vector icons (no emojis).
- [ ] **Step 3:** Redesign message bubbles (user bubble with asymmetric rounded corner, assistant bubble with elevated card).
- [ ] **Step 4:** Redesign tool call cards and action approval cards with vector icons and tonal buttons.
- [ ] **Step 5:** Redesign `ChatInputBar` with rounded outline and send button.
- [ ] **Step 6:** Purge all emojis in `ModeUI.kt` and replace with vector icons and clean pills.
- [ ] **Step 7:** Run `./gradlew compileDebugKotlin` to verify.

---

### Task 4: Repositories & Deployments Screen Redesign (`ReposScreen.kt`, `DeploymentsScreen.kt`)
**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/repos/ReposScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/deployments/DeploymentsScreen.kt`

- [ ] **Step 1:** Redesign `ReposScreen.kt` header, search bar, repo cards, and file viewer.
- [ ] **Step 2:** Redesign `DeploymentsScreen.kt` Cloudflare status banner, deployment history cards, and empty states. Eliminate all emojis.
- [ ] **Step 3:** Run `./gradlew compileDebugKotlin` to verify.

---

### Task 5: Auth Vault, Overnight Mode, Agents & Settings Screens
**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/authvault/AuthVaultScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/overnight/OvernightScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/agents/AgentsScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1:** Redesign `AuthVaultScreen.kt` provider cards with status chips ("Connected" / "Needs Login") and vector icons.
- [ ] **Step 2:** Redesign `OvernightScreen.kt` status banner, task queue cards, and real-time log box (zero emojis).
- [ ] **Step 3:** Redesign `AgentsScreen.kt` fleet status and event items with vector icons.
- [ ] **Step 4:** Redesign `SettingsScreen.kt` with theme selector (System / Light / Dark) and clean field cards.
- [ ] **Step 5:** Run `./gradlew compileDebugKotlin` to verify.

---

### Task 6: Local Verification, GitHub Actions APK Workflow, Commit & Push
**Files:**
- Modify: `.github/workflows/android-build.yml`

- [ ] **Step 1:** Ensure `.github/workflows/android-build.yml` builds debug APK (`assembleDebug`) and uploads `dettle-debug.apk` as an artifact.
- [ ] **Step 2:** Run `./gradlew assembleDebug` locally to ensure the APK compiles cleanly.
- [ ] **Step 3:** Git commit all changes and push to `origin main`.
- [ ] **Step 4:** Provide the GitHub Actions workflow run link and direct APK artifact link to the user.
