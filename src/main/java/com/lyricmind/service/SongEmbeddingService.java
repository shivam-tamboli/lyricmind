package com.lyricmind.service;

import com.lyricmind.component.DatasetGeneratorComponent;
import com.lyricmind.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongEmbeddingService {

    private static final String RESOURCES_PATH = "src/main/resources/";

    private final SongRepository songRepository;
    private final VectorStore vectorStore;
    private final DatasetGeneratorComponent datasetGeneratorComponent;

}
