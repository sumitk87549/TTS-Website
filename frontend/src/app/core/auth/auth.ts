import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Subject, tap } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${environment.apiBaseUrl}/auth`;

  // Signal-based token — integrates with OnPush CD and zoneless mode seamlessly
  // In production, HttpOnly cookies would be more secure but adds complexity for v1
  readonly token = signal<string | null>(localStorage.getItem('token'));

  // Computed signal: automatically updates any component reading it
  readonly isAuthenticated = computed(() => !!this.token());

  // Event bus to sync profile updates between Settings → UserProfileService → Dashboard
  // Kept as Subject (Observable) since it's a one-way notification, not a state value
  readonly profileUpdated$ = new Subject<void>();

  notifyProfileUpdate(): void {
    this.profileUpdated$.next();
  }

  register(data: any) {
    return this.http.post<{ token: string }>(`${this.apiUrl}/register`, data).pipe(
      tap(res => this.handleAuthSuccess(res.token))
    );
  }

  login(data: any) {
    return this.http.post<{ token: string }>(`${this.apiUrl}/login`, data).pipe(
      tap(res => this.handleAuthSuccess(res.token))
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.token.set(null);
    this.router.navigate(['/login']);
  }

  private handleAuthSuccess(token: string): void {
    localStorage.setItem('token', token);
    this.token.set(token);
  }
}
