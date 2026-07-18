import { Injectable, signal } from '@angular/core';

export type Theme = 'night' | 'day';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly storageKey = 'w2v-theme';
  readonly theme = signal<Theme>(this.loadTheme());

  get isDark(): boolean {
    return this.theme() === 'night';
  }

  constructor() {
    this.applyTheme(this.theme());
  }

  toggle(): void {
    const next: Theme = this.theme() === 'night' ? 'day' : 'night';
    this.theme.set(next);
    this.applyTheme(next);
    localStorage.setItem(this.storageKey, next);
  }

  private loadTheme(): Theme {
    const stored = localStorage.getItem(this.storageKey);
    if (stored === 'day' || stored === 'night') return stored;
    // Default to night
    return 'night';
  }

  private applyTheme(theme: Theme): void {
    document.documentElement.setAttribute('data-theme', theme);
  }
}
