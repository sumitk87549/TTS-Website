import { Directive, ElementRef, EventEmitter, HostListener, Output, inject } from '@angular/core';

/**
 * Attribute Directive: appClickOutside
 * Emits an event when a click occurs outside of the host element.
 * Usage: <aside (appClickOutside)="closeMenu()">...</aside>
 */
@Directive({
  selector: '[appClickOutside]',
  standalone: true,
})
export class ClickOutsideDirective {
  @Output() appClickOutside = new EventEmitter<MouseEvent>();

  private el = inject(ElementRef);

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const clickedInside = this.el.nativeElement.contains(event.target as Node);
    if (!clickedInside) {
      this.appClickOutside.emit(event);
    }
  }
}
