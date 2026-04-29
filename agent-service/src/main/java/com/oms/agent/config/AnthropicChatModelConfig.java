package com.oms.agent.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("anthropic")
public class AnthropicChatModelConfig {

    @Value("${ai.anthropic.api-key}")
    private String apiKey;

    @Value("${ai.anthropic.model}")
    private String model;

    @Value("${ai.anthropic.max-tokens}")
    private int maxTokens;

    @Bean
    public ChatModel chatModel() {
        AnthropicApi api = new AnthropicApi.Builder()
                .apiKey(apiKey)
                .build();

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model)
                .maxTokens(maxTokens)
                .temperature(0.3)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();
    }
}
