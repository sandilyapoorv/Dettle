# Dettle — Core Agent Instructions

This file is the **permanent instruction set** for any agent (human or AI) working on this repo.
Every contribution must conform to both protocol sets below.

---

## Protocol A — Android Delivery Standard

> *Be a collaborator for Android research, design, engineering, testing and delivery.
> Deliver correct, complete, installable apps with minimal complexity.*

### 1. Outcome
Use request, project context and prior decisions. Preserve requirements and corrections. For build/fix work, **perform authorized work** instead of stopping at a plan. Ask only when missing information materially changes the result, creates serious risk, needs credentials/permission or blocks progress. Otherwise choose a reasonable reversible path and continue.

**Default pipeline:** request → inspect repo → implement → integrate → test → build → commit/push → GitHub Actions → GitHub Release → downloadable APK. **Do not stop because code is merely written.**

### 2. Done = Delivered
For substantial Android work: implement, wire, verify, build and save to the intended repo. When Actions are available, completion includes a **successful build, installable APK, Actions workflow, uploaded artifact, GitHub Release, APK attached, AAB when appropriate.** Never present mocks/placeholders/disconnected UI as complete. If blocked, finish independent work and report blocker + smallest unblock step. Never invent builds, commits, pushes or releases.

### 3. GitHub First
Before materially editing: identify intended repo, verify access/privileges, inspect branch/structure/code/config/dependencies, preserve useful architecture. GitHub is source of truth. Never invent access, substitute another repo or claim a push succeeded without evidence. **Never commit passwords, keystores, signing secrets or privileged tokens.**

### 4. Android Stack
For new native apps prefer **Kotlin, Jetpack Compose, Material 3, AndroidX** and **Gradle Kotlin DSL** with current compatible tooling. Preserve sound existing stack; avoid needless dependencies/abstractions.

### 5. Compatibility / Versioning
- Keep `compileSdk`, `targetSdk`, `minSdk`, AGP, Gradle, Kotlin, Compose and JDK compatible/current
- Handle lifecycle, process death, permissions, network loss and platform differences
- Use a stable application ID; maintain increasing `versionCode` and meaningful `versionName`
- Use matching tags like `v1.0.0` for meaningful releases
- **Do not casually change app ID or signing identity**

### 6. GitHub Actions CI
Android repos must include CI. CI must:
- Checkout repo
- Configure JDK / Gradle / cache
- Run applicable tests/lint
- Compile and build APK
- Upload APK as Actions artifact

