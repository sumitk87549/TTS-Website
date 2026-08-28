import { Component, Input, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-mission-control',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mc-panel">
      <!-- Top Status Bar -->
      <div class="mc-header">
        <div class="mc-status-badge" [class.processing]="!isComplete">
          <span class="mc-dot"></span>
          {{ isComplete ? 'Complete' : 'Processing' }}
        </div>
        <div class="mc-timer">
          {{ formatTime(elapsedMs) }}
        </div>
      </div>

      <!-- ETA Bar -->
      <div class="mc-eta-section">
        <div class="mc-eta-text">
          <span>Estimated: {{ estimatedSeconds }}s</span>
          <span class="mc-eta-percent">{{ progressPercent }}%</span>
        </div>
        <div class="mc-progress-track">
          <div class="mc-progress-fill" [style.width.%]="progressPercent"></div>
        </div>
      </div>

      <!-- Animated Stages -->
      <div class="mc-stages">
        <div class="mc-stage" *ngFor="let stage of stages; let i = index"
             [class.done]="currentStageIndex > i"
             [class.active]="currentStageIndex === i"
             [class.pending]="currentStageIndex < i">
          <div class="mc-stage-icon">
            <span *ngIf="currentStageIndex > i">✓</span>
            <span *ngIf="currentStageIndex === i" class="mc-stage-spinner"></span>
            <span *ngIf="currentStageIndex < i" class="mc-stage-num">{{ i + 1 }}</span>
          </div>
          <span class="mc-stage-label">{{ stage }}</span>
        </div>
      </div>

      <!-- Text Stats Grid -->
      <div class="mc-stats-grid">
        <div class="mc-stat">
          <span class="mc-stat-value">{{ charCount | number }}</span>
          <span class="mc-stat-label">Characters</span>
        </div>
        <div class="mc-stat">
          <span class="mc-stat-value">{{ voiceName }}</span>
          <span class="mc-stat-label">Voice</span>
        </div>
        <div class="mc-stat">
          <span class="mc-stat-value">{{ quality }}</span>
          <span class="mc-stat-label">Quality</span>
        </div>
        <div class="mc-stat">
          <span class="mc-stat-value">{{ speed }}×</span>
          <span class="mc-stat-label">Speed</span>
        </div>
      </div>

      <!-- Animated Waveform -->
      <div class="mc-waveform">
        <span *ngFor="let bar of waveformBars" class="mc-wave-bar"></span>
      </div>

      <!-- Rotating Microcopy -->
      <div class="mc-microcopy">{{ currentTip }}</div>

      <!-- Confidence message -->
      <div class="mc-confidence">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
        Do not refresh — your audio will appear automatically
      </div>
    </div>
  `,
  styles: [`
    .mc-panel {
      background: var(--bg-elevated);
      border: 2px solid var(--accent-glow);
      border-radius: 1rem;
      padding: 1.25rem 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      animation: mc-slide-in 0.4s ease-out;
      box-shadow: 0 4px 24px rgba(124, 92, 247, 0.12), 0 1px 4px rgba(0,0,0,0.08);
    }

    /* Day mode: fully solid, clearly an integrated component */
    :host-context([data-theme="day"]) .mc-panel {
      background: #f5f4ff;
      border: 2px solid var(--accent);
      box-shadow: 0 4px 20px rgba(99, 65, 220, 0.18), 0 1px 4px rgba(0,0,0,0.06);
    }

    :host-context([data-theme="day"]) .mc-timer {
      color: #3a2d8a;
    }

    :host-context([data-theme="day"]) .mc-stat {
      background: #ffffff;
      border-color: rgba(99, 65, 220, 0.25);
    }

    @keyframes mc-slide-in {
      from { opacity: 0; transform: translateY(16px) scale(0.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }

    /* Header */
    .mc-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .mc-status-badge {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0.9rem;
      background: var(--success-subtle);
      color: var(--success);
      border-radius: 2rem;
      font-size: 0.78rem;
      font-weight: 700;
      letter-spacing: 0.02em;
    }

    .mc-status-badge.processing {
      background: var(--accent-subtle);
      color: var(--accent);
    }

    .mc-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: currentColor;
      animation: mc-pulse 1.2s ease-in-out infinite;
    }

    @keyframes mc-pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(0.8); }
    }

    .mc-timer {
      font-family: 'JetBrains Mono', monospace;
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--text-primary);
      letter-spacing: 0.05em;
    }

    /* ETA */
    .mc-eta-section {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .mc-eta-text {
      display: flex;
      justify-content: space-between;
      font-size: 0.78rem;
      color: var(--text-muted);
    }

    .mc-eta-percent {
      font-family: 'JetBrains Mono', monospace;
      color: var(--accent);
      font-weight: 600;
    }

    .mc-progress-track {
      height: 6px;
      background: var(--bg-elevated);
      border-radius: 3px;
      overflow: hidden;
    }

    .mc-progress-fill {
      height: 100%;
      background: var(--gradient-violet);
      border-radius: 3px;
      transition: width 0.5s ease;
      position: relative;
    }

    .mc-progress-fill::after {
      content: '';
      position: absolute;
      top: 0; right: 0; bottom: 0;
      width: 40px;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
      animation: mc-shimmer 1.5s ease-in-out infinite;
    }

    @keyframes mc-shimmer {
      0% { transform: translateX(-40px); }
      100% { transform: translateX(40px); }
    }

    /* Stages */
    .mc-stages {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .mc-stage {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      padding: 0.4rem 0.6rem;
      border-radius: 0.5rem;
      transition: all 0.3s;
    }

    .mc-stage.active {
      background: var(--accent-subtle);
    }

    .mc-stage.active .mc-stage-label { color: var(--accent); font-weight: 600; }
    .mc-stage.done .mc-stage-label { color: var(--text-muted); text-decoration: line-through; }
    .mc-stage.pending .mc-stage-label { color: var(--text-muted); opacity: 0.5; }

    .mc-stage-icon {
      width: 22px; height: 22px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 0.65rem; font-weight: 700;
    }

    .mc-stage.done .mc-stage-icon {
      background: var(--success-subtle);
      color: var(--success);
    }

    .mc-stage.active .mc-stage-icon {
      background: var(--accent-subtle);
      color: var(--accent);
    }

    .mc-stage.pending .mc-stage-icon {
      background: var(--bg-elevated);
      color: var(--text-muted);
    }

    .mc-stage-spinner {
      width: 12px; height: 12px;
      border: 2px solid var(--accent-subtle);
      border-top-color: var(--accent);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      display: block;
    }

    .mc-stage-num { font-size: 0.6rem; }

    .mc-stage-label {
      font-size: 0.8rem;
      color: var(--text-secondary);
      transition: all 0.3s;
    }

    /* Stats Grid */
    .mc-stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0.5rem;
    }

    .mc-stat {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 0.6rem 0.4rem;
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      border-radius: 0.5rem;
      text-align: center;
    }

    .mc-stat-value {
      font-size: 0.82rem;
      font-weight: 700;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100%;
    }

    .mc-stat-label {
      font-size: 0.6rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-top: 0.15rem;
    }

    /* Waveform */
    .mc-waveform {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 3px;
      height: 32px;
      padding: 0.5rem 0;
    }

    .mc-wave-bar {
      width: 3px;
      background: var(--accent);
      border-radius: 2px;
      opacity: 0.6;
    }

    .mc-wave-bar:nth-child(1)  { animation: mc-wave 0.8s 0.05s ease-in-out infinite; height: 8px; }
    .mc-wave-bar:nth-child(2)  { animation: mc-wave 0.8s 0.10s ease-in-out infinite; height: 12px; }
    .mc-wave-bar:nth-child(3)  { animation: mc-wave 0.8s 0.15s ease-in-out infinite; height: 16px; }
    .mc-wave-bar:nth-child(4)  { animation: mc-wave 0.8s 0.20s ease-in-out infinite; height: 20px; }
    .mc-wave-bar:nth-child(5)  { animation: mc-wave 0.8s 0.25s ease-in-out infinite; height: 24px; }
    .mc-wave-bar:nth-child(6)  { animation: mc-wave 0.8s 0.30s ease-in-out infinite; height: 20px; }
    .mc-wave-bar:nth-child(7)  { animation: mc-wave 0.8s 0.35s ease-in-out infinite; height: 16px; }
    .mc-wave-bar:nth-child(8)  { animation: mc-wave 0.8s 0.40s ease-in-out infinite; height: 24px; }
    .mc-wave-bar:nth-child(9)  { animation: mc-wave 0.8s 0.45s ease-in-out infinite; height: 20px; }
    .mc-wave-bar:nth-child(10) { animation: mc-wave 0.8s 0.50s ease-in-out infinite; height: 12px; }
    .mc-wave-bar:nth-child(11) { animation: mc-wave 0.8s 0.55s ease-in-out infinite; height: 8px; }
    .mc-wave-bar:nth-child(12) { animation: mc-wave 0.8s 0.60s ease-in-out infinite; height: 16px; }
    .mc-wave-bar:nth-child(13) { animation: mc-wave 0.8s 0.65s ease-in-out infinite; height: 24px; }
    .mc-wave-bar:nth-child(14) { animation: mc-wave 0.8s 0.70s ease-in-out infinite; height: 20px; }
    .mc-wave-bar:nth-child(15) { animation: mc-wave 0.8s 0.75s ease-in-out infinite; height: 12px; }
    .mc-wave-bar:nth-child(16) { animation: mc-wave 0.8s 0.80s ease-in-out infinite; height: 8px; }
    .mc-wave-bar:nth-child(17) { animation: mc-wave 0.8s 0.85s ease-in-out infinite; height: 20px; }
    .mc-wave-bar:nth-child(18) { animation: mc-wave 0.8s 0.90s ease-in-out infinite; height: 16px; }
    .mc-wave-bar:nth-child(19) { animation: mc-wave 0.8s 0.95s ease-in-out infinite; height: 12px; }
    .mc-wave-bar:nth-child(20) { animation: mc-wave 0.8s 1.00s ease-in-out infinite; height: 8px; }

    @keyframes mc-wave {
      0%, 100% { transform: scaleY(0.4); }
      50% { transform: scaleY(1); }
    }

    /* Microcopy */
    .mc-microcopy {
      text-align: center;
      font-size: 0.8rem;
      color: var(--text-secondary);
      font-style: italic;
      min-height: 1.2em;
      animation: mc-fade 0.5s ease;
    }

    @keyframes mc-fade {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    /* Confidence */
    .mc-confidence {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.4rem;
      font-size: 0.72rem;
      color: var(--text-muted);
      padding: 0.5rem;
      background: var(--bg-elevated);
      border-radius: 0.5rem;
      border: 1px dashed var(--border);
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* Responsive */
    @media (max-width: 640px) {
      .mc-stats-grid { grid-template-columns: repeat(2, 1fr); }
      .mc-timer { font-size: 1.1rem; }
    }

    @media (prefers-reduced-motion: reduce) {
      .mc-wave-bar, .mc-dot, .mc-stage-spinner, .mc-progress-fill::after {
        animation: none !important;
      }
    }
  `]
})
export class MissionControlComponent implements OnInit, OnDestroy {
  private cdr = inject(ChangeDetectorRef);

  @Input() charCount = 0;
  @Input() voiceName = '';
  @Input() language = 'Auto';
  @Input() quality = 'Standard';
  @Input() speed = 1.0;
  @Input() elapsedMs = 0;
  @Input() estimatedSeconds = 20;
  @Input() isComplete = false;

  readonly stages = [
    'Script received',
    'Language mode selected',
    'Voice profile loaded',
    'Acoustic tokens rendering',
    'Waveform encoding',
    'Final audio preparing'
  ];

  readonly tips = [
    'Tuning pronunciation for your script…',
    'Building natural speech pauses…',
    'Higher quality = richer voice, worth the wait…',
    'Hindi + English mixing is our specialty…',
    'Encoding studio-grade WAV audio…',
    'Almost there — finalizing your voiceover…',
    'Fun fact: our AI generates speech 10× faster than human recording…',
    'Your audio will auto-play when ready…',
  ];

  readonly waveformBars = Array(20).fill(0);

  currentStageIndex = 0;
  currentTip = this.tips[0];
  private tipTimer: ReturnType<typeof setInterval> | null = null;
  private stageTimer: ReturnType<typeof setInterval> | null = null;

  get progressPercent(): number {
    if (this.estimatedSeconds <= 0) return 0;
    const pct = Math.min(95, (this.elapsedMs / 1000 / this.estimatedSeconds) * 100);
    return Math.round(pct);
  }

  formatTime(ms: number): string {
    const totalSec = Math.floor(ms / 1000);
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    return m > 0 ? `${m}:${s.toString().padStart(2, '0')}` : `${s}s`;
  }

  ngOnInit() {
    // Rotate tips every 3 seconds
    let tipIdx = 0;
    this.tipTimer = setInterval(() => {
      tipIdx = (tipIdx + 1) % this.tips.length;
      this.currentTip = this.tips[tipIdx];
      this.cdr.detectChanges();
    }, 3000);

    // Advance stages based on estimated time
    const stageInterval = (this.estimatedSeconds * 1000) / this.stages.length;
    this.stageTimer = setInterval(() => {
      if (this.currentStageIndex < this.stages.length - 1) {
        this.currentStageIndex++;
        this.cdr.detectChanges();
      }
    }, Math.max(stageInterval, 2000));
  }

  ngOnDestroy() {
    if (this.tipTimer) clearInterval(this.tipTimer);
    if (this.stageTimer) clearInterval(this.stageTimer);
  }
}
