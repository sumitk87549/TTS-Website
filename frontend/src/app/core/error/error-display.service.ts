import { Injectable, signal } from '@angular/core';
import { UserFacingError } from './api-error.model';

/**
 * Signal-based service that manages the currently displayed error overlay.
 *
 * Components push errors here; the ErrorDialogComponent reads and displays them.
 * This decouples error display from individual components — no more error strings
 * scattered across studio, history, settings, etc.
 */
@Injectable({ providedIn: 'root' })
export class ErrorDisplayService {
  /** The currently active error. null = no error overlay shown. */
  readonly currentError = signal<UserFacingError | null>(null);

  /** Show a user-facing error overlay */
  show(error: UserFacingError): void {
    this.currentError.set(error);
  }

  /** Dismiss the current error overlay */
  dismiss(): void {
    this.currentError.set(null);
  }

  /** Whether the error overlay is currently visible */
  get isVisible() {
    return this.currentError() !== null;
  }
}
