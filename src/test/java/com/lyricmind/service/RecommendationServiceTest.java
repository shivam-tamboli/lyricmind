package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.repository.SongRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


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
}
