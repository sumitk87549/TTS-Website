import { Component, OnInit, inject, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ThemeService } from '../core/theme/theme.service';
import { AnalyticsService } from '../core/analytics/analytics.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss'],
  // ★ Default change detection so all async updates are caught automatically
})
export class LandingComponent implements OnInit {
  themeService = inject(ThemeService);
  private http = inject(HttpClient);
  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);
  private analytics = inject(AnalyticsService);

  voices: any[] = [];
  selectedVoiceId = '';
  textToSynthesize = 'कम बोलो, ज़्यादा करो — let your results speak for you.';
  isLoading = false;
  audioUrl: string | null = null;
  errorMessage: string | null = null;
  maxLength = 300;
  mobileMenuOpen = false;

  totalGenerations = 0;
  totalUsers = 0;

  ngOnInit() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/public/tts/voices`)
      .subscribe({
        next: (data) => {
          this.ngZone.run(() => {
            this.voices = data || [];
            if (this.voices.length > 0) {
              this.selectedVoiceId = this.voices[0].engine_voice_id;
            }
            this.cdr.markForCheck();
          });
        },
        error: (err) => console.error('Failed to load voices', err)
      });

    this.http.get<any>(`${environment.apiBaseUrl}/public/stats`)
      .subscribe({
        next: (data) => {
          this.ngZone.run(() => {
            this.totalGenerations = data?.totalGenerations || 0;
            this.totalUsers = data?.totalUsers || 0;
            this.cdr.markForCheck();
          });
        },
        error: () => { }
      });
  }

  get textLength(): number {
    return this.textToSynthesize ? this.textToSynthesize.length : 0;
  }

  onListen() {
    if (!this.textToSynthesize || this.textToSynthesize.trim() === '') {
      this.errorMessage = 'Please enter some text.';
      return;
    }
    if (this.textToSynthesize.length > this.maxLength) {
      this.errorMessage = 'Text is too long.';
      return;
    }

    this.analytics.track('preview_listen_clicked', { textLength: this.textLength, voiceId: this.selectedVoiceId });

    this.isLoading = true;
    this.errorMessage = null;
    this.audioUrl = null;

    // Revoke old blob URL
    if (this.audioUrl && (this.audioUrl as string).startsWith('blob:')) {
      URL.revokeObjectURL(this.audioUrl);
    }

    const payload = {
      text: this.textToSynthesize,
      engineVoiceId: this.selectedVoiceId
    };

    this.http.post(`${environment.apiBaseUrl}/public/tts/preview`, payload, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          // ★ NgZone.run + markForCheck = guaranteed immediate render
          this.ngZone.run(() => {
            this.isLoading = false;
            this.audioUrl = URL.createObjectURL(blob as Blob);
            this.cdr.markForCheck();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            this.isLoading = false;
            if (err.status === 429) {
              this.errorMessage = 'Rate limit reached. Please try again in a bit.';
            } else if (err.status === 503) {
              this.errorMessage = 'Voice engine is warming up — try again in a moment.';
            } else {
              this.errorMessage = 'Something went wrong during synthesis.';
            }
            this.cdr.markForCheck();
          });
        }
      });
  }
}
