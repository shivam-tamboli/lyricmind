package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.model.Song;
import com.lyricmind.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.document.Document;
import java.util.HashMap;
import java.util.Map;


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

    }

}
