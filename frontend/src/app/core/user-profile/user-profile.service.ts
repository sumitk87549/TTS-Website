import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { tap } from 'rxjs';

export interface UserProfile {
  email: string;
  displayName: string;
  createdAt: string;
  isAdmin: boolean;
}

/**
 * UserProfileService — Signal-based single source of truth for the logged-in user's profile.
 *
 * Separates the "current user" concern from AuthService (single responsibility).
 * Components (Dashboard, Settings) read from this shared service instead of each
 * making their own /me HTTP calls — demonstrating sibling communication via shared service.
 */
@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiBaseUrl}/me`;

  /** Signal holding the full user profile — null when not yet loaded or logged out */
  readonly profile = signal<UserProfile | null>(null);

  /** Derived signals for common fields — no need to unwrap the whole object */
  readonly displayName = computed(() => this.profile()?.displayName ?? 'My Profile');
  readonly email = computed(() => this.profile()?.email ?? '');
  readonly isAdmin = computed(() => this.profile()?.isAdmin ?? false);

  /** Avatar stored locally (not in backend) */
  readonly avatarEmoji = signal<string>(localStorage.getItem('w2v-avatar') ?? '🎤');
  readonly avatarColor = signal<string>(
    localStorage.getItem('w2v-avatar-color') ?? 'linear-gradient(135deg, #7c5cf7, #e8608a)'
  );

  /**
   * Load user profile from the API.
   * Idempotent — can be called multiple times; only fetches if not already loaded.
   * Call on app boot or after login.
   */
  loadProfile() {
    return this.http.get<UserProfile>(this.apiUrl).pipe(
      tap(profile => this.profile.set(profile))
    );
  }

  /**
   * Update display name (called from Settings).
   * On success, updates the Signal so Dashboard sidebar refreshes automatically.
   */
  updateDisplayName(displayName: string) {
    return this.http.patch<UserProfile>(this.apiUrl, { displayName }).pipe(
      tap(() => {
        this.profile.update(p => p ? { ...p, displayName } : p);
      })
    );
  }

  /**
   * Save avatar preferences to localStorage and update Signals.
   * Dashboard and Settings both read the same avatarEmoji/avatarColor signals.
   */
  saveAvatar(emoji: string, color: string): void {
    localStorage.setItem('w2v-avatar', emoji);
    localStorage.setItem('w2v-avatar-color', color);
    this.avatarEmoji.set(emoji);
    this.avatarColor.set(color);
  }

  /**
   * Reload avatar from localStorage (for initial load or after storage changes).
   */
  reloadAvatar(): void {
    const emoji = localStorage.getItem('w2v-avatar');
    const color = localStorage.getItem('w2v-avatar-color');
    if (emoji) this.avatarEmoji.set(emoji);
    if (color) this.avatarColor.set(color);
  }

  /** Clear profile on logout */
  clearProfile(): void {
    this.profile.set(null);
  }
}
