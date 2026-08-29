import {
  Directive,
  ElementRef,
  HostListener,
  Input,
  OnDestroy,
  inject,
  Renderer2,
} from '@angular/core';

/**
 * Attribute Directive: appTooltip
 * Shows a styled tooltip on hover, replacing the native `title` attribute.
 * Usage: <button appTooltip="Paste from clipboard">📋</button>
 */
@Directive({
  selector: '[appTooltip]',
  standalone: true,
})
export class TooltipDirective implements OnDestroy {
  @Input('appTooltip') tooltipText = '';
  @Input() tooltipPosition: 'top' | 'bottom' | 'left' | 'right' = 'top';

  private el = inject(ElementRef);
  private renderer = inject(Renderer2);
  private tooltipEl: HTMLElement | null = null;

  @HostListener('mouseenter')
  onMouseEnter(): void {
    if (!this.tooltipText) return;
    this.createTooltip();
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.destroyTooltip();
  }

  @HostListener('click')
  onClick(): void {
    this.destroyTooltip();
  }

  private createTooltip(): void {
    this.destroyTooltip();

    this.tooltipEl = this.renderer.createElement('div');
    this.renderer.addClass(this.tooltipEl, 'app-tooltip');
    this.renderer.addClass(this.tooltipEl, `app-tooltip--${this.tooltipPosition}`);
    const text = this.renderer.createText(this.tooltipText);
    this.renderer.appendChild(this.tooltipEl, text);

    // Inline styles for zero-dependency tooltip
    const styles: Record<string, string> = {
      position: 'fixed',
      background: 'var(--bg-elevated, #1a1a2e)',
      color: 'var(--text-primary, #e2e8f0)',
      padding: '0.35rem 0.7rem',
      borderRadius: '0.4rem',
      fontSize: '0.75rem',
      fontWeight: '500',
      whiteSpace: 'nowrap',
      zIndex: '99999',
      pointerEvents: 'none',
      border: '1px solid var(--border, rgba(255,255,255,0.1))',
      boxShadow: '0 4px 12px rgba(0,0,0,0.25)',
      opacity: '0',
      transition: 'opacity 0.15s ease',
    };
    Object.entries(styles).forEach(([k, v]) => this.renderer.setStyle(this.tooltipEl, k, v));

    this.renderer.appendChild(document.body, this.tooltipEl);
    this.positionTooltip();

    // Fade in
    requestAnimationFrame(() => {
      if (this.tooltipEl) {
        this.renderer.setStyle(this.tooltipEl, 'opacity', '1');
      }
    });
  }

  private positionTooltip(): void {
    if (!this.tooltipEl) return;
    const rect = this.el.nativeElement.getBoundingClientRect();
    const tip = this.tooltipEl.getBoundingClientRect();
    let top: number, left: number;

    switch (this.tooltipPosition) {
      case 'bottom':
        top = rect.bottom + 6;
        left = rect.left + rect.width / 2 - tip.width / 2;
        break;
      case 'left':
        top = rect.top + rect.height / 2 - tip.height / 2;
        left = rect.left - tip.width - 6;
        break;
      case 'right':
        top = rect.top + rect.height / 2 - tip.height / 2;
        left = rect.right + 6;
        break;
      default: // top
        top = rect.top - tip.height - 6;
        left = rect.left + rect.width / 2 - tip.width / 2;
    }

    this.renderer.setStyle(this.tooltipEl, 'top', `${top}px`);
    this.renderer.setStyle(this.tooltipEl, 'left', `${left}px`);
  }

  private destroyTooltip(): void {
    if (this.tooltipEl) {
      this.renderer.removeChild(document.body, this.tooltipEl);
      this.tooltipEl = null;
    }
  }

  ngOnDestroy(): void {
    this.destroyTooltip();
  }
}
