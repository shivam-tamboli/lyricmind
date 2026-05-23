# LyricMind

AI-powered mood-based song recommendation system built with React + Spring Boot.

Users describe a mood in natural language. The backend performs semantic vector search, re-ranks candidates with an LLM, and returns ranked recommendations — each with an AI-generated explanation of why it matches.

---

## What This Project Does

- Accepts natural-language mood input (e.g. `"nostalgic and calm"`, `"angry and intense"`)
- Returns 1–10 ranked song recommendations per request
- Explains each recommendation with an AI-generated motivation
- Uses MongoDB Atlas Vector Search (HNSW) for semantic retrieval
- Uses GPT-4o-mini for candidate re-ranking and motivation generation
- Caches results per (mood, limit) to eliminate redundant AI calls
- Includes a "Refine Search" bar to iterate on results without starting over

---

## Architecture

```
React Frontend
    │  POST /api/lyricmind/v1/recommendations
    ▼
RecommendationController
    └── RecommendationService  ← cache hit? return immediately
            ├── SemanticQueryComponent  → MongoDB Atlas vector search
            ├── RerankComponent         → GPT-4o-mini (select top N)
            └── SongRepository          → findAllById() batch lookup
```

Full architecture details: [DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)

---

## Backend Flow (RAG)

1. `POST /api/lyricmind/v1/recommendations` with `mood` and `limit`
2. Cache check — if `(mood, limit)` seen in last 15 min, return instantly
3. `SemanticQueryComponent` embeds the mood query and queries the HNSW vector index  
   (`topK = limit + min(limit, 5)`, similarity threshold `0.6`)
4. `RerankComponent` asks GPT-4o-mini to **select the top `limit`** songs from candidates  
   (output is O(limit) tokens, not O(topK) — intentionally smaller for latency)
5. `RecommendationService` loads full song metadata with a single `findAllById()` batch call
6. Returns `SongRecommendationResponse[]`

If reranking fails, the service falls back to the original vector-search order.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 7 |
| Backend | Java 21, Spring Boot 3.5 |
| AI framework | Spring AI 1.1.2 |
| Embedding model | `text-embedding-3-large` (3072 dims) |
| Reranking model | `gpt-4o-mini` |
| Vector store | MongoDB Atlas (HNSW, cosine similarity) |
| Document store | MongoDB Atlas `songs` collection |
| Cache | Caffeine in-memory (15m TTL) |

---

## API

### Get Recommendations

```
POST /api/lyricmind/v1/recommendations
```

```json
{ "mood": "happy and energetic", "limit": 5 }
```

Response (`200`):
```json
[
  {
    "title": "Sunflower",
    "artist": "Post Malone",
    "album": "Spider-Man: Into the Spider-Verse",
    "genre": "Pop",
    "releaseYear": 2018,
    "motivation": "Upbeat melody perfectly matches your energetic vibe"
  }
]
```

### Ingest Songs from CSV

```
POST /api/lyricmind/v1/embeddings/bulk-songs
```

```json
{ "fileName": "PostMalone.csv" }
```

Response (`201`): `{ "numberOfSongs": 123 }`

A sample dataset is included at `backend/src/main/resources/PostMalone.csv`.

---

## Quick Start

See **[DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)** for complete setup instructions including:
- MongoDB Atlas cluster and vector index creation
- OpenAI API key setup
- All environment variables (required vs optional)
- Local development, Docker build, Render deployment
- Performance tuning and troubleshooting

### TL;DR

```bash
# Backend
export SPRING_DATA_MONGODB_URI='mongodb+srv://...'
export SPRING_AI_OPENAI_API_KEY='sk-...'
cd backend && ./mvnw spring-boot:run

# Frontend (separate terminal)
cd frontend && npm install && npm run dev

# Ingest sample data
curl -s -X POST http://localhost:8080/api/lyricmind/v1/embeddings/bulk-songs \
  -H "Content-Type: application/json" \
  -d '{"fileName":"PostMalone.csv"}'
```

---

## Docker

```bash
cd backend
docker build -t lyricmind-backend .
docker run --rm -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI='...' \
  -e SPRING_AI_OPENAI_API_KEY='sk-...' \
  lyricmind-backend
```

---

## Performance Characteristics

| Scenario | Latency |
|---|---|
| Cache hit (repeat query) | < 5ms |
| limit=1, warm | ~1s |
| limit=5, warm | ~1.5–2s |
| limit=10, warm | ~2.5–3.5s |
| Cold start (Render free tier) | +8–15s first request only |

Primary latency driver is GPT-4o-mini reranking, which scales O(limit) — not O(topK) — due to the "select top N" prompt design. See [DEVELOPER_GUIDE.md § Performance Configuration](./DEVELOPER_GUIDE.md#7-performance-configuration).

---

## Health Check

```bash
curl https://<your-service>.onrender.com/actuator/health
```