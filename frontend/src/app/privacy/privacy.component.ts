import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../core/theme/theme.service';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <main class="legal-page india-pattern-bg">
      <nav class="navbar">
        <div class="nav-inner">
          <a routerLink="/" class="logo">
            <img src="logo.png" alt="words2voice logo" class="logo-img" />
            <span class="logo-text">words2voice</span>
          </a>
          <button class="theme-toggle" (click)="themeService.toggle()">
            {{ themeService.isDark ? '☀️' : '🌙' }}
          </button>
        </div>
      </nav>

      <section class="legal-content">
        <h1>Privacy Policy</h1>
        <p class="last-updated">Last updated: July 2026</p>

        <h2>1. Information We Collect</h2>
        <p>We collect information you provide directly to us, such as your email address when you register. We also collect anonymous usage data (like characters generated) to help us improve the service.</p>

        <h2>2. How We Use Your Scripts</h2>
        <p><strong>Your scripts are your property.</strong> We process them only to generate the requested audio. We do <em>not</em> use your scripts or the generated audio to train our AI models. Audio files are temporarily stored for your convenience and are periodically deleted.</p>

        <h2>3. Data Security</h2>
        <p>We use industry-standard security measures to protect your personal information. Passwords are securely hashed, and all data transmission is encrypted via HTTPS.</p>

        <h2>4. Contact Us</h2>
        <p>If you have any questions about this Privacy Policy, please contact us via our <a routerLink="/contact" class="link">Contact page</a>.</p>
      </section>
    </main>
  `,
  styles: [`
    .legal-page { min-height: 100vh; padding-bottom: 4rem; }
    .navbar { border-bottom: 1px solid var(--border); background: var(--glass-bg); padding: 1rem 0; }
    .nav-inner { max-width: 1200px; margin: 0 auto; padding: 0 1.5rem; display: flex; justify-content: space-between; align-items: center; }
    .logo { display: flex; align-items: center; gap: 0.75rem; text-decoration: none; }
    .logo-img { width: 32px; height: 32px; border-radius: 0.5rem; }
    .logo-text { font-weight: 800; font-size: 1.25rem; letter-spacing: -0.02em; color: var(--text-primary); }
    .theme-toggle { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 50%; width: 40px; height: 40px; cursor: pointer; color: var(--text-primary); }
    .legal-content { max-width: 800px; margin: 4rem auto 0; padding: 0 1.5rem; }
    h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }
    .last-updated { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 3rem; font-family: 'JetBrains Mono', monospace; }
    h2 { font-size: 1.5rem; margin: 2.5rem 0 1rem; color: var(--text-primary); }
    p { margin-bottom: 1.25rem; color: var(--text-secondary); line-height: 1.7; }
    strong { color: var(--text-primary); }
    .link { color: var(--accent); text-decoration: underline; }
  `]
})
export class PrivacyComponent {
  themeService = inject(ThemeService);
}
