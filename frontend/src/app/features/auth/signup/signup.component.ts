import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss'] // Reusing login styles + custom ones if needed
})
export class SignupComponent {
  authService = inject(AuthService);
  router = inject(Router);

  displayName = '';
  email = '';
  password = '';
  error = '';
  loading = false;

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.authService.register({ displayName: this.displayName, email: this.email, password: this.password }).subscribe({
      next: () => {
        this.router.navigate(['/studio']);
      },
      error: (err) => {
        this.error = err.error?.error || 'Registration failed';
        this.loading = false;
      }
    });
  }
}
