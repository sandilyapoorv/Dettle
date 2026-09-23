# Komal's Birthday Surprise Web Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a high-quality, tablet-optimized interactive birthday celebration web application for Komal inspired by HeartCraft (`birthday.myheartcraft.com`), featuring a 5-scene unfolding story with balloon popping, candle blowing, love letter, polaroid memory uploader, and mystery gift box.

**Architecture:** A standalone, responsive web application (`komal-birthday-web/`) utilizing vanilla HTML5, CSS3 3D animations, and JavaScript with Web Audio synthesis and Canvas particle effects. Designed with a tablet-first layout (landscape & portrait) that scales seamlessly to phones and desktop.

**Tech Stack:** HTML5, CSS3 (Custom Properties, Keyframe Animations, Glassmorphism), Modern Vanilla JavaScript (ES6+), Web Audio API (ambient music box synthesis + audio player), HTML5 Canvas (particles, confetti, sparklers, smoke), LocalStorage (for live customizer).

**Spec:** [docs/superpowers/specs/2026-09-23-komal-birthday-card-design.md](file:///c:/Users/Ritesh/Billionare/Desktop/Dettle/docs/superpowers/specs/2026-09-23-komal-birthday-card-design.md)

## Global Constraints
- Target Recipient: Komal
- Theme: Cute & Playful Pastel Wonderland (Soft blush pink `#FFF0F5`, pastel strawberry `#FFB6C1`, cream `#FFFDF9`, lilac `#E6E6FA`, mint `#E0F8F1`, golden spark `#FFD700`)
- Viewport / Tab Version: Responsive tablet orientation support (4:3, 16:10, landscape split-view and portrait single-column)
- Standalone: Zero build steps required; works directly when opening `index.html` in any browser or tablet

---

### Task 1: Scaffolding and HTML Skeleton

**Files:**
- Create: `komal-birthday-web/index.html`
- Create: `komal-birthday-web/css/style.css`

**Interfaces:**
- Produces: Base DOM structure containing all 5 scenes, header/music controls, personalization drawer modal, and base styling variables.

- [ ] **Step 1: Create directory structure and `index.html`**
  Set up the HTML5 markup with meta viewport, Google Fonts (`Comfortaa`, `Caveat`, `Outfit`), containers for Scene 1 (Surprise Gate), Scene 2 (Balloons), Scene 3 (Cake & Candles), Scene 4 (Letter & Polaroids), Scene 5 (Gift Finale), and audio toggle button.
- [ ] **Step 2: Create base CSS design system in `style.css`**
  Implement the pastel color palette, typography scales, glassmorphism cards, responsive tablet containers, and navigation dots.
- [ ] **Step 3: Verify basic structure rendered**
  Verify file syntax and proper element hierarchy.
- [ ] **Step 4: Commit**
  `git add komal-birthday-web/index.html komal-birthday-web/css/style.css; git commit -m "feat: scaffold HTML and base CSS for Komal's birthday surprise"`

---

### Task 2: Background Atmosphere & Ambient Audio Engine

**Files:**
- Create: `komal-birthday-web/js/audio.js`
- Modify: `komal-birthday-web/css/style.css`

**Interfaces:**
- Produces: `AudioEngine.init()`, `AudioEngine.playBgm()`, `AudioEngine.stopBgm()`, `AudioEngine.playPopSound()`, `AudioEngine.playChimeSound()`, `AudioEngine.playFanfare()`.

- [ ] **Step 1: Implement Web Audio API music box synthesizer and sound effects**
  Create a gentle, romantic lullaby/music box generator using Web Audio oscillators and envelope filters, plus realistic balloon pop sound synthesis, candle extinguish chime, and brass celebratory fanfare.
- [ ] **Step 2: Add floating ambient sparkle and heart background canvas**
  Implement subtle floating pastel particles that gracefully drift across the screen.
- [ ] **Step 3: Connect music toggle button with autoplay unlock**
  Handle user touch/interaction to unlock AudioContext seamlessly on tablet and mobile.
- [ ] **Step 4: Commit**
  `git add komal-birthday-web/js/audio.js komal-birthday-web/css/style.css; git commit -m "feat: implement ambient audio engine and particle background"`

---

### Task 3: Scene 1 (Surprise Gate) & Scene 2 (Balloon Pop Extravaganza)

**Files:**
- Create: `komal-birthday-web/js/balloons.js`
- Modify: `komal-birthday-web/index.html`
- Modify: `komal-birthday-web/css/style.css`

**Interfaces:**
- Consumes: `AudioEngine.playPopSound()`, `AudioEngine.playFanfare()`.
- Produces: `BalloonsEngine.init()`, `BalloonsEngine.reset()`.

- [ ] **Step 1: Implement 3D bouncing envelope for Scene 1**
  Wax-sealed pastel envelope with heart stamp "To: Komal ✨". Tapping opens the letter flap with 3D CSS rotate, plays gentle chime, and triggers smooth slide into Scene 2.
- [ ] **Step 2: Implement floating pastel balloons for Scene 2**
  Create 6 uniquely styled balloons with realistic string sway and floating keyframes.
- [ ] **Step 3: Implement tap-to-pop with mini-confetti burst & compliments**
  Tapping a balloon plays pop sound, triggers mini-heart particle burst, and reveals one of the 6 sweet compliments. Track popped count; when all 6 pop, trigger fanfare and reveal "Make a Wish, Komal! 🎂 ➔" button.
- [ ] **Step 4: Commit**
  `git add komal-birthday-web/js/balloons.js komal-birthday-web/css/style.css komal-birthday-web/index.html; git commit -m "feat: implement surprise gate and balloon pop extravaganza"`

---

### Task 4: Scene 3 (Birthday Cake & Candle Blowing Ritual)

**Files:**
- Create: `komal-birthday-web/js/cake.js`
- Modify: `komal-birthday-web/index.html`
- Modify: `komal-birthday-web/css/style.css`

**Interfaces:**
- Consumes: `AudioEngine.playFanfare()`, `AudioEngine.playChimeSound()`.
- Produces: `CakeEngine.init()`, `CakeEngine.blowOutCandles()`.

- [ ] **Step 1: Design 2-tier pastel strawberry birthday cake**
  Create rich SVG and CSS tiered cake with frosting drips, strawberries, sprinkles, and a customized cake topper: "Happy Birthday Komal 💕".
- [ ] **Step 2: Create animated flickering candle flames & smoke physics**
  Flickering multi-layered candle flames with soft radial glow. When blown out, flames morph into delicate rising smoke puffs with CSS keyframes and canvas particles.
- [ ] **Step 3: Implement microphone blow detection + touch fallback**
  Listen to tablet mic input volume threshold so she can genuinely blow out her candles. If mic is denied or unavailable, tap or swipe across candles smoothly blows them out.
- [ ] **Step 4: Trigger celebration explosion**
  Upon extinguishing all candles, trigger sparkler fountain canvas animation, screen-wide confetti shower, birthday chime, and reveal "Open Your Letter 💌 ➔" button.
- [ ] **Step 5: Commit**
  `git add komal-birthday-web/js/cake.js komal-birthday-web/css/style.css komal-birthday-web/index.html; git commit -m "feat: implement interactive birthday cake and candle blowing"`

---

### Task 5: Scene 4 (Love Letter & Polaroid Memory Gallery with Photo Uploader)

**Files:**
- Create: `komal-birthday-web/js/memories.js`
- Modify: `komal-birthday-web/index.html`
- Modify: `komal-birthday-web/css/style.css`

**Interfaces:**
- Produces: `MemoriesEngine.init()`, `MemoriesEngine.loadPhotos()`, `MemoriesEngine.saveCustomData()`.

- [ ] **Step 1: Build the heartfelt romantic love letter**
  Stationery paper styled with subtle pastel margin lines, wax seal decor, and heartfelt words celebrating Komal's kindness, beauty, and presence.
- [ ] **Step 2: Build the vintage Polaroid carousel / photo stack**
  4 tiltable polaroid cards with paper clips, handwritten dates/captions, and cute tape graphics. Optimized with tablet-friendly swipe and touch tilt.
- [ ] **Step 3: Build the Live Personalizer / Photo Uploader Drawer**
  A discreet modal / slide-out panel allowing the sender to replace polaroid placeholders with real photos of Komal (via instant drag & drop or file picker) and edit the letter text. Saves automatically to `localStorage`.
- [ ] **Step 4: Commit**
  `git add komal-birthday-web/js/memories.js komal-birthday-web/css/style.css komal-birthday-web/index.html; git commit -m "feat: implement love letter, polaroid gallery, and live photo uploader"`

---

### Task 6: Scene 5 (Mystery Gift Box Finale) & Navigation Orchestration

**Files:**
- Create: `komal-birthday-web/js/gift.js`
- Create: `komal-birthday-web/js/app.js`
- Modify: `komal-birthday-web/index.html`
- Modify: `komal-birthday-web/css/style.css`

**Interfaces:**
- Consumes: All scene modules.
- Produces: Full end-to-end interactive flow, scene router, and tablet orientation adaptors.

- [ ] **Step 1: Implement 3D wrapped gift box**
  Pastel box with satin ribbon. Tapping unties the ribbon, pops the lid off in 3D, and explodes golden sparkles and confetti.
- [ ] **Step 2: Reveal the grand birthday finale card**
  A glowing heart-shaped keepsake card pops out of the box with the climax love message for Komal.
- [ ] **Step 3: Connect master scene navigator**
  Implement smooth slide/fade transitions between scenes (with back/next buttons and progress indicator dots).
- [ ] **Step 4: Commit**
  `git add komal-birthday-web/js/gift.js komal-birthday-web/js/app.js komal-birthday-web/css/style.css; git commit -m "feat: implement 3D gift box finale and scene orchestration"`

---

### Task 7: Tablet Optimization, Polish, and Verification

**Files:**
- Modify: `komal-birthday-web/css/style.css`
- Modify: `komal-birthday-web/index.html`
- Create: `komal-birthday-web/run_preview.bat` (Quick launcher script for Windows / Chrome)

**Interfaces:**
- Produces: Polished tablet UX, smooth 60fps animations, instant one-click launch capability.

- [ ] **Step 1: Fine-tune Tablet Layouts (Landscape & Portrait)**
  Test and polish split-screen landscape layout (e.g. Letter on left, Polaroids on right) and stacked portrait layout with media queries.
- [ ] **Step 2: Create simple local preview server / launcher**
  Provide a simple batch script or local HTTP server launcher so it can be previewed or opened in a browser tab immediately.
- [ ] **Step 3: Full end-to-end manual verification**
  Verify sound effects, balloon pop, candle blow, polaroid drag, photo upload, gift box unwrap, and scene navigation.
- [ ] **Step 4: Commit**
  `git add komal-birthday-web/; git commit -m "polish: tablet responsiveness, touch optimizations, and preview launcher"`
