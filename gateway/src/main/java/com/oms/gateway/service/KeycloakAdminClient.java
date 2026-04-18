package com.oms.gateway.service;

import com.oms.gateway.dto.RegisterRequest;
import com.oms.gateway.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KeycloakAdminClient {

    private final WebClient webClient;
    private final String tokenUri;
    private final String logoutUri;
    private final String adminUsersUri;
    private final String clientId;
    private final String clientSecret;
    private final String publicClientId;

    public KeycloakAdminClient(
        @Value("${keycloak.url}") String keycloakUrl,
        @Value("${keycloak.realm}") String realm,
        @Value("${keycloak.client-id}") String clientId,
        @Value("${keycloak.client-secret}") String clientSecret,
        @Value("${keycloak.public-client-id}") String publicClientId
    ) {
        this.webClient = WebClient.builder().filter(logResponseStatus()).build();
        this.tokenUri = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        this.logoutUri = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
        this.adminUsersUri = keycloakUrl + "/admin/realms/" + realm + "/users";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.publicClientId = publicClientId;
    }

    private static ExchangeFilterFunction logResponseStatus() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("Keycloak response: status={} uri={}", response.statusCode(), response.request().getURI());
            return Mono.just(response);
        });
    }

    // ── User auth flows (public client) ──────────────────────────────────────

    public Mono<TokenResponse> login(String email, String password) {
        return webClient.post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("grant_type", "password")
                .with("client_id", publicClientId)
                .with("username", email)
                .with("password", password)
                .with("scope", "openid profile email"))
            .retrieve()
            .onStatus(s -> s.value() == 401, res -> {
                log.warn("Keycloak login rejected: status=401");
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
            })
            .onStatus(HttpStatusCode::is4xxClientError, res ->
                res.bodyToMono(Map.class).flatMap(body -> {
                    log.warn("Keycloak login error: status={} body={}", res.statusCode(), body);
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        (String) body.getOrDefault("error_description", "Login failed")));
                }))
            .bodyToMono(TokenResponse.class);
    }

    public Mono<TokenResponse> refresh(String refreshToken) {
        return webClient.post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                .with("client_id", publicClientId)
                .with("refresh_token", refreshToken))
            .retrieve()
            .onStatus(s -> s.value() == 400 || s.value() == 401, res -> {
                log.warn("Keycloak refresh rejected: status={}", res.statusCode());
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired"));
            })
            .bodyToMono(TokenResponse.class);
    }

    public Mono<Void> logout(String refreshToken) {
        return webClient.post()
            .uri(logoutUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("client_id", publicClientId)
                .with("refresh_token", refreshToken))
            .retrieve()
            .onStatus(HttpStatusCode::isError, res -> Mono.empty())
            .toBodilessEntity()
            .then();
    }

    // ── Admin flows (service-account client) ─────────────────────────────────

    public Mono<Void> createUser(RegisterRequest req) {
        return getServiceAccountToken()
            .flatMap(token -> webClient.post()
                .uri(adminUsersUri)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "firstName", req.firstName(),
                    "lastName", req.lastName(),
                    "email", req.email(),
                    "username", req.email(),
                    "enabled", true,
                    "credentials", List.of(Map.of(
                        "type", "password",
                        "value", req.password(),
                        "temporary", false
                    ))
                ))
                .retrieve()
                .onStatus(s -> s.value() == 409, res -> {
                    log.warn("Keycloak createUser conflict: status=409 email={}", req.email());
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered"));
                })
                .onStatus(HttpStatusCode::is4xxClientError, res ->
                    res.bodyToMono(Map.class).flatMap(body -> {
                        log.warn("Keycloak createUser error: status={} body={}", res.statusCode(), body);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            (String) body.getOrDefault("errorMessage", "Registration failed")));
                    }))
                .toBodilessEntity()
                .then());
    }

    private Mono<String> getServiceAccountToken() {
        return webClient.post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                .with("client_id", clientId)
                .with("client_secret", clientSecret))
            .retrieve()
            .onStatus(HttpStatusCode::isError, res -> {
                log.error("Keycloak service-account token failed: status={}", res.statusCode());
                return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Service token unavailable"));
            })
            .bodyToMono(Map.class)
            .map(body -> (String) body.get("access_token"));
    }
}
