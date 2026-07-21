import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { BehaviorSubject, Subject, tap } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${environment.apiBaseUrl}/auth`;

  // Store token in localStorage for this hobby project
  // In production, HttpOnly cookies would be more secure but adds complexity for v1
  private tokenSubject = new BehaviorSubject<string | null>(localStorage.getItem('token'));
  public token$ = this.tokenSubject.asObservable();

  get token(): string | null {
    return this.tokenSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.token;
  }

  // Event bus to sync profile updates between Settings and Dashboard
  public profileUpdated$ = new Subject<void>();

  notifyProfileUpdate() {
    this.profileUpdated$.next();
  }

  register(data: any) {
    return this.http.post<{token: string}>(`${this.apiUrl}/register`, data).pipe(
      tap(res => this.handleAuthSuccess(res.token))
    );
  }

  login(data: any) {
    return this.http.post<{token: string}>(`${this.apiUrl}/login`, data).pipe(
      tap(res => this.handleAuthSuccess(res.token))
    );
  }

  logout() {
    localStorage.removeItem('token');
    this.tokenSubject.next(null);
    this.router.navigate(['/login']);
  }

  private handleAuthSuccess(token: string) {
    localStorage.setItem('token', token);
    this.tokenSubject.next(token);
  }
}
