package com.oms.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service @RequiredArgsConstructor @Slf4j
public class ChatService {

    private final ChatClient chatClient;

    public Flux<String> stream(String userId, String sessionId, String message, String token) {
        log.info("Stream: user={} session={}", userId, sessionId);
        return chatClient.prompt()
            .user(u -> u.text(message).param("bearerToken", token).param("userId", userId))
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
            .stream()
            .content()
            .doOnError(e -> log.error("Stream error: {}", e.getMessage()));
    }

    public String call(String userId, String sessionId, String message, String token) {
        log.info("Call: user={} session={}", userId, sessionId);
        return chatClient.prompt()
            .user(u -> u.text(message).param("bearerToken", token).param("userId", userId))
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
            .call()
            .content();
    }
}
