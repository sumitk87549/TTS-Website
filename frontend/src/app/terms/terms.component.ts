import { Component, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../core/theme/theme.service';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <main class="legal-page india-pattern-bg">
      <nav class="navbar">
        <div class="nav-inner">
          <button class="back-btn" (click)="location.back()" aria-label="Go back">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            Back
          </button>
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
        <h1>Terms of Use</h1>
        <p class="last-updated">Last updated: July 2026</p>

        <h2>1. Acceptance of Terms</h2>
        <p>By accessing or using words2voice, you agree to be bound by these Terms of Use. If you disagree with any part of the terms, you may not access the service.</p>

        <h2>2. Service Usage</h2>
        <p>You agree not to use words2voice to generate audio that is illegal, defamatory, harassing, or intended to spread misinformation. We reserve the right to terminate accounts that violate this policy.</p>

        <h2>3. Intellectual Property</h2>
        <p>The audio generated from your original scripts is your property. You may use it for commercial and non-commercial purposes. However, the voices themselves and the underlying technology remain the intellectual property of words2voice and its licensors.</p>

        <h2>4. "As Is" Service</h2>
        <p>This service is currently in Public Beta. It is provided "as is" without warranty of any kind. We do not guarantee uninterrupted access or error-free generations.</p>

        <h2>5. Changes to Terms</h2>
        <p>We may modify these terms at any time. Continued use of the service constitutes acceptance of the modified terms.</p>
      </section>
    </main>
  `,
  styles: [`
    .legal-page { min-height: 100vh; padding-bottom: 4rem; }
    .navbar { border-bottom: 1px solid var(--border); background: var(--glass-bg); backdrop-filter: blur(20px); padding: 1rem 0; }
    .nav-inner { max-width: 1200px; margin: 0 auto; padding: 0 1.5rem; display: flex; justify-content: space-between; align-items: center; }
    .logo { display: flex; align-items: center; gap: 0.75rem; text-decoration: none; }
    .logo-img { width: 32px; height: 32px; border-radius: 0.5rem; }
    .logo-text { font-weight: 800; font-size: 1.25rem; letter-spacing: -0.02em; color: var(--text-primary); }
    .theme-toggle { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 50%; width: 40px; height: 40px; cursor: pointer; color: var(--text-primary); font-size: 1rem; display: flex; align-items: center; justify-content: center; }
    .back-btn {
      display: flex; align-items: center; gap: 0.4rem;
      padding: 0.4rem 0.85rem;
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      border-radius: 0.5rem;
      color: var(--text-secondary);
      font-size: 0.82rem;
      font-weight: 600;
      cursor: pointer;
      font-family: inherit;
      transition: all 0.2s;
    }
    .back-btn:hover { border-color: var(--accent); color: var(--accent); background: var(--accent-subtle); }
    .legal-content { max-width: 800px; margin: 4rem auto 0; padding: 0 1.5rem; }
    h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }
    .last-updated { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 3rem; font-family: 'JetBrains Mono', monospace; }
    h2 { font-size: 1.5rem; margin: 2.5rem 0 1rem; color: var(--text-primary); }
    p { margin-bottom: 1.25rem; color: var(--text-secondary); line-height: 1.7; }
    strong { color: var(--text-primary); }
  `]
})
export class TermsComponent {
  themeService = inject(ThemeService);
  location = inject(Location);
}
