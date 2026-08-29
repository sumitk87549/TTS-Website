import { Directive, ElementRef, HostListener, OnInit, inject } from '@angular/core';

/**
 * Attribute Directive: appAutoResize
 * Automatically resizes a <textarea> to fit its content as the user types.
 * Usage: <textarea appAutoResize></textarea>
 */
@Directive({
  selector: 'textarea[appAutoResize]',
  standalone: true,
})
export class AutoResizeDirective implements OnInit {
  private el = inject(ElementRef<HTMLTextAreaElement>);

  ngOnInit(): void {
    // Set initial height after render
    this.resize();
  }

  @HostListener('input')
  onInput(): void {
    this.resize();
  }

  @HostListener('ngModelChange')
  onModelChange(): void {
    this.resize();
  }

  private resize(): void {
    const textarea = this.el.nativeElement;
    // Reset height to shrink when text is deleted
    textarea.style.height = 'auto';
    // Set to scroll height to expand
    textarea.style.height = `${textarea.scrollHeight}px`;
  }
}
