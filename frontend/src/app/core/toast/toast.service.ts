import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _id = 0;
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: 'success' | 'error' | 'info' = 'info', duration = 4000) {
    const id = ++this._id;
    const toast: Toast = { id, message, type, duration };
    this.toasts.update(t => [...t, toast]);

    setTimeout(() => {
      this.toasts.update(t => t.filter(x => x.id !== id));
    }, duration);
  }

  success(message: string) { this.show(message, 'success'); }
  error(message: string) { this.show(message, 'error', 6000); }
  info(message: string) { this.show(message, 'info'); }
}
