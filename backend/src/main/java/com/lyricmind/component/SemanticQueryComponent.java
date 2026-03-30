package com.lyricmind.component;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;


import java.util.List;
@Component
public class SemanticQueryComponent {
    private final VectorStore vectorStore;
    private Logger logger = LoggerFactory.getLogger(SemanticQueryComponent.class);

    public SemanticQueryComponent(VectorStore vectorStore){
        this.vectorStore = vectorStore;
        logger.info("VectorStore initialized: {}", vectorStore.getClass().getName());
    }


    public List<Document> similaritySearch(String mood, int limit) {
        logger.info("Searching for songs with mood: '{}', limit: {}", mood, limit);

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(mood)
                    .topK(limit * 10)
                    .similarityThreshold(0.0)
                    .build();

            logger.info("Executing vector search with topK: {}, threshold: {}, VectorStore: {}", 
                    searchRequest.getTopK(), searchRequest.getSimilarityThreshold(), vectorStore.getClass().getName());

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            logger.info("Vector search returned {} documents", results.size());
            
            if (results.isEmpty()) {
                logger.warn("No results found for query: '{}'. Check if embeddings exist in MongoDB.", mood);
                logger.warn("VectorStore class: {}", vectorStore.getClass().getName());
                logger.warn("VectorStore toString: {}", vectorStore.toString());
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Vector search failed for mood: '{}', error: {}", mood, e.getMessage(), e);
            throw new RuntimeException("Vector search failed: " + e.getMessage(), e);
        }
    }
}
