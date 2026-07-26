import { Routes } from '@angular/router';
import { LandingComponent } from './landing/landing.component';
import { authGuard } from './core/auth/auth-guard';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'about', loadComponent: () => import('./about/about.component').then(m => m.AboutComponent) },
  { path: 'contact', loadComponent: () => import('./contact/contact.component').then(m => m.ContactComponent) },
  { path: 'privacy', loadComponent: () => import('./privacy/privacy.component').then(m => m.PrivacyComponent) },
  { path: 'terms', loadComponent: () => import('./terms/terms.component').then(m => m.TermsComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'signup', loadComponent: () => import('./features/auth/signup/signup.component').then(m => m.SignupComponent) },
  {
    path: '',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
    children: [
      { path: 'studio', loadComponent: () => import('./features/studio/studio.component').then(m => m.StudioComponent) },
      { path: 'projects', loadComponent: () => import('./features/projects/projects.component').then(m => m.ProjectsComponent) },
      { path: 'voice-lab', loadComponent: () => import('./features/voice-lab/voice-lab.component').then(m => m.VoiceLabComponent) },
      { path: 'history', loadComponent: () => import('./features/history/history.component').then(m => m.HistoryComponent) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent) },
      { path: 'admin', loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent) }
    ]
  },
  { path: '**', redirectTo: '' }
];
