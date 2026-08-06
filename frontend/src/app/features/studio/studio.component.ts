import { Component, inject, OnInit, OnDestroy, NgZone, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MissionControlComponent } from './mission-control.component';
import { AnalyticsService } from '../../core/analytics/analytics.service';
import { ToastService } from '../../core/toast/toast.service';
import { VoicePickerComponent } from './voice-picker/voice-picker.component';
import { StudioApiService } from './services/studio-api.service';
import { StudioEstimatorService } from './services/studio-estimator.service';
import { GenerationState, ProjectSummary, QualityPreset, ScriptPreset, StudioLanguage, Voice } from './models/studio.models';

@Component({
  selector: 'app-studio',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MissionControlComponent, VoicePickerComponent],
  templateUrl: './studio.component.html',
  styleUrls: ['./studio.component.scss']
})
export class StudioComponent implements OnInit, OnDestroy {
  private studioApi = inject(StudioApiService);
  private estimator = inject(StudioEstimatorService);
  private ngZone = inject(NgZone);
  private cdr = inject(ChangeDetectorRef);
  private analytics = inject(AnalyticsService);
  private toast = inject(ToastService);

  // ── Audio element ref ─────────────────────────────────────────────────
  @ViewChild('audioPlayerRef') audioPlayerRef!: ElementRef<HTMLAudioElement>;

  // ── Text ────────────────────────────────────────────────────────────
  text = '';
  maxChars = 1000;

  // ── Voices ──────────────────────────────────────────────────────────
  voices: Voice[] = [];
  maleVoices: Voice[] = []
  femaleVoices: Voice[] = [];
  selectedTab: 'Male' | 'Female' | 'My Voices' = 'Male';
  selectedVoice: Voice | null = null;

  // ── Voice use-case tags ───────────────────────────────────────────────
  readonly voiceUseCases: Record<string, string> = {
    'M1': 'Best for: Narration, News',
    'M2': 'Best for: Storytelling, Ads',
    'M3': 'Best for: YouTube, Reels',
    'M4': 'Best for: Announcements, Corporate',
    'M5': 'Best for: Casual, Social Media',
    'F1': 'Best for: Storytelling, Audiobooks',
    'F2': 'Best for: Presentations, Business',
    'F3': 'Best for: YouTube, Reels',
    'F4': 'Best for: Social Media, Ads',
    'F5': 'Best for: Meditation, Wellness',
  };

  // ── TTS Controls ────────────────────────────────────────────────────
  readonly langOptions = [
    { value: 'na',  label: '🌐 Auto (Hinglish)' },
    { value: 'hi',  label: '🇮🇳 Hindi' },
    { value: 'en',  label: '🇬🇧 English' },
  ];
  selectedLang: StudioLanguage = 'na';

  readonly speedPresets = [
    { value: 0.75, label: '0.75×' },
    { value: 0.9,  label: '0.9×'  },
    { value: 1.0,  label: '1.0×'  },
    { value: 1.1,  label: '1.1×'  },
    { value: 1.25, label: '1.25×' },
    { value: 1.5,  label: '1.5×'  },
  ];
  speed = 1.0;

  readonly qualityPresets: QualityPreset[] = [
    { steps: 4,  label: 'Draft',    description: 'Fast preview' },
    { steps: 8,  label: 'Standard', description: 'Balanced quality' },
    { steps: 16, label: 'High',     description: 'Rich voice quality' },
    { steps: 32, label: 'Ultra',    description: 'Studio-grade' },
  ];
  // ★ Standard is the default (not Ultra — Ultra is too slow for first impression)
  selectedQuality: QualityPreset = this.qualityPresets[1];

  // ── Generation state ─────────────────────────────────────────────────
  generating = false;
  error = '';
  audioUrl: string | null = null;
  generationState: GenerationState = 'idle';
  generationStartTime = 0;
  generationElapsedMs = 0;
  private _elapsedTimer: ReturnType<typeof setInterval> | null = null;

  // ── Voice Preview state ──────────────────────────────────────────────
  previewLoadingId: string | null = null;
  previewPlayingId: string | null = null;
  private _previewAudio: HTMLAudioElement | null = null;

  // ── Projects ─────────────────────────────────────────────────────────
  projects: ProjectSummary[] = [];
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

