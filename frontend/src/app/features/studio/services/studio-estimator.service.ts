import { Injectable } from '@angular/core';
import { QualityPreset, StudioLanguage } from '../models/studio.models';

@Injectable({ providedIn: 'root' })
export class StudioEstimatorService {
  spokenDuration(text: string, lang: StudioLanguage, speed: number): string {
    if (!text.trim()) return '';
    const charRate = lang === 'en' ? 16 : 14;
    const seconds = Math.ceil(text.length / charRate / speed);
    return seconds < 60 ? `~${seconds}s` : `~${Math.floor(seconds / 60)}m ${seconds % 60}s`;
  }

  generationSeconds(text: string, quality: QualityPreset): number {
    const factors: Record<QualityPreset['label'], number> = { Draft: 0.55, Standard: 1.0, High: 1.8, Ultra: 3.5 };
    return Math.ceil(4 + (text.length * 0.03 * factors[quality.label]));
  }

  generationTime(text: string, quality: QualityPreset): string {
    return text.trim() ? `~${this.generationSeconds(text, quality)}s` : '';
  }
}