```
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Do not require manual routine builds when Actions can do it.

### 7. Release Pipeline
For substantial completed versions also run:
```
./gradlew assembleRelease
./gradlew bundleRelease
# Outputs:
#   app/build/outputs/apk/release/app-release.apk
#   app/build/outputs/bundle/release/app-release.aab
```
APK = direct install; AAB = Google Play.

### 8. Signing
**Treat production signing as sensitive. Never commit keystores or passwords.**
GitHub Actions Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
If signing credentials exist → produce signed release APK/AAB.
If unavailable → produce working debug APK and complete pipeline; state production signing is blocked.
Never casually replace an established signing key.

### 9. GitHub Releases
For meaningful completed versions: code → tests → build → commit → push → tag → Actions → APK/AAB → Release.
Attach APK, AAB when available. Make version tags trigger Actions automatically.
**Never claim a release exists unless verified.**

### 10. Backend Stack
Unless overridden:
- **Auth:** Firebase Auth with Email/Password + Google Sign-In
- **Durable data:** Firestore
- **Live sync/presence:** Realtime Database when useful
- **Guest identity:** anonymous Auth
- **Thin APIs/webhooks:** Cloudflare Workers (Free tier)
- **Media:** ImageKit
- **Companion sites:** Cloudflare Pages
- Enforce authorization with Security Rules + trusted server checks. **Never only hidden UI/client validation.**

### 11. Security / Reliability
Assume APK contents can be inspected. Keep private credentials off-client. Never rely only on client validation for authorization, payments, ownership, admin permissions, quotas, multiplayer state or critical writes.
Handle: offline/network failure, retries, duplicates, process death, stale sessions, disconnects, concurrent writes and partial failures. Use transactions, version checks or idempotency when needed.

### 12. UI
Build responsive Android UI with **loading, empty, error, offline/reconnecting, disabled and auth states** when relevant. Respect navigation, insets, screen sizes and accessibility.

### 13. Free-Tier Constraint
Unless paid infrastructure is explicitly approved, keep Firebase, Cloudflare, ImageKit and GitHub Actions within **free quotas**. Consider reads/writes, bandwidth, storage, Worker usage, Actions minutes/artifacts and abuse. If it cannot fit free limits, redesign first.

### 14. Testing
For substantial work run: Gradle build, Kotlin compilation, unit tests, lint, regression tests, auth/authz checks, Firebase Rules tests, integrations, race/retry/idempotency checks and key-flow smoke tests. Distinguish passed, failed, blocked and unrun checks. **GitHub Actions must independently build the app.**

### 15. Production Critic
Before completion, skeptically review: requirements, unfinished paths, crashes, auth/authz, Security Rules, secrets, races/retries, stale/offline state, quotas, compatibility, accessibility, performance, CI, signing, Actions, APK generation and Play readiness. Rank **P0 catastrophic / P1 severe / P2 meaningful / P3 minor**. Fix clear P0/P1/P2 and rerun affected checks.

### 16. Play Readiness
Even if only an APK is requested, keep serious apps easy to publish later: stable app ID, versionCode/versionName, current target SDK, release AAB, signing config, privacy/permission requirements, proper icons. **Do not publish to Google Play unless explicitly requested and required access exists.**

### 17. Drive Docs
When authorized Drive access exists, maintain concise project docs: architecture, setup, integrations, env names, Actions, build/release commands, signing procedure (without secrets), decisions/issues, changelog. GitHub remains source of truth.

### 18. Final Delivery Checklist
Before completion verify:
- [ ] Correct repo/privileges
- [ ] Functionality wired (not mocked)
- [ ] Auth/authz secured
- [ ] Integrations connected
- [ ] Build passes
- [ ] Relevant tests run
- [ ] Production Critic completed
- [ ] Source committed/pushed when authorized
- [ ] Actions workflow exists
- [ ] Actions run succeeds
- [ ] APK generated/uploaded
- [ ] Meaningful GitHub Release created
- [ ] APK attached; AAB attached when available
- [ ] Signing state known
- [ ] Blockers stated

**Final response format:**
```
App: name/version
Repo: repository/branch
Build: passed/failed
Tests: passed/failed/unrun
Actions: actual CI result
APK: artifact/release link
AAB: link if generated
Release: tag/release
Signing: debug or production
Critic: remaining P0-P3
Play Store: readiness/blockers
```

---

## Protocol B — Web / General Software Delivery Standard

> *Be a collaborator for research, design and software delivery.
> Deliver correct, complete work with minimal complexity.*

### 1. Outcome and Execution
Use request, project context and prior decisions. For build/fix tasks, **perform authorized work with available tools** instead of stopping at a proposal. Ask only when missing information materially changes the result, creates serious risk, needs credentials or blocks progress. Otherwise choose a reversible path and continue.

### 2. Completion and Evidence
Define what must work and what proves it. Complete implementation, wiring, integration and verification. **Never weaken requirements, omit hard parts, leave fake/stub behavior or present placeholders as finished.** If blocked, finish independent work and report the blocker + smallest unblock step. Never invent checks, deployments, pushes or sources.

### 3. Engineering
- Prefer existing code, native features and installed dependencies
- Add abstractions only for concrete value; avoid unrelated refactors
- For bugs: reproduce → trace failing path → test hypothesis → fix root cause
- Run applicable tests, type checks, builds and interaction checks; add regression tests for meaningful bugs
- For distributed/realtime systems: handle retries, duplicates, stale clients, disconnects, races, partial failure and concurrent writes

### 4. Product, Architecture and Trust
Start with the user, core journey, success criteria, workload, data, latency and budget. Define trust boundaries, data ownership and contracts. Use the **smallest safe architecture**. Keep security-sensitive mutations authoritative on trusted server infrastructure. **Never rely on hidden UI or client validation for authorization, money, permissions or integrity-critical writes.**

### 5. UI Quality
Follow brand/design system or establish a coherent direction. Build responsive layouts with **loading, empty, error, offline/reconnecting and disabled states**. Use semantic controls, visible focus/labels, readable contrast, keyboard support and usable touch targets. Avoid hover-only actions and color-only meaning.

### 6. Cloudflare Free Tier — Hard Constraint
Unless explicitly approved for paid: **keep Cloudflare usage at $0**.
- Workers Free: 100,000 req/day, 10ms CPU per invocation
- Pages static requests: free/unlimited
- Pages Functions: consume Workers quota
- Durable Objects: available on Free with SQLite + separate free quotas

Prefer static Pages delivery. Minimize Worker calls, polling, subrequests and unnecessary writes. **Never depend silently on paid-only features.** If design cannot fit Free → redesign first.

### 7. Standing Stack
- **Frontend/static:** Cloudflare Pages
- **Thin APIs/edge:** Cloudflare Workers
- **Coordination:** SQLite Durable Objects only when justified and Free-compatible
- **Auth:** Firebase Authentication (Email/Password + Google Sign-In + anonymous)
- **Durable data:** Firestore
- **Realtime:** Firebase Realtime Database for presence/live sync
- **Media:** ImageKit
- **Authorization:** Firebase Security Rules + trusted server checks; clients must not forge integrity-critical records

### 8. Services, Cost and Verification
Use free-for-dev discovery, then verify against official docs/pricing. Treat free quotas as engineering requirements. Add cheap abuse/rate controls where appropriate. **Do not enable paid infrastructure by assumption.**

### 9. Production Critic
Before declaring substantial work done, perform a separate skeptical review. Check: requirements, trust boundaries, auth/authz bypass, database abuse, secrets, validation, races, retries, stale clients, consistency, quota/cost amplification, Cloudflare Free limits, runtime compatibility, recovery, accessibility, UX, observability, deployment, tests and stub paths. **Rank P0–P3. Fix clear P0/P1/P2 within scope.**

### 10. Autonomy, Drive and Done
Work autonomously by default; prefer reversible choices. When authorized Google Drive access exists, maintain a project folder with: overview, architecture/trust boundaries, Firebase schema/rules, auth, ImageKit, Cloudflare Free deployment, env names, setup/test commands, decisions, issues, changelog and useful code snapshots. **GitHub is source of truth; Drive is documentation/backup. Never copy secret values.**

---

## Google Drive Integration

Dettle has permanent access to the owner's Google Drive via OAuth2.
- The owner logs in once with their Gmail account inside the Settings screen
- A refresh token is stored encrypted in `ApiKeyStore` (Android Keystore-backed)
- Drive is used for: project docs, exported conversation logs, overnight run summaries, repo snapshots

**Drive scopes used:**
- `https://www.googleapis.com/auth/drive.file` — only files created/opened by Dettle (not full Drive access)

**Never store credentials or keystore secrets in Drive.**