  readonly scriptPresets: ScriptPreset[] = [
    { label: 'Story / Narration', text: 'एक समय की बात है, एक छोटे से गाँव में एक होनहार लड़की रहती थी। उसका नाम था आनंदी।' },
    { label: 'Promotional',       text: 'क्या आप अपने व्यापार को नई ऊँचाइयों पर ले जाना चाहते हैं? आज ही हमसे जुड़ें और अपने सपनों को साकार करें!' },
    { label: 'Greeting',          text: 'नमस्ते! आपका स्वागत है। आपका दिन शुभ और मंगलमय हो।' },
    { label: 'News Bulletin',     text: 'आज की ताज़ा ख़बरें: देश के विभिन्न हिस्सों में मानसून की अच्छी बारिश दर्ज की गई है।' },
    { label: 'Hinglish Casual',   text: "Yaar, aaj ka din bahut amazing raha! Maine socha tha ki kuch naya try karein, toh let's go!" },
    { label: 'English Business',  text: 'Good morning! Our quarterly results show a 28% growth in revenue. Let us walk through the key highlights.' },
  ];

  // ── Computed getters ──────────────────────────────────────────────────

  get charsPercent() { return Math.round((this.text.length / this.maxChars) * 100); }

  get estimatedSpokenDuration(): string {
    if (!this.text.trim()) return '';
    return this.estimator.spokenDuration(this.text, this.selectedLang, this.speed);
  }

  get estimatedGenerationTime(): string {
    if (!this.text.trim()) return '';
    return this.estimator.generationTime(this.text, this.selectedQuality);
  }

