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
    // Load saved avatar
    const savedEmoji = localStorage.getItem('w2v-avatar');
    if (savedEmoji) this.selectedAvatar = savedEmoji;
    const savedColor = localStorage.getItem('w2v-avatar-color');
    if (savedColor) this.selectedAvatarColor = savedColor;
  }
  
  updateProfile() {
    this.profileSuccess = '';
    this.http.patch(`${environment.apiBaseUrl}/me`, { displayName: this.displayNameInput }).subscribe(() => {
      this.user.displayName = this.displayNameInput;
      this.profileSuccess = 'Profile updated successfully.';
      this.authService.notifyProfileUpdate();
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
    const confirmation = prompt('Type "DELETE" to permanently delete your account and all data.');
    if (confirmation === 'DELETE') {
      this.http.delete(`${environment.apiBaseUrl}/me`).subscribe(() => {
        this.authService.logout();
      });
    }
  }

  // ── Avatar / Profile Customisation ──────────────────────────────────
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

  selectedAvatar = '🎤';
  selectedAvatarColor = 'linear-gradient(135deg, #7c5cf7, #e8608a)';
  avatarSaved = false;

  saveAvatar() {
    localStorage.setItem('w2v-avatar', this.selectedAvatar);
    localStorage.setItem('w2v-avatar-color', this.selectedAvatarColor);
    this.avatarSaved = true;
    this.authService.notifyProfileUpdate();
    setTimeout(() => this.avatarSaved = false, 2500);
  }

  // ── Interest/Feedback Form ───────────────────────────────────────────
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
