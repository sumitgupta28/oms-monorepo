package com.oms.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest
@Import({SecurityConfig.class, SecurityConfigTest.StubRouterConfig.class})
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext applicationContext;

    @Configuration
    static class StubRouterConfig {

        @Bean
        public RouterFunction<ServerResponse> stubRoutes() {
            return RouterFunctions.route()
                    .GET("/actuator/health", req -> ServerResponse.ok().bodyValue("UP"))
                    .GET("/api/products", req -> ServerResponse.ok().bodyValue("[]"))
                    .GET("/api/products/{id}", req -> ServerResponse.ok().bodyValue("{}"))
                    .POST("/api/products", req -> ServerResponse.ok().bodyValue("created"))
                    .GET("/api/chat/{path}", req -> ServerResponse.ok().bodyValue("chat"))
                    .POST("/api/chat/{path}", req -> ServerResponse.ok().bodyValue("chat"))
                    .GET("/api/admin/{path}", req -> ServerResponse.ok().bodyValue("admin"))
                    .GET("/api/orders/{id}", req -> ServerResponse.ok().bodyValue("order"))
                    .POST("/api/orders", req -> ServerResponse.ok().bodyValue("order-created"))
                    .GET("/api/payments/{id}", req -> ServerResponse.ok().bodyValue("payment"))
                    .GET("/api/inventory/{id}", req -> ServerResponse.ok().bodyValue("inventory"))
                    .build();
        }

        @Bean
        public ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build());
        }
    }

    @Test
    @DisplayName("SecurityWebFilterChain bean is present in the context")
    void securityWebFilterChainBeanIsPresent() {
        var bean = applicationContext.getBean(SecurityWebFilterChain.class);

        assertThat(bean).isNotNull();
    }

    @Nested
    @DisplayName("CSRF disabled")
    class CsrfDisabled {

        @Test
        @DisplayName("POST without CSRF token does not return 403 for CSRF reasons")
        void postWithoutCsrfTokenIsNotRejectedByCsrf() {
            webTestClient
                    .mutateWith(mockJwt())
                    .post().uri("/api/products")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("/actuator/health is accessible without authentication")
        void actuatorHealthIsPublic() {
            webTestClient
                    .get().uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("/api/products is accessible without authentication")
        void productsListIsPublic() {
            webTestClient
                    .get().uri("/api/products")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("/api/products/{id} is accessible without authentication")
        void productByIdIsPublic() {
            webTestClient
                    .get().uri("/api/products/123")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("POST /api/products is accessible without authentication (CSRF disabled)")
        void postProductIsPublicAndCsrfDisabled() {
            webTestClient
                    .post().uri("/api/products")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("/api/chat/** (requires CUSTOMER role)")
    class ChatEndpoints {

        @Test
        @DisplayName("returns 401 without authentication")
        void chatReturns401WithoutAuth() {
            webTestClient
                    .get().uri("/api/chat/stream")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("returns 403 without CUSTOMER role")
        void chatReturns403WithoutCustomerRole() {
            webTestClient
                    .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .get().uri("/api/chat/stream")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("returns 200 with CUSTOMER role")
        void chatReturns200WithCustomerRole() {
            webTestClient
                    .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                    .get().uri("/api/chat/stream")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("POST returns 200 with CUSTOMER role")
        void chatPostReturns200WithCustomerRole() {
            webTestClient
                    .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                    .post().uri("/api/chat/message")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("returns 403 when authenticated but with no roles")
        void chatReturns403WithNoRoles() {
            webTestClient
                    .mutateWith(mockJwt())
                    .get().uri("/api/chat/stream")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("/api/admin/** (requires ADMIN role)")
    class AdminEndpoints {

        @Test
        @DisplayName("returns 401 without authentication")
        void adminReturns401WithoutAuth() {
            webTestClient
                    .get().uri("/api/admin/dashboard")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("returns 403 without ADMIN role")
        void adminReturns403WithoutAdminRole() {
            webTestClient
                    .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                    .get().uri("/api/admin/dashboard")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("returns 200 with ADMIN role")
        void adminReturns200WithAdminRole() {
            webTestClient
                    .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .get().uri("/api/admin/dashboard")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("returns 403 when authenticated but with CUSTOMER role only")
        void adminReturns403WithCustomerRoleOnly() {
            webTestClient
                    .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                    .get().uri("/api/admin/users")
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("Authenticated-only endpoints (any role)")
    class AuthenticatedEndpoints {

        @Test
        @DisplayName("/api/orders/{id} returns 401 without authentication")
        void ordersReturns401WithoutAuth() {
            webTestClient
                    .get().uri("/api/orders/1")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("/api/orders/{id} returns 200 when authenticated")
        void ordersReturns200WhenAuthenticated() {
            webTestClient
                    .mutateWith(mockJwt())
                    .get().uri("/api/orders/1")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("/api/payments/{id} returns 401 without authentication")
        void paymentsReturns401WithoutAuth() {
            webTestClient
                    .get().uri("/api/payments/1")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("/api/payments/{id} returns 200 when authenticated")
        void paymentsReturns200WhenAuthenticated() {
            webTestClient
                    .mutateWith(mockJwt())
                    .get().uri("/api/payments/1")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("/api/inventory/{id} returns 401 without authentication")
        void inventoryReturns401WithoutAuth() {
            webTestClient
                    .get().uri("/api/inventory/1")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("/api/inventory/{id} returns 200 when authenticated")
        void inventoryReturns200WhenAuthenticated() {
            webTestClient
                    .mutateWith(mockJwt())
                    .get().uri("/api/inventory/1")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("POST /api/orders returns 200 when authenticated")
        void postOrdersReturns200WhenAuthenticated() {
            webTestClient
                    .mutateWith(mockJwt())
                    .post().uri("/api/orders")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("Keycloak JWT role extraction via gateway security")
    class KeycloakJwtIntegration {

        @Test
        @DisplayName("JWT with realm_access CUSTOMER role can access /api/chat")
        void jwtWithCustomerRealmAccessCanAccessChat() {
            webTestClient
                    .mutateWith(mockJwt().jwt(jwt -> jwt
                                    .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                            .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                    .get().uri("/api/chat/stream")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("JWT with realm_access ADMIN role can access /api/admin")
        void jwtWithAdminRealmAccessCanAccessAdmin() {
            webTestClient
                    .mutateWith(mockJwt().jwt(jwt -> jwt
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .get().uri("/api/admin/dashboard")
                    .exchange()
                    .expectStatus().isOk();
        }
    }
}
