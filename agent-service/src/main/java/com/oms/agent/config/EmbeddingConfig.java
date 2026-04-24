package com.oms.agent.config;

import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class EmbeddingConfig {

    @Value("${embedding.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${embedding.ollama.model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

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
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public VectorStore vectorStore(JedisPooled jedisPooled, OllamaEmbeddingModel ollamaEmbeddingModel) {
        return RedisVectorStore.builder(jedisPooled, ollamaEmbeddingModel)
                .indexName("product-embeddings")
                .prefix("product:")
                .initializeSchema(true)
                .build();
    }
}
