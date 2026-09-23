/**
 * Local Canvas Confetti Engine (Zero-dependency fallback)
 * Ensures magical confetti bursts work 100% reliably even offline
 */
if (!window.confetti) {
  window.confetti = (() => {
    const canvas = document.getElementById('confetti-canvas');
    if (!canvas) return () => {};
    const ctx = canvas.getContext('2d');
    let particles = [];
    let animationFrame = null;

    function resize() {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    }
    window.addEventListener('resize', resize);
    resize();

    class ConfettiPiece {
      constructor(opts) {
        const x = (opts.origin && opts.origin.x !== undefined) ? opts.origin.x * canvas.width : canvas.width / 2;
        const y = (opts.origin && opts.origin.y !== undefined) ? opts.origin.y * canvas.height : canvas.height / 2;
        
        this.x = x;
        this.y = y;
        this.size = Math.random() * 8 + 6;
        
        const angle = (opts.angle !== undefined ? opts.angle : 90) * (Math.PI / 180);
        const spread = (opts.spread !== undefined ? opts.spread : 70) * (Math.PI / 180);
        const randAngle = angle + (Math.random() - 0.5) * spread;
        const velocity = (opts.startVelocity || 25) * (Math.random() * 0.5 + 0.8);

        this.vx = Math.cos(randAngle) * velocity;
        this.vy = -Math.sin(randAngle) * velocity;
        this.gravity = 0.45;
        this.drag = 0.96;
        this.rotation = Math.random() * 360;
        this.rotSpeed = (Math.random() - 0.5) * 15;
        this.opacity = 1;
        this.decay = Math.random() * 0.015 + 0.01;

        const colors = opts.colors || ['#FF85A1', '#FFD700', '#C4F4E4', '#FFBE98', '#E8DDFA'];
        this.color = colors[Math.floor(Math.random() * colors.length)];
      }

      update() {
        this.vx *= this.drag;
        this.vy *= this.drag;
        this.vy += this.gravity;
        this.x += this.vx;
        this.y += this.vy;
        this.rotation += this.rotSpeed;
        this.opacity -= this.decay;
      }

      draw(c) {
        if (this.opacity <= 0) return;
        c.save();
        c.translate(this.x, this.y);
        c.rotate((this.rotation * Math.PI) / 180);
        c.globalAlpha = Math.max(0, this.opacity);
        c.fillStyle = this.color;
        c.fillRect(-this.size / 2, -this.size / 2, this.size, this.size * 0.6);
        c.restore();
      }
    }

    function loop() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i];
        p.update();
        p.draw(ctx);
        if (p.opacity <= 0 || p.y > canvas.height + 20) {
          particles.splice(i, 1);
        }
      }

      if (particles.length > 0) {
        animationFrame = requestAnimationFrame(loop);
      } else {
        cancelAnimationFrame(animationFrame);
        animationFrame = null;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    }

    return function fire(options = {}) {
      const count = options.particleCount || 40;
      for (let i = 0; i < count; i++) {
        particles.push(new ConfettiPiece(options));
      }
      if (!animationFrame) {
        loop();
      }
    };
  })();
}
