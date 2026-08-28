# ─── Voisetu TTS Service — Docker Image ──────────────────────────────────────
# Compatible with: Oracle Cloud Always Free (ARM), Render.com, Hugging Face Spaces
# Port: controlled by $PORT env var (default 8000)
# ─────────────────────────────────────────────────────────────────────────────

FROM python:3.10-slim

WORKDIR /app

# System deps needed for audio processing / ML packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libsndfile1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Non-root user (security + Hugging Face Spaces compatibility)
RUN useradd -m -u 1000 user
USER user

ENV HOME=/home/user \
    PATH=/home/user/.local/bin:$PATH \
    # Default port — override via $PORT env var at runtime
    PORT=8000 \
    # Model cache stays in home dir
    HF_HOME=/home/user/.cache \
    # Disable Python output buffering so logs appear immediately
    PYTHONUNBUFFERED=1

WORKDIR $HOME/app

# Install Python dependencies
COPY --chown=user requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY --chown=user . .

# Expose default port (document only — actual port from $PORT)
EXPOSE 8000

# Health check — waits 60s for model to load, then checks /health every 30s
HEALTHCHECK --start-period=120s --interval=30s --timeout=10s --retries=3 \
  CMD curl -sf http://localhost:${PORT}/health | grep -q '"ready":true' || exit 1

# Start with $PORT env var (default 8000)
# --workers 1  : single worker — TTS model is not thread-safe
# --timeout-keep-alive 65 : keep connection alive for slow TTS generation
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT} --workers 1 --timeout-keep-alive 65"]
