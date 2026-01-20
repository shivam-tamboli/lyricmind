package com.lyricmind.service;

import com.lyricmind.component.RerankComponent;
import com.lyricmind.component.SemanticQueryComponent;
import com.lyricmind.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 10;

    private final SongRepository songRepository;
    private final RerankComponent rerankComponent;
    private final SemanticQueryComponent semanticQueryComponent;

}
