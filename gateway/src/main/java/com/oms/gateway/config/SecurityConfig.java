package com.oms.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setMaxAge(3600L);
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * EventSource (SSE) cannot send headers, so the frontend passes the JWT as
     * ?access_token=... This filter hoists it into Authorization: Bearer before
     * the security filter chain processes the request.
     */
    @Bean
    @Order(-101)
    public WebFilter sseTokenFilter() {
        return (exchange, chain) -> {
            String token = exchange.getRequest().getQueryParams().getFirst("access_token");
            if (token != null && exchange.getRequest().getHeaders().getFirst("Authorization") == null) {
                URI originalUri = exchange.getRequest().getURI();
                String newQuery = Arrays.stream(originalUri.getRawQuery().split("&"))
                    .filter(p -> !p.startsWith("access_token="))
                    .collect(Collectors.joining("&"));
                URI newUri = UriComponentsBuilder.fromUri(originalUri)
                    .replaceQuery(newQuery.isEmpty() ? null : newQuery)
                    .build(true)
                    .toUri();
                exchange = exchange.mutate()
                    .request(r -> r.uri(newUri).header("Authorization", "Bearer " + token))
                    .build();
            }
            return chain.filter(exchange);
        };
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/auth/login", "/auth/refresh", "/auth/logout", "/auth/register").permitAll()
                .pathMatchers("/api/v1/products", "/api/v1/products/**").permitAll()
                .pathMatchers("/api/v1/chat/**").hasRole("CUSTOMER")
                .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakConverter()))
            )
            .build();
    }

    private ReactiveJwtAuthenticationConverter keycloakConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return Flux.empty();
            Object rolesObj = realmAccess.get("roles");
            List<String> roles;
            if (rolesObj instanceof List<?>) {
                roles = ((List<?>) rolesObj).stream()
                    .map(Object::toString)
                    .toList();
            } else {
                roles = List.of();
            }
            return Flux.fromIterable(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r));
        });
        return converter;
    }
}
