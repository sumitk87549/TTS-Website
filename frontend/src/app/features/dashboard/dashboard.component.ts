import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth';
import { ThemeService } from '../../core/theme/theme.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  themeService = inject(ThemeService);
  http = inject(HttpClient);

  menuOpen = false;
  usage = { charactersUsed: 0, charactersLimit: 5000, generationCount: 0 };

  ngOnInit() {
    this.http.get<any>(`${environment.apiBaseUrl}/usage/today`).subscribe({
      next: (res) => this.usage = res,
      error: () => {}
    });
  }

  toggleMenu() { this.menuOpen = !this.menuOpen; }
  closeMenu() { this.menuOpen = false; }
  logout() { this.authService.logout(); }

  get usagePercent() {
    if (this.usage.charactersLimit === 0) return 0;
    return Math.min(100, Math.round((this.usage.charactersUsed / this.usage.charactersLimit) * 100));
  }
}
