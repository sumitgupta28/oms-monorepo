package com.oms.agent.config;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${embedding.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${embedding.ollama.model:nomic-embed-text}")
    private String model;

    @Bean("embeddingOllamaApi")
    public OllamaApi embeddingOllamaApi() {
        return new OllamaApi.Builder().baseUrl(baseUrl).build();
    }

    @Bean
    public OllamaEmbeddingModel embeddingModel(@Qualifier("embeddingOllamaApi") OllamaApi ollamaApi) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaEmbeddingOptions.builder().model(model).build())
                .build();
    }
}
