package com.oms.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/api/products", "/api/products/**").permitAll()
                .pathMatchers("/api/chat/**").hasRole("CUSTOMER")
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
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
                    .collect(Collectors.toList());
            } else {
                roles = List.of();
            }
            return Flux.fromIterable(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r));
        });
        return converter;
    }
}
