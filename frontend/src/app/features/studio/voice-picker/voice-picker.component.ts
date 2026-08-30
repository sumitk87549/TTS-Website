import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { Voice, VoiceTab } from '../models/studio.models';

@Component({
  selector: 'app-voice-picker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voice-picker.component.html',
  styleUrls: ['./voice-picker.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VoicePickerComponent {
  @Input() maleVoices: Voice[] = [];
  @Input() femaleVoices: Voice[] = [];
  @Input() selectedVoice: Voice | null = null;
  @Input() voiceUseCases: Record<string, string> = {};
  @Input() previewLoadingId: string | null = null;
  @Input() previewPlayingId: string | null = null;

  @Output() voiceSelected = new EventEmitter<Voice>();
  @Output() previewRequested = new EventEmitter<{ voice: Voice; event: MouseEvent }>();

  selectedTab: VoiceTab = 'Male';

  /** Cartoon avatar paths for each voice persona (local assets) */
  private readonly avatarUrls: Record<string, string> = {
    // ── MALE VOICES ──
    'M1': 'assets/avatars/rohan-m1.png',     // Rohan - Calm Narrator
    'M2': 'assets/avatars/aryan-m2.png',     // Aryan - Dynamic Storyteller
    'M3': 'assets/avatars/kabir-m3.png',     // Kabir - Steady YouTuber
    'M4': 'assets/avatars/dev-m4.png',       // Dev - Warm Corporate
    'M5': 'assets/avatars/vihaan-m5.png',    // Vihaan - High Energy Social

    // ── FEMALE VOICES ──
    'F1': 'assets/avatars/isha-f1.png',      // Isha - Warm Storytelling
    'F2': 'assets/avatars/meera-f2.png',     // Meera - Calm Business
    'F3': 'assets/avatars/priya-f3.png',     // Priya - Dynamic Creator
    'F4': 'assets/avatars/kavya-f4.png',     // Kavya - High Energy Ads
    'F5': 'assets/avatars/naina-f5.png'      // Naina - Steady Wellness
  };

  /** Gets cartoon avatar path for voice persona */
  getAvatarUrl(voice: Voice): string {
    return this.avatarUrls[voice.engine_voice_id] ||
      (voice.gender === 'female'
        ? 'assets/avatars/isha-f1.png'
        : 'assets/avatars/rohan-m1.png');
  }
}
