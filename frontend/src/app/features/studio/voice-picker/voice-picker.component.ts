import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Voice, VoiceTab } from '../models/studio.models';

@Component({
  selector: 'app-voice-picker',
  standalone: true,
  imports: [CommonModule, RouterLink],
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

  /** High-quality SVG avatars for Indian male & female voice personas (100% offline & reliable) */
  private readonly svgAvatars: Record<string, string> = {
    // Rohan (M1) - Calm Narrator
    'M1': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgM1" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#4F46E5"/><stop offset="100%" stop-color="#7C3AED"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgM1)"/>
      <circle cx="50" cy="42" r="20" fill="#D08B5B"/>
      <path d="M30 40 Q50 20 70 40 Q50 30 30 40" fill="#1E1B18"/>
      <circle cx="43" cy="42" r="2.5" fill="#1E1B18"/><circle cx="57" cy="42" r="2.5" fill="#1E1B18"/>
      <path d="M45 52 Q50 56 55 52" stroke="#1E1B18" stroke-width="2" fill="none"/>
      <path d="M22 88 Q50 68 78 88 V100 H22 Z" fill="#1E293B"/>
    </svg>`,
    // Aryan (M2) - Storyteller
    'M2': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgM2" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#059669"/><stop offset="100%" stop-color="#10B981"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgM2)"/>
      <circle cx="50" cy="42" r="20" fill="#AE5D29"/>
      <path d="M28 38 Q50 18 72 38 Q50 26 28 38" fill="#111827"/>
      <circle cx="43" cy="42" r="2.5" fill="#111827"/><circle cx="57" cy="42" r="2.5" fill="#111827"/>
      <path d="M44 51 Q50 57 56 51" stroke="#111827" stroke-width="2" fill="none"/>
      <path d="M20 90 Q50 65 80 90 V100 H20 Z" fill="#047857"/>
    </svg>`,
    // Kabir (M3) - Energetic YouTube Creator
    'M3': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgM3" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#D97706"/><stop offset="100%" stop-color="#F59E0B"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgM3)"/>
      <circle cx="50" cy="42" r="20" fill="#EDB98A"/>
      <path d="M30 36 Q50 15 70 36 Q50 25 30 36" fill="#1F2937"/>
      <circle cx="43" cy="41" r="2.5" fill="#1F2937"/><circle cx="57" cy="41" r="2.5" fill="#1F2937"/>
      <path d="M43 50 Q50 58 57 50" stroke="#1F2937" stroke-width="2" stroke-linecap="round" fill="none"/>
      <path d="M22 88 Q50 66 78 88 V100 H22 Z" fill="#B45309"/>
    </svg>`,
    // Dev (M4) - Corporate Announcer
    'M4': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgM4" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#2563EB"/><stop offset="100%" stop-color="#3B82F6"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgM4)"/>
      <circle cx="50" cy="42" r="20" fill="#D08B5B"/>
      <path d="M32 35 Q50 22 68 35" stroke="#1E293B" stroke-width="6" stroke-linecap="round" fill="none"/>
      <circle cx="43" cy="42" r="2.5" fill="#1E293B"/><circle cx="57" cy="42" r="2.5" fill="#1E293B"/>
      <path d="M45 52 H55" stroke="#1E293B" stroke-width="2" stroke-linecap="round"/>
      <path d="M20 90 Q50 68 80 90 V100 H20 Z" fill="#1E3A8A"/>
      <polygon points="50,72 45,90 55,90" fill="#EF4444"/>
    </svg>`,
    // Vihaan (M5) - Casual Hinglish Social
    'M5': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgM5" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#EC4899"/><stop offset="100%" stop-color="#F43F5E"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgM5)"/>
      <circle cx="50" cy="42" r="20" fill="#AE5D29"/>
      <path d="M28 36 C 35 15, 65 15, 72 36 C 60 28, 40 28, 28 36 Z" fill="#111827"/>
      <circle cx="43" cy="42" r="2.5" fill="#111827"/><circle cx="57" cy="42" r="2.5" fill="#111827"/>
      <path d="M43 51 Q50 57 57 51" stroke="#111827" stroke-width="2.5" fill="none"/>
      <path d="M22 88 Q50 68 78 88 V100 H22 Z" fill="#881337"/>
    </svg>`,

    // Isha (F1) - Warm Audiobook Female
    'F1': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgF1" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#8B5CF6"/><stop offset="100%" stop-color="#A855F7"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgF1)"/>
      <path d="M26 40 C24 65, 76 65, 74 40 C76 20, 24 20, 26 40 Z" fill="#1E1B18"/>
      <circle cx="50" cy="44" r="19" fill="#EDB98A"/>
      <path d="M30 36 Q50 24 70 36 C60 30, 40 30, 30 36 Z" fill="#1E1B18"/>
      <circle cx="50" cy="38" r="1.8" fill="#DC2626"/> <!-- Bindi -->
      <circle cx="43" cy="44" r="2.3" fill="#1E1B18"/><circle cx="57" cy="44" r="2.3" fill="#1E1B18"/>
      <path d="M44 53 Q50 58 56 53" stroke="#DC2626" stroke-width="2" fill="none"/>
      <path d="M22 90 Q50 70 78 90 V100 H22 Z" fill="#6B21A8"/>
    </svg>`,
    // Priya (F2) - Business Presenter
    'F2': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgF2" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#0284C7"/><stop offset="100%" stop-color="#38BDF8"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgF2)"/>
      <path d="M28 38 C24 70, 76 70, 72 38 C75 18, 25 18, 28 38 Z" fill="#0F172A"/>
      <circle cx="50" cy="44" r="19" fill="#D08B5B"/>
      <path d="M32 35 Q50 25 68 35 Z" fill="#0F172A"/>
      <circle cx="50" cy="38" r="1.5" fill="#E11D48"/>
      <circle cx="43" cy="44" r="2.3" fill="#0F172A"/><circle cx="57" cy="44" r="2.3" fill="#0F172A"/>
      <path d="M45 53 Q50 57 55 53" stroke="#E11D48" stroke-width="2" fill="none"/>
      <path d="M20 90 Q50 68 80 90 V100 H20 Z" fill="#0369A1"/>
    </svg>`,
    // Meera (F3) - Vibrant Creator
    'F3': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgF3" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#F43F5E"/><stop offset="100%" stop-color="#FB7185"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgF3)"/>
      <path d="M25 38 C22 75, 78 75, 75 38 C78 18, 22 18, 25 38 Z" fill="#18181B"/>
      <circle cx="50" cy="44" r="19" fill="#EDB98A"/>
      <path d="M30 34 C40 25, 60 25, 70 34 Z" fill="#18181B"/>
      <circle cx="50" cy="37" r="1.8" fill="#991B1B"/>
      <circle cx="43" cy="43" r="2.3" fill="#18181B"/><circle cx="57" cy="43" r="2.3" fill="#18181B"/>
      <path d="M43 52 Q50 58 57 52" stroke="#BE123C" stroke-width="2" stroke-linecap="round" fill="none"/>
      <path d="M22 90 Q50 68 78 90 V100 H22 Z" fill="#9F1239"/>
    </svg>`,
    // Neha (F4) - Social & Ads
    'F4': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgF4" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#10B981"/><stop offset="100%" stop-color="#34D399"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgF4)"/>
      <path d="M27 38 C23 72, 77 72, 73 38 Z" fill="#111827"/>
      <circle cx="50" cy="44" r="19" fill="#AE5D29"/>
      <path d="M30 35 Q50 24 70 35 Z" fill="#111827"/>
      <circle cx="50" cy="38" r="1.5" fill="#EF4444"/>
      <circle cx="43" cy="44" r="2.3" fill="#111827"/><circle cx="57" cy="44" r="2.3" fill="#111827"/>
      <path d="M44 53 Q50 58 56 53" stroke="#EF4444" stroke-width="2" fill="none"/>
      <path d="M22 90 Q50 70 78 90 V100 H22 Z" fill="#065F46"/>
    </svg>`,
    // Kavya (F5) - Meditation & Wellness
    'F5': `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
      <defs><linearGradient id="bgF5" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#14B8A6"/><stop offset="100%" stop-color="#2DD4BF"/></linearGradient></defs>
      <circle cx="50" cy="50" r="50" fill="url(#bgF5)"/>
      <path d="M26 38 C22 75, 78 75, 74 38 Z" fill="#1F2937"/>
      <circle cx="50" cy="44" r="19" fill="#EDB98A"/>
      <path d="M30 35 Q50 25 70 35 Z" fill="#1F2937"/>
      <circle cx="50" cy="38" r="1.8" fill="#B91C1C"/>
      <circle cx="43" cy="44" r="2.3" fill="#1F2937"/><circle cx="57" cy="44" r="2.3" fill="#1F2937"/>
      <path d="M44 53 Q50 57 56 53" stroke="#B91C1C" stroke-width="2" fill="none"/>
      <path d="M22 90 Q50 70 78 90 V100 H22 Z" fill="#0F766E"/>
    </svg>`
  };

  /** Gets inline Data URI for SVG avatar (100% offline & fast) */
  getAvatarUrl(voice: Voice): string {
    const rawSvg = this.svgAvatars[voice.engine_voice_id] || this.svgAvatars['M1'];
    return `data:image/svg+xml;utf8,${encodeURIComponent(rawSvg)}`;
  }
}
