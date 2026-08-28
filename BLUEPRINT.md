# words2voice — Structural Blueprint

> **Purpose of this file**: A single-document structural reference for AI assistants, developers, and technical stakeholders. Reading this file gives a complete architectural understanding of the project without browsing source code.

---

## 1. Project Overview

**words2voice** (`words2voice.in`) is a free, India-focused, AI-powered **Text-to-Speech (TTS) web platform** that generates natural Hindi, English, and Hinglish voiceovers. It targets Indian content creators (YouTubers, Reels makers, podcasters, small businesses).

- **Brand name**: words2voice
- **Internal code name**: voisetu
- **Status**: Public Beta (free, no credit card required)
- **TTS engine**: Supertonic-3 (local diffusion-based model, runs on-device)
- **Languages**: Hindi (Devanagari), English, Hinglish (auto-detect)
- **Voices**: 10 presets — 5 male (M1–M5), 5 female (F1–F5)

---

## 2. Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Frontend | Angular 18 (standalone components, SCSS) | SPA, lazy-loaded routes |
| Backend API | Spring Boot 4.1, Java 17, JdbcTemplate | REST API, JWT auth, no JPA/ORM |
| TTS Engine | FastAPI (Python 3.10) + `supertonic` library | Diffusion TTS model, runs on port 8000 |
| Database | PostgreSQL (Neon in prod, local in dev) | Plain SQL via JdbcTemplate |
| Auth | JWT (JJWT 0.11.5) + BCrypt | Stateless, no sessions |
| Hosting (prod) | Cloudflare Pages / Render.com / Oracle ARM / Neon | All free tier |

---

## 3. Repository Structure

```
TTS-Website/
├── frontend/                      # Angular SPA
│   ├── src/
│   │   ├── app/
│   │   │   ├── landing/           # Public landing page + demo widget
│   │   │   ├── features/
│   │   │   │   ├── studio/        # Main TTS studio (auth-gated)
│   │   │   │   │   ├── studio.component.*        # Orchestrator
│   │   │   │   │   ├── mission-control.component.ts  # Generation progress UI
│   │   │   │   │   ├── voice-picker/             # Voice selection component
│   │   │   │   │   ├── models/studio.models.ts   # TS interfaces
│   │   │   │   │   └── services/                 # API + estimator services
│   │   │   │   ├── dashboard/     # User dashboard (saved projects)
│   │   │   │   ├── history/       # Generation history
│   │   │   │   ├── projects/      # Project management
│   │   │   │   └── admin/         # Admin panel (admin users only)
│   │   │   ├── auth/
│   │   │   │   ├── login/         # Login page
│   │   │   │   └── signup/        # Signup page
│   │   │   ├── contact/           # Contact form → backend DB
│   │   │   ├── terms/             # Terms of Service
│   │   │   ├── about/             # About page
│   │   │   ├── core/
│   │   │   │   ├── analytics/analytics.service.ts  # Event tracking
│   │   │   │   ├── auth/          # JWT storage, auth guard, interceptor
│   │   │   │   ├── theme/         # Day/night theme service
│   │   │   │   └── toast/         # Toast notification service
│   │   │   └── app.routes.ts      # Route definitions (lazy loading)
│   │   ├── environments/
│   │   │   ├── environment.ts     # Dev: apiBaseUrl = http://localhost:8080/api
│   │   │   └── environment.prod.ts # Prod: apiBaseUrl = https://<render-url>/api
│   │   ├── styles.scss            # Global CSS variables, themes
│   │   └── index.html             # Full SEO meta tags + Schema.org JSON-LD
│   └── public/
│       ├── _redirects             # Cloudflare Pages SPA routing fallback
│       ├── robots.txt             # SEO: crawl rules
│       └── sitemap.xml            # SEO: all public URLs
│
├── backend/                       # Spring Boot API
│   ├── src/main/java/com/voisetu/backend/
│   │   ├── BackendApplication.java
│   │   ├── SecurityConfig.java    # JWT filter chain, CORS from env var
│   │   ├── JwtAuthFilter.java     # JWT validation on every request
│   │   ├── JwtUtil.java           # Token generation/validation
│   │   ├── AuthController.java    # POST /api/auth/register, /login
│   │   ├── TtsController.java     # POST /api/public/tts/preview (anon), GET /voices
│   │   ├── GenerationController.java # POST /api/studio/generate (auth)
│   │   ├── SupertonicClient.java  # HTTP client → FastAPI TTS service
│   │   ├── TtsEngineManager.java  # Health-check of TTS service at startup
│   │   ├── AnalyticsController.java  # POST /api/public/analytics/events
│   │   ├── ContactController.java # POST /api/public/contact
│   │   ├── PublicStatsController.java # GET /api/public/stats (landing page)
│   │   ├── SiteMetricsController.java # GET /api/admin/metrics (admin only)
│   │   ├── AppUserRepository.java # User DB queries
│   │   └── CustomUserDetailsService.java
│   ├── src/main/resources/
│   │   ├── application.yml        # Profiles: local (hardcoded), prod (env vars)
│   │   └── schema.sql             # Full DB schema, runs on every startup
│   └── Dockerfile                 # Multi-stage build, JVM tuned for 512 MB
│
├── tts-service/                   # FastAPI TTS microservice
│   ├── main.py                    # All endpoints: /health, /voices, /synthesize
│   ├── requirements.txt           # fastapi, uvicorn, supertonic, soundfile, numpy
│   └── Dockerfile                 # Supports $PORT env var, health check
│
├── DEPLOYMENT.md                  # Step-by-step free hosting guide
└── BLUEPRINT.md                   # This file
```

