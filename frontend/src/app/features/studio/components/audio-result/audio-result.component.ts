import {
  Component,
  inject,
  ChangeDetectionStrategy,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { StudioStateService } from '../../studio-state.service';
import { MissionControlComponent } from '../../mission-control.component';
import { TimeFormatPipe } from '@shared/pipes/time-format.pipe';
import { TooltipDirective } from '@shared/directives/tooltip.directive';

/**
 * AudioResultComponent — displays the Mission Control loading panel and audio result card.
 * Child of StudioComponent. OnPush — all state from StudioStateService Signals.
 * Sibling to ScriptEditor and VoiceSettings — they all share StudioStateService.
 */
@Component({
  selector: 'app-audio-result',
  standalone: true,
  imports: [MissionControlComponent, TimeFormatPipe, TooltipDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="result-section" [class.visible]="state.error() || state.audioUrl() || state.generating()">

      <!-- Error -->
      @if (state.error()) {
        <div class="error-msg">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
          <div class="error-content">
            <span class="error-text">{{ state.error() }}</span>
            @if (state.generationState() === 'failed') {
              <button class="retry-btn" (click)="state.generate()">Try Again</button>
            }
          </div>
        </div>
      }

      <!-- Mission Control loading panel -->
      @if (state.generating()) {
        <app-mission-control
          [charCount]="state.text().length"
          [voiceName]="state.selectedVoice()?.display_name || ''"
          [language]="state.selectedLangLabel()"
          [quality]="state.selectedQuality().label"
          [speed]="state.speed()"
          [elapsedMs]="state.generationElapsedMs()"
          [estimatedSeconds]="state.estimatedGenerationSeconds()">
        </app-mission-control>
      }

      <!-- Audio result card -->
      @if (state.audioUrl()) {
        <div class="audio-result-card">
          <div class="audio-result-header">
            <div class="audio-ready-pill">
              <span class="audio-ready-dot"></span>
              Audio Ready
            </div>
            @if (state.generationElapsedMs() > 0) {
              <span class="generation-time">
                Generated in {{ state.generationElapsedMs() | timeFormat }}
              </span>
            }
          </div>

          <div class="audio-result-meta">
            <span class="meta-chip">{{ state.selectedVoice()?.display_name }}</span>
            <span class="meta-chip">{{ state.selectedLangLabel() }}</span>
            <span class="meta-chip">{{ state.speed() }}×</span>
            <span class="meta-chip">{{ state.selectedQuality().label }}</span>
            <span class="meta-chip">{{ state.text().length }} chars</span>
          </div>

          <div class="audio-row">
            <audio #audioPlayerRef [src]="state.audioUrl()" controls autoplay id="tts-audio-player"></audio>
          </div>

          <div class="audio-actions">
            <a [href]="state.audioUrl()" download="words2voice_audio.wav" class="btn-download"
              id="btn-download-audio"
              [appTooltip]="'Download your generated audio'"
              (click)="state.trackDownload()">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              Download WAV
            </a>
          </div>
        </div>
      }
    </div>
  `,
})
export class AudioResultComponent {
  readonly state = inject(StudioStateService);

  @ViewChild('audioPlayerRef') audioPlayerRef?: ElementRef<HTMLAudioElement>;
}
