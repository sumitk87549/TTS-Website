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
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';

/**
 * Custom cross-field validator: ensures password meets basic strength rules.
 * Demonstrates custom Validator function usage in ReactiveFormsModule.
 */
function passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value ?? '';
  if (!value) return null;
  const hasUppercase = /[A-Z]/.test(value);
  const hasDigit = /\d/.test(value);
  if (value.length >= 8 && hasUppercase && hasDigit) return null;
  return {
    weakPassword: {
      hasUppercase,
      hasDigit,
      longEnough: value.length >= 8,
    },
  };
}

/**
 * SignupComponent — Reactive Forms with custom validator + OnPush CD.
 *
 * Demonstrates:
 *  - FormBuilder group
 *  - Custom cross-field/single-field validators
 *  - Validators.required, Validators.email, Validators.minLength
 *  - Custom passwordStrengthValidator
 *  - Inline validation error display per field
 */
@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  signupForm: FormGroup = this.fb.group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(40)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), passwordStrengthValidator]],
  });

  serverError = '';
  loading = false;

  /** Convenience getters */
  get displayName() { return this.signupForm.get('displayName')!; }
  get email() { return this.signupForm.get('email')!; }
  get password() { return this.signupForm.get('password')!; }

  /** Password strength breakdown for progressive hints */
  get passwordStrength() {
    const errors = this.password.errors?.['weakPassword'];
    return errors ?? { hasUppercase: true, hasDigit: true, longEnough: true };
  }

  hasError(controlName: string, error: string): boolean {
    const ctrl = this.signupForm.get(controlName)!;
    return ctrl.touched && ctrl.hasError(error);
  }

  onSubmit(): void {
    this.signupForm.markAllAsTouched();
    if (this.signupForm.invalid) return;

    this.loading = true;
    this.serverError = '';

    const { displayName, email, password } = this.signupForm.value;
    this.authService.register({ displayName, email, password }).subscribe({
      next: () => {
        this.router.navigate(['/studio']);
      },
      error: (err) => {
        this.serverError = err.error?.error || 'Registration failed. Please try again.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
