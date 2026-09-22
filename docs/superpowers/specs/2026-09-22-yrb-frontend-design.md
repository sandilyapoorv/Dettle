# Dettle Frontend Redesign Specification (YRB Editorial Aesthetic)

## 1. Executive Summary
Transform the Dettle frontend from its current raw dark neon UI with emoji-based iconography into a refined, organic-modern editorial UI inspired by [yrb](https://github.com/shivitatiwari/yrb). The new design features warm paper/charcoal tones, generous pebble curves, subtle tinted surfaces, proper Material 3 vector iconography, zero emojis, and full support for both Light and Dark modes.

Following implementation, the project will be compiled locally, pushed to GitHub, built into an APK via GitHub Actions, and provided with a direct download link.

## 2. Global Design System Tokens

### 2.1 Color Palette
#### Light Palette
- `primary`: `#1B1B18` (deep warm charcoal ink)
- `onPrimary`: `#FFFFFF`
- `primaryContainer`: `#E8E5DE`
- `onPrimaryContainer`: `#1B1B18`
- `secondary`: `#67635C`
- `onSecondary`: `#FFFFFF`
- `background`: `#F6F3ED` (warm textured paper)
- `onBackground`: `#1B1B18`
- `surface`: `#FFFCF7` (bright warm ivory card)
- `onSurface`: `#1B1B18`
- `surfaceVariant`: `#EDE9E1`
- `onSurfaceVariant`: `#625F58`
- `outline`: `#817D75`
- `outlineVariant`: `#D5D0C6`
- `error`: `#B3261E`
- `onError`: `#FFFFFF`

#### Dark Palette
- `primary`: `#F2EFE8` (soft warm cream)
- `onPrimary`: `#22221F`
- `primaryContainer`: `#34332F`
- `onPrimaryContainer`: `#F2EFE8`
- `secondary`: `#C9C4BA`
- `onSecondary`: `#302F2B`
- `background`: `#11110F` (deep OLED warm charcoal)
- `onBackground`: `#F0EDE6`
- `surface`: `#191917` (warm elevated dark surface)
- `onSurface`: `#F0EDE6`
- `surfaceVariant`: `#282723`
- `onSurfaceVariant`: `#C9C4BA`
- `outline`: `#928D84`
- `outlineVariant`: `#403E38`
- `error`: `#FFB4AB`
- `onError`: `#690005`

### 2.2 Corner Shapes
- `extraSmall`: `RoundedCornerShape(10.dp)`
- `small`: `RoundedCornerShape(14.dp)`
- `medium`: `RoundedCornerShape(20.dp)`
- `large`: `RoundedCornerShape(28.dp)`
- `extraLarge`: `RoundedCornerShape(36.dp)`

### 2.3 Window Insets & System Bars
- Enable edge-to-edge with transparent status bar and transparent navigation bar.
- `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`
- `WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme`

## 3. Iconography & Elimination of Emojis
All emoji characters are strictly prohibited across the entire UI. Semantic vector icons from `androidx.compose.material.icons.Icons` (`Outlined` and `Default`) replace all previous emoji usages:
- Quick suggestions:
  - Repos: `Icons.Outlined.FolderCopy`
  - Bugs / Fixes: `Icons.Outlined.BugReport`
  - Deployments: `Icons.Outlined.CloudUpload`
  - Reviews: `Icons.Outlined.RateReview`
- Approvals / Status:
  - Approved / Done: `Icons.Outlined.CheckCircle`
  - Rejected / Error: `Icons.Outlined.Cancel` / `Icons.Outlined.ErrorOutline`
  - Lock / Session: `Icons.Outlined.Lock`
  - Key / Auth: `Icons.Outlined.Key` / `Icons.Outlined.VpnKey`
- Modes / Agents:
  - General / Orchestrator: `Icons.Outlined.Psychology`
  - Architecture: `Icons.Outlined.AccountTree`
  - Code / Debugger: `Icons.Outlined.Terminal`
  - Deploy: `Icons.Outlined.RocketLaunch`
  - Overnight: `Icons.Outlined.Bedtime`
  - Settings: `Icons.Outlined.Tune` / `Icons.Outlined.Settings`

## 4. UI Architecture & Screen Redesigns

### 4.1 Global Navigation (`DettleNavGraph.kt`)
- `Scaffold` containerColor = `MaterialTheme.colorScheme.background`.
- Bottom `NavigationBar` with `containerColor = MaterialTheme.colorScheme.surface`, subtle tonal indicators, and clean typography.
- Navigation items: Chat, Repos, Deployments, Vault, Overnight, Settings.

### 4.2 Chat Screen (`ChatScreen.kt` & `ModeUI.kt`)
- TopBar: `CenterAlignedTopAppBar` with title "Dettle", running agent status badge (tonal pill with `CircularProgressIndicator`), and clear chat action.
- Mode Selector: Clean `FilterChip` / `Surface` row with vector icons and lock status badge.
- Welcome Card: Editorial card matching YRB hero ("Agentic pair programming on your device..."), with clean suggested action pills using vector icons.
- Chat Bubbles:
  - User: Asymmetric rounded shape (`RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)`), subtle tonal container.
  - Assistant: Clean elevated surface with markdown rendering.
  - Tool calls: Collapsible `ElevatedCard` with monospace font, code highlights, and status indicator.
  - Action cards: Approval card with `FilledTonalButton` (Accept) and `OutlinedButton` (Reject).
- Chat Input Bar: Outlined pill (`RoundedCornerShape(28.dp)`) with `FilledIconButton` send button.

### 4.3 Repositories Screen (`ReposScreen.kt`)
- Editorial header ("Repositories") with subtitle.
- Outlined search bar with `Icons.Outlined.Link` leading icon.
- Clean file tree with `Icons.Outlined.Folder` and `Icons.Outlined.Description`.

### 4.4 Deployments Screen (`DeploymentsScreen.kt`)
- Token status banner using tinted `Surface` with `primaryContainer` and check icon.
- Deployment cards with clean typography, status badge, and clickable live URL.

### 4.5 Auth Vault Screen (`AuthVaultScreen.kt`)
- Grid/list of provider cards with connection status chips ("Connected" / "Needs Login").
- Clean fullscreen WebView dialog with close icon.

### 4.6 Overnight Screen (`OvernightScreen.kt`)
- Status banner with live pulse indicator and task counts.
- Start / Stop buttons with `FilledTonalButton` / `Button`.
- Task cards with vector icons and priority chips.
- Embedded live console / log viewer.

### 4.7 Settings Screen (`SettingsScreen.kt`)
- Clean grouped sections: Appearance (System / Light / Dark), API Keys (encrypted storage), Model Fallbacks.

## 5. CI/CD & GitHub Actions APK Build
- Update `.github/workflows/android-build.yml`:
  - Run `./gradlew assembleDebug --no-daemon`.
  - Stage the generated APK as `dettle-debug.apk`.
  - Upload artifact via `actions/upload-artifact@v4`.
  - Provide instructions and direct link to download the APK.
