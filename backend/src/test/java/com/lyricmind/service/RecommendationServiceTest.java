package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.model.Song;
import com.lyricmind.model.dto.SongRecommendationResponse;
import com.lyricmind.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.document.Document;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock private SongRepository songRepository;
    @Mock private RerankComponent rerankComponent;
    @Mock private SemanticQueryComponent semanticQueryComponent;

    @InjectMocks
    private RecommendationService recommendationService;

    private Song testSong;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testSong = new Song();
        testSong.setId("song123");
        testSong.setTitle("Test Song");
        testSong.setArtist("Test Artist");
        testSong.setAlbum("Test Album");
        testSong.setGenre("Rock");
        testSong.setReleaseYear(2023);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("songId", "song123");
        metadata.put("motivation", "Perfect match for your mood");
        testDocument = new Document("Test Content", metadata);
    }

    @Test
    void recommendSongs_ValidInput_ReturnsRecommendations() {
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);
        List<Document> reranked = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates, limit)).thenReturn(reranked);
        when(songRepository.findAllById(List.of("song123"))).thenReturn(List.of(testSong));

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Song", result.get(0).title());
        assertEquals("Perfect match for your mood", result.get(0).motivation());

        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verify(rerankComponent).rerank(mood, candidates, limit);
        verify(songRepository).findAllById(List.of("song123"));
    }

    @Test
    void recommendSongs_NoCandidatesFound_ReturnsEmptyList() {
        String mood = "unknown";
        int limit = 5;

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(List.of());

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verifyNoInteractions(rerankComponent);
        verifyNoInteractions(songRepository);
    }

    @Test
    void recommendSongs_RerankFailure_FallbackToCandidates() {
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates, limit)).thenThrow(new RuntimeException("Rerank failed"));
        when(songRepository.findAllById(List.of("song123"))).thenReturn(List.of(testSong));

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verify(rerankComponent).rerank(mood, candidates, limit);
        // Falls back to candidates order, still does DB lookup
        verify(songRepository).findAllById(List.of("song123"));
    }

    @Test
    void recommendSongs_SongNotFoundInDB_FiltersOut() {
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates, limit)).thenReturn(candidates);
        when(songRepository.findAllById(List.of("song123"))).thenReturn(List.of());

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void recommendSongs_SemanticSearchFailure_ThrowsException() {
        String mood = "happy";
        int limit = 5;

        when(semanticQueryComponent.similaritySearch(mood, limit))
                .thenThrow(new RuntimeException("Search failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> recommendationService.recommendSongs(mood, limit));
        assertEquals("Recommendation failed", exception.getMessage());
    }

    @Test
    void recommendSongs_MissingSongId_FiltersOut() {
        String mood = "happy";
        int limit = 5;

        Map<String, Object> metadataWithoutSongId = new HashMap<>();
        metadataWithoutSongId.put("motivation", "Test motivation");
        Document docWithoutSongId = new Document("Test Content", metadataWithoutSongId);
        List<Document> candidates = List.of(docWithoutSongId);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates, limit)).thenReturn(candidates);

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        // findAllById is called with empty list (no valid songIds) — no interactions expected
        verifyNoInteractions(songRepository);
    }

    @Test
    void recommendSongs_BatchLookup_UsesFindsAllById() {
        String mood = "chill";
        int limit = 3;

        Song song2 = new Song();
        song2.setId("song456");
        song2.setTitle("Song Two");
        song2.setArtist("Artist Two");

        Map<String, Object> meta2 = new HashMap<>();
        meta2.put("songId", "song456");
        meta2.put("motivation", "Matches chill vibe");
        Document doc2 = new Document("Content 2", meta2);

        List<Document> candidates = List.of(testDocument, doc2);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates, limit)).thenReturn(candidates);
        when(songRepository.findAllById(List.of("song123", "song456")))
                .thenReturn(List.of(testSong, song2));

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertEquals(2, result.size());
        // Verify single batch call — not individual findById calls
        verify(songRepository, times(1)).findAllById(anyList());
        verify(songRepository, never()).findById(anyString());
    }
}