---

## 4. Page & Feature Map

### Public (no login required)
| Route | Component | Description |
|---|---|---|
| `/` | `LandingComponent` | Hero, demo widget (try TTS), stats bar, features section, CTAs |
| `/about` | `AboutComponent` | Project story, creator info |
| `/contact` | `ContactComponent` | Contact form → saved to `contact_message` DB table |
| `/terms` | `TermsComponent` | Terms of Service with back button |
| `/privacy` | `PrivacyComponent` | Privacy policy |

### Authenticated (JWT required)
| Route | Component | Description |
|---|---|---|
| `/studio` | `StudioComponent` | Full TTS studio: script editor, voice picker, quality/speed/lang controls, generation |
| `/dashboard` | `DashboardComponent` | User's recent projects + quick actions |
| `/history` | `HistoryComponent` | All past generations with playback + download |
| `/projects` | `ProjectsComponent` | Project folders for organizing generations |
| `/settings` | `SettingsComponent` | Account settings |

### Admin only
| Route | Component | Description |
|---|---|---|
| `/admin` | `AdminComponent` | Admin dashboard; backend also exposes `GET /api/admin/metrics` |

---

## 5. TTS Generation — End-to-End Data Flow

### Landing Page Preview (anonymous)
```
User types text + selects voice
    → Angular POST /api/public/tts/preview
        → Rate limit check (5 req/IP/hour, in-memory)
        → SupertonicClient POST http://tts-service:8000/synthesize
            → supertonic-3 model generates WAV bytes
        → Save to generation table (no user_id, status='success')
        → Return audio/wav bytes
    → Angular creates blob URL → native <audio> element plays it
```

### Studio Generation (authenticated)
```
User writes script, picks voice/quality/speed/lang → clicks Generate
    → Angular POST /api/studio/generate (with JWT Bearer token)
        → JWT validated → user_id extracted
        → Usage check: characters_used today < daily_limit (5000)
        → SupertonicClient POST http://tts-service:8000/synthesize
            → voice_id, lang, speed, total_steps (diffusion quality: 4/8/16/32)
        → generation row inserted (with user_id, project_id, status)
        → usage_daily row upserted
        → Return audio/wav bytes
    → Mission Control panel shows progress (elapsed timer)
    → Audio ready → native <audio> plays + download button appears
```

