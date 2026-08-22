import { Component, inject, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth';
import { ThemeService } from '../../core/theme/theme.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  themeService = inject(ThemeService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  menuOpen = false;
  sidebarCollapsed = false;
  isAdmin = false;

  // Avatar state
  avatarEmoji = '🎤';
  avatarColor = 'linear-gradient(135deg, #7c5cf7, #e8608a)';
  avatarName = 'My Profile';

  private sub?: Subscription;

  ngOnInit() {
    this.loadProfileData();
    this.sub = this.authService.profileUpdated$.subscribe(() => {
      this.loadProfileData();
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  loadProfileData() {
    const saved = localStorage.getItem('w2v-avatar');
    if (saved) this.avatarEmoji = saved;
    const savedColor = localStorage.getItem('w2v-avatar-color');
    if (savedColor) this.avatarColor = savedColor;

    this.http.get<any>(`${environment.apiBaseUrl}/me`).subscribe(res => {
      if (res.displayName) {
        this.avatarName = res.displayName;
      }
      if (res.isAdmin) {
        this.isAdmin = res.isAdmin;
      }
      this.cdr.markForCheck();
    });
  }

  toggleMenu() { this.menuOpen = !this.menuOpen; }
  closeMenu() { this.menuOpen = false; }
  toggleSidebar() { this.sidebarCollapsed = !this.sidebarCollapsed; }
  logout() { this.authService.logout(); }
}
