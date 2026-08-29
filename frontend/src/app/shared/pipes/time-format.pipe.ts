import { Pipe, PipeTransform } from '@angular/core';

/**
 * Pure Pipe: timeFormat
 * Formats milliseconds into a human-readable time string.
 * Examples:
 *   0        → "0s"
 *   45000    → "45s"
 *   90000    → "1:30"
 *   3661000  → "61:01"
 *
 * Usage: {{ elapsedMs | timeFormat }}
 */
@Pipe({
  name: 'timeFormat',
  pure: true,
  standalone: true,
})
export class TimeFormatPipe implements PipeTransform {
  transform(ms: number): string {
    if (!ms || ms < 0) return '0s';
    const totalSec = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSec / 60);
    const seconds = totalSec % 60;
    return minutes > 0
      ? `${minutes}:${seconds.toString().padStart(2, '0')}`
      : `${seconds}s`;
  }
}