### TTS Service Internals (FastAPI)
```
POST /synthesize
    → Validate SynthRequest (voice_id in M1-M5/F1-F5, lang, speed 0.7-2.0, steps 1-40)
    → engine.get_voice_style(voice_id)
    → engine.synthesize(text, voice_style, lang, speed, total_steps)
    → Write WAV to BytesIO buffer (soundfile, PCM_16)
    → Return Response(audio/wav) with headers:
        X-Audio-Duration, X-Synthesis-Time, Content-Disposition
```

---

## 6. API Surface

### Base URL
- Dev: `http://localhost:8080`
- Prod: `https://<render-service>.onrender.com`

### Public Endpoints (no auth)
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/public/tts/voices` | List all 10 voice presets from DB |
| `POST` | `/api/public/tts/preview` | Anonymous TTS (max 300 chars, 5 req/IP/hr) |
| `POST` | `/api/auth/register` | Create account (`email`, `password`, `displayName`) |
| `POST` | `/api/auth/login` | Returns JWT token |
| `POST` | `/api/public/contact` | Save contact message to DB |
| `POST` | `/api/public/analytics/events` | Batch event tracking |
| `GET` | `/api/public/stats` | `{totalGenerations, totalUsers, totalVisitors}` |

### Authenticated Endpoints (JWT Bearer)
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/studio/generate` | Full TTS generation (max 15,000 chars, 5,000/day limit) |
| `GET` | `/api/studio/projects` | User's projects |
| `GET` | `/api/studio/history` | User's generation history |
| `GET` | `/api/user/profile` | Current user info |
| `PUT` | `/api/user/profile` | Update profile |

### Admin Endpoints (JWT + `is_admin=true`)
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/admin/metrics` | Full business KPIs: visitors, users, generations, voices, devices |

### TTS Service (internal only)
| Method | Path | Description |
|---|---|---|
| `GET` | `:8000/health` | `{status, ready, uptime_seconds, voices_available}` |
| `GET` | `:8000/voices` | Raw voice catalogue (10 entries) |
| `POST` | `:8000/synthesize` | Generate WAV from `{text, voice_id, lang, speed, total_steps}` |
| `POST` | `:8000/estimate` | Estimate generation time without generating |

---

## 7. Database Schema (PostgreSQL)

```sql
app_user          -- id, email, password_hash, display_name, is_admin, created_at
voice             -- id, engine_voice_id (M1-M5/F1-F5), display_name, gender, style_tag, is_available
project           -- id, user_id→app_user, name, created_at
generation        -- id, user_id→app_user (nullable for anon), project_id, voice_id→voice,
                  --   input_text, char_count, audio_path, duration_seconds, status,
                  --   is_liked, stage, estimated_seconds, synthesis_ms, created_at
usage_daily       -- id, user_id, usage_date, characters_used, generation_count  [UNIQUE(user_id, date)]
contact_message   -- id, name, email, message, created_at
interest_signal   -- id, user_id, would_pay, suggested_price_inr, comment, created_at
analytics_session -- id, session_id, anonymous_id, user_id, ip_hash, device_type, browser, referrer, created_at
analytics_event   -- id, session_id→analytics_session, event_name, route, properties (JSONB), created_at
synthesis_metric  -- id, generation_id, voice_id, char_count, quality_steps, speed, synthesis_ms, rtf, created_at
site_daily_stats  -- stat_date (PK), unique_sessions, unique_anon_ids, registered_users,
                  --   new_signups, total_generations, total_chars, page_views, updated_at
