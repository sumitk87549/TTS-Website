import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme/theme.service';
import { AnalyticsService } from './core/analytics/analytics.service';
import { ToastComponent } from './core/toast/toast.component';
import { SeoService } from './core/seo/seo.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  // Inject at root so it initializes theme on app boot
  private readonly themeService = inject(ThemeService);
  // Inject analytics to initialize session tracking on app boot
  private readonly analytics = inject(AnalyticsService);
  private readonly seo = inject(SeoService);

  constructor() {
    this.seo.init();
  }
}

