package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.model.Song;
import com.lyricmind.model.dto.SongRecommendationResponse;
import com.lyricmind.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;

    /**
     * Orchestrates the full RAG recommendation pipeline:
     * 1. Vector search — retrieve semantically similar candidates
     * 2. LLM rerank   — select and rank the top {@code limit} songs by mood fit
     * 3. DB enrich    — single batch query to load full Song documents
     *
     * <p>Results are cached by (mood, limit) for 15 minutes. Cache is evicted
     * when new songs are ingested via the bulk embedding endpoint.
     */
    @Cacheable(value = "recommendations", key = "#mood.toLowerCase().trim() + ':' + #limit")
    public List<SongRecommendationResponse> recommendSongs(String mood, int limit) {
        long totalStart = System.currentTimeMillis();
        log.info("[START] Recommendations request — mood: '{}', limit: {}", mood, limit);

        try {
            // 1. VECTOR SEARCH
            long t0 = System.currentTimeMillis();
            List<Document> candidates = findCandidateSongs(mood, limit);
            log.info("[PERF] Vector search: {}ms — {} candidates found", System.currentTimeMillis() - t0, candidates.size());

            if (candidates.isEmpty()) {
                log.info("[END] No candidates for mood: '{}' ({}ms total)", mood, System.currentTimeMillis() - totalStart);
                return Collections.emptyList();
            }

            // 2. LLM RERANK — skip when candidates <= limit: nothing to rank, all will be selected anyway
            long t1 = System.currentTimeMillis();
            List<Document> reranked;
            if (candidates.size() <= limit) {
                log.info("[PERF] Reranking skipped — {} candidates ≤ limit {}, using vector order", candidates.size(), limit);
                reranked = candidates;
            } else {
                reranked = rerankCandidates(mood, candidates, limit);
                log.info("[PERF] Reranking: {}ms — {} documents selected", System.currentTimeMillis() - t1, reranked.size());
            }

            // 3. BATCH DB ENRICHMENT + RESPONSE MAPPING
            long t2 = System.currentTimeMillis();
            List<SongRecommendationResponse> recommendations = mapDocumentsToRecommendations(reranked, limit);
            log.info("[PERF] DB lookup + mapping: {}ms — {} results", System.currentTimeMillis() - t2, recommendations.size());

            log.info("[END] Total: {}ms — {} recommendations for '{}'", System.currentTimeMillis() - totalStart, recommendations.size(), mood);
            return recommendations;

        } catch (Exception e) {
            log.error("[ERROR] Recommendation failed after {}ms for mood: '{}'", System.currentTimeMillis() - totalStart, mood, e);
            throw new RuntimeException("Recommendation failed", e);
        }
    }

    private List<Document> findCandidateSongs(String mood, int limit) {
        try {
            return semanticQueryComponent.similaritySearch(mood, limit);
        } catch (Exception e) {
            log.error("Vector search failed for mood: '{}'", mood, e);
            throw new RuntimeException("Search failed", e);
        }
    }

    private List<Document> rerankCandidates(String mood, List<Document> candidates, int limit) {
        try {
            return rerankComponent.rerank(mood, candidates, limit);
        } catch (Exception e) {
            log.warn("Reranking failed for mood: '{}', falling back to vector search order", mood, e);
            return candidates;  // Graceful fallback
        }
    }

    /**
     * Maps reranked documents to API response DTOs using a single batch DB call.
     *
     * <p>Instead of calling {@code findById()} once per document (N+1 anti-pattern),
     * this collects all songIds upfront, loads them in one {@code findAllById()} call,
     * and maps results while preserving the reranked order.
     */
    private List<SongRecommendationResponse> mapDocumentsToRecommendations(List<Document> documents, int limit) {
        List<Document> topDocs = documents.stream().limit(limit).collect(Collectors.toList());

        // Extract songIds in reranked order
        List<String> songIds = topDocs.stream()
                .map(this::extractSongId)
                .collect(Collectors.toList());

        // Short-circuit when no valid IDs (e.g. all metadata missing songId)
        List<String> validIds = songIds.stream().filter(StringUtils::hasText).collect(Collectors.toList());
        if (validIds.isEmpty()) {
            log.warn("No valid songIds in {} reranked documents", topDocs.size());
            return Collections.emptyList();
        }

        // Single batch DB call instead of N individual findById() calls
        Map<String, Song> songMap = songRepository
                .findAllById(validIds)
                .stream()
                .collect(Collectors.toMap(Song::getId, s -> s));

        // Build responses preserving reranked order
        List<SongRecommendationResponse> results = new ArrayList<>();
        for (int i = 0; i < topDocs.size(); i++) {
            String songId = songIds.get(i);
            if (!StringUtils.hasText(songId)) {
                log.warn("Missing songId at position {}", i);
                continue;
            }
            Song song = songMap.get(songId);
            if (song == null) {
                log.warn("Song not found for ID: {}", songId);
                continue;
            }
            results.add(createRecommendationResponse(song, extractMotivation(topDocs.get(i))));
        }
        return results;
    }

    private String extractSongId(Document document) {
        Object songIdObj = document.getMetadata().get("songId");
        return songIdObj != null ? songIdObj.toString() : null;
    }

    private String extractMotivation(Document document) {
        Object motivationObj = document.getMetadata().get("motivation");
        return motivationObj != null ? motivationObj.toString() : "Recommended based on mood similarity";
    }

    private SongRecommendationResponse createRecommendationResponse(Song song, String motivation) {
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
        return StringUtils.hasText(text) ? text.trim() : "";
    }
}