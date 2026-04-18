package com.oms.gateway.controller;

import com.oms.gateway.dto.*;
import com.oms.gateway.service.KeycloakAdminClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAdminClient keycloakAdminClient;

    @PostMapping("/login")
    public Mono<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return keycloakAdminClient.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public Mono<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return keycloakAdminClient.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@RequestBody @Valid LogoutRequest request) {
        return keycloakAdminClient.logout(request.refreshToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> register(@RequestBody @Valid RegisterRequest request) {
        return keycloakAdminClient.createUser(request);
    }
}
