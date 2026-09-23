/**
 * Komal's Birthday Surprise - Scene 5 Engine
 * 3D Mystery Gift Box Unwrapping & Grand Finale
 */

const GiftEngine = (() => {
  let isOpened = false;

  function init() {
    setupGiftBox();
    setupFinaleActions();
  }

  function setupGiftBox() {
    const giftWrapper = document.getElementById('gift-box-trigger');
    if (!giftWrapper) return;

    const openGift = () => {
      if (isOpened) return;
      isOpened = true;

      giftWrapper.classList.add('opened');
      AudioEngine.playFanfare();

      // Explode celebration fireworks and confetti
      launchGrandFireworks();

      // Reveal finale card
      setTimeout(() => {
        const finaleCard = document.getElementById('finale-card');
        const instruction = document.getElementById('gift-instruction');
        const tapHint = document.querySelector('.gift-tap-hint');

        if (instruction) instruction.textContent = '🎉 Happy Birthday to my favorite girl! 🎉';
        if (tapHint) tapHint.style.display = 'none';
        if (finaleCard) {
          finaleCard.classList.remove('hidden');
          finaleCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 550);
    };

    giftWrapper.addEventListener('click', openGift);
    giftWrapper.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        openGift();
      }
    });
  }

  function launchGrandFireworks() {
    if (!window.confetti) return;

    const duration = 4 * 1000;
    const animationEnd = Date.now() + duration;
    const defaults = { startVelocity: 30, spread: 360, ticks: 60, zIndex: 1000 };

    function randomInRange(min, max) {
      return Math.random() * (max - min) + min;
    }

    const interval = setInterval(() => {
      const timeLeft = animationEnd - Date.now();
      if (timeLeft <= 0) {
        return clearInterval(interval);
      }

      const particleCount = 50 * (timeLeft / duration);
      window.confetti({
        ...defaults,
        particleCount,
        origin: { x: randomInRange(0.1, 0.4), y: Math.random() - 0.2 },
        colors: ['#FF85A1', '#FFD700', '#C4F4E4', '#FFBE98', '#E8DDFA']
      });
      window.confetti({
        ...defaults,
        particleCount,
        origin: { x: randomInRange(0.6, 0.9), y: Math.random() - 0.2 },
        colors: ['#FF85A1', '#FFD700', '#C4F4E4', '#FFBE98', '#E8DDFA']
      });
    }, 250);
  }

  function setupFinaleActions() {
    const replayBtn = document.getElementById('replay-btn');
    const readLetterBtn = document.getElementById('open-share-btn');

    if (replayBtn) {
      replayBtn.addEventListener('click', () => {
        reset();
        if (window.BalloonsEngine) window.BalloonsEngine.reset();
        if (window.CakeEngine) window.CakeEngine.reset();

        const envelope = document.getElementById('envelope-trigger');
        if (envelope) envelope.classList.remove('open');

        if (window.App && typeof window.App.goToScene === 'function') {
          window.App.goToScene(1);
        }
      });
    }

    if (readLetterBtn) {
      readLetterBtn.addEventListener('click', () => {
        if (window.App && typeof window.App.goToScene === 'function') {
          window.App.goToScene(4);
        }
      });
    }
  }

  function reset() {
    isOpened = false;
    const giftWrapper = document.getElementById('gift-box-trigger');
    const finaleCard = document.getElementById('finale-card');
    const instruction = document.getElementById('gift-instruction');
    const tapHint = document.querySelector('.gift-tap-hint');

    if (giftWrapper) giftWrapper.classList.remove('opened');
    if (finaleCard) finaleCard.classList.add('hidden');
    if (instruction) instruction.textContent = 'Tap the golden ribbon to untie the gift box...';
    if (tapHint) tapHint.style.display = 'flex';
  }

  return {
    init,
    reset
  };
})();

window.addEventListener('DOMContentLoaded', () => {
  GiftEngine.init();
});
