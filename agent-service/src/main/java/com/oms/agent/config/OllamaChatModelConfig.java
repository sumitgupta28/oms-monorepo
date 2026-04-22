package com.oms.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("ollama")
@Slf4j
public class OllamaChatModelConfig {

    @Value("${ai.ollama.base-url}")
    private String baseUrl;

    @Value("${ai.ollama.model}")
    private String model;

    @Value("${ai.ollama.max-tokens}")
    private int numPredict;

    @Bean
    public ChatModel chatModel() {
        OllamaApi api = new OllamaApi.Builder()
                .baseUrl(baseUrl)
                .build();

        OllamaChatOptions options = OllamaChatOptions.builder()
                .logitsAll(true)
                .model(model)
                .numPredict(numPredict)
                .build();

        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(options)
                .build();
        log.info("Chat[OllamaChatModel] Model: {}", ollamaChatModel.getDefaultOptions().getModel());
        log.info("Chat[OllamaChatModel] MaxTokens: {}", ollamaChatModel.getDefaultOptions().getMaxTokens());
        return ollamaChatModel;
    }
}
