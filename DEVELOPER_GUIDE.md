# LyricMind — Developer Guide

Everything you need to run, configure, and deploy LyricMind from scratch.

---

## Table of Contents

1. [Required Services & Accounts](#1-required-services--accounts)
2. [Environment Variables Reference](#2-environment-variables-reference)
3. [MongoDB Atlas Setup](#3-mongodb-atlas-setup)
4. [OpenAI Setup](#4-openai-setup)
5. [Local Development](#5-local-development)
6. [Loading Song Data](#6-loading-song-data)
7. [Performance Configuration](#7-performance-configuration)
8. [Docker Build](#8-docker-build)
9. [Render Deployment](#9-render-deployment)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Required Services & Accounts

| Service | Required? | Purpose | Free Tier? |
|---|---|---|---|
| **MongoDB Atlas** | **Mandatory** | Document store + vector search index | Yes (M0 cluster) |
| **OpenAI API** | **Mandatory** | Embeddings (`text-embedding-3-large`) + reranking (`gpt-4o-mini`) | Pay-per-use |
| **Render** | Optional | Backend hosting (Docker) | Yes (with cold starts) |
| Java 21+ | Mandatory | Run the Spring Boot backend | Free (OpenJDK) |
| Node.js 18+ | Mandatory | Build and run the React frontend | Free |

---

## 2. Environment Variables Reference

### Backend (Spring Boot)

Set these as shell exports for local dev, or as Render environment variables for deployment.

| Variable | Required | Default | Description |
|---|---|---|---|
| `SPRING_DATA_MONGODB_URI` | **Yes** | — | MongoDB Atlas connection string |
| `SPRING_DATA_MONGODB_DATABASE` | No | `lyricmind` | Database name |
| `SPRING_AI_OPENAI_API_KEY` | **Yes** | — | OpenAI secret key (`sk-...`) |
| `SPRING_AI_OPENAI_EMBEDDING_MODEL` | No | `text-embedding-3-large` | Embedding model |
| `SPRING_AI_OPENAI_CHAT_MODEL` | No | `gpt-4o-mini` | Chat/reranking model |
| `SPRING_AI_VECTOR_COLLECTION` | No | `lyricmind_vector_store` | MongoDB vector collection name |
| `SPRING_AI_VECTOR_INDEX_NAME` | No | `lyricmind_vector_index` | Atlas vector search index name |
| `SPRING_AI_VECTOR_PATH_NAME` | No | `embedding` | Field name for the embedding vector |
| `SPRING_AI_VECTOR_INIT_SCHEMA` | No | `false` | Auto-create vector index (see note below) |
| `PORT` | No | `8080` | HTTP port (injected automatically by Render) |

> **`SPRING_AI_VECTOR_INIT_SCHEMA`**: Setting this to `true` causes Spring AI to attempt to create the vector search index automatically. This can conflict with manually created Atlas indexes. Leave it `false` and create the index manually (see [Section 3](#3-mongodb-atlas-setup)).

### Frontend (Vite)

| Variable | Required | Default | Description |
|---|---|---|---|
| `VITE_API_URL` | No | `http://localhost:8080` | Backend base URL |

Set in `frontend/.env` for local overrides. Prefix must be `VITE_` for Vite to expose it to the browser.

---

## 3. MongoDB Atlas Setup

### 3.1 Create a Cluster

1. Go to [cloud.mongodb.com](https://cloud.mongodb.com) and sign in / create an account.
2. Create a new **free M0 cluster** (any region — pick one close to your deployment region).
3. Under **Database Access**, create a user with **Read and write to any database** privilege.
4. Under **Network Access**, add your IP address (or `0.0.0.0/0` for development).

### 3.2 Get the Connection String

1. Click **Connect** on your cluster → **Drivers** → copy the connection string.
2. Replace `<username>` and `<password>` with your DB user credentials.
3. Append the database name: `.../lyricmind?retryWrites=true&w=majority`

```
mongodb+srv://<user>:<password>@<cluster>.mongodb.net/lyricmind?retryWrites=true&w=majority
```

Set this as `SPRING_DATA_MONGODB_URI`.

### 3.3 Create the Vector Search Index

This **must be done before running the ingestion endpoint**. Spring AI does not create the index automatically in this project (`initialize-schema=false`).

1. In the Atlas UI, go to your cluster → **Atlas Search** → **Create Search Index**.
2. Select **Vector Search**.
3. Choose the `lyricmind` database and `lyricmind_vector_store` collection.
4. Paste the following index definition:

```json
{
  "fields": [
    {
      "numDimensions": 3072,
      "path": "embedding",
      "similarity": "cosine",
      "type": "vector"
    }
  ]
}
```

5. Name the index **`lyricmind_vector_index`** (must match `SPRING_AI_VECTOR_INDEX_NAME`).
6. Click **Create**. The index takes ~1 minute to build.

> **Why `numDimensions: 3072`?** The `text-embedding-3-large` model produces 3072-dimensional vectors. This must match exactly.
>
> **Why `cosine`?** Cosine similarity is appropriate for semantic text embeddings — it measures angular distance, which is orientation-based and works well regardless of vector magnitude.

---

## 4. OpenAI Setup

1. Go to [platform.openai.com](https://platform.openai.com) and sign in / create an account.
2. Navigate to **API Keys** → **Create new secret key**.
3. Copy the key (shown only once) and set it as `SPRING_AI_OPENAI_API_KEY`.
4. Ensure your account has a payment method or credits — both models used have per-token costs:
   - `text-embedding-3-large`: ~$0.13 per 1M tokens (embedding at ingest + each query)
   - `gpt-4o-mini`: ~$0.15 input / $0.60 output per 1M tokens (reranking per request)

> **Typical cost per recommendation request**: < $0.001 (sub-cent) for warm cache misses.

---

## 5. Local Development

### Backend

```bash
# 1. Set environment variables
export SPRING_DATA_MONGODB_URI='mongodb+srv://...'
export SPRING_AI_OPENAI_API_KEY='sk-...'

# 2. Run the backend
cd backend
./mvnw spring-boot:run
```

Backend starts on `http://localhost:8080`. Health check: `http://localhost:8080/actuator/health`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:5173` and proxies API calls to `http://localhost:8080` by default.

To point the frontend at a deployed backend, create `frontend/.env`:
```
VITE_API_URL=https://your-backend.onrender.com
```

### Run Tests

```bash
cd backend
./mvnw test
```

Expected: 52 tests, 0 failures.

---

## 6. Loading Song Data

Before searching, you need to ingest songs into MongoDB. A sample dataset (`PostMalone.csv`) is included.

```bash
# Ingest the bundled Post Malone dataset
curl -s -X POST http://localhost:8080/api/lyricmind/v1/embeddings/bulk-songs \
  -H "Content-Type: application/json" \
  -d '{"fileName":"PostMalone.csv"}'

# Expected response:
{"numberOfSongs": 123}
```

This call:
1. Parses the CSV
2. Saves songs to the `songs` MongoDB collection
3. Sends each song's text (title + artist + lyrics) to OpenAI for embedding
4. Stores the embedding vectors in `lyricmind_vector_store`
5. Evicts the recommendation cache (so fresh results are available immediately)

**Note:** The first ingest will call OpenAI's embedding API for each song (~123 calls). This takes 30–60 seconds and incurs a small cost ($0.01–$0.05).

---

## 7. Performance Configuration

### Caching (Caffeine)

The recommendation pipeline results are cached in-memory by `(mood, limit)` with a 15-minute TTL.

Configured in `application.properties`:
```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=15m
```

| Config | Meaning | When to change |
|---|---|---|
| `maximumSize=500` | Max cached (mood, limit) pairs | Increase if serving many distinct mood queries |
| `expireAfterWrite=15m` | Cache TTL | Decrease if you ingest new songs frequently |

Cache is automatically evicted on `POST /api/lyricmind/v1/embeddings/bulk-songs`.

### Reranking

The reranking prompt asks GPT to select exactly `limit` songs from `limit + min(limit, 5)` candidates. This means:
- `limit=5` → GPT selects 5 from 10 candidates
- `limit=10` → GPT selects 10 from 15 candidates

GPT output token count is O(limit), not O(topK) — this is intentional and important for latency.

### Similarity Threshold

The vector search similarity threshold is `0.6` (configurable in `SemanticQueryComponent`). Lower values return more candidates but with weaker semantic match. Raise to `0.7–0.8` if you're getting irrelevant results.

---

## 8. Docker Build

```bash
cd backend

# Build image
docker build -t lyricmind-backend .

# Run locally
docker run --rm -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI='mongodb+srv://...' \
  -e SPRING_AI_OPENAI_API_KEY='sk-...' \
  lyricmind-backend
```

The Dockerfile uses a multi-stage build:
- **Stage 1** (`maven:3.9.9-eclipse-temurin-21`): compiles and packages the JAR
- **Stage 2** (`eclipse-temurin:21-jre`): runs the JAR in a minimal JRE image

Skipping tests during image build (`-DskipTests`) is intentional — tests require real MongoDB and OpenAI credentials.

---

## 9. Render Deployment

### Backend

1. Push repository to GitHub.
2. Create a new **Web Service** on [render.com](https://render.com).
3. Connect your GitHub repository.
4. Settings:
   - **Runtime**: Docker
   - **Root Directory**: `backend`
   - **Dockerfile Path**: `Dockerfile`
5. Add environment variables:
   - `SPRING_DATA_MONGODB_URI` — **required**
   - `SPRING_AI_OPENAI_API_KEY` — **required**
   - `SPRING_DATA_MONGODB_DATABASE` — optional, default `lyricmind`

> **Free vs Paid tier**: Render's free tier spins down after 15 minutes of inactivity, causing 30–60s cold starts. For production use or demos, upgrade to the paid Starter plan ($7/month) for always-on instances.

> **Region alignment**: For lowest latency, deploy the Render service in the same region as your MongoDB Atlas cluster (e.g., both in `us-east-1`).

### Frontend

Deploy as static files to Vercel, Netlify, or GitHub Pages:

```bash
cd frontend
npm run build
# Deploy the dist/ directory
```

Set `VITE_API_URL` to your Render backend URL in the hosting platform's environment settings.

---

## 10. Troubleshooting

| Error | Likely Cause | Fix |
|---|---|---|
| `Port 8080 already in use` | Another process on the port | `lsof -ti:8080 \| xargs kill` |
| `Failed to configure TLS for MongoDB` | JVM SSL issue | Ensure Java 21 and a valid Atlas URI |
| MongoDB connection timeout | IP not whitelisted in Atlas | Add IP in Atlas → Network Access |
| `ERR_CONNECTION_REFUSED` (frontend) | Backend not running | Start backend first, verify port matches `VITE_API_URL` |
| Vector search returns 0 results | Index not created, or no songs ingested | Create the Atlas vector index (§3.3), then run bulk ingest (§6) |
| `Invalid JSON response from AI model` | GPT returned markdown-wrapped JSON | Already handled by `cleanJsonResponse()` — if still failing, check OpenAI API status |
| Recommendations are stale after ingest | Cache not evicted | Cache is evicted automatically by `@CacheEvict` on the bulk-songs endpoint |
| Cold start delay on first request | Render free tier | Upgrade to Render Starter plan, or send a warm-up ping before demo |