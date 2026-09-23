/**
 * Komal's Birthday Surprise - Scene 4 Engine
 * Love Letter, Polaroid Memories & Live Photo Uploader / Customizer
 */

const MemoriesEngine = (() => {
  const STORAGE_KEY = 'komal_birthday_surprise_data_v1';

  // Sweet Romantic Defaults
  const DEFAULT_DATA = {
    recipient: 'Komal',
    sender: 'Your Favorite Person 💕',
    letter: `Happy Birthday to the girl who walked into my life and made everything brighter, gentler, and infinitely more beautiful.

Every time you laugh, my whole day lights up. Your kindness, your sweet stubbornness, the way your eyes sparkle when you're happy — there are a million little things about you that I fall in love with over and over again.

I hope today showers you with all the warmth, joy, and peace you so effortlessly give to everyone around you. You deserve the entire universe and every sweet dream your heart is holding.`,
    photos: [
      {
        url: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"><defs><linearGradient id="bg1" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%232D1B36"/><stop offset="100%" stop-color="%236B3E75"/></linearGradient></defs><rect width="400" height="400" fill="url(%23bg1)"/><circle cx="320" cy="90" r="45" fill="%23FFE599" opacity="0.9"/><circle cx="335" cy="80" r="42" fill="%232D1B36"/><g fill="%23FFF" opacity="0.8"><circle cx="70" cy="60" r="2.5"/><circle cx="150" cy="110" r="2"/><circle cx="210" cy="50" r="3"/><circle cx="80" cy="180" r="2"/><circle cx="250" cy="140" r="2.5"/></g><ellipse cx="200" cy="380" rx="220" ry="80" fill="%231C1022"/><circle cx="185" cy="225" r="14" fill="%23110815"/><circle cx="225" cy="235" r="12" fill="%23110815"/><path d="M170 310 Q175 250 185 240 Q195 250 200 310 Z" fill="%23110815"/><path d="M210 310 Q215 260 220 250 Q230 260 235 310 Z" fill="%23110815"/><path d="M198 238 Q205 230 212 238 Q205 248 198 238 Z" fill="%23FF85A1"/></svg>`,
        caption: 'That unforgettable radiant smile ✨'
      },
      {
        url: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"><defs><linearGradient id="bg2" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="%23FFF0F5"/><stop offset="100%" stop-color="%23FCD5CE"/></linearGradient></defs><rect width="400" height="400" fill="url(%23bg2)"/><ellipse cx="200" cy="320" rx="130" ry="25" fill="%23E8D1D8"/><rect x="110" y="220" width="180" height="90" rx="16" fill="%23FCE4EC"/><rect x="135" y="160" width="130" height="65" rx="12" fill="%23FFD3DD"/><rect x="195" y="125" width="10" height="35" rx="3" fill="%23FFE599"/><ellipse cx="200" cy="118" rx="7" ry="12" fill="%23FFA600"/><ellipse cx="200" cy="120" rx="4" ry="8" fill="%23FFF275"/><text x="200" y="275" font-family="sans-serif" font-size="28" text-anchor="middle" fill="%23FF6584">🍓 🍰 🍓</text></svg>`,
        caption: 'Sweetest laughs & memories 🌸'
      },
      {
        url: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"><defs><linearGradient id="bg3" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%23E8F4F8"/><stop offset="100%" stop-color="%23D8E2DC"/></linearGradient></defs><rect width="400" height="400" fill="url(%23bg3)"/><circle cx="150" cy="220" r="55" fill="%23FFE5D9"/><circle cx="150" cy="220" r="45" fill="%239C6644"/><circle cx="250" cy="220" r="55" fill="%23FFE5D9"/><circle cx="250" cy="220" r="45" fill="%237F4F24"/><path d="M190 140 Q200 115 210 140 Q200 155 190 140 Z" fill="%23FF85A1"/><text x="200" y="325" font-family="sans-serif" font-size="18" font-weight="bold" fill="%236B705C" text-anchor="middle">Favorite Coffee Dates &amp; Laughter</text></svg>`,
        caption: 'My favorite travel partner 🍓'
      },
      {
        url: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"><defs><linearGradient id="bg4" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="%23FDE2E4"/><stop offset="100%" stop-color="%23FFCAD4"/></linearGradient></defs><rect width="400" height="400" fill="url(%23bg4)"/><rect x="100" y="140" width="200" height="140" rx="8" fill="%23FFFDF9"/><path d="M100 140 L200 220 L300 140" stroke="%23F5B8C4" stroke-width="3" fill="none"/><circle cx="200" cy="215" r="18" fill="%23FF4D6D"/><text x="200" y="222" font-family="sans-serif" font-size="14" fill="%23FFF" text-anchor="middle">K</text><path d="M260 90 C260 65 290 65 290 90 C290 110 260 130 260 130 C260 130 230 110 230 90 C230 65 260 65 260 90 Z" fill="%23FF6584" opacity="0.85"/><text x="200" y="325" font-family="sans-serif" font-size="18" font-weight="bold" fill="%23A2677C" text-anchor="middle">Always &amp; Forever With You 💖</text></svg>`,
        caption: 'Always by your side, forever 💖'
      }
    ]
  };

  let activeData = { ...DEFAULT_DATA };

  function init() {
    loadSavedData();
    applyDataToDOM();
    setupPolaroidInteractions();
    setupModalDrawer();
    setupPhotoUploaders();
  }

  // Load from localStorage if present
  function loadSavedData() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        activeData = {
          ...DEFAULT_DATA,
          ...parsed,
          photos: parsed.photos && parsed.photos.length === 4 ? parsed.photos : DEFAULT_DATA.photos
        };
      }
    } catch (e) {
      console.warn('Could not load localStorage custom data', e);
      activeData = { ...DEFAULT_DATA };
    }
  }

  // Save to localStorage
  function saveData() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(activeData));
    } catch (e) {
      console.warn('LocalStorage save error (likely quota exceeded with large images)', e);
      alert('Note: Photos were loaded into the current session! For persistent offline storage, use compressed photos.');
    }
  }

  // Update DOM with current data
  function applyDataToDOM() {
    // Update all name occurrences
    const nameTargets = document.querySelectorAll('.name-target');
    nameTargets.forEach(el => {
      el.textContent = activeData.recipient;
    });

    // Update letter date
    const dateEl = document.getElementById('letter-date');
    if (dateEl) {
      const today = new Date();
      const options = { month: 'long', day: 'numeric', year: 'numeric' };
      dateEl.textContent = `${today.toLocaleDateString('en-US', options)} • For ${activeData.recipient}`;
    }

    // Update Letter Content
    const letterDisplay = document.getElementById('letter-content-display');
    if (letterDisplay) {
      const paragraphs = activeData.letter.split('\n\n').filter(p => p.trim());
      let html = '';
      paragraphs.forEach(p => {
        html += `<p>${escapeHtml(p)}</p>`;
      });
      html += `<p class="letter-closing">Always yours,<br><em id="sender-display" class="sender-name">${escapeHtml(activeData.sender)}</em></p>`;
      letterDisplay.innerHTML = html;
    }

    // Update Polaroids
    activeData.photos.forEach((photo, idx) => {
      const img = document.getElementById(`polaroid-img-${idx}`);
      const cap = document.getElementById(`polaroid-cap-${idx}`);
      if (img && photo.url) img.src = photo.url;
      if (cap) cap.textContent = photo.caption;

      // Also update modal input preview
      const previewImg = document.getElementById(`preview-photo-${idx}`);
      const capInput = document.getElementById(`cap-photo-${idx}`);
      if (previewImg && photo.url) {
        previewImg.src = photo.url;
        previewImg.classList.remove('hidden');
      }
      if (capInput) capInput.value = photo.caption || '';
    });

    // Sync modal fields
    const inputRecip = document.getElementById('input-recipient-name');
    const inputSender = document.getElementById('input-sender-name');
    const inputLetter = document.getElementById('input-letter-text');
    if (inputRecip) inputRecip.value = activeData.recipient;
    if (inputSender) inputSender.value = activeData.sender;
    if (inputLetter) inputLetter.value = activeData.letter;
  }

  function escapeHtml(str) {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // --- POLAROID TOUCH & TILT ---
  function setupPolaroidInteractions() {
    const cards = document.querySelectorAll('.polaroid-card');
    cards.forEach(card => {
      card.addEventListener('click', () => {
        AudioEngine.playChimeSound();
        // Temporary celebration sparkle
        if (window.confetti) {
          const rect = card.getBoundingClientRect();
          const x = (rect.left + rect.width / 2) / window.innerWidth;
          const y = (rect.top + rect.height / 2) / window.innerHeight;
          window.confetti({
            particleCount: 15,
            spread: 45,
            origin: { x, y },
            colors: ['#FF85A1', '#FFFDF9', '#FFD700']
          });
        }
      });
    });
  }

  // --- PERSONALIZATION MODAL & DRAWER ---
  function setupModalDrawer() {
    const modal = document.getElementById('personalize-modal');
    const openBtn = document.getElementById('open-settings-btn');
    const shortcutBtn = document.getElementById('open-uploader-shortcut');
    const closeBtn = document.getElementById('close-settings-btn');
    const saveBtn = document.getElementById('save-settings-btn');
    const resetBtn = document.getElementById('reset-defaults-btn');

    if (!modal) return;

    const openModal = () => {
      modal.classList.add('open');
      modal.setAttribute('aria-hidden', 'false');
    };

    const closeModal = () => {
      modal.classList.remove('open');
      modal.setAttribute('aria-hidden', 'true');
    };

    if (openBtn) openBtn.addEventListener('click', openModal);
    if (shortcutBtn) shortcutBtn.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);

    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal();
    });

    // Save changes
    if (saveBtn) {
      saveBtn.addEventListener('click', () => {
        const recip = document.getElementById('input-recipient-name')?.value.trim();
        const sender = document.getElementById('input-sender-name')?.value.trim();
        const letter = document.getElementById('input-letter-text')?.value.trim();

        if (recip) activeData.recipient = recip;
        if (sender) activeData.sender = sender;
        if (letter) activeData.letter = letter;

        // Captions
        activeData.photos.forEach((p, idx) => {
          const cap = document.getElementById(`cap-photo-${idx}`)?.value.trim();
          if (cap) p.caption = cap;
        });

        saveData();
        applyDataToDOM();
        closeModal();

        AudioEngine.playChimeSound();
        if (window.confetti) {
          window.confetti({
            particleCount: 40,
            spread: 70,
            origin: { y: 0.5 },
            colors: ['#FF85A1', '#FFD700', '#C4F4E4']
          });
        }
      });
    }

    // Reset defaults
    if (resetBtn) {
      resetBtn.addEventListener('click', () => {
        if (confirm('Reset letter and photos to default romantic values?')) {
          activeData = JSON.parse(JSON.stringify(DEFAULT_DATA));
          saveData();
          applyDataToDOM();
          closeModal();
        }
      });
    }
  }

  // --- PHOTO UPLOADER FILE HANDLERS ---
  function setupPhotoUploaders() {
    for (let i = 0; i < 4; i++) {
      const fileInput = document.getElementById(`file-photo-${i}`);
      const previewImg = document.getElementById(`preview-photo-${i}`);

      if (fileInput) {
        fileInput.addEventListener('change', (e) => {
          const file = e.target.files && e.target.files[0];
          if (!file) return;

          // Resize image on canvas to avoid blowing localStorage limits
          const reader = new FileReader();
          reader.onload = (loadEvent) => {
            const img = new Image();
            img.onload = () => {
              const canvas = document.createElement('canvas');
              const maxDim = 800;
              let width = img.width;
              let height = img.height;

              if (width > height && width > maxDim) {
                height = Math.round((height * maxDim) / width);
                width = maxDim;
              } else if (height > maxDim) {
                width = Math.round((width * maxDim) / height);
                height = maxDim;
              }

              canvas.width = width;
              canvas.height = height;
              const ctx = canvas.getContext('2d');
              ctx.drawImage(img, 0, 0, width, height);

              const compressedUrl = canvas.toDataURL('image/jpeg', 0.85);

              activeData.photos[i].url = compressedUrl;
              if (previewImg) {
                previewImg.src = compressedUrl;
                previewImg.classList.remove('hidden');
              }
            };
            img.src = loadEvent.target.result;
          };
          reader.readAsDataURL(file);
        });
      }
    }
  }

  return {
    init,
    getActiveData: () => activeData
  };
})();

window.addEventListener('DOMContentLoaded', () => {
  MemoriesEngine.init();
});
