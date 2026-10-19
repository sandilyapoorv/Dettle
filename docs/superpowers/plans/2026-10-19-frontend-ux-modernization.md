# Frontend & UX Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Dettle's frontend and UX into a world-class, tactile mobile developer environment (matching the polish of Linear, Claude, and Cursor) with rich Markdown code rendering, collapsible diffs/tools, frosted glass navigation without visual collisions, and a categorized settings hub.

**Architecture:** 
- Extract monolithic UI components (`ChatScreen.kt` 2,432 lines, `SettingsScreen.kt` 2,151 lines) into focused, reusable presentation units.
- Implement an AST/Regex-driven Markdown and syntax-highlighted code block engine with copy/diff viewer.
- Redesign the app shell with a frosted liquid glass app bar (preventing scroll-behind collisions) and categorize Settings into a tabbed interface.
- Add tactile micro-interactions (haptic feedback, spring physics, steppers).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX Navigation, Hilt, Coroutines, StateFlow, Markwon/Compose-native annotated string parser.

**Spec:** Frontend & UX master audit of Dettle (October 2026).

## Global Constraints
- Target SDK 35, Min SDK 26, Kotlin 2.x, Jetpack Compose Material 3.
- All write modifications must stay strictly inside `app/src/main/java/com/dettle/app/ui/` and `app/src/test/`. Do NOT touch backend/brain files (`LocalWorkspaceManager.kt`, `FileStateTracker.kt`, etc.).
- Maintain 100% backward compatibility for all `ChatViewModel` and `SettingsViewModel` public StateFlows and events.
- Minimum touch target for all interactive elements must be 44dp x 44dp (or 48dp).
- Every task must pass `./gradlew testDebugUnitTest` and compile cleanly.

## Review Focus
1. **Streaming message flicker**: Live token updates during SSE streaming must not re-parse the entire markdown AST from scratch every character; must throttle/memoize rendering.
2. **Soft keyboard insets (`imePadding`)**: Input bar and mode popovers must not jump off-screen or obscure the active input when keyboard opens.
3. **Accessibility font scaling (1.0x to 1.5x)**: Hardcoded `dp` text containers must never truncate critical labels; use flexible constraints and `sp` text sizes.
4. **Theme persistence round-trip**: Selecting any of the 9 themes in Settings must survive app process restart without reverting to OBSIDIAN.
5. **Memory leaks in code highlighting / Markdown**: Ensure views with large tool outputs (10k+ characters) do not cause Compose recomposition lag or frame drops.

---

### Task 1: Fix Theme Engine & Add Monospace Developer Typography

