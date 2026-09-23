/**
 * Komal's Birthday Surprise - Audio & Ambient Particle Engine
 * High-fidelity Web Audio API music box, sound effects & canvas sparkles
 */

const AudioEngine = (() => {
  let ctx = null;
  let isPlayingBgm = false;
  let bgmTimer = null;
  let masterGain = null;

  // Initialize Web Audio Context
  function init() {
    if (!ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) {
        ctx = new AudioCtx();
        masterGain = ctx.createGain();
        masterGain.gain.setValueAtTime(0.5, ctx.currentTime);
        masterGain.connect(ctx.destination);
      }
    }
    if (ctx && ctx.state === 'suspended') {
      ctx.resume();
    }
  }

  // Play a single music box / celesta tone
  function playNote(freq, startTime, duration = 1.2, volume = 0.3) {
    if (!ctx) return;
    try {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      // Sine wave with slight harmonics for a music-box chime feel
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, startTime);

      // Music box envelope: instant attack, bell-like decay
      gain.gain.setValueAtTime(0.001, startTime);
      gain.gain.exponentialRampToValueAtTime(volume, startTime + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

      osc.connect(gain);
      gain.connect(masterGain);

      osc.start(startTime);
      osc.stop(startTime + duration);
    } catch (e) {
      console.warn('Audio tone error', e);
    }
  }

  // Romantic Music Box Melody Progression
  // Frequencies in Hz for C5, D5, E5, F5, G5, A5, B5, C6
  const NOTES = {
    C4: 261.63, E4: 329.63, G4: 392.00, A4: 440.00, B4: 493.88,
    C5: 523.25, D5: 587.33, E5: 659.25, F5: 698.46, G5: 783.99,
    A5: 880.00, B5: 987.77, C6: 1046.50, D6: 1174.66, E6: 1318.51
  };

  // Gentle, romantic arpeggio pattern
  const MELODY_SEQUENCE = [
    // Measure 1 (C major)
    { note: NOTES.C5, delay: 0 },
    { note: NOTES.E5, delay: 350 },
    { note: NOTES.G5, delay: 700 },
    { note: NOTES.C6, delay: 1050 },
    { note: NOTES.E6, delay: 1400 },
    { note: NOTES.G5, delay: 1750 },
    
    // Measure 2 (A minor / F major soft transition)
    { note: NOTES.A5, delay: 2200 },
    { note: NOTES.C6, delay: 2550 },
    { note: NOTES.E6, delay: 2900 },
    { note: NOTES.D6, delay: 3300 },
    { note: NOTES.C6, delay: 3700 },

    // Measure 3 (F major warm)
    { note: NOTES.F5, delay: 4200 },
    { note: NOTES.A5, delay: 4550 },
    { note: NOTES.C6, delay: 4900 },
    { note: NOTES.E6, delay: 5250 },
    { note: NOTES.D6, delay: 5650 },

    // Measure 4 (G major resolving to sweet finish)
    { note: NOTES.G5, delay: 6100 },
    { note: NOTES.B5, delay: 6450 },
    { note: NOTES.D6, delay: 6800 },
    { note: NOTES.C6, delay: 7200 },
    { note: NOTES.G5, delay: 7600 }
  ];

  const LOOP_DURATION = 8200;

  function scheduleMelodyLoop() {
    if (!isPlayingBgm || !ctx) return;
    const now = ctx.currentTime;
    MELODY_SEQUENCE.forEach(item => {
      playNote(item.note, now + (item.delay / 1000), 1.5, 0.22);
    });

    bgmTimer = setTimeout(() => {
      scheduleMelodyLoop();
    }, LOOP_DURATION);
  }

  function startBgm() {
    init();
    if (isPlayingBgm) return;
    isPlayingBgm = true;
    scheduleMelodyLoop();
    updateUi();
  }

  function stopBgm() {
    isPlayingBgm = false;
    if (bgmTimer) {
      clearTimeout(bgmTimer);
      bgmTimer = null;
    }
    updateUi();
  }

  function toggleBgm() {
    if (isPlayingBgm) {
      stopBgm();
    } else {
      startBgm();
    }
  }

  function updateUi() {
    const btn = document.getElementById('music-btn');
    if (!btn) return;
    if (isPlayingBgm) {
      btn.classList.add('playing');
      btn.title = 'Pause Romantic Melody';
    } else {
      btn.classList.remove('playing');
      btn.title = 'Play Romantic Melody';
    }
  }

  // --- SOUND EFFECTS ---

  // Balloon Pop Sound
  function playPopSound() {
    init();
    if (!ctx) return;
    try {
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(380, now);
      osc.frequency.exponentialRampToValueAtTime(60, now + 0.08);

      gain.gain.setValueAtTime(0.5, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.08);

      osc.connect(gain);
      gain.connect(masterGain);

      osc.start(now);
      osc.stop(now + 0.09);
    } catch (e) {
      console.warn(e);
    }
  }

  // Sparkle / Chime Sound
  function playChimeSound() {
    init();
    if (!ctx) return;
    try {
      const now = ctx.currentTime;
      [NOTES.E5, NOTES.G5, NOTES.C6, NOTES.E6].forEach((note, idx) => {
        playNote(note, now + (idx * 0.09), 0.8, 0.25);
      });
    } catch (e) {
      console.warn(e);
    }
  }

  // Celebratory Fanfare (Happy Birthday motif snippet)
  function playFanfare() {
    init();
    if (!ctx) return;
    try {
      const now = ctx.currentTime;
      const fanfareNotes = [
        { note: NOTES.G4, delay: 0 },
        { note: NOTES.G4, delay: 180 },
        { note: NOTES.A4, delay: 360 },
        { note: NOTES.G4, delay: 650 },
        { note: NOTES.C5, delay: 950 },
        { note: NOTES.B4, delay: 1300 },
        // Chime finish
        { note: NOTES.C5, delay: 1750 },
        { note: NOTES.E5, delay: 1950 },
        { note: NOTES.G5, delay: 2150 },
        { note: NOTES.C6, delay: 2350 }
      ];

      fanfareNotes.forEach(item => {
        playNote(item.note, now + (item.delay / 1000), 1.2, 0.35);
      });
    } catch (e) {
      console.warn(e);
    }
  }

  return {
    init,
    startBgm,
    stopBgm,
    toggleBgm,
    playPopSound,
    playChimeSound,
    playFanfare,
    isPlaying: () => isPlayingBgm
  };
})();