  get estimatedGenerationSeconds(): number {
    return this.estimator.generationSeconds(this.text, this.selectedQuality);
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────
  ngOnInit() {
    this.fetchVoices();
    this.fetchProjects();

    // Restore draft
    const savedDraft = localStorage.getItem('w2v-draft-text');
    if (savedDraft) this.text = savedDraft;

    this._placeholderTimer = setInterval(() => {
      if (!this.text) {
        this._placeholderIdx = (this._placeholderIdx + 1) % this.placeholderExamples.length;
        this.currentPlaceholder = this.placeholderExamples[this._placeholderIdx];
      }
    }, 4000);
  }

  ngOnDestroy() {
    if (this._placeholderTimer) clearInterval(this._placeholderTimer);
    if (this._elapsedTimer) clearInterval(this._elapsedTimer);
    if (this._previewAudio) {
      this._previewAudio.pause();
      this._previewAudio = null;
    }
  }

  dismissFirstRun() {
    this.showFirstRun = false;
    localStorage.setItem('w2v-seen-intro', '1');
  }

  // ── Draft autosave ────────────────────────────────────────────────────
  saveDraft() {
    if (this.text.trim()) {
      localStorage.setItem('w2v-draft-text', this.text);
    } else {
      localStorage.removeItem('w2v-draft-text');
    }
  }

  // ── Utility buttons ───────────────────────────────────────────────────
  clearText() {
    this.text = '';
    localStorage.removeItem('w2v-draft-text');
    this.cdr.detectChanges();
  }

  async pasteFromClipboard() {
    try {
      const clipText = await navigator.clipboard.readText();
      if (clipText) {
        this.text = clipText.substring(0, this.maxChars);
        this.saveDraft();
        this.cdr.detectChanges();
      }
    } catch {
      // Clipboard permission denied — ignore silently
    }
  }

  // ── Voice fetching with cache ─────────────────────────────────────────
  fetchVoices() {
    this.studioApi.getVoices().subscribe({
      next: voices => this.applyVoices(voices),
      error: () => {}
    });
  }

  private applyVoices(voices: Voice[]) {
    this.ngZone.run(() => {
      this.voices = voices;
      this.maleVoices = voices.filter(v => v.gender === 'male');
      this.femaleVoices = voices.filter(v => v.gender === 'female');
      if (!this.selectedVoice && this.maleVoices.length > 0) {
        this.selectVoice(this.maleVoices[0]);
      }
      this.cdr.detectChanges();
    });
  }

  fetchProjects() {
    this.studioApi.getProjects().subscribe({
      next: res => {
        this.ngZone.run(() => {
          this.projects = res;
          this.cdr.detectChanges();
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
    this.saveDraft();
  }

  tryRandomPreset() {
    const idx = Math.floor(Math.random() * this.scriptPresets.length);
    this.applyPreset(this.scriptPresets[idx]);
  }

  selectQuality(q: typeof this.qualityPresets[0]) { this.selectedQuality = q; }
  selectSpeed(s: number) { this.speed = s; }
  selectLang(l: StudioLanguage) { this.selectedLang = l; }

  // ── Voice Preview ──────────────────────────────────────────────────────
  playVoicePreview(v: Voice, event: MouseEvent) {
    event.stopPropagation();

    if (this._previewAudio) {
      this._previewAudio.pause();
      this._previewAudio = null;
    }

    if (this.previewPlayingId === v.engine_voice_id) {
      this.previewPlayingId = null;
      return;
    }

    this.previewLoadingId = v.engine_voice_id;
    this.previewPlayingId = null;
    this.analytics.track('voice_previewed', { voiceId: v.engine_voice_id });

    this.studioApi.previewVoice(v.engine_voice_id).subscribe({
      next: (blob) => {
        this.ngZone.run(() => {
          this.previewLoadingId = null;
          this.previewPlayingId = v.engine_voice_id;
          const url = URL.createObjectURL(blob as Blob);
          this._previewAudio = new Audio(url);
          this.cdr.detectChanges();
          this._previewAudio.play();
          this._previewAudio.onended = () => {
            this.ngZone.run(() => {
              this.previewPlayingId = null;
              URL.revokeObjectURL(url);
              this.cdr.detectChanges();
            });
          };
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.previewLoadingId = null;
          this.cdr.detectChanges();
        });
      }
    });
  }

  // ── Generate ──────────────────────────────────────────────────────────
  generate() {
    if (!this.text.trim() || !this.selectedVoice) return;

    // Track analytics
    this.analytics.track('generate_clicked', {
      charCount: this.text.length,
      voice: this.selectedVoice?.display_name,
      voiceId: this.selectedVoice?.engine_voice_id,
      quality: this.selectedQuality.label,
      speed: this.speed,
      lang: this.selectedLang,
    });

    // Reset state
    this.generationState = 'processing';
    this.generating = true;
    this.error = '';
    this.generationStartTime = Date.now();
    this.generationElapsedMs = 0;

    // Start elapsed timer
    this._elapsedTimer = setInterval(() => {
      this.generationElapsedMs = Date.now() - this.generationStartTime;
      this.cdr.detectChanges();
    }, 500);

    // Revoke old blob
    if (this.audioUrl && this.audioUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.audioUrl);
    }
    this.audioUrl = null;
    this.cdr.detectChanges(); // Force clear old audio

    this.studioApi.generateAudio({
        text: this.text,
        voiceId: this.selectedVoice.engine_voice_id,
        engineVoiceId: this.selectedVoice.engine_voice_id,
        lang: this.selectedLang,
        speed: this.speed,
        totalSteps: this.selectedQuality.steps,
        projectId: this.selectedProjectId
    }).subscribe({
      next: (blob: Blob) => {
        this.ngZone.run(() => {
          // Stop elapsed timer
          if (this._elapsedTimer) { clearInterval(this._elapsedTimer); this._elapsedTimer = null; }

          this.audioUrl = URL.createObjectURL(blob);
          this.generating = false;
          this.generationState = 'ready';

          // Track success
          this.analytics.track('generate_success', {
            charCount: this.text.length,
            voice: this.selectedVoice?.display_name,
            elapsedMs: this.generationElapsedMs,
          });

          // Clear draft after successful generation
          localStorage.removeItem('w2v-draft-text');

          // CRITICAL: Use detectChanges() not markForCheck() for zoneless
          this.cdr.detectChanges();

          // Force the audio element to load the new source
          queueMicrotask(() => {
            const audioEl = this.audioPlayerRef?.nativeElement;
            if (audioEl) {
              audioEl.load();
              audioEl.play().catch(() => { /* autoplay blocked, fine */ });
            }
            this.cdr.detectChanges();
          });

          this.toast.success('Audio ready — click play to listen!');
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          if (this._elapsedTimer) { clearInterval(this._elapsedTimer); this._elapsedTimer = null; }
          this.generationState = 'failed';

          this.analytics.track('generate_failed', {
            errorStatus: err.status,
            charCount: this.text.length,
          });

          if (err.status === 429) {
            this.error = "You've reached today's free limit (5,000 characters). Come back tomorrow — your limit resets at midnight.";
          } else if (err.status === 503) {
            this.error = 'Our voice engine is warming up. Please try again in about 30 seconds.';
          } else if (err.status === 413) {
            this.error = 'Your script is too long for one generation. Try splitting it into smaller parts.';
          } else if (err.status === 0 || err.status >= 500) {
            this.error = 'Something went wrong on our end. Please try again in a moment.';
          } else {
            this.error = 'Generation failed. Please check your text and try again.';
          }
          this.generating = false;
          this.cdr.detectChanges(); // CRITICAL: detectChanges not markForCheck
        });
      }
    });
  }

  trackDownload() {
    this.analytics.track('audio_downloaded', { charCount: this.text.length });
    this.toast.info('Download started');
  }
}
