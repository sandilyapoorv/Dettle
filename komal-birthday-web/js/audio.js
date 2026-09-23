/**
 * Komal's Birthday Surprise - Audio Engine
 * Features:
 * 1. Arctic Monkeys - "I Wanna Be Yours" Ambient Synth/Arrangement
 * 2. HTML5 Audio fallback for external/uploaded MP3 tracks
 * 3. Balloon pop, candle blow, and celebration sound effects
 */

const AudioEngine = (() => {
  let ctx = null;
  let isPlayingBgm = false;
  let bgmTimer = null;
  let masterGain = null;
  let bgmAudioEl = null;
  let usingCustomAudio = false;

  // Initialize Web Audio Context
  function init() {
    if (!ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) {
        ctx = new AudioCtx();
        masterGain = ctx.createGain();
        masterGain.gain.setValueAtTime(0.48, ctx.currentTime);
        masterGain.connect(ctx.destination);
      }
    }
    if (ctx && ctx.state === 'suspended') {
      ctx.resume();
    }

    if (!bgmAudioEl) {
      bgmAudioEl = document.getElementById('bgm-audio');
      if (bgmAudioEl) {
        bgmAudioEl.volume = 0.75;
        bgmAudioEl.addEventListener('error', () => {
          // If local mp3 not found, fallback to Web Audio synth
          usingCustomAudio = false;
        });
      }
    }
  }

  // --- ARCTIC MONKEYS: "I WANNA BE YOURS" SYNTHESIZER ---
  // Key: C minor | Slow, moody, romantic vibe (~68 BPM)
  const FREQS = {
    C2: 65.41, F2: 87.31, G2: 98.00, Bb2: 116.54,
    C3: 130.81, Eb3: 155.56, F3: 174.61, G3: 196.00, Ab3: 207.65, Bb3: 233.08,
    C4: 261.63, D4: 293.66, Eb4: 311.13, F4: 349.23, G4: 392.00, Ab4: 415.30, Bb4: 466.16,
    C5: 523.25, D5: 587.33, Eb5: 622.25, F5: 698.46, G5: 783.99, Bb5: 932.33
  };

  // Play a warm, vintage Rhodes / electric guitar style note
  function playSynthTone(freq, startTime, duration = 1.8, volume = 0.22, isBass = false) {
    if (!ctx) return;
    try {
      const osc = ctx.createOscillator();
      const oscHarmonic = ctx.createOscillator();
      const filter = ctx.createBiquadFilter();
      const gain = ctx.createGain();

      // Warm analog tone
      osc.type = isBass ? 'sine' : 'triangle';
      osc.frequency.setValueAtTime(freq, startTime);

      if (!isBass) {
        oscHarmonic.type = 'sine';
        oscHarmonic.frequency.setValueAtTime(freq * 2, startTime); // Octave overtone
      }

      // Lowpass filter for dreamy indie atmosphere
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(isBass ? 350 : 1600, startTime);
      filter.Q.setValueAtTime(isBass ? 1 : 2.5, startTime);

      // Amplitude envelope
      gain.gain.setValueAtTime(0.0001, startTime);
      gain.gain.exponentialRampToValueAtTime(volume, startTime + 0.04);
      gain.gain.exponentialRampToValueAtTime(volume * 0.6, startTime + duration * 0.4);
      gain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);

      osc.connect(filter);
      if (!isBass) oscHarmonic.connect(filter);
      filter.connect(gain);
      gain.connect(masterGain);

      osc.start(startTime);
      if (!isBass) oscHarmonic.start(startTime);
      osc.stop(startTime + duration);
      if (!isBass) oscHarmonic.stop(startTime + duration);
    } catch (e) {
      console.warn('Synth error', e);
    }
  }

  // Arctic Monkeys - "I Wanna Be Yours" Melodic Arrangement
  // "Secrets I have held in my heart... Are harder to hide than I thought... Maybe I just wanna be yours"
  const IWBY_ARRANGEMENT = [
    // === BAR 1: Cm Chord (C - Eb - G) ===
    { bass: FREQS.C2, chord: [FREQS.C3, FREQS.G3, FREQS.Eb4], delay: 0 },
    // "Secrets I have" (G4 - G4 - G4 - G4)
    { lead: FREQS.G4, delay: 350, dur: 0.35 },
    { lead: FREQS.G4, delay: 750, dur: 0.35 },
    { lead: FREQS.G4, delay: 1150, dur: 0.35 },
    { lead: FREQS.G4, delay: 1550, dur: 0.45 },
    // "held in my heart" (F4 - Eb4 - F4)
    { lead: FREQS.F4, delay: 2000, dur: 0.4 },
    { lead: FREQS.Eb4, delay: 2450, dur: 0.45 },
    { lead: FREQS.F4, delay: 2900, dur: 0.8 },

    // === BAR 2: Fm Chord (F - Ab - C) ===
    { bass: FREQS.F2, chord: [FREQS.F3, FREQS.C4, FREQS.Ab4], delay: 3600 },
    // "Are harder to" (G4 - G4 - G4 - G4)
    { lead: FREQS.G4, delay: 3950, dur: 0.35 },
    { lead: FREQS.G4, delay: 4350, dur: 0.35 },
    { lead: FREQS.G4, delay: 4750, dur: 0.35 },
    { lead: FREQS.G4, delay: 5150, dur: 0.45 },
    // "hide than I thought" (F4 - Eb4 - F4)
    { lead: FREQS.F4, delay: 5600, dur: 0.4 },
    { lead: FREQS.Eb4, delay: 6050, dur: 0.45 },
    { lead: FREQS.F4, delay: 6500, dur: 0.8 },

    // === BAR 3: Bb Chord (Bb - D - F) ===
    { bass: FREQS.Bb2, chord: [FREQS.Bb3, FREQS.D4, FREQS.F4], delay: 7200 },
    // "Maybe I just" (Eb4 - D4 - C4)
    { lead: FREQS.Eb4, delay: 7550, dur: 0.45 },
    { lead: FREQS.D4, delay: 8050, dur: 0.45 },
    { lead: FREQS.C4, delay: 8550, dur: 0.5 },
    // "wanna be" (Eb4 - D4 - C4)
    { lead: FREQS.Eb4, delay: 9100, dur: 0.45 },
    { lead: FREQS.D4, delay: 9600, dur: 0.45 },
    { lead: FREQS.C4, delay: 10100, dur: 0.6 },

    // === BAR 4: G / Eb Chord Resolving -> "Yours... I wanna be yours" ===
    { bass: FREQS.G2, chord: [FREQS.G3, FREQS.Bb3, FREQS.D4], delay: 10800 },
    // "yours..."
    { lead: FREQS.C4, delay: 11200, dur: 0.8 },
    // "I wanna be yours..." (C4 - Eb4 - F4 - G4 - F4 - Eb4 - C4)
    { lead: FREQS.C4, delay: 12200, dur: 0.4 },
    { lead: FREQS.Eb4, delay: 12650, dur: 0.4 },
    { lead: FREQS.F4, delay: 13100, dur: 0.4 },
    { lead: FREQS.G4, delay: 13550, dur: 0.7 },
    { lead: FREQS.F4, delay: 14350, dur: 0.45 },
    { lead: FREQS.Eb4, delay: 14850, dur: 0.5 },
    { lead: FREQS.C4, delay: 15400, dur: 1.2 }
  ];

  const SONG_LOOP_DURATION = 17200;

  function scheduleIwbyLoop() {
    if (!isPlayingBgm || !ctx || usingCustomAudio) return;
    const now = ctx.currentTime;

    IWBY_ARRANGEMENT.forEach(step => {
      const time = now + (step.delay / 1000);

      // Play bass note
      if (step.bass) {
        playSynthTone(step.bass, time, 3.4, 0.28, true);
      }

      // Play chord backing
      if (step.chord) {
        step.chord.forEach((freq, idx) => {
          playSynthTone(freq, time + (idx * 0.04), 3.0, 0.11, false);
        });
      }

      // Play vocal/guitar lead melody
      if (step.lead) {
        playSynthTone(step.lead, time, step.dur || 0.6, 0.26, false);
      }
    });

    bgmTimer = setTimeout(() => {
      scheduleIwbyLoop();
    }, SONG_LOOP_DURATION);
  }

  function startBgm() {
    init();
    if (isPlayingBgm) return Promise.resolve();
    isPlayingBgm = true;

    // Try HTML5 audio first (e.g. if i-wanna-be-yours.mp3 exists or was uploaded)
    if (bgmAudioEl && bgmAudioEl.src && !bgmAudioEl.src.endsWith('/')) {
      const playPromise = bgmAudioEl.play();
      if (playPromise !== undefined) {
        return playPromise
          .then(() => {
            usingCustomAudio = true;
            updateUi();
          })
          .catch((err) => {
            console.warn('Autoplay restricted by browser, waiting for first interaction', err);
            isPlayingBgm = false;
            updateUi();
            throw err;
          });
      }
    }

    // Default to synthesized "I Wanna Be Yours"
    usingCustomAudio = false;
    scheduleIwbyLoop();
    updateUi();
    return Promise.resolve();
  }

  function stopBgm() {
    isPlayingBgm = false;
    if (bgmTimer) {
      clearTimeout(bgmTimer);
      bgmTimer = null;
    }
    if (bgmAudioEl) {
      try { bgmAudioEl.pause(); } catch (e) {}
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

  function setCustomAudio(url) {
    init();
    if (bgmAudioEl) {
      bgmAudioEl.src = url;
      bgmAudioEl.load();
      usingCustomAudio = true;
      if (isPlayingBgm) {
        if (bgmTimer) clearTimeout(bgmTimer);
        bgmAudioEl.play();
      }
    }
  }

  function updateUi() {
    const btn = document.getElementById('music-btn');
    const statusDetail = document.getElementById('music-status-detail');

    if (btn) {
      if (isPlayingBgm) {
        btn.classList.add('playing');
        btn.title = 'Pause Arctic Monkeys - I Wanna Be Yours';
      } else {
        btn.classList.remove('playing');
        btn.title = 'Play Arctic Monkeys - I Wanna Be Yours';
      }
    }

    if (statusDetail) {
      statusDetail.textContent = usingCustomAudio 
        ? 'Playing custom uploaded MP3 track' 
        : 'Playing built-in Arctic Monkeys - I Wanna Be Yours arrangement';
    }
  }

  // --- SOUND EFFECTS ---

  function playPopSound() {
    init();
    if (!ctx) return;
    try {
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(360, now);
      osc.frequency.exponentialRampToValueAtTime(55, now + 0.08);

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

  function playChimeSound() {
    init();
    if (!ctx) return;
    try {
      const now = ctx.currentTime;
      [FREQS.C5, FREQS.Eb5, FREQS.G5, FREQS.C5].forEach((freq, idx) => {
        playSynthTone(freq, now + (idx * 0.08), 0.7, 0.22, false);
      });
    } catch (e) {
      console.warn(e);
    }
  }

  function playFanfare() {
    init();
    if (!ctx) return;
    try {
      const now = ctx.currentTime;
      const fanfare = [
        { note: FREQS.C4, delay: 0 },
        { note: FREQS.Eb4, delay: 180 },
        { note: FREQS.G4, delay: 360 },
        { note: FREQS.C5, delay: 600 },
        { note: FREQS.Bb4, delay: 900 },
        { note: FREQS.C5, delay: 1200 }
      ];

      fanfare.forEach(item => {
        playSynthTone(item.note, now + (item.delay / 1000), 1.0, 0.32, false);
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
    setCustomAudio,
    playPopSound,
    playChimeSound,
    playFanfare,
    isPlaying: () => isPlayingBgm
  };
})();

// Attach event listeners when DOM loads
window.addEventListener('DOMContentLoaded', () => {
  const musicBtn = document.getElementById('music-btn');
  if (musicBtn) {
    musicBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      AudioEngine.toggleBgm();
    });
  }

  // Audio file uploader in modal
  const musicFileInput = document.getElementById('music-file-input');
  if (musicFileInput) {
    musicFileInput.addEventListener('change', (e) => {
      const file = e.target.files && e.target.files[0];
      if (file) {
        const objectUrl = URL.createObjectURL(file);
        AudioEngine.setCustomAudio(objectUrl);
        AudioEngine.startBgm();
      }
    });
  }

  // Auto-start music as soon as website is opened
  const autoPlaySound = () => {
    AudioEngine.init();
    AudioEngine.startBgm().catch(() => {
      // Browser autoplay policy blocked raw unprompted audio -> start on very first touch/click
      const startOnInteraction = () => {
        AudioEngine.startBgm();
        ['pointerdown', 'touchstart', 'touchend', 'click', 'scroll', 'keydown'].forEach(ev => {
          document.removeEventListener(ev, startOnInteraction);
        });
      };
      ['pointerdown', 'touchstart', 'touchend', 'click', 'scroll', 'keydown'].forEach(ev => {
        document.addEventListener(ev, startOnInteraction, { once: true, passive: true });
      });
    });
  };

  // Attempt immediately on load
  autoPlaySound();
  window.addEventListener('load', autoPlaySound, { once: true });
});
