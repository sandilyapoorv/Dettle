# Design Specification: Komal's Birthday Surprise Experience (Tablet & Web)

**Date**: 2026-09-23  
**Target Recipient**: Komal  
**Theme**: Cute & Playful Pastel Wonderland (Strawberry shortcake, pastel macarons, soft lavender, warm candlelight, bubbly animations)  
**Form Factor**: Optimized for Tablets (iPad, Galaxy Tab, etc.) in both Landscape and Portrait, seamlessly responsive on Mobile & Desktop.

---

## 1. Executive Summary & Concept
An interactive, high-fidelity digital celebration experience inspired by `birthday.myheartcraft.com`. Instead of a static greeting, Komal is guided through a 5-scene cinematic storybook that combines playful interactivity (popping balloons, blowing out candles with flame and smoke physics, unwrapping a 3D gift box) and heartfelt emotion (unfolding love letter, memory polaroids with live upload capability, and romantic background music).

---

## 2. Technical Architecture & Tech Stack

- **Delivery**: Standalone, zero-dependency, ultra-fast web application (`index.html`, `style.css`, `app.js`). Runs locally in any browser or tablet tab instantly without requiring npm builds, and can be shared or hosted on GitHub Pages / Vercel / Netlify in seconds.
- **Audio System**: Web Audio API-synthesized soothing music box / romantic melody + royalty-free sweet audio tracks with interactive Play/Pause and volume control (works on touch and adheres to browser autoplay policies).
- **Physics & Particle Engines**:
  - `canvas-confetti` (embedded/loaded via CDN or local canvas script) for multi-stage confetti cannons, heart showers, and fireworks.
  - Custom Canvas Sparkle & Floating Pastel Bokeh background engine.
- **Microphone / Touch Candle Blowing**:
  - Web Audio microphone input detection (optional mic access: when she blows into the tablet mic, the candles blow out!).
  - Graceful fallback: Tap/Swipe to blow out candle flames with rising smoke puff animation.
- **Dynamic Personalization**:
  - Pre-loaded with sweet, romantic default messages and memory polaroids for Komal.
  - A discreet "Personalize / Add Photos" drawer allowing the sender to upload real photos and customize the letter without touching code. State is safely stored in `localStorage`.

---

## 3. Scene Breakdown & User Flow

### Scene 1: The Surprise Gate ("For My Favorite Person")
- **Visuals**: Soft pastel pink/lavender gradient, floating hearts and sparkles.
- **Hero Element**: A bouncing 3D envelope with a wax seal stamped with a heart and Komal's name.
- **Action**: "Tap to Open Komal's Birthday Surprise ✨".
- **Interaction**: Envelope opens with a cute sound, revealing the journey card. Music starts softly.

### Scene 2: Balloon Pop Extravaganza
- **Visuals**: 6 colorful pastel balloons floating with gentle realistic bobbing physics (strawberry pink, mint green, lemon yellow, soft peach, lilac, sky blue).
- **Interaction**: Tapping each balloon pops it with a burst of mini hearts and reveals a sweet hidden compliment:
  1. *"Your smile lights up my whole world 💖"*
  2. *"The kindest and most beautiful soul 🌸"*
  3. *"My favorite laugh in the entire universe ✨"*
  4. *"Every moment with you is pure magic 🍰"*
  5. *"Today is all about celebrating YOU 🎀"*
  6. *"The best thing that ever happened to me 🌟"*
- **Milestone**: When all balloons are popped, a celebratory fanfare sounds and a button appears: *"Make a Wish, Komal! 🎂 ➔"*

### Scene 3: The Birthday Cake & Candle Ritual
- **Visuals**: A beautifully rendered pastel 2-tier strawberry birthday cake decorated with cream frosting, sprinkles, strawberries, and candles with glowing, flickering SVG/canvas flames.
- **Personalized Cake Topper**: *"Happy Birthday Komal 💕"*
- **Interactive Blowing**:
  - Instruction: *"Make a wish and blow into your mic or tap the candles!"*
  - Extinguishing: Flames flicker violently, blow out into realistic smoke puffs.
  - Climax: Golden sparklers erupt from the cake, a huge burst of pastel confetti covers the screen, and celebratory birthday chimes play!
- **Transition**: Button appears: *"Open Your Letter 💌 ➔"*

### Scene 4: The Love Letter & Memory Polaroids
- **Visuals**: A romantic open stationery letter with subtle lined paper texture and handwritten-style typography.
- **The Letter**:
  - Emotional, tender, romantic message crafted specifically for Komal.
  - Typewriter animation option or smooth scroll with gentle floral accents.
- **Polaroid Memory Gallery**:
  - 4 vintage polaroid cards with paper-clip effects and sweet handwritten dates/captions.
  - Interactive tilt/drag on tablet touch.
  - Live photo upload button: sender can click "Add Her Photos" to replace placeholders with real photos of Komal instantly.
- **Transition**: Button: *"One More Surprise For You 🎁 ➔"*

### Scene 5: The Grand Finale Mystery Gift Box
- **Visuals**: A pastel gift box tied with a satin ribbon.
- **Interaction**: Tap the ribbon to untie it — the box lid flies off in 3D perspective!
- **Finale**: A sparkling card bursts out: *"I love you to the moon and back, Komal. May this year bring you all the happiness your heart can hold!"*
- **Replay / Nav Bar**: Quick navigation bar at the bottom to jump back to any scene anytime.

---

## 4. Tablet Optimization Specifications ("Tab Version")
- **Viewport & Touch Targets**: Sized for comfortable 44px+ touch targets on iPad / Android tablets.
- **Orientation**: Full responsive layout with adaptive two-column display for Tablet Landscape (Letter on left, Polaroids on right) and centered stack for Tablet Portrait.
- **Performance**: 60fps CSS transitions and hardware-accelerated canvas particles (`requestAnimationFrame`), lightweight asset footprint with zero lag.

---

## 5. Verification Plan
- Cross-resolution testing (Tablet landscape 1024x768, 1280x800; Tablet portrait 768x1024; Mobile 375x812; Desktop).
- Interactive touch testing: Balloon pop triggers, candle blow detection & tap fallback, audio controls, photo upload preview, local storage persistence.
