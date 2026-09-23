/**
 * Komal's Birthday Surprise - Scene 3 Engine
 * Interactive Birthday Cake, Candle Extinguishing & Microphone Blow Detection
 */

const CakeEngine = (() => {
  let candlesCount = 3;
  let extinguishedCount = 0;
  let isListeningMic = false;
  let micStream = null;
  let analyser = null;
  let micInterval = null;

  function init() {
    setupCandleTaps();
    setupMicDetection();
  }

  // --- CANDLE TAP & SWIPE EXTINGUISH ---
  function setupCandleTaps() {
    const candles = document.querySelectorAll('.candle');
    candles.forEach((candle, idx) => {
      const extinguishHandler = (e) => {
        e.stopPropagation();
        extinguishCandle(candle);
      };

      candle.addEventListener('click', extinguishHandler);
      candle.addEventListener('touchstart', extinguishHandler, { passive: true });
    });

    // Also allow clicking anywhere on the cake to blow out candles
    const cakeInteractive = document.getElementById('cake-interactive');
    if (cakeInteractive) {
      cakeInteractive.addEventListener('click', () => {
        // Find first lit candle and extinguish
        const litCandle = document.querySelector('.candle:not(.extinguished)');
        if (litCandle) {
          extinguishCandle(litCandle);
        }
      });
    }
  }

  function extinguishCandle(candleElem) {
    if (candleElem.classList.contains('extinguished')) return;

    candleElem.classList.add('extinguished');
    extinguishedCount++;

    // Play gentle blow sound
    AudioEngine.playChimeSound();

    // Trigger little puff of smoke particles
    if (window.confetti) {
      const rect = candleElem.getBoundingClientRect();
      const x = (rect.left + rect.width / 2) / window.innerWidth;
      const y = (rect.top + 10) / window.innerHeight;
      window.confetti({
        particleCount: 12,
        spread: 35,
        startVelocity: 15,
        origin: { x, y },
        colors: ['#E0E0E0', '#FFFDF9', '#FFA600']
      });
    }

    checkAllExtinguished();
  }

  function extinguishAllCandles() {
    const candles = document.querySelectorAll('.candle:not(.extinguished)');
    candles.forEach((c, i) => {
      setTimeout(() => {
        extinguishCandle(c);
      }, i * 200);
    });
  }

  function checkAllExtinguished() {
    if (extinguishedCount >= candlesCount) {
      // Stop mic if running
      stopMicListening();

      // Trigger Celebration!
      setTimeout(() => {
        onCakeCelebration();
      }, 400);
    }
  }

  function onCakeCelebration() {
    AudioEngine.playFanfare();

    // Mega birthday confetti cascade
    if (window.confetti) {
      // Left and right cannons
      const end = Date.now() + 2.5 * 1000;
      const colors = ['#FF85A1', '#FFD700', '#C4F4E4', '#E8DDFA', '#FFBE98'];

      (function frame() {
        window.confetti({
          particleCount: 5,
          angle: 60,
          spread: 55,
          origin: { x: 0 },
          colors: colors
        });
        window.confetti({
          particleCount: 5,
          angle: 120,
          spread: 55,
          origin: { x: 1 },
          colors: colors
        });

        if (Date.now() < end) {
          requestAnimationFrame(frame);
        }
      })();
    }

    // Show Wish Success Card
    const successCard = document.getElementById('wish-success');
    if (successCard) {
      successCard.style.display = 'block';
    }

    // Enable Next Button
    const nextBtn = document.getElementById('cake-next-btn');
    if (nextBtn) {
      nextBtn.classList.remove('disabled');
      nextBtn.focus();
    }

    const instruction = document.getElementById('blow-instruction');
    if (instruction) {
      instruction.innerHTML = `✨ <strong>Happy Birthday Komal!</strong> Your wish has been sent to the universe! ✨`;
    }
  }

  // --- MICROPHONE BLOW DETECTION ---
  function setupMicDetection() {
    const micBtn = document.getElementById('enable-mic-btn');
    if (!micBtn) return;

    micBtn.addEventListener('click', async (e) => {
      e.stopPropagation();
      if (isListeningMic) {
        stopMicListening();
        micBtn.innerHTML = `<span>🎙️ Enable Mic to Blow Out Candles</span>`;
      } else {
        await startMicListening();
      }
    });
  }

  async function startMicListening() {
    const micBtn = document.getElementById('enable-mic-btn');
    const feedback = document.getElementById('mic-feedback');

    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        alert('Microphone access is not supported by your current browser. You can tap the candles to blow them out!');
        return;
      }

      micStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      const audioCtx = new AudioCtx();
      const source = audioCtx.createMediaStreamSource(micStream);
      analyser = audioCtx.createAnalyser();
      analyser.fftSize = 256;
      source.connect(analyser);

      isListeningMic = true;
      if (micBtn) {
        micBtn.innerHTML = `<span>🛑 Mic Active: Blow into your tablet/device! 💨</span>`;
        micBtn.style.borderColor = '#52D8A7';
        micBtn.style.background = '#E8FBF4';
      }

      const bufferLength = analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      let blowStreak = 0;
      micInterval = setInterval(() => {
        if (!isListeningMic || extinguishedCount >= candlesCount) return;
        analyser.getByteFrequencyData(dataArray);

        // Calculate average volume energy
        let sum = 0;
        for (let i = 0; i < bufferLength; i++) {
          sum += dataArray[i];
        }
        const average = sum / bufferLength;

        if (feedback) {
          feedback.textContent = `Blow meter: ${Math.round(average)}%`;
        }

        // Blowing into mic generates sustained low-frequency high-volume air noise
        if (average > 38) {
          blowStreak++;
          if (blowStreak >= 2) {
            // Blow detected!
            extinguishAllCandles();
            blowStreak = 0;
          }
        } else {
          blowStreak = Math.max(0, blowStreak - 1);
        }
      }, 100);

    } catch (err) {
      console.warn('Microphone permission or access error:', err);
      if (feedback) {
        feedback.textContent = `Mic blocked or unavailable. Tap candles to blow!`;
      }
      if (micBtn) {
        micBtn.innerHTML = `<span>👆 Tap Candles to Blow Them Out!</span>`;
      }
    }
  }

  function stopMicListening() {
    isListeningMic = false;
    if (micInterval) {
      clearInterval(micInterval);
      micInterval = null;
    }
    if (micStream) {
      micStream.getTracks().forEach(track => track.stop());
      micStream = null;
    }
    const feedback = document.getElementById('mic-feedback');
    if (feedback) feedback.textContent = '';
  }

  function reset() {
    extinguishedCount = 0;
    const candles = document.querySelectorAll('.candle');
    candles.forEach(c => c.classList.remove('extinguished'));

    const successCard = document.getElementById('wish-success');
    if (successCard) successCard.style.display = 'none';

    const nextBtn = document.getElementById('cake-next-btn');
    if (nextBtn) nextBtn.classList.add('disabled');

    const instruction = document.getElementById('blow-instruction');
    if (instruction) {
      instruction.innerHTML = `Close your eyes, make your deepest wish, and <strong>blow gently into your mic</strong> or <strong>tap each candle flame</strong>!`;
    }
  }

  return {
    init,
    extinguishAllCandles,
    reset
  };
})();

window.addEventListener('DOMContentLoaded', () => {
  CakeEngine.init();
});
