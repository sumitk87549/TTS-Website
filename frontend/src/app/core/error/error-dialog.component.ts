import {
  Component, inject, ChangeDetectionStrategy, HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ErrorDisplayService } from './error-display.service';
import { UserFacingError } from './api-error.model';

/**
 * Beautiful error dialog overlay — shown for any unhandled backend error.
 *
 * Design:
 * - Glassmorphism card with blurred backdrop
 * - Animated entrance from bottom
 * - Emoji icon matching error type
 * - Title, plain-English analogy, themed quote with attribution
 * - Action button (Retry / Dismiss / Go Back)
 *
 * Add <app-error-dialog /> once in app.html — it self-manages visibility via signals.
 */
@Component({
  selector: 'app-error-dialog',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (errorDisplay.currentError(); as err) {
      <div class="error-backdrop" (click)="onBackdropClick($event)" role="dialog"
           aria-modal="true" [attr.aria-label]="err.title">
        <div class="error-card" role="alertdialog">

          <!-- Icon -->
          <div class="error-icon" [attr.data-type]="iconType(err)">
            {{ iconEmoji(err) }}
          </div>

          <!-- Sorry label -->
          <div class="sorry-badge">
            <span>We're Sorry</span>
          </div>

          <!-- Title -->
          <h2 class="error-title">{{ err.title }}</h2>

          <!-- Analogy -->
          <p class="error-analogy">{{ err.analogy }}</p>

          <!-- Divider -->
          <div class="error-divider"></div>

          <!-- Quote -->
          <blockquote class="error-quote">
            <span class="quote-mark">"</span>
            {{ err.quote }}
            <span class="quote-mark">"</span>
            <footer class="quote-author">— {{ err.quoteAuthor }}</footer>
          </blockquote>

          <!-- Actions -->
          <div class="error-actions">
            @if (err.canRetry) {
              <button class="btn-retry" (click)="dismiss()">
                🔄 {{ err.actionLabel }}
              </button>
            } @else {
              <button class="btn-dismiss" (click)="dismiss()">
                {{ err.actionLabel }}
              </button>
            }
            <button class="btn-close" (click)="dismiss()" aria-label="Close error">
              ✕ Dismiss
            </button>
          </div>

          <!-- Error code (subtle, for debugging) -->
          <p class="error-code">Error: {{ err.code }} ({{ err.httpStatus }})</p>

        </div>
      </div>
    }
  `,
  styles: [`
    .error-backdrop {
      position: fixed;
      inset: 0;
      z-index: 10000;
      background: rgba(0, 0, 0, 0.65);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      display: flex;
      align-items: flex-end;
      justify-content: center;
      padding: 1rem;
      animation: backdrop-in 0.25s ease-out;
    }

    @media (min-width: 640px) {
      .error-backdrop {
        align-items: center;
      }
    }

    .error-card {
      background: var(--surface, #1a1a2e);
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 1.5rem;
      padding: 2rem 2rem 1.5rem;
      max-width: 480px;
      width: 100%;
      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6),
                  0 0 0 1px rgba(255, 255, 255, 0.06) inset;
      animation: card-up 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
      text-align: center;
    }

    .error-icon {
      font-size: 3.5rem;
      line-height: 1;
      margin-bottom: 0.75rem;
      animation: icon-bounce 0.6s cubic-bezier(0.36, 0.07, 0.19, 0.97) 0.2s both;
    }

    .sorry-badge {
      display: inline-block;
      padding: 0.25rem 0.85rem;
      border-radius: 999px;
      font-size: 0.7rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
      margin-bottom: 0.85rem;
    }

    .error-title {
      font-size: 1.35rem;
      font-weight: 700;
      color: var(--text-primary, #f0f0f0);
      margin: 0 0 0.85rem;
      line-height: 1.3;
    }

    .error-analogy {
      font-size: 0.9rem;
      color: var(--text-secondary, #a0a0b0);
      line-height: 1.7;
      margin: 0 0 1.25rem;
    }

    .error-divider {
      height: 1px;
      background: linear-gradient(
        to right,
        transparent,
        rgba(255, 255, 255, 0.1),
        transparent
      );
      margin-bottom: 1.25rem;
    }

    .error-quote {
      background: rgba(255, 255, 255, 0.04);
      border-left: 3px solid var(--accent, #7c3aed);
      border-radius: 0.5rem;
      padding: 0.85rem 1.1rem;
      margin: 0 0 1.5rem;
      font-style: italic;
      color: var(--text-secondary, #a0a0b0);
      font-size: 0.88rem;
      line-height: 1.65;
      text-align: left;
    }

    .quote-mark {
      font-size: 1.5rem;
      line-height: 0;
      vertical-align: -0.4em;
      color: var(--accent, #7c3aed);
      opacity: 0.6;
      font-style: normal;
    }

    .quote-author {
      display: block;
      margin-top: 0.5rem;
      font-size: 0.78rem;
      font-style: normal;
      color: var(--text-muted, #6b7280);
      font-weight: 500;
    }

    .error-actions {
      display: flex;
      gap: 0.65rem;
      justify-content: center;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }

    .btn-retry {
      padding: 0.65rem 1.5rem;
      border-radius: 0.75rem;
      border: none;
      background: var(--accent, #7c3aed);
      color: #fff;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-retry:hover {
      background: var(--accent-hover, #6d28d9);
      transform: translateY(-1px);
      box-shadow: 0 4px 16px rgba(124, 58, 237, 0.4);
    }

    .btn-dismiss {
      padding: 0.65rem 1.5rem;
      border-radius: 0.75rem;
      border: none;
      background: var(--accent, #7c3aed);
      color: #fff;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-dismiss:hover {
      background: var(--accent-hover, #6d28d9);
      transform: translateY(-1px);
    }

    .btn-close {
      padding: 0.65rem 1.25rem;
      border-radius: 0.75rem;
      border: 1px solid rgba(255, 255, 255, 0.12);
      background: transparent;
      color: var(--text-secondary, #a0a0b0);
      font-size: 0.88rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-close:hover {
      background: rgba(255, 255, 255, 0.06);
      color: var(--text-primary, #f0f0f0);
    }

    .error-code {
      font-size: 0.7rem;
      color: var(--text-muted, #4b5563);
      letter-spacing: 0.03em;
      margin: 0;
    }

    @keyframes backdrop-in {
      from { opacity: 0; }
      to   { opacity: 1; }
    }

    @keyframes card-up {
      from { opacity: 0; transform: translateY(40px) scale(0.95); }
      to   { opacity: 1; transform: translateY(0) scale(1); }
    }

    @keyframes icon-bounce {
      0%, 100% { transform: translateY(0); }
      30%       { transform: translateY(-8px); }
      60%       { transform: translateY(-4px); }
    }
  `]
})
export class ErrorDialogComponent {
  protected readonly errorDisplay = inject(ErrorDisplayService);

  dismiss(): void {
    this.errorDisplay.dismiss();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('error-backdrop')) {
      this.dismiss();
    }
  }

  @HostListener('keydown.escape')
  onEscape(): void {
    this.dismiss();
  }

  iconEmoji(err: UserFacingError): string {
    switch (err.code) {
      case 'TTS_ENGINE_UNAVAILABLE':
      case 'TTS_ENGINE_BUSY':       return '🎙️';
      case 'TTS_ENGINE_TIMEOUT':    return '⏱️';
      case 'DAILY_LIMIT_EXCEEDED':  return '📔';
      case 'TEXT_TOO_LONG':         return '📚';
      case 'RATE_LIMIT_EXCEEDED':   return '🎚️';
      case 'VALIDATION_ERROR':      return '✏️';
      case 'EMAIL_ALREADY_EXISTS':  return '📬';
      case 'INVALID_CREDENTIALS':   return '🔑';
      case 'WRONG_CURRENT_PASSWORD':return '🔒';
      case 'RESOURCE_NOT_FOUND':    return '🔍';
      case 'ACCESS_DENIED':         return '🚫';
      case 'INTERNAL_ERROR':        return '🛠️';
      default:                       return '😅';
    }
  }

  iconType(err: UserFacingError): string {
    if (err.httpStatus >= 500) return 'error';
    if (err.httpStatus === 429) return 'warning';
    if (err.httpStatus === 404) return 'info';
    return 'warning';
  }
}
