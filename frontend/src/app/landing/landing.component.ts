import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ThemeService } from '../core/theme/theme.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss']
})
export class LandingComponent implements OnInit {
  themeService = inject(ThemeService);
  private http = inject(HttpClient);

  voices: any[] = [];
  selectedVoiceId = '';
  textToSynthesize = 'Namaste, aap kaise hain? Mujhe aapki awaaz mein kuch sunna hai.';
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
          this.voices = data;
          if (this.voices.length > 0) {
            this.selectedVoiceId = this.voices[0].engine_voice_id;
          }
        },
        error: (err) => console.error('Failed to load voices', err)
      });

    this.http.get<any>(`${environment.apiBaseUrl}/public/stats`)
      .subscribe({
        next: (data) => {
          this.totalGenerations = data.totalGenerations || 0;
          this.totalUsers = data.totalUsers || 0;
        },
        error: () => {} // Silent fail — stats are non-critical
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

    this.isLoading = true;
    this.errorMessage = null;
    this.audioUrl = null;

    const payload = {
      text: this.textToSynthesize,
      engineVoiceId: this.selectedVoiceId
    };

    this.http.post(`${environment.apiBaseUrl}/public/tts/preview`, payload, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          this.isLoading = false;
          const url = window.URL.createObjectURL(blob);
          this.audioUrl = url;
        },
        error: (err) => {
          this.isLoading = false;
          if (err.status === 429) {
            this.errorMessage = 'Rate limit reached. Please try again in a bit.';
          } else if (err.status === 503) {
            this.errorMessage = 'Voice engine is warming up — try again in a moment.';
          } else {
            this.errorMessage = 'Something went wrong during synthesis.';
          }
        }
      });
  }
}
