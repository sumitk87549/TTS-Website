# TTS Service Blueprint — FastAPI + Supertonic-3

## Technology Stack & Versions

| Dependency     | Version       | Purpose                                        |
| -------------- | ------------- | ---------------------------------------------- |
| Python         | 3.13          | Runtime (local venv), 3.10 (Docker)            |
| FastAPI        | ≥0.115.0      | Async web framework                            |
| Uvicorn        | ≥0.30.0       | ASGI server                                    |
| Supertonic SDK | ≥1.3.1        | TTS engine (`Supertone/supertonic-3` model)     |
| Pydantic       | ≥2.0.0        | Request validation                             |
| NumPy          | ≥1.24.0       | Audio array processing                         |
| SoundFile      | ≥0.12.0       | WAV encoding (`libsndfile1` required)          |

**API Version**: `1.0.0`  
**Server Port**: `8000` (configurable via CLI arg or `$PORT` env var)  
**Binding**: `127.0.0.1` (local), `0.0.0.0` (Docker)  
**Model Cache**: `~/.cache/supertonic3`

---

## Architecture Overview

The TTS Service is a **single-file FastAPI microservice** (`main.py`) that wraps the Supertonic-3 diffusion-based TTS model. It is accessed exclusively by the Spring Boot backend (via `SupertonicClient`) and is never exposed directly to end-users.

### Key Architectural Decisions

| Decision                | Implementation                                                               |
| ----------------------- | ---------------------------------------------------------------------------- |
| Model Loading           | Lazy-loaded in `@app.on_event("startup")` — server binds immediately         |
| Graceful Degradation    | If model fails to load, the service stays up; all synthesis requests get 503 |
| Concurrency             | Single Uvicorn worker (model is not thread-safe)                             |
| Audio Format            | WAV (PCM_16), in-memory encoding via `io.BytesIO()` + `soundfile`           |
| CORS                    | Allow all origins (`*`) — both Spring Boot and Angular access this           |
| API Documentation       | Auto-generated Swagger UI at `GET /docs`                                     |

---

## Directory Structure

```
tts-service/
├── main.py                          # Single-file FastAPI application (221 lines)
├── requirements.txt                 # Python dependencies
├── Dockerfile                       # Production Docker image (Python 3.10-slim)
├── start-tts-service.sh             # Local startup script (activates venv, auto-clears port)
├── setup-tunnel.sh                  # Cloudflare tunnel setup
├── setup-cloudflared-service.sh     # Cloudflare systemd service setup
└── venv/                            # Local Python virtual environment (NOT committed)
```

---

## Global Variables & Constants

| Variable          | Type                        | Description                                        |
| ----------------- | --------------------------- | -------------------------------------------------- |
| `app`             | `FastAPI`                   | Application instance (title: "Voisetu TTS Service")|
| `tts_engine`      | `TTS \| None`               | Loaded Supertonic-3 model reference                |
| `_start_time`     | `float`                     | `time.time()` at module load — for uptime calc     |
| `VOICE_CATALOGUE` | `list[dict]`                | 10 voice preset definitions                        |
| `VOICE_ID_SET`    | `set[str]`                  | `{"M1","M2","M3","M4","M5","F1","F2","F3","F4","F5"}` |
| `log`             | `logging.Logger`            | Named `"voisetu.tts"`                              |

---

## Voice Catalogue

The service defines 10 built-in voice presets (5 male, 5 female):

| ID | Display Name | Gender | Style Tag          | Description                                           |
| -- | ------------ | ------ | ------------------ | ----------------------------------------------------- |
| M1 | Arjun        | male   | Neutral & Clear    | Calm, authoritative voice ideal for narration          |
| M2 | Vikram       | male   | Deep & Warm        | Rich, deep voice with warm conversational tone         |
| M3 | Rohan        | male   | Expressive         | Energetic voice with natural expressiveness            |
| M4 | Dev          | male   | Professional       | Crisp voice suited for announcements                   |
| M5 | Karan        | male   | Young & Casual     | Youthful, friendly voice with casual delivery          |
| F1 | Priya        | female | Soft & Melodic     | Warm, melodic voice perfect for storytelling            |
| F2 | Ananya       | female | Confident          | Clear female voice great for presentations             |
| F3 | Neha         | female | Expressive         | Expressive voice with natural cadence                  |
| F4 | Kavya        | female | Young & Bright     | Bright, cheerful voice with modern feel                |
| F5 | Meera        | female | Calm & Soothing    | Smooth voice ideal for meditation/long-form content    |

> **Note**: The FastAPI service and the DB `data.sql` seed have different display names for the same IDs — the FastAPI catalogue names are what's sent to the Supertonic engine via `get_voice_style()`, while the DB names are what's shown in the Angular UI.

---

## Pydantic Request Model

### `SynthRequest(BaseModel)`

| Field              | Type              | Default  | Constraints / Validation                              |
| ------------------ | ----------------- | -------- | ----------------------------------------------------- |
| `text`             | `str`             | required | `min_length=1`, `max_length=2000`                     |
| `voice_id`         | `str`             | `"M1"`   | Must be in `VOICE_ID_SET`                              |
| `lang`             | `Optional[str]`   | `None`   | One of 32 ISO codes + `"na"` (auto-detect/Hinglish)   |
| `speed`            | `float`           | `1.0`    | `ge=0.7, le=2.0`                                      |
| `total_steps`      | `int`             | `8`      | `ge=1, le=40` (diffusion steps — higher = better quality but slower) |
| `silence_duration` | `float`           | `0.3`    | `ge=0.0, le=2.0` (silence between chunks in seconds)  |

