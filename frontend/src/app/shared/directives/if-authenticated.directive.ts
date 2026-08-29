import {
  Directive,
  OnInit,
  OnDestroy,
  TemplateRef,
  ViewContainerRef,
  inject,
  effect,
} from '@angular/core';
import { AuthService } from '../../core/auth/auth';

/**
 * Structural Directive: *appIfAuthenticated
 * Conditionally renders content only when the user is authenticated.
 * Reactively updates when auth state changes (Signal-based).
 *
 * Usage:
 *   <div *appIfAuthenticated>This is only visible when logged in</div>
 */
@Directive({
  selector: '[appIfAuthenticated]',
  standalone: true,
})
export class IfAuthenticatedDirective implements OnInit, OnDestroy {
  private templateRef = inject(TemplateRef<unknown>);
  private viewContainer = inject(ViewContainerRef);
  private authService = inject(AuthService);

  private hasView = false;

  constructor() {
    // React to auth signal changes automatically
    effect(() => {
      const authenticated = this.authService.isAuthenticated();
      this.updateView(authenticated);
    });
  }

  ngOnInit(): void {
    // Initial render handled by effect()
  }

  ngOnDestroy(): void {
    this.viewContainer.clear();
  }

  private updateView(authenticated: boolean): void {
    if (authenticated && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!authenticated && this.hasView) {
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
