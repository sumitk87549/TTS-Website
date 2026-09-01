# 🐳 Docker Mastery — Words2Voice Edition

> **Goal:** Containerize every service in your TTS platform exactly how MNCs do it.
> **Cost:** ₹0 — Docker Desktop/Engine is free for personal use.
> **Hardware:** Your laptop (8 cores, 5.6 GB RAM) is more than enough.

---

## Table of Contents

1. [What Docker Solves in Your Project](#1-what-docker-solves-in-your-project)
2. [Prerequisites & Installation](#2-prerequisites--installation)
3. [Phase 1 — Single Container Basics](#3-phase-1--single-container-basics)
4. [Phase 2 — Multi-Stage Builds (Production-Grade)](#4-phase-2--multi-stage-builds-production-grade)
5. [Phase 3 — Docker Compose (Full Stack)](#5-phase-3--docker-compose-full-stack)
6. [Phase 4 — Networking Deep Dive](#6-phase-4--networking-deep-dive)
7. [Phase 5 — Volumes & Data Persistence](#7-phase-5--volumes--data-persistence)
8. [Phase 6 — Health Checks & Restart Policies](#8-phase-6--health-checks--restart-policies)
9. [Phase 7 — Docker Registry (Private)](#9-phase-7--docker-registry-private)
10. [Phase 8 — Resource Limits & Security](#10-phase-8--resource-limits--security)
11. [Phase 9 — Scaling with Friend's Laptop](#11-phase-9--scaling-with-friends-laptop)
12. [MNC Patterns You Must Know](#12-mnc-patterns-you-must-know)
13. [Troubleshooting Cheat Sheet](#13-troubleshooting-cheat-sheet)

---

## 1. What Docker Solves in Your Project

Right now your Words2Voice stack looks like this:

```
┌─────────────────────────────────────────────────┐
│                  YOUR LAPTOP                    │
│                                                 │
│  PostgreSQL (bare metal, port 5432)             │
│  Spring Boot JAR (bare metal, port 8080)        │
│  FastAPI/Uvicorn (virtualenv, port 8000)        │
│  Angular (ng serve / nginx, port 4200/80)       │
│  nginx (bare metal reverse proxy)               │
└─────────────────────────────────────────────────┘
```

**Problems this creates in a real job:**
- "Works on my machine" — different Java/Python/Node versions break things
- Installing PostgreSQL differently on every developer's laptop
- No isolation — one service crash can affect others
- Can't replicate production environment locally
- Onboarding a new developer takes hours of setup

**With Docker:**
```
┌─────────────────────────────────────────────────┐
│                  YOUR LAPTOP                     │
│                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ postgres │ │ backend  │ │ tts-svc  │         │
│  │ :5432    │ │ :8080    │ │ :8000    │         │
│  └──────────┘ └──────────┘ └──────────┘         │
│  ┌──────────┐                                    │
│  │ frontend │  ← All isolated, reproducible      │
│  │ :80      │                                    │
│  └──────────┘                                    │
└─────────────────────────────────────────────────┘
```

---

## 2. Prerequisites & Installation

Docker is already installed on your machine (`Docker version 29.7.2`). Verify:

```bash
# Check Docker is running
docker info

# If Docker daemon isn't running:
sudo systemctl start docker
sudo systemctl enable docker

# Add yourself to docker group (avoid sudo every time)
sudo usermod -aG docker $USER
# LOG OUT AND LOG BACK IN after this command

# Verify
docker run hello-world
```

---

## 3. Phase 1 — Single Container Basics

### 3.1 — Containerize PostgreSQL (Your Database)

Instead of bare-metal PostgreSQL, use Docker. This is how every MNC runs databases in dev.

```bash
# Stop your bare-metal PostgreSQL first
sudo systemctl stop postgresql
sudo systemctl disable postgresql

# Run PostgreSQL in Docker
docker run -d \
  --name w2v-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=0000 \
  -e POSTGRES_DB=postgres \
  -p 5432:5432 \
  -v w2v-pgdata:/var/lib/postgresql/data \
  postgres:16-alpine

# Verify it's running
docker ps
docker logs w2v-postgres
```

**What each flag means (learn these — interviewers ask):**

| Flag | Meaning |
|------|---------|
| `-d` | Detached mode (runs in background) |
| `--name` | Human-readable container name |
| `-e` | Environment variable (config without hardcoding) |
| `-p 5432:5432` | Map host port → container port |
| `-v w2v-pgdata:/var/...` | Named volume — data survives container restarts |

**Practice commands:**

```bash
# See running containers
docker ps

# See ALL containers (including stopped)
docker ps -a

# View logs (real-time)
docker logs -f w2v-postgres

# Execute a command inside the container
docker exec -it w2v-postgres psql -U postgres

# Stop / Start / Remove
docker stop w2v-postgres
docker start w2v-postgres
docker rm w2v-postgres          # removes container (volume stays)
docker volume rm w2v-pgdata     # removes data (careful!)
```

### 3.2 — Containerize the TTS Service

You already have a `tts-service/Dockerfile`. Let's build and run it:

```bash
cd /home/sumit/Documents/GitHub/TTS-Website

# Build the image
docker build -t w2v-tts:1.0 ./tts-service

# Run it
docker run -d \
  --name w2v-tts \
  -p 8000:8000 \
  -v ~/.cache/supertonic3:/home/user/.cache/supertonic3:ro \
  w2v-tts:1.0

# Test it
curl http://localhost:8000/health
curl http://localhost:8000/voices
```

### 3.3 — Containerize the Spring Boot Backend

Your current `backend/Dockerfile` is a simple single-stage build. Let's use it first:

```bash
# Build the JAR first (needed by current Dockerfile)
cd backend
./mvnw package -DskipTests
cd ..

# Build Docker image
docker build -t w2v-backend:1.0 ./backend

# Run it (connects to Docker PostgreSQL and TTS service)
docker run -d \
  --name w2v-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/postgres \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=0000 \
  -e SUPERTONIC_ENGINE_BASE_URL=http://host.docker.internal:8000 \
  w2v-backend:1.0

# Test
curl http://localhost:8080/actuator/health
```

> **⚠️ KEY CONCEPT — `host.docker.internal`**
> Containers can't use `localhost` to reach other containers or the host.
> `host.docker.internal` resolves to the host machine's IP from inside a container.
> In production, you use Docker networks instead (covered in Phase 4).

### 3.4 — Containerize the Angular Frontend

Create a new Dockerfile for the frontend:

```bash
cat > frontend/Dockerfile << 'EOF'
# ─── Stage 1: Build Angular ───────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration=production

# ─── Stage 2: Serve with nginx ────────────────────────────────
FROM nginx:alpine
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

# Custom nginx config for SPA routing
RUN echo 'server { \
    listen 80; \
    root /usr/share/nginx/html; \
    index index.html; \
    location /api/ { \
        proxy_pass http://backend:8080/api/; \
        proxy_read_timeout 180s; \
    } \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
EOF
```

```bash
docker build -t w2v-frontend:1.0 ./frontend
docker run -d --name w2v-frontend -p 80:80 w2v-frontend:1.0
```

---

## 4. Phase 2 — Multi-Stage Builds (Production-Grade)

Multi-stage builds are **THE** pattern used in every MNC. They keep images small and secure.

### Upgrade Backend Dockerfile

Replace `backend/Dockerfile` with a production-grade multi-stage build:

```dockerfile
# ─── Stage 1: Build ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q          # Cache deps layer
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user (security best practice — MNCs enforce this)
RUN useradd -m -u 1000 appuser
USER appuser

COPY --from=builder --chown=appuser /app/target/*.jar app.jar

# JVM tuning for containers
ENV JAVA_OPTS="-Xmx350m -Xms64m -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Why multi-stage matters:**
```
Single-stage:  ~800 MB (includes Maven, source code, build tools)
Multi-stage:   ~250 MB (only JRE + your JAR)
```

**Exercise:** Compare image sizes:
```bash
docker build -t w2v-backend:single -f Dockerfile.single ./backend
docker build -t w2v-backend:multi  -f Dockerfile.multi  ./backend
docker images | grep w2v-backend
```

---

## 5. Phase 3 — Docker Compose (Full Stack)

This is what you'll use 90% of the time at any job. One command to start everything.

Create `docker-compose.yml` in the project root:

```yaml
# docker-compose.yml — Words2Voice Full Stack
# Usage: docker compose up -d

services:

  # ── PostgreSQL Database ──────────────────────────────────────
  postgres:
    image: postgres:16-alpine
    container_name: w2v-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: "0000"
      POSTGRES_DB: postgres
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # ── TTS Service (FastAPI) ────────────────────────────────────
  tts-service:
    build: ./tts-service
    container_name: w2v-tts
    ports:
      - "8000:8000"
    volumes:
      - ~/.cache/supertonic3:/home/user/.cache/supertonic3:ro
    environment:
      PORT: "8000"
    healthcheck:
      test: ["CMD", "curl", "-sf", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      start_period: 120s
      retries: 3
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: "4.0"

  # ── Spring Boot Backend ──────────────────────────────────────
  backend:
    build: ./backend
    container_name: w2v-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: local
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/postgres
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: "0000"
      SUPERTONIC_ENGINE_BASE_URL: http://tts-service:8000
      JWT_SECRET: "your-dev-secret-key-min-32-characters-long"
    depends_on:
      postgres:
        condition: service_healthy
      tts-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-sf", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: "2.0"

  # ── Angular Frontend (nginx) ─────────────────────────────────
  frontend:
    build: ./frontend
    container_name: w2v-frontend
    ports:
      - "80:80"
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped

volumes:
  pgdata:
    driver: local
```

### Daily Commands You'll Use at Work

```bash
# Start everything
docker compose up -d

# View all logs
docker compose logs -f

# View logs for one service
docker compose logs -f backend

# Stop everything
docker compose down

# Stop and DELETE all data (fresh start)
docker compose down -v

# Rebuild after code changes
docker compose up -d --build backend

# Check status
docker compose ps

# Scale a service (preview of Kubernetes thinking)
docker compose up -d --scale tts-service=2
```

---

## 6. Phase 4 — Networking Deep Dive

### How Containers Talk to Each Other

Docker Compose auto-creates a network. Services find each other **by service name**:

```
frontend  →  http://backend:8080/api/...
backend   →  http://postgres:5432
backend   →  http://tts-service:8000/synthesize
```

**Experiment — Inspect the network:**

```bash
# See all Docker networks
docker network ls

# Inspect the compose network
docker network inspect tts-website_default

# Ping between containers
docker exec w2v-backend ping postgres
docker exec w2v-backend ping tts-service
```

### Create a Custom Network (MNC pattern)

```bash
# In production, teams isolate services into networks
docker network create --driver bridge w2v-internal
docker network create --driver bridge w2v-public

# Only frontend is on w2v-public
# backend + postgres + tts on w2v-internal
# frontend also on w2v-internal (to reach backend)
```

Update `docker-compose.yml` to use custom networks:

```yaml
networks:
  internal:
    driver: bridge
  public:
    driver: bridge

services:
  postgres:
    networks: [internal]
  tts-service:
    networks: [internal]
  backend:
    networks: [internal]
  frontend:
    networks: [internal, public]
    ports:
      - "80:80"
```

---

## 7. Phase 5 — Volumes & Data Persistence

### Types of Volumes

```bash
# 1. Named Volume (recommended for databases)
docker volume create w2v-pgdata

# 2. Bind Mount (for development — live code reload)
docker run -v /home/sumit/project/src:/app/src myimage

# 3. tmpfs (in-memory, for secrets)
docker run --tmpfs /app/secrets myimage
```

### Exercise — Backup and Restore PostgreSQL

```bash
# Backup
docker exec w2v-postgres pg_dump -U postgres postgres > backup.sql

# Restore to a new container
docker run -d --name w2v-postgres-new \
  -e POSTGRES_PASSWORD=0000 \
  -v w2v-pgdata-new:/var/lib/postgresql/data \
  postgres:16-alpine

docker exec -i w2v-postgres-new psql -U postgres < backup.sql
```

---

## 8. Phase 6 — Health Checks & Restart Policies

Health checks are critical in production. Your compose file already has them. Test:

```bash
# See health status
docker inspect --format='{{.State.Health.Status}}' w2v-backend

# Simulate failure — kill the Java process inside backend
docker exec w2v-backend kill 1

# Watch Docker auto-restart it (because restart: unless-stopped)
watch docker ps
```

---

## 9. Phase 7 — Docker Registry (Private)

MNCs push images to a private registry. You can run one locally for free:

```bash
# Run a local Docker registry
docker run -d -p 5000:5000 --name registry registry:2

# Tag and push your images
docker tag w2v-backend:1.0 localhost:5000/w2v-backend:1.0
docker push localhost:5000/w2v-backend:1.0

# Pull from registry (simulates another machine)
docker pull localhost:5000/w2v-backend:1.0

# List images in your registry
curl http://localhost:5000/v2/_catalog
```

---

## 10. Phase 8 — Resource Limits & Security

### Memory & CPU Limits

```bash
# Run with limits
docker run -d \
  --name w2v-backend \
  --memory=512m \
  --cpus=2.0 \
  w2v-backend:1.0

# Monitor resource usage (like htop for Docker)
docker stats
```

### Security Best Practices (MNC interview questions)

```bash
# 1. Never run as root
USER appuser     # Already in your Dockerfiles ✓

# 2. Scan images for vulnerabilities
docker scout cve w2v-backend:1.0

# 3. Use .dockerignore
cat > backend/.dockerignore << 'EOF'
.git
target/
*.log
.env
node_modules/
EOF

# 4. Don't store secrets in images — use env vars or Docker secrets
docker secret create db_password db_password.txt   # Swarm mode
```

---

## 11. Phase 9 — Scaling with Friend's Laptop

When you add your friend's PC, use **Docker Swarm** (free, built into Docker):

### On YOUR laptop (Manager node):

```bash
# Initialize swarm
docker swarm init --advertise-addr <YOUR_IP>
# This prints a join token — give it to your friend
```

### On FRIEND's laptop (Worker node):

```bash
docker swarm join --token <TOKEN> <YOUR_IP>:2377
```

### Deploy as a Stack:

```bash
# Deploy your compose file as a swarm stack
docker stack deploy -c docker-compose.yml w2v

# Scale TTS across both machines
docker service scale w2v_tts-service=3

# See where containers are running
docker service ps w2v_tts-service
```

---

## 12. MNC Patterns You Must Know

| Pattern | What It Is | Your Project Example |
|---------|-----------|---------------------|
| Multi-stage builds | Small, secure images | Backend Dockerfile |
| Health checks | Auto-detect failures | `/health`, `/actuator/health` |
| Named volumes | Persist data across restarts | PostgreSQL data |
| `.dockerignore` | Keep images lean | Exclude `.git`, `target/` |
| Non-root user | Security compliance | `USER appuser` |
| Resource limits | Prevent OOM kills | `--memory=512m` |
| Docker Compose | Dev environment orchestration | `docker-compose.yml` |
| Private registry | Store team images | `localhost:5000` |
| Log drivers | Centralized logging | `docker logs -f` |
| Restart policies | Self-healing | `restart: unless-stopped` |

---

## 13. Troubleshooting Cheat Sheet

```bash
# Container won't start
docker logs <container>

# Can't connect between containers
docker network inspect <network>
docker exec <container> ping <other-container>

# Out of disk space
docker system prune -a           # Remove ALL unused images/containers
docker volume prune              # Remove unused volumes

# Port already in use
sudo lsof -i :8080
docker ps --format "{{.Names}} {{.Ports}}"

# Image too large
docker history <image>           # See which layer is biggest

# Container keeps restarting
docker inspect <container> | grep -A 5 "RestartCount"
docker logs --tail 50 <container>
```

---

## Checkpoint Exercises

Before moving to Kubernetes, make sure you can:

- [ ] Run `docker compose up -d` and access http://localhost (frontend)
- [ ] Stop a container and watch Docker auto-restart it
- [ ] Exec into the PostgreSQL container and run a SQL query
- [ ] Rebuild only the backend after a code change
- [ ] View resource usage with `docker stats`
- [ ] Push an image to your local registry
- [ ] Explain multi-stage builds in an interview

---

> **Next:** Once comfortable with Docker, move to `02_KUBERNETES_GUIDE.md` →
