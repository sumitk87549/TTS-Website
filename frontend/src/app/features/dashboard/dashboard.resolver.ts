import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { catchError, of } from 'rxjs';
import { UserProfileService, UserProfile } from '../../core/user-profile/user-profile.service';

/**
 * dashboardResolver — pre-fetches user profile before DashboardComponent activates.
 *
 * This means the sidebar will have the user's name and avatar ready on first paint
 * (no "My Profile" flicker). Resolvers run before the component initialises.
 *
 * If the API call fails (e.g. offline), we return null gracefully so the route
 * still activates — DashboardComponent handles null profile defensively.
 */
export const dashboardResolver: ResolveFn<UserProfile | null> = () => {
  const profileService = inject(UserProfileService);
  return profileService.loadProfile().pipe(
    catchError(() => of(null))
  );
};
