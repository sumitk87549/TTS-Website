# Words2Voice — Production Upgrade Implementation Plan (All 5 Parts)

This plan upgrades words2voice from a working prototype to a **commercial-grade, production-ready TTS platform** by implementing all 5 upgrade sets in sequence.

---

## What This Achieves

- ✅ Audio appears instantly after generation (critical bug fix)
- ✅ Cinematic "Mission Control" panel during TTS processing with ETA
- ✅ Full analytics: page views, funnel, TTS metrics, session tracking
- ✅ Admin dashboard: overview, users, voices, contacts, daily stats
- ✅ Premium UI: toast notifications, skeleton loaders, voice personality tags
- ✅ Landing page: voice samples section, how-it-works, more use cases
- ✅ Mobile: sticky generate button, responsive layouts
- ✅ Performance: lazy routes, voice caching, skeleton states
- ✅ Security: headers, JWT env var, stack trace suppression
- ✅ Legal: Privacy Policy + Terms of Use pages
- ✅ Brand: consistent "words2voice" naming everywhere

---

## SPRINT 1 — Studio Bug Fix + UX (Part 1)

### Frontend — Studio Component

#### [MODIFY] [studio.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/studio/studio.component.ts)
- Add `ViewChild` for `#audioPlayerRef`, `NgZone` already imported
- Add `generationState`, `generationElapsedMs`, `generationStartTime` properties
- Replace `generate()` method with elapsed-timer + `cdr.detectChanges()` pattern
- Change all `cdr.markForCheck()` → `cdr.detectChanges()` (4 places)
- Change default quality from `qualityPresets[3]` (Ultra) → `qualityPresets[1]` (Standard)
- Add `estimatedSpokenDuration` and `estimatedGenerationTime` computed getters
- Add `clearText()` and `pasteFromClipboard()` methods
- Add `saveDraft()` and localStorage draft restore in `ngOnInit()`
- Add `static _voiceCache` for voice caching
- Add `applyVoices()` helper method
- Add `voiceUseCases` mapping object
- Add `estimatedGenerationSeconds` computed property
- Update `ngOnDestroy()` to clear `_elapsedTimer`

#### [MODIFY] [studio.component.html](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/studio/studio.component.html)
- Add `#audioPlayerRef` to `<audio>` element
- Add `(ngModelChange)="saveDraft()"` to textarea
- Add editor-actions div with Paste/Clear buttons after char-count
- Add `.text-stats` div with spoken duration, generation time, word count chips
- Add `.sample-chips` section (only when text is empty)
- Replace result section with improved version: error-content+retry, loading-state with elapsed, audio-result-card with metadata chips + regenerate button
- Replace `download="voisetu_audio.wav"` → `download="words2voice_audio.wav"`
- Add `voice-usecase` div inside voice-card (from voiceUseCases mapping)
- Replace loading-state div with `<app-mission-control>` component

#### [MODIFY] [studio.component.scss](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/studio/studio.component.scss)
- Add `.text-stats`, `.stat-chip` styles
- Add `.editor-actions`, `.editor-action-btn` styles
- Add `.sample-chips`, `.sample-chip` styles
- Add `.error-content`, `.retry-btn` styles
- Add `.loading-main`, `.loading-text`, `.loading-hint` styles
- Add `.audio-result-card`, `.audio-result-header`, `.audio-result-meta`, `.meta-chip` styles
- Add `.audio-actions`, `.btn-regenerate` styles
- Add `.voice-usecase` styles
- Add mobile `.bottom-bar` sticky styles

#### [NEW] [mission-control.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/studio/mission-control.component.ts)
- New standalone component with inline template+styles
- Inputs: `charCount`, `voiceName`, `language`, `quality`, `speed`, `elapsedMs`, `estimatedSeconds`, `isComplete`
- Animated stages, rotating tips, ETA progress bar, waveform animation
- Implements `OnInit`/`OnDestroy` to clean up timers

---

