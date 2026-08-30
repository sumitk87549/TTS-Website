import { Component, inject, OnInit, OnDestroy, ChangeDetectionStrategy, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { StudioStateService } from './studio-state.service';
import { ScriptEditorComponent } from './components/script-editor/script-editor.component';
import { VoiceSettingsComponent } from './components/voice-settings/voice-settings.component';
import { AudioResultComponent } from './components/audio-result/audio-result.component';
import { VoicePickerComponent } from './voice-picker/voice-picker.component';
import { TooltipDirective } from '@shared/directives/tooltip.directive';

/**
 * StudioComponent — thin orchestrator (parent).
 *
 * Child components and their communication pattern:
 *   - ScriptEditorComponent  ┐
 *   - VoiceSettingsComponent ├── All read/write via StudioStateService (sibling via service)
 *   - VoicePickerComponent   │
 *   - AudioResultComponent   ┘
 *
 * Parent only handles: layout, top-bar UI, generate button, and footer.
 * All state lives in StudioStateService (Signals).
 */
@Component({
  selector: 'app-studio',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    ScriptEditorComponent,
    VoiceSettingsComponent,
    AudioResultComponent,
    VoicePickerComponent,
    TooltipDirective,
  ],
  templateUrl: './studio.component.html',
  styleUrls: ['./studio.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class StudioComponent implements OnInit, OnDestroy {
  readonly state = inject(StudioStateService);

  /** First-run dismissal state (local-only, no need for a service) */
  showFirstRun = !localStorage.getItem('w2v-seen-intro');

  ngOnInit(): void {
    this.state.fetchVoices();
    this.state.fetchProjects();
  }

  ngOnDestroy(): void {
    this.state.cleanup();
  }

  dismissFirstRun(): void {
    this.showFirstRun = false;
    localStorage.setItem('w2v-seen-intro', '1');
  }

  applyRandomPreset(): void {
    const idx = Math.floor(Math.random() * this.state.scriptPresets.length);
    this.state.applyPreset(this.state.scriptPresets[idx]);
  }
}
