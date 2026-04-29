package com.oms.agent.config;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EmbeddingConfig {

    @Value("${embedding.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${embedding.ollama.model:nomic-embed-text}")
    private String embeddingModel;

    @Bean("embeddingOllamaApi")
    public OllamaApi embeddingOllamaApi() {
        return new OllamaApi.Builder().baseUrl(ollamaBaseUrl).build();
    }

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(@Qualifier("embeddingOllamaApi") OllamaApi ollamaApi) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaEmbeddingOptions.builder().model(embeddingModel).build())
                .build();
    }

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, OllamaEmbeddingModel ollamaEmbeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, ollamaEmbeddingModel)
                .dimensions(768) // nomic-embed-text produces 768-dimensional vectors
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .build();
    }
}
