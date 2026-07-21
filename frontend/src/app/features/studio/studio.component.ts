import { Component, inject, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Voice {
  id: number;
  engine_voice_id: string;
  display_name: string;
  gender: string;
  style_tag: string;
  description?: string;
}

@Component({
  selector: 'app-studio',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './studio.component.html',
  styleUrls: ['./studio.component.scss']
})
export class StudioComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);

  // ── Text ────────────────────────────────────────────────────────────
  text = '';
  maxChars = 1000;

  // ── Voices ──────────────────────────────────────────────────────────
  voices: Voice[] = [];
  maleVoices: Voice[] = [];
  femaleVoices: Voice[] = [];
  selectedTab: 'Male' | 'Female' | 'My Voices' = 'Male';
  selectedVoice: Voice | null = null;

  // ── TTS Controls ────────────────────────────────────────────────────
  readonly langOptions = [
    { value: 'na',  label: '🌐 Auto (Hinglish)' },
    { value: 'hi',  label: '🇮🇳 Hindi' },
    { value: 'en',  label: '🇬🇧 English' },
  ];
  selectedLang = 'na';

  readonly speedPresets = [
    { value: 0.75, label: '0.75×' },
    { value: 0.9,  label: '0.9×'  },
    { value: 1.0,  label: '1.0×'  },
    { value: 1.1,  label: '1.1×'  },
    { value: 1.25, label: '1.25×' },
    { value: 1.5,  label: '1.5×'  },
  ];
  speed = 1.0;

  readonly qualityPresets = [
    { steps: 4,  label: 'Draft',    description: 'Fast preview' },
    { steps: 8,  label: 'Standard', description: 'Balanced quality' },
    { steps: 16, label: 'High',     description: 'Rich voice quality' },
    { steps: 32, label: 'Ultra',    description: 'Studio-grade' },
  ];
  // ★ Ultra is now the default
  selectedQuality = this.qualityPresets[3];

  // ── Generation state ─────────────────────────────────────────────────
  generating = false;
  error = '';
  audioUrl: string | null = null;

  // ── Voice Preview state ──────────────────────────────────────────────
  previewLoadingId: string | null = null;
  previewPlayingId: string | null = null;
  private _previewAudio: HTMLAudioElement | null = null;

  // ── Projects ─────────────────────────────────────────────────────────
  projects: any[] = [];
  selectedProjectId: number | null = null;

  // ── UI State ─────────────────────────────────────────────────────────
  settingsOpen = false;
  showFirstRun = !localStorage.getItem('w2v-seen-intro');

  readonly placeholderExamples = [
    'Type your Hindi / English / Hinglish script here…\n\nFor example: Yaar, aaj का दिन bahut amazing था!',
    'एक समय की बात है, एक छोटे से गाँव में…\n\n(Hindi, English, and Hinglish all work here!)',
    'Good morning! Aaj hum baat karenge ek important topic ke baare mein…',
    'नमस्ते! आपका हमारे channel पर swagat hai। आज ka video bahut special hai…',
  ];
  private _placeholderIdx = 0;
  currentPlaceholder = this.placeholderExamples[0];
  private _placeholderTimer: ReturnType<typeof setInterval> | null = null;

  readonly scriptPresets = [
    { label: 'Story / Narration', text: 'एक समय की बात है, एक छोटे से गाँव में एक होनहार लड़की रहती थी। उसका नाम था आनंदी।' },
    { label: 'Promotional',       text: 'क्या आप अपने व्यापार को नई ऊँचाइयों पर ले जाना चाहते हैं? आज ही हमसे जुड़ें और अपने सपनों को साकार करें!' },
    { label: 'Greeting',          text: 'नमस्ते! आपका स्वागत है। आपका दिन शुभ और मंगलमय हो।' },
    { label: 'News Bulletin',     text: 'आज की ताज़ा ख़बरें: देश के विभिन्न हिस्सों में मानसून की अच्छी बारिश दर्ज की गई है।' },
    { label: 'Hinglish Casual',   text: "Yaar, aaj ka din bahut amazing raha! Maine socha tha ki kuch naya try karein, toh let's go!" },
    { label: 'English Business',  text: 'Good morning! Our quarterly results show a 28% growth in revenue. Let us walk through the key highlights.' },
  ];

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit() {
    this.fetchVoices();
    this.fetchProjects();
    this._placeholderTimer = setInterval(() => {
      if (!this.text) {
        this._placeholderIdx = (this._placeholderIdx + 1) % this.placeholderExamples.length;
        this.currentPlaceholder = this.placeholderExamples[this._placeholderIdx];
      }
    }, 4000);
  }

  ngOnDestroy() {
    if (this._placeholderTimer) clearInterval(this._placeholderTimer);
    if (this._previewAudio) {
      this._previewAudio.pause();
      this._previewAudio = null;
    }
  }

  dismissFirstRun() {
    this.showFirstRun = false;
    localStorage.setItem('w2v-seen-intro', '1');
  }

  fetchVoices() {
    this.http.get<Voice[]>(`${environment.apiBaseUrl}/voices`).subscribe({
      next: res => {
        this.ngZone.run(() => {
          this.voices = (res || []).map(v => ({
            ...v,
            display_name: v.display_name.replace(/^(M|F)\d+\s*-\s*/i, '')
          }));
          this.maleVoices   = this.voices.filter(v => v.gender === 'male');
          this.femaleVoices = this.voices.filter(v => v.gender === 'female');
          if (this.maleVoices.length > 0) this.selectVoice(this.maleVoices[0]);
          this.cdr.markForCheck();
        });
      },
      error: () => {}
    });
  }

  fetchProjects() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/projects`).subscribe({
      next: res => {
        this.ngZone.run(() => {
          this.projects = res;
          this.cdr.markForCheck();
        });
      },
      error: () => {}
    });
  }

  selectVoice(v: Voice) { this.selectedVoice = v; }

  applyPreset(preset: { label: string; text: string }) {
    this.text = preset.text;
    const hasHindi = /[\u0900-\u097F]/.test(preset.text);
    const hasLatin = /[a-zA-Z]/.test(preset.text);
    if (hasHindi && hasLatin) this.selectedLang = 'na';
    else if (hasHindi)        this.selectedLang = 'hi';
    else                      this.selectedLang = 'en';
  }

  tryRandomPreset() {
    const idx = Math.floor(Math.random() * this.scriptPresets.length);
    this.applyPreset(this.scriptPresets[idx]);
  }

  get charsPercent() { return Math.round((this.text.length / this.maxChars) * 100); }

  selectQuality(q: typeof this.qualityPresets[0]) { this.selectedQuality = q; }
  selectSpeed(s: number) { this.speed = s; }
  selectLang(l: string) { this.selectedLang = l; }

  // ── Voice Preview ──────────────────────────────────────────────────────
  playVoicePreview(v: Voice, event: MouseEvent) {
    event.stopPropagation(); // Don't select the voice

    // Stop any existing preview
    if (this._previewAudio) {
      this._previewAudio.pause();
      this._previewAudio = null;
    }

    // If clicking the same voice that's playing, just stop
    if (this.previewPlayingId === v.engine_voice_id) {
      this.previewPlayingId = null;
      return;
    }

    this.previewLoadingId = v.engine_voice_id;
    this.previewPlayingId = null;

    const greeting = 'नमस्ते! मैं आपकी आवाज़ हूँ। कैसे हैं आप?';

    this.http.post(
      `${environment.apiBaseUrl}/public/tts/preview`,
      { text: greeting, engineVoiceId: v.engine_voice_id },
      { responseType: 'blob' }
    ).subscribe({
      next: (blob) => {
        this.ngZone.run(() => {
          this.previewLoadingId = null;
          this.previewPlayingId = v.engine_voice_id;
          const url = URL.createObjectURL(blob as Blob);
          this._previewAudio = new Audio(url);
          this.cdr.markForCheck();
          this._previewAudio.play();
          this._previewAudio.onended = () => {
            this.ngZone.run(() => {
              this.previewPlayingId = null;
              URL.revokeObjectURL(url);
              this.cdr.markForCheck();
            });
          };
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.previewLoadingId = null;
          this.cdr.markForCheck();
        });
      }
    });
  }

  // ── Generate ──────────────────────────────────────────────────────────
  generate() {
    if (!this.text.trim() || !this.selectedVoice) return;
    this.generating = true;
    this.error = '';

    if (this.audioUrl && this.audioUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.audioUrl);
    }
    this.audioUrl = null;

    this.http.post(
      `${environment.apiBaseUrl}/tts/generate`,
      {
        text: this.text,
        voiceId: this.selectedVoice.engine_voice_id,
        engineVoiceId: this.selectedVoice.engine_voice_id,
        lang: this.selectedLang,
        speed: this.speed,
        totalSteps: this.selectedQuality.steps,
        projectId: this.selectedProjectId
      },
      { responseType: 'blob' }
    ).subscribe({
      next: (blob: Blob) => {
        this.ngZone.run(() => {
          this.audioUrl = URL.createObjectURL(blob);
          this.generating = false;
          this.cdr.markForCheck();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          if (err.status === 429) {
            this.error = 'Daily character limit reached. Resets tomorrow.';
          } else if (err.status === 503) {
            this.error = 'Voice engine is not running. Start tts-service/start-tts-service.sh first.';
          } else {
            this.error = 'Failed to generate audio. Please try again.';
          }
          this.generating = false;
          this.cdr.markForCheck();
        });
      }
    });
  }
}
