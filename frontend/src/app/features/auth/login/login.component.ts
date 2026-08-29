import {
  Component,
  inject,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';
import { AnalyticsService } from '../../../core/analytics/analytics.service';

/**
 * LoginComponent — Reactive Forms with inline validation + OnPush CD.
 *
 * Demonstrates ReactiveFormsModule:
 *  - FormBuilder for concise group definition
 *  - Validators.email, Validators.minLength
 *  - Accessing controls via loginForm.get() in template
 *  - submit only when form is valid
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private analytics = inject(AnalyticsService);
  private cdr = inject(ChangeDetectorRef);

  /** Reactive form group with built-in validators */
  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  serverError = '';
  loading = false;

  /** Convenience getters for cleaner template access */
  get email() { return this.loginForm.get('email')!; }
  get password() { return this.loginForm.get('password')!; }

  /** Whether a field has been touched and is invalid (used for showing errors) */
  hasError(controlName: string, error: string): boolean {
    const ctrl = this.loginForm.get(controlName)!;
    return ctrl.touched && ctrl.hasError(error);
  }

  onSubmit(): void {
    // Mark all fields as touched to trigger validation messages
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.serverError = '';

    const { email, password } = this.loginForm.value;
    this.authService.login({ email, password }).subscribe({
      next: () => {
        this.analytics.track('login_success');
        this.router.navigate(['/studio']);
      },
      error: () => {
        this.serverError = 'Invalid email or password. Please try again.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
