package com.lyricmind.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.mongodb.collection-name:lyricmind_vector_store}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.mongodb.index-name:lyricmind_vector_index}")
    private String indexName;

    @Value("${spring.ai.vectorstore.mongodb.path-name:embedding}")
    private String pathName;

    @Bean
    public VectorStore vectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel) {
        return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .collectionName(collectionName)
                .vectorIndexName(indexName)
                .pathName(pathName)
                .initializeSchema(false)
                .build();
    }
}
