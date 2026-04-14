package com.oms.agent.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatClient chatClient;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
        @RequestParam String message,
        @RequestParam(defaultValue = "default") String sessionId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        String email  = jwt.getClaimAsString("email");
        log.info("Chat request from user {} session {}", userId, sessionId);

        String systemPrompt = """
            You are an intelligent order management assistant for OMS.
            Current user: %s (ID: %s).
            You can help users place orders, track shipments, search products, and manage payments.
            Always confirm details before placing orders or making payments.
            When using tools, explain what you are doing to the user.
            """.formatted(email, userId);

        return chatClient.prompt()
            .system(systemPrompt)
            .user(message)
            .stream()
            .content();
    }

    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        log.info("Session cleared: {}", sessionId);
    }
}
