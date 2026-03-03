# 🎵 LyricMind - AI Mood-Based Song Recommendation

## Overview
LyricMind is a Spring Boot backend that recommends songs from lyrics using a RAG-style pipeline:
- embed songs into a MongoDB Atlas vector index
- retrieve candidates with semantic search
- rerank with an LLM for mood relevance
- return clean recommendation DTOs

## Backend High-Level Architecture

```mermaid
flowchart LR
    %% Clients
    A[Client App / Postman] --> B[RecommendationController\n/api/lyricmind/v1/recommendations]
    A --> C[EmbeddingsController\n/api/lyricmind/v1/embeddings/bulk-songs]

    %% Recommendation path
    B --> D[RecommendationService]
    D --> E[SemanticQueryComponent\nVector similaritySearch]
    E --> F[(MongoDB Atlas\nVector Store)]
    D --> G[RerankComponent\nOpenAI Chat rerank]
    G --> H[(OpenAI Chat Model\ngpt-4o-mini)]
    D --> I[SongRepository]
    I --> J[(MongoDB Songs Collection)]
    D --> K[SongRecommendationResponse List]

    %% Ingestion path
    C --> L[SongEmbeddingService]
    L --> M[DatasetGeneratorComponent\nCSV -> SongRequest]
    L --> I
    L --> N[VectorStore.add Documents]
    N --> F

    %% Cross-cutting
    O[ErrorHandler @ControllerAdvice] -. handles .-> B
    O -. handles .-> C

    %% Styling
    classDef api fill:#e3f2fd,stroke:#1565c0,color:#0d47a1;
    classDef svc fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20;
    classDef ai fill:#fff3e0,stroke:#ef6c00,color:#e65100;
    classDef data fill:#f3e5f5,stroke:#6a1b9a,color:#4a148c;

    class B,C api;
    class D,E,G,L,M,N svc;
    class H ai;
    class F,I,J,K,O data;
```

## Recommendation Flow (Runtime)

```mermaid
sequenceDiagram
    participant U as Client
    participant RC as RecommendationController
    participant RS as RecommendationService
    participant SQ as SemanticQueryComponent
    participant VS as MongoDB Vector Store
    participant RR as RerankComponent
    participant OAI as OpenAI Chat Model
    participant Repo as SongRepository
    participant DB as MongoDB Songs

    U->>RC: POST /recommendations { mood, limit }
    RC->>RS: recommendSongs(mood, limit)
    RS->>SQ: similaritySearch(mood, limit*2)
    SQ->>VS: vector query (threshold 0.6)
    VS-->>SQ: candidate Documents
    SQ-->>RS: candidates

    RS->>RR: rerank(mood, candidates)
    RR->>OAI: ranking prompt (JSON output)
    OAI-->>RR: ranked doc indexes + motivation
    RR-->>RS: reranked Documents

    RS->>Repo: findById(songId per document)
    Repo->>DB: load full Song entities
    DB-->>Repo: Song records
    Repo-->>RS: Song + metadata

    RS-->>RC: List<SongRecommendationResponse>
    RC-->>U: 200 OK recommendations
```

## Ingestion Flow (Bulk CSV)

```mermaid
sequenceDiagram
    participant U as Client
    participant EC as EmbeddingsController
    participant ES as SongEmbeddingService
    participant DG as DatasetGeneratorComponent
    participant Repo as SongRepository
    participant DB as MongoDB Songs
    participant VS as VectorStore
    participant VDB as MongoDB Vector Store

    U->>EC: POST /embeddings/bulk-songs { fileName }
    EC->>ES: createEmbeddingFromBulkSong(request)
    ES->>DG: generateSongRequestFromCSV(fileName)
    DG-->>ES: List<SongRequest>

    ES->>Repo: saveAll(Song entities)
    Repo->>DB: insert songs
    DB-->>Repo: saved songs with IDs
    Repo-->>ES: saved songs

    ES->>VS: add(List<Document>)
    VS->>VDB: embed + store vectors
    VDB-->>VS: indexed documents
    VS-->>ES: success

    ES-->>EC: BulkSongResponse(count)
    EC-->>U: 201 Created
```

## Interview Talking Points
- Two storage views: structured song data in MongoDB + semantic vectors in Atlas vector index.
- Hybrid retrieval: vector search gets relevant candidates; repository lookup fetches full song entity.
- Quality boost: LLM reranking adds better ordering and human-readable motivation.
- Resilience: rerank failures gracefully fall back to original vector ranking.
- Clear separation of concerns: controller -> service -> component -> repository/vector store.

## Tech Stack
- Spring Boot 3
- Spring AI (OpenAI model + MongoDB Atlas vector store)
- MongoDB Atlas (document + vector index)
- OpenAI Embeddings + Chat model