## SPRINT 2 — Analytics + Admin (Part 3)

### Backend — Analytics

#### [MODIFY] [schema.sql](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/resources/schema.sql)
- Add 3 tables: `analytics_session`, `analytics_event`, `synthesis_metric`
- Add indexes on `event_name`, `created_at`, `session_id`, `anonymous_id`, `generation_id`
- Add `ALTER TABLE generation ADD COLUMN IF NOT EXISTS stage`, `estimated_seconds`, `synthesis_ms`

#### [NEW] [AnalyticsController.java](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend/AnalyticsController.java)
- `POST /api/public/analytics/events` — receives batched events from sendBeacon
- Hashes IP for privacy, stores to `analytics_event` table
- Custom JSON serializer (no Jackson dependency needed)

#### [MODIFY] [AdminController.java](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend/AdminController.java)
- Replace stub with full expanded version
- 6 endpoints: `/stats`, `/top-voices`, `/recent-users`, `/daily-stats`, `/contacts`, `/analytics-summary`
- All guarded by `isAdmin()` check

#### [MODIFY] [SecurityConfig.java](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend/SecurityConfig.java)
- Add security headers: `frameOptions.deny()`, `contentTypeOptions`, `xssProtection`

#### [MODIFY] [application.yml](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/resources/application.yml)
- Add `server.error.include-stacktrace: never`
- Externalize JWT secret: `${JWT_SECRET:existing-fallback-key}`

### Frontend — Analytics Service + Admin

#### [NEW] [analytics.service.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/core/analytics/analytics.service.ts)
- Tracks page views via router events
- Batches events, flushes via `sendBeacon` every 8 seconds or on page hide
- Generates persistent `anonymousId` (localStorage) + session-scoped `sessionId`
- Detects device type and browser automatically

#### [MODIFY] [studio.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/studio/studio.component.ts)
- Inject `AnalyticsService`
- Track: `generate_clicked`, `generate_success`, `generate_failed`, `voice_previewed`, `audio_downloaded`

#### [MODIFY] [landing.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/landing/landing.component.ts)
- Inject `AnalyticsService`
- Track: `landing_demo_generate`, `landing_demo_success`

#### [MODIFY] [login.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/auth/login/login.component.ts)
- Track: `login_success`

#### [MODIFY] [app.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/app.ts)
- Inject `AnalyticsService` at root to initialize it on boot
- Import `ToastComponent` and add to imports array

#### [NEW] [admin.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/admin/admin.component.ts)
- Loads all admin endpoints in parallel
- 6 tabs: Overview, Top Voices, Users, Daily Stats, Contacts, Analytics

#### [NEW] [admin.component.html](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/admin/admin.component.html)
- Tab-based admin dashboard using design system variables

#### [NEW] [admin.component.scss](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/admin/admin.component.scss)
- Styled admin UI (cards, tables, metric tiles)

---

## SPRINT 3 — UI Polish + Landing (Part 4)

### Frontend — Design System

#### [MODIFY] [styles.scss](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/styles.scss)
- Fix day mode `--text-muted` from `#9896b0` → `#6b698a`
- Add `--text-on-accent`, `--text-on-saffron`, `--text-on-success`, `--text-on-danger` to both themes
- Add `@keyframes toast-in`, `toast-out`
- Add `.skeleton`, `.skeleton-text`, `.skeleton-card` shimmer classes

### Frontend — Toast System

#### [NEW] [toast.service.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/core/toast/toast.service.ts)
- `show()`, `success()`, `error()`, `info()` methods
- Uses Angular `signal<Toast[]>` — reactive

#### [NEW] [toast.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/core/toast/toast.component.ts)
- Fixed-position toast container (bottom-right)
- Success/error/info variants with icons

### Frontend — Landing Page

#### [MODIFY] [landing.component.html](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/landing/landing.component.html)
- Update h1 and hero-subtitle copy
- Add `<!-- Voice Samples -->` section after hero
- Add `<!-- How It Works -->` section (3 steps)
- Add 3 more use case cards (Education, Meditation, Announcements)
- Add Privacy and Terms links to footer nav

