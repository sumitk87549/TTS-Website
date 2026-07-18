import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  http = inject(HttpClient);
  authService = inject(AuthService);
  
  user = { email: '', displayName: '', createdAt: '' };
  
  // Profile Form
  displayNameInput = '';
  profileSuccess = '';
  
  // Password Form
  currentPassword = '';
  newPassword = '';
  passwordError = '';
  passwordSuccess = '';
  
  ngOnInit() {
    this.http.get<any>(`${environment.apiBaseUrl}/me`).subscribe(res => {
      this.user = res;
      this.displayNameInput = res.displayName;
    });
  }
  
  updateProfile() {
    this.profileSuccess = '';
    this.http.patch(`${environment.apiBaseUrl}/me`, { displayName: this.displayNameInput }).subscribe(() => {
      this.user.displayName = this.displayNameInput;
      this.profileSuccess = 'Profile updated successfully.';
      setTimeout(() => this.profileSuccess = '', 3000);
    });
  }
  
  changePassword() {
    this.passwordError = '';
    this.passwordSuccess = '';
    
    if (!this.currentPassword || !this.newPassword) return;
    
    this.http.post(`${environment.apiBaseUrl}/me/change-password`, {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.passwordSuccess = 'Password changed successfully.';
        this.currentPassword = '';
        this.newPassword = '';
        setTimeout(() => this.passwordSuccess = '', 3000);
      },
      error: (err) => {
        this.passwordError = err.error?.error || 'Failed to change password.';
      }
    });
  }
  
  deleteAccount() {
    const confirmation = prompt('Are you sure you want to delete your account? This will delete all your projects and audio files forever. Type "DELETE" to confirm.');
    if (confirmation === 'DELETE') {
      this.http.delete(`${environment.apiBaseUrl}/me`).subscribe(() => {
        this.authService.logout();
      });
    }
  }

  // Interest Signal Form
  wouldPay = '';
  suggestedPriceInr: number | null = null;
  comment = '';
  interestSubmitted = false;
  interestLoading = false;
  interestError = '';

  submitInterest() {
    if (!this.wouldPay) return;
    this.interestLoading = true;
    this.interestError = '';
    
    this.http.post(`${environment.apiBaseUrl}/interest`, {
      wouldPay: this.wouldPay,
      suggestedPriceInr: this.suggestedPriceInr,
      comment: this.comment
    }).subscribe({
      next: () => {
        this.interestLoading = false;
        this.interestSubmitted = true;
      },
      error: (err) => {
        this.interestLoading = false;
        this.interestError = err.error?.error || 'Failed to submit. Please try again.';
      }
    });
  }
}
