import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div *ngFor="let t of toastService.toasts()"
           class="toast-item"
           [class.success]="t.type === 'success'"
           [class.error]="t.type === 'error'"
           [class.info]="t.type === 'info'">
        <span class="toast-icon">
          {{ t.type === 'success' ? '✓' : t.type === 'error' ? '✕' : 'ℹ' }}
        </span>
        {{ t.message }}
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      bottom: 1.5rem;
      right: 1.5rem;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 380px;
      pointer-events: none;
    }

    .toast-item {
      padding: 0.75rem 1.25rem;
      border-radius: 0.75rem;
      font-size: 0.85rem;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      animation: toast-in 0.3s ease-out;
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      box-shadow: var(--shadow-lg, 0 8px 32px rgba(0,0,0,0.3));
      pointer-events: auto;
    }

    .toast-item.success {
      background: var(--success-subtle);
      color: var(--success);
      border: 1px solid var(--success);
    }

    .toast-item.error {
      background: var(--danger-subtle);
      color: var(--danger);
      border: 1px solid var(--danger);
    }

    .toast-item.info {
      background: var(--accent-subtle);
      color: var(--accent);
      border: 1px solid var(--accent);
    }

    .toast-icon {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.7rem;
      font-weight: 700;
      flex-shrink: 0;
    }

    .success .toast-icon { background: var(--success); color: #fff; }
    .error .toast-icon { background: var(--danger); color: #fff; }
    .info .toast-icon { background: var(--accent); color: #fff; }

    @keyframes toast-in {
      from { opacity: 0; transform: translateY(20px) scale(0.95); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }

    @media (max-width: 480px) {
      .toast-container {
        bottom: 5rem;
        left: 1rem;
        right: 1rem;
        max-width: none;
      }
    }
  `]
})
export class ToastComponent {
  toastService = inject(ToastService);
}
