import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

interface Voice {
  id: number;
  engine_voice_id: string;
  display_name: string;
  gender: string;
  style_tag: string;
}

interface UsageStat {
  charactersUsed: number;
  charactersLimit: number;
}

@Component({
  selector: 'app-studio',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './studio.component.html',
  styleUrls: ['./studio.component.scss']
})
export class StudioComponent implements OnInit {
  private http = inject(HttpClient);
  private sanitizer = inject(DomSanitizer);

  // ── Text ────────────────────────────────────────────────────────────
  text = '';
  maxChars = 1000;
  usage: UsageStat = { charactersUsed: 0, charactersLimit: 5000 };

  // ── Voices ──────────────────────────────────────────────────────────
  voices: Voice[] = [];
  maleVoices: Voice[] = [];
  femaleVoices: Voice[] = [];
  selectedTab: 'Male' | 'Female' | 'My Voices' = 'Male';
  selectedVoice: Voice | null = null;

  // ── TTS Controls ────────────────────────────────────────────────────
  /** Language presets */
  readonly langOptions = [
    { value: 'na',  label: '🌐 Auto (Hinglish)' },
    { value: 'hi',  label: '🇮🇳 Hindi' },
    { value: 'en',  label: '🇬🇧 English' },
  ];
  selectedLang = 'na';

  /** Speed presets */
  readonly speedPresets = [
    { value: 0.75, label: '0.75×' },
    { value: 0.9,  label: '0.9×'  },
    { value: 1.0,  label: '1.0×'  },
    { value: 1.1,  label: '1.1×'  },
    { value: 1.25, label: '1.25×' },
    { value: 1.5,  label: '1.5×'  },
  ];
  speed = 1.0;

  /** Quality presets */
  readonly qualityPresets = [
    { steps: 4,  label: 'Draft',    description: 'Fast preview' },
    { steps: 8,  label: 'Standard', description: 'Balanced quality' },
    { steps: 16, label: 'High',     description: 'Rich voice quality' },
    { steps: 32, label: 'Ultra',    description: 'Studio-grade (slow)' },
  ];
  selectedQuality = this.qualityPresets[1]; // Standard

  // ── Generation state ────────────────────────────────────────────────
  generating = false;
  error = '';
  audioUrl: string | null = null;
  audioDuration: number | null = null;
  generationTime: number | null = null;

  // ── Projects ────────────────────────────────────────────────────────
  projects: any[] = [];
  selectedProjectId: number | null = null;

  // ── Script presets ──────────────────────────────────────────────────
  readonly scriptPresets = [
    { label: 'Story / Narration', text: 'एक समय की बात है, एक छोटे से गाँव में एक होनहार लड़की रहती थी। उसका नाम था आनंदी।' },
    { label: 'Promotional',       text: 'क्या आप अपने व्यापार को नई ऊँचाइयों पर ले जाना चाहते हैं? आज ही हमसे जुड़ें और अपने सपनों को साकार करें!' },
    { label: 'Greeting',          text: 'नमस्ते! आपका स्वागत है। आपका दिन शुभ और मंगलमय हो।' },
    { label: 'News Bulletin',     text: 'आज की ताज़ा ख़बरें: देश के विभिन्न हिस्सों में मानसून की अच्छी बारिश दर्ज की गई है।' },
    { label: 'Hinglish Casual',   text: 'Yaar, aaj ka din bahut amazing raha! Maine socha tha ki kuch naya try karein, toh let\'s go!' },
    { label: 'English Business',  text: 'Good morning! Our quarterly results show a 28% growth in revenue. Let us walk through the key highlights.' },
  ];

  // ── Lifecycle ────────────────────────────────────────────────────────
  ngOnInit() {
    this.fetchUsage();
    this.fetchVoices();
    this.fetchProjects();
  }

  fetchUsage() {
    this.http.get<any>(`${environment.apiBaseUrl}/usage/today`).subscribe({
      next: res => this.usage = res,
      error: () => {}
    });
  }

  fetchVoices() {
    this.http.get<Voice[]>(`${environment.apiBaseUrl}/voices`).subscribe({
      next: res => {
        this.voices = res;
        this.maleVoices   = res.filter(v => v.gender === 'male');
        this.femaleVoices = res.filter(v => v.gender === 'female');
        if (this.maleVoices.length > 0) this.selectVoice(this.maleVoices[0]);
      },
      error: () => {}
    });
  }

  fetchProjects() {
    this.http.get<any[]>(`${environment.apiBaseUrl}/projects`).subscribe({
      next: res => this.projects = res,
      error: () => {}
    });
  }

  // ── Voice selection ──────────────────────────────────────────────────
  selectVoice(v: Voice) {
    this.selectedVoice = v;
  }

  // ── Script preset ────────────────────────────────────────────────────
  applyPreset(preset: { label: string; text: string }) {
    this.text = preset.text;
    // Auto-detect language hint
    const hasHindi = /[\u0900-\u097F]/.test(preset.text);
    const hasLatin = /[a-zA-Z]/.test(preset.text);
    if (hasHindi && hasLatin) this.selectedLang = 'na';
    else if (hasHindi)        this.selectedLang = 'hi';
    else                      this.selectedLang = 'en';
  }

  // ── Controls helpers ─────────────────────────────────────────────────
  get charsLeft() { return this.usage.charactersLimit - this.usage.charactersUsed; }
  get charsPercent() { return Math.round((this.text.length / this.maxChars) * 100); }
  get usagePercent() { return Math.round((this.usage.charactersUsed / this.usage.charactersLimit) * 100); }

  selectQuality(q: typeof this.qualityPresets[0]) { this.selectedQuality = q; }
  selectSpeed(s: number) { this.speed = s; }
  selectLang(l: string) { this.selectedLang = l; }

  // ── Generate ─────────────────────────────────────────────────────────
  generate() {
    if (!this.text.trim() || !this.selectedVoice) return;
    this.generating = true;
    this.error = '';

    // Revoke any previous Object URL to free memory
    if (this.audioUrl && this.audioUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.audioUrl);
    }
    this.audioUrl = null;
    this.audioDuration = null;
    this.generationTime = null;

    // responseType: 'blob' — Spring returns audio/wav bytes directly.
    // This completely avoids the Jackson JSON serialisation issue in
    // spring-boot-starter-webmvc where Map.of() responses never complete.
    this.http.post(
      `${environment.apiBaseUrl}/tts/generate`,
      {
        text: this.text,
        voiceId: this.selectedVoice.engine_voice_id,
        engineVoiceId: this.selectedVoice.engine_voice_id, // legacy compat
        lang: this.selectedLang,
        speed: this.speed,
        totalSteps: this.selectedQuality.steps,
        projectId: this.selectedProjectId
      },
      { responseType: 'blob' }
    ).subscribe({
      next: (blob: Blob) => {
        this.audioUrl = URL.createObjectURL(blob);
        this.generating = false;
        this.fetchUsage();
      },
      error: (err) => {
        if (err.status === 429) {
          this.error = 'Daily character limit reached. Resets tomorrow.';
        } else if (err.status === 503) {
          this.error = 'Voice engine is not running. Start tts-service/start-tts-service.sh first.';
        } else {
          this.error = 'Failed to generate audio. Please try again.';
        }
        this.generating = false;
      }
    });
  }

  downloadUrl(): string | null {
    return this.audioUrl;
  }
}
