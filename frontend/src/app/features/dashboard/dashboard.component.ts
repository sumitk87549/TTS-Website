import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth';
import { ThemeService } from '../../core/theme/theme.service';
import { UserProfileService } from '@core/user-profile/user-profile.service';
import { ClickOutsideDirective } from '@shared/directives/click-outside.directive';
import { TooltipDirective } from '@shared/directives/tooltip.directive';
import { Subscription } from 'rxjs';

/**
 * DashboardComponent — shell layout with sidebar (OnPush).
 *
 * Architecture patterns demonstrated:
 *  - OnPush CD: only re-renders on Signal changes or explicit markForCheck()
 *  - Resolver data: profile was pre-fetched by dashboardResolver before activation
 *    so sidebar shows the real name on first paint (no flicker)
 *  - Signal reads: isAdmin, avatarEmoji, avatarColor, displayName from UserProfileService
 *  - ClickOutside directive: closes mobile menu on outside click
 *  - IfAuthenticated structural directive: conditionally renders admin nav
 *  - Sibling communication: Settings saves avatar → profileService signals update →
 *    this component's template re-renders automatically
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ClickOutsideDirective,
    TooltipDirective,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  readonly profileService = inject(UserProfileService);
  private cdr = inject(ChangeDetectorRef);

  menuOpen = false;
  sidebarCollapsed = false;

  private sub?: Subscription;

  ngOnInit(): void {
    // Profile was pre-loaded by dashboardResolver — no HTTP call needed here.
    // Subscribe to profileUpdated$ for backward compat with parts not yet using UserProfileService
    this.sub = this.authService.profileUpdated$.subscribe(() => {
      // UserProfileService signals already updated — just trigger CD for this component
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  toggleMenu(): void { this.menuOpen = !this.menuOpen; }
  closeMenu(): void { this.menuOpen = false; }
  toggleSidebar(): void { this.sidebarCollapsed = !this.sidebarCollapsed; }
  logout(): void {
    this.profileService.clearProfile();
    this.authService.logout();
  }
}
