import { Pipe, PipeTransform, inject } from '@angular/core';
import { StudioEstimatorService } from '../../features/studio/services/studio-estimator.service';
import { StudioLanguage } from '../../features/studio/models/studio.models';

/**
 * Pure Pipe: spokenDuration
 * Estimates spoken duration for a given text, language, and speed.
 * Delegates to StudioEstimatorService.
 *
 * Usage: {{ text | spokenDuration:selectedLang:speed }}
 * Example output: "~2m 30s"
 */
@Pipe({
  name: 'spokenDuration',
  pure: true,
  standalone: true,
})
export class SpokenDurationPipe implements PipeTransform {
  private estimator = inject(StudioEstimatorService);

  transform(text: string, lang: StudioLanguage = 'na', speed: number = 1.0): string {
    if (!text?.trim()) return '';
    return this.estimator.spokenDuration(text, lang, speed);
  }
}
