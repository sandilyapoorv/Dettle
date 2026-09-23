/**
 * Komal's Birthday Surprise - Scene 1 & 2 Engine
 * Interactive Envelope & Balloon Pop Extravaganza
 */

const BalloonsEngine = (() => {
  // 6 Lovingly crafted compliments for Komal
  const COMPLIMENTS = [
    { text: "Your radiant smile lights up my entire world brighter than all the stars! 💖", emoji: "✨" },
    { text: "You have the sweetest, kindest, and most beautiful heart in the universe. 🌸", emoji: "🎀" },
    { text: "Hearing your cute laugh is the absolute favorite part of my day. 🍓", emoji: "🍰" },
    { text: "Every single moment spent with you feels like pure, effortless magic. 💕", emoji: "🌟" },
    { text: "Today is all about celebrating the incredible, graceful queen that you are! 👑", emoji: "💐" },
    { text: "Meeting you is the greatest blessing of my life. Happy Birthday my love! 🎂", emoji: "💖" }
  ];

  const BALLOON_COLORS = [
    { name: 'pink', bg: 'linear-gradient(135deg, #FF9EBB 0%, #FF6584 100%)', tail: '#FF6584' },
    { name: 'peach', bg: 'linear-gradient(135deg, #FFD0B5 0%, #FF9A6C 100%)', tail: '#FF9A6C' },
    { name: 'yellow', bg: 'linear-gradient(135deg, #FFF399 0%, #FFD633 100%)', tail: '#FFD633' },
    { name: 'mint', bg: 'linear-gradient(135deg, #B7F5DF 0%, #52D8A7 100%)', tail: '#52D8A7' },
    { name: 'lavender', bg: 'linear-gradient(135deg, #E6D5FC 0%, #B584F7 100%)', tail: '#B584F7' },
    { name: 'blue', bg: 'linear-gradient(135deg, #BAE6FD 0%, #60A5FA 100%)', tail: '#60A5FA' }
  ];

  let poppedCount = 0;
  const TOTAL_BALLOONS = 6;

  function init() {
    setupEnvelope();
    renderBalloons();
  }

  // --- SCENE 1: WAX-SEALED ENVELOPE ---
  function setupEnvelope() {
    const envelope = document.getElementById('envelope-trigger');
    const startBtn = document.getElementById('start-btn');

    if (!envelope) return;

    const openEnvelope = () => {
      if (envelope.classList.contains('open')) return;
      envelope.classList.add('open');
      AudioEngine.playChimeSound();
      AudioEngine.startBgm();

      // Confetti burst from envelope
      if (window.confetti) {
        const rect = envelope.getBoundingClientRect();
        const x = (rect.left + rect.width / 2) / window.innerWidth;
        const y = (rect.top + rect.height / 2) / window.innerHeight;
        window.confetti({
          particleCount: 40,
          spread: 70,
          origin: { x, y },
          colors: ['#FF85A1', '#FFD3DD', '#FFFDF9', '#FFD700']
        });
      }

      // Auto advance to Scene 2 after peek
      setTimeout(() => {
        if (window.App && typeof window.App.goToScene === 'function') {
          window.App.goToScene(2);
        }
      }, 1600);
    };

    envelope.addEventListener('click', openEnvelope);
    envelope.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        openEnvelope();
      }
    });

    if (startBtn) {
      startBtn.addEventListener('click', () => {
        openEnvelope();
        if (window.App && typeof window.App.goToScene === 'function') {
          window.App.goToScene(2);
        }
      });
    }
  }

  // --- SCENE 2: BALLOONS PLAYGROUND ---
  function renderBalloons() {
    const arena = document.getElementById('balloons-arena');
    if (!arena) return;

    arena.innerHTML = '';
    poppedCount = 0;
    updateCounter();

    BALLOON_COLORS.forEach((color, index) => {
      const balloon = document.createElement('div');
      balloon.className = 'balloon-item';
      balloon.setAttribute('role', 'button');
      balloon.setAttribute('tabindex', '0');
      balloon.setAttribute('aria-label', `Pop balloon ${index + 1}`);

      // Stagger floating animation delays
      const animDelay = (index * 0.45).toFixed(2);
      const floatDuration = (2.6 + (index % 3) * 0.4).toFixed(2);
      balloon.style.animationDelay = `${animDelay}s`;
      balloon.style.animationDuration = `${floatDuration}s`;

      balloon.innerHTML = `
        <div class="balloon-body" style="background: ${color.bg};"></div>
        <div class="balloon-tail" style="border-bottom-color: ${color.tail};"></div>
        <div class="balloon-string"></div>
      `;

      balloon.addEventListener('click', (e) => popBalloon(balloon, index, e));
      balloon.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          popBalloon(balloon, index, e);
        }
      });

      arena.appendChild(balloon);
    });
  }

  function popBalloon(balloonElem, index, event) {
    if (balloonElem.classList.contains('popped')) return;

    balloonElem.classList.add('popped');
    poppedCount++;
    AudioEngine.playPopSound();

    // Trigger colorful mini-confetti burst from balloon position
    if (window.confetti) {
      const rect = balloonElem.getBoundingClientRect();
      const x = (rect.left + rect.width / 2) / window.innerWidth;
      const y = (rect.top + rect.height / 2) / window.innerHeight;
      window.confetti({
        particleCount: 28,
        spread: 60,
        origin: { x, y },
        colors: ['#FF85A1', '#FFBE98', '#FFF2B2', '#80ED99', '#C77DFF', '#90E0EF']
      });
    }

    // Display compliment with highlight bounce
    const comp = COMPLIMENTS[index % COMPLIMENTS.length];
    const compDisplay = document.getElementById('compliment-display');
    const compText = document.getElementById('compliment-text');

    if (compDisplay && compText) {
      compText.innerHTML = `${comp.emoji} ${comp.text}`;
      compDisplay.classList.remove('highlight');
      // Trigger reflow to restart animation
      void compDisplay.offsetWidth;
      compDisplay.classList.add('highlight');
    }

    updateCounter();

    // Check if all balloons are popped
    if (poppedCount >= TOTAL_BALLOONS) {
      setTimeout(() => {
        onAllBalloonsPopped();
      }, 500);
    }
  }

  function updateCounter() {
    const counter = document.getElementById('popped-count');
    if (counter) {
      counter.textContent = poppedCount;
    }
  }

  function onAllBalloonsPopped() {
    AudioEngine.playFanfare();

    // Grand celebratory confetti burst
    if (window.confetti) {
      window.confetti({
        particleCount: 80,
        spread: 90,
        origin: { y: 0.6 },
        colors: ['#FF85A1', '#FFD700', '#C4F4E4', '#E8DDFA']
      });
    }

    // Unlock the next button
    const nextBtn = document.getElementById('balloons-next-btn');
    if (nextBtn) {
      nextBtn.classList.remove('disabled');
      nextBtn.focus();
    }

    const compText = document.getElementById('compliment-text');
    if (compText) {
      compText.innerHTML = `🎉 <strong>All balloons popped, Komal!</strong> Time to make your birthday wish on the cake! 🎂`;
    }
  }

  function reset() {
    renderBalloons();
    const nextBtn = document.getElementById('balloons-next-btn');
    if (nextBtn) {
      nextBtn.classList.add('disabled');
    }
    const compText = document.getElementById('compliment-text');
    if (compText) {
      compText.innerHTML = "Tap any balloon above to discover what's inside!";
    }
  }

  return {
    init,
    reset
  };
})();

window.addEventListener('DOMContentLoaded', () => {
  BalloonsEngine.init();
});
