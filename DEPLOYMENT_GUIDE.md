# 🚀 Words2Voice: Zero-Cost Deployment Guide (India Optimized)

This guide provides a step-by-step walkthrough to deploy your entire stack (Frontend, Backend, Database, and AI TTS Service) **100% for free**. 

The platforms chosen here are highly reliable, offer generous free tiers, and perform excellently in the Asia/India region, ensuring your web app will be indexed quickly by Google and commercial AIs.

---

## 🏗️ Architecture Overview

| Component | Tech Stack | Hosting Platform | Why? |
| :--- | :--- | :--- | :--- |
| **Database** | PostgreSQL | **Neon.tech** | Free serverless Postgres, fast setup, highly reliable. |
| **AI Model (TTS)** | Python / FastAPI | **Hugging Face Spaces** | Offers free 16GB RAM Docker instances — perfect for AI! |
| **Backend API** | Spring Boot (Java) | **Render.com** | Easiest free tier for Spring Boot web services. |
| **Frontend UI** | Angular | **Vercel** | Lightning-fast Edge network in India, best-in-class SEO. |

---

## 🛑 Prerequisites

Before you start, make sure you have:
1. Created a free **GitHub** account.
2. Pushed your entire `TTS-Website` project to a public or private GitHub repository.

*(If you haven't uploaded your code to GitHub yet, do that first!)*

---

## 🛠️ Step 1: Deploy the Database (Neon.tech)

1. Go to [neon.tech](https://neon.tech) and sign up using your GitHub account.
2. Click **"New Project"**.
   - **Name**: `words2voice-db`
   - **Region**: Choose the closest one to India (e.g., `Asia Pacific (Singapore)`).
   - **Database version**: 15 or 16.
3. Click **"Create Project"**.
4. You will see a connection string that looks like this:
   `postgresql://neondb_owner:password123@ep-cool-snowflake-12345.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`
5. **Save this entire URL securely**. You will need it in Step 3.
6. The backend application will automatically create all the necessary tables using your `schema.sql` when it connects for the first time.

---

## 🧠 Step 2: Deploy the AI TTS Service (Hugging Face)

*(Note: I have already added a `Dockerfile` to your `tts-service` folder to make this work automatically!)*

1. Go to [huggingface.co](https://huggingface.co) and create a free account.
2. Click your profile picture (top right) ➔ **"New Space"**.
   - **Space Name**: `words2voice-tts`
   - **License**: `mit` (or your choice)
   - **Select the Space SDK**: Choose **Docker** ➔ **Blank**.
   - **Space Hardware**: Choose the Free tier (`2 vCPU, 16GB RAM`).
   - Click **"Create Space"**.
3. Once the space is created, you will see instructions to push code. However, the easiest way is via the **Files** tab in the UI or by connecting your GitHub repository.
   - Alternatively, you can copy the contents of your local `tts-service` folder into the Hugging Face space repository directly via their web interface. Ensure `main.py`, `requirements.txt`, and `Dockerfile` are at the root of the space.
4. Hugging Face will automatically detect the `Dockerfile` and start building the AI service.
5. Once the status turns to **"Running"**, click the three dots (`...`) at the top right of the Space ➔ **"Embed this Space"**.
6. Look for the **Direct URL**. It will look something like:
   `https://sumitk-words2voice-tts.hf.space`
7. **Save this URL**. You will need it in Step 3.

---

## ⚙️ Step 3: Deploy the Backend API (Render.com)

1. Go to [render.com](https://render.com) and sign up with GitHub.
2. Click **"New"** ➔ **"Web Service"**.
3. Connect your GitHub account and select your `TTS-Website` repository.
4. Configure the Web Service:
   - **Name**: `words2voice-api`
   - **Region**: `Singapore (Southeast Asia)`
   - **Root Directory**: `backend` *(Crucial: This tells Render to look in the backend folder!)*
   - **Environment**: `Java`
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -Xmx300m -jar target/backend-0.0.1-SNAPSHOT.jar` *(Note: `Xmx300m` keeps memory usage low for the free tier)*
   - **Instance Type**: `Free`
5. Scroll down to **Environment Variables** and add the following:
   - `SPRING_DATASOURCE_URL` ➔ Paste your **Neon Database URL** from Step 1 (Replace `postgresql://` with `jdbc:postgresql://`). 
     *(Example: `jdbc:postgresql://ep-cool-snowflake-12345.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`)*
   - `TTS_SERVICE_URL` ➔ Paste your **Hugging Face Direct URL** from Step 2.
   - `JWT_SECRET` ➔ Type a long, random string (e.g., `s3cr3tKeyF0rW0rds2V01ceS3cur1ty2026!`).
   - `CORS_ALLOWED_ORIGINS` ➔ `https://words2voice.vercel.app` *(We will create this in Step 4)*
6. Click **"Create Web Service"**.
7. Wait a few minutes for the build to finish. Once it's live, copy the URL at the top left (e.g., `https://words2voice-api.onrender.com`).

---

## 💻 Step 4: Deploy the Frontend UI (Vercel)

Before deploying to Vercel, we need to tell Angular where the backend lives.

1. In your local code editor, open `/frontend/src/environments/environment.prod.ts` (If it doesn't exist, create it or just modify `environment.ts`).
2. Change the `apiBaseUrl` to your new Render Backend URL:
   ```typescript
   export const environment = {
     production: true,
     apiBaseUrl: 'https://words2voice-api.onrender.com/api' // Replace with your Render URL
   };
   ```
3. Commit this change and push it to GitHub:
   ```bash
   git add .
   git commit -m "Update API URL for production"
   git push origin main
   ```
4. Go to [vercel.com](https://vercel.com) and sign up with GitHub.
5. Click **"Add New"** ➔ **"Project"**.
6. Import your `TTS-Website` repository from GitHub.
7. Configure the Project:
   - **Project Name**: `words2voice`
   - **Framework Preset**: `Angular` (Vercel should detect this automatically).
   - **Root Directory**: `frontend` *(Click Edit and select the frontend folder)*.
8. Click **"Deploy"**.
9. Vercel will build and launch your website globally! When finished, it will give you a domain like `https://words2voice.vercel.app`.

---

## 🎉 Step 5: Final Testing & SEO

Congratulations! Your entire platform is now deployed for free. 

### Testing the Flow:
1. Open your Vercel URL (`https://words2voice.vercel.app`).
2. Register a new account. (This tests the **Frontend ➔ Backend ➔ Database** connection).
3. Go to the Studio and generate a Hindi audio clip. (This tests the **Backend ➔ Hugging Face AI** connection).

### SEO & Indexing (Getting found by Google & AI):
Your Vercel `.vercel.app` domain is highly respected by search engines and will be indexed quickly.
1. Go to [Google Search Console](https://search.google.com/search-console/about).
2. Add your Vercel URL as a new property.
3. Submit a sitemap (if you generate one) or simply request indexing for the homepage.
4. Because we have already added rich `Schema.org` data, metadata, and "HowTo" JSON-LD tags into your `index.html`, Google, Bing, and AI crawlers (like ChatGPT Web Search or Perplexity) will instantly understand that this is an Indian AI Text-to-Speech platform.

*(Note: Render free instances "spin down" after 15 minutes of inactivity. The first time you load the app after a break, it might take ~45 seconds to wake up. This is normal for free hosting!)*
