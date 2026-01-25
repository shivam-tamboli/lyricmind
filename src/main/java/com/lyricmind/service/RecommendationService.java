package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.model.dto.SongRecommendationResponse;
import com.lyricmind.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 10;

    //Dependency injection.
    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;

    public List<SongRecommendationResponse> recommendSongs(String mood, int limit) {

        // Input: User mood ("happy") + desired result count
        // Output: Ranked song recommendations with AI explanations
        // Flow: Vector Search -> AI Rerank -> DTO Response

        log.info("Requesting song recommendations for mood: '{}' with limit: {}", mood, limit);
        // ENTRY LOG: Trace API calls for debugging and monitoring
        try {
            // STEP 1: RAG RETRIEVAL PHASE - Vector similarity search
            // Calls SemanticQueryComponent -> MongoDB Atlas vector search
            // Gets top candidates (limit*2 for reranking extras)
            List<Document> candidates = findCandidateSongs(mood, limit);

            // EARLY RETURN: Handle no results.
            if (candidates.isEmpty()) {
                log.info("No candidate songs found for mood: '{}'", mood);
                return Collections.emptyList(); //clean empty response.
            }

            // STEP 2: RAG POST-RETRIEVAL - AI-powered re-ranking
            // Calls RerankComponent -> OpenAI chat model refines vector search results
            // Adds "motivation" metadata explaining relevance
            List<Document> rerankedResults = rerankCandidates(mood, candidates);

            // STEP 3: RESPONSE MAPPING - Raw Documents to clean API DTOs
            // Extracts metadata and applies final limit for JSON response
            List<SongRecommendationResponse> recommendations = mapDocumentsToRecommendations(rerankedResults, limit);

            // SUCCESS LOG: Track successful recommendations generated
            log.info("Successfully generated {} recommendations for mood: '{}'", recommendations.size(), mood);
            return recommendations;

        } catch (Exception e) {
            // ERROR HANDLING: Catch vector search, AI, or mapping failures
            log.error("Failed to generate recommendations for mood: '{}'", mood, e);
            throw new RuntimeException("Recommendation generation failed", e);
        }
    }

    private List<Document> findCandidateSongs(String mood, int limit){
        // VECTOR SEARCH: Finds candidate songs matching mood
        // Calls SemanticQueryComponent.similaritySearch()
        // Returns raw Documents from MongoDB vector search (limit*2 candidates)
        // Used as input for AI re-ranking

        try{
            // EXECUTE SEMANTIC SEARCH: Embeds mood query -> MongoDB HNSW vector search
            // similarityThreshold=0.6 filters weak matches automatically
            // topK = limit*2 (extra candidates for re-ranking quality)
            List<Document> candidates = semanticQueryComponent.similaritySearch(mood, limit);
            return candidates;

        }catch (Exception e) {
            // ERROR HANDLING: Vector store connection, embedding, or search failures
            log.error("Failed to find candidate songs for mood: '{}'", mood, e);
            throw new RuntimeException("Candidate search failed", e);
        }
    }

    private List<Document> rerankCandidates(String mood, List<Document> candidates){
        // AI RERANKING: Refines vector search results using OpenAI chat model
        // Calls RerankComponent.rerank() -> LLM reorders by true mood relevance
        // Adds "motivation" metadata to each Document explaining ranking
        // Input: Raw vector search candidates, Output: Ranked + enriched results

        try{
            // EXECUTE AI RERANKING: OpenAI analyzes mood + song metadata
            // Returns Documents sorted by semantic relevance (most relevant first)
            // Each Document gets "motivation" metadata (e.g., "Upbeat matches happy")
            List<Document> rerankedResults = rerankComponent.rerank(mood, candidates);

            log.debug("Re-ranked {} candidates for mood: '{}' (original: {})",
                    rerankedResults.size(), mood, candidates.size());
            return rerankedResults;

        }catch (Exception e){
            // ERROR HANDLING: AI API timeout, JSON parsing, or prompt failures
            log.error("Failed to re-rank candidates for mood: '{}'. Returning original candidates.", mood, e);
            return candidates;
        }


    }

    private List<SongRecommendationResponse> mapDocumentsToRecommendations(List<Document> documents, int limit) {
        return documents.stream()
                .limit(limit)
                .map(this::mapDocumentToRecommendation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
