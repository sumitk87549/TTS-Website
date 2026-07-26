import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  stats: any = {};
  topVoices: any[] = [];
  recentUsers: any[] = [];
  dailyStats: any[] = [];
  contacts: any[] = [];
  analyticsSummary: any = {};
  loading = true;
  error = '';
  activeTab = 'overview';

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.loading = true;
    this.error = '';

    this.http.get<any>(`${environment.apiBaseUrl}/admin/stats`).subscribe({
      next: data => {
        this.stats = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        this.error = err.status === 403 ? 'Admin access required.' : 'Failed to load admin data.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });

    this.http.get<any[]>(`${environment.apiBaseUrl}/admin/top-voices`).subscribe({
      next: data => { this.topVoices = data; this.cdr.detectChanges(); },
      error: () => {}
    });

    this.http.get<any[]>(`${environment.apiBaseUrl}/admin/recent-users`).subscribe({
      next: data => { this.recentUsers = data; this.cdr.detectChanges(); },
      error: () => {}
    });

    this.http.get<any[]>(`${environment.apiBaseUrl}/admin/daily-stats`).subscribe({
      next: data => { this.dailyStats = data; this.cdr.detectChanges(); },
      error: () => {}
    });

    this.http.get<any[]>(`${environment.apiBaseUrl}/admin/contacts`).subscribe({
      next: data => { this.contacts = data; this.cdr.detectChanges(); },
      error: () => {}
    });

    this.http.get<any>(`${environment.apiBaseUrl}/admin/analytics-summary`).subscribe({
      next: data => { this.analyticsSummary = data; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  get successRate(): string {
    const total = (this.stats.totalGenerations || 0) + (this.stats.failedGenerations || 0);
    if (total === 0) return '—';
    return ((this.stats.totalGenerations / total) * 100).toFixed(1) + '%';
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.cdr.detectChanges();
  }
}
