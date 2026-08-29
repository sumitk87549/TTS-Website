import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import {
  ReactiveFormsModule,
  FormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth';
import { UserProfileService } from '@core/user-profile/user-profile.service';

/**
 * Cross-field validator: newPassword must differ from currentPassword.
 * Demonstrates custom cross-field validator at the group level.
 */
function passwordsDifferentValidator(group: AbstractControl): ValidationErrors | null {
  const current = group.get('currentPassword')?.value ?? '';
  const next = group.get('newPassword')?.value ?? '';
  if (current && next && current === next) {
    return { samePassword: true };
  }
  return null;
}

/**
 * SettingsComponent — OnPush CD.
 *
 * Demonstrates mixed form strategy (per Angular best practices):
 *  - Profile section     → Reactive Form (typed, validated)
 *  - Password section    → Reactive Form with cross-field validator
 *  - Feedback/Avatar     → Template-driven (FormsModule) — simple, no complex validation needed
 *
 * Uses UserProfileService (shared Signal state) for sibling communication
 * with DashboardComponent — when display name is saved here, the sidebar
 * updates automatically via Signal without any event bus / Subject needed.
 */
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  readonly authService = inject(AuthService);
  readonly profileService = inject(UserProfileService);
  private cdr = inject(ChangeDetectorRef);

  // ── Reactive Forms ────────────────────────────────────────────────────

  /** Profile form: display name only (email is read-only) */
  profileForm: FormGroup = this.fb.group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(40)]],
  });
  profileSuccess = '';

  /** Password form with cross-field validator */
  passwordForm: FormGroup = this.fb.group(
    {
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
    },
    { validators: passwordsDifferentValidator }
  );
  passwordSuccess = '';
  passwordError = '';

  // ── Avatar / Template-driven (simple, no validation needed) ──────────
  readonly avatarOptions = [
    '🎤', '🧑‍💻', '🦚', '🌸', '🎵', '🐯',
    '🦋', '🌟', '🎨', '🏔️', '🧡', '💫',
    '🎭', '🦁', '🌺', '🎙️', '🦜', '🎸'
  ];

  readonly colorOptions = [
    { label: 'Violet',  value: 'linear-gradient(135deg, #7c5cf7, #e8608a)' },
    { label: 'Saffron', value: 'linear-gradient(135deg, #ff9933, #ff6b35)' },
    { label: 'Teal',    value: 'linear-gradient(135deg, #00c9a7, #0066cc)' },
    { label: 'Rose',    value: 'linear-gradient(135deg, #e8608a, #ff9933)' },
    { label: 'Indigo',  value: 'linear-gradient(135deg, #4f46e5, #7c3aed)' },
    { label: 'Amber',   value: 'linear-gradient(135deg, #ffbb00, #ff9933)' },
    { label: 'Forest',  value: 'linear-gradient(135deg, #138808, #00c9a7)' },
    { label: 'Dusk',    value: 'linear-gradient(135deg, #1a1a2e, #6c47e8)' },
  ];

  // Template-driven bindings for avatar (no validation needed)
  selectedAvatar = this.profileService.avatarEmoji();
  selectedAvatarColor = this.profileService.avatarColor();
  avatarSaved = false;

  // ── Feedback (template-driven, radios + textarea) ────────────────────
  wouldPay = '';
  suggestedPriceInr: number | null = null;
  comment = '';
  interestSubmitted = false;
  interestLoading = false;
  interestError = '';

  // ── Lifecycle ─────────────────────────────────────────────────────────

  ngOnInit(): void {
    // If profile already loaded (resolver ran), populate the form immediately
    const profile = this.profileService.profile();
    if (profile) {
      this.profileForm.patchValue({ displayName: profile.displayName });
    } else {
      // Fallback: load profile if resolver didn't run (direct navigation)
      this.profileService.loadProfile().subscribe(profile => {
        this.profileForm.patchValue({ displayName: profile.displayName });
        this.cdr.markForCheck();
      });
    }
  }

  // ── Convenience getters ────────────────────────────────────────────────

  get displayName() { return this.profileForm.get('displayName')!; }
  get currentPassword() { return this.passwordForm.get('currentPassword')!; }
  get newPassword() { return this.passwordForm.get('newPassword')!; }

  hasProfileError(error: string): boolean {
    return this.displayName.touched && this.displayName.hasError(error);
  }

  hasPasswordError(controlName: string, error: string): boolean {
    const ctrl = this.passwordForm.get(controlName)!;
    return ctrl.touched && ctrl.hasError(error);
  }

  // ── Form Submit Handlers ────────────────────────────────────────────────

  updateProfile(): void {
    this.profileForm.markAllAsTouched();
    if (this.profileForm.invalid) return;
    this.profileSuccess = '';

    const { displayName } = this.profileForm.value;
    // UserProfileService updates the Signal → DashboardComponent sidebar refreshes automatically
    this.profileService.updateDisplayName(displayName).subscribe({
      next: () => {
        this.profileSuccess = 'Profile updated successfully.';
        this.authService.notifyProfileUpdate(); // backward compat
        this.cdr.markForCheck();
        setTimeout(() => { this.profileSuccess = ''; this.cdr.markForCheck(); }, 3000);
      },
      error: () => {
        this.profileSuccess = '';
        this.cdr.markForCheck();
      },
    });
  }

  changePassword(): void {
    this.passwordForm.markAllAsTouched();
    if (this.passwordForm.invalid) return;
    this.passwordError = '';
    this.passwordSuccess = '';

    const { currentPassword, newPassword } = this.passwordForm.value;
    this.http.post(`${environment.apiBaseUrl}/me/change-password`, { currentPassword, newPassword }).subscribe({
      next: () => {
        this.passwordSuccess = 'Password changed successfully.';
        this.passwordForm.reset();
        this.cdr.markForCheck();
        setTimeout(() => { this.passwordSuccess = ''; this.cdr.markForCheck(); }, 3000);
      },
      error: (err) => {
        this.passwordError = err.error?.error || 'Failed to change password.';
        this.cdr.markForCheck();
      },
    });
  }

  deleteAccount(): void {
    const confirmation = prompt('Type "DELETE" to permanently delete your account and all data.');
    if (confirmation === 'DELETE') {
      this.http.delete(`${environment.apiBaseUrl}/me`).subscribe(() => {
        this.authService.logout();
      });
    }
  }

  // ── Avatar ─────────────────────────────────────────────────────────────

  saveAvatar(): void {
    // UserProfileService.saveAvatar() updates Signals → DashboardComponent sidebar refreshes
    this.profileService.saveAvatar(this.selectedAvatar, this.selectedAvatarColor);
    this.avatarSaved = true;
    this.authService.notifyProfileUpdate(); // backward compat
    setTimeout(() => { this.avatarSaved = false; this.cdr.markForCheck(); }, 2500);
  }

  // ── Feedback ────────────────────────────────────────────────────────────

  submitInterest(): void {
    if (!this.wouldPay) return;
    this.interestLoading = true;
    this.interestError = '';
    this.http.post(`${environment.apiBaseUrl}/interest`, {
      wouldPay: this.wouldPay,
      suggestedPriceInr: this.suggestedPriceInr,
      comment: this.comment,
    }).subscribe({
      next: () => {
        this.interestLoading = false;
        this.interestSubmitted = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.interestLoading = false;
        this.interestError = err.error?.error || 'Failed to submit. Please try again.';
        this.cdr.markForCheck();
      },
    });
  }
}
