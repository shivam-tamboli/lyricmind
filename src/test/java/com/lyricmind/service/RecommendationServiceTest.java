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


@ExtendWith(MockitoExtension.class)                                    //Enable mock injection automatically
public class RecommendationServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private RerankComponent rerankComponent;

    @Mock
    private SemanticQueryComponent semanticQueryComponent;

    @InjectMocks
    private RecommendationService recommendationService;

    private Song testSong;                                              //shared test objects
    private Document testDocument;

    @BeforeEach                                                        //this method runs before every test cases.
    void setUp() {
        testSong = new Song();                                          //dummy test data.
        testSong.setId("song123");
        testSong.setTitle("Test Song");
        testSong.setArtist("Test Artist");
        testSong.setAlbum("Test Album");
        testSong.setGenre("Rock");
        testSong.setReleaseYear(2023);

        Map<String, Object> metadate = new HashMap<>();                 //sample document.
        metadate.put("songId", "song123");
        metadate.put("motivation", "Perfect match for your mood");

        testDocument = new Document("Test Content", metadate);
    }

    @Test
    void recommendSongs_ValidInput_ReturnsRecommendations() {
        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);
        List<Document> reranked = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood, candidates)).thenReturn(reranked);
        when(songRepository.findById("song123")).thenReturn(Optional.of(testSong));

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verify(rerankComponent).rerank(mood, candidates);
        verify(songRepository).findById("song123");
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

    }

    @Test
    void recommendSongs_RerankFailure_FallbackToCandidates() {

        String mood = "happy";
        int limit = 5;

        List<Document> candidates = List.of(testDocument);

        when(semanticQueryComponent.similaritySearch(mood, limit)).thenReturn(candidates);
        when(rerankComponent.rerank(mood,candidates)).thenThrow(new RuntimeException("Rerank failed"));
        when(songRepository.findById("song123")).thenReturn(Optional.of(testSong));

        List<SongRecommendationResponse> result = recommendationService.recommendSongs(mood, limit);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(semanticQueryComponent).similaritySearch(mood, limit);
        verify(rerankComponent).rerank(mood,candidates);
        verify(songRepository).findById("song123");
    }



}
