# words2voice

A free, hobbyist Hindi text-to-speech platform powered by open-source voice models, designed for creators, storytellers, and indie developers.

## Prerequisites

To run this project locally, you will need:
- **Java 21** (or 17+)
- **Node.js** (v24 recommended, with npm)
- **PostgreSQL** (v14+)
- **Python 3.10+** (for the TTS engine)

## 1. Database Setup

1. Start your local PostgreSQL server.
2. Create a database for the project:
   ```bash
   createdb voisetu
   ```
3. The Spring Boot backend uses `spring.datasource.username=postgres` and `spring.datasource.password=postgres` by default. If your local credentials differ, update them in `backend/src/main/resources/application.yml`.
4. The schema (tables for users, voices, generations, etc.) and seed data (the 10 built-in voices) will be automatically initialized by Spring Boot on startup via `schema.sql` and `data.sql`.

## 2. Start the Voice Engine (Supertonic)

This project relies on the open-source Supertonic Hindi TTS model. It runs as a separate local Python service.

1. Install the engine:
   ```bash
   pip install 'supertonic[serve]'
   ```
2. Start the local server:
   ```bash
   supertonic serve --host 127.0.0.1 --port 7788
   ```

## 3. Start the Backend (Spring Boot)

The backend handles authentication, rate-limiting, dashboard state, and relays synthesis requests to the Supertonic engine.

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. (Optional) Check the configuration in `src/main/resources/application.yml`. Key properties:
   - `supertonic.engine.base-url`: Defaults to `http://127.0.0.1:7788`
   - `app.usage.daily-limit`: Defaults to `5000` (characters per user per day)
   - `app.storage.audio-dir`: Directory where generated WAV files are saved.
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend will start on **port 8080**.

## 4. Start the Frontend (Angular)

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies (ensure `npm` allows postinstall scripts):
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm start
   ```
   The frontend will be available at **http://localhost:4200**.

---

## Operator Stats Queries

If you're the operator of this hobby project, you don't need a heavy admin panel. Connect to your database (`psql -d voisetu`) and run these queries to check your progress:

**1. Total Users**
```sql
SELECT COUNT(*) as total_users FROM app_user;
```

**2. Total Usage (Generations & Characters)**
```sql
SELECT 
  COUNT(*) as successful_generations, 
  COALESCE(SUM(char_count), 0) as total_chars_generated 
FROM generation 
WHERE status = 'success';
```

**3. Willingness-to-Pay Breakdown**
```sql
SELECT 
  would_pay, 
  COUNT(*) as vote_count, 
  ROUND(AVG(suggested_price_inr)) as avg_suggested_price 
FROM interest_signal 
GROUP BY would_pay 
ORDER BY would_pay;
```