**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/theme/ThemeManager.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/theme/Typography.kt`
- Create: `app/src/test/java/com/dettle/app/ui/theme/ThemeManagerTest.kt`

**Interfaces:**
- Consumes: `AppTheme` enum and DataStore preferences.
- Produces: Correct theme restoration for all 9 themes via `AppTheme.fromId(id)`; provides `CodeTypography` and monospace font families.

- [ ] **Step 1: Write the failing unit test for ThemeManager**

```kotlin
package com.dettle.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeManagerTest {
    @Test
    fun testFromId_resolvesAllKnownThemes() {
        assertEquals(AppTheme.TITANIUM, AppTheme.fromId("titanium"))
        assertEquals(AppTheme.CYBER_INDIGO, AppTheme.fromId("cyber_indigo"))
        assertEquals(AppTheme.EMERALD_MATRIX, AppTheme.fromId("emerald_matrix"))
        assertEquals(AppTheme.SUNSET_AMBER, AppTheme.fromId("sunset_amber"))
        assertEquals(AppTheme.TOKYO_NEON, AppTheme.fromId("tokyo_neon"))
        assertEquals(AppTheme.OAT_LIGHT, AppTheme.fromId("oat_light"))
        assertEquals(AppTheme.OLED_BLACK, AppTheme.fromId("oled_black"))
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromId("obsidian"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.dettle.app.ui.theme.ThemeManagerTest`
Expected: FAIL on `testFromId_resolvesAllKnownThemes` (returns OBSIDIAN for titanium).

- [ ] **Step 3: Fix AppTheme.fromId and integrate monospace typography**

In `app/src/main/java/com/dettle/app/ui/theme/ThemeManager.kt`:
Update `fromId(id: String?)` to search all entries:
```kotlin
companion object {
    fun fromId(id: String?): AppTheme {
        if (id.isNullOrBlank()) return OBSIDIAN
        return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN
    }
}
```

In `app/src/main/java/com/dettle/app/ui/theme/Typography.kt`:
Add explicit developer code typography:
```kotlin
val CodeMonospace = FontFamily.Monospace

val CodeBlockStyle = TextStyle(
    fontFamily = CodeMonospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.dettle.app.ui.theme.ThemeManagerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/theme/ app/src/test/java/com/dettle/app/ui/theme/
git commit -m "fix(theme): support all 9 themes in fromId and add developer code typography"
```

---

### Task 2: Markdown & Syntax-Highlighted Code Block Composable

**Files:**
- Create: `app/src/main/java/com/dettle/app/ui/components/MarkdownText.kt`
- Create: `app/src/main/java/com/dettle/app/ui/components/CodeBlockCard.kt`
- Create: `app/src/test/java/com/dettle/app/ui/components/MarkdownParserTest.kt`

**Interfaces:**
- Consumes: Raw markdown text string, streaming state, color tokens.
- Produces: `@Composable fun MarkdownText(...)` with formatted inline code, headers, lists, and interactive `@Composable fun CodeBlockCard(...)` with language badge and one-tap copy button.

- [ ] **Step 1: Write unit tests for Markdown segment parser**

```kotlin
package com.dettle.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun testParseFencedCodeBlocks() {
        val input = "Here is some code:\n```kotlin\nval x = 42\n```\nAnd more text."
        val blocks = MarkdownParser.parseBlocks(input)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Text)
        assertTrue(blocks[1] is MarkdownBlock.Code)
        assertEquals("kotlin", (blocks[1] as MarkdownBlock.Code).language)
        assertEquals("val x = 42", (blocks[1] as MarkdownBlock.Code).code.trim())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.dettle.app.ui.components.MarkdownParserTest`
Expected: FAIL (class not found).

- [ ] **Step 3: Implement MarkdownBlock, MarkdownParser, CodeBlockCard, and MarkdownText**

Create `app/src/main/java/com/dettle/app/ui/components/MarkdownText.kt` and `CodeBlockCard.kt`:
- Support fenced code blocks (` ```lang ... ``` `).
- Render `CodeBlockCard`:
  - Header bar with language icon/name ("KOTLIN", "PYTHON", "JSON", "BASH", etc.).
  - Copy to clipboard button with animated checkmark feedback.
  - Dark terminal background (`#0D0D11`), 0.5dp subtle outline border.
  - Monospace font (`CodeMonospace`), horizontal scroll for long lines (no wrapping).
- Render formatted inline spans (`**bold**`, `` `code` ``, `*italic*`, bullet items).

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.dettle.app.ui.components.MarkdownParserTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/components/ app/src/test/java/com/dettle/app/ui/components/
git commit -m "feat(ui): add MarkdownText parser and CodeBlockCard with copy button"
```

---

### Task 3: Terminal-Style Tool Execution & Git Diff Cards

**Files:**
- Create: `app/src/main/java/com/dettle/app/ui/components/TerminalToolCard.kt`
- Create: `app/src/main/java/com/dettle/app/ui/components/GitDiffCard.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt`

**Interfaces:**
- Consumes: `ChatMessage`, `ToolCall`, `ToolResult`, and execution latency.
- Produces: Collapsible terminal widget replacing legacy `ToolCallCard` and `ToolResultCard`. Shows file paths, stdout output, status badge (Running / Succeeded / Failed), and git diff syntax highlighting.

- [ ] **Step 1: Implement GitDiffCard and TerminalToolCard**

In `app/src/main/java/com/dettle/app/ui/components/GitDiffCard.kt`:
- Parse unified diff lines (`+` green background, `-` red background, `@@` cyan header).
- Monospace font, line numbers, and file path header.

In `app/src/main/java/com/dettle/app/ui/components/TerminalToolCard.kt`:
- Terminal header with tool icon (Terminal, Git, Cloud, Search), tool name, and duration badge.
- Expandable argument payload with JSON formatting.
- Foldable execution output log with max height (200dp) and "Show Full Output" toggle.

- [ ] **Step 2: Replace legacy ToolCallCard & ToolResultCard in ChatScreen.kt**

Wire `TerminalToolCard` into `MessageItem` in `ChatScreen.kt`.

- [ ] **Step 3: Verify build passes cleanly**

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/components/TerminalToolCard.kt app/src/main/java/com/dettle/app/ui/components/GitDiffCard.kt app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt
git commit -m "feat(ui): implement modern TerminalToolCard and GitDiffCard"
```

---

### Task 4: Frosted Glass App Bar & Top Navigation De-clutter

**Files:**
- Create: `app/src/main/java/com/dettle/app/ui/components/FrostedChatTopBar.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/navigation/DettleNavGraph.kt`

**Interfaces:**
- Consumes: `activeConversationId`, `environmentMode`, `profile`, `activeProject`, navigation lambdas.
- Produces: A unified, single 56dp frosted glass header with blur scrim that blocks messages from bleeding behind top buttons.

- [ ] **Step 1: Build FrostedChatTopBar**

- Solid/Frosted background (`background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))`).
- Specular bottom border (`0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)`).
- Left: Hamburger Menu (44dp target) + Agent Pulse Indicator.
- Center: Sleek Segmented Mode Pill ("Chat" vs "Build").
- Right: Streak/XP Chip + Settings Gear Icon.
- Project chip moved to an integrated subtitle breadcrumb inside the bar rather than a separate floating row.

- [ ] **Step 2: Wire AgentsScreen into DettleNavGraph**

Add `Screen.Agents` route into `DettleNavGraph.kt` and add a direct shortcut from the top bar agent indicator.

- [ ] **Step 3: Update ChatScreen layout**

Replace the 3 floating rows in `ChatScreen.kt` with `FrostedChatTopBar`. Ensure `LazyColumn` contentPadding cleanly clears the top bar without overlapping.

- [ ] **Step 4: Verify build & tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/components/FrostedChatTopBar.kt app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt app/src/main/java/com/dettle/app/ui/navigation/DettleNavGraph.kt
git commit -m "feat(ui): add FrostedChatTopBar and reconnect AgentsScreen to NavGraph"
```

---

### Task 5: Ergonomic Chat Input Bar & Tactile Micro-Interactions

**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt` (extract/modernize `ChatInputBar`)
- Create: `app/src/main/java/com/dettle/app/ui/chat/ChatInputBarView.kt`
- Create: `app/src/main/java/com/dettle/app/ui/components/Haptics.kt`

**Interfaces:**
- Consumes: Input string, voice state, chat modes, attachments.
- Produces: Ergonomic input bar with popover mode selector, stop button when agent is running, and tactile haptic feedback.

- [ ] **Step 1: Create Haptics helper utility**

In `app/src/main/java/com/dettle/app/ui/components/Haptics.kt`:
Provide `View.performTactileTick()`, `View.performTactileConfirm()`, `View.performTactileReject()`.

- [ ] **Step 2: Modernize ChatInputBar**

- Move the 3 mode pills (`Normal`, `Web`, `Swarm`) into a compact pill toggle above the plus button or as a long-press selector, freeing up text field space.
- Add Stop/Cancel button when `isRunning == true`.
- Trigger haptic tick on send, attachment removal, and mode toggle.
- Clean up dead code (`EffortSlideBar`, `AppleModePillToggle`).

- [ ] **Step 3: Verify build passes cleanly**

Run: `./gradlew compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/chat/ChatInputBarView.kt app/src/main/java/com/dettle/app/ui/components/Haptics.kt app/src/main/java/com/dettle/app/ui/chat/ChatScreen.kt
git commit -m "feat(ui): modernize ChatInputBar with haptics, stop button, and ergonomic layout"
```

---

### Task 6: Categorized Settings Hub (De-monolithize SettingsScreen)

**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/dettle/app/ui/settings/SettingsTabs.kt`
- Create: `app/src/main/java/com/dettle/app/ui/settings/tabs/AiProvidersTab.kt`
- Create: `app/src/main/java/com/dettle/app/ui/settings/tabs/CloudServicesTab.kt`
- Create: `app/src/main/java/com/dettle/app/ui/settings/tabs/AppearanceTab.kt`
- Create: `app/src/main/java/com/dettle/app/ui/settings/tabs/BackupsTab.kt`

**Interfaces:**
- Consumes: `SettingsViewModel.state` and events.
- Produces: Clean, categorized tabbed settings interface:
  1. **Providers** (AI keys, models, daily budgets)
  2. **Cloud & Git** (GitHub PAT & scopes, Cloudflare Pages/Workers)
  3. **Appearance** (Themes, OLED mode, Developer mode, Voice engine)
  4. **Backups** (Google Drive, Local JSON export/restore)

- [ ] **Step 1: Create SettingsTabs component**

Implement a horizontal segmented pill tab bar at the top of Settings.

- [ ] **Step 2: Extract tab composables**

Move the 11 sprawling cards out of `SettingsScreen.kt` into the 4 focused tab files.

- [ ] **Step 3: Add test connection ping feedback**

Add an instant verification pill to API Key inputs ("Verified ✓" or "Invalid Key ✕").

- [ ] **Step 4: Verify build & tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/settings/
git commit -m "refactor(settings): split SettingsScreen into categorized tabs with live verification"
```

---

### Task 7: Repos Code Viewer & Step Budget Stepper Polish

**Files:**
- Modify: `app/src/main/java/com/dettle/app/ui/repos/ReposScreen.kt`
- Modify: `app/src/main/java/com/dettle/app/ui/mode/ModeUI.kt`
- Create: `app/src/main/java/com/dettle/app/ui/components/TactileStepperSlider.kt`

**Interfaces:**
- Consumes: File content, lines, step budgets.
- Produces: Professional monospace file viewer with horizontal scrolling, and a tactile (- / +) stepper slider for mode step budgets.

- [ ] **Step 1: Upgrade FileViewerScreen in ReposScreen.kt**

- Add horizontal scrolling `Modifier.horizontalScroll(rememberScrollState())` to preserve indentation.
- Apply `CodeMonospace` font with line numbering gutter.
- Add copy file content button and share intent in top bar actions.

- [ ] **Step 2: Replace 49-step Slider with TactileStepperSlider in ModeUI.kt**

- Implement `TactileStepperSlider`: provides `-` and `+` touch buttons (44dp targets) flanking a smooth slider with haptic feedback on drag.

- [ ] **Step 3: Verify build & tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dettle/app/ui/repos/ReposScreen.kt app/src/main/java/com/dettle/app/ui/mode/ModeUI.kt app/src/main/java/com/dettle/app/ui/components/TactileStepperSlider.kt
git commit -m "feat(ui): add monospace file viewer and tactile stepper slider"
```

---

## Final Verification Gate
1. Run `./gradlew testDebugUnitTest` -> All tests pass.
2. Run `./gradlew assembleDebug` -> Clean build produces installable APK.
3. Verify zero regression in chat streaming, tool execution, and navigation.
