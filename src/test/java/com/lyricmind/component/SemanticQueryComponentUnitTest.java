package com.lyricmind.component;


import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
public class SemanticQueryComponentUnitTest {

    @Mock
    private VectorStore vectorStore;//

    @InjectMocks
    private SemanticQueryComponent semanticQueryComponent;


}
