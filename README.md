# Words2Voice — Hindi/Hinglish TTS Platform

A full-stack TTS web app: **Angular** frontend → **Spring Boot** backend → **FastAPI TTS service** (Supertonic-3 on-device).

---

## Architecture

```
Browser (Angular :4200)
        │  JWT in header
        ▼
Spring Boot (:8080)   ──  PostgreSQL
        │  HTTP POST /synthesize
        ▼
FastAPI TTS service (:8000)
        │  Python supertonic SDK
        ▼
supertonic-3 (ONNX, cached at ~/.cache/supertonic3)
```

### Three services to start

| Service | Port | Command |
|---------|------|---------|
| **TTS Service** (Python/FastAPI) | 8000 | `cd tts-service && ../supertonic-env/bin/uvicorn main:app --port 8000` |
| **Backend** (Spring Boot) | 8080 | `cd backend && ./mvnw spring-boot:run` |
| **Frontend** (Angular) | 4200 | `cd frontend && npm run start` |

> **Start in order: TTS → Backend → Frontend.**

---

## Quick Start

### 1 — TTS Service (must start first)

```bash
cd tts-service
./start-tts-service.sh          # or: ../supertonic-env/bin/uvicorn main:app --port 8000
```

First run loads the model from `~/.cache/supertonic3` (~4 seconds). Verify:

```bash
curl http://127.0.0.1:8000/health
# → {"status":"ok","engine":"supertonic-3","ready":true}

curl http://127.0.0.1:8000/voices
# → [{id:"M1",...}, {id:"M2",...}, ..., {id:"F5",...}]
```

### 2 — Spring Boot Backend

```bash
cd backend
./mvnw spring-boot:run
```

Needs PostgreSQL on `localhost:5432` (see `src/main/resources/application.yml`).

### 3 — Angular Frontend

```bash
cd frontend
npm run start
# Open http://localhost:4200
```

---

## TTS Service API

Base URL: `http://127.0.0.1:8000`

### `GET /health`
Returns engine readiness.

### `GET /voices`
Returns all 10 voice presets.

| ID | Name   | Gender | Style |
|----|--------|--------|-------|
| M1 | Arjun  | Male   | Neutral & Clear |
| M2 | Vikram | Male   | Deep & Warm |
| M3 | Rohan  | Male   | Expressive |
| M4 | Dev    | Male   | Professional |
| M5 | Karan  | Male   | Young & Casual |
| F1 | Priya  | Female | Soft & Melodic |
| F2 | Ananya | Female | Confident |
| F3 | Neha   | Female | Expressive |
| F4 | Kavya  | Female | Young & Bright |
| F5 | Meera  | Female | Calm & Soothing |

### `POST /synthesize`

```json
{
  "text": "नमस्ते! Yaar, aaj ka din bahut अच्छा है।",
  "voice_id": "F1",
  "lang": "na",
  "speed": 1.0,
  "total_steps": 8,
  "silence_duration": 0.3
}
```

**Returns:** `audio/wav` binary (44100 Hz, 16-bit mono PCM)

| Field | Default | Range | Notes |
|-------|---------|-------|-------|
| `voice_id` | `M1` | M1–M5, F1–F5 | Voice preset |
| `lang` | `na` | `hi`, `en`, `na` | `na` = auto/Hinglish |
| `speed` | `1.0` | 0.7 – 2.0 | Speed multiplier |
| `total_steps` | `8` | 1 – 40 | Diffusion steps; higher = richer |
| `silence_duration` | `0.3` | 0.0 – 2.0 | Pause between text chunks (s) |

**Example (curl):**

```bash
curl -X POST http://127.0.0.1:8000/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello! आपका स्वागत है।","voice_id":"M1","lang":"na","speed":1.0,"total_steps":8}' \
  -o output.wav
```

**Swagger UI:** http://127.0.0.1:8000/docs

---

## Studio Controls (UI)

Inside the Studio page (`/studio`) after logging in:

| Control | Options | Notes |
|---------|---------|-------|
| **Voice** | M1–M5 (Male), F1–F5 (Female) | Pick a preset voice |
| **Language** | 🌐 Auto · 🇮🇳 Hindi · 🇬🇧 English | `na` handles Hinglish well |
| **Speed** | 0.75× · 0.9× · 1.0× · 1.1× · 1.25× · 1.5× | Speaking rate |
| **Quality** | Draft (4) · Standard (8) · High (16) · Ultra (32) | Diffusion steps |
| **Script Presets** | Story · Promotional · Greeting · News · Hinglish · English | Auto-fills text + language hint |

After generating, the audio player appears in-page with a **Download WAV** button.

---

## Spring Boot → TTS Service Wiring

`SupertonicClient.java` calls `POST /synthesize` with all parameters:

```java
supertonicClient.synthesize(text, voiceId, lang, speed, totalSteps);
// e.g.: synthesize("नमस्ते!", "F1", "hi", 1.0, 8)
```

The backend accepts both `voiceId` (new) and `engineVoiceId` (legacy) in POST `/api/tts/generate`.

---

## Environment

| Path | Python | Key packages |
|------|--------|-------------|
| `supertonic-env/` | 3.13 | supertonic 1.3.1, fastapi 0.139, uvicorn 0.51, soundfile 0.14 |

Model files cached at `~/.cache/supertonic3/` — no internet required after first download.
