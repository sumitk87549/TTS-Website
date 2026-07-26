import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';
import { AnalyticsService } from '../../../core/analytics/analytics.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  authService = inject(AuthService);
  router = inject(Router);
  analytics = inject(AnalyticsService);

  email = '';
  password = '';
  error = '';
  loading = false;

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.analytics.track('login_success');
        this.router.navigate(['/studio']);
      },
      error: (err) => {
        this.error = 'Invalid email or password';
        this.loading = false;
      }
    });
  }
}
