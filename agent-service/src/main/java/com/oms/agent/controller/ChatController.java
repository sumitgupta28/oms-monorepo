package com.oms.agent.controller;

import com.oms.agent.security.JwtTokenHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatClient chatClient;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
        @RequestParam String message,
        @RequestParam(defaultValue = "default") String sessionId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        String userId = jwt.getSubject();
        String email  = jwt.getClaimAsString("email");
        log.info("Chat request from user {} (email: {}) session {}", userId, email, sessionId);

        String systemPrompt = """
            You are an intelligent order management assistant for OMS. \
            Current user: %s (ID: %s). \
            You can help users place orders, track shipments, search products, and manage payments. \
            Always confirm details before placing orders or making payments. When using tools, explain what you are doing to the user.
            """.formatted(email, userId);

        ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder().event("done").data("").build();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", sessionId))
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder().data(content).build())
                .concatWith(Flux.just(doneEvent))
                .doOnError(error -> log.error("Stream error: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just(doneEvent))
                .contextWrite(ctx -> ctx.put(JwtTokenHolder.CONTEXT_KEY, jwt.getTokenValue()));
    }

    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        log.info("Session cleared: {}", sessionId);
    }
}
