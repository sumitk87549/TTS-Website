import { Component, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent {
  http = inject(HttpClient);
  private location = inject(Location);

  goBack() { this.location.back(); }

  name = '';
  email = '';
  message = '';
  loading = false;
  error = '';
  submitted = false;

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.http.post<any>(`${environment.apiBaseUrl}/public/contact`, {
      name: this.name, email: this.email, message: this.message
    }).subscribe({
      next: () => {
        this.submitted = true;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Something went wrong. Please try again.';
        this.loading = false;
      }
    });
  }
}
