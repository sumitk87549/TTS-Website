# 📨 Apache Kafka Mastery — Words2Voice Edition

> **Goal:** Add event-driven architecture to your TTS platform — the #1 skill MNCs demand.
> **Cost:** ₹0 — Kafka is open source, runs locally via Docker.
> **Prerequisite:** Complete Docker Guide first.

---

## Table of Contents

1. [Why Kafka in Your Project](#1-why-kafka-in-your-project)
2. [Installation (Docker)](#2-installation-docker)
3. [Phase 1 — Core Concepts Hands-On](#3-phase-1--core-concepts-hands-on)
4. [Phase 2 — Integrate Kafka into Backend](#4-phase-2--integrate-kafka-into-backend)
5. [Phase 3 — Async TTS Pipeline](#5-phase-3--async-tts-pipeline)
6. [Phase 4 — Analytics & Audit Events](#6-phase-4--analytics--audit-events)
7. [Phase 5 — Notification Service](#7-phase-5--notification-service)
8. [Phase 6 — Multi-Partition Scaling](#8-phase-6--multi-partition-scaling)
9. [Phase 7 — Kafka on Kubernetes](#9-phase-7--kafka-on-kubernetes)
10. [Phase 8 — Scaling to Friend's Laptop](#10-phase-8--scaling-to-friends-laptop)
11. [MNC Patterns & Interview Prep](#11-mnc-patterns--interview-prep)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Why Kafka in Your Project

### Current Architecture (Synchronous)

```
User clicks "Generate" → Frontend → Backend → TTS Service → Wait 30s → Response
                                                    ↑
                                            User is BLOCKED
```

**Problems:**
- User waits 30+ seconds staring at a spinner
- If TTS service is slow/down, the entire request fails
- No audit trail of who generated what
- Can't do analytics on usage patterns
- Backend threads are tied up waiting for TTS

### With Kafka (Asynchronous, Event-Driven)

```
User clicks "Generate" → Backend → Kafka Topic: "tts-requests"
                          ↓                      ↓
                   Returns immediately    TTS Worker picks it up
                   "Job queued! ID: abc"        ↓
                          ↓               Generates audio
                   User sees progress         ↓
                          ↓            Kafka Topic: "tts-completed"
                   Poll for result            ↓
                          ↓            Backend notifies user
                   Download audio       + logs to "tts-analytics"
```

### What Kafka Adds to Your Project

| Feature | Without Kafka | With Kafka |
|---------|-------------|-----------|
| TTS Generation | Synchronous (30s block) | Async (instant response) |
| Audit Trail | Manual DB logging | Automatic event stream |
| Analytics | Query DB (slow) | Real-time stream processing |
| Failure Handling | Request fails | Retry from queue |
| Scaling TTS | Limited by backend threads | Add more consumers |

---

## 2. Installation (Docker)

### Start Kafka with Docker Compose

Add to your `docker-compose.yml` or create `docker-compose.kafka.yml`:

```yaml
# docker-compose.kafka.yml
services:

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: w2v-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    deploy:
      resources:
        limits:
          memory: 256M

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: w2v-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
    deploy:
      resources:
        limits:
          memory: 512M

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: w2v-kafka-ui
    ports:
      - "9090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: words2voice-local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    depends_on:
      - kafka
    deploy:
      resources:
        limits:
          memory: 256M
```

```bash
# Start Kafka stack
docker compose -f docker-compose.kafka.yml up -d

# Verify
docker compose -f docker-compose.kafka.yml ps

# Open Kafka UI in browser
# http://localhost:9090
```

---

## 3. Phase 1 — Core Concepts Hands-On

### 3.1 — Create Topics for Your Project

```bash
# Enter the Kafka container
docker exec -it w2v-kafka bash

# Create topics
kafka-topics --bootstrap-server localhost:29092 --create \
  --topic tts-requests --partitions 3 --replication-factor 1

kafka-topics --bootstrap-server localhost:29092 --create \
  --topic tts-completed --partitions 3 --replication-factor 1

kafka-topics --bootstrap-server localhost:29092 --create \
  --topic tts-failed --partitions 1 --replication-factor 1

kafka-topics --bootstrap-server localhost:29092 --create \
  --topic user-events --partitions 3 --replication-factor 1

kafka-topics --bootstrap-server localhost:29092 --create \
  --topic analytics --partitions 2 --replication-factor 1

# List all topics
kafka-topics --bootstrap-server localhost:29092 --list

# Describe a topic (see partitions, replicas)
kafka-topics --bootstrap-server localhost:29092 --describe --topic tts-requests
```

**Your Topics:**

| Topic | Purpose | Partitions | Why |
|-------|---------|-----------|-----|
| `tts-requests` | Queued TTS generation jobs | 3 | Parallel processing by multiple TTS workers |
| `tts-completed` | Completed audio notifications | 3 | Backend picks up results |
| `tts-failed` | Failed generation (dead letter) | 1 | Error tracking & retry |
| `user-events` | Login, register, profile update | 3 | Audit trail |
| `analytics` | Page views, button clicks | 2 | Usage analytics |

### 3.2 — Produce & Consume Messages (CLI)

Open two terminals:

**Terminal 1 — Consumer (listens for messages):**
```bash
docker exec -it w2v-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic tts-requests \
  --from-beginning \
  --property print.key=true \
  --property key.separator="|"
```

**Terminal 2 — Producer (sends messages):**
```bash
docker exec -it w2v-kafka kafka-console-producer \
  --bootstrap-server localhost:29092 \
  --topic tts-requests \
  --property parse.key=true \
  --property key.separator="|"
```

Type in Terminal 2:
```
user123|{"text":"नमस्ते","voice_id":"M1","quality":8}
user456|{"text":"Hello World","voice_id":"F1","quality":16}
```

Watch Terminal 1 — messages appear instantly!

### 3.3 — Understand Consumer Groups

```bash
# Start consumer in group "tts-workers"
docker exec -it w2v-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic tts-requests \
  --group tts-workers

# Check consumer group status
docker exec -it w2v-kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --describe --group tts-workers
```

**Key concept:** Multiple consumers in the same group = work is divided (each message goes to ONE consumer). Different groups = each gets ALL messages (broadcast).

---

## 4. Phase 2 — Integrate Kafka into Backend

### 4.1 — Add Spring Kafka Dependency

Add to `backend/pom.xml`:

```xml
<!-- Apache Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 4.2 — Kafka Configuration

Add to `application.yml` (under the `local` profile):

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      group-id: w2v-backend
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: "com.voisetu.backend.dto.*,com.voisetu.backend.event.*"
```

### 4.3 — Create Event Classes

```java
// src/main/java/com/voisetu/backend/event/TtsRequestEvent.java
package com.voisetu.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequestEvent {
    private String jobId;
    private String userId;
    private String text;
    private String voiceId;
    private String lang;
    private double speed;
    private int totalSteps;
    private Instant createdAt;
}
```

```java
// src/main/java/com/voisetu/backend/event/TtsCompletedEvent.java
package com.voisetu.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsCompletedEvent {
    private String jobId;
    private String userId;
    private String audioPath;
    private double audioDuration;
    private double synthesisTime;
    private Instant completedAt;
}
```

```java
// src/main/java/com/voisetu/backend/event/UserActivityEvent.java
package com.voisetu.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityEvent {
    private String userId;
    private String action;    // LOGIN, REGISTER, GENERATE_TTS, etc.
    private String details;
    private String ipAddress;
    private Instant timestamp;
}
```

### 4.4 — Kafka Producer Service

```java
// src/main/java/com/voisetu/backend/service/KafkaProducerService.java
package com.voisetu.backend.service;

import com.voisetu.backend.event.TtsRequestEvent;
import com.voisetu.backend.event.TtsCompletedEvent;
import com.voisetu.backend.event.UserActivityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTtsRequest(TtsRequestEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send("tts-requests", event.getUserId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send TTS request to Kafka: {}", ex.getMessage());
            } else {
                log.info("TTS request queued | jobId={} partition={} offset={}",
                    event.getJobId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }

    public void sendTtsCompleted(TtsCompletedEvent event) {
        kafkaTemplate.send("tts-completed", event.getUserId(), event);
    }

    public void sendUserActivity(UserActivityEvent event) {
        kafkaTemplate.send("user-events", event.getUserId(), event);
    }
}
```

### 4.5 — Kafka Consumer (TTS Worker)

```java
// src/main/java/com/voisetu/backend/service/TtsKafkaConsumer.java
package com.voisetu.backend.service;

import com.voisetu.backend.client.SupertonicClient;
import com.voisetu.backend.event.TtsRequestEvent;
import com.voisetu.backend.event.TtsCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsKafkaConsumer {

    private final SupertonicClient supertonicClient;
    private final KafkaProducerService kafkaProducer;
    private final AudioStorageService audioStorage;

    @KafkaListener(
        topics = "tts-requests",
        groupId = "tts-workers",
        concurrency = "2"          // 2 threads consuming in parallel
    )
    public void processTtsRequest(TtsRequestEvent event) {
        log.info("Processing TTS job | jobId={} text={}...",
            event.getJobId(), event.getText().substring(0, Math.min(30, event.getText().length())));

        try {
            long start = System.currentTimeMillis();

            // Call TTS engine
            byte[] audioBytes = supertonicClient.synthesize(
                event.getText(),
                event.getVoiceId(),
                event.getLang(),
                event.getSpeed(),
                event.getTotalSteps()
            );

            double synthesisTime = (System.currentTimeMillis() - start) / 1000.0;

            // Save audio file
            String audioPath = audioStorage.save(event.getJobId(), audioBytes);

            // Publish completion event
            kafkaProducer.sendTtsCompleted(TtsCompletedEvent.builder()
                .jobId(event.getJobId())
                .userId(event.getUserId())
                .audioPath(audioPath)
                .synthesisTime(synthesisTime)
                .completedAt(Instant.now())
                .build());

            log.info("TTS job completed | jobId={} time={}s", event.getJobId(), synthesisTime);

        } catch (Exception ex) {
            log.error("TTS job failed | jobId={} error={}", event.getJobId(), ex.getMessage());
            // Message goes to dead letter topic (configure DLT in production)
        }
    }
}
```

### 4.6 — Wire It Into the TTS Controller

Update your existing TTS generation flow to publish to Kafka:

```java
// In TtsController.java or TtsGenerationService.java — add async endpoint

@PostMapping("/api/tts/generate-async")
public ResponseEntity<?> generateAsync(
        @RequestBody TtsGenerateRequest request,
        @AuthenticationPrincipal UserDetails user) {

    String jobId = UUID.randomUUID().toString();

    kafkaProducer.sendTtsRequest(TtsRequestEvent.builder()
        .jobId(jobId)
        .userId(user.getUsername())
        .text(request.getText())
        .voiceId(request.getVoiceId())
        .lang(request.getLang())
        .speed(request.getSpeed())
        .totalSteps(request.getTotalSteps())
        .createdAt(Instant.now())
        .build());

    return ResponseEntity.accepted().body(Map.of(
        "jobId", jobId,
        "status", "QUEUED",
        "message", "Your audio is being generated. Poll /api/tts/status/" + jobId
    ));
}
```

---

## 5. Phase 3 — Async TTS Pipeline

### Full Architecture with Kafka

```
┌──────────┐     POST /generate-async     ┌──────────┐
│ Frontend │ ──────────────────────────→  │ Backend  │
│ (Angular)│ ← 202 Accepted {jobId}       │ (Spring) │
└──────────┘                              └────┬─────┘
     │                                         │
     │  Poll GET /status/{jobId}               │ produce
     │                                         ▼
     │                               ┌─────────────────┐
     │                               │  tts-requests    │
     │                               │  (Kafka Topic)   │
     │                               └────────┬────────┘
     │                                        │ consume
     │                                        ▼
     │                               ┌─────────────────┐
     │                               │  TTS Worker      │
     │                               │  (Consumer)      │
     │                               └────────┬────────┘
     │                                        │ produce
     │                                        ▼
     │                               ┌─────────────────┐
     │                               │  tts-completed   │
     │                               │  (Kafka Topic)   │
     │                               └────────┬────────┘
     │                                        │ consume
     │            ┌───────────────────────────┘
     │            ▼
     │   ┌──────────────┐
     └──→│ Status API   │ → Returns audio URL when ready
         └──────────────┘
```

---

## 6. Phase 4 — Analytics & Audit Events

### Publish User Events from AuthController

```java
// In AuthController.java — after successful login
kafkaProducer.sendUserActivity(UserActivityEvent.builder()
    .userId(user.getUsername())
    .action("LOGIN")
    .details("Successful login")
    .ipAddress(request.getRemoteAddr())
    .timestamp(Instant.now())
    .build());
```

### Analytics Consumer

```java
// src/main/java/com/voisetu/backend/service/AnalyticsConsumer.java
@Service
@Slf4j
public class AnalyticsConsumer {

    @KafkaListener(topics = "user-events", groupId = "analytics")
    public void processUserEvent(UserActivityEvent event) {
        log.info("ANALYTICS | user={} action={} at={}",
            event.getUserId(), event.getAction(), event.getTimestamp());
        // In MNCs: write to Elasticsearch, ClickHouse, or data warehouse
    }

    @KafkaListener(topics = "tts-completed", groupId = "analytics")
    public void processTtsMetrics(TtsCompletedEvent event) {
        log.info("METRICS | jobId={} synthesisTime={}s duration={}s",
            event.getJobId(), event.getSynthesisTime(), event.getAudioDuration());
        // Track: avg synthesis time, total audio generated, popular voices, etc.
    }
}
```

---

## 7. Phase 5 — Notification Service

Create a separate consumer that could send WebSocket updates:

```java
@Service
public class NotificationConsumer {

    @KafkaListener(topics = "tts-completed", groupId = "notifications")
    public void notifyUser(TtsCompletedEvent event) {
        // Send WebSocket notification to frontend
        // In MNCs: push notification, email, Slack webhook, etc.
        log.info("NOTIFY | User {} — audio ready: {}", event.getUserId(), event.getAudioPath());
    }
}
```

**Key concept:** Two consumer groups (`analytics` and `notifications`) both read from `tts-completed`. Each gets ALL messages independently. This is the **pub-sub** pattern.

---

## 8. Phase 6 — Multi-Partition Scaling

### Why Partitions Matter

```
Topic: tts-requests (3 partitions)

Partition 0: [msg1, msg4, msg7, ...]  →  Consumer Thread 1
Partition 1: [msg2, msg5, msg8, ...]  →  Consumer Thread 2
Partition 2: [msg3, msg6, msg9, ...]  →  Consumer Thread 3
```

- Messages with the same key (userId) always go to the same partition → **ordering guaranteed per user**
- More partitions = more parallelism = faster processing

### Experiment — Watch Partition Assignment

```bash
# Open Kafka UI at http://localhost:9090
# Navigate to Topics → tts-requests → Messages

# Produce messages with different keys
docker exec -it w2v-kafka kafka-console-producer \
  --bootstrap-server localhost:29092 \
  --topic tts-requests \
  --property parse.key=true \
  --property key.separator="|"

# Type:
user-A|{"text":"msg1"}
user-B|{"text":"msg2"}
user-A|{"text":"msg3"}
user-C|{"text":"msg4"}
user-A|{"text":"msg5"}

# Notice: all user-A messages are in the SAME partition
```

---

## 9. Phase 7 — Kafka on Kubernetes

### Kafka Kubernetes Manifest

```yaml
# k8s/base/kafka/zookeeper.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zookeeper
  namespace: words2voice
spec:
  replicas: 1
  selector:
    matchLabels:
      app: zookeeper
  template:
    metadata:
      labels:
        app: zookeeper
    spec:
      containers:
        - name: zookeeper
          image: confluentinc/cp-zookeeper:7.5.0
          ports:
            - containerPort: 2181
          env:
            - name: ZOOKEEPER_CLIENT_PORT
              value: "2181"
          resources:
            limits:
              memory: "256Mi"
---
apiVersion: v1
kind: Service
metadata:
  name: zookeeper-svc
  namespace: words2voice
spec:
  selector:
    app: zookeeper
  ports:
    - port: 2181
```

```yaml
# k8s/base/kafka/kafka.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka
  namespace: words2voice
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:7.5.0
          ports:
            - containerPort: 9092
          env:
            - name: KAFKA_BROKER_ID
              value: "1"
            - name: KAFKA_ZOOKEEPER_CONNECT
              value: "zookeeper-svc:2181"
            - name: KAFKA_ADVERTISED_LISTENERS
              value: "PLAINTEXT://kafka-svc:9092"
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
          resources:
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-svc
  namespace: words2voice
spec:
  selector:
    app: kafka
  ports:
    - port: 9092
```

Update your backend ConfigMap:
```yaml
SPRING_KAFKA_BOOTSTRAP_SERVERS: "kafka-svc:9092"
```

---

## 10. Phase 8 — Scaling to Friend's Laptop

### Multi-Broker Kafka Cluster

When you add your friend's PC:

```
Your Laptop:    Kafka Broker 1  +  ZooKeeper  +  Backend  +  Frontend
Friend's PC:    Kafka Broker 2  +  TTS Worker (extra consumer)
```

**Steps:**
1. Both PCs on same network (WiFi/LAN)
2. Run Kafka Broker 2 on friend's PC with unique `KAFKA_BROKER_ID: 2`
3. Point it to your ZooKeeper: `KAFKA_ZOOKEEPER_CONNECT: <YOUR_IP>:2181`
4. Create topics with `--replication-factor 2` — data is replicated across both

```bash
# On friend's laptop
docker run -d \
  --name kafka-broker-2 \
  -e KAFKA_BROKER_ID=2 \
  -e KAFKA_ZOOKEEPER_CONNECT=<YOUR_IP>:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://<FRIEND_IP>:9092 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0
```

Now topics can have replication factor 2 — if one broker dies, no data loss!

---

## 11. MNC Patterns & Interview Prep

### Patterns Used in Production

| Pattern | Implementation | Your Project |
|---------|---------------|-------------|
| **Event Sourcing** | Store every state change as event | TTS request → completed → analytics |
| **CQRS** | Separate read/write models | Write to Kafka, read from DB |
| **Dead Letter Queue** | Failed messages go to DLT | `tts-failed` topic |
| **Consumer Groups** | Parallel processing | `tts-workers`, `analytics` |
| **Pub-Sub** | Multiple consumers per topic | analytics + notifications |
| **Idempotency** | Process message exactly once | Use jobId as dedup key |
| **Backpressure** | Consumers control own speed | Kafka retains messages |

### Interview Questions — Your Answers

| Question | Answer |
|---------|--------|
| Why Kafka over RabbitMQ? | Kafka: persistent log, replay, high throughput. RabbitMQ: simpler, lower latency |
| What's a consumer group? | Consumers sharing work — each partition assigned to one consumer in the group |
| How does Kafka guarantee ordering? | Per-partition ordering. Same key → same partition → same order |
| What happens if a consumer dies? | Kafka rebalances partitions to remaining consumers. Uncommitted messages reprocessed |
| What's offset? | Position in the partition log. Consumers track their offset to know where they left off |
| Exactly-once semantics? | Use idempotent producer + transactional consumer + unique processing keys |

---

## 12. Troubleshooting

```bash
# Kafka not starting
docker logs w2v-kafka
# Common: ZooKeeper not ready yet — add depends_on

# Consumer not receiving messages
docker exec -it w2v-kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 --describe --group tts-workers
# Check LAG column — high lag = consumer is slow

# Topic has no messages
docker exec -it w2v-kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 --topic tts-requests --from-beginning

# Out of memory
# Reduce Kafka heap: KAFKA_HEAP_OPTS="-Xmx256m -Xms128m"

# Check broker health
docker exec -it w2v-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:29092
```

---

## Checkpoint Exercises

- [ ] Start Kafka + ZooKeeper with Docker Compose
- [ ] Create all 5 topics and verify in Kafka UI
- [ ] Produce/consume messages via CLI
- [ ] Add spring-kafka to backend and send a TTS request event
- [ ] Create a consumer that processes TTS requests asynchronously
- [ ] Watch partition assignment in Kafka UI
- [ ] Explain consumer groups vs pub-sub in your own words
- [ ] Describe how you'd scale TTS processing with more consumers

---

> **🎓 You now have hands-on experience with all three technologies!**
> Docker → Kubernetes → Kafka — this is the exact stack most MNCs use.
