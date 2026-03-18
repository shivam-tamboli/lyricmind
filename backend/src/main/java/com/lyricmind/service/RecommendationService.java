package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.model.Song;
import com.lyricmind.model.dto.SongRecommendationResponse;
import com.lyricmind.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    // DEPENDENCY INJECTION: Spring auto-injects these components
    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;

    /**
     * MAIN METHOD: Orchestrates complete RAG pipeline for song recommendations
     * 1. Vector search finds candidate songs
     * 2. AI re-ranks by mood relevance
     * 3. Formats results for API response
     */
    public List<SongRecommendationResponse> recommendSongs(String mood, int limit) {
        log.info("Requesting recommendations for mood: '{}' limit: {}", mood, limit);

        try {
            // 1. VECTOR SEARCH: Find candidate songs matching mood
            List<Document> candidates = findCandidateSongs(mood, limit);

            if (candidates.isEmpty()) {
                log.info("No songs found for mood: '{}'", mood);
                return Collections.emptyList();
            }

            // 2. AI RERANK: Refine results using OpenAI chat model
            List<Document> reranked = rerankCandidates(mood, candidates);

            // 3. FORMAT RESPONSE: Convert to clean API objects
            List<SongRecommendationResponse> recommendations =
                    mapDocumentsToRecommendations(reranked, limit);

            log.info("Generated {} recommendations for '{}'", recommendations.size(), mood);
            return recommendations;

        } catch (Exception e) {
            log.error("Recommendation failed for mood: '{}'", mood, e);
            throw new RuntimeException("Recommendation failed", e);
        }
    }

    // HELPER 1: Delegates to vector search component
    private List<Document> findCandidateSongs(String mood, int limit) {
        try {
            return semanticQueryComponent.similaritySearch(mood, limit);
        } catch (Exception e) {
            log.error("Vector search failed for mood: '{}'", mood, e);
            throw new RuntimeException("Search failed", e);
        }
    }

    // HELPER 2: Delegates to AI reranking component
    private List<Document> rerankCandidates(String mood, List<Document> candidates) {
        try {
            return rerankComponent.rerank(mood, candidates);
        } catch (Exception e) {
            log.warn("Reranking failed for mood: '{}', using original order", mood, e);
            return candidates;  // Graceful fallback
        }
    }

    // HELPER 3: Converts Documents to API response format
    private List<SongRecommendationResponse> mapDocumentsToRecommendations(
            List<Document> documents, int limit) {
        return documents.stream()
                .limit(limit)
                .map(this::mapDocumentToRecommendation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<SongRecommendationResponse> mapDocumentToRecommendation(Document document) {
        // CORE MAPPING: Converts vector search Document → full SongRecommendationResponse DTO
        // 1. Extracts songId from Document metadata
        // 2. Loads complete Song from MongoDB using SongRepository
        // 3. Adds AI "motivation" from reranking
        // 4. Creates clean API response (handles missing data safely)

        try {
            String songId = extractSongId(document);
            if (!StringUtils.hasText(songId)) {
                log.warn("Song ID missing in document metadata");
                return Optional.empty();
            }

            // LOAD FULL SONG: Vector search gives metadata only, need complete Song
            Optional<Song> songOptional = findSongById(songId);
            if (songOptional.isEmpty()) {
                log.warn("Song not found for ID: {}", songId);
                return Optional.empty();
            }

            Song song = songOptional.get();
            String motivation = extractMotivation(document);

            // CREATE FINAL DTO: All song details + AI explanation
            SongRecommendationResponse recommendation = createRecommendationResponse(song, motivation);

            log.debug("Mapped song: '{}' by '{}' to recommendation",
                    song.getTitle(), song.getArtist());
            return Optional.of(recommendation);

        } catch (Exception e) {
            log.error("Document mapping failed", e);
            return Optional.empty();  // Safe fallback
        }
    }

    private String extractSongId(Document document) {
        // Extracts songId from Document metadata (set during ingestion)
        Object songIdObj = document.getMetadata().get("songId");
        return songIdObj != null ? songIdObj.toString() : null;
    }

    private String extractMotivation(Document document) {
        // Gets AI reranking explanation ("Upbeat matches happy mood")
        Object motivationObj = document.getMetadata().get("motivation");
        return motivationObj != null ? motivationObj.toString() : "Recommended based on mood similarity";
    }

    private Optional<Song> findSongById(String songId) {
        // HYBRID LOOKUP: Vector search finds relevant docs, repository gets full Song details
        try {
            return songRepository.findById(songId);
        } catch (Exception e) {
            log.error("Database lookup failed for song ID: {}", songId, e);
            return Optional.empty();
        }
    }

    private SongRecommendationResponse createRecommendationResponse(Song song, String motivation) {
        // BUILDS API RESPONSE: Clean DTO with sanitized data
        return new SongRecommendationResponse(
                sanitizeText(song.getTitle()),
                sanitizeText(song.getArtist()),
                sanitizeText(song.getAlbum()),
                sanitizeText(song.getGenre()),
                song.getReleaseYear(),
                sanitizeText(motivation)
        );
    }

    private String sanitizeText(String text) {
        // CLEANUP: Trims whitespace, handles null/empty → empty string
        return StringUtils.hasText(text) ? text.trim() : "";
    }

}
