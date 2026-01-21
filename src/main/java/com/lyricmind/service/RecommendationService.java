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

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 10;

    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;

    public List<SongRecommendationResponse> recommendSongs(String mood, int limit) {

        log.info("Requesting song recommendations for mood: '{}' with limit: {}", mood, limit);

        try {
            // Get candidate songs through semantic search
            List<Document> candidates = findCandidateSongs(mood, limit);

            if (candidates.isEmpty()) {
                log.info("No candidate songs found for mood: '{}'", mood);
                return Collections.emptyList();
            }
            // Re-rank candidates using AI
            List<Document> rerankedResults = rerankCandidates(mood, candidates);
            // Map to recommendation responses
            List<SongRecommendationResponse> recommendations = mapDocumentsToRecommendations(rerankedResults, limit);

            log.info("Successfully generated {} recommendations for mood: '{}'", recommendations.size(), mood);
            return recommendations;

        } catch (Exception e) {
            log.error("Failed to generate recommendations for mood: '{}'", mood, e);
            throw new RuntimeException("Recommendation generation failed", e);
        }
    }

    private List<Document> findCandidateSongs(String mood, int limit){
        try{
            List<Document> candidates = semanticQueryComponent.similaritySearch(mood, limit);
            return candidates;
        }catch (Exception e) {
            log.error("Failed to find candidate songs for mood: '{}'", mood, e);
            throw new RuntimeException("Candidate search failed", e);
        }
    }

}
