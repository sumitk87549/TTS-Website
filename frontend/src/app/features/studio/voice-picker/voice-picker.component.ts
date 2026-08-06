import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Voice, VoiceTab } from '../models/studio.models';

@Component({
  selector: 'app-voice-picker',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './voice-picker.component.html',
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
}
