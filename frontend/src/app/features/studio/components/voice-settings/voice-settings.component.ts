import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { StudioStateService } from '../../studio-state.service';
import { TooltipDirective } from '@shared/directives/tooltip.directive';

/**
 * VoiceSettingsComponent — compact controls bar (Speed, Language, Quality).
 * Child of StudioComponent. Reads/writes to StudioStateService.
 * A sibling to ScriptEditorComponent — both share the same service state.
 */
@Component({
  selector: 'app-voice-settings',
  standalone: true,
  imports: [FormsModule, TooltipDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="settings-compact">
      <div class="settings-compact-header">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3" />
          <path
            d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
        </svg>
        <span class="settings-title">Voice Settings</span>
      </div>

      <div class="settings-compact-row">
        <!-- Speed Dropdown -->
        <div class="compact-control speed-control">
          <label class="compact-label" [appTooltip]="'Speech playback speed multiplier'">Speed</label>
          <select class="compact-select" id="speed-select"
            [ngModel]="state.speed()"
            (ngModelChange)="state.selectSpeed($event)">
            @for (s of state.speedPresets; track s.value) {
              <option [ngValue]="s.value">{{ s.label }}</option>
            }
          </select>
        </div>

        <!-- Language Dropdown -->
        <div class="compact-control lang-control">
          <label class="compact-label" [appTooltip]="'Language mode for TTS engine'">Language</label>
          <select class="compact-select" id="lang-select"
            [ngModel]="state.selectedLang()"
            (ngModelChange)="state.selectLang($event)">
            @for (lang of state.langOptions; track lang.value) {
              <option [ngValue]="lang.value">{{ lang.label }}</option>
            }
          </select>
        </div>

        <!-- Quality Dropdown -->
        <div class="compact-control quality-control">
          <label class="compact-label" [appTooltip]="'Higher quality = better voice but longer generation'">Quality</label>
          <select class="compact-select quality-select" id="quality-select"
            [ngModel]="state.selectedQuality().steps"
            (ngModelChange)="state.selectQualityBySteps($event)">
            @for (q of state.qualityPresets; track q.steps) {
              <option [ngValue]="q.steps">{{ q.label }} — {{ q.description }}</option>
            }
          </select>
        </div>
      </div>
    </div>
  `,
})
export class VoiceSettingsComponent {
  readonly state = inject(StudioStateService);
}
