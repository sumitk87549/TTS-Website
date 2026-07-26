# Words2Voice — Production Upgrade Task Tracker

## Sprint 1 — Bug Fix + Studio UX
- [/] studio.component.ts — full rewrite (generate(), detectChanges, voice cache, analytics hooks)
- [ ] studio.component.html — audio ref, text-stats, chips, sample-chips, improved result, voice-usecase
- [ ] studio.component.scss — new style additions
- [ ] mission-control.component.ts — NEW file

## Sprint 2 — Analytics + Admin
- [ ] schema.sql — analytics tables + generation columns
- [ ] AnalyticsController.java — NEW file
- [ ] AdminController.java — expand with 6 endpoints
- [ ] SecurityConfig.java — add security headers
- [ ] application.yml — JWT env var + no stack traces
- [ ] analytics.service.ts — NEW file
- [ ] admin.component.ts — NEW file
- [ ] admin.component.html — NEW file
- [ ] admin.component.scss — NEW file

## Sprint 3 — UI Polish + Landing
- [ ] styles.scss — day mode contrast + toast keyframes + skeleton
- [ ] toast.service.ts — NEW file
- [ ] toast.component.ts — NEW file
- [ ] app.ts — inject analytics + toast
- [ ] app.html — add <app-toast>
- [ ] landing.component.html — hero copy + samples + how-it-works + use cases + footer
- [ ] landing.component.scss — new sections
- [ ] landing.component.ts — inject analytics
- [ ] privacy.component.ts — NEW file
- [ ] terms.component.ts — NEW file
- [ ] dashboard.component.ts — isAdmin
- [ ] dashboard.component.html — admin nav link
- [ ] login.component.ts — analytics tracking
- [ ] app.routes.ts — lazy loading + admin + privacy + terms routes

## Sprint 4 — Backend Health + TTS + Security
- [ ] HealthController.java — NEW file
- [ ] TtsEngineManager.java — add isEngineReachable()
- [ ] UserController.java — add isAdmin to /me response
- [ ] main.py — /estimate + improved /health + filename fix
