package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.model.Song;
import com.lyricmind.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.text.Document;


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
    }
}