#### [MODIFY] [landing.component.scss](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/landing/landing.component.scss)
- Add `.samples-section`, `.samples-grid`, `.sample-card`, `.sample-avatar`, `.sample-info`, `.sample-name`, `.sample-style`, `.sample-gender-badge`
- Add `.how-section`, `.steps-grid`, `.step-card`, `.step-number`, `.step-connector`
- Add/ensure responsive rules `@media (max-width: 768px)`, `@media (max-width: 480px)`

### Frontend — Legal Pages

#### [NEW] [privacy.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/privacy/privacy.component.ts)
#### [NEW] [terms.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/terms/terms.component.ts)

### Frontend — App Routes + Layout

#### [MODIFY] [app.routes.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/app.routes.ts)
- Change all dashboard children to lazy-loaded `loadComponent`
- Make login/signup lazy-loaded
- Add admin route as lazy child
- Add `privacy` and `terms` routes
- Remove now-unnecessary direct imports at top

#### [MODIFY] [app.html](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/app.html)
- Add `<app-toast>` after `<router-outlet>`

#### [MODIFY] [dashboard.component.ts](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/dashboard/dashboard.component.ts)
- Add `isAdmin = false` property
- Read `res.isAdmin` in `loadProfileData()` subscribe callback

#### [MODIFY] [dashboard.component.html](file:///home/sumit/Documents/GitHub/TTS-Website/frontend/src/app/features/dashboard/dashboard.component.html)
- Add Admin nav link (conditionally shown when `isAdmin`)

---

## SPRINT 4 — Backend Health + TTS Service (Parts 2+5)

### Backend

#### [NEW] [HealthController.java](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend/HealthController.java)
- `GET /api/public/health` — reports DB + TTS engine + uptime

#### [MODIFY] [TtsEngineManager.java](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend/TtsEngineManager.java)
- Add `isEngineReachable()` method (HTTP GET to TTS /health with 3s timeout)

### TTS Service

#### [MODIFY] [main.py](file:///home/sumit/Documents/GitHub/TTS-Website/tts-service/main.py)
- Add `_start_time = time.time()` at module level
- Update `/health` to include `voices_available`, `uptime_seconds`
- Add `POST /estimate` endpoint
- Update `Content-Disposition` filename: `voisetu_audio.wav` → `words2voice_audio.wav`

### Backend UserController

#### [MODIFY] [UserController.java](file:///home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend/UserController.java)
- Ensure `/api/me` response includes `isAdmin` field

---

## Verification Plan

### Build Verification
- `cd frontend && ng build --configuration production` — must produce 0 errors

### Functional Tests (Manual)
1. **Audio fix**: Generate → audio appears immediately without extra click
2. **Mission Control**: Generating shows animated panel with elapsed time + stages
3. **Draft autosave**: Type text, close tab, reopen → text restored
4. **Quick start chips**: Empty studio shows chips; clicking fills text
5. **Toast**: After success, toast appears "Audio ready — click play to listen!"
6. **Admin dashboard**: Login as admin → see `/admin` in sidebar, load data
7. **Analytics**: After generation, check DB for `analytics_event` rows
8. **Privacy/Terms pages**: Navigate to `/privacy`, `/terms` — both load

### Theme Verification
- Night mode: all text visible, no blending into background
- Day mode: all text visible, `--text-muted` is dark enough to read

### Mobile Verification
- Studio: generate button sticky at bottom on small screens
- Landing: hero, features, steps all responsive

---

> [!NOTE]
> Backend package names `com.voisetu` are kept as-is since they're not user-facing. Only user-facing strings are changed to "words2voice".

> [!IMPORTANT]
> The analytics endpoint is at `/api/public/analytics/events` — it must be added to `SecurityConfig` `permitAll()` list, which already permits `/api/public/**`.
