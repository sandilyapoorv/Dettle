/**
 * Komal's Birthday Surprise - Master Orchestrator
 * Scene Transitions, Navigation Stepper, Touch Gestures & Tablet Adaptations
 */

const App = (() => {
  let currentScene = 1;
  const TOTAL_SCENES = 5;

  function init() {
    setupNavigationButtons();
    setupStepperDots();
    setupTouchGestures();
    setupKeyboardNavigation();
    goToScene(1, false);
  }

  // --- SCENE ROUTER ---
  function goToScene(targetIndex, playSound = true) {
    if (targetIndex < 1 || targetIndex > TOTAL_SCENES) return;

    if (playSound && typeof AudioEngine !== 'undefined') {
      AudioEngine.playChimeSound();
    }

    // Hide old scene, show target scene
    const scenes = document.querySelectorAll('.scene');
    scenes.forEach(s => s.classList.remove('active'));

    const targetScene = document.getElementById(`scene-${targetIndex}`);
    if (targetScene) {
      targetScene.classList.add('active');
    }

    // Scroll viewport to top
    const viewport = document.querySelector('.viewport-stage');
    if (viewport) {
      viewport.scrollTo({ top: 0, behavior: 'smooth' });
    }

    currentScene = targetIndex;
    updateStepper(currentScene);
  }

  // --- STEPPER PROGRESS BAR ---
  function updateStepper(activeNum) {
    const dots = document.querySelectorAll('.step-dot');
    dots.forEach(dot => {
      const dotNum = parseInt(dot.getAttribute('data-scene'), 10);
      dot.classList.remove('active', 'completed');

      if (dotNum === activeNum) {
        dot.classList.add('active');
      } else if (dotNum < activeNum) {
        dot.classList.add('completed');
      }
    });
  }

  // --- NAVIGATION BUTTONS ---
  function setupNavigationButtons() {
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        if (btn.classList.contains('disabled')) return;

        const target = parseInt(btn.getAttribute('data-target'), 10);
        if (!isNaN(target)) {
          goToScene(target);
        }
      });
    });
  }

  // --- STEPPER DOTS CLICKABLE ---
  function setupStepperDots() {
    const dots = document.querySelectorAll('.step-dot');
    dots.forEach(dot => {
      dot.addEventListener('click', () => {
        const target = parseInt(dot.getAttribute('data-scene'), 10);
        if (!isNaN(target)) {
          goToScene(target);
        }
      });
    });
  }

  // --- TOUCH GESTURES (SWIPE FOR TABLETS) ---
  function setupTouchGestures() {
    let touchStartX = 0;
    let touchStartY = 0;
    const stage = document.querySelector('.viewport-stage');

    if (!stage) return;

    stage.addEventListener('touchstart', (e) => {
      touchStartX = e.changedTouches[0].screenX;
      touchStartY = e.changedTouches[0].screenY;
    }, { passive: true });

    stage.addEventListener('touchend', (e) => {
      const touchEndX = e.changedTouches[0].screenX;
      const touchEndY = e.changedTouches[0].screenY;
      handleSwipe(touchStartX, touchStartY, touchEndX, touchEndY);
    }, { passive: true });
  }

  function handleSwipe(startX, startY, endX, endY) {
    const diffX = endX - startX;
    const diffY = endY - startY;

    // Ensure horizontal swipe is dominant and significant (> 60px)
    if (Math.abs(diffX) > 60 && Math.abs(diffX) > Math.abs(diffY) * 1.5) {
      if (diffX < 0) {
        // Swipe Left -> Next Scene (if allowed)
        if (canAdvanceScene(currentScene)) {
          goToScene(currentScene + 1);
        }
      } else {
        // Swipe Right -> Previous Scene
        if (currentScene > 1) {
          goToScene(currentScene - 1);
        }
      }
    }
  }

  function canAdvanceScene(sceneNum) {
    if (sceneNum === 1) return true;
    if (sceneNum === 2) {
      const btn = document.getElementById('balloons-next-btn');
      return btn && !btn.classList.contains('disabled');
    }
    if (sceneNum === 3) {
      const btn = document.getElementById('cake-next-btn');
      return btn && !btn.classList.contains('disabled');
    }
    if (sceneNum === 4) return true;
    return false;
  }

  // --- KEYBOARD ARROWS ---
  function setupKeyboardNavigation() {
    window.addEventListener('keydown', (e) => {
      // Ignore if user is currently typing in input/textarea
      if (['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) return;

      if (e.key === 'ArrowRight') {
        if (canAdvanceScene(currentScene) && currentScene < TOTAL_SCENES) {
          goToScene(currentScene + 1);
        }
      } else if (e.key === 'ArrowLeft') {
        if (currentScene > 1) {
          goToScene(currentScene - 1);
        }
      }
    });
  }

  return {
    init,
    goToScene,
    getCurrentScene: () => currentScene
  };
})();

// Attach to global window
window.App = App;

window.addEventListener('DOMContentLoaded', () => {
  App.init();
});
