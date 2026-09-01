# ☸️ Kubernetes Mastery — Words2Voice Edition

> **Goal:** Deploy your TTS platform on Kubernetes the way MNCs do — on your single laptop.
> **Cost:** ₹0 — Minikube is free and runs locally.
> **Prerequisite:** Complete Docker Guide first. You need working Docker images.

---

## Table of Contents

1. [Why Kubernetes — What Problem It Solves](#1-why-kubernetes--what-problem-it-solves)
2. [Installation — Minikube + kubectl](#2-installation--minikube--kubectl)
3. [Phase 1 — Core Concepts with Your Project](#3-phase-1--core-concepts-with-your-project)
4. [Phase 2 — Deploy PostgreSQL (StatefulSet)](#4-phase-2--deploy-postgresql-statefulset)
5. [Phase 3 — Deploy TTS Service](#5-phase-3--deploy-tts-service)
6. [Phase 4 — Deploy Backend](#6-phase-4--deploy-backend)
7. [Phase 5 — Deploy Frontend + Ingress](#7-phase-5--deploy-frontend--ingress)
8. [Phase 6 — ConfigMaps & Secrets](#8-phase-6--configmaps--secrets)
9. [Phase 7 — Scaling & Self-Healing](#9-phase-7--scaling--self-healing)
10. [Phase 8 — Resource Management](#10-phase-8--resource-management)
11. [Phase 9 — Monitoring & Logging](#11-phase-9--monitoring--logging)
12. [Phase 10 — Scaling to Friend's Laptop](#12-phase-10--scaling-to-friends-laptop)
13. [MNC Patterns & Interview Prep](#13-mnc-patterns--interview-prep)
14. [Troubleshooting Cheat Sheet](#14-troubleshooting-cheat-sheet)

---

## 1. Why Kubernetes — What Problem It Solves

Docker Compose is great for **one machine**. But in a real company:

```
Problem                           Docker Compose     Kubernetes
─────────────────────────────────────────────────────────────────
Auto-restart on crash              ✓ (basic)         ✓ (advanced)
Scale to multiple machines         ✗                  ✓
Rolling updates (zero downtime)    ✗                  ✓
Auto-scaling based on load         ✗                  ✓
Service discovery                  ✓ (basic)         ✓ (advanced)
Load balancing                     ✗                  ✓
Secret management                  ✗                  ✓
Health-based routing               ✗                  ✓
```

**How Kubernetes sees your project:**

```
┌────────────── Kubernetes Cluster ──────────────┐
│                                                 │
│  ┌─── Namespace: words2voice ───────────────┐  │
│  │                                           │  │
│  │  Pod: postgres     ← StatefulSet (1)      │  │
│  │  Pod: tts-service  ← Deployment (1-3)     │  │
│  │  Pod: backend      ← Deployment (2-4)     │  │
│  │  Pod: frontend     ← Deployment (2)       │  │
│  │                                           │  │
│  │  Service: postgres-svc   (ClusterIP)      │  │
│  │  Service: tts-svc        (ClusterIP)      │  │
│  │  Service: backend-svc    (ClusterIP)      │  │
│  │  Service: frontend-svc   (NodePort)       │  │
│  │                                           │  │
│  │  Ingress: w2v-ingress  → frontend-svc     │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

---

## 2. Installation — Minikube + kubectl

### Install kubectl

```bash
# Download kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# Install
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verify
kubectl version --client
```

### Install Minikube

```bash
# Download
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64

# Install
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Start cluster (uses Docker as driver — no VM needed)
minikube start \
  --driver=docker \
  --cpus=4 \
  --memory=3072 \
  --disk-size=20g

# Verify
kubectl cluster-info
kubectl get nodes
```

> **⚠️ Memory note:** With 5.6 GB RAM, allocate 3 GB to Minikube. Close unnecessary
> apps (browsers, IDEs) when running the full stack.

### Enable Essential Addons

```bash
minikube addons enable ingress          # nginx ingress controller
minikube addons enable metrics-server   # for resource monitoring
minikube addons enable dashboard        # web UI
```

---

## 3. Phase 1 — Core Concepts with Your Project

### Key Kubernetes Objects — Mapped to Words2Voice

| K8s Object | What It Is | Your Project Usage |
|-----------|-----------|-------------------|
| **Pod** | Smallest unit — one or more containers | Each service runs in a pod |
| **Deployment** | Manages pod replicas, rolling updates | backend, tts-service, frontend |
| **StatefulSet** | Like Deployment but for stateful apps | PostgreSQL |
| **Service** | Stable network endpoint for pods | How backend finds postgres |
| **ConfigMap** | Non-secret configuration | Spring profiles, feature flags |
| **Secret** | Sensitive data (base64 encoded) | DB password, JWT secret |
| **Ingress** | External HTTP routing | Route `words2voice.local` → frontend |
| **PersistentVolumeClaim** | Disk storage request | PostgreSQL data directory |
| **Namespace** | Virtual cluster isolation | `words2voice` namespace |
| **HPA** | Horizontal Pod Autoscaler | Scale TTS pods on CPU load |

### Project Structure — Create the K8s manifests directory

```bash
mkdir -p k8s/{base,overlays/dev,overlays/prod}
```

```
k8s/
├── base/                    # Shared manifests
│   ├── namespace.yaml
│   ├── postgres/
│   │   ├── statefulset.yaml
│   │   ├── service.yaml
│   │   └── pvc.yaml
│   ├── tts-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── backend/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── frontend/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml
│   └── ingress.yaml
└── overlays/
    ├── dev/                 # Dev-specific overrides
    └── prod/                # Prod-specific overrides
```

---

## 4. Phase 2 — Deploy PostgreSQL (StatefulSet)

### Why StatefulSet for Databases?

Deployments are for **stateless** apps (backend, frontend). Databases need:
- Stable network identity (pod name doesn't change)
- Persistent storage that survives pod restarts
- Ordered startup/shutdown

### Step 1 — Namespace

```yaml
# k8s/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: words2voice
  labels:
    app.kubernetes.io/part-of: words2voice
```

```bash
kubectl apply -f k8s/base/namespace.yaml
```

### Step 2 — Secrets (DB credentials)

```bash
# Create secrets (base64 encoded automatically)
kubectl create secret generic w2v-db-secret \
  --namespace=words2voice \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=0000 \
  --from-literal=POSTGRES_DB=postgres

kubectl create secret generic w2v-jwt-secret \
  --namespace=words2voice \
  --from-literal=JWT_SECRET=6RZGSfFc65TdepCcXHxVaeJfrkvnVVUTOcZUJVfX6fZezoXtVoVtMrGk7FAhKopf
```

### Step 3 — PostgreSQL StatefulSet

```yaml
# k8s/base/postgres/statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: words2voice
  labels:
    app: postgres
    tier: database
spec:
  serviceName: postgres-svc
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
        tier: database
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
              name: pg-port
          envFrom:
            - secretRef:
                name: w2v-db-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 5
            periodSeconds: 10
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 30
            periodSeconds: 15
          volumeMounts:
            - name: pg-data
              mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
    - metadata:
        name: pg-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 2Gi
```

### Step 4 — PostgreSQL Service

```yaml
# k8s/base/postgres/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-svc
  namespace: words2voice
  labels:
    app: postgres
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
      name: pg-port
  clusterIP: None          # Headless service for StatefulSet
```

```bash
kubectl apply -f k8s/base/postgres/
kubectl get pods -n words2voice -w     # Watch pod come up
```

---

## 5. Phase 3 — Deploy TTS Service

### Load Docker Images into Minikube

Minikube has its own Docker daemon. You need to load your images:

```bash
# Option 1: Point your shell to Minikube's Docker
eval $(minikube docker-env)

# Now build directly inside Minikube
docker build -t w2v-tts:1.0 ./tts-service
docker build -t w2v-backend:1.0 ./backend
docker build -t w2v-frontend:1.0 ./frontend

# Option 2: Load from host
# minikube image load w2v-tts:1.0
```

### TTS Deployment

```yaml
# k8s/base/tts-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tts-service
  namespace: words2voice
  labels:
    app: tts-service
    tier: ml-engine
spec:
  replicas: 1                        # Start with 1 (ML model uses lots of RAM)
  selector:
    matchLabels:
      app: tts-service
  template:
    metadata:
      labels:
        app: tts-service
        tier: ml-engine
    spec:
      containers:
        - name: tts
          image: w2v-tts:1.0
          imagePullPolicy: Never      # Use local image (Minikube)
          ports:
            - containerPort: 8000
          env:
            - name: PORT
              value: "8000"
          resources:
            requests:
              memory: "1Gi"
              cpu: "1000m"
            limits:
              memory: "2Gi"
              cpu: "3000m"
          readinessProbe:
            httpGet:
              path: /health
              port: 8000
            initialDelaySeconds: 120   # Model takes time to load
            periodSeconds: 15
          livenessProbe:
            httpGet:
              path: /health
              port: 8000
            initialDelaySeconds: 180
            periodSeconds: 30
```

```yaml
# k8s/base/tts-service/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: tts-svc
  namespace: words2voice
spec:
  selector:
    app: tts-service
  ports:
    - port: 8000
      targetPort: 8000
```

---

## 6. Phase 4 — Deploy Backend

### ConfigMap for Spring Boot

```yaml
# k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: w2v-config
  namespace: words2voice
data:
  SPRING_PROFILES_ACTIVE: "local"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-svc:5432/postgres"
  SUPERTONIC_ENGINE_BASE_URL: "http://tts-svc:8000"
  ALLOWED_ORIGINS: "http://words2voice.local,http://localhost"
```

> **KEY INSIGHT:** Notice `postgres-svc` and `tts-svc` — these are the Kubernetes Service
> names. Kubernetes DNS automatically resolves them to the correct pod IPs. This is
> exactly like Docker Compose service names, but more powerful.

### Backend Deployment

```yaml
# k8s/base/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: words2voice
  labels:
    app: backend
    tier: api
spec:
  replicas: 2                        # 2 replicas for high availability
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0              # Zero downtime deployments
      maxSurge: 1
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
        tier: api
    spec:
      containers:
        - name: backend
          image: w2v-backend:1.0
          imagePullPolicy: Never
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: w2v-config
          env:
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: w2v-db-secret
                  key: POSTGRES_USER
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: w2v-db-secret
                  key: POSTGRES_PASSWORD
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: w2v-jwt-secret
                  key: JWT_SECRET
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "1000m"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
```

```yaml
# k8s/base/backend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-svc
  namespace: words2voice
spec:
  selector:
    app: backend
  ports:
    - port: 8080
      targetPort: 8080
```

---

## 7. Phase 5 — Deploy Frontend + Ingress

### Frontend Deployment

```yaml
# k8s/base/frontend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: words2voice
  labels:
    app: frontend
    tier: web
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
        tier: web
    spec:
      containers:
        - name: frontend
          image: w2v-frontend:1.0
          imagePullPolicy: Never
          ports:
            - containerPort: 80
          resources:
            requests:
              memory: "64Mi"
              cpu: "50m"
            limits:
              memory: "128Mi"
              cpu: "200m"
          readinessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 10
```

```yaml
# k8s/base/frontend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend-svc
  namespace: words2voice
spec:
  selector:
    app: frontend
  ports:
    - port: 80
      targetPort: 80
```

### Ingress (External Access)

```yaml
# k8s/base/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: w2v-ingress
  namespace: words2voice
  annotations:
    nginx.ingress.kubernetes.io/proxy-read-timeout: "180"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
spec:
  ingressClassName: nginx
  rules:
    - host: words2voice.local
      http:
        paths:
          # API routes → backend
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend-svc
                port:
                  number: 8080
          # Actuator → backend
          - path: /actuator
            pathType: Prefix
            backend:
              service:
                name: backend-svc
                port:
                  number: 8080
          # Everything else → frontend
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-svc
                port:
                  number: 80
```

### Access the App

```bash
# Add to /etc/hosts
echo "$(minikube ip) words2voice.local" | sudo tee -a /etc/hosts

# Deploy everything
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/configmap.yaml
kubectl apply -f k8s/base/postgres/
kubectl apply -f k8s/base/tts-service/
kubectl apply -f k8s/base/backend/
kubectl apply -f k8s/base/frontend/
kubectl apply -f k8s/base/ingress.yaml

# Open in browser
# http://words2voice.local

# Or use minikube tunnel for LoadBalancer services
minikube tunnel
```

---

## 8. Phase 6 — ConfigMaps & Secrets

### Update Config Without Redeploying

```bash
# Edit configmap
kubectl edit configmap w2v-config -n words2voice

# Restart pods to pick up changes
kubectl rollout restart deployment backend -n words2voice
```

### Seal Secrets for Git (MNC Pattern)

Never commit secrets to Git. Use Sealed Secrets (optional advanced):

```bash
# For now, use kubectl to create secrets directly
# In MNCs, tools like HashiCorp Vault or AWS Secrets Manager are used
```

---

## 9. Phase 7 — Scaling & Self-Healing

### Manual Scaling

```bash
# Scale backend to 3 replicas
kubectl scale deployment backend -n words2voice --replicas=3

# Watch pods come up
kubectl get pods -n words2voice -w

# Scale back down
kubectl scale deployment backend -n words2voice --replicas=1
```

### Horizontal Pod Autoscaler (HPA)

```yaml
# k8s/base/backend/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: words2voice
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 1
  maxReplicas: 4
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

```bash
kubectl apply -f k8s/base/backend/hpa.yaml

# Watch autoscaler
kubectl get hpa -n words2voice -w

# Generate load to test autoscaling
# (Run this in another terminal)
kubectl run -n words2voice load-test --rm -i --tty \
  --image=busybox -- sh -c "while true; do wget -q -O- http://backend-svc:8080/actuator/health; done"
```

### Self-Healing — Test It

```bash
# Delete a pod — Kubernetes recreates it automatically
kubectl delete pod -n words2voice -l app=backend

# Watch it come back
kubectl get pods -n words2voice -w

# Kill the process inside a pod — liveness probe restarts it
kubectl exec -n words2voice deploy/backend -- kill 1
```

### Rolling Updates (Zero Downtime)

```bash
# Update the image (after rebuilding)
kubectl set image deployment/backend backend=w2v-backend:2.0 -n words2voice

# Watch the rolling update
kubectl rollout status deployment/backend -n words2voice

# Rollback if something goes wrong
kubectl rollout undo deployment/backend -n words2voice

# View rollout history
kubectl rollout history deployment/backend -n words2voice
```

---

## 10. Phase 8 — Resource Management

### View Resource Usage

```bash
# Pod resource usage
kubectl top pods -n words2voice

# Node resource usage
kubectl top nodes

# Detailed pod info
kubectl describe pod <pod-name> -n words2voice
```

### Resource Quotas (MNC Pattern)

```yaml
# Limit total resources for the namespace
apiVersion: v1
kind: ResourceQuota
metadata:
  name: w2v-quota
  namespace: words2voice
spec:
  hard:
    requests.cpu: "4"
    requests.memory: "4Gi"
    limits.cpu: "6"
    limits.memory: "5Gi"
    pods: "15"
```

---

## 11. Phase 9 — Monitoring & Logging

### Dashboard

```bash
minikube dashboard
# Opens Kubernetes Dashboard in your browser — visual management
```

### Logs

```bash
# Single pod logs
kubectl logs -n words2voice deploy/backend

# Follow logs (real-time)
kubectl logs -n words2voice deploy/backend -f

# Logs from all backend pods
kubectl logs -n words2voice -l app=backend --all-containers

# Previous container logs (after a crash)
kubectl logs -n words2voice <pod-name> --previous
```

### Debug a Pod

```bash
# Get a shell inside a running pod
kubectl exec -it -n words2voice deploy/backend -- /bin/sh

# Run a debug pod in the same network
kubectl run debug -n words2voice --rm -it --image=busybox -- sh
# Inside: wget -qO- http://backend-svc:8080/actuator/health
```

---

## 12. Phase 10 — Scaling to Friend's Laptop

### Option A: k3s (Lightweight — Recommended)

On both machines, install k3s (lightweight Kubernetes):

**Your laptop (master):**
```bash
curl -sfL https://get.k3s.io | sh -

# Get the token
sudo cat /var/lib/rancher/k3s/server/node-token
```

**Friend's laptop (worker):**
```bash
curl -sfL https://get.k3s.io | K3S_URL=https://<YOUR_IP>:6443 \
  K3S_TOKEN=<TOKEN> sh -
```

**Verify:**
```bash
kubectl get nodes
# NAME          STATUS   ROLES                  AGE
# your-laptop   Ready    control-plane,master   5m
# friend-pc     Ready    <none>                 1m
```

Now scale across both machines:
```bash
kubectl scale deployment backend -n words2voice --replicas=4
# Kubernetes automatically distributes pods across both nodes
```

### Option B: Minikube with Multiple Nodes

```bash
minikube start --nodes=2
# Simulates multi-node cluster on one machine
```

---

## 13. MNC Patterns & Interview Prep

### Essential kubectl Commands for Daily Work

```bash
# Quick overview
kubectl get all -n words2voice

# Describe (detailed info + events)
kubectl describe deployment backend -n words2voice

# Watch changes in real-time
kubectl get pods -n words2voice -w

# Port-forward for debugging
kubectl port-forward -n words2voice svc/backend-svc 8080:8080

# Apply all manifests
kubectl apply -f k8s/base/ --recursive

# Delete all resources in a namespace
kubectl delete namespace words2voice
```

### Common Interview Questions — Your Answers

| Question | Your Answer |
|---------|------------|
| Deployment vs StatefulSet? | Deployment for stateless (backend), StatefulSet for stateful (PostgreSQL) — stable identity + persistent storage |
| How does service discovery work? | Kubernetes DNS — `backend-svc.words2voice.svc.cluster.local` resolves to pod IPs |
| What's a rolling update? | Replace pods one-by-one with new version. `maxUnavailable: 0` = zero downtime |
| How do you handle secrets? | K8s Secrets + never commit to Git. MNCs use Vault or cloud KMS |
| What's an Ingress? | L7 load balancer — routes external HTTP to internal services by path/host |
| How does autoscaling work? | HPA watches metrics (CPU/memory), scales replicas between min and max |
| Liveness vs Readiness probe? | Liveness: restart if dead. Readiness: don't send traffic until ready |

---

## 14. Troubleshooting Cheat Sheet

```bash
# Pod stuck in Pending
kubectl describe pod <pod> -n words2voice    # Check Events section

# Pod in CrashLoopBackOff
kubectl logs <pod> -n words2voice --previous  # See crash logs

# Service not reachable
kubectl get endpoints -n words2voice          # Check if endpoints exist
kubectl run debug --rm -it --image=busybox -- wget -qO- http://backend-svc:8080

# Image pull errors
kubectl describe pod <pod> -n words2voice     # Check image name
# Use imagePullPolicy: Never for local images

# Ingress not working
kubectl get ingress -n words2voice
kubectl logs -n ingress-nginx deploy/ingress-nginx-controller

# Resource issues
kubectl top pods -n words2voice
kubectl describe node minikube | grep -A 10 "Allocated resources"
```

---

## Checkpoint Exercises

Before moving to Kafka, make sure you can:

- [ ] Start Minikube and deploy all 4 services
- [ ] Scale backend to 3 replicas and see traffic distributed
- [ ] Delete a pod and watch Kubernetes self-heal
- [ ] Perform a rolling update on the backend
- [ ] Roll back a bad deployment
- [ ] View logs from multiple pods simultaneously
- [ ] Use port-forward to debug a service
- [ ] Explain Deployment vs StatefulSet in your own words

---

> **Next:** Move to `03_KAFKA_GUIDE.md` for event-driven architecture →
