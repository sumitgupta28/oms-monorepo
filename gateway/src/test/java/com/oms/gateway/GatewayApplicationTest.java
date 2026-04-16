package com.oms.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {GatewayApplication.class, GatewayApplicationTest.TestJwtConfig.class}
)
@DisplayName("GatewayApplication")
class GatewayApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Configuration
    static class TestJwtConfig {
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
    @DisplayName("application context loads successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("SecurityWebFilterChain bean is registered")
    void securityWebFilterChainBeanIsRegistered() {
        var filterChain = applicationContext.getBean(SecurityWebFilterChain.class);

        assertThat(filterChain).isNotNull();
    }

    @Test
    @DisplayName("application name is configured as 'gateway'")
    void applicationNameIsConfigured() {
        var appName = applicationContext.getEnvironment().getProperty("spring.application.name");

        assertThat(appName).isEqualTo("gateway");
    }
}