/* ========================================================
   AMBIENT PARTICLE CANVAS (Floating Sparkles & Hearts)
   ======================================================== */
const AmbientParticles = (() => {
  let canvas, ctx;
  let particles = [];
  const MAX_PARTICLES = 36;

  class Particle {
    constructor(w, h, initial = false) {
      this.reset(w, h, initial);
    }

    reset(w, h, initial = false) {
      this.x = Math.random() * w;
      this.y = initial ? Math.random() * h : h + 20;
      this.size = Math.random() * 6 + 4;
      this.speedY = Math.random() * 0.7 + 0.3;
      this.speedX = (Math.random() - 0.5) * 0.4;
      this.opacity = Math.random() * 0.5 + 0.3;
      this.fadeSpeed = Math.random() * 0.005 + 0.002;
      this.isHeart = Math.random() > 0.5;
      this.hue = Math.random() > 0.5 ? 340 : 45; // Pink or Golden sparkle
      this.rotation = Math.random() * Math.PI * 2;
      this.rotSpeed = (Math.random() - 0.5) * 0.02;
    }

    update(w, h) {
      this.y -= this.speedY;
      this.x += this.speedX;
      this.rotation += this.rotSpeed;

      if (this.y < -30 || this.x < -20 || this.x > w + 20) {
        this.reset(w, h);
      }
    }

    draw(ctx) {
      ctx.save();
      ctx.translate(this.x, this.y);
      ctx.rotate(this.rotation);
      ctx.globalAlpha = this.opacity;

      if (this.isHeart) {
        // Draw delicate heart
        ctx.fillStyle = `hsl(${this.hue}, 90%, 75%)`;
        const s = this.size * 0.5;
        ctx.beginPath();
        ctx.moveTo(0, s * 0.3);
        ctx.bezierCurveTo(-s, -s * 0.6, -s * 1.3, s * 0.3, 0, s * 1.3);
        ctx.bezierCurveTo(s * 1.3, s * 0.3, s, -s * 0.6, 0, s * 0.3);
        ctx.fill();
      } else {
        // Draw 4-point golden star sparkle
        ctx.fillStyle = `hsl(${this.hue}, 100%, 78%)`;
        const r = this.size;
        ctx.beginPath();
        ctx.moveTo(0, -r);
        ctx.quadraticCurveTo(0, 0, r, 0);
        ctx.quadraticCurveTo(0, 0, 0, r);
        ctx.quadraticCurveTo(0, 0, -r, 0);
        ctx.quadraticCurveTo(0, 0, 0, -r);
        ctx.fill();
      }
      ctx.restore();
    }
  }

  function init() {
    canvas = document.getElementById('ambient-canvas');
    if (!canvas) return;
    ctx = canvas.getContext('2d');

    function resize() {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }
    window.addEventListener('resize', resize);
    resize();

    particles = [];
    for (let i = 0; i < MAX_PARTICLES; i++) {
      particles.push(new Particle(canvas.width, canvas.height, true));
    }

    animate();
  }

  function animate() {
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const w = canvas.width;
    const h = canvas.height;

    particles.forEach(p => {
      p.update(w, h);
      p.draw(ctx);
    });

    requestAnimationFrame(animate);
  }

  return { init };
})();

// Attach listeners when DOM is loaded
window.addEventListener('DOMContentLoaded', () => {
  AmbientParticles.init();

  const musicBtn = document.getElementById('music-btn');
  if (musicBtn) {
    musicBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      AudioEngine.toggleBgm();
    });
  }

  // Auto-init audio context on first user tap/click
  const unlockAudio = () => {
    AudioEngine.init();
    document.removeEventListener('click', unlockAudio);
    document.removeEventListener('touchstart', unlockAudio);
  };
  document.addEventListener('click', unlockAudio, { once: true });
  document.addEventListener('touchstart', unlockAudio, { once: true });
});