```

**Key indexes**: `analytics_event(event_name)`, `analytics_event(created_at)`,
`analytics_session(created_at)`, `analytics_session(anonymous_id)`, `generation(status, created_at)`

---

## 8. Authentication & Security

- **Mechanism**: Stateless JWT (RS256/HS256 via JJWT)
- **Token lifetime**: 24 hours (`expiration-ms: 86400000`)
- **Storage**: Angular stores JWT in `localStorage` via `AuthService`
- **Interceptor**: `JwtInterceptor` auto-attaches `Authorization: Bearer <token>` to all `/api/**` requests
- **Guard**: `AuthGuard` protects `/studio`, `/dashboard`, `/history`, `/projects`, `/settings`, `/admin`
- **Password**: BCrypt hashing
- **CSRF**: Disabled (stateless JWT makes CSRF irrelevant)
- **CORS**: Configured from `${app.allowed-origins}` env var (comma-separated list)
- **Admin check**: At controller level via `appUser.isAdmin()` (not Spring Security roles)

---

## 9. Frontend Architecture

### Theme System
- Two themes: **night** (default, dark purple/indigo) and **day** (light)
- Toggled via `ThemeService` which sets `data-theme` attribute on `<html>`
- All colors via CSS custom properties in `styles.scss`
- Pattern: India-themed geometric SVG background on landing and studio

### Angular Routing (lazy-loaded)
```
/                → LandingComponent     (eager, main bundle)
/login           → LoginComponent       (lazy chunk)
/signup          → SignupComponent      (lazy chunk)
/studio          → StudioComponent      (lazy chunk, auth-gated)
/dashboard       → DashboardComponent   (lazy chunk, auth-gated)
/history         → HistoryComponent     (lazy chunk, auth-gated)
/projects        → ProjectsComponent    (lazy chunk, auth-gated)
/settings        → SettingsComponent    (lazy chunk, auth-gated)
/admin           → AdminComponent       (lazy chunk, admin-gated)
/about           → AboutComponent       (lazy chunk)
/contact         → ContactComponent     (lazy chunk)
/terms           → TermsComponent       (lazy chunk)
/privacy         → PrivacyComponent     (lazy chunk)
```

### Key Frontend Services
| Service | Responsibility |
|---|---|
| `AnalyticsService` | Tracks events (page_view, session_start, generate_clicked, etc.), batches + sends to backend every 8s via `sendBeacon` |
| `ThemeService` | Day/night toggle, persisted to `localStorage` |
| `ToastService` | Global notification toasts |
| `AuthService` | JWT storage, login/logout, `isAuthenticated$` observable |
| `StudioApiService` | Calls `/api/studio/generate`, `/api/public/tts/voices`, `/api/studio/projects` |
| `StudioEstimatorService` | Client-side estimation of generation time before API call |

### Studio Component State Machine
```
idle → [Generate clicked] → processing → [Audio ready] → ready
                         → [Error]     → failed
```
- `generationState: 'idle' | 'processing' | 'ready' | 'failed'`
- `MissionControlComponent` renders during `processing` state (elapsed timer, estimated time, quality info)

---

## 10. Analytics Events Tracked

| Event | Triggered When |
|---|---|
| `session_start` | App first loads (device_type, browser, referrer) |
| `page_view` | Every Angular route change |
| `preview_listen_clicked` | Landing page demo button |
| `voice_previewed` | Studio voice preview play button |
| `generate_clicked` | Studio generate button (charCount, voice, quality, speed, lang) |
| `generate_success` | Audio generation completes (charCount, voice, elapsedMs) |
| `generate_failed` | Generation error (errorStatus, charCount) |
| `audio_downloaded` | Download button clicked |

---

## 11. Configuration Reference

### Spring Boot Profiles
| Profile | When Active | DB |
|---|---|---|
| `local` (default) | `localhost:8080`, local PostgreSQL | `jdbc:postgresql://localhost:5432/postgres` |
| `prod` | Set `SPRING_PROFILES_ACTIVE=prod` | From `DATABASE_URL` env var (Neon) |

### Environment Variables (Production)
| Variable | Used By | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | Must be `prod` |
| `DATABASE_URL` | Backend | JDBC URL for Neon PostgreSQL |
| `DB_USERNAME` | Backend | Neon DB username |
| `DB_PASSWORD` | Backend | Neon DB password |
| `JWT_SECRET` | Backend | HMAC secret, min 32 chars |
| `TTS_ENGINE_URL` | Backend | FastAPI service URL (e.g. `http://oracle-ip:8000`) |
| `ALLOWED_ORIGINS` | Backend | Comma-separated CORS origins |
| `PORT` | TTS service | Port to bind (default 8000) |

---

## 12. Business Logic & Limits

| Rule | Value | Enforced By |
|---|---|---|
| Anonymous preview limit | 5 requests/IP/hour | `TtsController` (in-memory ConcurrentHashMap) |
| Anonymous preview max chars | 300 chars | `TtsController` validation |
| Authenticated daily char limit | 5,000 chars/user/day | `GenerationController` + `usage_daily` table |
| Max chars per single request | 1,000 chars | Config `app.usage.max-request-chars` |
| Studio max chars (client) | 15,000 chars | `StudioComponent.maxChars` (soft UI limit) |
| Draft auto-save | localStorage key `w2v-draft-text` | `StudioComponent.saveDraft()` |
| Anonymous visitor tracking | `localStorage` key `w2v-anon-id` | `AnalyticsService` |

---

## 13. Deployment Architecture (Production)

```
words2voice.in (Cloudflare DNS + CDN)
       │
       ├─── Static assets (JS/CSS/images)
       │    └─► Cloudflare Pages (global CDN, ~20ms India)
       │         Build: ng build --configuration=production
       │         SPA routing: public/_redirects → /* /index.html 200
       │
       └─── API calls (/api/*)
            └─► Render.com Web Service (Singapore, free, ~80ms India)
                 Docker: backend/Dockerfile (JVM: -Xmx350m -UseSerialGC)
                 Profile: SPRING_PROFILES_ACTIVE=prod
                 │
                 ├─── SQL queries
                 │    └─► Neon PostgreSQL (Singapore, free, ~10ms from Render)
                 │
                 └─── TTS synthesis
                      └─► Oracle Cloud Always Free ARM VM (Mumbai, free)
                           4 cores, 24 GB RAM, port 8000
                           Docker: tts-service/Dockerfile
                           Model: supertonic-3 (local, ~2-4 GB RAM)
```

---

## 14. SEO Configuration

- **Domain**: `words2voice.in`
- **index.html** contains:
  - Primary meta: title, description, keywords, robots
  - Open Graph (Facebook/LinkedIn preview)
  - Twitter Card (large image)
  - India geo tags (`geo.region: IN`)
  - `hreflang` for `en-IN` and `hi-IN`
  - Canonical URL
  - Schema.org JSON-LD: `WebApplication`, `FAQPage`, `HowTo`, `Product`, `BreadcrumbList`
- **robots.txt**: Allows all crawlers; disallows `/admin`, `/settings`, `/history`, `/projects`
- **sitemap.xml**: `/`, `/about`, `/contact`, `/privacy`, `/terms`
- **PWA**: `site.webmanifest` with standalone display, categories: `productivity, music, utilities`

---

## 15. Known Constraints & Gotchas

1. **TTS cold start**: Supertonic-3 takes ~30–60 seconds to load on first startup. Spring Boot returns 503 until ready.
2. **Render free tier sleep**: Backend sleeps after 15 min idle. Use UptimeRobot to ping `/actuator/health` every 10 min.
3. **Audio storage**: Generated audio is NOT persisted between requests. Users must download immediately. `/tmp/w2v-audio` on prod is ephemeral.
4. **Neon scale-to-zero**: Neon free tier pauses compute after 5 min. First DB query after idle has ~500ms cold start.
5. **Analytics URL**: `AnalyticsService` uses `environment.apiBaseUrl` — must be set correctly in `environment.prod.ts` before building.
6. **Schema auto-init**: `spring.sql.init.mode: always` means `schema.sql` runs on every backend startup. All `CREATE TABLE IF NOT EXISTS` — safe to repeat.
7. **Admin access**: No admin registration UI. Set `is_admin = true` manually in DB after creating account.
8. **JJWT version**: Uses `0.11.5` (older API) — `Jwts.parser()` not `Jwts.parserBuilder()`.
