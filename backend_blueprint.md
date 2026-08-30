# Backend Blueprint — Spring Boot

## Technology Stack & Versions

| Dependency             | Version     | Notes                             |
| ---------------------- | ----------- | --------------------------------- |
| Spring Boot            | 4.1.0       | Parent POM                        |
| Java                   | 17          | `java.version` property           |
| Spring Security        | (Boot 4.1)  | JWT + method-level security       |
| Spring Data JDBC       | (Boot 4.1)  | `JdbcTemplate` — no JPA/Hibernate |
| Spring Retry           | 2.0.12      | `@Retryable` on TTS client        |
| Spring Aspects         | 6.2.9       | AOP for retry proxying            |
| PostgreSQL Driver      | (runtime)   | `org.postgresql:postgresql`       |
| JJWT (JSON Web Token)  | 0.11.5      | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` |
| Lombok                 | (optional)  | `@Getter`, `@Setter`, `@Builder`  |
| Hibernate Validator    | (Boot 4.1)  | `spring-boot-starter-validation`  |
| Spring Actuator        | (Boot 4.1)  | Health, info, metrics endpoints   |
| Build Tool             | Maven       | `spring-boot-maven-plugin`        |

**Group ID**: `com.voisetu` | **Artifact ID**: `backend` | **Version**: `0.0.1-SNAPSHOT`  
**Server Port**: `8080` (default)  
**Application Name**: `voisetu-backend`

---

## Configuration (`application.yml`)

### Profile System
- **`local`** (default): Connects to `localhost:5432/postgres`, TTS engine at `http://127.0.0.1:8000`.
- **`prod`**: All secrets from environment variables (`DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `TTS_ENGINE_URL`, `ALLOWED_ORIGINS`). HikariCP pool: max 5, min idle 2.

### Key Configuration Properties (`app.*`)

| Property                      | Default (local)           | Description                                     |
| ----------------------------- | ------------------------- | ----------------------------------------------- |
| `app.allowed-origins`         | `http://localhost:4200,...`| CORS allowed origins (comma-separated)          |
| `app.jwt.secret`              | Hex-encoded default       | HMAC-SHA256 signing key                         |
| `app.jwt.expiration-ms`       | `86400000` (24h)          | JWT token lifetime                              |
| `app.storage.audio-dir`       | `backend-data/audio`      | File system path for WAV storage                |
| `app.usage.daily-limit`       | `5000`                    | Max characters per user per day                 |
| `app.usage.max-request-chars` | `1000`                    | Max characters per single generation request    |
| `app.tts.semaphore-permits`   | `3`                       | Max simultaneous TTS synthesis calls            |
| `supertonic.engine.base-url`  | `http://127.0.0.1:8000`   | FastAPI TTS service URL                         |

### `AppProperties.java` (Typed Config)
All `app.*` properties are bound to a `@ConfigurationProperties(prefix = "app")` POJO with nested static classes: `Jwt`, `Storage`, `Usage`, `Tts`. Uses Lombok `@Getter @Setter`.

### Actuator Endpoints
- `GET /actuator/health` — public (for load balancer).
- `GET /actuator/info` — public (app name, version `1.0.0`).
- `GET /actuator/metrics` — requires ADMIN role.
- Custom indicator: `ttsEngine` → calls `GET /health` on the FastAPI service.

---

## Package Structure

```
com.voisetu.backend/
├── BackendApplication.java          # @SpringBootApplication entry point
├── client/
│   └── SupertonicClient.java        # HTTP client → FastAPI TTS service
├── config/
│   ├── AppProperties.java           # Typed @ConfigurationProperties
│   ├── AsyncConfig.java             # Async thread pool for TTS workers
│   └── TtsEngineHealthIndicator.java# Actuator health indicator
├── controller/                      # REST API endpoints (14 controllers)
│   ├── AdminController.java
│   ├── AnalyticsController.java
│   ├── AuthController.java
│   ├── ContactController.java
│   ├── GenerationController.java
│   ├── HistoryController.java
│   ├── InterestController.java
│   ├── ProjectController.java
│   ├── PublicStatsController.java
│   ├── SiteMetricsController.java
│   ├── TtsController.java
│   ├── UsageController.java
│   ├── UserController.java
│   └── VoiceController.java
├── dto/
│   ├── request/                     # Incoming request DTOs (9 records)
│   └── response/                    # Outgoing response DTOs (7 records)
├── exception/                       # Error handling (8 classes)
│   ├── ApiError.java
│   ├── AppException.java
│   ├── DailyLimitExceededException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── TextTooLongException.java
│   ├── TtsEngineTimeoutException.java
│   ├── TtsEngineUnavailableException.java
│   └── ValidationException.java
├── model/
│   └── AppUser.java                 # Java record: id, email, passwordHash, displayName, isAdmin, createdAt
├── repository/
│   ├── AppUserRepository.java       # User CRUD via JdbcTemplate
│   └── DashboardRepository.java     # Projects, generations, usage via JdbcTemplate
├── security/
│   ├── CustomUserDetailsService.java# Loads user + grants ROLE_ADMIN/ROLE_USER
│   ├── JwtAuthFilter.java           # OncePerRequestFilter — extracts JWT from Authorization header
│   ├── JwtService.java              # Token generation (HS256), validation, claim extraction
│   └── SecurityConfig.java          # Filter chain, CORS, route auth rules, BCrypt
└── service/
    ├── AudioStorageService.java      # Saves WAV bytes to filesystem
    ├── AuthenticatedUserService.java # Resolves userId from Authentication
    ├── ProjectService.java           # Project CRUD (delegates to DashboardRepository)
    ├── RateLimitService.java         # In-memory sliding-window rate limiter
    └── TtsGenerationService.java     # Core TTS orchestration with semaphore
```

---

## REST API Endpoints (Complete)

### Public (No Authentication)

| Method | Path                          | Controller             | Description                           | Rate Limit         |
| ------ | ----------------------------- | ---------------------- | ------------------------------------- | ---------------    |
| POST   | `/api/auth/register`          | `AuthController`       | Create account → returns JWT          | —                  |
| POST   | `/api/auth/login`             | `AuthController`       | Login → returns JWT                   | —                  |
| GET    | `/api/voices`                 | `VoiceController`      | List available voices from DB         | —                  |
| GET    | `/api/public/tts/voices`      | `TtsController`        | List voices (alternative public path) | —                  |
| POST   | `/api/public/tts/preview`     | `TtsController`        | Generate short voice preview          | 30/hour per IP     |
| POST   | `/api/public/contact`         | `ContactController`    | Submit contact form                   | 5/hour per IP      |
| POST   | `/api/public/analytics/events`| `AnalyticsController`  | Batch analytics event ingestion       | —                  |
| GET    | `/api/public/stats`           | `PublicStatsController`| Social proof counters (landing page)  | —                  |

### Authenticated (JWT Required)

| Method | Path                          | Controller              | Description                                  |
| ------ | ----------------------------- | ----------------------- | -------------------------------------------- |
| POST   | `/api/tts/generate`           | `GenerationController`  | Full TTS generation → returns WAV bytes      |
| GET    | `/api/generations/{id}/audio` | `GenerationController`  | Stream saved WAV file                        |
| POST   | `/api/generations/{id}/like`  | `GenerationController`  | Toggle like on a generation                  |
| GET    | `/api/generations`            | `HistoryController`     | Paginated history (`?projectId=&page=&size=`)|
| DELETE | `/api/generations/{id}`       | `HistoryController`     | Delete generation + audio file               |
| GET    | `/api/projects`               | `ProjectController`     | List user's projects                         |
| POST   | `/api/projects`               | `ProjectController`     | Create project                               |
| PATCH  | `/api/projects/{id}`          | `ProjectController`     | Rename project                               |
| DELETE | `/api/projects/{id}`          | `ProjectController`     | Delete project                               |
| GET    | `/api/usage/today`            | `UsageController`       | Today's char usage vs. limit                 |
| GET    | `/api/me`                     | `UserController`        | Get current user profile                     |
| PATCH  | `/api/me`                     | `UserController`        | Update display name                          |
| POST   | `/api/me/change-password`     | `UserController`        | Change password (requires current password)  |
| DELETE | `/api/me`                     | `UserController`        | Delete account + all audio files             |
| POST   | `/api/interest`               | `InterestController`    | Submit monetisation interest signal          |

### Admin (JWT + `ROLE_ADMIN`)

| Method | Path                          | Controller              | Description                                  |
| ------ | ----------------------------- | ----------------------- | -------------------------------------------- |
| GET    | `/api/admin/stats`            | `AdminController`       | Platform KPIs (users, generations, interest) |
| GET    | `/api/admin/top-voices`       | `AdminController`       | Top 20 voices by generation count            |
| GET    | `/api/admin/recent-users`     | `AdminController`       | Last 50 registered users + their stats       |
| GET    | `/api/admin/daily-stats`      | `AdminController`       | Daily generation stats (last 30 days)        |
| GET    | `/api/admin/contacts`         | `AdminController`       | Contact form submissions (last 100)          |
| GET    | `/api/admin/analytics-summary`| `AdminController`       | 7-day event summary + top events             |
| GET    | `/api/admin/metrics`          | `SiteMetricsController` | Full business analytics dashboard data       |

---

## Request / Response DTOs

### Request DTOs (all Java `record` types with Bean Validation)

| DTO                    | Fields                                                                     | Validation                                                    |
| ---------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `TtsGenerateRequest`   | `text`, `voiceId`, `lang?`, `speed?`, `totalSteps?`, `projectId?`          | `@NotBlank text`, `@Size(max=1000)`, `@Pattern voiceId [MF][1-5]`, speed 0.7–2.0, steps 1–40 |
| `TtsPreviewRequest`    | `text`, `voiceId`, `lang?`                                                 | `@NotBlank text`, `@Size(max=300)`, `@Pattern voiceId [MF][1-5]` |
| `RegisterRequest`      | `displayName`, `email`, `password`                                          | name 2–40, `@Email`, password min 8                           |
| `LoginRequest`         | `email`, `password`                                                         | `@NotBlank`, `@Email`                                         |
| `ContactRequest`       | `name`, `email`, `message`                                                  | name max 100, `@Email`, message max 2000                      |
| `InterestRequest`      | `wouldPay`, `suggestedPriceInr?`, `comment?`                                | `@Pattern wouldPay (yes\|no\|maybe)`                          |
| `UpdateProfileRequest` | `displayName?`                                                              | `@Size(2, 40)`                                                |
| `ChangePasswordRequest`| `currentPassword`, `newPassword`                                            | `@NotBlank`, newPassword min 8                                |
| `ProjectRequest`       | `name`                                                                      | `@NotBlank`, `@Size(1, 100)`                                  |

- `TtsGenerateRequest` exposes resolver methods: `resolvedLang()` → `"na"` default, `resolvedSpeed()` → `1.0` default, `resolvedTotalSteps()` → `8` default.
- `@JsonAlias` on `voiceId` accepts: `engineVoiceId`, `engine_voice_id`, `voice_id`.

### Response DTOs

| DTO                   | Fields                                                             |
| --------------------- | ------------------------------------------------------------------ |
| `AuthResponse`        | `token`, `userId`, `displayName`, `isAdmin`                        |
| `VoiceResponse`       | `id`, `engineVoiceId`, `displayName`, `gender`, `styleTag` (dual casing via `@JsonProperty`) |
| `GenerationResponse`  | `id`, `inputText`, `durationSeconds`, `status`, `createdAt`, `isLiked`, `voiceName` |
| `UserProfileResponse` | `email`, `displayName`, `createdAt`, `isAdmin`                     |
| `UsageResponse`       | `charactersUsed`, `generationCount`, `charactersLimit`             |
| `ProjectResponse`     | `id`, `name`, `createdAt`                                          |
| `PagedResponse<T>`    | `content`, `page`, `size`, `totalElements`                         |

---

## Key Services (Detailed)

### `TtsGenerationService`
**The central orchestrator for authenticated TTS generation.**

| Field            | Type         | Description                                              |
| ---------------- | ------------ | -------------------------------------------------------- |
| `ttsSemaphore`   | `Semaphore`  | Fair semaphore, permits = `app.tts.semaphore-permits` (3)|

**`generate(userId, request)`** — Entry point:
1. Validates `text.length > maxRequestChars` → throws `TextTooLongException`.
2. Checks `DashboardRepository.getUsageToday()` → throws `DailyLimitExceededException`.
3. `ttsSemaphore.tryAcquire(3, SECONDS)` → throws `TtsEngineUnavailableException.busy()` if full.
4. Delegates to `doGenerate()` inside `finally { semaphore.release() }`.

**`doGenerate(userId, request, text)`** — `@Transactional`:
1. `resolveVoiceDbId(voiceId)` — maps engine ID (e.g. `"M1"`) to DB PK.
2. `DashboardRepository.createGeneration()` — inserts row with status `'pending'`.
3. `SupertonicClient.synthesize()` — HTTP call to FastAPI.
4. `AudioStorageService.saveWav()` — writes bytes to `{audioDir}/{userId}/{generationId}.wav`.
5. `DashboardRepository.updateGenerationSuccess()` — updates row with `audio_path`.
6. `DashboardRepository.upsertUsage()` — upserts daily usage counter.
7. On failure → `DashboardRepository.updateGenerationFailed()` + re-throws.

### `SupertonicClient`
**HTTP client for the FastAPI TTS microservice** using Java 11 `HttpClient`.

| Config                 | Value           |
| ---------------------- | --------------- |
| Connect timeout        | 10 seconds      |
| Request timeout        | 120 seconds     |
| HTTP version           | HTTP/1.1        |
| Retry                  | 2 attempts max  |
| Retry backoff          | 1.5s × 1.5 multiplier |
| Retried exceptions     | `TtsEngineUnavailableException` only |
| Non-retried exceptions | `TtsEngineTimeoutException` (→ 504), validation errors (422) |

**`synthesize(text, voiceId, lang, speed, totalSteps)`**: Builds JSON manually (no Jackson dependency needed), POSTs to `{baseUrl}/synthesize`, returns raw `byte[]` on 200. Maps 5xx/503 to retryable `TtsEngineUnavailableException`, timeout to `TtsEngineTimeoutException`, 422/400 to non-retryable `RuntimeException`.

**`@PostConstruct checkEngineStatus()`**: On startup, calls `GET /health` to log whether the TTS engine is reachable.

**`@Recover recoverSynthesize(...)`**: Called when all retries are exhausted — re-throws the exception (logs are generated before).

### `RateLimitService`
In-memory sliding-window rate limiter using `ConcurrentHashMap<String, RateLimitEntry>`.
- `isAllowed(clientIp, maxPerHour, context)` → checks/increments counter per IP within a 1-hour window.
- Expired windows are lazily evicted on each call.
- Used by `TtsController` (30 previews/hour) and `ContactController` (5 messages/hour).

### `AudioStorageService`
- `saveWav(userId, generationId, audioBytes)` → creates directory `{audioDir}/{userId}/`, writes `{generationId}.wav`.

### `AuthenticatedUserService`
- `userId(Authentication auth)` → resolves `auth.getName()` (email from JWT) to the DB `app_user.id`.

### `ProjectService`
- CRUD wrapper around `DashboardRepository` project methods.
- Returns typed `ProjectResponse` DTOs.

---

## Security Architecture

### `SecurityConfig`
- **Session**: Stateless (`SessionCreationPolicy.STATELESS`).
- **CSRF**: Disabled (stateless JWT API).
- **CORS**: Dynamic origins from `app.allowed-origins` config.
- **Headers**: X-Frame-Options DENY, X-Content-Type-Options, XSS-Protection.
- **Password**: `BCryptPasswordEncoder`.
- **Exposed CORS headers**: `X-Generation-Id`.

### Route Authorization Rules
1. OPTIONS `/**` → `permitAll()` (CORS preflight).
2. `/api/public/**`, `/api/auth/**`, `/api/voices/**` → `permitAll()`.
3. `/actuator/health`, `/actuator/info` → `permitAll()`.
4. `/actuator/**` → `hasRole('ADMIN')`.
5. `/api/**` → `authenticated()`.
6. Any other → `permitAll()`.

### JWT Flow
1. **Login/Register**: `AuthController` → `authenticationManager.authenticate()` → `JwtService.generateToken()` → return `AuthResponse(token, ...)`.
2. **Every request**: `JwtAuthFilter` (extends `OncePerRequestFilter`) → extracts `Bearer` token → `JwtService.extractUsername()` → `CustomUserDetailsService.loadUserByUsername()` → validates → sets `SecurityContextHolder`.
3. **Algorithm**: HMAC-SHA256.
4. **Subject**: User email.
5. **Expiry**: 24 hours.

### Admin Role
- `CustomUserDetailsService` checks `appUser.isAdmin()` → grants `ROLE_ADMIN` + `ROLE_USER`.
- `AdminController` methods use `@PreAuthorize("hasRole('ADMIN')")`.

---

## Exception Handling (`GlobalExceptionHandler`)

| Exception Type                       | HTTP Status | Error Code               |
| ------------------------------------ | ----------- | ------------------------ |
| `AppException`                       | (dynamic)   | (from exception)         |
| `DailyLimitExceededException`        | 429         | `DAILY_LIMIT_EXCEEDED`   |
| `TextTooLongException`               | 413         | `TEXT_TOO_LONG`          |
| `TtsEngineUnavailableException`      | 503         | `TTS_ENGINE_UNAVAILABLE` |
| `TtsEngineTimeoutException`          | 504         | `TTS_ENGINE_TIMEOUT`     |
| `ResourceNotFoundException`          | 404         | `RESOURCE_NOT_FOUND`     |
| `ValidationException`               | 400         | (from exception)         |
| `MethodArgumentNotValidException`    | 400         | `VALIDATION_ERROR`       |
| `MethodArgumentTypeMismatchException`| 400         | `INVALID_PATH_VARIABLE`  |
| `BadCredentialsException`            | 401         | `INVALID_CREDENTIALS`    |
| `AccessDeniedException`              | 403         | `ACCESS_DENIED`          |
| `Exception` (catch-all)              | 500         | `INTERNAL_ERROR`         |

All errors are serialized as `ApiError` JSON:
```json
{
  "code": "DAILY_LIMIT_EXCEEDED",
  "status": 429,
  "message": "Daily character limit of 5000 exceeded.",
  "details": null,
  "timestamp": "2026-08-30T10:00:00Z"
}
```

The Angular frontend reads `code` to select user-friendly error UX (analogy, quote, action button).

---

## Repository Layer

### `AppUserRepository`
| Method                | SQL                                                      |
| --------------------- | -------------------------------------------------------- |
| `findByEmail(email)`  | `SELECT * FROM app_user WHERE email = ?`                 |
| `findById(id)`        | `SELECT * FROM app_user WHERE id = ?`                    |
| `save(email, hash, name)` | `INSERT INTO app_user ...`                           |
| `updateDisplayName()` | `UPDATE app_user SET display_name = ? WHERE id = ?`      |
| `updatePassword()`    | `UPDATE app_user SET password_hash = ? WHERE id = ?`     |
| `deleteById(id)`      | `DELETE FROM app_user WHERE id = ?`                      |

### `DashboardRepository`
Central repository covering projects, usage, and generations:

**Projects**: `getProjects()`, `createProject()` (returns generated ID), `updateProject()`, `deleteProject()`.

**Usage**: `getUsageToday()` → returns `{characters_used, generation_count}` for today's date. `upsertUsage()` → `INSERT ... ON CONFLICT DO UPDATE` (atomic upsert).

**Generations**: `createGeneration()` (returns ID), `updateGenerationSuccess()`, `updateGenerationFailed()`, `getGenerations()` (with optional projectId filter, pagination), `toggleGenerationLike()`, `getGenerationAudioPath()`, `deleteGeneration()`, `getAllUserAudioPaths()` (for account deletion cleanup).

---

## Concurrency & Async

### TTS Semaphore (`TtsGenerationService`)
- `Semaphore(3, fair=true)` — at most 3 TTS synthesis calls run concurrently.
- Requests that can't acquire a permit in 3 seconds get an immediate `503 TTS_ENGINE_BUSY`.

### Async Thread Pool (`AsyncConfig`)
- **Bean**: `ttsTaskExecutor`
- Core pool: 3, Max pool: 5, Queue capacity: 10
- Thread name prefix: `tts-worker-`
- Graceful shutdown: waits 60 seconds for running tasks.

---

## Seed Data (`data.sql`)
10 voice presets inserted on startup (idempotent via `ON CONFLICT DO NOTHING`):

| ID | Display Name | Gender | Style       |
| -- | ------------ | ------ | ----------- |
| M1 | Rohan        | male   | Calm        |
| M2 | Aryan        | male   | Dynamic     |
| M3 | Kabir        | male   | Steady      |
| M4 | Dev          | male   | Warm        |
| M5 | Vihaan       | male   | High Energy |
| F1 | Isha         | female | Warm        |
| F2 | Meera        | female | Calm        |
| F3 | Priya        | female | Dynamic     |
| F4 | Kavya        | female | High Energy |
| F5 | Naina        | female | Steady      |