**Supported Languages**: `en, ko, ja, ar, bg, cs, da, de, el, es, et, fi, fr, hi, hr, hu, id, it, lt, lv, nl, pl, pt, ro, ru, sk, sl, sv, tr, uk, vi, na`

---

## API Endpoints

### `GET /health` — Liveness Check
**Auth**: None  
**Response** (200):
```json
{
  "status": "ok",           // "ok" or "loading"
  "engine": "supertonic-3",
  "ready": true,            // false while model is loading
  "uptime_seconds": 1234.5,
  "voices_available": 10
}
```
Used by: Spring Boot `TtsEngineHealthIndicator`, Docker HEALTHCHECK, load balancers.

### `POST /estimate` — Generation Time Estimate
**Auth**: None  
**Body**: `SynthRequest`  
**Response** (200):
```json
{ "estimated_seconds": 12 }
```
**Formula**: `base(4) + (len(text) × 0.03 × quality_factor)` where quality_factor = `{4: 0.55, 8: 1.0, 16: 1.8, 32: 3.5}`.

### `GET /voices` — Voice Catalogue
**Auth**: None  
**Response** (200): Array of voice objects from `VOICE_CATALOGUE`.

### `POST /synthesize` — Generate TTS Audio
**Auth**: None (called by Spring Boot backend, not directly by users)  
**Body**: `SynthRequest`  
**Response** (200): Raw WAV audio bytes  
**Content-Type**: `audio/wav`  
**Custom Headers**:
- `X-Audio-Duration`: Generated audio duration in seconds (e.g. `"3.456"`)
- `X-Synthesis-Time`: Processing time in seconds (e.g. `"8.123"`)
- `Content-Disposition`: `attachment; filename="words2voice_audio.wav"`

**Internal Flow**:
1. Calls `get_engine()` — returns `tts_engine` or raises HTTP 503.
2. Gets voice style: `engine.get_voice_style(voice_id)`.
3. Resolves language: `lang or "na"` (auto-detect for Hinglish).
4. Calls `engine.synthesize(text, voice_style, lang, speed, total_steps, silence_duration)` → returns `(wav_ndarray, duration)`.
5. Encodes to WAV: `soundfile.write(buf, wav.squeeze(), engine.sample_rate, format="WAV", subtype="PCM_16")`.
6. Logs RTF (Real-Time Factor): `synthesis_time / audio_duration`.
7. Returns `Response(content=buf.read(), media_type="audio/wav", headers=...)`.

**Error Responses**:
- `422`: Validation error (invalid voice_id, text too long, etc.)
- `500`: Synthesis engine internal failure
- `503`: Engine not loaded yet

### `GET /docs` — Swagger UI
Auto-generated interactive API documentation.

---

## Startup Lifecycle

```
1. Module loads → FastAPI app created, CORS middleware added
2. @app.on_event("startup"):
   └── from supertonic import TTS
   └── tts_engine = TTS(model="supertonic-3")     # Downloads/loads model from cache
   └── Logs: "✅ Supertonic-3 ready in X.Xs"
   └── On failure: logs error, tts_engine stays None → all /synthesize → 503
3. Uvicorn starts listening on port 8000
```

---

## Error Handling

### Global Exception Handler
```python
@app.exception_handler(Exception)
async def generic_error_handler(request, exc):
    return JSONResponse(status_code=500, content={"detail": "Internal server error"})
```
Catches all unhandled exceptions and returns a safe 500 response. Logs full traceback.

---

## Deployment

### Local Development
```bash
./start-tts-service.sh          # Uses supertonic-env venv at project root
./start-tts-service.sh 8001     # Custom port
```
The script:
1. Validates `supertonic-env` venv exists at `${PROJECT_ROOT}/supertonic-env`.
2. Auto-kills any process on the target port using `fuser`.
3. Runs `uvicorn main:app --host 127.0.0.1 --port 8000 --log-level info`.

### Docker
```dockerfile
FROM python:3.10-slim
# Installs: build-essential, libsndfile1, curl
# Non-root user (UID 1000) for security + HF Spaces compatibility
# HEALTHCHECK: curl /health → checks "ready":true, start-period=120s
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT} --workers 1 --timeout-keep-alive 65"]
```

**Environment Variables** (Docker):
- `PORT` (default `8000`)
- `HF_HOME` — Hugging Face model cache directory
- `PYTHONUNBUFFERED=1` — immediate log output

### Cloudflare Tunnel
Scripts `setup-tunnel.sh` and `setup-cloudflared-service.sh` configure a Cloudflare tunnel for exposing the local service to the internet (used for production deployment on a personal machine).

---

## Performance Characteristics

| Metric                    | Approximate Value                |
| ------------------------- | -------------------------------- |
| Model load time (cold)    | 5–15 seconds                     |
| Model load time (cached)  | 2–5 seconds                      |
| Synthesis speed (8 steps) | ~1.0× real-time on modern CPU    |
| Synthesis speed (32 steps)| ~3.5× slower than 8-step         |
| Memory footprint          | ~2–4 GB (model dependent)        |
| Max concurrent requests   | 1 (single worker)                |
| Request timeout           | 120 seconds (set in Spring Boot) |
